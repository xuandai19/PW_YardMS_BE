package com.yms.yms.infrastructure.security;

import com.yms.yms.domain.user.entity.User;
import com.yms.yms.domain.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class YmsUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public YmsUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        var authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getCode()))
                .collect(Collectors.toList());

        // also expose permissions as authorities
        user.getRoles().forEach(role ->
                role.getPermissions().forEach(p ->
                        authorities.add(new SimpleGrantedAuthority(p.getCode()))));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .accountLocked(user.getLockedUntil() != null && user.getLockedUntil().isAfter(java.time.Instant.now()))
                .authorities(authorities)
                .build();
    }
}
