package de.ethinking.gradle.extension.escenic

import org.gradle.api.Project

class PublicationExtension {

    def String[] publications
    def Boolean provideResources=true
    def Boolean ignoreResourcesFailure=true;
    def File resourcesBase
    def Project project
    def Map<String,String> resourcesHosts = new HashMap<String,String>()
    
    public PublicationExtension(Project project){
        this.project = project
        resourcesBase = project.file("src/main/resources/META-INF/escenic/publication-resources/escenic")
    }
        
    def publications(String... args){
        publications = args
    }
    
    def resourcesBase(String location){
        resourcesBase = project.file(location)
    }
    
    def resourcesBase(File file){
        resourcesBase = file
    }
    
    def resourcesHost(String host,String url){
        resourcesHosts.put(host, url)
    }
}
