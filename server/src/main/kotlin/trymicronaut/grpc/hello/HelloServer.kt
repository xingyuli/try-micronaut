package trymicronaut.grpc.hello

import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import io.micronaut.context.annotation.Value
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.inject.Singleton

@Singleton
class HelloServer(@Value("\${custom.grpc.port}") private val port: Int) {

    private val server: Server

    init {
        server = ServerBuilder.forPort(port)
            .addService(HelloService())
            .build()
    }

    fun start() {
        server.start()
        logger.info("Server started, listening on $port")
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down")
                try {
                    this@HelloServer.stop()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                System.err.println("*** server shut down")
            }
        })
    }

    private fun stop() {
        server.shutdown().awaitTermination(30, TimeUnit.SECONDS)
    }

    fun blockUntilShutdown() {
        server.awaitTermination()
    }

    private class HelloService : HelloServiceGrpc.HelloServiceImplBase() {
        override fun greeting(request: Person, responseObserver: StreamObserver<GreetingResponse?>) {
            responseObserver.onNext(
                GreetingResponse.newBuilder().setText("Hello " + request.name.toString() + "!").build()
            )
            responseObserver.onCompleted()
        }
    }

    companion object {

        private val logger = Logger.getLogger(HelloServer::class.java.name)

        @JvmStatic
        fun main(args: Array<String>) {
            val server = HelloServer(8980)
            server.start()
            server.blockUntilShutdown()
        }

    }

}