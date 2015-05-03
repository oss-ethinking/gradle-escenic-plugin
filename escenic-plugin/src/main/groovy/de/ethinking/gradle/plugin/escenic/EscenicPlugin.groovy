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
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Delete



import de.ethinking.gradle.extension.escenic.EscenicExtension
import de.ethinking.gradle.repository.DependencyResolver
import de.ethinking.gradle.repository.EscenicEngineModel
import de.ethinking.gradle.task.escenic.StudioPluginAccessTask
import org.gradle.api.logging.Logging

import de.ethinking.gradle.task.escenic.CreateFeatureArtifactTask


class EscenicPlugin implements Plugin<Project> {

	static Logger LOG = Logging.getLogger(EscenicPlugin.class)



	void apply(Project project) {

		project.configure(project) {
			project.extensions.create("escenic",EscenicExtension,project)
		}

		project.configurations{
			engine
			plugin
			assembly
		}

		addPublicEngineAssemblyTasks(project)
		injectRepositoryClosures(project)

		//helper tasks
		project.task('init-feature', type: CreateFeatureArtifactTask)

		project.afterEvaluate {

			EscenicEngineModel escenicEngineModel = new EscenicEngineModel(project.escenic)
			if(!escenicEngineModel.isInitialized(project)){
				escenicEngineModel.initialize(project)
			}

			evaluateDependencies(escenicEngineModel,project.escenic)
			File webappsRepository = escenicEngineModel.getWebappRepositoryLocation()

			if(LOG.isInfoEnabled()){
				LOG.info("Assembly war file repository:"+webappsRepository.getAbsoluteFile())
			}

			//adding webapps location  as
			for(Project subproject:project.allprojects){

				DependencyClosures.addEscenicDistributionClosure(subproject)
				subproject.repositories{
					flatDir	{ dirs webappsRepository.getAbsolutePath() }
				}
			}
			addEngineAssemblyTasks(project)
		}

	}

	private injectRepositoryClosures(Project project) {
        
		project.repositories.metaClass.escenicRepository = {String repoURL,String user,String passwd  ->

			if(LOG.isInfoEnabled()){
				LOG.info("Setup Ivy-Repo from:"+repoURL)
				if(user&&passwd){
					LOG.info("Using credentials user:"+user+" and password:"+passwd)
				}
			}

			project.repositories.add(project.repositories.ivy{
				url repoURL
				layout "pattern", {artifact "[artifact]-[revision].zip"}
				if(user && passwd){
					credentials {
						username user
						password passwd
					}
				}
			}
			)
		}


		project.repositories.metaClass.escenicLocal = {String repoLocation  ->

			String repoURL =  project.file(repoLocation).toURI().toURL().toExternalForm()
			if(LOG.isInfoEnabled()){
				LOG.info("Setup Ivy-Repo with url:"+repoURL)
			}

			project.repositories.add(project.repositories.ivy{
				url repoURL
				layout "pattern", {artifact "[artifact]-[revision].zip"}
			}
			)
		}
	}


	protected void evaluateDependencies(EscenicEngineModel escenicRepository,EscenicExtension escenicExtension){
		escenicRepository.setupDependencies(escenicExtension)
	}


	def addEngineAssemblyTasks(Project project){
		if(project.configurations.assembly){

			File assemblyDirectory = new File(project.escenic.getLocalRepositoryLocation(),"assembly/")
			File engineSourceDirectory =  new File(project.escenic.getLocalRepositoryLocation(),"engine-source/")
			//File initialAssemblyFile = new File(assemblyDirectory,"assemble.properties")

			//if(initialAssemblyFile.exists()){
			project.task("installAssembly",type:Delete)<<{
				delete assemblyDirectory
				assemblyDirectory.mkdirs()
				DependencyResolver resolver = new DependencyResolver()
				resolver.storeZipDistribution(project,project.configurations.assembly, assemblyDirectory)
			}

			project.task("prepareAssembly",dependsOn:'installAssembly')<<{
				File layerConfigDirectory = new File(assemblyDirectory,"conf")
				//copy layer from escenic distribution

				project.copy{
					from new File(engineSourceDirectory,"siteconfig/bootstrap-skeleton")
					into layerConfigDirectory
				}
				//copy layer from project
				project.copy{
					from project.file(project.escenic.layerConf)
					into layerConfigDirectory
				}
			}

			project.task("initializeAssembly",dependsOn:'prepareAssembly')<<{
				project.ant{
					ant(dir: assemblyDirectory.getAbsolutePath(), antfile:"build.xml", inheritall:"false" ){
						property(name:"engine.root",value:engineSourceDirectory.getAbsolutePath())
						target(name:"initialize")
					}
				}
			}
			project.tasks.findByName("collectStudioPlugins").dependsOn "initializeAssembly"
			//}


			final def List<File> collectedStudioPluginLibs = []
			project.task("copyStudioPluginForAssembly",dependsOn:"collectStudioPlugins")<<{
				File studioPluginsLibDir = new File(engineSourceDirectory,"plugins/local-plugin-"+project.getName()+"/studio/lib")
				project.allprojects { Project subProject ->
					if(subProject.plugins.hasPlugin('de.ethinking.escenic.studio')&&subProject.studio.includePlugin){
						LOG.info("copy studio plugin project:"+subProject.getName())
						studioPluginsLibDir.mkdirs()
						project.copy{
							from subProject.jar
							from subProject.configurations.runtimeStudio
							into studioPluginsLibDir
							eachFile{ FileCopyDetails detail ->
								collectedStudioPluginLibs.add(detail.getRelativePath().getFile(studioPluginsLibDir))
							}
						}
					}
				}
			}

			project.tasks["prepareStudioPlugins"].dependsOn "copyStudioPluginForAssembly" 
			project.tasks["prepareStudioPlugins"].studioPluginLibs=collectedStudioPluginLibs
			project.tasks["prepareStudioPlugins"].pluginsBasePath=new File(engineSourceDirectory,"plugins")

			project.task("runAssembly",dependsOn:'prepareStudioPlugins')<<{
				project.ant{
					ant(dir: assemblyDirectory.getAbsolutePath(), antfile:"build.xml", inheritall:"false" ){
						property(name:"engine.root",value:engineSourceDirectory.getAbsolutePath())
						target(name:"clean")
						target(name:"ear")
					}
				}
			}
			project.task("afterAssembly",dependsOn:'runAssembly')<<{
				File webappRepository =  new File(project.escenic.getLocalRepositoryLocation(),"webapps")
				webappRepository.mkdirs()

				Set<String> dublicationFilter = new HashSet<String>()
				File assemblyDistWarFileLocations = new File(assemblyDirectory,"dist/war")

				FileTree assembledWebapps = project.fileTree(dir: assemblyDistWarFileLocations.getAbsolutePath(), include: '**/*.war')

				project.copy{
					from assembledWebapps.files
					into webappRepository
					eachFile{ FileCopyDetails detail ->
						dublicationFilter.add(detail.getName())
					}

				}

				FileTree tree = project.fileTree(dir: engineSourceDirectory.getAbsolutePath(), include: '**/*.war')
				project.copy{
					from tree.files
					into webappRepository
					eachFile{ FileCopyDetails detail ->
						if(dublicationFilter.contains(detail.getName())){
							detail.exclude()
						}
					}
				}


				File assemblyEngineEarFile = new File(assemblyDirectory,"dist/engine.ear")
				project.copy{
					from project.zipTree(assemblyEngineEarFile)
					into project.escenic.getLocalRepositoryLocation()
					include "lib/**"
				}
			}

			project.task("cleanupAssembly",dependsOn:'afterAssembly',type:Delete)<<{
				File studioPluginsLibDir = new File(engineSourceDirectory,"plugins/local-plugin-"+project.getName()+"/studio/lib")
				delete studioPluginsLibDir
			}

			project.task("assembly",dependsOn:'cleanupAssembly')<<{

			}
		}
	}

	def addPublicEngineAssemblyTasks(Project project) {
		project.task("collectStudioPlugins")<<{

		}
		project.task("prepareStudioPlugins",type:StudioPluginAccessTask){

		}
	}
}