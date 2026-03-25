package com.ecommerce.auth.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ResilienceConfig {
    
    @Bean
    public RegistryEventConsumer<CircuitBreaker> circuitBreakerEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> entryAddedEvent) {
                CircuitBreaker circuitBreaker = entryAddedEvent.getAddedEntry();
                circuitBreaker.getEventPublisher()
                        .onStateTransition(event -> 
                                log.info("Circuit breaker {} transitioned from {} to {}", 
                                        event.getCircuitBreakerName(), 
                                        event.getStateTransition().getFromState(), 
                                        event.getStateTransition().getToState()))
                        .onFailureRateExceeded(event -> 
                                log.warn("Circuit breaker {} failure rate exceeded: {}%", 
                                        event.getCircuitBreakerName(), 
                                        event.getFailureRate()))
                        .onCallNotPermitted(event -> 
                                log.warn("Circuit breaker {} call not permitted", 
                                        event.getCircuitBreakerName()))
                        .onError(event -> 
                                log.error("Circuit breaker {} error: {}", 
                                        event.getCircuitBreakerName(), 
                                        event.getThrowable().getMessage()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> entryRemoveEvent) {
                log.info("Circuit breaker {} removed", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> entryReplacedEvent) {
                log.info("Circuit breaker {} replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
    
    @Bean
    public RegistryEventConsumer<Retry> retryEventConsumer() {
        return new RegistryEventConsumer<>() {
            @Override
            public void onEntryAddedEvent(EntryAddedEvent<Retry> entryAddedEvent) {
                Retry retry = entryAddedEvent.getAddedEntry();
                retry.getEventPublisher()
                        .onRetry(event -> 
                                log.info("Retry {} - attempt #{}", 
                                        event.getName(), 
                                        event.getNumberOfRetryAttempts()))
                        .onError(event -> 
                                log.error("Retry {} failed after {} attempts: {}", 
                                        event.getName(), 
                                        event.getNumberOfRetryAttempts(), 
                                        event.getLastThrowable().getMessage()))
                        .onSuccess(event -> 
                                log.info("Retry {} succeeded after {} attempts", 
                                        event.getName(), 
                                        event.getNumberOfRetryAttempts()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<Retry> entryRemoveEvent) {
                log.info("Retry {} removed", entryRemoveEvent.getRemovedEntry().getName());
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<Retry> entryReplacedEvent) {
                log.info("Retry {} replaced", entryReplacedEvent.getNewEntry().getName());
            }
        };
    }
} 