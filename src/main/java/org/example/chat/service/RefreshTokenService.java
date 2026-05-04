package org.example.chat.service;

import lombok.RequiredArgsConstructor;
import org.example.chat.dto.reponse.TokenRefreshResponse;
import org.example.chat.entity.RefreshToken;
import org.example.chat.entity.User;
import org.example.chat.exception.TokenRefreshException;
import org.example.chat.repository.RefreshTokenRepository;
import org.example.chat.security.JwtUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;

    @Value("${chat.app.jwtRefreshExpirationMs}")
    private long refreshExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(User user) {
        refreshTokenRepository.deleteByUser(user);
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setToken(UUID.randomUUID().toString());
        token.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        return refreshTokenRepository.save(token);
    }

    @Transactional
    public TokenRefreshResponse refreshAccessToken(String requestToken) {
        return refreshTokenRepository.findByToken(requestToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String newAccess = jwtUtils.generateToken(user.getUsername());
                    return new TokenRefreshResponse(newAccess, requestToken);
                })
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found"));
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new TokenRefreshException("Refresh token expired. Please log in again.");
        }
        return token;
    }
}
