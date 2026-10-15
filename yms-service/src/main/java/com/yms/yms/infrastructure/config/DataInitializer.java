package com.yms.yms.infrastructure.config;

import com.yms.yms.domain.user.entity.Role;
import com.yms.yms.domain.user.entity.User;
import com.yms.yms.domain.user.repository.RoleRepository;
import com.yms.yms.domain.user.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Configuration
public class DataInitializer {

    /**
     * Ensures an admin user exists after Flyway seed.
     * Default password: Admin@123 (override via env in production).
     */
    @Bean
    ApplicationRunner ensureAdminUser(UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        return args -> {
            if (users.findByUsername("admin").isPresent()) {
                return;
            }
            Role adminRole = roles.findByCode("ADMIN").orElse(null);
            if (adminRole == null) {
                return;
            }
            User user = new User();
            user.setUsername("admin");
            user.setPasswordHash(encoder.encode("Admin@123"));
            user.setEnabled(true);
            user.setRoles(Set.of(adminRole));
            users.save(user);
        };
    }
}
