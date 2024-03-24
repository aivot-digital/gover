package de.aivot.GoverBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "de.aivot.GoverBackend.repositories")
@EnableRedisRepositories(basePackages = "de.aivot.GoverBackend.redis")
public class GoverBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(GoverBackendApplication.class, args);
	}
}
