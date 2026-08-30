package com.example.URLShortener.security;

import com.example.URLShortener.entity.User;
import com.example.URLShortener.exception.UnauthorizedException;
import com.example.URLShortener.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@AllArgsConstructor
public class CurrentUser {

    private final UserRepository userRepository;

    public Long requireUserId() {
        return currentUserId().orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }

    public User requireUser() {
        Long userId = requireUserId();
        System.out.println(userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }

    public Optional<Long> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long userId) {
            return Optional.of(userId);
        }
        if (principal instanceof String userId) {
            try {
                return Optional.of(Long.parseLong(userId));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    public boolean hasRole(String role) {
        if (role == null) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return false;
        }
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority ->
                        authority.equals(role));
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
