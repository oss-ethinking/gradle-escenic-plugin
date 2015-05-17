package de.ethinking.gradle.plugin.escenicimport org.gradle.api.Plugin
import org.gradle.api.Project



class EscenicGafferPlugin implements Plugin<Project> {    @Override    public void apply(Project project) {        project.configure(project) {            project.afterEvaluate {                DependencyClosures.addEscenicDistributionClosure(project)            }        }    }
}
