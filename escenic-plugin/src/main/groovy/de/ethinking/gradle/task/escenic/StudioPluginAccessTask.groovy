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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory

class StudioPluginAccessTask extends DefaultTask{
    
    @InputDirectory
    def File studioPluginsSourceDir
    
    @OutputDirectory
	def File studioPluginsLibDir

    @InputDirectory
    def File pluginsBasePath
    
    @TaskAction
    def copyStudioPlugins(){
        
        project.delete(studioPluginsLibDir)

        project.copy{
            from studioPluginsSourceDir
            into studioPluginsLibDir
        }
    }
}
