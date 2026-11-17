package com.tomazbr9.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ItemVenda {

    private Long id;
    private Produto produto;
    private Long vendaId;

    private Double quantidade;
    private Double valorUnitario;
    private Double subtotal;
}
