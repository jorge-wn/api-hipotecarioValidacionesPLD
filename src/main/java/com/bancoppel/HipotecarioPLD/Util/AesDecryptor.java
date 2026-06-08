package com.bancoppel.HipotecarioPLD.Util;
 
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;
 
public class AesDecryptor {
 
    // Cambié algoritmo a AES/GCM/NoPadding

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    // Longitud tag GCM en bits (generalmente 128)
    private static final int GCM_TAG_LENGTH = 128;
    private final SecretKeySpec keySpec;
    private final GCMParameterSpec gcmSpec;
 
 
 
    // Constructor sin modificar la firma pública

    public AesDecryptor(char[] base64Key, String base64Iv) {
        byte[] keyBytes = Base64.getDecoder().decode(new String(base64Key));
        byte[] ivBytes = Base64.getDecoder().decode(base64Iv);
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
        // Usar GCMParameterSpec con tag length y vector de inicialización
        this.gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, ivBytes);
 
        // Limpieza segura de claves temporales
        Arrays.fill(base64Key, '\0');
        Arrays.fill(keyBytes, (byte) 0);
    }
 
    public String decrypt(String encryptedBase64) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        byte[] decodedEncrypted = Base64.getDecoder().decode(encryptedBase64);
        byte[] decryptedBytes = cipher.doFinal(decodedEncrypted);
        String result = new String(decryptedBytes, StandardCharsets.UTF_8);

        // Limpieza de datos sensibles
        Arrays.fill(decodedEncrypted, (byte) 0);
        Arrays.fill(decryptedBytes, (byte) 0);
        return result;
    }
 
    public char[] decryptToCharArray(String encryptedBase64) throws Exception {
        String decrypted = decrypt(encryptedBase64);
        char[] chars = decrypted.toCharArray();
        decrypted = null;
        return chars;
    }
}