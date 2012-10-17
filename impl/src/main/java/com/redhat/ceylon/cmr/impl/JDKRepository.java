/*
 * Copyright 2012 Red Hat inc. and third party contributors as noted 
 * by the author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.ceylon.cmr.impl;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.redhat.ceylon.cmr.api.ArtifactResult;
import com.redhat.ceylon.cmr.api.ContentFinder;
import com.redhat.ceylon.cmr.api.ModuleQuery;
import com.redhat.ceylon.cmr.api.ModuleSearchResult;
import com.redhat.ceylon.cmr.api.ModuleVersionDetails;
import com.redhat.ceylon.cmr.api.ModuleVersionQuery;
import com.redhat.ceylon.cmr.api.ModuleVersionResult;
import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.cmr.spi.Node;

/**
 * Repository that provides ContentFinder implementation for JDK modules
 *
 * @author Stéphane Épardaud <stef@epardaud.fr>
 */
public class JDKRepository extends AbstractRepository {

    public static final String JDK_VERSION = "7";

    private static final SortedSet<String> FixedVersionSet = new TreeSet<String>(){{
        add(JDK_VERSION);
    }};
    private static final SortedSet<String> EmptySet = new TreeSet<String>();

    public static final Set<String> JDK_MODULES = new TreeSet<String>();
    static{
        JDK_MODULES.add("jdk.base");
        JDK_MODULES.add("jdk.logging");
        JDK_MODULES.add("jdk.management");
        JDK_MODULES.add("jdk.instrument");
        JDK_MODULES.add("jdk.rmi");
        JDK_MODULES.add("jdk.prefs");
        JDK_MODULES.add("jdk.tls");
        JDK_MODULES.add("jdk.kerberos");
        JDK_MODULES.add("jdk.auth");
        JDK_MODULES.add("jdk.xmldsig");
        JDK_MODULES.add("jdk.security.acl");
        JDK_MODULES.add("jdk.jndi");
        JDK_MODULES.add("jdk.jta");
        JDK_MODULES.add("jdk.jdbc");
        JDK_MODULES.add("jdk.jdbc.rowset");
        JDK_MODULES.add("jdk.scripting");
        JDK_MODULES.add("jdk.jaxp");
        JDK_MODULES.add("jdk.jaxws");
        JDK_MODULES.add("jdk.jx.annotations");
        JDK_MODULES.add("jdk.corba");
        JDK_MODULES.add("jdk.desktop");
        JDK_MODULES.add("jdk.compiler");
        JDK_MODULES.add("oracle.jdk.base");
        JDK_MODULES.add("oracle.sun.charsets");
        JDK_MODULES.add("oracle.jdk.logging");
        JDK_MODULES.add("oracle.jdk.management.iiop");
        JDK_MODULES.add("oracle.jdk.management");
        JDK_MODULES.add("oracle.jdk.tools.jre");
        JDK_MODULES.add("oracle.jdk.instrument");
        JDK_MODULES.add("oracle.jdk.rmi");
        JDK_MODULES.add("oracle.jdk.auth");
        JDK_MODULES.add("oracle.jdk.xmldsig");
        JDK_MODULES.add("oracle.jdk.smartcardio");
        JDK_MODULES.add("oracle.jdk.security.acl");
        JDK_MODULES.add("oracle.jdk.jndi");
        JDK_MODULES.add("oracle.jdk.cosnaming");
        JDK_MODULES.add("oracle.jdk.jdbc.rowset");
        JDK_MODULES.add("oracle.jdk.scripting");
        JDK_MODULES.add("oracle.jdk.httpserver");
        JDK_MODULES.add("oracle.jdk.sctp");
        JDK_MODULES.add("oracle.jdk.desktop");
        JDK_MODULES.add("oracle.jdk.jaxp");
        JDK_MODULES.add("oracle.jdk.tools.jaxws");
        JDK_MODULES.add("oracle.jdk.jaxws");
        JDK_MODULES.add("oracle.jdk.corba");
        JDK_MODULES.add("oracle.jdk.deploy");
        JDK_MODULES.add("oracle.jdk.compat");
        JDK_MODULES.add("oracle.jdk.tools.base");
    }

    public JDKRepository() {
        super(new JDKRoot());
    }

    @Override
    protected ArtifactResult getArtifactResultInternal(RepositoryManager manager, Node node) {
        return null;
    }
    
    public static class JDKRoot extends DefaultNode implements ContentFinder {

        public JDKRoot() {
            addService(ContentFinder.class, this);
        }

        @Override
        public String getDisplayString() {
            return "JDK modules repository";
        }
        
        @Override
        public void completeModules(ModuleQuery query, ModuleSearchResult result) {
            // abort if not JVM
            if(query.getType() != ModuleQuery.Type.JVM)
                return;
            String name = query.getName();
            if(name == null)
                name = "";
            for(String module : JDK_MODULES){
                if(module.startsWith(name))
                    result.addResult(module, doc(module), null, EmptySet, FixedVersionSet);
            }
        }

        private String doc(String module) {
            return "JDK module " + module;
        }

        @Override
        public void completeVersions(ModuleVersionQuery query, ModuleVersionResult result) {
            // abort if not JVM
            if(query.getType() != ModuleQuery.Type.JVM)
                return;
            if(query.getName() == null || !JDK_MODULES.contains(query.getName()))
                return;
            if(query.getVersion() != null && !query.getVersion().equals(JDK_VERSION))
                return;
            final ModuleVersionDetails newVersion = result.addVersion(JDK_VERSION);
            newVersion.setDoc(doc(query.getName()));
            newVersion.setVersion(JDK_VERSION);
        }

        @Override
        public void searchModules(ModuleQuery query, ModuleSearchResult result) {
            // abort if not JVM
            if(query.getType() != ModuleQuery.Type.JVM)
                return;
            String name = query.getName();
            if(name == null)
                name = "";
            name = name.toLowerCase();
            boolean stopSearching = false;
            int found = 0;
            for(String module : JDK_MODULES){
                // does it match?
                if(module.contains(name)){
                    // check if we were already done but were checking for a next results
                    if (stopSearching) {
                        // we already found enough results but were checking if there
                        // were more results to be found for paging, so record that
                        // and stop
                        result.setHasMoreResults(true);
                        return;
                    }
                    if (query.getStart() == null || found++ >= query.getStart()) {
                        // are we interested in this result or did we need to skip it?
                        result.addResult(module, doc(module), null, EmptySet, FixedVersionSet);
                        // stop if we're done searching
                        if (query.getStart() != null
                                && query.getCount() != null
                                && found >= query.getStart() + query.getCount()) {
                            // we're done, but we want to see if there's at least one more result
                            // to be found so we can tell clients there's a next page
                            stopSearching = true;
                        }
                    }
                }
            }
        }
    }

}