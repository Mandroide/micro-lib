package com.micro.pattern.saga;

import com.micro.pattern.saga.model.Saga;
import com.micro.pattern.saga.model.SagaException;
import com.micro.pattern.saga.model.SagaStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@ConditionalOnProperty(prefix = "pattern", name = "saga.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
@Component
public class SagaOrchestrator {
    private final ApplicationContext applicationContext;

    public <T> T orchestrate(Saga<T> saga, int secondTimeout) {
        try (ExecutorService executorService = Executors.newSingleThreadExecutor()) {
            Future<T> future = executorService.submit(() -> orchestrate(saga));
            try {
                return future.get(secondTimeout, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SagaException(e);
            } catch (TimeoutException | ExecutionException e) {
                future.cancel(true);
                throw new SagaException(e);
            }
        }
    }

    private <T> T orchestrate(Saga<T> saga) {
        for (Class<? extends SagaStep<T>> sagaStep : saga.getRequiredStep()) {
            if (Thread.interrupted()) {
                triggerCompensation(saga);
            }
            saga.setCurrentStep(sagaStep);
            try {
                SagaStep<T> step = applicationContext.getBean(sagaStep);
                log.info("Executing SAGA {} - {} : Step {}", saga.getKey(), saga.getName(), step.getName());
                step.getHandler().handle(saga.getPayload());
            } catch (Exception e) {
                triggerCompensation(saga);
                saga.setIsCompleteExecution(true);
                throw e;
            }
        }
        saga.setIsCompleteExecution(true);
        return saga.getPayload().getResult();
    }

    private <T> void triggerCompensation(Saga<T> saga) {
        log.info("Triggering compensator SAGA {} : {}", saga.getKey(), saga.getName());
        int index = saga.getRequiredStep().indexOf(saga.getCurrentStep());
        for (int i = index; i >= 0; i--) {
            Class<? extends SagaStep<T>> sagaStep = saga.getRequiredStep().get(i);
            SagaStep<T> bean = applicationContext.getBean(sagaStep);
            if (bean.getCompensator() != null) {
                log.info("Triggering Compensator SAGA {} - {} : Step {}", saga.getKey(), saga.getName(), bean.getName());
                bean.getCompensator().handle(saga.getPayload());
            } else {
                log.info("SAGA {} - {} : Step {}", saga.getKey(), saga.getName(), bean.getName());
            }
        }
    }
}
