package com.nnuist.coursehub.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 *
 * <p><b>为什么要自定义</b>：Spring 默认的 RedisTemplate 用 JDK 序列化，
 * 存进去的值在 redis-cli 里看是一堆乱码（\xac\xed...），排查问题极其痛苦。
 * 这里改成：
 * <ul>
 *   <li>key 用 String 序列化 —— redis-cli 里能直接读懂</li>
 *   <li>value 用 JSON 序列化 —— 结构清晰，跨语言可用</li>
 * </ul>
 *
 * @author CourseHub Team
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // key 用字符串序列化，便于在 redis-cli 中直接查看
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // value 用 JSON 序列化
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(buildObjectMapper());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 构建支持 Java 8 时间类型的 ObjectMapper
     *
     * <p>开启 defaultTyping 后，序列化结果里会带上类名信息
     * （形如 {@code ["com.nnuist.coursehub.entity.User", {...}]}），
     * 反序列化时才能还原成正确的对象类型。</p>
     */
    private ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 允许序列化 private 字段
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 日期不写成时间戳数组，改为可读格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 支持 LocalDateTime / LocalDate
        mapper.registerModule(new JavaTimeModule());
        // 写入类型信息，保证反序列化能还原具体类型
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }
}
