import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
    id("com.github.johnrengelman.shadow") version "8.1.0"
    id("cl.franciscosolis.blossom-extended") version "1.3.1"

    kotlin("jvm") version "1.8.10"
    id("org.jetbrains.dokka") version "1.8.10"
}

val env = project.rootProject.file(".env").let { file ->
    if(file.exists()) file.readLines().filter { it.isNotBlank() && !it.startsWith("#") && it.split("=").size == 2 }.associate { it.split("=")[0] to it.split("=")[1] } else emptyMap()
}.toMutableMap().apply { putAll(System.getenv()) }

val projectVersion = env["VERSION"] ?: "0.2.0-SNAPSHOT"

group = "xyz.theprogramsrc"
version = projectVersion
description = "Command Builder module for SimpleCore API."

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://s01.oss.sonatype.org/content/groups/public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/releases/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://jitpack.io/")
}

dependencies {
    compileOnly("xyz.theprogramsrc:simplecoreapi:0.6.2-SNAPSHOT")
    compileOnly("xyz.theprogramsrc:translationsmodule:0.2.0-SNAPSHOT")
    compileOnly("xyz.theprogramsrc:tasksmodule:0.2.0-SNAPSHOT")
    compileOnly("xyz.theprogramsrc:filesmodule:0.2.0-SNAPSHOT")

    compileOnly("org.spigotmc:spigot-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.19-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.1.2-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
}


tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
        exclude("**/*.kotlin_metadata")
        exclude("**/*.kotlin_builtins")

        archiveBaseName.set(rootProject.name)
        archiveClassifier.set("")
    }

    test {
        useJUnitPlatform()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        withSourcesJar()
        withJavadocJar()
    }

    compileKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    compileTestKotlin {
        kotlinOptions {
            jvmTarget = "11"
        }
    }

    compileJava {
        options.encoding = "UTF-8"
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    copy {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    dokkaHtml {
        outputDirectory.set(file(project.buildDir.absolutePath + "/dokka"))

    }
}

configurations {
    testImplementation {
        extendsFrom(configurations.compileOnly.get())
    }
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc, tasks.dokkaHtml)
    from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    repositories {
        if (env["ENV"] == "prod") {
            if (env.containsKey("GITHUB_ACTOR") && env.containsKey("GITHUB_TOKEN")) {
                maven {
                    name = "GithubPackages"
                    url = uri("https://maven.pkg.github.com/TheProgramSrc/SimpleCore-${rootProject.name}")
                    credentials {
                        username = env["GITHUB_ACTOR"]
                        password = env["GITHUB_TOKEN"]
                    }
                }
            }
        } else {
            mavenLocal()
        }
    }

    publications {
        create<MavenPublication>("shadow") {
            project.extensions.configure<ShadowExtension> {
                artifactId = rootProject.name.lowercase()

                component(this@create)
                artifact(dokkaJavadocJar)
                artifact(tasks.kotlinSourcesJar)

                pom {
                    name.set(rootProject.name)
                    description.set(project.description)
                    url.set("https://github.com/TheProgramSrc/SimpleCore-${rootProject.name}")

                    licenses {
                        license {
                            name.set("GNU GPL v3")
                            url.set("https://github.com/TheProgramSrc/SimpleCore-${rootProject.name}/blob/master/LICENSE")
                        }
                    }

                    developers {
                        developer {
                            id.set("ImFran")
                            name.set("Francisco Solis")
                            email.set("imfran@duck.com")
                        }
                    }

                    scm {
                        url.set("https://github.com/TheProgramSrc/SimpleCore-${rootProject.name}")
                    }
                }
            }
        }
    }
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))

            username.set(env["SONATYPE_USERNAME"])
            password.set(env["SONATYPE_PASSWORD"])
        }
    }
}

tasks.withType<PublishToMavenRepository> {
    dependsOn(tasks.test, tasks.kotlinSourcesJar, dokkaJavadocJar, tasks.jar, tasks.shadowJar)
}

tasks.withType<PublishToMavenLocal> {
    dependsOn(tasks.test, tasks.kotlinSourcesJar, tasks.jar, dokkaJavadocJar, tasks.shadowJar)
}