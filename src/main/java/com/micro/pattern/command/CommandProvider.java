package com.micro.pattern.command;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;

@ConditionalOnProperty(prefix = "pattern", name = "cqrs.enabled", havingValue = "true")
@Slf4j
@Component
public class CommandProvider implements BeanPostProcessor {
    @SuppressWarnings("rawtypes")
    final Map<Class<? extends Command>, CommandHandler> registry = new HashMap<>();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public CommandHandler<?, Command<?>> get(Class<? extends Command> c) {
        return registry.get(c);
    }

    @SuppressWarnings({"NullableProblems", "rawtypes"})

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> clazz = ClassUtils.getUserClass(bean);
        if (clazz.isAnnotationPresent(CommandEvent.class)) {
            log.info("Configuring command: {}", clazz);
            Class<? extends Command> command = clazz.getAnnotation(CommandEvent.class).command();
            registry.put(command, (CommandHandler) bean);
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
