package com.micro.pattern.saga.model;

public interface SagaStepCompensator<T> {
    void handle(SagaPayload<T> sagaPayload);
}
