
        package com.northstar.crm.interaction;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String correlationId,
        Map<String, String> validationErrors
) {
}
