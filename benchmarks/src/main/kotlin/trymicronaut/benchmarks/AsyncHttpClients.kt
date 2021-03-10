package trymicronaut.benchmarks

import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import org.HdrHistogram.Histogram
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton


// The histogram can record values between 1 microsecond and 1 min.
const val HISTOGRAM_MAX_VALUE = 60000000L

// Value quantization will be no more than 1%. See the README of HdrHistogram for more details.
const val HISTOGRAM_PRECISION = 2

@Singleton
class AsyncHttpClient {

    @Inject
    @field:Client("benchmark")
    private lateinit var client: HttpClient

    lateinit var config: HttpConfig

    fun run() {
        val uri = "/hello/greeting?name=Viclau"

        warmup(uri)

        val startTime = System.nanoTime()
        val endTime = startTime + TimeUnit.SECONDS.toNanos(config.duration)
        val histograms = doBenchmark(uri, endTime)
        val elapsedTime = System.nanoTime() - startTime

        val merged = merge(histograms)

        printStats(merged, elapsedTime)
    }

    private fun warmup(uri: String) {
        val endTime = System.nanoTime() + TimeUnit.SECONDS.toNanos(config.warmupDuration)
        doBenchmark(uri, endTime)
        // I don't know if this helps, but it doesn't hurt trying. We sometimes run warmups
        // of several minutes at full load and it would be nice to start the actual benchmark
        // with a clean heap.
        System.gc()
    }

    private fun doBenchmark(uri: String, endTime: Long): List<Histogram> {
        val futures = mutableListOf<Future<Histogram>>()

        for (i in 1..config.channels) {
            for (j in 1..config.outstandingCalls) {
                if (config.blocking) {
                    futures.add(doBlockingHttpCall(uri, endTime))
                } else {
                    futures.add(doAsyncHttpCall(uri, endTime))
                }
            }
        }

        // Wait for completion
        return futures.map { it.get() }
    }

    private fun doAsyncHttpCall(uri: String, endTime: Long): Future<Histogram> {
        val histogram = Histogram(HISTOGRAM_MAX_VALUE, HISTOGRAM_PRECISION)
        val f = CompletableFuture<Histogram>()

        client.retrieve(HttpRequest.GET<String>(uri).header("Connection", "keep-alive")).subscribe(object : Subscriber<String> {

            var lastCall = System.nanoTime()

            override fun onSubscribe(s: Subscription) {
                s.request(1)
            }

            override fun onNext(t: String?) {
            }

            override fun onError(t: Throwable) {
                f.completeExceptionally(t)
            }

            override fun onComplete() {
                val now = System.nanoTime()
                // Record the latencies in microseconds
                histogram.recordValue((now - lastCall) / 1000)
                lastCall = now

                if (endTime - now > 0) {
                    client.retrieve(HttpRequest.GET<String>(uri).header("Connection", "keep-alive")).subscribe(this)
                } else {
                    f.complete(histogram)
                }
            }

        })

        return f
    }

    private fun doBlockingHttpCall(uri: String, endTime: Long): Future<Histogram> {
        val histogram = Histogram(HISTOGRAM_MAX_VALUE, HISTOGRAM_PRECISION)
        val f = CompletableFuture<Histogram>()
        return doHttpCall0(uri, endTime, System.nanoTime(), histogram, f)
    }

    private fun doHttpCall0(
        uri: String,
        endTime: Long,
        lastCall: Long,
        histogram: Histogram,
        f: CompletableFuture<Histogram>
    ): Future<Histogram> {
        client.toBlocking().retrieve(uri)

        val now = System.nanoTime()
        // Record the latencies in microseconds
        histogram.recordValue((now - lastCall) / 1000)

        if (endTime - now > 0) {
            doHttpCall0(uri, endTime, now, histogram, f)
        } else {
            f.complete(histogram)
        }

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
            .append("Outstanding Calls per Channel:  ")
            .append(config.outstandingCalls).append('\n')
            .append("Blocking:                       ").append(config.blocking).append('\n')
            .append("Connections:                    ").append(config.connections).append('\n')
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

data class HttpConfig(
    val channels: Int,
    val outstandingCalls: Int,
    val blocking: Boolean,
    val connections: Int,
    val warmupDuration: Long,
    val duration: Long
)

fun main(args: Array<String>) {
    val config = with(args) {
        val configurationKeys = filterIndexed { index, _ -> index % 2 == 0 }
        val configurationValues = filterIndexed { index, _ -> index % 2 == 1 }

        val configurations = configurationKeys.zip(configurationValues).toMap().withDefault { null }

        HttpConfig(
            channels = configurations["channels"]?.toIntOrNull() ?: 4,
            outstandingCalls = configurations["outstanding_calls"]?.toIntOrNull() ?: 10,
            blocking = configurations["blocking"]?.toBoolean() ?: false,
            connections = configurations["connections"]?.toIntOrNull() ?: 10,
            warmupDuration = configurations["warmup_duration"]?.toLongOrNull() ?: 10,
            duration = configurations["duration"]?.toLongOrNull() ?: 60
        )
    }

    System.setProperty("micronaut.http.services.benchmark.urls", "http://localhost:8080")
    System.setProperty("micronaut.http.services.benchmark.pool.enabled", "true")
    System.setProperty("micronaut.http.services.benchmark.pool.max-connections", config.connections.toString())

    // see:
    // https://docs.micronaut.io/1.3.5/guide/configurationreference.html#io.micronaut.http.client.DefaultHttpClientConfiguration

    // Sets the read timeout. Default value (10 seconds).
    System.setProperty("micronaut.http.client.read-timeout", "60s")
    // Sets the number of threads the client should use for requests.
    System.setProperty("micronaut.http.client.num-of-threads", config.channels.toString())

    with(ApplicationContext.run()) {
        val client = getBean(AsyncHttpClient::class.java)
        client.config = config
        client.run()
        stop()
    }
}