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



import java.io.File

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencySet
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTree
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Copy



import de.ethinking.gradle.extension.escenic.EscenicExtension
import de.ethinking.gradle.repository.DependencyResolver
import de.ethinking.gradle.repository.EscenicEngineModel
import de.ethinking.gradle.task.escenic.StudioPluginAccessTask
import de.ethinking.gradle.task.escenic.EscenicReportTask
import de.ethinking.gradle.task.escenic.StudioPluginCollectorTask


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
        project.repositories { mavenCentral() }

        project.dependencies{ antRuntime 'xalan:xalan:2.7.1' }


        addPublicEngineAssemblyTasks(project)
        injectRepositoryClosures(project)

        //helper tasks
        project.task('init-feature', type: CreateFeatureArtifactTask)




        project.afterEvaluate {


            EscenicEngineModel escenicEngineModel = new EscenicEngineModel(project.escenic)
            if(!escenicEngineModel.isInitialized(project)){
                LOG.info("Create local escenic repository")
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

            project.repositories.add(
                    project.repositories.ivy{
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

        project.repositories.metaClass.escenicMaven = {String user,String passwd  ->
            project.repositories.add(project.repositories.maven {
                url "http://maven.escenic.com"
                credentials {
                    username = user
                    password = passwd
                }
            }
            )
        }
    }


    protected void evaluateDependencies(EscenicEngineModel escenicRepository,EscenicExtension escenicExtension){
        escenicRepository.setupDependencies(escenicExtension)
    }


    protected void addEngineAssemblyTasks(Project project){

        if(project.configurations.assembly){


            def assemblyVersion = createCacheKeyFromDependencies(project.configurations.assembly.dependencies)

            File nurseryLayerConfiguration =  project.file(project.escenic.layerConf)
            File engineSourceDirectory =  new File(project.escenic.getLocalRepositoryLocation(),"engine-source/")
            File layerConfigDirectory = new File(project.escenic.getAssemblyBase(),"conf")

            project.task("installAssembly",type:Delete){

                inputs.property 'assemblyVersion',assemblyVersion
                outputs.dir project.escenic.getAssemblyBase()

                doLast{
                    delete project.escenic.getAssemblyBase()
                    project.escenic.getAssemblyBase().mkdirs()

                    DependencyResolver resolver = new DependencyResolver()
                    resolver.storeZipDistribution(project,project.configurations.assembly, project.escenic.getAssemblyBase())

                    // check for nested assemblytool
                    def filter = new FilenameFilter() {
                                @Override
                                boolean accept(File path, String filename) {
                                    return filename.startsWith('assemblytool-')
                                }
                            }

                    def assemblytoolFiles = project.escenic.getAssemblyBase().listFiles(filter)
                    if (assemblytoolFiles && assemblytoolFiles.length > 0 && assemblytoolFiles[0].isDirectory()) {
                        File nestedAssembly = assemblytoolFiles[0];
                        project.copy{
                            from nestedAssembly
                            into nestedAssembly.getParentFile()
                        }
                    }
                }
            }

            project.task("cleanAssembly",dependsOn:'installAssembly',type:Delete){
                ext.layerConfigDirectory = new File(project.escenic.getAssemblyBase(),"conf")
                delete layerConfigDirectory
            }


            project.task("prepareAssembly",dependsOn:'cleanAssembly'){


                ext.skeletonConf = new File(engineSourceDirectory,"siteconfig/bootstrap-skeleton")
                ext.projectLayerConf =  nurseryLayerConfiguration

                inputs.dir skeletonConf
                if(projectLayerConf.exists()){
                    inputs.dir projectLayerConf
                }
                outputs.dir layerConfigDirectory

                doLast{
                    //copy layer from escenic distribution
                    project.copy{
                        from skeletonConf
                        into layerConfigDirectory
                    }
                    //copy layer from project
                    if(projectLayerConf.exists()){
                        project.copy{
                            from projectLayerConf
                            into layerConfigDirectory
                        }
                    }
                }
            }

            project.task("initializeAssembly",dependsOn:'prepareAssembly'){

                ext.srcDir = project.escenic.getAssemblyBase()
                ext.targetFile = new File(project.escenic.getAssemblyBase(),"assemble.properties")

                inputs.property 'assemblyVersion',assemblyVersion
                inputs.dir new File(project.escenic.getAssemblyBase(),"resources")
                inputs.properties project.escenic.assemblyProperties
                outputs.file targetFile



                doFirst{

                    project.ant{
                        ant(dir: project.escenic.getAssemblyBase(), antfile:"build.xml", inheritall:"false" ){
                            property(name:"engine.root",value:engineSourceDirectory.getAbsolutePath())
                            target(name:"initialize")
                        }
                    }

                    File assemblyPropertiesFile = new File(project.escenic.getAssemblyBase(),"assemble.properties")
                    File backupProperties = new File(assemblyPropertiesFile.getAbsolutePath()+".backup")
                    
                    if(!backupProperties.exists()){
                        assemblyPropertiesFile.renameTo(backupProperties)
                    }



                    Properties props = new Properties()
                    props.load(new FileInputStream(backupProperties))
                    project.escenic.assemblyProperties.each{ String key, String value ->
                        props.setProperty(key, value)
                    }
                    def propertiesFile = new File(project.escenic.getAssemblyBase(),"assemble.properties")
                    props.store(propertiesFile.newWriter(), "created by escenic gradle plugin")


                }
            }


            File studioPluginsSourceDir = new File(engineSourceDirectory,"tmp/plugins/local-plugin-"+project.getName()+"/studio/lib")
            File studioPluginsLibDir = new File(engineSourceDirectory,"plugins/local-plugin-"+project.getName()+"/studio/lib")


            
           def cleanStudioPluginsTask = project.task("cleanStudioPlugins",dependsOn:["initializeAssembly","collectStudioPlugins"],type:Delete){
                ext.studioPluginsSourceDir = studioPluginsSourceDir
                delete studioPluginsSourceDir
                
 
            }
                
            def copyStudioPluginsTask = project.tasks.create("copyStudioPlugins",StudioPluginCollectorTask,studioPluginsSourceDir)
            copyStudioPluginsTask.dependsOn  cleanStudioPluginsTask 
      

            project.tasks["prepareStudioPlugins"].dependsOn copyStudioPluginsTask
            project.tasks["prepareStudioPlugins"].studioPluginsSourceDir=studioPluginsSourceDir
            project.tasks["prepareStudioPlugins"].studioPluginsLibDir=studioPluginsLibDir
            project.tasks["prepareStudioPlugins"].pluginsBasePath=new File(engineSourceDirectory,"plugins")

            project.task("runAssembly",dependsOn:'prepareStudioPlugins'){

                ext.distDir = new File(project.escenic.getAssemblyBase(),"dist")

                outputs.dir distDir
                inputs.file new File(project.escenic.getAssemblyBase(),"assemble.properties")
                inputs.files studioPluginsLibDir,new File(project.escenic.getAssemblyBase(),"assemble.properties")
                inputs.property 'assemblyVersion',assemblyVersion

                doFirst{
                    ClassLoader antClassLoader = org.apache.tools.ant.Project.class.classLoader
                    project.configurations.antRuntime.each { File f ->
                        antClassLoader.addURL(f.toURI().toURL())
                    }

                    project.ant{
                        ant(dir: project.escenic.getAssemblyBase().getAbsolutePath(), antfile:"build.xml", inheritall:"false",useNativeBasedir:true){
                            property(name:"engine.root",value:engineSourceDirectory.getAbsolutePath())
                            project.escenic.antParameters.each{ String key, String value ->
                                property(name:key,value:value)
                            }
                            target(name:"clean")
                            target(name:"ear")
                        }
                    }
                }
            }
            
            project.task("cleanEscenicWebappRepository",dependsOn:'runAssembly',type:Delete){
                ext.webappRepository =  new File(project.escenic.getLocalRepositoryLocation(),"webapps")
                delete webappRepository
            }

            Task copyAssemblesTask = project.task("copyAssembles",dependsOn:'cleanEscenicWebappRepository'){

                ext.webappRepository =  new File(project.escenic.getLocalRepositoryLocation(),"webapps")
                ext.assemblyDistWarFileLocations = new File(project.escenic.getAssemblyBase(),"dist/war")

                inputs.dir assemblyDistWarFileLocations
                outputs.dir webappRepository

            }

            copyAssemblesTask.doLast{

                webappRepository.mkdirs()

                File webapps =  new File(project.escenic.getAssemblyBase(),"dist/war")
                Set<String> dublicationFilter = new HashSet<String>()
                FileTree assembledWebapps = project.fileTree(dir: webapps.getAbsolutePath(), include: '**/*.war')

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

                File assemblyEngineEarFile = new File(project.escenic.getAssemblyBase(),"dist/engine.ear")
                project.copy{
                    from project.zipTree(assemblyEngineEarFile)
                    into project.escenic.getLocalRepositoryLocation()
                    include "lib/**"
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
        
        project.task("collectStudioPlugins"){

        }
        

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