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
package de.ethinking.gradle.task.escenic

import de.ethinking.gradle.contenttype.ContentTypeFileProcessor
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction

class MergeContentTypeTask extends DefaultTask {

    @Input
    String publication

    @InputFile
    File baseFile

    @Input
    FileCollection fragments

    @OutputFile
    File outputFile

    @TaskAction
    def merge(){
        System.out.println("Merging content types for publication $publication")

        ContentTypeFileProcessor processor = new ContentTypeFileProcessor()
        String result = processor.createFileForPublication(publication,baseFile,fragments)
        outputFile.getParentFile().mkdirs()
        outputFile.text = result
    }
}
