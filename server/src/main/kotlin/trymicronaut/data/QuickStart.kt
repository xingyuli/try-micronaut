package trymicronaut.data

import io.micronaut.data.annotation.*
import io.micronaut.data.jdbc.annotation.JdbcRepository
import io.micronaut.data.model.query.builder.sql.Dialect
import io.micronaut.data.repository.CrudRepository
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import javax.inject.Inject

@MappedEntity
class Book {
    @Id
    @GeneratedValue
    var id: Long? = null
    var title: String? = null
    var pages: Int = 0
}

@JdbcRepository(dialect = Dialect.MYSQL)
interface BookRepository : CrudRepository<Book, Long> {
    fun find(title: String): Book
}

@Controller("/data/books")
class BookController {

    @Inject
    private lateinit var bookRepository: BookRepository

    @Get("/byTitle")
    fun getByTitle(@QueryValue value: String): Book {
        return bookRepository.find(value)
    }

}

@Controller("/hello")
class HelloController {

    @Get("/greeting")
    fun greeting(@QueryValue name: String): String = "Hello ${name}!"

}