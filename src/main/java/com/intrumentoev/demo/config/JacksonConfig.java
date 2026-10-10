package com.intrumentoev.demo.config;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer strictNumberCoercionCustomizer() {
        return builder -> {
            builder.serializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);
            builder.featuresToDisable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
            builder.postConfigurer(mapper -> {
                mapper.coercionConfigFor(LogicalType.Float)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail);
                mapper.coercionConfigFor(LogicalType.Integer)
                        .setCoercion(CoercionInputShape.String, CoercionAction.Fail);
            });
        };
    }
}
