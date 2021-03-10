package trymicronaut.benchmarks

import io.grpc.Channel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import io.micronaut.context.ApplicationContext
import org.HdrHistogram.Histogram
import trymicronaut.grpc.hello.GreetingResponse
import trymicronaut.grpc.hello.HelloServiceGrpc
import trymicronaut.grpc.hello.Person
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Singleton
class AsyncGrpcClient {

    lateinit var config: GrpcConfig

    fun run() {
        val channels = (1..config.channels).map {
            ManagedChannelBuilder.forTarget(config.address).usePlaintext().build()
        }

        warmup(channels)

        val startTime = System.nanoTime()
        val endTime = startTime + TimeUnit.SECONDS.toNanos(config.duration)
        val histograms = doBenchmark(channels, endTime)
        val elapsedTime = System.nanoTime() - startTime

        val merged = merge(histograms)

        printStats(merged, elapsedTime)
    }

    private fun warmup(channels: List<Channel>) {
        val endTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(config.warmupDuration)
        doBenchmark(channels, endTime)
        System.gc()
    }

    private fun doBenchmark(channels: List<Channel>, endTime: Long): List<Histogram> {
        val futures = mutableListOf<Future<Histogram>>()

        for (i in 0 until config.channels) {
            for (j in 1..config.outstandingRpcs) {
                val channel = channels[i]
                futures.add(doUnaryCall(channel, endTime))
            }
        }

        return futures.map { it.get() }
    }

    private fun doUnaryCall(channel: Channel, endTime: Long): Future<Histogram> {
        val histogram = Histogram(HISTOGRAM_MAX_VALUE, HISTOGRAM_PRECISION)
        val f = CompletableFuture<Histogram>()

        val stub = HelloServiceGrpc.newStub(channel)

        val person = Person.newBuilder().setName("viclau").build()

        stub.greeting(person, object : StreamObserver<GreetingResponse> {

            var lastCall = System.nanoTime()

            override fun onNext(value: GreetingResponse?) {
            }

            override fun onError(t: Throwable?) {
                f.completeExceptionally(t)
            }

            override fun onCompleted() {
                val now = System.nanoTime()
                // Record the latencies in microseconds
                histogram.recordValue((now - lastCall) / 1000)
                lastCall = now

                if (endTime - now > 0) {
                    stub.greeting(person, this)
                } else {
                    f.complete(histogram)
                }
            }

        })

        return f
    }

    private fun merge(histograms: List<Histogram>): Histogram {
        val merged = Histogram(HISTOGRAM_MAX_VALUE, HISTOGRAM_PRECISION)
        for (histogram in histograms) {
            for (value in histogram.allValues()) {
                val latency = value.valueIteratedTo
                val count = value.countAtValueIteratedTo
                merged.recordValueWithCount(latency, count)
            }
        }
        return merged
    }

    private fun printStats(histogram: Histogram, elapsedTime: Long) {
        val latency50 = histogram.getValueAtPercentile(50.0)
        val latency90 = histogram.getValueAtPercentile(90.0)
        val latency95 = histogram.getValueAtPercentile(95.0)
        val latency99 = histogram.getValueAtPercentile(99.0)
        val latency999 = histogram.getValueAtPercentile(99.9)
        val latencyMax = histogram.getValueAtPercentile(100.0)
        val queriesPerSecond = histogram.totalCount * 1000000000L / elapsedTime
        val values = StringBuilder()
        values.append("Channels:                       ").append(config.channels).append('\n')
            .append("Outstanding Rpcs per Channel:   ")
            .append(config.outstandingRpcs).append('\n')
//            .append("Server Payload Size:            ").append(config.serverPayload).append('\n')
//            .append("Client Payload Size:            ").append(config.clientPayload).append('\n')
            .append("50%ile Latency (in micros):     ").append(latency50).append('\n')
            .append("90%ile Latency (in micros):     ").append(latency90).append('\n')
            .append("95%ile Latency (in micros):     ").append(latency95).append('\n')
            .append("99%ile Latency (in micros):     ").append(latency99).append('\n')
            .append("99.9%ile Latency (in micros):   ").append(latency999).append('\n')
            .append("Maximum Latency (in micros):    ").append(latencyMax).append('\n')
            .append("QPS:                            ").append(queriesPerSecond).append('\n')
        println(values)
    }

}

data class GrpcConfig(
    val address: String,
    val channels: Int,
    val outstandingRpcs: Int,
    val warmupDuration: Long,
    val duration: Long
)

fun main(args: Array<String>) {
    val config = with(args) {
        val configurationKeys = filterIndexed { index, _ -> index % 2 == 0 }
        val configurationValues = filterIndexed { index, _ -> index % 2 == 1 }

        val configurations = configurationKeys.zip(configurationValues).toMap().withDefault { null }

        GrpcConfig(
            address = configurations["address"]!!,
            channels = configurations["channels"]?.toIntOrNull() ?: 4,
            outstandingRpcs = configurations["outstanding_rpcs"]?.toIntOrNull() ?: 10,
            warmupDuration = configurations["warmup_duration"]?.toLongOrNull() ?: 10,
            duration = configurations["duration"]?.toLongOrNull() ?: 60
        )
    }

    with(ApplicationContext.run()) {
        val client = getBean(AsyncGrpcClient::class.java)
        client.config = config
        client.run()
        stop()
    }
}