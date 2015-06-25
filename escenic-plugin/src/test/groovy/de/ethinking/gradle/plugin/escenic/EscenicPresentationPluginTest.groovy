package de.ethinking.gradle.plugin.escenic
import static org.junit.Assert.*

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
                resourcesHost 'live','http://live-backend:8080/escenic-admin/'
            }
        }
        
        project.evaluate()

        assertNotNull(project.publication.resourcesBase)
        assertTrue(project.publication.resourcesBase.exists())
        assertNotNull(project.tasks.'resources-pub1-dev')
        assertNotNull(project.tasks.findByName('resources-pub2-live'))
        
    }
}
