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
package de.ethinking.gradle.extension.escenic

import org.gradle.api.Project
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging



class EscenicExtension {

    static Logger LOG = Logging.getLogger(EscenicExtension.class)

    Project project
    String engineRepository = '.repo'
    String layerConf="src/conf/nursery"
    Map<String,String> assemblyProperties = new HashMap<String,String>()
    File assemblyBase = new File(".repo/assembly")

    List<String> engineLibs = []
    List<String> engineAPILibs = []
    List<String> presentationLibs = []
    List<String> presentationAPILibs = []
    List<String> studioLibs = []

    Map<String,List<String>> pluginPresentationLibs = new HashMap<String,List<String>>()
    Map<String,List<String>> pluginEngineLibs = new HashMap<String,List<String>>()
    Map<String,List<String>> pluginStudioLibs = new HashMap<String,List<String>>()
    Map<String,List<String>> applicationsLibs = new HashMap<String,List<String>>()
    
   


    public EscenicExtension(Project project){
        this.project=project
    }

    
    public File getLocalRepositoryLocationFile(){
        return project.file(engineRepository)
    }
    

    public String getLocalRepositoryLocation(){
        return getLocalRepositoryLocationFile().getAbsolutePath();
    }


    public void setPresenationDependencies(List<String> libs){
        presentationLibs=libs
    }

    public List<String> getPresentationDependencies(){
        return presentationLibs
    }

    public List<String> getPresentationAPIDependencies(){
        return presentationAPILibs
    }


    public void addPluginEngineDependencies(String plugin,List<String> libs){
        pluginEngineLibs.put(plugin,libs)
    }

    public Map<String,String> getPluginEngineDependencies(){
        return pluginEngineLibs
    }

    public void addPluginPresentationDependencies(String plugin,List<String> libs){
        pluginPresentationLibs.put(plugin,libs)
    }

    public Map<String,String> getPluginPresentationDependencies(){
        return pluginPresentationLibs
    }

    public void addPluginStudioDependencies(String plugin,List<String> libs){
        pluginStudioLibs.put(plugin,libs)
    }

    public Map<String,String> getPluginStudioDependencies(){
        return pluginStudioLibs
    }


    public void setEngineDependencies(List<String> engineLibs ){
        this.engineLibs=engineLibs
    }

    public List<String> getEngineDependencies(){
        return engineLibs
    }

    public List<String> getEngineAPIDependencies(){
        return engineAPILibs
    }



    public void setStudioDependencies(List<String> libs){
        studioLibs=libs;
    }

    public List<String> getStudioDependencies(){
        return studioLibs;
    }
    
    public assemblyProperty(String key,String value){
        assemblyProperties.put(key,value)
    }
    
    public assemblyBase(File f){
        assemblyBase=f
    }
    
    public File getAssemblyBase(){
        return assemblyBase
    }
}
