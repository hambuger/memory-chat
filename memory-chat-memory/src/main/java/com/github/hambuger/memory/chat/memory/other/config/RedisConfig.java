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

    //实例化一个Redis链接工厂
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;


    //自定义Redis操作组件RedisTemplate的配置
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        //实例化一个RedisTemplate对象
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
        //设置redis操作组件RedisTemplate的链接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //接下来开始自定义操作组件RedisTemplate的配置
        //指定大key序列化策略为为String序列化
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //value序列化策略为java自带的序列化策略
        redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());
        //指定hashKey序列化策略为String序列化
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        //redisTemplate.setHashValueSerializer(new StringRedisSerializer());
        return redisTemplate;
    }

}