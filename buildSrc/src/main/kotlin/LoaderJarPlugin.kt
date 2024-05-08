
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

class LoaderJarPlugin : Plugin<Project> {
    
    override fun apply(project: Project) {
        val novaLoader = project.project(":nova-loader")
        val nova = project.project(":nova")
        val novaAPI = project.project(":nova-api")
        val hooks = project.subprojects.filter { it.name.startsWith("nova-hook-") }
        
        project.tasks.register<BuildLoaderJarTask>("loaderJar") {
            this.group = "build"
            this.novaLoader = novaLoader
            this.nova = nova
            this.novaApi = novaAPI
            this.hooks = hooks
            
            dependsOn(
                novaLoader.tasks.named("classes"),
                nova.tasks.named("classes"),
                novaAPI.tasks.named("classes"),
                hooks.map { it.tasks.named("classes") }
            )
        }
    }
    
}