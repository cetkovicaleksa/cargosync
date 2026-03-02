package rs.ac.uns.ftn.nvt.team5.cargosynccore.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;

@Configuration
public class RedisConfig {
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .build();

        ObjectMapper objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .activateDefaultTyping(ptv,
                        DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY)
                .build();

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJacksonJsonRedisSerializer(objectMapper)
                        )
                );
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer(RedisCacheConfiguration cacheConfiguration) {
        return builder -> builder
                .withCacheConfiguration("productAnalytics",
                        cacheConfiguration.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("catalogue",
                        cacheConfiguration.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("temperatureChart",
                        cacheConfiguration.entryTtl(Duration.ofMinutes(5)))
                .withCacheConfiguration("warehouseList",
                        cacheConfiguration.entryTtl(Duration.ofSeconds(30)))
                .withCacheConfiguration("warehouseDetail",
                        cacheConfiguration.entryTtl(Duration.ofSeconds(30)));
    }
}
