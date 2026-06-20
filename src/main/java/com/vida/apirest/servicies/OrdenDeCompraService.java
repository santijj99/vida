package com.vida.apirest.servicies;

import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.dto.pedido.*;
import com.vida.apirest.model.almacen.Sucursal;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.Taxon;
import com.vida.apirest.model.articulo.TaxonArticulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import com.vida.apirest.model.pedido.OrdenDeCompra;
import com.vida.apirest.model.pedido.OrdenDeCompraDetalle;
import com.vida.apirest.model.persona.Proveedor;
import com.vida.apirest.repositories.OrdenDeCompraRepository;
import com.vida.apirest.repositories.ProveedorRepository;
import com.vida.apirest.repositories.SucursalRepository;
import com.vida.apirest.utils.PaginationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrdenDeCompraService {

    private final OrdenDeCompraRepository ordenDeCompraRepository;
    private final SucursalRepository sucursalRepository;
    private final ProveedorRepository proveedorRepository;
    private final ArticuloService articuloService;

    @Transactional(readOnly = true)
    public PageResponse<OrdenCompraResponse> findPage(String q, String estado, int page, int size) {
        PaginationUtils.PageParams params = PaginationUtils.normalize(page, size);
        Page<OrdenDeCompra> result = ordenDeCompraRepository.searchPage(
                blankToNull(q),
                blankToNull(estado),
                PageRequest.of(params.page(), params.size()));
        return PageResponse.from(result.map(this::toResponseResumido));
    }

    @Transactional(readOnly = true)
    public OrdenCompraResponse findById(Long id) {
        OrdenDeCompra orden = ordenDeCompraRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + id));
        return toResponse(orden);
    }

    @Transactional(readOnly = true)
    public OrdenCompraVarianteLookupResponse buscarPorCodigoBarras(String codigo) {
        return articuloService.buscarVariantePorCodigoBarras(codigo);
    }

    @Transactional(readOnly = true)
    public List<OrdenCompraVarianteLookupResponse> resolverCodigos(List<String> codigos) {
        return articuloService.resolverVariantesPorCodigos(codigos);
    }

    @Transactional
    public OrdenCompraResponse crear(OrdenCompraRequest request) {
        validarCabecera(request);
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un ítem en el pedido");
        }

        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        OrdenDeCompra orden = new OrdenDeCompra();
        orden.setSucursal(sucursal);
        orden.setProveedor(proveedor);
        orden.setFechaOrden(LocalDateTime.now());
        orden.setFechaEntregaEstimada(request.getFechaEntregaEstimada());
        orden.setDescuento(defaultZero(request.getDescuento()));
        orden.setImpuesto(defaultZero(request.getImpuesto()));
        orden.setCondicionPago(request.getCondicionPago());
        orden.setObservaciones(request.getObservaciones());
        orden.setEstado(OrdenDeCompra.EstadoOrden.BORRADOR);
        orden.setResponsable(usuarioActual());
        orden.setNumero("OC-TMP-" + System.currentTimeMillis());
        orden.setDetalles(construirDetalles(orden, request.getDetalles(), request.getSucursalId(), request.getDepositoId()));
        recalcularTotales(orden);

        orden = ordenDeCompraRepository.saveAndFlush(orden);
        orden.setNumero("OC-" + orden.getId());
        orden = ordenDeCompraRepository.saveAndFlush(orden);
        return findById(orden.getId());
    }

    @Transactional
    public OrdenCompraResponse actualizar(Long id, OrdenCompraRequest request) {
        OrdenDeCompra orden = ordenDeCompraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + id));
        if (orden.getEstado() != OrdenDeCompra.EstadoOrden.BORRADOR) {
            throw new RuntimeException("Solo se pueden editar pedidos en estado BORRADOR");
        }
        validarCabecera(request);
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un ítem en el pedido");
        }

        Sucursal sucursal = sucursalRepository.findById(request.getSucursalId())
                .orElseThrow(() -> new RuntimeException("Sucursal no encontrada"));
        Proveedor proveedor = proveedorRepository.findById(request.getProveedorId())
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        orden.setSucursal(sucursal);
        orden.setProveedor(proveedor);
        orden.setFechaEntregaEstimada(request.getFechaEntregaEstimada());
        orden.setDescuento(defaultZero(request.getDescuento()));
        orden.setImpuesto(defaultZero(request.getImpuesto()));
        orden.setCondicionPago(request.getCondicionPago());
        orden.setObservaciones(request.getObservaciones());
        orden.getDetalles().clear();
        orden.getDetalles().addAll(construirDetalles(orden, request.getDetalles(), request.getSucursalId(), request.getDepositoId()));
        recalcularTotales(orden);
        ordenDeCompraRepository.saveAndFlush(orden);
        return findById(id);
    }

    @Transactional
    public OrdenCompraResponse confirmar(Long id) {
        OrdenDeCompra orden = ordenDeCompraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + id));
        if (orden.getEstado() != OrdenDeCompra.EstadoOrden.BORRADOR) {
            throw new RuntimeException("Solo se pueden confirmar pedidos en estado BORRADOR");
        }
        orden.setEstado(OrdenDeCompra.EstadoOrden.CONFIRMADA);
        ordenDeCompraRepository.save(orden);
        return findById(id);
    }

    @Transactional
    public OrdenCompraResponse cancelar(Long id) {
        OrdenDeCompra orden = ordenDeCompraRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pedido no encontrado con ID: " + id));
        if (orden.getEstado() == OrdenDeCompra.EstadoOrden.CANCELADA
                || orden.getEstado() == OrdenDeCompra.EstadoOrden.RECIBIDA_TOTAL) {
            throw new RuntimeException("No se puede cancelar el pedido en estado " + orden.getEstado());
        }
        orden.setEstado(OrdenDeCompra.EstadoOrden.CANCELADA);
        ordenDeCompraRepository.save(orden);
        return findById(id);
    }

    private void validarCabecera(OrdenCompraRequest request) {
        if (request.getSucursalId() == null) {
            throw new RuntimeException("Sucursal requerida");
        }
        if (request.getProveedorId() == null) {
            throw new RuntimeException("Proveedor requerido");
        }
    }

    private List<OrdenDeCompraDetalle> construirDetalles(
            OrdenDeCompra orden,
            List<OrdenCompraDetalleRequest> requests,
            Long sucursalId,
            Long depositoId
    ) {
        List<OrdenDeCompraDetalle> detalles = new ArrayList<>();
        for (OrdenCompraDetalleRequest req : requests) {
            if (req.getCantidadSolicitada() == null || req.getCantidadSolicitada() <= 0) {
                continue;
            }
            if (req.getPrecioUnitario() == null || req.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("El costo unitario debe ser mayor a cero en todas las líneas");
            }

            ArticuloService.ResultadoPedidoVariante resultado = articuloService.resolverOCrearParaPedido(
                    req, sucursalId, depositoId);

            OrdenDeCompraDetalle detalle = new OrdenDeCompraDetalle();
            detalle.setOrdenDeCompra(orden);
            detalle.setArticuloId(resultado.articuloId());
            detalle.setVarianteId(resultado.varianteId());
            detalle.setCantidadSolicitada(req.getCantidadSolicitada());
            detalle.setCantidadRecibida(0);
            detalle.setPrecioUnitario(req.getPrecioUnitario());
            detalle.setMargenPorcentaje(req.getMargenPorcentaje());
            detalle.setPrecioVenta(req.getPrecioVenta());
            detalle.setDescuentoPorcentaje(defaultZero(req.getDescuentoPorcentaje()));
            detalle.setSubtotal(calcularSubtotalLinea(req));
            detalle.setObservaciones(req.getObservaciones());
            detalle.setItemEnSistema(true);
            detalle.setCatalogoActivo(resultado.catalogoActivo());
            detalles.add(detalle);
        }

        if (detalles.isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un ítem válido en el pedido");
        }
        return detalles;
    }

    private BigDecimal calcularSubtotalLinea(OrdenCompraDetalleRequest req) {
        BigDecimal bruto = req.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(req.getCantidadSolicitada()));
        BigDecimal descPct = defaultZero(req.getDescuentoPorcentaje());
        BigDecimal descuento = bruto.multiply(descPct).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return bruto.subtract(descuento).setScale(2, RoundingMode.HALF_UP);
    }

    private void recalcularTotales(OrdenDeCompra orden) {
        BigDecimal subtotal = orden.getDetalles().stream()
                .map(OrdenDeCompraDetalle::getSubtotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        orden.setSubtotal(subtotal);
        BigDecimal descuento = defaultZero(orden.getDescuento());
        BigDecimal impuesto = defaultZero(orden.getImpuesto());
        orden.setTotal(subtotal.subtract(descuento).add(impuesto).setScale(2, RoundingMode.HALF_UP));
    }

    private OrdenCompraResponse toResponseResumido(OrdenDeCompra orden) {
        OrdenCompraResponse dto = new OrdenCompraResponse();
        dto.setId(orden.getId());
        dto.setNumero(orden.getNumero());
        dto.setFechaOrden(orden.getFechaOrden());
        dto.setFechaEntregaEstimada(orden.getFechaEntregaEstimada());
        dto.setSubtotal(orden.getSubtotal());
        dto.setDescuento(orden.getDescuento());
        dto.setImpuesto(orden.getImpuesto());
        dto.setTotal(orden.getTotal());
        dto.setEstado(orden.getEstado() != null ? orden.getEstado().name() : null);
        dto.setCondicionPago(orden.getCondicionPago());
        dto.setObservaciones(orden.getObservaciones());
        dto.setResponsable(orden.getResponsable());
        if (orden.getSucursal() != null) {
            dto.setSucursalId(orden.getSucursal().getId());
            dto.setSucursalNombre(orden.getSucursal().getNombre());
        }
        if (orden.getProveedor() != null) {
            dto.setProveedorId(orden.getProveedor().getId());
            dto.setProveedorNombre(
                    orden.getProveedor().getRazonSocial() != null
                            ? orden.getProveedor().getRazonSocial()
                            : orden.getProveedor().getNombre());
            dto.setProveedorCodigo(orden.getProveedor().getCodigo());
        }
        dto.setDetalles(List.of());
        return dto;
    }

    private OrdenCompraResponse toResponse(OrdenDeCompra orden) {
        OrdenCompraResponse dto = toResponseResumido(orden);
        dto.setFechaEntregaReal(orden.getFechaEntregaReal());
        dto.setDetalles(orden.getDetalles().stream()
                .map(this::toDetalleResponse)
                .collect(Collectors.toList()));
        return dto;
    }

    private OrdenCompraDetalleResponse toDetalleResponse(OrdenDeCompraDetalle detalle) {
        OrdenCompraDetalleResponse dto = new OrdenCompraDetalleResponse();
        dto.setId(detalle.getId());
        dto.setArticuloId(detalle.getArticuloId());
        dto.setVarianteId(detalle.getVarianteId());
        dto.setCantidadSolicitada(detalle.getCantidadSolicitada());
        dto.setCantidadRecibida(detalle.getCantidadRecibida());
        dto.setPrecioUnitario(detalle.getPrecioUnitario());
        dto.setMargenPorcentaje(detalle.getMargenPorcentaje());
        dto.setPrecioVenta(detalle.getPrecioVenta());
        dto.setDescuentoPorcentaje(detalle.getDescuentoPorcentaje());
        dto.setSubtotal(detalle.getSubtotal());
        dto.setObservaciones(detalle.getObservaciones());
        dto.setItemEnSistema(detalle.getItemEnSistema());
        dto.setCatalogoActivo(detalle.getCatalogoActivo());

        Articulo articulo = detalle.getArticulo();
        VarianteArticulo variante = detalle.getVariante();
        if (articulo != null) {
            dto.setCodigoArticulo(articulo.getCodigo());
            dto.setModelo(articulo.getModelo());
            dto.setMarca(articulo.getMarca() != null ? articulo.getMarca().getNombre() : null);
            dto.setCategoria(articulo.getCategoria() != null ? articulo.getCategoria().getNombre() : null);
            dto.setGenero(articulo.getGenero() != null ? articulo.getGenero().getNombre() : null);
            if (articulo.getTaxones() != null) {
                articulo.getTaxones().stream()
                        .map(TaxonArticulo::getTaxon)
                        .filter(Objects::nonNull)
                        .map(Taxon::getNombre)
                        .findFirst()
                        .ifPresent(dto::setSubCategoria);
            }
        }
        if (variante != null) {
            dto.setCodigoBarras(variante.getCodigoBarras());
            if (variante.getColor() != null) {
                dto.setColor(variante.getColor().getNombre());
            }
            if (variante.getTalle() != null) {
                dto.setTalle(variante.getTalle().getNumero());
                dto.setPaisTalle(variante.getTalle().getPais() != null ? variante.getTalle().getPais().name() : "AR");
            }
        }
        return dto;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getName();
    }
}
