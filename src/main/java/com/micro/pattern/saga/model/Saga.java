package com.micro.pattern.saga.model;

import lombok.*;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class Saga<T> {
    private String name;
    private UUID key;
    private List<Class<? extends SagaStep<T>>> requiredStep;
    private SagaPayload<T> payload;

    @Setter
    private boolean isErrorOccurred;
    private boolean isCompleteExecution;
    @Setter
    private Class<? extends SagaStep<T>> currentStep;

    public void setIsCompleteExecution(boolean value) {
        if (value) {
            clearCurrentStep();
        }
        this.isCompleteExecution = true;
    }

    public void clearCurrentStep() {
        this.currentStep = null;
    }
}
