package com.micro.pattern.saga.model;

import java.util.Objects;

public record SagaPayloadKey<T> (String id, Class<T> type) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SagaPayloadKey<?> that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
