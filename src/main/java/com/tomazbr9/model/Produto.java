
package com.tomazbr9.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Locale;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Produto {

    private Long id;
    private String nome;
    private Double preco;
    private String unidade = "L";

    @Override
    public String toString() {
        return nome + " - "
                + String.format(Locale.US, "R$ %.2f", preco)
                + "/" + unidade;
    }
}
