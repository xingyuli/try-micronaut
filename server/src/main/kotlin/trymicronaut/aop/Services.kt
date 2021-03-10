package trymicronaut.aop

import io.micronaut.context.event.ApplicationEventPublisher
import javax.inject.Singleton

@Singleton
open class AopService(private val eventPublisher: ApplicationEventPublisher) {

    // With the interceptor and annotation implemented you can then simply
    // apply the annotation to the target classes
    @TransactionalAdvice
    open fun testTx() {
        println("testTx invoked")

        eventPublisher.publishEvent(NewBookEvent("b-123"))
    }

    @TransactionalEventListener
    open fun onNewBook(event: NewBookEvent) {
        println("book = ${event.book}")
    }

    data class NewBookEvent(val book: String)

}