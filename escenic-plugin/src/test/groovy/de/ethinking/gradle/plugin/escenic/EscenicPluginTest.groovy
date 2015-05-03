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


import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import static org.junit.Assert.*

class EscenicPluginTest {
	
	@Test
	public void escenicPluginProject() {
		
		Project parent = ProjectBuilder.builder().withName("escenic").build()
		Project studioProject = ProjectBuilder.builder().withParent(parent).withName("studio").build()
		Project engineProject = ProjectBuilder.builder().withParent(parent).withName("engine").build()
		Project presentationProject = ProjectBuilder.builder().withParent(parent).withName("presentation").build()

		parent.apply plugin: 'de.ethinking.escenic'
		studioProject.apply plugin: 'java'
		studioProject.apply plugin: 'de.ethinking.escenic.studio'
		presentationProject.apply plugin:'de.ethinking.escenic.presentation'
		engineProject.apply plugin:'de.ethinking.escenic.engine'
		
		parent.evaluate()
		studioProject.evaluate()
		
		assertNotNull(parent.tasks.collectStudioPlugins)
		assertTrue(parent.tasks.collectStudioPlugins.taskDependencies.getDependencies(parent.tasks.collectStudioPlugins).contains(studioProject.build))
		
	}
}
