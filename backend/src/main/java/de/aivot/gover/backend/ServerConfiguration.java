package de.aivot.gover.backend;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.accept.FixedContentNegotiationStrategy;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

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
                "de.aivot.gover.backend.process.workers.*",
                "de.aivot.gover.backend.codeLists.services.*"
        ));
        return converter;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // XmlMapper extends ObjectMapper. Exposing it as a bean would replace Boot's default JSON mapper.
        converters.removeIf(MappingJackson2XmlHttpMessageConverter.class::isInstance);
        converters.add(new MappingJackson2XmlHttpMessageConverter(createXmlMapper()));
    }

    private static XmlMapper createXmlMapper() {
        JacksonXmlModule module = new JacksonXmlModule();
        module.setXMLTextElementName("$text");

        XmlMapper mapper = new XmlMapper(module);
        mapper.configure(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}
