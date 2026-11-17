
package com.tomazbr9.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class TributacaoSimulada {

    // Percentuais ficticios para aprendizagem.
    // Nao representam aliquotas fiscais reais.
    private static final BigDecimal ICMS =
            new BigDecimal("0.18");

    private static final BigDecimal FEDERAIS =
            new BigDecimal("0.10");

    private TributacaoSimulada() {
    }

    public record Resultado(
            BigDecimal icms,
            BigDecimal federais,
            BigDecimal total
    ) {
    }

    public static Resultado calcular(BigDecimal valorVenda) {

        if (valorVenda == null
                || valorVenda.signum() < 0) {
            throw new IllegalArgumentException(
                    "Valor da venda invalido."
            );
        }

        BigDecimal icms = calcularParcela(
                valorVenda, ICMS
        );

        BigDecimal federais = calcularParcela(
                valorVenda, FEDERAIS
        );

        return new Resultado(
                icms,
                federais,
                icms.add(federais)
        );
    }

    private static BigDecimal calcularParcela(
            BigDecimal valor,
            BigDecimal percentual
    ) {
        return valor.multiply(percentual)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
