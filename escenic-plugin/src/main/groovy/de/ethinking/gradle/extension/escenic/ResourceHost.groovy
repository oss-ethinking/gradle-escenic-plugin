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

class ResourceHost {
    
    String url
    File resourceBase
    String user
    String password
    def boolean usePublicationDirectories=false
    
    
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
    
    public boolean isUsePublicationDirectories() {
        return usePublicationDirectories;
    }
    
    public void setUsePublicationDirectories(boolean usePublicationDirectories) {
        this.usePublicationDirectories = usePublicationDirectories;
    }
    
    def usePublicationDirectories(boolean usePublicationDirectories) {
        this.usePublicationDirectories = usePublicationDirectories;
    }
        
}
