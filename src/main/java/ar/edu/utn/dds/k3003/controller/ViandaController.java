package ar.edu.utn.dds.k3003.controller;

import ar.edu.utn.dds.k3003.app.Fachada;
import ar.edu.utn.dds.k3003.facades.dtos.EstadoViandaEnum;
import ar.edu.utn.dds.k3003.facades.dtos.ViandaDTO;
import ar.edu.utn.dds.k3003.model.HeladeraDestino;
import ar.edu.utn.dds.k3003.model.Respuesta;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.Context;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.step.StepMeterRegistry;
import org.mockito.internal.matchers.Null;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

public class ViandaController {
    private final Fachada fachada;
    private StepMeterRegistry stepMeterRegistry;
    private Counter contadorViandas;
    private Counter heladerasCambiadas;
    private Counter estadoModificado;

    public ViandaController(Fachada fachada, StepMeterRegistry stepMeterRegistry) {
        this.fachada = fachada;
        this.stepMeterRegistry = stepMeterRegistry;
        this.contadorViandas = stepMeterRegistry.counter("ddsViandas.viandasCreadas");
        this.heladerasCambiadas = stepMeterRegistry.counter("ddsViandas.heladerasCambiadas");
        this.estadoModificado = stepMeterRegistry.counter("ddsViandas.cambiosDeEstado");
    }

    public void agregar(Context context) { //heladeraId, colaboradorId, codigoQR
        // String codigoQR,
        //      LocalDateTime fechaElaboracion,
        //      EstadoViandaEnum estado,
        //      Long colaboradorId,
        //      Integer heladeraId) {
        ViandaDTO viandaDto = context.bodyAsClass(ViandaDTO.class);
        ViandaDTO viandaDTOFix = new ViandaDTO(viandaDto.getCodigoQR(), LocalDateTime.now(), EstadoViandaEnum.PREPARADA, viandaDto.getColaboradorId(), null);
        try {
            var viandaDtoRta = this.fachada.agregar(viandaDTOFix);
            contadorViandas.increment();
            context.json(viandaDtoRta);
            context.status(HttpStatus.CREATED);
        } catch (NoSuchElementException e){
            //fachada.getViandaRepository().getEntityManagerFactory();
            throw new BadRequestResponse("Error de solicitud. El QR ya existe");
        }
    }

    public void buscarPorColaboradorIdMesYAnio(Context context) {
        var colabId = context.queryParamAsClass("colaboradorId", Long.class).get();
        var anio = context.queryParamAsClass("anio", Integer.class).get();
        var mes = context.queryParamAsClass("mes", Integer.class).get();
        var ViandaDtoRta = this.fachada.viandasDeColaborador(colabId, mes, anio);
        context.json(ViandaDtoRta);
    }

    public void buscarPorQr(Context context) {
        var qr = context.pathParamAsClass("qr", String.class).get();
        try {
            var ViandaDtoRta = this.fachada.buscarXQR(qr);
            context.json(ViandaDtoRta);
        } catch (NoSuchElementException e){
            throw new BadRequestResponse("Error de solicitud. El QR NO existe");
        }

    }

    public void verificarVencimiento(Context context) {
        var qr = context.pathParamAsClass("qr", String.class).get();
        var respuesta = new Respuesta(this.fachada.evaluarVencimiento(qr));
        context.json(respuesta);
    }

    public void modificarHeladera(Context context) {
        var qr = context.pathParamAsClass("qr", String.class).get();
        HeladeraDestino heladera = context.bodyAsClass(HeladeraDestino.class);
        var ViandaDtoRta = this.fachada.modificarHeladera(qr, heladera.getHeladeraDestino());
        heladerasCambiadas.increment();
        context.json(ViandaDtoRta);
    }

    public void modificarEstado(Context context) {
        var qr = context.pathParamAsClass("qr", String.class).get();
        EstadoViandaEnum estado = context.bodyAsClass(EstadoViandaEnum.class);
        var respuesta = this.fachada.modificarEstado(qr, estado);
        estadoModificado.increment();
        context.json(respuesta);
    }

    public void listar(Context context) {
        var ViandaDtoRta = this.fachada.viandasLista();
        context.json(ViandaDtoRta);
    }
}
