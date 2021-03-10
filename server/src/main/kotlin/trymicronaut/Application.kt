package trymicronaut

import io.micronaut.runtime.Micronaut
import trymicronaut.grpc.hello.HelloServer

object Application {

    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty("custom.grpc.port", "8980")

        val applicationContext = Micronaut.build()
                .packages("trymicronaut")
                .mainClass(Application.javaClass)
                .start()

        with(applicationContext.getBean(HelloServer::class.java)) {
            start()
            blockUntilShutdown()
        }
    }

}