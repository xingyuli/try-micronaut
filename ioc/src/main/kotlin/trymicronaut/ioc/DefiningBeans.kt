package trymicronaut.ioc

import io.micronaut.context.BeanContext
import javax.inject.Named
import javax.inject.Singleton

interface Engine {
    val cylinders: Int
    fun start(): String
}

@Singleton
class V6Engine: Engine {

    override val cylinders = 6

    override fun start(): String {
        return "Starting V6"
    }

}

@Singleton
class V8Engine : Engine {

    override val cylinders = 8

    override fun start(): String {
        return "Starting V8"
    }

}

@Singleton
class Vehicle(@Named("v6") private val engine: Engine) {
    fun start(): String = engine.start()
}

fun main() {
    val context = BeanContext.run()
    val vehicle = context.getBean(Vehicle::class.java)
    println(vehicle.start())
}