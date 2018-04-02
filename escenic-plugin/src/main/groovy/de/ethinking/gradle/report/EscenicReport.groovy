package de.ethinking.gradle.report



class EscenicReport {

    Map<String,String> releaseNotes = new TreeMap<String,String>(Collections.reverseOrder())
    List<EscenicReport> pluginReports = new ArrayList<EscenicReport>()
    String name
    Map<String,List<String>> extensions = new HashMap<String,List<String>>()
    Map<String,Map<String,String>> documentation = new HashMap<String,Map<String,String>>()
    
    
}
