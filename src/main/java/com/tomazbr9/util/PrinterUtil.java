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

        // 1. Busca os dados do estabelecimento no banco
        EstabelecimentoDAO estabelecimentoDAO = new EstabelecimentoDAO();
        Estabelecimento estabelecimento;

        try {
            estabelecimento = estabelecimentoDAO.buscar()
                    .orElseThrow(() -> new IllegalStateException(
                            "Configure os dados do estabelecimento antes de imprimir."
                    ));
        } catch (SQLException e) {
            throw new Exception("Erro ao consultar estabelecimento: " + e.getMessage(), e);
        }

        // 2. Dados auxiliares
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String dataEmissao = venda.getDataHora().format(formatter);

        BigDecimal valorVenda = BigDecimal.valueOf(venda.getTotal());
        TributacaoSimulada.Resultado tributos = TributacaoSimulada.calcular(valorVenda);

        String codigoInterno = NotaFiscalUtil.gerarCodigo();

        String conteudoHash = codigoInterno + "|"
                + venda.getId() + "|"
                + dataEmissao + "|"
                + estabelecimento.getCnpj() + "|"
                + valorVenda.toPlainString() + "|"
                + tributos.total().toPlainString();

        String md5 = NotaFiscalUtil.gerarMd5(conteudoHash);

        // 3. Tenta buscar a impressora padrão do sistema
        PrintService printService = PrinterOutputStream.getDefaultPrintService();

        if (printService == null) {
            PrintService[] servicosDisponiveis = PrintServiceLookup.lookupPrintServices(null, null);

            if (servicosDisponiveis.length > 0) {
                printService = servicosDisponiveis[0];
            } else {
                throw new Exception("Nenhuma impressora encontrada no sistema.");
            }
        }

        // 4. Conecta na impressora
        try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
             EscPos escpos = new EscPos(printerOutputStream)) {

            // Estilos de Fonte
            Style titulo = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setJustification(EscPosConst.Justification.Center);

            Style centro = new Style()
                    .setJustification(EscPosConst.Justification.Center);

            Style negrito = new Style().setBold(true);

            Style totalStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setJustification(EscPosConst.Justification.Right);

            // === CABEÇALHO ===
            escpos.writeLF(titulo, valorOuVazio(estabelecimento.getNome()));
            escpos.writeLF(centro, "CNPJ: " + valorOuVazio(estabelecimento.getCnpj()));

            if (temTexto(estabelecimento.getEndereco())) {
                escpos.writeLF(centro, estabelecimento.getEndereco());
            }

            String cidadeUf = formatarCidadeUf(estabelecimento);
            if (temTexto(cidadeUf)) {
                escpos.writeLF(centro, cidadeUf);
            }

            if (temTexto(estabelecimento.getCep())) {
                escpos.writeLF(centro, "CEP: " + estabelecimento.getCep());
            }

            if (temTexto(estabelecimento.getTelefone())) {
                escpos.writeLF(centro, "Fone: " + estabelecimento.getTelefone());
            }

            escpos.writeLF("========================================");
            escpos.writeLF(centro, "COMPROVANTE DE ABASTECIMENTO");
            escpos.writeLF("========================================");

            // === DADOS DA VENDA ===
            escpos.writeLF("Data: " + dataEmissao + "   Cupom: " + venda.getId());

            if (temTexto(venda.getCnpjCpf())) {
                escpos.writeLF("CPF/CNPJ: " + venda.getCnpjCpf());
            }

            String dadosVeiculo = "";
            if (temTexto(venda.getPlaca())) {
                dadosVeiculo += "Placa: " + venda.getPlaca() + "   ";
            }
            if (venda.getKm() != null && venda.getKm() > 0) {
                dadosVeiculo += "KM: " + venda.getKm();
            }
            if (!dadosVeiculo.isEmpty()) {
                escpos.writeLF(dadosVeiculo);
            }

            // === CABEÇALHO DOS ITENS ===
            escpos.writeLF("----------------------------------------");
            escpos.writeLF(negrito, "QTD    PRODUTO         VLR UNIT  TOTAL");
            escpos.writeLF("----------------------------------------");

            // === ITENS ===
            for (ItemVenda item : venda.getItens()) {
                String nomeProduto = ajustarTamanho(item.getProduto().getNome(), 14);

                String linhaItem = String.format(
                        Locale.US,
                        "%-5s  %-14s R$%6.2f R$%6.2f",
                        String.format(Locale.US, "%.1fL", item.getLitros()),
                        nomeProduto,
                        item.getValorUnitario(),
                        item.getSubtotal()
                );

                escpos.writeLF(linhaItem);
            }

            escpos.writeLF("----------------------------------------");

            // === RESUMO ===
            escpos.writeLF(String.format(Locale.US, "Qtd. total de itens: %d", venda.getItens().size()));
            escpos.writeLF(totalStyle, String.format(Locale.US, "TOTAL: R$ %.2f", venda.getTotal()));

            // === INFORMAÇÕES TRIBUTÁRIAS / TÉCNICAS ===
            String infoTributos = String.format(
                    Locale.US,
                    "ICMS: %.2f; Federais: %.2f; Tributos: %.2f; MD5: %s",
                    tributos.icms(),
                    tributos.federais(),
                    tributos.total(),
                    md5
            );

            imprimirTextoComQuebra(escpos, infoTributos, LARGURA_LINHA);

            escpos.writeLF("Chave de acesso: " + codigoInterno);

            // === RODAPÉ ===
            escpos.feed(1);
            escpos.writeLF(centro, "OBRIGADO E VOLTE SEMPRE!");
            escpos.feed(5);
            escpos.cut(EscPos.CutMode.FULL);

        } catch (IOException e) {
            throw new Exception("Erro de comunicação com a impressora: " + e.getMessage(), e);
        }
    }

    private static boolean temTexto(String texto) {
        return texto != null && !texto.isBlank();
    }

    private static String valorOuVazio(String texto) {
        return texto == null ? "" : texto;
    }

    private static String formatarCidadeUf(Estabelecimento estabelecimento) {
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
        if (temUf) {
            return uf;
        }
        return "";
    }

    private static String ajustarTamanho(String texto, int tamanho) {
        if (texto == null) {
            return "";
        }
        if (texto.length() <= tamanho) {
            return texto;
        }
        return texto.substring(0, tamanho);
    }

    private static void imprimirTextoComQuebra(EscPos escpos, String texto, int largura) throws IOException {
        if (texto == null || texto.isBlank()) {
            return;
        }

        StringBuilder linha = new StringBuilder();

        for (String palavra : texto.split("\\s+")) {

            if (palavra.length() > largura) {
                if (!linha.isEmpty()) {
                    escpos.writeLF(linha.toString());
                    linha.setLength(0);
                }

                for (int i = 0; i < palavra.length(); i += largura) {
                    int fim = Math.min(i + largura, palavra.length());
                    escpos.writeLF(palavra.substring(i, fim));
                }
                continue;
            }

            if (!linha.isEmpty() && linha.length() + 1 + palavra.length() > largura) {
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