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
package de.ethinking.gradle.plugin.escenic

import de.ethinking.gradle.task.escenic.UploadResourcesTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.*

class EscenicPresentationPluginTest {


    @Test
    public void testPublicationExtension() {
        Project project = ProjectBuilder.builder().build()
        project.with{
            apply plugin: 'de.ethinking.escenic.presentation'

            publication{
                publications "pub1","pub2"
                resourcesBase  project.file(".")
                resourcesHost 'dev','http://localhost:8080/escenic-admin/publication-resources/'
                resourcesHost 'live','http://live-backend:8080/escenic-admin/publication-resources/','test','test'
                resourcesHost 'stage',{ 
                           url 'http://stage-backend:8080/escenic-admin/publication-resources/'
                           resourceBase project.file("build/stage")
                           user "admin"
                           password "stage"
                               
                }
                resourcesHost 'test',{
                    url 'http://test-backend:8080/escenic-admin/publication-resources/'
                    resourceBase project.file("build/test")
                    usePublicationDirectories true                        
               }
            }
        }

        project.evaluate()

        Task prepareTask = project.tasks.'preparePublicationResources'
        
        assertNotNull(project.publication.resourcesBase)
        assertTrue(project.publication.resourcesBase.exists())
        assertNotNull(prepareTask)
        assertNotNull(project.tasks.'resources-pub1-dev')
        
        
        UploadResourcesTask task = project.tasks.findByName('resources-pub2-live')
        assertNotNull(task)
        assertEquals("test",task.resourceHost.user)
        assertEquals("test",task.resourceHost.password)
        assertEquals("http://live-backend:8080/escenic-admin/publication-resources/",task.resourceHost.url)
        
        assertTrue(task.taskDependencies.getDependencies().contains(prepareTask))

        task = project.tasks.findByName('resources-pub2-stage')
        assertNotNull(task)
        assertEquals("admin",task.resourceHost.user)
        assertEquals("stage",task.resourceHost.password)
        assertEquals("http://stage-backend:8080/escenic-admin/publication-resources/",task.resourceHost.url)
        assertEquals(project.file("build/stage"),task.findSourceDirectory())
       
        assertNotNull(project.tasks.'resources-test')
        assertTrue(project.tasks.'resources-test'.taskDependencies.getDependencies().contains(prepareTask))
        
    }
}
