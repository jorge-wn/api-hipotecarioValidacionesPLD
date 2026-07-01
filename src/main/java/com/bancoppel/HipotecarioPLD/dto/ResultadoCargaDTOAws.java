package com.bancoppel.HipotecarioPLD.dto;
import java.time.OffsetDateTime;
import java.util.List;
public class ResultadoCargaDTOAws {


    private List<String> archivosProcesados;
    private String mensaje;
    private String resultado;
    private String rutaS3;
    private String rutaSftp;
    private OffsetDateTime timestamp;
    private String BucketS3;
    private String PrefixS3;
    
    
    public String getRutaS3() {
		return rutaS3;
	}

	public void setRutaS3(String rutaS3) {
		this.rutaS3 = rutaS3;
	}

	public String getBucketS3() {
		return BucketS3;
	}

	public void setBucketS3(String bucketS3) {
		BucketS3 = bucketS3;
	}

	public String getPrefixS3() {
		return PrefixS3;
	}

	public void setPrefixS3(String prefixS3) {
		PrefixS3 = prefixS3;
	}

	public ResultadoCargaDTOAws() {
    }

    public List<String> getArchivosProcesados() {
        return archivosProcesados;
    }

    public void setArchivosProcesados(List<String> archivosProcesados) {
        this.archivosProcesados = archivosProcesados;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public String getRutaSftp() {
        return rutaSftp;
    }

    public void setRutaSftp(String rutaSftp) {
        this.rutaSftp = rutaSftp;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
