# trymicronaut

## ioc

Try the ioc functionality.

## server

Simple http server app running on port `8080`, with following Micronaut's features demonstrated:
- AOP
- db access

Beside these, a HelloWorld like grpc server `HelloServer` is running on port `8980` within the app for my benchmark purpose. 

## benchmarks

### AsyncHttpClients.kt

A hands on http client benchmark tool, which test against the http server endpoint `http://localhost:8080/hello/greeting?name=Viclau` .

### AsyncGrpcClients.kt

A hands on grpc client benchmark tool, which test against the generated grpc code by the `server` module.
