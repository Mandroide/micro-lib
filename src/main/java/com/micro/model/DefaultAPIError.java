package com.micro.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class DefaultAPIError {
    @Builder.Default
    private LocalDateTime date = LocalDateTime.now();
    private int status;
    private String message;

    private UUID tid;
    private String owner;
}
