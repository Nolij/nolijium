pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        mavenCentral()
        gradlePluginPortal()
    }
}

val minecraft_version: String by settings
rootProject.name = "nolijium-${minecraft_version}"

include("stub:sodium")
include("stub:indium")
