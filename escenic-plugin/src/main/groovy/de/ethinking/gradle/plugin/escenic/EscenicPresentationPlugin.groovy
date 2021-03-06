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

import de.ethinking.gradle.extension.escenic.PublicationExtension
import de.ethinking.gradle.extension.escenic.ResourceHost
import de.ethinking.gradle.task.escenic.UploadResourcesTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class EscenicPresentationPlugin implements Plugin<Project> {

    void apply(Project project) {
        project.configure(project) {

            project.extensions.create("publication",PublicationExtension,project)

            project.configurations{
                runtimeWebapp
                pluginWebapp
            }

            DependencyClosures.addEngineCoreDependencyClosure(project)
            DependencyClosures.addEnginePluginDependencyClosure(project)
            DependencyClosures.addPresentationCoreDependencyClosure(project)
            DependencyClosures.addPresentationPluginDependencyClosure(project)
        }
        project.afterEvaluate { applyPresentationTasks(project) }
    }

    def applyPresentationTasks(Project project){
        if(project.publication.provideResources && !project.publication.resourcesHosts.isEmpty()){

            project.task("preparePublicationResources"){}
            List<String> publications = []

            if(project.publication.publications){
                project.publication.publications.each{ String publicationName ->
                    publications.add(publicationName)
                }
            }else{
                publications.add(project.getName())
            }

            project.publication.resourcesHosts.each { String hostName, ResourceHost host ->
                Task resourcesHostTask=project.task("resourcesHost-"+hostName,group:"resources upload", description:"Upload all resources for host:"+hostName)

                if(host.isUsePublicationDirectories()){
                    String taskName = "resources-"+hostName
                    Task resourcesUploadTask = project.task(taskName,type:UploadResourcesTask,dependsOn:"preparePublicationResources",group:"resources upload",description:"Uploads resources for host "+hostName){
                        resourcesBase = project.publication.resourcesBase
                        ignoreFailure= project.publication.ignoreResourcesFailure
                        resourceHost = host
                    }
                    resourcesHostTask.dependsOn resourcesUploadTask
                }else{
                    publications.each{ String resourcePublication ->
                        String taskName = "resources-"+resourcePublication+"-"+hostName
                        Task resourcesUploadTask =  project.task(taskName,type:UploadResourcesTask,dependsOn:"preparePublicationResources",group:"resources upload",description:"Uploads resources for publication "+resourcePublication+" on host "+hostName){
                            publication = resourcePublication
                            resourcesBase = project.publication.resourcesBase
                            ignoreFailure= project.publication.ignoreResourcesFailure
                            resourceHost = host
                        }
                        resourcesHostTask.dependsOn resourcesUploadTask
                    }
                    
                }
            }
        }
    }
}
