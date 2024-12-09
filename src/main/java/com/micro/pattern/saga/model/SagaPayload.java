package com.micro.pattern.saga.model;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

public class SagaPayload<T> {
    private final Map<SagaPayloadKey<?>, Object> properties = new HashMap<>();

    @Getter
    @Setter
    private T result;

    public <M> M getProperty(SagaPayloadKey<M> sagaPayloadKey) {
        return sagaPayloadKey.type().cast(properties.get(sagaPayloadKey));
    }

    public <M> void addProperty(SagaPayloadKey<M> sagaPayloadKey, M value) {
        properties.put(sagaPayloadKey, value);
    }

    public <M> boolean hasProperty(SagaPayloadKey<M> sagaPayloadKey) {
        return properties.containsKey(sagaPayloadKey);
    }
}
