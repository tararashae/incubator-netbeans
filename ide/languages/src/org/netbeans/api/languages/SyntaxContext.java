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

package org.netbeans.api.languages;

import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.languages.ASTToken;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.modules.editor.NbEditorUtilities;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;


/**
 * Represents context for methods called from nbs files. This version of context
 * contains syntax information too.
 *
 * @author Jan Jancura
 */
public abstract class SyntaxContext extends Context { 

    
    /**
     * Returns current AST path.
     * 
     * @return current AST path
     */
    public abstract ASTPath getASTPath ();
    
    /**
     * Creates a new SyntaxContext.
     * 
     * @return a new SyntaxContext
     */
    public static SyntaxContext create (Document doc, ASTPath path) {
        return new CookieImpl (doc, path);
    }
    
    private static class CookieImpl extends SyntaxContext {
        
        private Document        doc;
        private ASTPath         path;
        private JTextComponent  component;
        
        CookieImpl (
            Document doc,
            ASTPath path
        ) {
            this.doc = doc;
            this.path = path;
        }
        
        public JTextComponent getJTextComponent () {
            if (component == null) {
                DataObject dob = NbEditorUtilities.getDataObject (doc);
                EditorCookie ec = dob.getLookup ().lookup (EditorCookie.class);
                if (ec.getOpenedPanes ().length > 0)
                    component = ec.getOpenedPanes () [0];
            }
            return component;
        }
        
        public ASTPath getASTPath () {
            return path;
        }
        
        public Document getDocument () {
            return doc;
        }

        public int getOffset() {
            return path.getLeaf ().getOffset ();
        }
    }
}


