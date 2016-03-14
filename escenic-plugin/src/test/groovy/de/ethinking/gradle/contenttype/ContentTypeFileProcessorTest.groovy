/*
 *  Copyright 2015 eThinking GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.ethinking.gradle.contenttype

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.junit.Before
import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder

import static org.junit.Assert.assertEquals

/**
 * Test of the {@link ContentTypeFileProcessor} class.
 */
class ContentTypeFileProcessorTest {

    ContentTypeFileProcessor instance;

    @Before
    void setUp() throws Exception {
        instance = new ContentTypeFileProcessor()
    }

    @Test
    void testMergeOne() throws Exception {
        publication('one', 'expected-one.xml')
    }

    @Test
    void testMergeTwo() throws Exception {
        publication('two', 'expected-two.xml')
    }

    @Test
    void testMergeThree() throws Exception {
        publication('three', 'expected-three.xml')
    }

    /**
     * Take base, common and widget XMLs and create a content-type.xml for a specified publication.
     * Check that the result fits the expected file.
     * @param publicationName name of the publication, used in the in-publication or not-in-publication attributes
     * @param expectedResourceName resource containing the expected result
     */
    private void publication(String publicationName, String expectedResourceName) {
        String expected = new Scanner(ContentTypeFileProcessorTest.getResourceAsStream(expectedResourceName), 'UTF-8').useDelimiter('\\A').next()
        Project project = ProjectBuilder.builder().build()
        FileCollection injectingPaths=project.files(load('widget-common.xml'), load('widgetA.xml'))
        String result = instance.createFileForPublication(publicationName, load('base.xml'), injectingPaths)
        assertEquals expected.replaceAll("\\n","").replaceAll("\\r",""), result.replaceAll("\\n","").replaceAll("\\r","")
    }

    /**
     * @param resourceName resource name
     * @return URI where the resource is
     */
    private static File load(String resourceName) {
       return new File(ContentTypeFileProcessorTest.getResource(resourceName).toURI())
    }
}
