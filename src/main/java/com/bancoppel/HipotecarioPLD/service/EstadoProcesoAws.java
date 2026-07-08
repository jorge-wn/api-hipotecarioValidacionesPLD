package com.bancoppel.HipotecarioPLD.service;

import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Service;

@Service
public class EstadoProcesoAws {

    private final AtomicBoolean errorEstructura = new AtomicBoolean(false);

    public void registrarErrorEstructura() {
        errorEstructura.set(true);
    }

    public boolean tieneErrorEstructura() {
        return errorEstructura.get();
    }

    public void limpiar() {
        errorEstructura.set(false);
    }
}