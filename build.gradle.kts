import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RemapSourcesJarTask

plugins {
    id("fabric-loom") version("1.5.7")

    // This dependency is only used to determine the state of the Git working tree so that build artifacts can be
    // more easily identified. TODO: Lazily load GrGit via a service only when builds are performed.
    id("org.ajoberstar.grgit") version("5.0.0")

	id("me.modmuss50.mod-publish-plugin") version "0.3.4"

    id("maven-publish")
}

operator fun String.invoke(): String {
    return (rootProject.properties[this] as String?)!!
}

base {
    archivesName = "archives_base_name"()
}

version = "${"mod_version"()}${getVersionMetadata()}+mc${"minecraft_version"()}"
group = "maven_group"()

apply(from = "${rootProject.projectDir}/gradle/forge.gradle")
apply(from = "${rootProject.projectDir}/gradle/java.gradle")

loom {
    mixin {
        defaultRefmapName = "embeddium-refmap.json"
    }

    accessWidenerPath = file("src/main/resources/sodium.accesswidener")
}

configurations {
    val modIncludeImplementation = create("modIncludeImplementation")

    this["include"].extendsFrom(modIncludeImplementation)
    this["modImplementation"].extendsFrom(modIncludeImplementation)
}

sourceSets {
    val main = getByName("main")
    val api = create("api")
    val legacy = create("legacy")
    val compat = create("compat")
    
    api.apply {
        java {
            compileClasspath += main.compileClasspath
        }
    }

    main.apply {
        java {
            compileClasspath += api.output
            runtimeClasspath += api.output
        }
    }

    legacy.apply {
        java {
            compileClasspath += main.compileClasspath
            compileClasspath += main.output
        }
    }

    compat.apply {
        java {
            compileClasspath += main.compileClasspath
            compileClasspath += main.output
        }
    }
}

loom {
    runs {
        this["client"].apply {
            mods {
                create("archives_base_name"()) {
                    sourceSet(sourceSets["main"])
                    sourceSet(sourceSets["api"])
                }
            }
        }
    }
    createRemapConfigurations(sourceSets["compat"])
}

val apiJar = tasks.register<Jar>("apiJar") {
    archiveClassifier = "api-dev"

    from(sourceSets["api"].output)
}

val remapApiJar = tasks.register<RemapJarTask>("remapApiJar") {
    dependsOn(apiJar)
    archiveClassifier = "api"
    
    input = apiJar.get().archiveFile.get().asFile
    addNestedDependencies = false
}

tasks.build {
    dependsOn(apiJar)
    dependsOn(remapApiJar)
}

tasks.jar {
    from(sourceSets["api"].output.classesDirs)
    from(sourceSets["api"].output.resourcesDir)
    from(sourceSets["legacy"].output.classesDirs)
    from(sourceSets["legacy"].output.resourcesDir)
    from(sourceSets["compat"].output.classesDirs)
    from(sourceSets["compat"].output.resourcesDir)
}

java {
    withSourcesJar()
}

repositories {
    // curseforge
    maven("https://www.cursemaven.com") {
        content {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    //to change the versions see the gradle.properties file
    "minecraft"("com.mojang:minecraft:${"minecraft_version"()}")
    "mappings"(loom.officialMojangMappings())
    "modImplementation"("net.fabricmc:fabric-loader:${"loader_version"()}")

    // Fabric API
    "modIncludeImplementation"(fabricApi.module("fabric-api-base", "fabric_version"()))
    "modIncludeImplementation"(fabricApi.module("fabric-block-view-api-v2", "fabric_version"()))
    "modIncludeImplementation"(fabricApi.module("fabric-rendering-fluids-v1", "fabric_version"()))
    "modIncludeImplementation"(fabricApi.module("fabric-rendering-data-attachment-v1", "fabric_version"()))
    "modIncludeImplementation"(fabricApi.module("fabric-resource-loader-v0", "fabric_version"()))
    "modIncludeImplementation"(fabricApi.module("fabric-renderer-api-v1", "fabric_version"()))
    "modIncludeImplementation"(fabricApi.module("fabric-renderer-indigo", "fabric_version"()))

    // provide mod IDs of former mods/addons
    "include"(project(":stub:sodium"))
    runtimeOnly(project(":stub:sodium"))
    "include"(project(":stub:indium"))
    runtimeOnly(project(":stub:indium"))
}

val remapJar = tasks.withType<RemapJarTask>()["remapJar"]
val remapSourcesJar = tasks.withType<RemapSourcesJarTask>()["remapSourcesJar"]

val copyJarNameConsistent = tasks.register<Copy>("copyJarNameConsistent") {
    from(remapJar) // shortcut for createJar.outputs.files
    into(project.file("build/libs"))
    rename { name -> "embeddium-latest.jar" }
}

val copyJarToBin = tasks.register<Copy>("copyJarToBin") {
    from(remapJar) // shortcut for createJar.outputs.files
    into(rootProject.file("bin"))
    mustRunAfter(copyJarNameConsistent)
}

tasks.named("remapApiJar") {
    mustRunAfter(copyJarNameConsistent)
}

tasks.named("remapSourcesJar") {
    mustRunAfter(copyJarNameConsistent)
}

tasks.build {
    dependsOn(copyJarToBin, copyJarNameConsistent)
}

publishMods {
	file = remapJar.archiveFile
	changelog = "https://github.com/embeddedt/embeddium/wiki/Changelog"
	type = STABLE
    modLoaders.add("forge")
    modLoaders.add("neoforge")

	curseforge {
		projectId = "908741"
		accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
		minecraftVersions.add("minecraft_version"())

		incompatible {
			slug = "rubidium"
		}
	}
	modrinth {
		projectId = "sk9rgfiA"
		accessToken = providers.environmentVariable("MODRINTH_TOKEN")
		minecraftVersions.add("minecraft_version"())

		incompatible {
			slug = "rubidium"
		}
	}

	displayName = "[${"minecraft_version"()}] Embeddium ${"mod_version"()}"
}

fun getVersionMetadata(): String {
	// CI builds only
	if (project.hasProperty("build.release")) {
		return "" // no tag whatsoever
	}

	if (grgit != null) {
		val head = grgit.head()
		var id = head.abbreviatedId

		// Flag the build if the build tree is not clean
		if (!grgit.status().isClean) {
			id += ".dirty"
		}

		return "-git.${id}"
	}

	// No tracking information could be found about the build
	return "-unknown"
}
