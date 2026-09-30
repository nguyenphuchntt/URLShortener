package com.example.URLShortener.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Role information")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

    @Schema(description = "Role name", example = "ADMIN")
    private String name;
}
