
package com.northstar.crm.interaction;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/customers/{customerId}/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }
    @GetMapping
    public ResponseEntity<java.util.List<InteractionResponse>> list(
            @PathVariable String customerId
    ) {
        return ResponseEntity.ok(interactionService.list(customerId));
    }

    @PostMapping
    public ResponseEntity<InteractionResponse> create(
            @PathVariable String customerId,
            @Valid @RequestBody CreateInteractionRequest request,
            @RequestHeader(value = "X-Correlation-ID", defaultValue = "lab-request-001")
            String correlationId,
            @RequestHeader(value = "X-Actor", defaultValue = "AGENT")
            String actor
    ) {
        InteractionResponse response = interactionService.create(
                customerId, request, correlationId, actor
        );

        URI location = URI.create(
                "/api/customers/" + customerId
                        + "/interactions/" + response.interactionId()
        );

        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{interactionId}")
    public ResponseEntity<InteractionResponse> update(
            @PathVariable String customerId,
            @PathVariable java.util.UUID interactionId,
            @Valid @RequestBody UpdateInteractionRequest request
    ) {
        return ResponseEntity.ok(
                interactionService.update(customerId, interactionId, request)
        );
    }

    @DeleteMapping("/{interactionId}")
    public ResponseEntity<Void> delete(
            @PathVariable String customerId,
            @PathVariable java.util.UUID interactionId
    ) {
        interactionService.delete(customerId, interactionId);
        return ResponseEntity.noContent().build();
    }

}
