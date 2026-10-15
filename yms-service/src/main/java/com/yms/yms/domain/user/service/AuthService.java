package com.yms.yms.domain.user.service;

import com.yms.yms.domain.user.entity.User;
import com.yms.yms.domain.user.repository.UserRepository;
import com.yms.yms.infrastructure.security.JwtService;
import com.yms.yms.infrastructure.security.TokenRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository,
                       TokenRepository tokenRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public LoginResult login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new LockedException("User disabled");
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new LockedException("Account temporarily locked");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
        } catch (AuthenticationException ex) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLockedUntil(Instant.now().plusSeconds(900));
                user.setFailedLoginAttempts(0);
            }
            userRepository.save(user);
            throw ex;
        }

        String accessToken = jwtService.generateToken(username);
        String refreshToken = UUID.randomUUID().toString();
        tokenRepository.save(refreshToken, user.getId(), 7 * 24 * 3600);
        return new LoginResult(accessToken, refreshToken, user);
    }

    @Transactional
    public LoginResult refresh(String refreshToken) {
        UUID userId = tokenRepository.consume(refreshToken);
        if (userId == null) {
            throw new BadCredentialsException("Invalid refresh token");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String newRefresh = UUID.randomUUID().toString();
        tokenRepository.save(newRefresh, userId, 7 * 24 * 3600);
        return new LoginResult(jwtService.generateToken(user.getUsername()), newRefresh, user);
    }

    public void logout(String refreshToken) {
        if (refreshToken != null) {
            tokenRepository.revoke(refreshToken);
        }
    }

    public record LoginResult(String accessToken, String refreshToken, User user) {
    }
}
