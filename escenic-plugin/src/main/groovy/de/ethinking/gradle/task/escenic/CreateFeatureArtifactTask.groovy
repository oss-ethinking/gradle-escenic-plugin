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
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CreateFeatureArtifactTask extends DefaultTask {



	@TaskAction
	def createFeatureArtifactLayout() {

		String featureBaseName;
		Set<String> subProjects = new HashSet<String>()
		Set<String> studioProjects = new HashSet<String>()
		Set<String> engineProjects = new HashSet<String>()
		Set<String> presentationProjects = new HashSet<String>()

		featureBaseName =  System.getProperty("feature")
		def value  = System.getProperty("engine")
		addToSet(value, subProjects)
		addToSet(value, engineProjects)
		value  = System.getProperty("presentation")
		addToSet(value, subProjects)
		addToSet(value, presentationProjects)
		value  =System.getProperty("studio")
		addToSet(value, subProjects)
		addToSet(value, studioProjects)
		value  = System.getProperty("project")
		addToSet(value, subProjects)

		File baseDirectory = new File(featureBaseName)
		project.mkdir(baseDirectory)

		File settingFile = project.file(new File("settings.gradle"))
		boolean addSpacerInSettings=true
		for(String subProject:subProjects){
			File subprojectDirectory = new File(baseDirectory,subProject)
			createFolderLayout(subprojectDirectory, project)
			File buildFile = project.file(new File(subprojectDirectory,"build.gradle"))
			if(!buildFile.exists()){
				println "Create build.gradle for:"+subProject
				if(engineProjects.contains(subProject)){
					buildFile << "apply plugin: 'de.ethinking.escenic.engine'\n"
				}
				if(presentationProjects.contains(subProject)){
					buildFile << "apply plugin: 'de.ethinking.escenic.presentation'\n"
				}
				if(studioProjects.contains(subProject)){
					buildFile << "apply plugin: 'de.ethinking.escenic.studio'\n"
				}
				buildFile << "\n\n"

				buildFile << "dependencies{\n"
				if(engineProjects.contains(subProject)){
					buildFile << "\tcompile  engineCore()\n"
				}
				if(presentationProjects.contains(subProject)){
					buildFile << "\tcompile  presentationCore()\n"
				}
				if(studioProjects.contains(subProject)){
					buildFile << "\tcompile  studioCore()\n"
				}
				buildFile << "}\n"
				if(addSpacerInSettings){
					settingFile.append("\n")
					addSpacerInSettings=false
				}
				settingFile.append("include '"+featureBaseName+":"+subProject+"'\n")
			}
		}
	}


	public void addToSet(String params,Set<String> set){
		if(params){
			String[] paramArray = params.split(",")
			for(String param:paramArray){
				if(param && param.trim().length()>0){
					set.add(param)
				}
			}
		}
	}

	public void createFolderLayout(File directory,Project project){
		project.mkdir(directory)
		project.mkdir(new File(directory,"src/main/java"))
		project.mkdir(new File(directory,"src/main/resources"))
		project.mkdir(new File(directory,"src/test/java"))
		project.mkdir(new File(directory,"src/test/resources"))
	}
}
