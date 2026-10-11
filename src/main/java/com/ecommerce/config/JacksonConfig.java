package com.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.datatype.hibernate7.Hibernate7Module;

@Configuration
public class JacksonConfig {

	@Bean
    public Hibernate7Module hibernate6Module() {
        Hibernate7Module module = new Hibernate7Module();
        module.disable(Hibernate7Module.Feature.USE_TRANSIENT_ANNOTATION);
        return module;
    }
	
}
