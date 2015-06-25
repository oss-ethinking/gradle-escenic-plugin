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
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Delete
import org.gradle.api.file.DuplicatesStrategy



import de.ethinking.gradle.extension.escenic.EscenicExtension
import de.ethinking.gradle.repository.DependencyResolver
import de.ethinking.gradle.repository.EscenicEngineModel
import de.ethinking.gradle.task.escenic.StudioPluginAccessTask
import de.ethinking.gradle.task.escenic.EscenicReportTask

import org.gradle.api.logging.Logging
import org.gradle.tooling.BuildException

import de.ethinking.gradle.task.escenic.CreateFeatureArtifactTask


class EscenicPlugin implements Plugin<Project> {

    static Logger LOG = Logging.getLogger(EscenicPlugin.class)


    void apply(Project project) {
      
        project.configure(project) {
            project.extensions.create("escenic",EscenicExtension,project)
            
        }
        project.configurations{
            antRuntime
            engine
            plugin
            assembly
        }
        project.repositories {
            mavenCentral()
        }
        
        project.dependencies{
            antRuntime 'xalan:xalan:2.7.1'
        }
       
     
        addPublicEngineAssemblyTasks(project)
        injectRepositoryClosures(project)

        //helper tasks
        project.task('init-feature', type: CreateFeatureArtifactTask)

    


        project.afterEvaluate {

         
            EscenicEngineModel escenicEngineModel = new EscenicEngineModel(project.escenic)
            if(!escenicEngineModel.isInitialized(project)){
                println "Create local escenic repository"
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


            def assemblyVersion = createCacheKeyFromDependencies(project.configurations.assembly.dependencies)


            File assemblyDirectory = new File(project.escenic.getLocalRepositoryLocation(),"assembly/")
            File assemblyPropertiesFile = new File(assemblyDirectory,"assemble.properties")
            File nurseryLayerConfiguration =  project.file(project.escenic.layerConf)
            File engineSourceDirectory =  new File(project.escenic.getLocalRepositoryLocation(),"engine-source/")


            project.task("installAssembly",type:Delete){

                inputs.property 'assemblyVersion',assemblyVersion
                outputs.dir assemblyDirectory

                doLast{
                    delete assemblyDirectory
                    assemblyDirectory.mkdirs()
                    DependencyResolver resolver = new DependencyResolver()
                    resolver.storeZipDistribution(project,project.configurations.assembly, assemblyDirectory)
                }
            }

            project.task("prepareAssembly",dependsOn:'installAssembly',type:Delete){

                ext.layerConfigDirectory = new File(assemblyDirectory,"conf")
                ext.skeletonConf = new File(engineSourceDirectory,"siteconfig/bootstrap-skeleton")
                ext.projectLayerConf =  nurseryLayerConfiguration

                inputs.dir skeletonConf
                inputs.dir projectLayerConf
                outputs.dir layerConfigDirectory

                delete layerConfigDirectory

                doLast{


                    //copy layer from escenic distribution
                    project.copy{
                        from skeletonConf
                        into layerConfigDirectory
                    }
                    //copy layer from project
                    project.copy{
                        from projectLayerConf
                        into layerConfigDirectory
                    }
                }
            }

            project.task("initializeAssembly",dependsOn:'prepareAssembly'){

                ext.srcDir = assemblyDirectory
                ext.targetFile = assemblyPropertiesFile

                inputs.property 'assemblyVersion',assemblyVersion
                inputs.dir new File(assemblyDirectory,"resources")
                inputs.properties project.escenic.assemblyProperties
                outputs.file targetFile

                
                
                doFirst{

                    project.ant{
                        ant(dir: assemblyDirectory.getAbsolutePath(), antfile:"build.xml", inheritall:"false" ){
                            property(name:"engine.root",value:engineSourceDirectory.getAbsolutePath())
                            target(name:"initialize")
                        }
                    }

                    File backupProperties = new File(assemblyPropertiesFile.getAbsolutePath()+".backup")
                    if(!backupProperties.exists()){
                        assemblyPropertiesFile.renameTo(backupProperties)
                    }



                    Properties props = new Properties()
                    props.load(new FileInputStream(backupProperties))
                    project.escenic.assemblyProperties.each{ String key, String value ->
                        props.setProperty(key, value)
                    }

                    File defaultProps = new File(targetFile.getAbsolutePath())
                    props.store(targetFile.newWriter(), "created by escenic gradle plugin")


                }
            }


            File studioPluginsSourceDir = new File(engineSourceDirectory,"tmp/plugins/local-plugin-"+project.getName()+"/studio/lib")
            File studioPluginsLibDir = new File(engineSourceDirectory,"plugins/local-plugin-"+project.getName()+"/studio/lib")


            project.task("copyStudioPlugins",dependsOn:"initializeAssembly",type:Delete){

                ext.studioPluginsSourceDir = studioPluginsSourceDir
                ext.pluginBasePath = new File(engineSourceDirectory,"plugins")

                outputs.dir studioPluginsSourceDir

                delete studioPluginsSourceDir

                doLast{
                    studioPluginsSourceDir.mkdirs()
                    project.allprojects { Project subProject ->
                        if(subProject.plugins.hasPlugin('de.ethinking.escenic.studio')&&subProject.studio.includePlugin){
                            LOG.info("copy studio plugin project:"+subProject.getName())
                            project.copy{
                                from subProject.jar
                                from subProject.configurations.runtimeStudio
                                into studioPluginsSourceDir
                            }
                        }
                    }
                }
            }

            project.tasks["prepareStudioPlugins"].dependsOn "copyStudioPlugins"
            project.tasks["prepareStudioPlugins"].studioPluginsSourceDir=studioPluginsSourceDir
            project.tasks["prepareStudioPlugins"].studioPluginsLibDir=studioPluginsLibDir
            project.tasks["prepareStudioPlugins"].pluginsBasePath=new File(engineSourceDirectory,"plugins")

            project.task("runAssembly",dependsOn:'prepareStudioPlugins'){



                ext.distDir = new File(assemblyDirectory,"dist")

                outputs.dir distDir
                inputs.dir   assemblyPropertiesFile
                inputs.files studioPluginsLibDir,assemblyPropertiesFile
                inputs.property 'assemblyVersion',assemblyVersion

                doFirst{
                    ClassLoader antClassLoader = org.apache.tools.ant.Project.class.classLoader
                     project.configurations.antRuntime.each { File f ->
                            antClassLoader.addURL(f.toURI().toURL())
                    }
                    
                    project.ant{
                        ant(dir: assemblyDirectory.getAbsolutePath(), antfile:"build.xml", inheritall:"false",useNativeBasedir:true){
                            property(name:"engine.root",value:engineSourceDirectory.getAbsolutePath())
                            target(name:"clean")
                            target(name:"ear")
                        }
                    }
                }
            }

            project.task("copyAssembles",dependsOn:'runAssembly',type:Delete){

                ext.webappRepository =  new File(project.escenic.getLocalRepositoryLocation(),"webapps")
                ext.assemblyDistWarFileLocations = new File(assemblyDirectory,"dist/war")

                inputs.dir assemblyDistWarFileLocations
                outputs.dir webappRepository

                delete webappRepository

                doLast{

                    webappRepository.mkdirs()

                    Set<String> dublicationFilter = new HashSet<String>()
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
            }

            project.task("assembly",dependsOn:'copyAssembles'){

            }
        }else{
            throw new BuildException("You must add an assembly tool version as dependencies.")
        }

        project.task("cleanEscenic",type:Delete){
            delete project.escenic.getLocalRepositoryLocation()
        }
    }

    def addPublicEngineAssemblyTasks(Project project) {

        project.task("prepareStudioPlugins",type:StudioPluginAccessTask){

        }

        project.task("escenicReport",type:EscenicReportTask){
            reportBase  = new File(project.getBuildDir(),"reports/escenic")
            escenicExtension = project.escenic
        }

        project.task("serveEscenicReport",dependsOn:"escenicReport"){
            doFirst{
                java.awt.Desktop.getDesktop().open( new File(project.getBuildDir(),"reports/escenic/index.html"))
            }
        }
    }


    def createCacheKeyFromDependencies(DependencySet dependencies){

        def dependencyList = []

        String cacheKey = ''

        for(Dependency dependency:dependencies){
            dependencyList.add(dependency.getGroup()+":"+dependency.getName()+":"+dependency.getVersion())
        }
        dependencyList = dependencyList.sort()
        cacheKey = dependencyList.iterator().join(',')

        return cacheKey
    }
}