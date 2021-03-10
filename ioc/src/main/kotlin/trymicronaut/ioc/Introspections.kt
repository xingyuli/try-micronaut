package trymicronaut.ioc

import io.micronaut.core.annotation.Introspected
import io.micronaut.core.beans.BeanIntrospection

@Introspected
data class Person(var name: String) {
    var age: Int = 18
}

fun main() {
    val introspection = BeanIntrospection.getIntrospection(Person::class.java)
    val person: Person = introspection.instantiate("John")
    println("Hello ${person.name}")

    val property = introspection.getRequiredProperty("name", String::class.java)
    property.set(person, "Fred")
    val name = property.get(person)
    println("Hello $name")
}
