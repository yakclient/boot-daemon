package net.yakclient.components.daemon

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import net.yakclient.boot.BootInstance
import net.yakclient.boot.component.ComponentConfiguration
import net.yakclient.boot.component.ComponentFactory
import net.yakclient.boot.component.ComponentInstance
import net.yakclient.boot.component.artifact.SoftwareComponentArtifactRequest
import net.yakclient.boot.component.artifact.SoftwareComponentDescriptor
import net.yakclient.boot.component.artifact.SoftwareComponentRepositorySettings
import net.yakclient.boot.component.context.ContextNodeValue
import net.yakclient.components.daemon.routing.configureRouting
import net.yakclient.components.daemon.routing.configureSerialization

public class BootDaemon(
    private val boot: BootInstance,
    private val runner: ComponentRunner = ComponentRunner()
) : ComponentInstance<BootDaemonConfiguration> {
    private val factories : MutableMap<SoftwareComponentDescriptor, ComponentFactory<*, *>> =HashMap()
    private val ntEnabled: MutableMap<SoftwareComponentDescriptor, ComponentInstance<*>> = HashMap() // non-transitively enabled
    private lateinit var server : ApplicationEngine

    override fun start() {
        Thread {
            server = embeddedServer(Netty, port = 5000, host = "127.0.0.1", module = {
                configureRouting(this@BootDaemon)
                configureSerialization()
            }).start(wait = true)
        }.start()

        runner.start()
    }

    override fun end() {
        runner.end()
        ntEnabled.forEach {
            it.value.end()
        }
        server.stop()
    }

    public fun cache(descriptor: SoftwareComponentDescriptor, settings: SoftwareComponentRepositorySettings) {
        val request = SoftwareComponentArtifactRequest(
            descriptor
        )
        boot.cache(
            request,
            settings
        )

        factories[descriptor] = requireNotNull(boot.componentGraph.get(request.descriptor).orNull()?.factory) {"Descriptor"}
    }

    public fun start(descriptor: SoftwareComponentDescriptor, context: ContextNodeValue) {
        val factory = requireNotNull(factories[descriptor]) {"Cannot start component: '$descriptor' because its not cached."} as ComponentFactory<ComponentConfiguration, ComponentInstance<ComponentConfiguration>>
        val configuration = factory.parseConfiguration(context)

        val instance = factory.new(configuration)
        runner.run(instance)

        ntEnabled[descriptor] = instance
    }

    public fun end(descriptor: SoftwareComponentDescriptor) {
        ntEnabled[descriptor]?.end()
    }
}