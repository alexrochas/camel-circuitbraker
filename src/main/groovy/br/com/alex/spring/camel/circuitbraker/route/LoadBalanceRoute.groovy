package br.com.alex.spring.camel.circuitbraker.route

import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.model.dataformat.JsonLibrary
import org.springframework.stereotype.Component

import java.util.concurrent.RejectedExecutionException

@Component
class LoadBalanceRoute extends RouteBuilder {

    @Override
    void configure() throws Exception {
        restConfiguration().port(8080)

        rest("/circuitbreaker/exception")
				.get()
				.route()
                .routeId("timeout-route")
                .onException(RejectedExecutionException.class)
                    .handled(true)
                    .log("Exception")
                    .setBody().constant("Circuit open")
                .end()
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .loadBalance()
                    .circuitBreaker(5, 5000, IllegalStateException.class)
                    .process( {exchange ->
                        throw new IllegalStateException()
                    })
                .end()

        rest("/circuitbreaker/hystrix")
                .get()
                .route()
                .setHeader(Exchange.CONTENT_TYPE, constant("text/plain"))
                .hystrix()
                    .hystrixConfiguration()
                        .queueSizeRejectionThreshold(5)
                        .executionTimeoutInMilliseconds(100)
                        .circuitBreakerSleepWindowInMilliseconds(10000)
                    .end()
                    .process({exchange ->
                        sleep(150)
                    })
                    .log("Processing")
                    .transform().constant("Circuit closed")
                .onFallbackViaNetwork()
                    .log("Fallback")
                    .transform().constant("Circuit open")
                .end()
    }
}
