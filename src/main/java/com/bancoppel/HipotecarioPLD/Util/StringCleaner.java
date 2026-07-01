package com.bancoppel.HipotecarioPLD.Util;
import java.text.Normalizer;

public class StringCleaner {

    public static String cleanText(String input) {
    	    if (input == null) return null;
   	 
    	    // 1. Normalizar texto para separar acentos
    	    String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
    	 
    	    // 2. Eliminar caracteres no ASCII (acentos)
    	    String noAccents = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    	 
    	    // 3. Eliminar signos de puntuación excepto letras, números, espacios, pipes, diagonales y guiones
    	    String cleaned = noAccents.replaceAll("[^\\p{L}\\p{Nd}| /-]+", "");
    	 
    	    // 4. Conservamos los espacios tal cual, solo hacemos trim para quitar espacios al inicio y final
    	    return cleaned.trim();
    	}
}
