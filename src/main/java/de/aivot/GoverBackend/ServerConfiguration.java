package de.aivot.GoverBackend;

import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.servlet.HttpExchangesFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServerConfiguration {
    @Bean
    public InMemoryHttpExchangeRepository createTraceRepository() {
        var exchanges = new InMemoryHttpExchangeRepository();
        exchanges.setCapacity(100);
        return exchanges;
    }

    /*
    @Bean
    public PrometheusMeterRegistry createPrometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }
     */
}
