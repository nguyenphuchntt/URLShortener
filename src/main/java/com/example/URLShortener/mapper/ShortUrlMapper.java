package com.example.URLShortener.mapper;

import com.example.URLShortener.dto.response.CreateShortUrlResponse;
import com.example.URLShortener.entity.ShortUrl;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ShortUrlMapper {

    CreateShortUrlResponse toResponse(ShortUrl shortUrl);
}
