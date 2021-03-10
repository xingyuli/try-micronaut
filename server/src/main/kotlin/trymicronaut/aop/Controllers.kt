package trymicronaut.aop

import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get

@Controller("/aop")
class AopController(private val aopService: AopService) {

    @Get("/tx")
    fun tx() {
        aopService.testTx()
    }

}