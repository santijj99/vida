package com.vida.apirest.servicies;

import com.vida.apirest.dto.ariticulo.CreateTalleRequest;
import com.vida.apirest.dto.ariticulo.TalleResponse;
import com.vida.apirest.model.articulo.Talle;
import com.vida.apirest.repositories.TalleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TalleService {

    private final TalleRepository talleRepository;

    @Transactional(readOnly = true)
    public List<TalleResponse> findAll() {
        return talleRepository.findAllByOrderByPaisAscNumeroAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TalleResponse create(CreateTalleRequest request) {
        if (request.getPais() == null || request.getPais().isBlank()) {
            throw new RuntimeException("El país del talle es obligatorio");
        }
        if (request.getNumero() == null || request.getNumero().isBlank()) {
            throw new RuntimeException("El número de talle es obligatorio");
        }

        Talle.Pais pais;
        try {
            pais = Talle.Pais.valueOf(request.getPais().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("País de talle inválido. Valores: AR, UK, BR, US, EU");
        }

        String numero = request.getNumero().trim();
        if (talleRepository.findByPaisAndNumero(pais, numero).isPresent()) {
            throw new RuntimeException("Ya existe el talle " + numero + " para el país " + pais.name());
        }

        Talle talle = new Talle();
        talle.setPais(pais);
        talle.setNumero(numero);
        talle.setDescripcion(request.getDescripcion() != null ? request.getDescripcion().trim() : null);

        return toResponse(talleRepository.save(talle));
    }

    private TalleResponse toResponse(Talle talle) {
        TalleResponse response = new TalleResponse();
        response.setId(talle.getId());
        response.setPais(talle.getPais() != null ? talle.getPais().name() : null);
        response.setNumero(talle.getNumero());
        response.setDescripcion(talle.getDescripcion());
        return response;
    }
}
