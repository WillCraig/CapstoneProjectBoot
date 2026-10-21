
package com.northstar.crm.interaction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateInteractionRequest(
        @NotNull InteractionChannel channel,
        @NotBlank @Size(max = 2000) String summary
) {}
