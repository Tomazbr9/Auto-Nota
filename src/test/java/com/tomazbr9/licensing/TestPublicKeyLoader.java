package com.tomazbr9.licensing;

import java.security.PublicKey;

public class TestPublicKeyLoader {

    public static void main(String[] args)
            throws Exception {

        PublicKey chave =
                LicensePublicKeyLoader.carregar();

        System.out.println(
                "Chave publica carregada com sucesso!"
        );

        System.out.println(
                "Algoritmo: " + chave.getAlgorithm()
        );

        System.out.println(
                "Formato: " + chave.getFormat()
        );

        if (chave.getEncoded().length == 0) {
            throw new AssertionError(
                    "Chave publica vazia."
            );
        }

        System.out.println(
                "Teste concluido!"
        );
    }
}
