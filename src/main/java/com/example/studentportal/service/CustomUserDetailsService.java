package com.example.studentportal.service;

import com.example.studentportal.model.User;
import com.example.studentportal.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;

/**
 * Service – Spring Security UserDetailsService — loads user by email for authentication
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>find User by email; throw UsernameNotFoundException if absent</li>
 *   <li>wrap User in CustomUserPrincipal for security context</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true) // read-only tx [no writes during auth]
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserPrincipal(user);
    }

    // UserDetails wrapper exposing User entity to Spring Security
    public static class CustomUserPrincipal implements UserDetails {
        private final User user;

        public CustomUserPrincipal(User user) { this.user = user; }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()));
        }

        @Override
        public String getPassword() { return user.getPasswordHash(); }

        @Override
        public String getUsername() { return user.getEmail(); }

        @Override
        public boolean isAccountNonExpired() { return true; }

        @Override
        public boolean isAccountNonLocked() { return true; }

        @Override
        public boolean isCredentialsNonExpired() { return true; }

        @Override
        public boolean isEnabled() { return true; }

        public User getUser() { return user; }
    }
}