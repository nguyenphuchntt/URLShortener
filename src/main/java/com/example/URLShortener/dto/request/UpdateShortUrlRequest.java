package com.example.URLShortener.dto.request;

import com.example.URLShortener.entity.enums.ShortUrlStatus;
import jakarta.annotation.Nullable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Data
public class UpdateShortUrlRequest {

    @Size(min = 6, max = 16)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String shortCode;

    @URL
    private String originUrl;

    @Size(min = 6, max = 16)
    @Pattern(regexp = "^[a-zA-Z0-9]+$")
    private String newShortCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ShortUrlStatus status;

    @Nullable
    @Future
    private LocalDateTime expiresAt;
}