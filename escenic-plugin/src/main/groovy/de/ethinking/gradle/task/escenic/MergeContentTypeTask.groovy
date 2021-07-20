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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction

class MergeContentTypeTask extends DefaultTask {

    @Input
    String publication

    @InputFile
    File baseFile

    @InputFiles
    FileCollection fragments

    @OutputFile
    File outputFile

    @Internal
    String encoding = 'UTF-8'

    @Internal
    ContentTypeFileProcessor processor = new ContentTypeFileProcessor()

    @TaskAction
    def merge(){
        String result = processor.createFileForPublication(publication,baseFile,fragments)
        outputFile.getParentFile().mkdirs()
        outputFile.setText(result, encoding)
    }
}
