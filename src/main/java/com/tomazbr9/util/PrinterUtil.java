package com.tomazbr9.util;

import com.github.anastaciocintra.escpos.EscPos;
import com.github.anastaciocintra.escpos.EscPosConst;
import com.github.anastaciocintra.escpos.Style;
import com.github.anastaciocintra.output.PrinterOutputStream;

import com.tomazbr9.dao.EstabelecimentoDAO;
import com.tomazbr9.model.Estabelecimento;
import com.tomazbr9.model.ItemVenda;
import com.tomazbr9.model.Venda;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class PrinterUtil {

    private static final int LARGURA_LINHA = 40;

    public static void imprimirCupom(Venda venda) throws Exception {

        // 1. Recupera os dados do estabelecimento

        EstabelecimentoDAO estabelecimentoDAO =
                new EstabelecimentoDAO();

        Estabelecimento estabelecimento;

        try {
            estabelecimento = estabelecimentoDAO.buscar()
                    .orElseThrow(() -> new IllegalStateException(
                            "Configure o estabelecimento antes de imprimir."
                    ));

        } catch (SQLException e) {
            throw new Exception(
                    "Erro ao consultar o estabelecimento.", e
            );
        }

        // 2. Calcula os tributos simulados

        BigDecimal valorVenda =
                BigDecimal.valueOf(venda.getTotal());

        TributacaoSimulada.Resultado tributos =
                TributacaoSimulada.calcular(valorVenda);

        // 3. Gera data, codigo e hash

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "dd/MM/yyyy HH:mm:ss"
                );

        String dataEmissao =
                venda.getDataHora().format(formatter);

        String codigoSimulacao =
                NotaFiscalUtil.gerarCodigo();

        String conteudoHash =
                codigoSimulacao + "|"
                        + venda.getId() + "|"
                        + dataEmissao + "|"
                        + estabelecimento.getCnpj() + "|"
                        + valorVenda.toPlainString() + "|"
                        + tributos.total().toPlainString();

        String hashMd5 =
                NotaFiscalUtil.gerarMd5(conteudoHash);

        // 4. Busca a impressora padrao

        PrintService printService =
                PrinterOutputStream.getDefaultPrintService();

        if (printService == null) {

            PrintService[] servicosDisponiveis =
                    PrintServiceLookup.lookupPrintServices(
                            null, null
                    );

            if (servicosDisponiveis.length > 0) {
                printService = servicosDisponiveis[0];

            } else {
                throw new Exception(
                        "Nenhuma impressora encontrada no sistema."
                );
            }
        }

        // 5. Conecta na impressora

        try (PrinterOutputStream printerOutputStream =
                     new PrinterOutputStream(printService);

             EscPos escpos = new EscPos(printerOutputStream)) {

            // === ESTILOS ===

            Style titulo = new Style()
                    .setFontSize(
                            Style.FontSize._2,
                            Style.FontSize._2
                    )
                    .setJustification(
                            EscPosConst.Justification.Center
                    );

            Style centro = new Style()
                    .setJustification(
                            EscPosConst.Justification.Center
                    );

            Style negrito = new Style()
                    .setBold(true);

            Style totalStyle = new Style()
                    .setFontSize(
                            Style.FontSize._2,
                            Style.FontSize._2
                    )
                    .setJustification(
                            EscPosConst.Justification.Right
                    );

            // ======================================
            // CABECALHO DO ESTABELECIMENTO
            // ======================================

            escpos.writeLF(
                    titulo,
                    estabelecimento.getNome()
            );

            escpos.writeLF(
                    centro,
                    "CNPJ: " + estabelecimento.getCnpj()
            );

            if (temTexto(estabelecimento.getEndereco())) {

                imprimirTextoComQuebra(
                        escpos,
                        estabelecimento.getEndereco(),
                        LARGURA_LINHA
                );
            }

            String cidadeUf =
                    formatarCidadeUf(estabelecimento);

            if (temTexto(cidadeUf)) {
                escpos.writeLF(centro, cidadeUf);
            }

            if (temTexto(estabelecimento.getCep())) {

                escpos.writeLF(
                        centro,
                        "CEP: " + estabelecimento.getCep()
                );
            }

            if (temTexto(estabelecimento.getTelefone())) {

                escpos.writeLF(
                        centro,
                        "Fone: " + estabelecimento.getTelefone()
                );
            }

            escpos.writeLF(
                    "----------------------------------------"
            );

            escpos.writeLF(
                    centro,
                    "COMPROVANTE DE ABASTECIMENTO"
            );

            escpos.writeLF(
                    "----------------------------------------"
            );

            // ======================================
            // DADOS DA VENDA
            // ======================================

            if (temTexto(venda.getCnpjCpf())) {

                escpos.writeLF(
                        "CPF/CNPJ: " + venda.getCnpjCpf()
                );
            }

            String dadosVeiculo = "";

            if (temTexto(venda.getPlaca())) {

                dadosVeiculo +=
                        "Placa: " + venda.getPlaca() + "   ";
            }

            if (venda.getKm() != null
                    && venda.getKm() > 0) {

                dadosVeiculo +=
                        "KM: " + venda.getKm();
            }

            if (!dadosVeiculo.isEmpty()) {
                escpos.writeLF(dadosVeiculo);
            }

            // ======================================
            // ITENS DA VENDA
            // ======================================

            escpos.writeLF(
                    "----------------------------------------"
            );

            escpos.writeLF(
                    negrito,
                    "QTD/UN PRODUTO          UNIT.    TOTAL"
            );

            escpos.writeLF(
                    "----------------------------------------"
            );

            for (ItemVenda item : venda.getItens()) {

                String unidade =
                        item.getProduto().getUnidade();

                String quantidade;

                if ("UN".equals(unidade)) {

                    quantidade = String.format(
                            Locale.US,
                            "%.0fUN",
                            item.getQuantidade()
                    );

                } else {

                    quantidade = String.format(
                            Locale.US,
                            "%.3fL",
                            item.getQuantidade()
                    );
                }

                String nomeProduto =
                        item.getProduto().getNome();

                String nomeResumido =
                        limitarTexto(nomeProduto, 14);

                String linhaItem = String.format(
                        Locale.US,
                        "%-7s %-14s %7.2f %8.2f",
                        quantidade,
                        nomeResumido,
                        item.getValorUnitario(),
                        item.getSubtotal()
                );

                // Produtos com nomes grandes, como
                // oleos e aditivos de marcas especificas,
                // sao exibidos por completo.

                if (nomeProduto.length() > 14
                        || linhaItem.length() > LARGURA_LINHA) {

                    imprimirTextoComQuebra(
                            escpos,
                            nomeProduto,
                            LARGURA_LINHA
                    );

                    String detalhes = String.format(
                            Locale.US,
                            "%s  Unit: %.2f  Total: %.2f",
                            quantidade,
                            item.getValorUnitario(),
                            item.getSubtotal()
                    );

                    imprimirTextoComQuebra(
                            escpos,
                            detalhes,
                            LARGURA_LINHA
                    );

                } else {

                    escpos.writeLF(linhaItem);
                }
            }

            escpos.writeLF(
                    "----------------------------------------"
            );

            // ======================================
            // RESUMO DA VENDA
            // ======================================

            escpos.writeLF(
                    "Qtd. total de itens: "
                            + venda.getItens().size()
            );

            // ======================================
            // TOTAL GERAL
            // ======================================

            escpos.writeLF(
                    totalStyle,
                    String.format(
                            Locale.US,
                            "TOTAL: R$ %.2f",
                            venda.getTotal()
                    )
            );

            // ======================================
            // TRIBUTOS E MD5
            // ======================================

            String informacoes = String.format(
                    Locale.US,
                    "ICMS: %.2f; "
                            + "Federais: %.2f; "
                            + "Tributos: %.2f; "
                            + "MD5: %s;"
                            + "Emissão: %s",
                    tributos.icms(),
                    tributos.federais(),
                    tributos.total(),
                    hashMd5,
                    dataEmissao
            );

            imprimirTextoComQuebra(
                    escpos,
                    informacoes,
                    LARGURA_LINHA
            );

            // ======================================
            // CODIGO INTERNO DA SIMULACAO
            // ======================================

            escpos.writeLF(
                    "Codigo: " + codigoSimulacao
            );

            // ======================================
            // RODAPE
            // ======================================

            escpos.feed(2);

            escpos.writeLF(
                    centro,
                    "Documento emitido por Auto Nota TAC"
            );

            escpos.writeLF(
                    centro,
                    "OBRIGADO E VOLTE SEMPRE!"
            );

            escpos.feed(5);
            escpos.cut(EscPos.CutMode.FULL);

        } catch (IOException e) {

            throw new Exception(
                    "Erro de comunicacao com a impressora: "
                            + e.getMessage(),
                    e
            );
        }
    }

    // ==========================================
    // METODOS AUXILIARES
    // ==========================================

    private static boolean temTexto(String texto) {

        return texto != null && !texto.isBlank();
    }

    private static String limitarTexto(
            String texto,
            int tamanho
    ) {

        if (texto == null) {
            return "";
        }

        if (texto.length() <= tamanho) {
            return texto;
        }

        return texto.substring(0, tamanho);
    }

    private static String formatarCidadeUf(
            Estabelecimento estabelecimento
    ) {

        String cidade = estabelecimento.getCidade();
        String uf = estabelecimento.getUf();

        boolean temCidade = temTexto(cidade);
        boolean temUf = temTexto(uf);

        if (temCidade && temUf) {
            return cidade + " - " + uf;
        }

        if (temCidade) {
            return cidade;
        }

        return temUf ? uf : "";
    }

    private static void imprimirTextoComQuebra(
            EscPos escpos,
            String texto,
            int largura
    ) throws IOException {

        if (texto == null || texto.isBlank()) {
            return;
        }

        if (largura <= 0) {
            throw new IllegalArgumentException(
                    "Largura de impressao invalida."
            );
        }

        StringBuilder linha = new StringBuilder();

        for (String palavra : texto.split("\\s+")) {

            if (palavra.length() > largura) {

                if (!linha.isEmpty()) {

                    escpos.writeLF(linha.toString());
                    linha.setLength(0);
                }

                for (int i = 0;
                     i < palavra.length();
                     i += largura) {

                    int fim = Math.min(
                            i + largura,
                            palavra.length()
                    );

                    escpos.writeLF(
                            palavra.substring(i, fim)
                    );
                }

                continue;
            }

            if (!linha.isEmpty()
                    && linha.length()
                    + palavra.length() + 1 > largura) {

                escpos.writeLF(linha.toString());
                linha.setLength(0);
            }

            if (!linha.isEmpty()) {
                linha.append(" ");
            }

            linha.append(palavra);
        }

        if (!linha.isEmpty()) {
            escpos.writeLF(linha.toString());
        }
    }
}
