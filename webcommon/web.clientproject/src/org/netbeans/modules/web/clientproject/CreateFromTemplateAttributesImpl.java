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
package org.netbeans.modules.web.clientproject;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.templates.CreateDescriptor;
import org.netbeans.api.templates.CreateFromTemplateAttributes;
import org.netbeans.modules.web.clientproject.env.CommonProjectHelper;
import org.netbeans.modules.web.clientproject.env.Licenses;
import org.netbeans.modules.web.common.spi.ProjectWebRootQuery;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.EditableProperties;
import org.openide.util.Utilities;

/**
 * Default implementation of {@link CreateFromTemplateAttributes}.
 */
class CreateFromTemplateAttributesImpl implements CreateFromTemplateAttributes {

    private final CommonProjectHelper helper;
    private final FileEncodingQueryImplementation encodingQuery;
    private static final Logger LOG = Logger.getLogger(CreateFromTemplateAttributesImpl.class.getName());


    public CreateFromTemplateAttributesImpl(CommonProjectHelper helper, FileEncodingQueryImplementation encodingQuery) {
        super();
        this.helper = helper;
        this.encodingQuery = encodingQuery;
    }

    @Override
    public Map<String, ?> attributesFor(CreateDescriptor desc) {
        Map<String, String> values = new HashMap<>();
        EditableProperties priv  = helper.getProperties(CommonProjectHelper.PRIVATE_PROPERTIES_PATH);
        EditableProperties props = helper.getProperties(CommonProjectHelper.PROJECT_PROPERTIES_PATH);
        String licensePath = priv.getProperty(Licenses.LICENSE_PATH);
        if (licensePath == null) {
            licensePath = props.getProperty(Licenses.LICENSE_PATH);
        }
        if (licensePath != null) {
            licensePath = helper.getStandardPropertyEvaluator().evaluate(licensePath);
            if (licensePath != null) {
                File path = FileUtil.normalizeFile(helper.resolveFile(licensePath));
                if (path.exists() && path.isAbsolute()) { //is this necessary? should prevent failed license header inclusion
                    URI uri = Utilities.toURI(path);
                    licensePath = uri.toString();
                    values.put("licensePath", licensePath);
                } else {
                    LOG.log(Level.INFO, "project.licensePath value not accepted - " + licensePath);
                }
            }
        }
        String license = priv.getProperty(Licenses.LICENSE_NAME);
        if (license == null) {
            license = props.getProperty(Licenses.LICENSE_NAME);
        }
        if (license != null) {
            values.put("license", license); // NOI18N
        }
        Charset charset = encodingQuery.getEncoding(desc.getTarget());
        String encoding = (charset != null) ? charset.name() : null;
        if (encoding != null) {
            values.put("encoding", encoding); // NOI18N
        }
        try {
            Project prj = ProjectManager.getDefault().findProject(helper.getProjectDirectory());
            assert prj != null : helper.getProjectDirectory();
            ProjectInformation info = ProjectUtils.getInformation(prj);
            if (info != null) {
                String pname = info.getName();
                if (pname != null) {
                    values.put("name", pname);// NOI18N
                }
                String pdname = info.getDisplayName();
                if (pdname != null) {
                    values.put("displayName", pdname);// NOI18N
                }
            }
            values.put("webRootPath", getWebRootPath(prj)); // NOI18N
        } catch (Exception ex) {
            //not really important, just log.
            Logger.getLogger(CreateFromTemplateAttributesImpl.class.getName()).log(Level.FINE, "", ex);
        }

        if (values.isEmpty()) {
            return null;
        }
        return Collections.singletonMap("project", values); // NOI18N
    }

    @CheckForNull
    private static String getWebRootPath(Project project) {
        for (FileObject webRoot : ProjectWebRootQuery.getWebRoots(project)) {
            return FileUtil.getRelativePath(project.getProjectDirectory(), webRoot);
        }
        return null;
    }

}
