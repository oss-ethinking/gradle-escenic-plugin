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
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger;

import de.ethinking.gradle.extension.escenic.EscenicExtension
import de.ethinking.gradle.extension.escenic.ExtensionUtils
import de.ethinking.gradle.repository.EscenicEngineModel

import org.gradle.api.Project
import org.gradle.api.logging.Logging

class DependencyClosures {

	static Logger LOG = Logging.getLogger(DependencyClosures.class)

	public static void addEngineCoreDependencyClosure(Project project){
		project.ext.engineCore  =  {
			EscenicExtension escenic =   ExtensionUtils.findEscenicExtension(project)
			FileCollection fileCollection=null;
			int count=0
			if(escenic != null){
				List<File> files = escenic.getEngineDependencies()
				count=files.size()
				fileCollection =project.files(files)
			}else{
				println "No escenic engine found -> configure your dependencies"
				fileCollection = project.files()
			}
			if(LOG.isInfoEnabled()){
				LOG.info("Adding "+count+" jars from engine as dependencies for project:"+project.getName())
			}
			return fileCollection;
		}
		project.ext.engineAPI  =  {
			EscenicExtension escenic =   ExtensionUtils.findEscenicExtension(project)
			FileCollection fileCollection=null;
			int count=0
			if(escenic != null){
				List<File> files = escenic.getEngineAPIDependencies()
				count=files.size()
				fileCollection =project.files(files)
			}else{
				println "No escenic engine found -> configure your dependencies"
				fileCollection = project.files()
			}
			if(LOG.isInfoEnabled()){
				LOG.info("Adding "+count+" jars from engine api as dependencies for project:"+project.getName())
			}
			return fileCollection;
		}
		
		
	}

	public static void addEnginePluginDependencyClosure(Project project){
		project.ext.enginePlugin = { String pluginName ->
			FileCollection fileCollection
			EscenicExtension escenic =   ExtensionUtils.findEscenicExtension(project)
			if(escenic != null && escenic.getPluginEngineDependencies().containsKey(pluginName)){
				fileCollection= project.files(escenic.getPluginEngineDependencies().get(pluginName))
			}else{
				fileCollection=   project.files()
			}

			//println "Plugin:"+pluginName+" files:"+fileCollection.getFiles().size()
			return fileCollection;
		}
	}


	public static void addPresentationPluginDependencyClosure(Project project){
		project.ext.presentationPlugin  = { String pluginName ->
			FileCollection fileCollection
			EscenicExtension escenic =   ExtensionUtils.findEscenicExtension(project)
			if(escenic != null && escenic.getPluginPresentationDependencies().containsKey(pluginName)){
				fileCollection= project.files(escenic.getPluginPresentationDependencies().get(pluginName))
			}else{
				fileCollection=   project.files()
			}
			//println "Plugin:"+pluginName+" files:"+fileCollection.getFiles().size()
			return fileCollection;
		}
	}

	public static void addPresentationCoreDependencyClosure(Project project){
		project.ext.presentationCore = {
			FileCollection fileCollection
			EscenicExtension escenic =   ExtensionUtils.findEscenicExtension(project)
			int count=0
			if(escenic != null){
				List<File> files= escenic.getPresentationDependencies()
				count=files.size()
				fileCollection=project.files(files)
			}else{
				fileCollection=project.files()
			}
			if(LOG.isInfoEnabled()){
				LOG.info("Adding "+count+" jars from presentation as dependencies for project:"+project.getName())
			}
			return fileCollection
		}
		
		project.ext.presentationAPI = {
			FileCollection fileCollection
			EscenicExtension escenic =   ExtensionUtils.findEscenicExtension(project)
			int count=0
			if(escenic != null){
				List<File> files= escenic.getPresentationAPIDependencies()
				count=files.size()
				fileCollection=project.files(files)
			}else{
				fileCollection=project.files()
			}
			if(LOG.isInfoEnabled()){
				LOG.info("Adding "+count+" jars from presentation api as dependencies for project:"+project.getName())
			}
			return fileCollection
		}
		
	}


	public static void addEscenicDistributionClosure(Project project){
		project.ext.escenicDistribution  =  { String relativePath ->
			List<String> result = []
			EscenicExtension escenic =   ExtensionUtils.findEscenicExtension(project)
			if(escenic != null){
				EscenicEngineModel model = new EscenicEngineModel(escenic)
				for(File f:model.resolve(relativePath)){
					result.add(f.getAbsolutePath())
				}
			}
			return result
		}

		project.ext.escenicEngineLibs  =  {
			List<String> result = []
			EscenicExtension escenic =   ExtensionUtils.findEscenicExtension(project)
			if(escenic != null){
				File libs = new File(escenic.getLocalRepositoryLocation(),"lib")
				result.add(libs.getAbsolutePath())
			}
			return result
		}

		project.ext.escenicDistributionWar = { String war ->

			EscenicExtension escenic =   ExtensionUtils.findEscenicExtension(project)
			if(escenic != null){
				File libs = new File(escenic.getLocalRepositoryLocation(),"webapps/"+war+".war")
				return libs
			}
			return null
		}

		project.extensions.escenicPluginLibs = { String pluginName,String folder ->

			EscenicExtension escenic =   ExtensionUtils.findEscenicExtension(project)
			if(escenic != null){
				EscenicEngineModel model = new EscenicEngineModel(escenic)
				Collection<File> libs = model.resolvePluginLibs(pluginName,folder)
				return project.files(libs)
			}
			return project.files()
		}
	}
}
