package com.tomazbr9.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Produto {

    private Long id;
    private String nome;
    private Double preco;

    @Override
    public String toString() {
        return this.nome + " - R$ " + String.format("%.2f", this.preco);
    }
}
