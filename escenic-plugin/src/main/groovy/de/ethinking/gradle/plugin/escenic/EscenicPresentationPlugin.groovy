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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import de.ethinking.gradle.extension.escenic.PublicationExtension
import de.ethinking.gradle.extension.escenic.EscenicExtension
import de.ethinking.gradle.extension.escenic.ExtensionUtils
import de.ethinking.gradle.task.escenic.UploadResourcesTask

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
            if(project.publication.resourcesBase.exists()){
                List<String> publications = []
                if(project.publication.publications){
                    project.publication.publications.each{ String publication ->
                        publications.add(publication)
                    }
                }else{
                    publications.add(project.getName())
                }
                publications.each{ String resourcePublication ->
                    project.publication.resourcesHosts.each { String host, String uploadUrl ->
                        String taskName = "resources-"+resourcePublication+"-"+host
                        project.task(taskName,type:UploadResourcesTask){
                            publication = resourcePublication
                            resourcesBase = project.publication.resourcesBase
                            resourceUploadUrl= uploadUrl
                            ignoreFailure= project.publication.ignoreResourcesFailure
                            if (project.publication.resourcesHostsAuth.containsKey(host)) {
                                resourceUploadAuth = project.publication.resourcesHostsAuth.get(host)
                            }
                        }
                    }
                }
            }
        }
    }
}
