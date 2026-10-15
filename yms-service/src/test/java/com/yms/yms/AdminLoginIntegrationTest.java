package com.yms.yms;

import com.yms.yms.domain.user.entity.Role;
import com.yms.yms.domain.user.entity.User;
import com.yms.yms.domain.user.repository.RoleRepository;
import com.yms.yms.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminLoginIntegrationTest {

    private static final String FIXED_SEED_PASSWORD_HASH =
            "$2a$10$I.sIVmE7jTicSZS5tB0EN.LThWgTP6g7aLmE/9VNsoo.2aSs.k7su";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void seededAdminPasswordAuthenticatesThroughLoginApi() throws Exception {
        Role adminRole = roleRepository.findByCode("ADMIN").orElseGet(() ->
                roleRepository.save(new Role(UUID.randomUUID(), "ADMIN", "Administrator", new HashSet<>())));
        User admin = new User();
        admin.setUsername("admin");
        admin.setPasswordHash(FIXED_SEED_PASSWORD_HASH);
        admin.setEnabled(true);
        admin.setRoles(new HashSet<>(Set.of(adminRole)));
        userRepository.save(admin);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    void fixedSeedHashMatchesDocumentedAdminPassword() {
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("Admin@123", FIXED_SEED_PASSWORD_HASH));
    }
}
