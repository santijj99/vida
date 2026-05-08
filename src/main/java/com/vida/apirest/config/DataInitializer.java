package com.vida.apirest.config;

import com.vida.apirest.model.auth.Role;
import com.vida.apirest.model.finanzas.Moneda;
import com.vida.apirest.repositories.MonedaRepository;
import com.vida.apirest.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final RoleRepository roleRepository;
    private final MonedaRepository monedaRepository;

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

    @Bean
    public CommandLineRunner seedMonedas() {
        return args -> {
            // Monedas básicas
            List<Moneda> monedas = List.of(
                createMoneda("ARS", "Peso Argentino", "$", BigDecimal.ONE, 2, true, true),
                createMoneda("USD", "Dólar Estadounidense", "USD", BigDecimal.valueOf(950), 2, true, false),
                createMoneda("EUR", "Euro", "€", BigDecimal.valueOf(1050), 2, true, false),
                createMoneda("BRL", "Real Brasileño", "R$", BigDecimal.valueOf(180), 2, true, false)
            );

            for (Moneda moneda : monedas) {
                if (!monedaRepository.findByCodigo(moneda.getCodigo()).isPresent()) {
                    monedaRepository.save(moneda);
                }
            }
        };
    }

    private Moneda createMoneda(String codigo, String nombre, String simbolo, BigDecimal tasaCambio,
                               int decimalPlaces, boolean activo, boolean predeterminada) {
        Moneda moneda = new Moneda();
        moneda.setCodigo(codigo);
        moneda.setNombre(nombre);
        moneda.setSimbolo(simbolo);
        moneda.setTasaCambio(tasaCambio);
        moneda.setDecimalPlaces(decimalPlaces);
        moneda.setActivo(activo);
        moneda.setPredeterminada(predeterminada);
        return moneda;
    }
}
