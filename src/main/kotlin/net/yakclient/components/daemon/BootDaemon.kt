package net.yakclient.components.daemon

import net.yakclient.boot.component.ComponentContext
import net.yakclient.boot.component.SoftwareComponent

public class BootDaemon : SoftwareComponent {
    override fun onDisable() {}

    override fun onEnable(context: ComponentContext) {
        val rawComponents = context.configuration["components"] ?:

        val components = rawComponents

        context.boot.cache(

        )
    }
}