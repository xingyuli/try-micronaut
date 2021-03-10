package trymicronaut.aop

import io.micronaut.aop.Around
import io.micronaut.aop.MethodInterceptor
import io.micronaut.aop.MethodInvocationContext
import io.micronaut.context.annotation.Type
import io.micronaut.core.annotation.AnnotationValue
import io.micronaut.core.annotation.AnnotationValueBuilder
import io.micronaut.core.annotation.Internal
import io.micronaut.inject.annotation.NamedAnnotationMapper
import io.micronaut.inject.visitor.VisitorContext
import javax.inject.Singleton

// The first step to defining Around advice is to implement a MethodInterceptor.
@Singleton
class TransactionalInterceptor : MethodInterceptor<Any, Any> {

    override fun intercept(context: MethodInvocationContext<Any, Any>): Any? {
        val qualifier = context.executableMethod.stringValue(TransactionalAdvice::class.java)
        println(qualifier)

        val retVal: Any?
        try {
            retVal = context.proceed()
        } catch (e: Exception) {
            // completeTransactionAfterThrowing
            throw e
        } finally {
            // cleanupTransactionInfo
        }
        // commitTransactionAfterReturning
        println("commitTransactionAfterReturning")
        return retVal
    }

}


// To put the new MethodInterceptor to work the next step is to define an
// annotation that will trigger the MethodInterceptor.
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Around
@Type(TransactionalInterceptor::class)
//@Internal
annotation class TransactionalAdvice(val value: String = "")

// for working properly:
//   META-INF/services/io.micronaut.inject.annotation.AnnotationMapper
// is needed, and must be placed in a separate project for annotation
// processing
class JtaTransactionMapper : NamedAnnotationMapper {

    override fun getName(): String = "javax.transaction.Transactional"

    override fun map(
        annotation: AnnotationValue<Annotation>?,
        visitorContext: VisitorContext?
    ): MutableList<AnnotationValue<*>> {
        val builder: AnnotationValueBuilder<Annotation> = AnnotationValue.builder("trymicronaut.aop.TransactionalAdvice")
        return mutableListOf(builder.build())
    }

}