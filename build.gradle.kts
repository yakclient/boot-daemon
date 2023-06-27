plugins {
    kotlin("jvm") version "1.7.10"
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.6.0"
//    id("io.ktor.plugin") version "2.3.0"
//    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.0"
}

group = "net.yakclient.components"

repositories {
    maven {
        isAllowInsecureProtocol = true
        url = uri("http://maven.yakclient.net/snapshots")
    }
    maven {
        name = "Durgan McBroom GitHub Packages"
        url = uri("https://maven.pkg.github.com/durganmcbroom/artifact-resolver")
        credentials {
            username = project.findProperty("dm.gpr.user") as? String
                    ?: throw IllegalArgumentException("Need a Github package registry username!")
            password = project.findProperty("dm.gpr.key") as? String
                    ?: throw IllegalArgumentException("Need a Github package registry key!")
        }
    }
    mavenCentral()
}

tasks.wrapper {
    gradleVersion = "8.1.1"
}

val jarDependency by configurations.creating

fun DependencyHandlerScope.packInJar(notation: String) {
    implementation(notation)
    jarDependency(notation)
}

val bootConfiguration by configurations.creating

dependencies {
    implementation("com.durganmcbroom:artifact-resolver:1.0-SNAPSHOT") {
        isChanging = true
    }
    implementation("com.durganmcbroom:artifact-resolver-simple-maven:1.0-SNAPSHOT") {
        isChanging = true
    }
    implementation("io.arrow-kt:arrow-core:1.1.2")
    implementation("net.yakclient:boot:1.0-SNAPSHOT") {
        exclude(group = "com.durganmcbroom", module = "artifact-resolver")
        exclude(group = "com.durganmcbroom", module = "artifact-resolver-simple-maven")

        exclude(group = "com.durganmcbroom", module = "artifact-resolver-jvm")
        exclude(group = "com.durganmcbroom", module = "artifact-resolver-simple-maven-jvm")
        isChanging = true
    }
    bootConfiguration("net.yakclient:boot:1.0-SNAPSHOT") {
        exclude(group = "com.durganmcbroom", module = "artifact-resolver")
        exclude(group = "com.durganmcbroom", module = "artifact-resolver-simple-maven")

        exclude(group = "com.durganmcbroom", module = "artifact-resolver-jvm")
        exclude(group = "com.durganmcbroom", module = "artifact-resolver-simple-maven-jvm")
        isChanging = true
    }
    implementation("net.yakclient:common-util:1.0-SNAPSHOT") {
        isChanging = true
    }
    packInJar("io.ktor:ktor-server-content-negotiation:2.3.0")
    packInJar("io.ktor:ktor-serialization-jackson:2.3.0")
    packInJar("io.ktor:ktor-server-core-jvm:2.3.0")
    packInJar("io.ktor:ktor-server-netty-jvm:2.3.0")
    packInJar("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
//    packInJar("ch.qos.logback:logback-classic:1.4.8")

    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.0")
    testImplementation("net.yakclient:boot-test:1.0-SNAPSHOT")
    testImplementation("io.ktor:ktor-client-content-negotiation:2.3.0")
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:2.3.0")
}

tasks.jar {
    val dontInclude = bootConfiguration.incoming.resolutionResult.allDependencies.mapTo(HashSet()) {
        it.requested.displayName.split(":")[1]
    }

    jarDependency.filterNot { file ->
        val it = file.name.substring(0, file.name.lastIndexOf('-'))

        dontInclude.contains(it)
    }.forEach { file ->
        from(zipTree(file.absoluteFile)) {
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
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
//    apply(plugin = "org.jetbrains.dokka")

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

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}