package de.ethinking.gradle.task.escenic

import de.ethinking.gradle.extension.escenic.ResourceHost
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.BuildException

class UploadResourcesTask extends DefaultTask{


    def String publication
    def ResourceHost resourceHost
    def File resourcesBase
    def boolean ignoreFailure=true

    @TaskAction
    def uploadResources(){
        
        File sourceDirectory = findSourceDirectory()
             
        if(sourceDirectory.exists()){
            sourceDirectory.listFiles().each(){ File resourceFile->
                if(resourceFile.isFile()){
                    String resource = resourceFile.getName().replaceAll("\\..*", "")
                    URL url = new URL(resourceHost.getUrl()+publication+"/escenic/"+resource)
                    String response= uploadResource(resourceFile, url)
                    if(response.length()>0){
                        println "Invalid resource:"+resourceFile.getAbsolutePath()
                        println "Response:"+response
                    }
                }
            }
        }
    }

    def File findSourceDirectory() {
        if(resourceHost.getResourceBase()){
           return resourceHost.getResourceBase()
        }else{
            return resourcesBase
        }
    }

    
    def injectAuthentication(HttpURLConnection connection){
        if(resourceHost.user && resourceHost.password){
            connection.addRequestProperty("Authorization", "Basic " + resourceUploadAuth)
        }
    }
    
    
    
    def uploadResource(File resource,URL target) throws BuildException{
        final HttpURLConnection connection=target.openConnection()
        connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.addRequestProperty("Content-Length", ""+resource.length())
       
        injectAuthentication(connection);
        
        connection.setDoOutput(true)
        connection.outputStream.withWriter('UTF-8') { Writer writer ->
            writer << resource.getText()
        }
        String response =""
        try{
            response = connection.inputStream.withReader { Reader reader -> reader.text }
            return response
        }catch(Exception e){
            response = connection.errorStream.withReader { Reader reader -> reader.text }
            File errorFile = new File(project.getBuildDir(),"reports/escenic-resource/"+resource.getName()+".html")
            errorFile.getParentFile().mkdirs()
            errorFile.write(response)
            String message ="Invalid Resource:"+resource.getName()+"\nError:"+connection.responseCode+"\nResourceURL:"+target.toString()+"\nSee "+ errorFile.getAbsolutePath()
            if(ignoreFailure){
                return message
            }
            throw new BuildException(message,e)
        }
    }
}
