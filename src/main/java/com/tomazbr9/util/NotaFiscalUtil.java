package com.tomazbr9.util;

import java.security.SecureRandom;

public class NotaFiscalUtil {

    private static final SecureRandom radom = new SecureRandom();

    public static String gerarCodigo(){

        StringBuilder numeros = new StringBuilder();

        for(int i = 0; i < 43; i++){
            numeros.append(radom.nextInt(10));
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
}
