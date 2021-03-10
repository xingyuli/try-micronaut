package trymicronaut.ioc

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.context.scope.Refreshable
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import java.text.SimpleDateFormat
import java.util.*
import javax.annotation.PostConstruct

// HINT: class must be open
@Refreshable
open class WeatherService {

    private lateinit var foreCast: String

    @PostConstruct
    fun init() {
        foreCast = "Scattered Clouds " + SimpleDateFormat("dd/MMM/yy HH:mm:ss.SSS").format(Date())
    }

    // HINT: function must be open
    open fun latestForecast(): String = foreCast

}

fun main() {
    with (ApplicationContext.run()) {
        val weatherService = getBean(WeatherService::class.java)
        println(weatherService.latestForecast())
        println(weatherService.latestForecast())

        Thread.sleep(1000)
        publishEvent(RefreshEvent())

        Thread.sleep(100)
        println(weatherService.latestForecast())

        stop()
    }
}