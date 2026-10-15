package com.yms.yms;

import com.yms.yms.domain.user.entity.Role;
import com.yms.yms.domain.user.entity.User;
import com.yms.yms.domain.user.repository.UserRepository;
import com.yms.yms.domain.user.service.AuthService;
import com.yms.yms.infrastructure.security.JwtService;
import com.yms.yms.infrastructure.security.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock UserRepository userRepository;
    @Mock TokenRepository tokenRepository;

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(authenticationManager, jwtService, userRepository, tokenRepository);
    }

    @Test
    void loginSuccess() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("admin");
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        Role role = new Role();
        role.setCode("ADMIN");
        user.setRoles(Set.of(role));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(jwtService.generateToken("admin")).thenReturn("access-token");
        when(userRepository.save(any())).thenReturn(user);

        AuthService.LoginResult result = authService.login("admin", "Admin@123");
        assertEquals("access-token", result.accessToken());
        assertNotNull(result.refreshToken());
        verify(tokenRepository).save(anyString(), eq(user.getId()), anyLong());
    }
}
