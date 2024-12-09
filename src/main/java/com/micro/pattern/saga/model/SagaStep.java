package com.micro.pattern.saga.model;

public interface SagaStep<T> {
    String getName();

    SagaStepHandler<T> getHandler();

    SagaStepCompensator<T> getCompensator();
}
