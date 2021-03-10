package trymicronaut.aop

import io.micronaut.aop.Adapter
import io.micronaut.aop.Around
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.context.annotation.Type
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.core.annotation.Internal
import javax.inject.Singleton

@Singleton
class TransactionalEventInterceptor : MethodInterceptor<Any, Any> {

    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
        val phase = context.stringValue(TransactionalEventListener::class.java)
        println("TransactionalEventInterceptor: $phase")

        context.proceed()
        return null
    }

}

@Around
@Type(TransactionalEventInterceptor::class)
@Internal
annotation class TransactionalEventAdvice

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Adapter(ApplicationEventListener::class)
@TransactionalEventAdvice // combine Around with Adapter
annotation class TransactionalEventListener(val value: String = "AFTER_COMMIT")
