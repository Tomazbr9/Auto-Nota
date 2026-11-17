package com.tomazbr9.util;

import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Random;

public final class NotaFiscalUtil {

    private static final SecureRandom RANDOM =
            new SecureRandom();

    private NotaFiscalUtil() {
    }

    // Identificador interno, sem validade fiscal
    public static String gerarCodigo(){

        StringBuilder numeros = new StringBuilder();

        for(int i = 0; i < 43; i++){
            numeros.append(RANDOM.nextInt(10));
        }

        StringBuilder formatado = new StringBuilder();

        for(int i = 0; i < numeros.length(); i++){
            if(i > 0 && i % 4 == 0){
                formatado.append(" ");
            }
            formatado.append(i);
        }

        return formatado.toString();
    }

    // MD5 didatico: nao e assinatura fiscal
    public static String gerarMd5(String conteudo) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("MD5");

            byte[] hash = digest.digest(
                    conteudo.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "Algoritmo MD5 indisponivel.", e
            );
        }
    }
}
