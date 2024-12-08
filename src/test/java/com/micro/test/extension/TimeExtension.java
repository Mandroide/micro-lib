package com.micro.test.extension;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

@Slf4j
public class TimeExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final String START_TIME = "startTime";
    @Override
    public void afterTestExecution(ExtensionContext extensionContext) throws Exception {
        getStore(extensionContext).put(START_TIME, System.currentTimeMillis());
    }

    @Override
    public void beforeTestExecution(ExtensionContext extensionContext) throws Exception {
        log.info("Method {} took {} MS", extensionContext.getRequiredTestMethod().getName(), System.currentTimeMillis() - getStore(extensionContext).remove(START_TIME, Long.class));
    }

    private ExtensionContext.Store getStore(ExtensionContext extensionContext) {
        return extensionContext.getStore(ExtensionContext.Namespace.create(getClass(), extensionContext.getRequiredTestMethod()));
    }
}
