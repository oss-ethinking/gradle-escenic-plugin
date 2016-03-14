package de.ethinking.gradle.plugin.escenic
import static org.junit.Assert.*

import de.ethinking.gradle.task.escenic.UploadResourcesTask
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class EscenicPresentationPluginTest {


    @Test
    public void testPublicationExtension() {
        Project project = ProjectBuilder.builder().build()
        project.with{
            apply plugin: 'de.ethinking.escenic.presentation'

            publication{
                publications "pub1","pub2"
                resourcesBase  project.file(".")
                resourcesHost 'dev','http://localhost:8080/escenic-admin/'
                resourcesHost 'live','http://live-backend:8080/escenic-admin/','test','test'
                resourcesHost 'stage',{ 
                           url 'http://stage-backend:8080/escenic-admin/'
                           resourceBase project.file("build/stage")
                           user "stage"
                           password "stage"
                               
                }
            }
        }

        project.evaluate()

        assertNotNull(project.publication.resourcesBase)
        assertTrue(project.publication.resourcesBase.exists())
        assertNotNull(project.tasks.'resources-pub1-dev')
        UploadResourcesTask task = project.tasks.findByName('resources-pub2-live')
        assertNotNull(task)
        assertEquals("test",task.resourceHost.user)
        assertEquals("test",task.resourceHost.password)
        assertEquals("http://live-backend:8080/escenic-admin/",task.resourceHost.url)
        task = project.tasks.findByName('resources-pub2-stage')
        assertNotNull(task)
        assertEquals("stage",task.resourceHost.user)
        assertEquals("stage",task.resourceHost.password)
        assertEquals("http://stage-backend:8080/escenic-admin/",task.resourceHost.url)
        assertEquals(project.file("build/stage"),task.findSourceDirectory())
    }
}
