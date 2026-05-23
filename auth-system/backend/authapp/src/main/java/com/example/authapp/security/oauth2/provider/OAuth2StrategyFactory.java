package com.example.authapp.security.oauth2.provider;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

//전략 찾기 팩토리
@Component
public class OAuth2StrategyFactory {

    private final Map<String, OAuth2ProviderStrategy> strategies;

    public OAuth2StrategyFactory(List<OAuth2ProviderStrategy> list) {
        this.strategies = list.stream()
        		.collect(Collectors.toMap(s -> s.provider(), Function.identity()));
    }

    public OAuth2ProviderStrategy get(String provider) {

        OAuth2ProviderStrategy strategy = strategies.get(provider.toUpperCase());

        if (strategy == null) {
            throw new IllegalArgumentException("지원하지 않는 공급자: " + provider);
        }

        return strategy;
    }
    
    
}