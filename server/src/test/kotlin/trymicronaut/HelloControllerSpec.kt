package trymicronaut

import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import javax.inject.Inject

//@MicronautTest
class HelloControllerSpec {

    @Inject
    lateinit var server: EmbeddedServer

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

//    @Test
    fun testHelloWorldResponse() {
        val rsp: String = client.toBlocking()
            .retrieve("/hello")
        assertEquals("Hello World", rsp)
    }

//    @Test
    fun testHelloGreetingBlocking() {

        val blockingHttpClient = client.toBlocking()

        val warmupTimeElapses = mutableListOf<Long>()
        for (i in 1..100) {
            val start = System.nanoTime()
            blockingHttpClient.retrieve("/hello/greeting?name=Viclau${i}")
            warmupTimeElapses.add((System.nanoTime() - start) / 1000)
        }

        val times = 200
        val workloadTimeElapses = mutableListOf<Long>()
        for (i in 1..times) {
            val start = System.nanoTime()
            val response = blockingHttpClient.retrieve("/hello/greeting?name=Viclau${i}")
            workloadTimeElapses.add((System.nanoTime() - start) / 1000)
            // println(response)
        }

        printTimes("workload timeElapses", workloadTimeElapses)
        printTimes("warmup timeElapses", warmupTimeElapses)
        printTimes("warmup+workload timeElapses", mutableListOf<Long>().apply {
            addAll(warmupTimeElapses)
            addAll(workloadTimeElapses)
        })
    }

//    @Test
    fun testHelloGreetingAsync() {
        client.retrieve(HttpRequest.GET<String>("/hello/greeting?name=Viclau"))
            .subscribe(object : Subscriber<String> {
                override fun onSubscribe(s: Subscription) {
                    s.request(1)
                }

                override fun onNext(t: String) {
                    println("onNext: $t")
                }

                override fun onError(t: Throwable) {
                    println("onError called")
                    t.printStackTrace()
                }

                override fun onComplete() {
                    println("onComplete called")
                }

            })
    }

    private fun printTimes(desc: String, times: List<Long>) {
        println("$desc count = ${times.size}, min = ${times.min()} us, max = ${times.max()} us, avg = ${times.average()} us")
    }

}