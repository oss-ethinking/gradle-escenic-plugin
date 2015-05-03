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
import org.gradle.api.logging.Logging
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.Logger;

class DependencyResolver {

	static Logger LOG = Logging.getLogger(EscenicEngineModel.class)

	public void storeZipDistribution(Project project,Configuration configuration,File target){

		configuration.files.each { file ->
			if (file.isFile()) {
				if(LOG.isInfoEnabled()){
					LOG.info("Unzip archive:"+file.getName());
				}
				project.copy{
					from project.zipTree(file)
					into target
				}
			}
		}
	}
}
