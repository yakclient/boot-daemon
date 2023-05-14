import org.w3c.dom.NodeList

plugins {
    kotlin("jvm") version "1.7.10"
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.6.0"
}

group = "net.yakclient.components"

repositories {
    mavenLocal()
    maven {
        isAllowInsecureProtocol = true
        url = uri("http://maven.yakclient.net/snapshots")
    }
    mavenCentral()
}

dependencies {
    implementation("net.yakclient:archives:1.1-SNAPSHOT")
    implementation("net.yakclient:archives-mixin:1.1-SNAPSHOT") {
        isChanging = true
    }

    implementation("io.arrow-kt:arrow-core:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.5")
    implementation("net.yakclient:boot:1.0-SNAPSHOT") {
        exclude(group = "com.durganmcbroom", module = "artifact-resolver")
        exclude(group = "com.durganmcbroom", module = "artifact-resolver-simple-maven")

        exclude(group = "com.durganmcbroom", module = "artifact-resolver-jvm")
        exclude(group = "com.durganmcbroom", module = "artifact-resolver-simple-maven-jvm")
        isChanging = true
    }
    implementation("net.yakclient:common-util:1.0-SNAPSHOT") {
        isChanging = true
    }
}

task<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

task<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
}

publishing {
    publications {
        create<MavenPublication>("boot-daemon-maven") {
            from(components["java"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
            artifact("${sourceSets.main.get().resources.srcDirs.first().absoluteFile}${File.separator}component-model.json").classifier =
                "component-model"

            artifactId = "boot-daemon"

            pom {
                packaging = "jar"

                developers {
                    developer {
                        name.set("Durgan McBroom")
                    }
                }
                withXml {
                    val repositoriesNode = asNode().appendNode("repositories")
                    val yakclientRepositoryNode = repositoriesNode.appendNode("repository")
                    yakclientRepositoryNode.appendNode("id", "yakclient")
                    yakclientRepositoryNode.appendNode("url", "http://maven.yakclient.net/snapshots")
                }

                licenses {
                    license {
                        name.set("GNU General Public License")
                        url.set("https://opensource.org/licenses/gpl-license")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/yakclient/boot-daemon")
                    developerConnection.set("scm:git:ssh://github.com:yakclient/boot-daemon.git")
                    url.set("https://github.com/yakclient/boot-daemon")
                }
            }
        }
    }
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.dokka")

    version = "1.0-SNAPSHOT"



    publishing {
        repositories {
            if (project.hasProperty("maven-user") && project.hasProperty("maven-secret")) maven {
                logger.quiet("Maven user and password found.")
                val repo = if ((version as String).endsWith("-SNAPSHOT")) "snapshots" else "releases"

                isAllowInsecureProtocol = true

                url = uri("http://maven.yakclient.net/$repo")

                credentials {
                    username = project.findProperty("maven-user") as String
                    password = project.findProperty("maven-secret") as String
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            } else logger.quiet("Maven user and password not found.")
        }
    }

    kotlin {
        explicitApi()
    }

    configurations.all {
        resolutionStrategy.cacheChangingModulesFor(0, "seconds")
    }

    dependencies {
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))
        testImplementation(kotlin("test"))
    }

    tasks.compileKotlin {
        destinationDirectory.set(tasks.compileJava.get().destinationDirectory.asFile.get())

        kotlinOptions {
            jvmTarget = "17"
        }
    }

    tasks.compileTestKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.compileJava {
        targetCompatibility = "17"
        sourceCompatibility = "17"
    }
}