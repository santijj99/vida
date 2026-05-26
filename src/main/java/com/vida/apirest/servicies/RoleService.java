package com.vida.apirest.servicies;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vida.apirest.dto.role.RoleDTO;
import com.vida.apirest.model.auth.Role;
import com.vida.apirest.repositories.RoleRepository;

@Service
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<RoleDTO> obtenerTodos() {
        return roleRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public RoleDTO crearRol(String nombre) {
        if (roleRepository.existsByNombre(nombre)) {
            throw new RuntimeException("El rol '" + nombre + "' ya existe");
        }

        Role role = new Role();
        role.setNombre(nombre);
        Role savedRole = roleRepository.save(role);

        return toDTO(savedRole);
    }

    private RoleDTO toDTO(Role role) {
        RoleDTO dto = new RoleDTO();
        dto.setId(role.getId());
        dto.setNombre(role.getNombre());
        dto.setImage(role.getImage());
        dto.setRoute(role.getRoute());
        return dto;
    }
}
