package com.zycus.config;

import com.zycus.strategy.RoutingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class RoutingStrategyConfig {

    @Bean
    public Map<String, RoutingStrategy> routingStrategies(List<RoutingStrategy> strategies) {
        return strategies.stream()
                .collect(Collectors.toUnmodifiableMap(
                        RoutingStrategy::strategyKey,
                        Function.identity()
                ));
    }
}
