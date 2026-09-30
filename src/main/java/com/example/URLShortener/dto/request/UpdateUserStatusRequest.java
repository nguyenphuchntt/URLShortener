package com.example.URLShortener.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "Request to enable or disable a user (ADMIN only)")
@Data
public class UpdateUserStatusRequest {

    @Schema(description = """
            `true` = enable the account, `false` = disable the account. Disabled users cannot sign in
            but their data is retained. This field is required.
            """,
            example = "false")
    @NotNull
    private Boolean enabled;
}
