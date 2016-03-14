package de.ethinking.gradle.extension.escenic

class ResourceHost {
    
    String url;
    File resourceBase;
    String user;
    String password;
    
    
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    
    def url(String url){
        this.url = url
    }
    public File getResourceBase() {
        return resourceBase;
    }
    public void setResourceBase(File resourceBase) {
        this.resourceBase = resourceBase;
    }
    
    def resourceBase(File resourceBase) {
        this.resourceBase = resourceBase;
    }
    
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    
    def user(String user) {
        this.user = user;
    }
    
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    def password(String password) {
        this.password = password;
    }
        
}
