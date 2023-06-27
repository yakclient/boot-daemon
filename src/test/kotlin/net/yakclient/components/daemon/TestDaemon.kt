package net.yakclient.components.daemon

import com.durganmcbroom.artifact.resolver.simple.maven.SimpleMavenRepositorySettings
import com.durganmcbroom.artifact.resolver.simple.maven.layout.mavenLocal
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.yakclient.boot.component.ComponentConfiguration
import net.yakclient.boot.component.ComponentInstance
import net.yakclient.boot.component.artifact.SoftwareComponentArtifactRequest
import net.yakclient.boot.component.artifact.SoftwareComponentDescriptor
import net.yakclient.boot.component.context.ContextNodeTypes
import net.yakclient.boot.test.testBootInstance
import java.lang.IllegalStateException
import kotlin.test.Test

class TestDaemon {
    @Test
    fun `Daemon start`() {
        val boot = testBootInstance(mapOf())
        println(boot.location)

        val daemon = BootDaemon(boot)
        daemon.start()
    }

    @Test
    fun `Test component runner`() {
        val runner = ComponentRunner()
        val thread = Thread(runner::start).also(Thread::start)

        val component = object : ComponentInstance<ComponentConfiguration> {
            private var started = false
            override fun end() {
                if (!started) throw IllegalStateException("Not started yet, cant end!")
                println("Ending")
            }

            override fun start() {
                if (started) throw IllegalStateException("Already started, cant do it again!")
                started = true
                println("Starting")
            }
        }

        runner.run(component)
        Thread.sleep(10)
        runner.end()
        component.end()

        check(!thread.isAlive) {"Component runner didnt end successfully"}
    }

    @Test
    fun `Test cache and run`() {
        val boot = testBootInstance(mapOf())
        val runner = ComponentRunner()
        Thread {
            runner.start()
        }.start()

        val daemon = BootDaemon(boot, runner)

        val yakDescriptor = SoftwareComponentDescriptor.parseDescription("net.yakclient.components:yak:1.0-SNAPSHOT")!!
        daemon.cache(
                SoftwareComponentArtifactRequest(yakDescriptor),
                SimpleMavenRepositorySettings.local()
        )

        val mapper = ObjectMapper()
        val resource = mapper.readValue<Map<String, Any>>("""
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

        daemon.start(yakDescriptor, ContextNodeTypes.newValueType(resource))
    }
}