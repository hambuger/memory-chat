package com.github.hambuger.memory.chat.memory.other.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


/**
 * @author hamburger
 * @since 2024/7/23
 */
@Configuration
public class RedisConfig {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;


    //Configuration of custom Redis operation component RedisTemplate
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();

        redisTemplate.setConnectionFactory(redisConnectionFactory);


        redisTemplate.setKeySerializer(new StringRedisSerializer());

        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());

        redisTemplate.setHashKeySerializer(new StringRedisSerializer());

        return redisTemplate;
    }

}