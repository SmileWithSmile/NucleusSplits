plugins {
    java
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
}

val modVersion: String by project
val mavenGroup: String by project
val archivesBaseName: String by project
val minecraftVersion: String by project
val fabricLoaderVersion: String by project
val fabricApiVersion: String by project

group = mavenGroup
version = "$modVersion+mc26.1"
base.archivesName.set(archivesBaseName)

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net") {
        content { includeGroupAndSubgroups("net.fabricmc") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$fabricLoaderVersion")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:5.14.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

loom {
    runs {
        named("client") {
            isIdeConfigGenerated = true
            vmArgs("-Xmx2G")
        }
        removeIf { it.name == "server" }
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to version,
        "fabricLoader" to fabricLoaderVersion,
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.test {
    useJUnitPlatform()
}
