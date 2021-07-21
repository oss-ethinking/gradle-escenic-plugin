/*
*  Copyright 2021 ethinking GmbH
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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.Project
import org.gradle.api.internal.TaskOutputsInternal
import org.gradle.api.internal.tasks.DefaultTaskOutputs

import org.gradle.api.internal.tasks.properties.*


import org.gradle.api.tasks.FileNormalizer;
import org.gradle.internal.fingerprint.DirectorySensitivity;
import org.gradle.internal.scan.UsedByScanPlugin;

import javax.annotation.Nullable;


import java.io.File
import javax.inject.Inject

class StudioPluginCollectorTask extends DefaultTask{
    
    @OutputDirectory
    File studioPluginsSourceDir
    
    
    @Inject
    StudioPluginCollectorTask(File studioDirectory){
        studioPluginsSourceDir=studioDirectory
    }
    
    


    @TaskAction
    def collectPlugins(){

      
        project.allprojects { Project subProject ->
            if(subProject.plugins.hasPlugin('de.ethinking.escenic.studio')&&subProject.studio.includePlugin){
                getLogger().info("copy studio plugin project:"+subProject.getName())
                project.copy{
                    from subProject.jar
                    from subProject.configurations.runtimeStudio
                    into project.file(studioPluginsSourceDir)
                }
            }
        }
        
    }
}
