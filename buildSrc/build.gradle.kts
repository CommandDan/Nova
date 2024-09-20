plugins {
    `kotlin-dsl`
}

repositories { 
    mavenCentral()
    maven("https://repo.xenondevs.xyz/releases/")
}

dependencies {
    implementation("org.ow2.asm:asm:9.7")
    implementation("net.fabricmc:access-widener:2.1.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("xyz.xenondevs.commons:commons-gson:1.17")
}

gradlePlugin {
    plugins {
        create("bundler-jar-plugin") {
            id = "xyz.xenondevs.bundler-jar-plugin"
            implementationClass = "bundler.BundlerJarPlugin"
        }
        create("bundler-plugin") {
            id = "xyz.xenondevs.bundler-plugin"
            implementationClass = "bundler.BundlerPlugin"
        }
        create("access-widener-plugin") {
            id = "xyz.xenondevs.access-widener-plugin"
            implementationClass = "accesswidener.AccessWidenerPlugin"
        }
    }
}