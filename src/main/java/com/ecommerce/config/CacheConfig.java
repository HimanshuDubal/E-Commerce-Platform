package com.ecommerce.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.hibernate7.Hibernate7Module;

@Configuration
@EnableCaching
public class CacheConfig {

	 @Bean
	    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
	        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer()
	                .configure(mapper -> {
	                    mapper.registerModule(new JavaTimeModule());
	                    Hibernate7Module hibernateModule = new Hibernate7Module();
	                    hibernateModule.disable(Hibernate7Module.Feature.USE_TRANSIENT_ANNOTATION);
	                    mapper.registerModule(hibernateModule);
	                });

	        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
	                .entryTtl(Duration.ofMinutes(10))
	                .disableCachingNullValues()
	                .serializeValuesWith(RedisSerializationContext.SerializationPair
	                        .fromSerializer(serializer));

	        Map<String, RedisCacheConfiguration> perCacheConfig = new HashMap<>();
	        perCacheConfig.put("products", defaultConfig.entryTtl(Duration.ofMinutes(15)));
	        perCacheConfig.put("categories", defaultConfig.entryTtl(Duration.ofHours(1)));
	        perCacheConfig.put("productRatings", defaultConfig.entryTtl(Duration.ofMinutes(30)));

	        return builder -> builder
	                .cacheDefaults(defaultConfig)
	                .withInitialCacheConfigurations(perCacheConfig);
	    }
	 
}
