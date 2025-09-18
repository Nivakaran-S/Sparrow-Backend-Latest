// RateLimitingFilter.java
package com.sparrow.api_gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RateLimitingFilter(ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            String key = "rate_limit:" + ip;

            return redisTemplate.opsForValue().get(key)
                    .flatMap(count -> {
                        int currentCount = Integer.parseInt(count != null ? count : "0");
                        if (currentCount >= config.maxRequests) {
                            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                            return exchange.getResponse().setComplete();
                        }
                        return redisTemplate.opsForValue().increment(key)
                                .flatMap(newCount -> {
                                    if (newCount == 1) {
                                        return redisTemplate.expire(key, Duration.ofSeconds(config.timeWindow));
                                    }
                                    return Mono.just(newCount);
                                })
                                .then(chain.filter(exchange));
                    })
                    .switchIfEmpty(
                            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(config.timeWindow))
                                    .then(chain.filter(exchange))
                    );
        };
    }

    public static class Config {
        private int maxRequests = 100;
        private int timeWindow = 60; // seconds

        public int getMaxRequests() { return maxRequests; }
        public void setMaxRequests(int maxRequests) { this.maxRequests = maxRequests; }

        public int getTimeWindow() { return timeWindow; }
        public void setTimeWindow(int timeWindow) { this.timeWindow = timeWindow; }
    }
}
