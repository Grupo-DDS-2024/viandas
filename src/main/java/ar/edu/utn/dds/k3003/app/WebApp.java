package ar.edu.utn.dds.k3003.app;

import ar.edu.utn.dds.k3003.clients.HeladerasProxy;
import ar.edu.utn.dds.k3003.controller.ViandaController;
import ar.edu.utn.dds.k3003.model.Vianda;
import ar.edu.utn.dds.k3003.repositories.ViandaRepository;
import ar.edu.utn.dds.k3003.utils.DataDogUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import ar.edu.utn.dds.k3003.facades.dtos.Constants;
import io.javalin.micrometer.MicrometerPlugin;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

public class WebApp {
    public static void main(String[] args) {
        EntityManagerFactory entityManagerFactory = startEntityManagerFactory();
        var fachada = new Fachada(entityManagerFactory);
        var objectMapper = createObjectMapper();
        var DDUtils = new DataDogUtils("Viandas");
        var registro = DDUtils.getRegistro();

        // Metricas
        final var gauge = registro.gauge("ddsViandas.unGauge", new AtomicInteger(0));

        // Config
        final var micrometerPlugin = new MicrometerPlugin(config -> config.registry = registro);
        Integer port = Integer.parseInt(System.getProperty("PORT", "8081"));
        Javalin app = Javalin.create(javalinConfig -> {
            javalinConfig.registerPlugin(micrometerPlugin);
        }).start(port);


        var viandaController = new ViandaController(fachada, registro);
        fachada.setHeladerasProxy(new HeladerasProxy(objectMapper));

        app.post("/viandas", viandaController::agregar);
        app.get("/viandas", viandaController::listar);
        app.get("/viandas/search/findByColaboradorIdAndAnioAndMes", viandaController::buscarPorColaboradorIdMesYAnio);
        app.get("/viandas/{qr}", viandaController::buscarPorQr);
        app.get("/viandas/{qr}/vencida", viandaController::verificarVencimiento);
        app.patch("/viandas/{qr}", viandaController::modificarHeladera);
        app.patch("/viandas/{qr}/estado", viandaController::modificarEstado);
    }

    public static ObjectMapper createObjectMapper() {
        var objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        var sdf = new SimpleDateFormat(Constants.DEFAULT_SERIALIZATION_FORMAT, Locale.getDefault());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        objectMapper.setDateFormat(sdf);
        return objectMapper;
    }

    public static EntityManagerFactory startEntityManagerFactory() {
// https://stackoverflow.com/questions/8836834/read-environment-variables-in-persistence-xml-file
        Map<String, String> env = System.getenv();
        Map<String, Object> configOverrides = new HashMap<String, Object>();
        String[] keys = new String[]{"javax.persistence.jdbc.url", "javax.persistence.jdbc.user",
                "javax.persistence.jdbc.password", "javax.persistence.jdbc.driver", "hibernate.hbm2ddl.auto",
                "hibernate.connection.pool_size", "hibernate.show_sql"};
        for (String key : keys) {
            if (env.containsKey(key)) {
                String value = env.get(key);
                configOverrides.put(key, value);
            }
        }
        return Persistence.createEntityManagerFactory("defaultdb", configOverrides);
    }
}
