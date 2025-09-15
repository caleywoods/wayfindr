plugins {
    kotlin("jvm") version "2.2.0" apply false
    id("dev.kikugie.stonecutter")
    id("fabric-loom") version "1.11-SNAPSHOT" apply false
    id("me.modmuss50.mod-publish-plugin") version "0.8.+" apply false
}

stonecutter active "1.21.4-fabric" /* [SC] DO NOT EDIT */

stonecutter parameters {
    constants.match(node.metadata.project.substringAfterLast('-'), "fabric")
    filters.include("**/*.kt", "**/*.kts", "**/*.java")
}

stonecutter tasks {
    // order("publishModrinth")
    // order("publishCurseforge")
}

for (version in stonecutter.versions.map { it.version }.distinct()) tasks.register("publish$version") {
    group = "publishing"
    dependsOn(stonecutter.tasks.named("publishMods") { metadata.version == version })
}

for (node in stonecutter.tree.nodes) {
    if (node.metadata != stonecutter.current || node.branch.id.isEmpty()) continue

    val loader = node.branch.id.replaceFirstChar { it.uppercase() }
    node.project.tasks.register("runActive$loader") {
        dependsOn("runClient")
        group = "project"
    }
}
