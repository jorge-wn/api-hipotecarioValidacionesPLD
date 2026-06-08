package com.bancoppel.HipotecarioPLD.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bancoppel.HipotecarioPLD.entity.BitacoraEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BitacoraRepository extends JpaRepository<BitacoraEntity, Integer> {

    @Modifying
    @Transactional
    @Query(value = """
            insert into bitacora (
                nom_archivo,
                estatus,
                registros,
                registros_procesados,
                registros_con_error,
                fechaprocess)
            values (
                :nom_archivo,
                :estatus,
                :registros,
                :registros_procesados,
                :registros_con_error,
                :fechaprocess)
            """, nativeQuery = true)

    int guardarEnBitacora(
            @Param("nom_archivo") String nom_archivo,
            @Param("estatus") String estatus,
            @Param("registros") Integer registros,
            @Param("registros_procesados") Integer registros_procesados,
            @Param("registros_con_error") Integer registros_con_error,
            @Param("fechaprocess") LocalDateTime fechaprocess);
}
