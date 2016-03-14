package de.ethinking.gradle.task.escenic

import de.ethinking.gradle.contenttype.ContentTypeFileProcessor
import org.gradle.api.DefaultTask;
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction

class MergeContentTypeTask extends DefaultTask {
   
    @InputFile
    File baseFile
    @Input
    FileCollection fragments
    
    String publication
    @OutputFile
    File outputFile
    
    @TaskAction
    def merge(){
        ContentTypeFileProcessor processor = new ContentTypeFileProcessor()
        String result = processor.createFileForPublication(publication,baseFile,fragments)
        outputFile.getParentFile().mkdirs()
        
    }
}
