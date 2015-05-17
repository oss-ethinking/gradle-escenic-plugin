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

import org.gradle.api.Plugin;
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import de.ethinking.gradle.extension.escenic.StudioPluginExtension


class EscenicStudioPlugin implements Plugin<Project> {

    static Logger LOG = Logging.getLogger(EscenicStudioPlugin.class)

    void apply(Project project) {
        project.configure(project) {
            project.extensions.create("studio",StudioPluginExtension,project)
            project.configurations{
                runtimeStudio{ transitive=false }
                pluginStudio
            }


            project.ext.studioPlugin  = { String pluginName ->
                FileCollection fileCollection
                if(escenic.getPluginPresentationDependencies().containsKey(pluginName)){
                    fileCollection= project.files(escenic.getPluginStudioDependencies().get(pluginName))
                }else{
                    fileCollection=   project.files()
                }
                if(LOG.isInfoEnabled()){
                    LOG.info("Studio Plugin:"+pluginName+" files:"+fileCollection.getFiles().size())
                }
                return fileCollection;
            }
            project.ext.studioCore = {
                FileCollection fileCollection=project.files(escenic.getStudioDependencies())
                if(LOG.isInfoEnabled()){
                    LOG.info("StudioCore  files:"+fileCollection.getFiles().size())
                }
                return fileCollection;
            }

            project.afterEvaluate {
                if(project.studio.includePlugin){
                    Project parent = project.rootProject
                    parent.allprojects { Project testingProject ->
                        Task collectStudioPluginsTask = testingProject.tasks.findByName("collectStudioPlugins")
                        if(collectStudioPluginsTask){
                            collectStudioPluginsTask.dependsOn project.build
                        }
                    }
                }
                project.rootProject.tasks.copyStudioPluginForAssembly.inputs.files project.configurations.runtimeStudio
                project.rootProject.tasks.copyStudioPluginForAssembly.inputs.source project.jar.outputs
                project.rootProject.tasks.copyStudioPluginForAssembly.inputs.property("includeProject:"+project.getPath(),project.studio.includePlugin)
                project.rootProject.tasks.runAssembly.inputs.property("includeProject:"+project.getPath(),project.studio.includePlugin)
            }
        }
    }
}

