package de.ethinking.gradle.extension.escenic

import org.gradle.api.Project

class PublicationExtension {

    def String[] publications
    def Boolean provideResources=true
    def Boolean ignoreResourcesFailure=true;
    def File resourcesBase
    def Project project
    def Map<String,String> resourcesHosts = new HashMap<String,ResourceHost>()


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
        ResourceHost resourceHost = new ResourceHost()
        resourceHost.setUrl(url)
        resourcesHosts.put(host, resourceHost)
    }

    def resourcesHost(String host,String url,String user,String password){
        
        ResourceHost resourceHost = new ResourceHost()
        resourceHost.setUrl(url)
        resourceHost.setPassword(password)
        resourceHost.setUser(user)
        
        resourcesHosts.put(host, resourceHost)
    }
    
    def resourcesHost(String host,Closure closure){
        
        ResourceHost resourceHost = new ResourceHost()
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = resourceHost
        closure.call()
        
        resourcesHosts.put(host, resourceHost)
        
    }
}
