package de.aivot.GoverBackend.services;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

public class RedisService {
    @Bean
    public JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory();
    }

    @Bean
    public RedisTemplate<Integer, String> redisTemplate() {
        var template = new RedisTemplate<Integer, String>();
        template.setConnectionFactory(jedisConnectionFactory());
        return template;
    }
}
