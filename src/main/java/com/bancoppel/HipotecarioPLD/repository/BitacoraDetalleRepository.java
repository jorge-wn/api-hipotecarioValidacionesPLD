package com.bancoppel.HipotecarioPLD.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository; // Asegúrate de tener esta anotación

import com.bancoppel.HipotecarioPLD.entity.BitacoraDetalleEntity;

import java.time.LocalDateTime;

@Repository
public interface BitacoraDetalleRepository extends JpaRepository<BitacoraDetalleEntity, Integer> {

    @Modifying
    @Transactional
    @Query(value = """
            insert into bitacoradetalle (
                nom_archivo,
                estatus,
                registros,
                fechaprocess)
            values (
                :nom_archivo,
                :estatus,
                :registros,
                :fechaprocess)
            """, nativeQuery = true)

    int guardarEnBitacoraDetalle(
            @Param("nom_archivo") String nom_archivo,
            @Param("estatus") String estatus,
            @Param("registros") String registros,
            @Param("fechaprocess") LocalDateTime fechaprocess);
}