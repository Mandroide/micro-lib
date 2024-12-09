package com.micro.pattern.saga.model;

public interface SagaStepHandler<T> {
    void handle(SagaPayload<T> sagaPayload);
}
