/*
 * Copyright 2011 Red Hat inc. and third party contributors as noted 
 * by the author tags.
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

package com.redhat.ceylon.test.smoke.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import com.redhat.ceylon.cmr.api.ModuleDependencyInfo;
import com.redhat.ceylon.cmr.api.ModuleQuery;
import com.redhat.ceylon.cmr.api.ModuleQuery.Retrieval;
import com.redhat.ceylon.cmr.api.ModuleQuery.Type;
import com.redhat.ceylon.cmr.api.ModuleSearchResult;
import com.redhat.ceylon.cmr.api.ModuleSearchResult.ModuleDetails;
import com.redhat.ceylon.cmr.api.ModuleVersionArtifact;
import com.redhat.ceylon.cmr.api.ModuleVersionDetails;
import com.redhat.ceylon.cmr.api.ModuleVersionQuery;
import com.redhat.ceylon.cmr.api.ModuleVersionResult;
import com.redhat.ceylon.cmr.api.RepositoryManager;
import com.redhat.ceylon.cmr.api.RepositoryManagerBuilder;
import com.redhat.ceylon.cmr.impl.CMRJULLogger;
import com.redhat.ceylon.cmr.impl.DefaultRepository;
import com.redhat.ceylon.cmr.impl.FileContentStore;
import com.redhat.ceylon.common.FileUtil;
import com.redhat.ceylon.common.log.Logger;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class AbstractTest {

    protected Logger log = new CMRJULLogger();

    private Path temp;

    @Before
    public void setUp() throws Exception {
        temp = Files.createTempDirectory("temp-");
    }

    @After
    public void tearDown() throws Exception {
        Files.walkFileTree(temp, new FileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        FileUtil.deleteQuietly(temp.toFile());
    }

    protected File getRepositoryRoot() throws URISyntaxException {
        URL url = getClass().getResource("/repo");
        Assert.assertNotNull("RepositoryManager root '/repo' not found", url);
        return new File(url.toURI());
    }

    protected File getFolders() throws URISyntaxException {
        URL url = getClass().getResource("/folders");
        Assert.assertNotNull("RepositoryManager folder '/folders' not found", url);
        return new File(url.toURI());
    }

    protected RepositoryManagerBuilder getRepositoryManagerBuilder(boolean offline, int timeout) throws Exception {
        return getRepositoryManagerBuilder(getRepositoryRoot(), offline, timeout);
    }

    protected RepositoryManagerBuilder getRepositoryManagerBuilder(File root, boolean offline, int timeout) throws Exception {
        RepositoryManagerBuilder builder = new RepositoryManagerBuilder(temp.toFile(), log, offline, timeout);
        builder.addRepository(new DefaultRepository(new FileContentStore(root).createRoot()));
        return builder;
    }

    protected RepositoryManager getRepositoryManager(boolean offline, int timeout) throws Exception {
        RepositoryManagerBuilder builder = getRepositoryManagerBuilder(offline, timeout);
        return builder.buildRepository();
    }

    protected RepositoryManager getRepositoryManager() throws Exception {
        return getRepositoryManager(false, 20000);
    }

    protected void testComplete(String query, ModuleDetails[] expected, RepositoryManager manager) {
        testComplete(query, expected, manager, ModuleQuery.Type.JVM);
    }

    protected void testComplete(String query, ModuleDetails[] expected, RepositoryManager manager, ModuleQuery.Type type) {
        testComplete(query, expected, manager, type, null, null);
    }

    protected void testComplete(String query, ModuleDetails[] expected, RepositoryManager manager,
                                ModuleQuery.Type type, Integer binaryMajor, Integer binaryMinor) {
        ModuleQuery lookup = new ModuleQuery(query, type);
        lookup.setBinaryMajor(binaryMajor);
        lookup.setBinaryMinor(binaryMinor);
        ModuleSearchResult result = manager.completeModules(lookup);
        compareSearchResults(expected, result);
    }

    protected void testListVersions(String query, String versionQuery, ModuleVersionDetails[] expected) throws Exception {
        RepositoryManager manager = getRepositoryManager();
        testListVersions(query, versionQuery, expected, manager);
    }

    protected void testListVersions(String query, String versionQuery, ModuleVersionDetails[] expected,
                                    RepositoryManager manager) throws Exception {
        testListVersions(query, versionQuery, expected, manager, null, null);
    }

    protected void testListVersions(String query, String versionQuery, ModuleVersionDetails[] expected,
                                    RepositoryManager manager, Integer binaryMajor, Integer binaryMinor) throws Exception {
        ModuleVersionQuery lookup = new ModuleVersionQuery(query, versionQuery, ModuleQuery.Type.JVM);
        lookup.setBinaryMajor(binaryMajor);
        lookup.setBinaryMinor(binaryMinor);
        ModuleVersionResult result = manager.completeVersions(lookup);
        int i = 0;
        Assert.assertEquals(expected.length, result.getVersions().size());
        for (Entry<String, ModuleVersionDetails> entry : result.getVersions().entrySet()) {
            ModuleVersionDetails expectedVersion = expected[i++];
            ModuleVersionDetails version = entry.getValue();
            Assert.assertEquals(expectedVersion.getVersion(), entry.getKey());
            Assert.assertEquals(expectedVersion.getVersion(), version.getVersion());
            if (expectedVersion.getDoc() != null) {
                // Docs can be really big, this let's allow us to ignore them for comparisons
                Assert.assertEquals(expectedVersion.getDoc(), version.getDoc());
            }
            Assert.assertEquals(expectedVersion.getLicense(), version.getLicense());
            Assert.assertEquals(expectedVersion.getAuthors(), version.getAuthors());
            Assert.assertEquals(expectedVersion.getDependencies(), version.getDependencies());
            Assert.assertEquals(expectedVersion.getArtifactTypes(), version.getArtifactTypes());
            Assert.assertEquals(expectedVersion.isRemote(), version.isRemote());
            Assert.assertEquals(expectedVersion.getOrigin(), version.getOrigin());
        }
    }

    protected ModuleSearchResult testSearchResults(String q, Type type, ModuleDetails[] expected) throws Exception {
        return testSearchResults(q, type, expected, null, null);
    }

    protected ModuleSearchResult testSearchResults(String q, Type type, Retrieval retrieval, ModuleDetails[] expected) throws Exception {
        RepositoryManager manager = getRepositoryManager();
        return testSearchResults(q, type, retrieval, expected, null, null, manager, null, null, null);
    }

    protected ModuleSearchResult testSearchResults(String q, Type type, ModuleDetails[] expected, RepositoryManager manager) throws Exception {
        return testSearchResults(q, type, expected, null, null, manager);
    }

    protected ModuleSearchResult testSearchResults(String q, Type type, ModuleDetails[] expected, Long start, Long count) throws Exception {
        RepositoryManager manager = getRepositoryManager();
        return testSearchResults(q, type, expected, start, count, manager);
    }

    protected ModuleSearchResult testSearchResults(String q, Type type, ModuleDetails[] expected,
                                                   Long start, Long count, RepositoryManager manager) throws Exception {
        return testSearchResults(q, type, expected, start, count, manager, null);
    }

    protected ModuleSearchResult testSearchResults(String q, Type type, ModuleDetails[] expected,
                                                   Long start, Long count, RepositoryManager manager, long[] pagingInfo) throws Exception {
        return testSearchResults(q, type, expected, start, count, manager, pagingInfo, null, null);
    }

    protected ModuleSearchResult testSearchResults(String q, Type type, ModuleDetails[] expected,
            Long start, Long count, RepositoryManager manager, long[] pagingInfo,
            Integer binaryMajor, Integer binaryMinor) throws Exception {
        return testSearchResults(q, type, Retrieval.ANY, expected, start, count, manager, pagingInfo, binaryMajor, binaryMinor);
    }
    
    protected ModuleSearchResult testSearchResults(String q, Type type, Retrieval retrieval, ModuleDetails[] expected,
                                                   Long start, Long count, RepositoryManager manager, long[] pagingInfo,
                                                   Integer binaryMajor, Integer binaryMinor) throws Exception {

        ModuleQuery query = new ModuleQuery(q, type, retrieval);
        query.setStart(start);
        query.setCount(count);
        query.setPagingInfo(pagingInfo);
        query.setBinaryMajor(binaryMajor);
        query.setBinaryMinor(binaryMinor);
        ModuleSearchResult results = manager.searchModules(query);

        compareSearchResults(expected, results);
        return results;
    }

    protected ModuleSearchResult testSearchResultsMember(String q, Type type, String members, boolean memberSearchExact, boolean memberSearchPackageOnly, ModuleDetails[] expected) throws Exception {
        ModuleQuery query = new ModuleQuery(q, type, Retrieval.ANY);
        query.setMemberName(members);
        query.setMemberSearchExact(memberSearchExact);
        query.setMemberSearchPackageOnly(memberSearchPackageOnly);
        RepositoryManager manager = getRepositoryManager();
        ModuleSearchResult results = manager.searchModules(query);

        compareSearchResults(expected, results);
        return results;
    }

    private void compareSearchResults(ModuleDetails[] expected, ModuleSearchResult results) {
        int i = 0;
        Collection<ModuleDetails> resultsList = results.getResults();
        Assert.assertEquals(expected.length, resultsList.size());
        for (ModuleDetails result : resultsList) {
            ModuleDetails expectedResult = expected[i++];
            System.err.println("Testing " + result.getName());
            Assert.assertEquals(expectedResult.getName(), result.getName());
            if (expectedResult.getDoc() != null) {
                // Docs can be really big, this let's allow us to ignore them for comparisons
                Assert.assertEquals(expectedResult.getDoc(), result.getDoc());
            }
            Assert.assertEquals(expectedResult.getLicense(), result.getLicense());
            Assert.assertEquals(expectedResult.getAuthors(), result.getAuthors());
            Assert.assertEquals(expectedResult.getVersions(), result.getVersions());
            Assert.assertEquals(expectedResult.getDependencies(), result.getDependencies());
            Assert.assertEquals(expectedResult.getArtifactTypes(), result.getArtifactTypes());
        }
    }

    protected static SortedSet<String> set(String... values) {
        SortedSet<String> ret = new TreeSet<>();
        Collections.addAll(ret, values);
        return ret;
    }

    protected static SortedSet<ModuleDependencyInfo> deps(ModuleDependencyInfo... values) {
        SortedSet<ModuleDependencyInfo> ret = new TreeSet<>();
        Collections.addAll(ret, values);
        return ret;
    }

    protected static SortedSet<ModuleVersionArtifact> types(ModuleVersionArtifact... values) {
        SortedSet<ModuleVersionArtifact> ret = new TreeSet<>();
        Collections.addAll(ret, values);
        return ret;
    }

    protected static ModuleVersionArtifact art(String suffix) {
        return art(suffix, null, null);
    }

    protected static ModuleVersionArtifact art(String suffix, Integer majorBinaryVersion, Integer minorBinaryVersion) {
        return new ModuleVersionArtifact(suffix, majorBinaryVersion, minorBinaryVersion);
    }

    protected static Manifest mockManifest(String version) {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, version);
        return manifest;
    }

    protected static InputStream mockJar(String entryName, byte[] entryContent) throws IOException {
        return mockJar(entryName, entryContent, null);
    }

    protected static InputStream mockJar(String entryName, byte[] entryContent, Manifest manifest) throws IOException {
        return mockJar(new String[]{entryName}, Collections.singletonList(entryContent), manifest);
    }

    protected static InputStream mockJar(String[] entryNames, List<byte[]> entryContents) throws IOException {
        return mockJar(entryNames, entryContents, null);
    }

    protected static InputStream mockJar(String[] entryNames, List<byte[]> entryContents, Manifest manifest) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JarOutputStream output = (manifest != null) ? new JarOutputStream(baos, manifest) : new JarOutputStream(baos)) {
            for (int i = 0; i < entryNames.length; i++) {
                String entryName = entryNames[i];
                JarEntry jarEntry = new JarEntry(entryName);
                output.putNextEntry(jarEntry);
                output.write(entryContents.get(i));
                output.closeEntry();
            }
        }
        return new ByteArrayInputStream(baos.toByteArray());
    }
}
