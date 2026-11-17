package com.tomazbr9.model;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class Estabelecimento {

    private Long id;
    private String nome;
    private String cnpj;
    private String endereco;
    private String cidade;
    private String uf;
    private String cep;
    private String telefone;

}
