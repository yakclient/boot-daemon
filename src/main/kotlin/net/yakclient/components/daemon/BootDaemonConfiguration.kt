package net.yakclient.components.daemon

import net.yakclient.boot.component.ComponentConfiguration
import net.yakclient.boot.component.artifact.SoftwareComponentDescriptor
import net.yakclient.boot.component.context.ContextNodeValue

public class BootDaemonConfiguration(
//        val components: List<SoftwareComponentDescriptor>
) : ComponentConfiguration

public data class BootDaemonConfigurationComponent(
        val descriptor: SoftwareComponentDescriptor,
        val configuration: ContextNodeValue,
)