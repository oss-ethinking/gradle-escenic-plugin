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
package de.ethinking.gradle.task.escenic

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import static org.junit.Assert.assertTrue

class MergeContentTypeTaskTest {

    @Test
    void canAddTaskToProject() {
        File emptyFile = new File(MergeContentTypeTaskTest.getResource('empty-content-type.xml').toURI())
        File resultFile = File.createTempFile('result', '.xml')
        resultFile.deleteOnExit()
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply('de.ethinking.escenic.presentation')
        Task task = project.task('mergeContentType', type: MergeContentTypeTask, {
            publication = 'news'
            baseFile = emptyFile
            fragments = project.files(emptyFile, emptyFile)
            outputFile = resultFile
        })
        assertTrue(task instanceof MergeContentTypeTask)
    }

}
