package trymicronaut.ioc

import io.micronaut.context.ApplicationContext
import io.micronaut.context.event.ShutdownEvent
import io.micronaut.context.event.StartupEvent
import io.micronaut.runtime.event.annotation.EventListener
import io.micronaut.scheduling.annotation.Async
import javax.inject.Singleton

@Singleton
open class SampleEventListener {

    @EventListener
    @Async
    internal open fun onStartupEvent(event: StartupEvent) {
        println("[${Thread.currentThread().name}] received event: startup")
    }

    @EventListener
    internal fun onShutdownEvent(event: ShutdownEvent) {
        println("[${Thread.currentThread().name}] received event: shutdown")
    }

}

fun main() {
    with(ApplicationContext.run()) {
        Thread.sleep(1000)
        stop()
    }
}