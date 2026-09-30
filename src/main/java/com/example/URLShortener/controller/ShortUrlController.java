package com.example.URLShortener.controller;

import com.example.URLShortener.dto.request.CreateShortUrlRequest;
import com.example.URLShortener.dto.request.UpdateShortUrlRequest;
import com.example.URLShortener.dto.response.CreateShortUrlResponse;
import com.example.URLShortener.dto.response.ShortUrlResponse;
import com.example.URLShortener.entity.ShortUrl;
import com.example.URLShortener.exception.ResourceNotFoundException;
import com.example.URLShortener.service.ShortUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Links", description = "Create and manage short links")
@RestController
@RequestMapping(value = "/api/v1/urls", produces = MediaType.APPLICATION_JSON_VALUE)
@SecurityRequirement(name = "bearerAuth")
@Validated
public class ShortUrlController {

    private final ShortUrlService shortUrlService;

    public ShortUrlController(ShortUrlService shortUrlService) {
        this.shortUrlService = shortUrlService;
    }

    @Operation(
            summary = "Create a short link",
            description = """
                    Creates a short link for `originUrl`. If `customShortCode` is provided, exactly that code
                    is used (`409`, `SHORT_CODE_TAKEN`, if someone already uses it); otherwise the system
                    generates a code from a counter + Base62. `expiresAt`, if present, must be in the future.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created successfully"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    @PostMapping
    public ResponseEntity<CreateShortUrlResponse> createShortUrl(@RequestBody @Valid CreateShortUrlRequest request) {
        ShortUrl shortUrl = shortUrlService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                CreateShortUrlResponse.builder()
                        .createdAt(LocalDateTime.now())
                        .originUrl(request.getOriginUrl())
                        .shortCode(shortUrl.getShortCode())
                        .build()
        );
    }

    @Operation(
            summary = "Update a short link",
            description = """
                    Edits a link by `shortCode` (not by id). Send only the fields to change; null fields
                    keep their previous values. Changing to a different `newShortCode` returns `409` if
                    that code already exists. `status` accepts `ACTIVE` / `DISABLED` / `DELETED`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "The link after the update"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    })
    @PatchMapping
    public ResponseEntity<ShortUrlResponse> updateShortUrl (
            @RequestBody @Valid UpdateShortUrlRequest request) {
        ShortUrl shortUrl = shortUrlService.update(request)
                .orElseThrow(() -> new ResourceNotFoundException("Short code not found"));
        return ResponseEntity.status(HttpStatus.OK).body(
                ShortUrlResponse.builder()
                        .shortCode(shortUrl.getShortCode())
                        .updatedAt(shortUrl.getUpdatedAt())
                        .expiresAt(shortUrl.getExpiresAt())
                        .originUrl(shortUrl.getOriginUrl())
                        .status(shortUrl.getStatus())
                        .build()
        );
    }

    @Operation(
            summary = "List my links",
            description = """
                    Returns a page of links created by the signed-in user. Supports `page` (zero-based),
                    `size` (default 20, max 100) and `sort`, for example `sort=createdAt,desc`.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "A page of short links"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
    })
    @GetMapping("/me")
    public ResponseEntity<Page<ShortUrlResponse>> getMyUrls(@ParameterObject Pageable pageable) {
        return ResponseEntity.status(HttpStatus.OK).body(
                shortUrlService.getMyUrls(pageable)
        );
    }

    @Operation(
            summary = "Delete a short link",
            description = """
                    Soft delete: the link moves to the `DELETED` status, and later calls to `/r/{shortCode}`
                    return `404`. This action cannot be undone.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted successfully, no body"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "404", ref = "#/components/responses/NotFound")
    })
    @DeleteMapping("{shortCode}")
    public ResponseEntity<Void> deleteShortCode(
            @Parameter(description = "The short code to delete", example = "u3Kf9a")
            @PathVariable("shortCode") @Size(max = 16) @Pattern(regexp = "^[a-zA-Z0-9]+$") String shortCode) {
        shortUrlService.delete(shortCode);
        return ResponseEntity.noContent().build();
    }
}
