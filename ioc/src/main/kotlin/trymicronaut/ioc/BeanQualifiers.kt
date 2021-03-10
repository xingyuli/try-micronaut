package trymicronaut.ioc

import io.micronaut.context.BeanContext
import io.micronaut.context.annotation.Primary
import javax.inject.Singleton

interface ColorPicker {
    fun color(): String
}

@Primary
@Singleton
class Green : ColorPicker {
    override fun color(): String = "green"
}

@Singleton
class Blue : ColorPicker {
    override fun color(): String = "blue"
}

fun main() {
    with (BeanContext.run()) {
        println(getBean(ColorPicker::class.java).color())
    }
}
