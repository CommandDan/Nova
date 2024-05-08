plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.dokka)
    id("xyz.xenondevs.loader-jar-plugin")
}

fun RepositoryHandler.configureRepos() {
    mavenLocal { content { includeGroupAndSubgroups("xyz.xenondevs") }}
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.xenondevs.xyz/releases")
}

repositories { configureRepos() }

subprojects {
    group = "xyz.xenondevs.nova"
    version = properties["version"] as String
    
    repositories { configureRepos() }
    
    // The following excludes the deprecated kotlin-stdlib-jdk8 and kotlin-stdlib-jdk7
    // Since Kotlin 1.8.0, those are merged into kotlin-stdlib
    // Due to the way our library loader works, excluding these is required to prevent version conflicts
    dependencies {
        configurations.all {
            exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
            exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
        }
    }
    
    tasks {
        register<Jar>("sources") {
            dependsOn(JavaPlugin.CLASSES_TASK_NAME)
            from("src/main/java", "src/main/kotlin")
            archiveClassifier.set("sources")
        }
        
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile> {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                
                freeCompilerArgs.addAll(
                    "-Xjvm-default=all", // Emit JVM default methods for interface declarations with bodies
                    
                    // experimental features
                    "-opt-in=kotlin.io.path.ExperimentalPathApi",
                    "-opt-in=kotlin.time.ExperimentalTime",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
                )
                
                if (project.hasProperty("release")) {
                    freeCompilerArgs.addAll(
                        "-Xlambdas=indy", // Generate lambdas using invokedynamic with LambdaMetafactory.metafactory
                        "-Xsam-conversions=indy", // Generate SAM conversions using invokedynamic with LambdaMetafactory.metafactory
                        "-Xno-call-assertions", // Don't generate not-null assertions for arguments of platform types
                        "-Xno-receiver-assertions", // Don't generate not-null assertion for extension receiver arguments of platform types
                        "-Xno-param-assertions", // Don't generate not-null assertions on parameters of methods accessible from Java
                    )
                } else {
                    freeCompilerArgs.addAll(
                        "-Xdebug" // https://kotlinlang.org/docs/debug-coroutines-with-idea.html#optimized-out-variables
                    )
                }
            }
        }
    }
}