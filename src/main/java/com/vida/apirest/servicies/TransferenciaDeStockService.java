package com.vida.apirest.servicies;

import com.vida.apirest.dto.almacen.*;
import com.vida.apirest.dto.common.PageResponse;
import com.vida.apirest.model.almacen.*;
import com.vida.apirest.model.articulo.Articulo;
import com.vida.apirest.model.articulo.VarianteArticulo;
import com.vida.apirest.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransferenciaDeStockService {

    private final TransferenciaDeStockRepository transferenciaRepository;
    private final TransferenciaStockQueryRepository transferenciaStockQueryRepository;
    private final DepositoRepository depositoRepository;
    private final StockRepository stockRepository;
    private final StockOperacionesService stockOperacionesService;
    private final StockMovimientoRepository stockMovimientoRepository;
    private final ArticuloRepository articuloRepository;
    private final VarianteArticuloRepository varianteArticuloRepository;

    @Transactional(readOnly = true)
    public PageResponse<StockDepositoResponse> listarStockDeposito(
            Long depositoId, String q, int page, int size) {
        if (depositoId == null) {
            throw new RuntimeException("Depósito origen requerido");
        }
        if (!depositoRepository.existsById(depositoId)) {
            throw new RuntimeException("Depósito no encontrado con ID: " + depositoId);
        }
        return transferenciaStockQueryRepository.findStockByDepositoPage(depositoId, q, page, size);
    }

    @Transactional(readOnly = true)
    public List<TransferenciaStockResponse> listar(Long depositoOrigenId, Long sucursalDestinoId) {
        return transferenciaRepository.findAllWithRelations(depositoOrigenId, sucursalDestinoId).stream()
                .map(this::mapResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransferenciaStockResponse obtener(Long id) {
        return mapResponse(buscarTransferencia(id));
    }

    @Transactional
    public TransferenciaStockResponse crear(TransferenciaStockCreateRequest request) {
        validarRequest(request);

        Deposito origen = depositoRepository.findById(request.getDepositoOrigenId())
                .orElseThrow(() -> new RuntimeException("Depósito origen no encontrado"));
        Deposito destino = depositoRepository.findById(request.getDepositoDestinoId())
                .orElseThrow(() -> new RuntimeException("Depósito destino no encontrado"));

        if (origen.getId().equals(destino.getId())) {
            throw new RuntimeException("El depósito origen y destino deben ser distintos");
        }
        if (Boolean.FALSE.equals(origen.getActivo()) || Boolean.FALSE.equals(destino.getActivo())) {
            throw new RuntimeException("Los depósitos deben estar activos");
        }

        TransferenciaDeStock transferencia = new TransferenciaDeStock();
        transferencia.setNumero("TS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        transferencia.setDepositoOrigen(origen);
        transferencia.setDepositoDestino(destino);
        transferencia.setDescripcion(request.getDescripcion());
        transferencia.setEstado(TransferenciaDeStock.EstadoTransferencia.RECIBIDA_TOTAL);
        transferencia.setFechaEnvio(LocalDateTime.now());
        transferencia.setFechaRecepcion(LocalDateTime.now());

        Set<String> claves = new HashSet<>();
        for (TransferenciaStockDetalleRequest detReq : request.getDetalles()) {
            validarDetalle(detReq);
            String clave = detReq.getVarianteId() + "-" + detReq.getArticuloId();
            if (!claves.add(clave)) {
                continue;
            }

            VarianteArticulo variante = varianteArticuloRepository.findById(detReq.getVarianteId())
                    .orElseThrow(() -> new RuntimeException("Variante no encontrada: " + detReq.getVarianteId()));
            Articulo articulo = articuloRepository.findById(detReq.getArticuloId())
                    .orElseThrow(() -> new RuntimeException("Artículo no encontrado: " + detReq.getArticuloId()));

            Stock stockOrigen = stockRepository
                    .findByDepositoIdAndArticuloIdAndVarianteId(origen.getId(), articulo.getId(), variante.getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Sin stock en depósito origen para variante " + variante.getId()));
            Stock stockDestinoExistente = stockRepository
                    .findByDepositoIdAndArticuloIdAndVarianteId(destino.getId(), articulo.getId(), variante.getId())
                    .orElse(null);

            List<Long> lockIds = new ArrayList<>();
            lockIds.add(stockOrigen.getId());
            if (stockDestinoExistente != null) {
                lockIds.add(stockDestinoExistente.getId());
            }
            Map<Long, Stock> locked = stockOperacionesService.lockAllById(lockIds);
            stockOrigen = locked.get(stockOrigen.getId());

            descontarOrigen(stockOrigen, detReq.getCantidad(), transferencia.getNumero());
            incrementarDestino(destino, articulo, variante, detReq.getCantidad(), transferencia.getNumero(),
                    stockDestinoExistente != null ? locked.get(stockDestinoExistente.getId()) : null);

            TransferenciaDetalleStock detalle = new TransferenciaDetalleStock();
            detalle.setTransferencia(transferencia);
            detalle.setArticulo(articulo);
            detalle.setVariante(variante);
            detalle.setCantidadEnviada(detReq.getCantidad());
            detalle.setCantidadRecibida(detReq.getCantidad());
            transferencia.getDetalles().add(detalle);
        }

        if (transferencia.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos una variante válida");
        }

        transferencia = transferenciaRepository.save(transferencia);
        return mapResponse(transferenciaRepository.findByIdWithRelations(transferencia.getId()).orElse(transferencia));
    }

    private void validarRequest(TransferenciaStockCreateRequest request) {
        if (request.getDepositoOrigenId() == null) {
            throw new RuntimeException("Depósito origen requerido");
        }
        if (request.getDepositoDestinoId() == null) {
            throw new RuntimeException("Depósito destino requerido");
        }
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe incluir al menos un artículo a transferir");
        }
    }

    private void validarDetalle(TransferenciaStockDetalleRequest detReq) {
        if (detReq.getArticuloId() == null || detReq.getVarianteId() == null) {
            throw new RuntimeException("Cada línea requiere articuloId y varianteId");
        }
        if (detReq.getCantidad() == null || detReq.getCantidad() <= 0) {
            throw new RuntimeException("La cantidad debe ser mayor a cero");
        }
    }

    private void descontarOrigen(Stock stock, int cantidad, String referencia) {
        int disponible = stock.getCantidadDisponible() != null ? stock.getCantidadDisponible() : 0;
        if (disponible < cantidad) {
            throw new RuntimeException("Stock insuficiente en depósito origen para variante "
                    + (stock.getVariante() != null ? stock.getVariante().getId() : stock.getArticulo().getId()));
        }

        int actual = stock.getCantidadActual() != null ? stock.getCantidadActual() : 0;
        int nuevoDisponible = disponible - cantidad;
        stock.setCantidadDisponible(nuevoDisponible);
        stock.setCantidadActual(Math.max(0, actual - cantidad));
        stockRepository.save(stock);

        registrarMovimiento(stock, StockMovimiento.TipoMovimiento.SALIDA_TRANSFERENCIA,
                cantidad, disponible, nuevoDisponible, referencia, "Salida por transferencia de stock");
    }

    private void incrementarDestino(
            Deposito depositoDestino,
            Articulo articulo,
            VarianteArticulo variante,
            int cantidad,
            String referencia,
            Stock stockDestinoLockeado
    ) {
        Stock stockDestino = stockDestinoLockeado;
        if (stockDestino == null) {
            stockDestino = stockRepository
                    .findByDepositoIdAndArticuloIdAndVarianteId(depositoDestino.getId(), articulo.getId(), variante.getId())
                    .map(stockOperacionesService::lock)
                    .orElseGet(() -> crearStockDestino(depositoDestino, articulo, variante));
        }

        int disponible = stockDestino.getCantidadDisponible() != null ? stockDestino.getCantidadDisponible() : 0;
        int actual = stockDestino.getCantidadActual() != null ? stockDestino.getCantidadActual() : 0;
        int nuevoDisponible = disponible + cantidad;

        stockDestino.setCantidadDisponible(nuevoDisponible);
        stockDestino.setCantidadActual(actual + cantidad);
        stockRepository.save(stockDestino);

        registrarMovimiento(stockDestino, StockMovimiento.TipoMovimiento.INGRESO_TRANSFERENCIA,
                cantidad, disponible, nuevoDisponible, referencia, "Ingreso por transferencia de stock");
    }

    private Stock crearStockDestino(Deposito deposito, Articulo articulo, VarianteArticulo variante) {
        Stock stock = new Stock();
        stock.setDeposito(deposito);
        stock.setSucursal(deposito.getSucursal());
        stock.setArticulo(articulo);
        stock.setVariante(variante);
        stock.setCantidadActual(0);
        stock.setCantidadDisponible(0);
        stock.setCantidadReservada(0);
        stock.setCantidadMinima(0);
        return stockRepository.save(stock);
    }

    private void registrarMovimiento(
            Stock stock,
            StockMovimiento.TipoMovimiento tipo,
            int cantidad,
            int saldoAnterior,
            int saldoNuevo,
            String referencia,
            String descripcion
    ) {
        StockMovimiento mov = new StockMovimiento();
        mov.setStock(stock);
        mov.setTipo(tipo);
        mov.setCantidad(cantidad);
        mov.setSaldoAnterior(saldoAnterior);
        mov.setSaldoNuevo(saldoNuevo);
        mov.setReferencia(referencia);
        mov.setDescripcion(descripcion);
        mov.setUsuario("sistema");
        stockMovimientoRepository.save(mov);
    }

    private TransferenciaDeStock buscarTransferencia(Long id) {
        return transferenciaRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new RuntimeException("Transferencia no encontrada"));
    }

    private TransferenciaStockResponse mapResponse(TransferenciaDeStock transferencia) {
        TransferenciaStockResponse r = new TransferenciaStockResponse();
        r.setId(transferencia.getId());
        r.setNumero(transferencia.getNumero());
        r.setEstado(transferencia.getEstado().name());
        r.setDescripcion(transferencia.getDescripcion());
        r.setFechaEnvio(transferencia.getFechaEnvio());
        r.setFechaRecepcion(transferencia.getFechaRecepcion());
        r.setCreatedAt(transferencia.getCreatedAt());

        Deposito origen = transferencia.getDepositoOrigen();
        Deposito destino = transferencia.getDepositoDestino();
        r.setDepositoOrigenId(origen.getId());
        r.setDepositoOrigenNombre(origen.getNombre());
        r.setSucursalOrigenId(origen.getSucursal().getId());
        r.setSucursalOrigenNombre(origen.getSucursal().getNombre());
        r.setDepositoDestinoId(destino.getId());
        r.setDepositoDestinoNombre(destino.getNombre());
        r.setSucursalDestinoId(destino.getSucursal().getId());
        r.setSucursalDestinoNombre(destino.getSucursal().getNombre());

        int totalUnidades = 0;
        List<TransferenciaStockDetalleResponse> detalles = new ArrayList<>();
        for (TransferenciaDetalleStock det : transferencia.getDetalles()) {
            TransferenciaStockDetalleResponse dr = new TransferenciaStockDetalleResponse();
            dr.setId(det.getId());
            dr.setArticuloId(det.getArticulo().getId());
            if (det.getVariante() != null) {
                dr.setVarianteId(det.getVariante().getId());
                dr.setCodigo(det.getVariante().getCodigoBarras() != null
                        ? det.getVariante().getCodigoBarras()
                        : String.valueOf(det.getArticulo().getId()));
                if (det.getVariante().getTalle() != null) {
                    dr.setTalle(det.getVariante().getTalle().getNumero());
                }
                if (det.getVariante().getColor() != null) {
                    dr.setColor(det.getVariante().getColor().getNombre());
                }
            }
            String marca = det.getArticulo().getMarca() != null ? det.getArticulo().getMarca().getNombre() : "";
            String modelo = det.getArticulo().getModelo() != null ? det.getArticulo().getModelo() : "";
            dr.setDescripcion((marca + " " + modelo).trim());
            dr.setCantidadEnviada(det.getCantidadEnviada());
            dr.setCantidadRecibida(det.getCantidadRecibida());
            totalUnidades += det.getCantidadEnviada() != null ? det.getCantidadEnviada() : 0;
            detalles.add(dr);
        }
        r.setDetalles(detalles);
        r.setTotalUnidades(totalUnidades);
        return r;
    }
}
