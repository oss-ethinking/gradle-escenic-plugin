package de.ethinking.gradle.task.escenic

import de.ethinking.gradle.extension.escenic.ResourceHost
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction;
import org.gradle.tooling.BuildException
import org.gradle.api.logging.Logging
import org.gradle.api.logging.Logger

class UploadResourcesTask extends DefaultTask{

    @Input
    def String publication
    @Input
    def ResourceHost resourceHost
    @InputFile
    def File resourcesBase
    @Console
    def boolean ignoreFailure=true
    
    static Logger LOG = Logging.getLogger(UploadResourcesTask.class)

    @TaskAction
    def uploadResources(){
        File sourceDirectory = findSourceDirectory()
        if(sourceDirectory.exists()){
            if(resourceHost.usePublicationDirectories){
                sourceDirectory.listFiles().each(){ File child->
                    if(child.isDirectory()){
                        processPublicationDirectory(child,child.getName())
                    }
                }
            }else{
                processPublicationDirectory(sourceDirectory,publication)
            }
        }
    }

   def processPublicationDirectory(File resourceDirectory,String publication) {
        resourceDirectory.listFiles().each(){ File resourceFile->
            if(resourceFile.isFile()){
                String resource = resourceFile.getName().replaceAll("\\..*", "")
                URL url = new URL(resourceHost.getUrl()+publication+"/escenic/"+resource)
                String response= uploadResource(resourceFile, url)
                if(response.length()>0){
                    LOG.error("Invalid resource:"+resourceFile.getAbsolutePath()+" response:"+response+" from url:"+url)
                }else{
                    LOG.info("Uploaded resource:"+resourceFile.getAbsolutePath()+" to url:"+url)                
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
            String base64 = (resourceHost.user+":"+resourceHost.password).getBytes('iso-8859-1').encodeBase64().toString()
            connection.setRequestProperty("Authorization", "Basic "+base64)
        }
    }



    def uploadResource(File resource,URL target) throws BuildException{
        final HttpURLConnection connection=target.openConnection()
        connection.setDoInput(true)
        connection.setDoOutput(true)
        
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.setRequestProperty("Content-Length", ""+resource.length())

        injectAuthentication(connection);


        connection.outputStream.withWriter('UTF-8') { Writer writer ->
            writer << resource.getText()
        }
        String response =""
        try{
            response = connection.inputStream.withReader { Reader reader -> reader.text }
            return response
        }catch(IOException e){
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
