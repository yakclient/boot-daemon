package net.yakclient.components.daemon

import com.durganmcbroom.artifact.resolver.simple.maven.layout.mavenLocal
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import net.yakclient.boot.test.testBootInstance
import net.yakclient.common.util.readInputStream
import net.yakclient.components.daemon.routing.configureRouting
import net.yakclient.components.daemon.routing.configureSerialization
import kotlin.test.Test

class TestWebRun {
    @Test
    fun `Test cache`() = testApplication {
        val boot = testBootInstance(mapOf())
        val daemon = BootDaemon(boot)

        application {
            configureRouting(daemon)
            configureSerialization()
        }


        val client = createClient {
            install(ContentNegotiation) {
                jackson {
                    registerModule(KotlinModule.Builder().build())
                }
            }
        }

        val response =
                client.post("/cache") {
                    contentType(ContentType.Application.Json)
                    setBody("""
                       {
                       "repositoryType": "LOCAL",
                       "repository": "$mavenLocal",
                       "request": "net.yakclient.components:yak:1.0-SNAPSHOT"
                       }
                   """.trimIndent())
                }
        println(response)
        check(response.status.value == 200)
    }

    @Test
    fun `Test start component`() = testApplication {
        val boot = testBootInstance(mapOf())
        val daemon = BootDaemon(boot)

        application {
            configureRouting(daemon)
            configureSerialization()
        }


        val client = createClient {
            install(ContentNegotiation) {
                jackson {
                    registerModule(KotlinModule.Builder().build())
                }
            }
        }

        val cacheResponse =
                client.post("/cache") {
                    contentType(ContentType.Application.Json)
                    setBody("""
                       {
                       "repositoryType": "LOCAL",
                       "repository": "$mavenLocal",
                       "request": "net.yakclient.components:yak:1.0-SNAPSHOT"
                       }
                   """.trimIndent())
                }
        check(cacheResponse.status.value == 200)

        val startResponse =
                client.put("/start?group=net.yakclient.components&artifact=yak&version=1.0-SNAPSHOT") {
                    contentType(ContentType.Application.Json)
                    setBody("""
                        {
                          "mcVersion": "1.19.2",
                          "mcArgs": ["--version", "1.19.2", "--accessToken", ""],
                          "extensions": [
                            {
                              "descriptor": {
                                "groupId": "net.yakclient.extensions",
                                "artifactId": "example-extension",
                                "version": "1.0-SNAPSHOT"
                              },
                              "repository": {
                                "type": "local",
                                "location": "$mavenLocal"
                              }
                            }
                          ]
                        }
                    """.trimIndent())
                }

        println(startResponse)
        check(startResponse.status.value == 200)
    }
}