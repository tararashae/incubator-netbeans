/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.netbeans.modules.debugger.jpda.visual.breakpoints;

import com.sun.jdi.ObjectReference;
import java.awt.EventQueue;
import java.beans.PropertyChangeListener;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.netbeans.api.debugger.Breakpoint;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.debugger.DebuggerManagerListener;
import org.netbeans.api.debugger.LazyActionsManagerListener;
import org.netbeans.spi.debugger.ContextProvider;
import org.netbeans.api.debugger.Session;
import org.netbeans.api.debugger.Watch;

import org.netbeans.api.debugger.jpda.JPDADebugger;

import org.netbeans.modules.debugger.jpda.SourcePath;
import org.netbeans.modules.debugger.jpda.JPDADebuggerImpl;
import org.netbeans.modules.debugger.jpda.visual.RemoteAWTScreenshot.AWTComponentInfo;



/**
 * Listens on JPDADebugger.PROP_STATE and DebuggerManager.PROP_BREAKPOINTS, and
 * and creates XXXBreakpointImpl classes for all JPDABreakpoints.
 *
 * @author  Martin Entlicher
 */
@LazyActionsManagerListener.Registration(path="netbeans-JPDASession/Java")
public class BreakpointsEngineListener extends LazyActionsManagerListener 
implements PropertyChangeListener, DebuggerManagerListener {
    
    private static final Logger logger = Logger.getLogger(BreakpointsEngineListener.class.getName());

    private JPDADebuggerImpl        debugger;
    private SourcePath           engineContext;
    private boolean                 started = false;
    private Session                 session;


    public BreakpointsEngineListener (ContextProvider lookupProvider) {
        debugger = (JPDADebuggerImpl) lookupProvider.lookupFirst 
            (null, JPDADebugger.class);
        engineContext = lookupProvider.lookupFirst(null, SourcePath.class);
        session = lookupProvider.lookupFirst(null, Session.class);
        debugger.addPropertyChangeListener (
            JPDADebugger.PROP_STATE,
            this
        );
    }
    
    @Override
    protected void destroy () {
        debugger.removePropertyChangeListener (
            JPDADebugger.PROP_STATE,
            this
        );
        DebuggerManager.getDebuggerManager ().removeDebuggerListener (
            DebuggerManager.PROP_BREAKPOINTS,
            this
        );
        removeBreakpointImpls ();
    }
    
    @Override
    public String[] getProperties () {
        return new String[] {"asd"};
    }

    @Override
    public void propertyChange (java.beans.PropertyChangeEvent evt) {
        if (debugger.getState () == JPDADebugger.STATE_RUNNING) {
            if (started) return;
            started = true;
            createBreakpointImpls ();
            DebuggerManager.getDebuggerManager ().addDebuggerListener (
                DebuggerManager.PROP_BREAKPOINTS,
                this
            );
        }
        if (debugger.getState () == JPDADebugger.STATE_DISCONNECTED) {
            removeBreakpointImpls ();
            started = false;
            DebuggerManager.getDebuggerManager ().removeDebuggerListener (
                DebuggerManager.PROP_BREAKPOINTS,
                this
            );
        }
    }
    
    @Override
    public void actionPerformed (Object action) {
//        if (action == ActionsManager.ACTION_FIX)
//            fixBreakpointImpls ();
    }

    @Override
    public void breakpointAdded (final Breakpoint breakpoint) {
        final boolean[] started = new boolean[] { false };
        if (!EventQueue.isDispatchThread() && debugger.accessLock.readLock().tryLock()) { // Was already locked or can be easily acquired
            try {
                createBreakpointImpl (breakpoint);
            } finally {
                debugger.accessLock.readLock().unlock();
            }
            return ;
        } // Otherwise:
        debugger.getRequestProcessor().post(new Runnable() {
            @Override
            public void run() {
                debugger.accessLock.readLock().lock();
                try {
                    synchronized (started) {
                        started[0] = true;
                        started.notify();
                    }
                    createBreakpointImpl (breakpoint);
                } finally {
                    debugger.accessLock.readLock().unlock();
                }
            }
        });
        if (!EventQueue.isDispatchThread()) { // AWT should not wait for debugger.LOCK
            synchronized (started) {
                if (!started[0]) {
                    try {
                        started.wait();
                    } catch (InterruptedException iex) {}
                }
            }
        }
    }    

    @Override
    public void breakpointRemoved (final Breakpoint breakpoint) {
        final boolean[] started = new boolean[] { false };
        if (!EventQueue.isDispatchThread() && debugger.accessLock.readLock().tryLock()) { // Was already locked or can be easily acquired
            try {
                removeBreakpointImpl (breakpoint);
            } finally {
                debugger.accessLock.readLock().unlock();
            }
            return ;
        } // Otherwise:
        debugger.getRequestProcessor().post(new Runnable() {
            @Override
            public void run() {
                debugger.accessLock.readLock().lock();
                try {
                    synchronized (started) {
                        started[0] = true;
                        started.notify();
                    }
                    removeBreakpointImpl (breakpoint);
                } finally {
                    debugger.accessLock.readLock().unlock();
                }
            }
        });
        if (!EventQueue.isDispatchThread()) { // AWT should not wait for debugger.LOCK
            synchronized (started) {
                if (!started[0]) {
                    try {
                        started.wait();
                    } catch (InterruptedException iex) {}
                }
            }
        }
    }
    

    @Override
    public Breakpoint[] initBreakpoints () {return new Breakpoint [0];}
    @Override
    public void initWatches () {}
    @Override
    public void sessionAdded (Session session) {}
    @Override
    public void sessionRemoved (Session session) {}
    @Override
    public void watchAdded (Watch watch) {}
    @Override
    public void watchRemoved (Watch watch) {}
    @Override
    public void engineAdded (DebuggerEngine engine) {}
    @Override
    public void engineRemoved (DebuggerEngine engine) {}


    // helper methods ..........................................................
    
    private final Map<Breakpoint, ComponentBreakpointImpl> breakpointToImpl = new IdentityHashMap<Breakpoint, ComponentBreakpointImpl>();
    private final ComponentBreakpointImpl preliminaryBreakpointImpl = new PreliminaryBreakpointImpl();
    
    private void createBreakpointImpls () {
        Breakpoint[] bs = DebuggerManager.getDebuggerManager ().getBreakpoints ();
        int i, k = bs.length;
        for (i = 0; i < k; i++) {
            createBreakpointImpl (bs [i]);
        }
    }
    
    private void removeBreakpointImpls () {
        Breakpoint[] bs = DebuggerManager.getDebuggerManager ().getBreakpoints ();
        int i, k = bs.length;
        for (i = 0; i < k; i++) {
            boolean removed = removeBreakpointImpl (bs [i]);
            /*
            if (removed && bs[i] instanceof JPDABreakpoint) {
                JPDABreakpoint jb = (JPDABreakpoint) bs[i];
                // TODO: JPDADebugger bDebugger = jb.getSession();
                JPDADebugger bDebugger;
                try {
                    java.lang.reflect.Method getSessionMethod = JPDABreakpoint.class.getDeclaredMethod("getSession");
                    getSessionMethod.setAccessible(true);
                    bDebugger = (JPDADebugger) getSessionMethod.invoke(jb);
                } catch (Exception ex) {
                    bDebugger = null;
                    Exceptions.printStackTrace(ex);
                }
                if (bDebugger != null && bDebugger.equals(debugger)) {
                    // A hidden breakpoint submitted just for this one session. Remove it with the end of the session.
                    DebuggerManager.getDebuggerManager ().removeBreakpoint(jb);
                }
            }
             */
        }
    }
    
    private void createBreakpointImpl (Breakpoint b) {
        if (!(b instanceof ComponentBreakpoint)) return ;
        ComponentBreakpoint ab = (ComponentBreakpoint) b;
        synchronized (breakpointToImpl) {
            if (breakpointToImpl.containsKey (b)) return;
            ObjectReference component = ab.getComponent().getComponent(debugger);
            if (component == null) {
                return;
            }
            breakpointToImpl.put(b, preliminaryBreakpointImpl);
        }
        ComponentBreakpointImpl bpi = (ab.getComponent().getComponentInfo() instanceof AWTComponentInfo) ?
                                        new AWTComponentBreakpointImpl(ab, debugger) :
                                        new FXComponentBreakpointImpl(ab, debugger);
        ComponentBreakpointImpl bpiToRemove = null;
        synchronized (breakpointToImpl) {
            if (bpi == null) {
                breakpointToImpl.remove(b);
            } else {
                if (!breakpointToImpl.containsKey(b)) {
                    // There already was a request to remove this
                    bpiToRemove = bpi;
                } else {
                    breakpointToImpl.put(b, bpi);
                }
            }
        }
        logger.finer("BreakpointsEngineListener: created impl "+bpi+" for "+b);
        if (bpiToRemove != null) {
            bpiToRemove.notifyRemoved();
            logger.finer("BreakpointsEngineListener: removed impl "+bpiToRemove);
        }
    }

    private boolean removeBreakpointImpl (Breakpoint b) {
        ComponentBreakpointImpl impl;
        synchronized (breakpointToImpl) {
            impl = breakpointToImpl.remove(b);
            if (impl == null) return false;
        }
        logger.finer("BreakpointsEngineListener: removed impl "+impl+" for "+b);
        impl.notifyRemoved ();
        return true;
    }

    private static final class PreliminaryBreakpointImpl extends ComponentBreakpointImpl {

        public PreliminaryBreakpointImpl() {
            super();
        }

    }
}
