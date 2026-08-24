package de.aivot.prosuna.backend;

import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.xml.JacksonXmlHttpMessageConverter;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlReadFeature;

import java.util.List;

@Configuration
public class ServerConfiguration implements WebMvcConfigurer {
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.strategies(List.of(new FixedContentNegotiationStrategy(MediaType.APPLICATION_JSON)));
    }

    @Value("${spring.flyway.repairOnMigrate}")
    private Boolean repairOnMigrate;

    @Bean
    public FlywayMigrationStrategy repairFlyway() {
        return flyway -> {
            if (repairOnMigrate) {
                flyway.repair();
            }

            flyway.migrate();
        };
    }

    @Bean
    public SimpleMessageConverter converter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(List.of(
                "de.aivot.prosuna.backend.process.workers.*",
                "de.aivot.prosuna.backend.codeLists.services.*"
        ));
        return converter;
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.withXmlConverter(new JacksonXmlHttpMessageConverter(createXmlMapperBuilder()));
    }

    private static XmlMapper.Builder createXmlMapperBuilder() {
        return XmlMapper.builder(JacksonXmlHttpMessageConverter.defensiveXmlFactory())
                .nameForTextElement("$text")
                .enable(XmlReadFeature.EMPTY_ELEMENT_AS_NULL)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
