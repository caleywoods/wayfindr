@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.2.0"
    id("fabric-loom")
    id("me.modmuss50.mod-publish-plugin")
}

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["version"] = prop("mod.version")
        this["minecraft"] = prop("deps.minecraft")
    }

    filesMatching(listOf("fabric.mod.json")) {
        expand(props)
    }
}

version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
base.archivesName = property("mod.id") as String

loom {
    runs.named("client") {
        programArgs("--username=dfnkt")
    }
}

repositories {
    mavenLocal()
}

dependencies {
    minecraft("com.mojang:minecraft:${property("deps.minecraft")}")
    mappings("net.fabricmc:yarn:${property("deps.yarn")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric-loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric-api")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.fabric-kotlin")}")
    // implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
}

tasks {
    processResources {
        exclude("**/neoforge.mods.toml", "**/mods.toml")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

java {
    val javaCompat = JavaVersion.VERSION_21
    sourceCompatibility = javaCompat
    targetCompatibility = javaCompat
}

publishMods {
    file = tasks.remapJar.map { it.archiveFile.get() }
    additionalFiles.from(tasks.remapSourcesJar.map { it.archiveFile.get() })

    type = BETA
    displayName = "${property("mod.name")} ${property("mod.version")} for ${property("deps.minecraft")} Fabric"
    version = "${property("mod.version")}+${property("deps.minecraft")}-fabric"
    changelog = provider { rootProject.file("CHANGELOG.md").readText() }
    modLoaders.add("fabric")

    if (project.hasProperty("publish.modrinth")) {
        modrinth {
            projectId = property("publish.modrinth") as String
            minecraftVersions.add(property("deps.minecraft") as String)
            requires("fabric-api", "fabric-language-kotlin")
        }
    }

    if (project.hasProperty("publish.curseforge")) {
        curseforge {
            projectId = property("publish.curseforge") as String
            minecraftVersions.add(property("deps.minecraft") as String)
            requires("fabric-api", "fabric-language-kotlin")
        }
    }
}
