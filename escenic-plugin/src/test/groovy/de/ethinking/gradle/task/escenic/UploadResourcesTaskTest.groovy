package de.ethinking.gradle.task.escenic

import de.ethinking.gradle.extension.escenic.ResourceHost
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertTrue


class UploadResourcesTaskTest {


    @Test
    void canAddTaskToProject() {
        File baseDirectory = new File('/gradle-plugin-test/')
        ResourceHost host = new ResourceHost()
        host.url("")
        host.password("")
        host.resourceBase(baseDirectory)

        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply('de.ethinking.escenic.presentation')
        Task task = project.task('uploadResources', type: UploadResourcesTask, {
            publication = 'news'
            resourceHost = host
            resourcesBase = baseDirectory
        })
        assertTrue(task instanceof UploadResourcesTask)
    }
}
