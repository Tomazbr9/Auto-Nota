package com.tomazbr9.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Venda {
    private Long id;
    private LocalDateTime dataHora;
    private String cnpjCpf;
    private String placa;
    private Integer km;
    private Integer bico;
    private Double total;

    private List<ItemVenda> itens = new ArrayList<>();


    public void addItem(ItemVenda item) {
        this.itens.add(item);
    }
}
