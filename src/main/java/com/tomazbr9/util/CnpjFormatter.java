package com.tomazbr9.util;

public final class CnpjFormatter {

    private CnpjFormatter() {
    }

    public static String somenteNumeros(String cnpj) {

        if (cnpj == null) {
            return "";
        }

        if (!cnpj.matches("[0-9./\\-\\s]*")) {
            throw new IllegalArgumentException(
                    "O CNPJ deve conter apenas numeros "
                            + "e caracteres de formatacao."
            );
        }

        return cnpj.replaceAll("\\D", "");
    }

    public static String formatar(String cnpj) {

        String numeros = somenteNumeros(cnpj);

        if (numeros.length() != 14) {
            throw new IllegalArgumentException(
                    "O CNPJ deve conter 14 digitos."
            );
        }

        return formatarParcial(numeros);
    }

    public static String formatarParcial(String cnpj) {

        String numeros = somenteNumeros(cnpj);

        if (numeros.length() > 14) {
            throw new IllegalArgumentException(
                    "O CNPJ nao pode ultrapassar 14 digitos."
            );
        }

        StringBuilder resultado = new StringBuilder();

        for (int i = 0; i < numeros.length(); i++) {

            if (i == 2 || i == 5) {
                resultado.append('.');
            } else if (i == 8) {
                resultado.append('/');
            } else if (i == 12) {
                resultado.append('-');
            }

            resultado.append(numeros.charAt(i));
        }

        return resultado.toString();
    }
}
