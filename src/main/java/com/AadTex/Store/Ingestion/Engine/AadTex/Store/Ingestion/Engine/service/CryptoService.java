package com.AadTex.Store.Ingestion.Engine.AadTex.Store.Ingestion.Engine.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

@Service
public class CryptoService {

    // Descifra con AES-128 / ECB / PKCS5Padding.
    // Recibe los bytes cifrados y devuelve los bytes en claro, que solo existen en memoria.
    public byte[] decrypt(byte[] encrypted, String secret) throws GeneralSecurityException {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(encrypted);
    }
}