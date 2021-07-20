package de.ethinking.gradle.task.escenic

import de.ethinking.gradle.extension.escenic.EscenicExtension
import de.ethinking.gradle.report.EscenicReport
import de.ethinking.gradle.extension.escenic.ExtensionUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.logging.Logging
import org.gradle.api.logging.Logger
import groovy.json.*


class EscenicReportTask extends DefaultTask {

    @Input
    def EscenicExtension escenicExtension
    @InputFile
    def File reportBase
    static Logger LOG = Logging.getLogger(EscenicReportTask.class)


    @TaskAction
    def report(){
        EscenicReport escenicReport = createReport()
        String jsonReport = JsonOutput.toJson(escenicReport)
        createReportWebapp(jsonReport, reportBase)
    }


    def createReportWebapp(String jsonReport,File reportBase){

        File reportFile = new File(reportBase,"report.js")
        reportBase.mkdirs()
        if(reportFile.exists()){
            reportFile.delete()
        }
        reportFile.createNewFile()
        reportFile << "var report="+JsonOutput.prettyPrint(jsonReport)+";\n"

        File reportData = new File(reportBase,"report.json")
        if(reportData.exists()){
            reportData.delete()
        }
        reportData.createNewFile()
        reportData << jsonReport

        File webinf = new File(reportBase,"WEB-INF")
        webinf.mkdirs()
        explodeReportWebapp("/report/escenic-report.zip", reportBase)
    }

    def explodeReportWebapp(String resource,File base){

        ZipInputStream zipStream= null
        OutputStream out = null

        try {
            zipStream=new ZipInputStream(this.class.getResourceAsStream(resource));
            // Get the first entry
            ZipEntry entry = null;
            while ((entry = zipStream.getNextEntry()) != null) {
                String outFilename = entry.getName();

                if (entry.isDirectory()) {
                    new File(base, outFilename).mkdirs();
                } else {
                    out = new FileOutputStream(new File(base,outFilename));
                    // Transfer bytes from the ZIP file to the output file
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = zipStream.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                }
            }
        } catch(IOException e){
           LOG.error("Error on processing resource:"+resource,e)
        } finally {
            // Close the stream
            if (zipStream != null) {
                zipStream.close();
            }
            if (out != null) {
                out.close();
            }

        }

    }

    def createReport(){

        File repositoryBase = escenicExtension.getLocalRepositoryLocationFile()
        File engineSource = new File(repositoryBase,"engine-source")
        EscenicReport report = createReport(engineSource)
        report.name = project.getName()
        File plugins = new File(engineSource,"plugins")
        if(plugins.exists()){
            plugins.listFiles().each { File plugin ->
                if(plugin.isDirectory()){
                    EscenicReport pluginReport = createReport(plugin)
                    if(pluginReport){
                        pluginReport.name = ExtensionUtils.transformPluginDirectoryToName(plugin.getName())
                        report.pluginReports.add(pluginReport)
                    }
                }
            }
        }
        return report
    }


    def EscenicReport createReport(File directory){
        EscenicReport report = new EscenicReport()
        File releaseNoteDirectory = new File(directory,"releasenotes")
        if(releaseNoteDirectory.exists()){
            releaseNoteDirectory.listFiles().each { File releaseNote ->
                if(releaseNote.isFile()){
                    report.releaseNotes.put(releaseNote.getName(),releaseNote.getAbsolutePath())
                }
            }
        }

        searchExtension("engine", directory,"lib", report)
        searchExtension("studio", directory,"studio/lib", report)
        searchExtension("presentation-core", directory,"template/WEB-INF/lib", report)
        searchExtension("presentation", directory,"publication/webapp/WEB-INF/lib", report)
        searchExtension("wars", directory,"wars", report)
        searchExtension("webapps", directory,"webapps", report)
        searchExtension("webservice-extensions", directory," webservice-extensions/webapp", report)
        searchExtension("webservice", directory,"webservice/webapp", report)
        searchExtension("escenic", directory,"escenic/webapp", report)
        searchDocumentation(directory, report)

        return report
    }

    private searchDocumentation(File directory, EscenicReport report) {
        File documentation = new File(directory,"documentation")
        if(documentation.exists()){
            findDocumentation(documentation, report.documentation)
        }
        File apidocs = new File(directory,"apidocs")
        if(apidocs.exists()){
            findDocumentation(apidocs, report.documentation)
        }
    }

    def findDocumentation(File parent, Map<String,Map<String,String>> documentation){
        if(parent.isDirectory()){
            Map<String,String> docs = new TreeMap<String,String>();
            parent.listFiles().each{ File file ->
                if(file.isFile()&&isDocFile(file)){
                    docs.put(file.getName(), file.getAbsolutePath())
                }else if(file.isDirectory()){
                    findDocumentation(file, documentation)
                }
            }
            if(!docs.isEmpty()){
                documentation.put(parent.getName(), docs)
            }
        }
    }


    def isDocFile(File file){
        if(file.getName().endsWith(".pdf")|| "index.html".equals(file.getName())){
            return true;
        }
    }
    def searchExtension(String extension,File directory,String location,EscenicReport report){
        File extensionPath = new File(directory,location)
        if(extensionPath.exists()){
            List<String> files = new ArrayList<String>()
            extensionPath.listFiles().each { File lib ->
                if(lib.isFile()){
                    files.add(lib.getName())
                }
            }
            report.extensions.put(extension,files)
        }
    }





}
