package net.yakclient.components.daemon

import io.ktor.util.logging.*
import net.yakclient.boot.component.ComponentInstance
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public class ComponentRunner {
    private val logger = KtorSimpleLogger("yakclient.daemon.component.runner")
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val components: Queue<ComponentInstance<*>> = LinkedList()
    private var shouldEnd = false

    public fun end() {
        lock.withLock {
            shouldEnd = true
            condition.signal()
        }
    }

    public fun run(component: ComponentInstance<*>) {
        lock.withLock {
            components.offer(component)
            condition.signal()
        }
    }

    public fun start() {
        while (true) {
            lock.withLock {
                // Wait until the queue is not empty
                while (components.isEmpty() && !shouldEnd) {
                    condition.await()
                }

                if (shouldEnd) return
                // Retrieve and execute the closure from the queue
                val component = components.poll()
                val result = runCatching {
                    component.start()
                }
                if (result.isFailure) { logger.error(result.exceptionOrNull().toString()) }
            }
        }
    }
}