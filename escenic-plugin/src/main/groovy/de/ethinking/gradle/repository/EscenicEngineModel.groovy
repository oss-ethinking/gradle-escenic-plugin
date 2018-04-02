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
package de.ethinking.gradle.repository

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import de.ethinking.gradle.CommonUtils
import de.ethinking.gradle.extension.escenic.EscenicExtension
import de.ethinking.gradle.extension.escenic.ExtensionUtils

class EscenicEngineModel{

	static Logger LOG = Logging.getLogger(EscenicEngineModel.class)


	EscenicExtension escenicExtension

	String engineStorePath='engine/'
	String pluginStorePath='plugins/'

	String engineSourcePath='engine-source/'

	String validationFileName='.cacheKey'



	public EscenicEngineModel(){
		
	}






	public EscenicEngineModel(EscenicExtension escenicExtension){
		this.escenicExtension=escenicExtension
	}




	public File createCleanRepositoryLocation(){
		File repositoryLocation = new File(escenicExtension.getLocalRepositoryLocation())
		if(repositoryLocation.exists()){
			repositoryLocation.deleteDir()
		}
		repositoryLocation.mkdirs()
		return repositoryLocation
	}



	public void storeEngine(File repositoryLocation,Project project){
		if(project.configurations.engine){
			DependencyResolver resolver = new DependencyResolver()



			//download from Source Repository and store in local Repository
			File tempDirectory = new File(repositoryLocation,"engine-temp/")
			resolver.storeZipDistribution(project,project.configurations.engine, tempDirectory)
			File engineSourceDirectory =  new File(repositoryLocation,engineSourcePath)

			tempDirectory.listFiles().each { File f ->
				if(f.isDirectory() && f.getName().startsWith('engine')){
					if(LOG.isInfoEnabled()){
						LOG.info("Found Engine Directory:"+f.getAbsolutePath())
					}
					boolean b = f.renameTo(engineSourceDirectory)

					if(!b){
						if(LOG.isInfoEnabled()){
							LOG.info("Could not rename directory to:"+engineSourceDirectory.getAbsolutePath())
						}
						engineSourceDirectory.mkdirs()
						project.copy{
							from f
							into engineSourceDirectory
						}
						f.deleteDir()
						f.delete()
					}
				}
			}
		}else{
			LOG.info("There is no engine configured for your project.")
		}
	}


	public void storePlugins(File repositoryLocation,Project project){
		DependencyResolver resolver = new DependencyResolver()
		File pluginTarget = new File(repositoryLocation,engineSourcePath+pluginStorePath)
		pluginTarget.getParentFile().mkdirs()
		resolver.storeZipDistribution(project,project.configurations.plugin, pluginTarget)
	}

	public void initialize(Project project){
        
		//create a completely clean repository directory
		File repoLocation = createCleanRepositoryLocation()
		new File(repoLocation,engineSourcePath+pluginStorePath).mkdirs()


		storeEngine(repoLocation,project)
		storePlugins(repoLocation,project)

		//create a checksum and store it
		//so we can check if something is new and
		//we have to recreate the engine repository
		storeRepositoryCacheKey(createCacheKey(project))
	}


	public boolean isInitialized(Project project){
		String cacheKey = createCacheKey(project)
		String repositoryCacheKey = readRepositoryCacheKey()
		if(LOG.isInfoEnabled()){
			LOG.info("Escenic RepoistoryCacheKey:"+repositoryCacheKey)
			LOG.info("Escenic ProjectCacheKey:"+cacheKey)
			LOG.info("Update EngineRepository:"+!cacheKey.equals(repositoryCacheKey))
		}
		return cacheKey.equals(repositoryCacheKey)
	}

	public String createCacheKey(Project project){
		
        def dependencyList = []
        
        String cacheKey = ''
        
		for(Dependency dependency:project.configurations.engine.dependencies){
			dependencyList.add(dependency.getGroup()+":"+dependency.getName()+":"+dependency.getVersion())
		}
		for(Dependency dependency:project.configurations.plugin.dependencies){
			dependencyList.add(dependency.getGroup()+":"+dependency.getName()+":"+dependency.getVersion())
		}
        
        dependencyList = dependencyList.sort()

        cacheKey = dependencyList.iterator().join(',')
        
        return cacheKey
	}



	public String readRepositoryCacheKey(){
		String repositoryCacheKey = ''
		File cacheKeyFile = new File(escenicExtension.getLocalRepositoryLocation(),validationFileName)
		if(cacheKeyFile.exists()){
			repositoryCacheKey=cacheKeyFile.text
		}
		return repositoryCacheKey
	}

	public void storeRepositoryCacheKey(String cacheKey){
		File cacheKeyFile = new File(escenicExtension.getLocalRepositoryLocation(),validationFileName)
		if(cacheKeyFile.exists()){
			cacheKeyFile.delete()
		}
		cacheKeyFile.withWriter() { it <<  cacheKey}
	}


	public void setupDependencies(EscenicExtension escenicExtension){

		List<String> engineDependencies = []
		Set<String> filter = new HashSet<String>()
		File engineCoreDirectory = new File(escenicExtension.getLocalRepositoryLocation(),engineSourcePath+"lib")
		engineDependencies.addAll(findJars(engineCoreDirectory,filter))
		escenicExtension.setEngineDependencies(engineDependencies)

		Set<String> engineAPIFilter =  new HashSet<String>()
		filter.each { String fileName ->  
			if(!fileName.contains("engine-core-")&&!fileName.contains("engine-syndication-")&&!fileName.contains("classification-api")&&!fileName.contains("model-core")&&!fileName.contains("common-nursery")&&!fileName.contains("common-util")){
				engineAPIFilter.add(fileName)
			}
		}
		List<String> engineAPI = []
		
		engineAPI.addAll(findJars(engineCoreDirectory,engineAPIFilter))
		escenicExtension.engineAPILibs=engineAPI
		
		
		//handle all plugins
		File pluginsDirectory = new File(escenicExtension.getLocalRepositoryLocation(),engineSourcePath+pluginStorePath)
		if(pluginsDirectory.isDirectory()){
			for(File f:pluginsDirectory.listFiles()){
				if(f.isDirectory()){
					File pluginEngineLibs = new File(f,"lib")
					escenicExtension.addPluginEngineDependencies(ExtensionUtils.transformPluginDirectoryToName(f.getName()),findJars(pluginEngineLibs,new HashSet<String>()))

					File pluginPresentationLibs = new File(f,"publication/webapp/WEB-INF/lib")
					escenicExtension.addPluginPresentationDependencies(ExtensionUtils.transformPluginDirectoryToName(f.getName()), findJars(pluginPresentationLibs,new HashSet<String>()))

					File pluginStudioLibs = new File(f,"studio/lib")
					escenicExtension.addPluginStudioDependencies(ExtensionUtils.transformPluginDirectoryToName(f.getName()), findJars(pluginStudioLibs,new HashSet<String>()))
				}
			}
		}
		List<String> publicationDependencies = []
		File publicationCoreDirectory = new File(escenicExtension.getLocalRepositoryLocation(),engineSourcePath+"template/WEB-INF/lib")
		Set<String> presentationFilter =  new HashSet<String>()
		publicationDependencies.addAll(findJars(publicationCoreDirectory,presentationFilter ))
		escenicExtension.setPresenationDependencies(publicationDependencies)
		List<String> publicationAPI = []
		Set<String> presentationAPIFilter =  new HashSet<String>()
		presentationFilter.each { String fileName ->  
			if(!fileName.contains("-presentation-")&&!fileName.contains("common-nursery-servlet")){
				presentationAPIFilter.add(fileName)
			}
		}
		publicationAPI.addAll(findJars(publicationCoreDirectory,presentationAPIFilter))
		escenicExtension.presentationAPILibs = publicationAPI
		
		
		//extract studio war into diretory
		File studioDirectory = new File(escenicExtension.getLocalRepositoryLocation(),engineSourcePath+"tmp/webapps/studio")

		if(!studioDirectory.exists()){
			extractWarIntoDirectory(new File(escenicExtension.getLocalRepositoryLocation(),engineSourcePath+"webapps/studio.war"),studioDirectory)
		}
		List<String> studioDependencies = []
		File studioCoreDirectory = new File(escenicExtension.getLocalRepositoryLocation(),engineSourcePath+"tmp/webapps/studio/studio/lib")
		studioDependencies.addAll(findJars(studioCoreDirectory, new HashSet<String>()))
		escenicExtension.setStudioDependencies(studioDependencies)

	}


	protected List<String> findJars(File f,Set<String> filter ){
		List<String> jars = []
		if(f.exists()&&f.isDirectory()){
			for(File file:f.listFiles()){
				String fileName = file.getName()
				if(fileName.endsWith(".jar")&& !filter.contains(fileName)){
					jars.add(file.getAbsolutePath())
					filter.add(fileName)
				}
			}
		}
		if(LOG.isInfoEnabled()){
			LOG.info("Found "+jars.size()+" jars in:"+f.getAbsolutePath())
		}
		return jars
	}

	protected void extractWarIntoDirectory(File warFile,File targetDirectory){
		targetDirectory.mkdirs()
		CommonUtils.unzip(warFile,targetDirectory)
	}

	def File getWebappRepositoryLocation() {
		File webappRepository =  new File(escenicExtension.getLocalRepositoryLocation(),"webapps")
		return webappRepository
	}

	def File getAssemblyDirectory() {
		File assemblyDirectory = new File(escenicExtension.getLocalRepositoryLocation(),"assembly/")
		return assemblyDirectory
	}

	def File getEngineSourceDirectory() {
		File engineSourceDirectory =  new File(escenicExtension.getLocalRepositoryLocation(),engineSourcePath)
		return engineSourceDirectory
	}

	public Collection<File> resolvePluginLibs(String pluginName,String folder) {
		Set<File>  files = new HashSet<File>()
		File from = new File(findPluginFolder(pluginName),folder)
		if(from.exists()){
			if(from.isDirectory()){
				for(File file:from.listFiles()){
					if(file.isFile()){
						files.add(file)
					}
				}
			}else{
				files.add(from)
			}
		}
		return files;
	}


	def findPluginFolder(String pluginName){
		File folder = new File(getEngineSourceDirectory(),"plugins/"+pluginName)
		if(!folder.exists()){
			File plugins = new File(getEngineSourceDirectory(),"plugins/")
			if(plugins.exists()&&plugins.isDirectory()){
				for(File pluginDir:plugins.listFiles()){
					if(pluginDir.isDirectory()&&pluginDir.getName().startsWith(pluginName)){
						return pluginDir
					}
				}
			}
			if(LOG.isInfoEnabled()){
				LOG.info("Could not find the plugin folder for:"+pluginName+" in "+(getEngineSourceDirectory()+"plugins"))
			}
		}
		return folder
	}



	public Collection<File> resolve(String value) {
		Set<File>  files = new HashSet<File>()
		File from = new File(getEngineSourceDirectory(),value)
		if(from.exists()){
			files.add(from)
		}
		return files;
	}
}
