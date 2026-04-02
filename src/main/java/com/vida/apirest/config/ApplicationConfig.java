package com.vida.apirest.config;

import com.vida.apirest.servicies.CustomUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final CustomUserDetailService customUserDetailService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationManager authenticationManager (UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        return authentication -> {
            String usuario = authentication.getName();
            String password = authentication.getCredentials().toString();

            UserDetails user = userDetailsService.loadUserByUsername(usuario);
            if (!passwordEncoder.matches(password, user.getPassword())){
                throw new BadCredentialsException("Credenciales invalidas");
            }
            return new UsernamePasswordAuthenticationToken(user,password,user.getAuthorities());
        };
    }

    public UserDetailsService userDetailsServices(){
        return customUserDetailService;
    }

}

