package com.bancoppel.HipotecarioPLD.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.bancoppel.HipotecarioPLD.dto.BitacoraDetalleDTO;
@Data
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "bitacoradetalle")
public class BitacoraDetalleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // O GenerationType.AUTO, SEQUENCE, TABLE según tu base de datos
    @Column(name = "idBitacora")
    private Integer idBitacora;

    @Column(name= "nomArchivo")
    private String nomArchivo;
    @Column(name= "estatus")
    private String estatus;
    @Column(name= "registros")
    private String registros;
    @Column(name= "fechaprocess")
    private LocalDateTime fechaProcess; 
    
    public BitacoraDetalleEntity(BitacoraDetalleDTO BitacoraDetalleDTO) {
    	this.nomArchivo = BitacoraDetalleDTO.getNomArchivo();
         this.estatus = BitacoraDetalleDTO.getEstatus();
         this.registros = BitacoraDetalleDTO.getRegistros();
         this.fechaProcess = BitacoraDetalleDTO.getFechaProcess(); 
    }
}