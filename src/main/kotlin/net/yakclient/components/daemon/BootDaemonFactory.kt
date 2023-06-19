package net.yakclient.components.daemon

import net.yakclient.boot.BootInstance
import net.yakclient.boot.component.ComponentFactory
import net.yakclient.boot.component.context.ContextNodeValue

public class BootDaemonFactory(
    boot: BootInstance
) : ComponentFactory<BootDaemonConfiguration, BootDaemon>(
    boot
) {
    override fun parseConfiguration(tree: ContextNodeValue): BootDaemonConfiguration {
        return BootDaemonConfiguration()
    }

    override fun new(configuration: BootDaemonConfiguration): BootDaemon {
        return BootDaemon(boot)
    }
}