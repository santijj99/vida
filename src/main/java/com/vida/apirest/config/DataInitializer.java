package com.vida.apirest.config;

import com.vida.apirest.model.auth.Role;
import com.vida.apirest.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;

    @Bean
    public CommandLineRunner seedRoles() {
        return args -> {
            List<String> roles = List.of("CLIENTE", "EMPLEADO", "ADMINISTRADOR");
            for (String nombre : roles) {
                if (!roleRepository.existsByNombre(nombre)) {
                    Role role = new Role();
                    role.setNombre(nombre);
                    roleRepository.save(role);
                }
            }
        };
    }
}
