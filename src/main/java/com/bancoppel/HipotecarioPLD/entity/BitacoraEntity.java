package com.bancoppel.HipotecarioPLD.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.bancoppel.HipotecarioPLD.dto.BitacoraDTO;
@Data
@AllArgsConstructor
@NoArgsConstructor

@Entity
@Table(name = "bitacora")
public class BitacoraEntity { 
   
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // O GenerationType.AUTO, SEQUENCE, TABLE según tu base de datos
    @Column(name = "idBitacora")
    private Integer idBitacora;
	   
    @Column(name= "nomArchivo")
    private String nomArchivo;
    @Column(name= "estatus")
    private String estatus;
    @Column(name= "registros")
    private Integer registros;
    @Column(name= "fechaprocess")
    private LocalDateTime fechaProcess;
    @Column(name= "registros_procesados")
    private Integer registros_procesados;
    @Column(name= "registros_con_error")
    private Integer registros_con_error;
    
    public BitacoraEntity(BitacoraDTO BitacoraDTO) {
    	this.nomArchivo = BitacoraDTO.getNomArchivo();
         this.estatus = BitacoraDTO.getEstatus();
         this.registros = BitacoraDTO.getRegistros();
         this.fechaProcess = BitacoraDTO.getFechaProcess();
         this.registros_procesados = BitacoraDTO.getRegistros_procesados();
         this.registros_con_error = BitacoraDTO.registros_con_error();
    }
}
