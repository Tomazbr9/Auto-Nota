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
import java.io.IOException;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.print.PrintServiceLookup;

public class PrinterUtil {

    public static void imprimirCupom(Venda venda) throws Exception {
        // 1. Tenta buscar a impressora padrão do sistema
        PrintService printService = PrinterOutputStream.getDefaultPrintService();

        // Se não encontrar, busca qualquer uma disponível usando a API nativa do Java
        if (printService == null) {
            // CORREÇÃO: Usando a classe padrão do Java para listar impressoras
            PrintService[] servicosDisponiveis = PrintServiceLookup.lookupPrintServices(null, null);

            if (servicosDisponiveis.length > 0) {
                printService = servicosDisponiveis[0]; // Pega a primeira que encontrar
            } else {
                throw new Exception("Nenhuma impressora encontrada no sistema.");
            }
        }

        Estabelecimento estabelecimento;

        try {
            estabelecimento = new EstabelecimentoDAO()
                    .buscar()
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Configure o estabelecimento."
                            )
                    );
        } catch (SQLException e) {
            throw new Exception(
                    "Erro ao consultar estabelecimento.", e
            );
        }

        // 2. Conecta na impressora (Modo Produção/Real)
        try (PrinterOutputStream printerOutputStream = new PrinterOutputStream(printService);
             EscPos escpos = new EscPos(printerOutputStream)) {

            // Estilos de Fonte
            Style titulo = new Style()
                    .setFontSize(Style.FontSize._1, Style.FontSize._1)
                    .setJustification(EscPosConst.Justification.Center);
            Style centro = new Style().setJustification(EscPosConst.Justification.Center);
            Style negrito = new Style().setBold(true);

            // === CABEÇALHO ===
            escpos.writeLF(titulo, estabelecimento.getNome());
            escpos.writeLF(centro, "CNPJ: " + CnpjFormatter.formatar(estabelecimento.getCnpj()));
            escpos.writeLF(centro, estabelecimento.getEndereco() + ", ");
            escpos.writeLF(centro, estabelecimento.getCidade() + ", Cep: " + estabelecimento.getCep());
            escpos.writeLF(centro, "----------------------------------------");
            escpos.writeLF(centro, "COMPROVANTE DE ABASTECIMENTO");
            escpos.writeLF(centro, "----------------------------------------");
            escpos.writeLF(centro, "Documento Auxiliar de Nota fiscal de Consumidor Eletronica");

            // === DADOS DA VENDA ===
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            escpos.writeLF("Data: " + venda.getDataHora().format(formatter) + "   Cupom: " + venda.getId());

            if (venda.getCnpjCpf() != null && !venda.getCnpjCpf().isEmpty()) {
                escpos.writeLF("CPF/CNPJ: " + venda.getCnpjCpf());
            }

            String dadosVeiculo = "";
            if (venda.getPlaca() != null && !venda.getPlaca().isEmpty()) {
                dadosVeiculo += "Placa: " + venda.getPlaca() + "   ";
            }
            if (venda.getKm() != null && venda.getKm() > 0) {
                dadosVeiculo += "KM: " + venda.getKm();
            }
            if (!dadosVeiculo.isEmpty()) {
                escpos.writeLF(dadosVeiculo);
            }

            // === ITENS ===
            escpos.writeLF("----------------------------------------");
            escpos.writeLF(negrito, "QTD    PRODUTO         VLR UNIT  TOTAL");

            for (ItemVenda item : venda.getItens()) {

                String unidade = item.getProduto().getUnidade();
                String quantidade;

                if(unidade.equals("UN")){
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

                String linhaItem = String.format("%-7s  %s  R$ %.2f  R$ %.2f",
                        quantidade,
                        item.getProduto().getNome(),
                        item.getValorUnitario(),
                        item.getSubtotal());

                escpos.writeLF(linhaItem.replace(",", "."));
            }
            escpos.writeLF("----------------------------------------");
            escpos.writeLF(
                    "Qtd. total de itens: "
                            + venda.getItens().size()
            );

            // === TOTAL ===
            Style totalStyle = new Style()
                    .setFontSize(Style.FontSize._2, Style.FontSize._2)
                    .setJustification(EscPosConst.Justification.Right);

            // Nota: Certifique-se que o método venda.getTotal() exista na sua classe Venda
            escpos.writeLF(totalStyle, String.format("TOTAL: R$ %.2f", venda.getTotal()).replace(",", "."));

            escpos.feed(1);
            escpos.writeLF(centro, "Consulte pela chave de acesso em");
            escpos.writeLF(centro, "https://www.nfce.fazenda.sp.gov.br/consulta");

            // Certifique-se que a classe NotaFiscalUtil existe
            escpos.writeLF(centro, NotaFiscalUtil.gerarCodigo());

            // === RODAPÉ ===
            escpos.feed(2);
            escpos.writeLF(centro, "Documento emitido por Auto Nota TAC / www.autonota.com.br");
            escpos.writeLF(centro, "OBRIGADO E VOLTE SEMPRE!");

            escpos.feed(5);
            escpos.cut(EscPos.CutMode.FULL);

        } catch (IOException e) {
            throw new Exception("Erro de comunicação com a impressora: " + e.getMessage());
        }
    }
}