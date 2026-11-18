package com.tomazbr9.licensing;

import java.io.IOException;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.security.GeneralSecurityException;

import java.util.Locale;
import java.util.Objects;

public final class LicenseService {

    @FunctionalInterface
    public interface InstallationIdSource {
        String obter() throws InstallationIdException;
    }

    private final LicenseValidator validator;
    private final InstallationIdSource idSource;
    private final Path arquivoLicenca;

    public LicenseService(
            LicenseValidator validator,
            InstallationIdSource idSource,
            Path arquivoLicenca
    ) {
        this.validator = Objects.requireNonNull(
                validator,
                "Validador obrigatorio."
        );

        this.idSource = Objects.requireNonNull(
                idSource,
                "Identificador obrigatorio."
        );

        this.arquivoLicenca = Objects.requireNonNull(
                arquivoLicenca,
                "Caminho obrigatorio."
        ).toAbsolutePath().normalize();
    }

    public static LicenseService criarPadrao()
            throws IOException, GeneralSecurityException {

        return new LicenseService(
                LicenseValidator.criarPadrao(),
                InstallationIdProvider::obter,
                caminhoPadrao()
        );
    }

    public static Path caminhoPadrao() {

        String sistema = System.getProperty(
                "os.name", ""
        ).toLowerCase(Locale.ROOT);

        Path home = Path.of(
                System.getProperty("user.home")
        );

        Path pasta;

        if (sistema.contains("win")) {

            String localAppData =
                    System.getenv("LOCALAPPDATA");

            pasta = localAppData != null
                    && !localAppData.isBlank()
                    ? Path.of(localAppData)
                    : home.resolve("AppData")
                    .resolve("Local");

            pasta = pasta.resolve("AutoNota");

        } else {

            String xdgDataHome =
                    System.getenv("XDG_DATA_HOME");

            if (xdgDataHome != null
                    && !xdgDataHome.isBlank()
                    && Path.of(xdgDataHome).isAbsolute()) {

                pasta = Path.of(xdgDataHome);

            } else {

                pasta = home.resolve(".local")
                        .resolve("share");
            }

            pasta = pasta.resolve("auto-nota");
        }

        return pasta.resolve("license.lic")
                .toAbsolutePath()
                .normalize();
    }

    public Path obterCaminhoLicenca() {
        return arquivoLicenca;
    }

    // =====================================
    // VERIFICAR LICENCA INSTALADA
    // =====================================

    public LicenseValidationResult verificarInstalada() {

        if (!Files.isRegularFile(arquivoLicenca)) {

            return LicenseValidationResult.rejeitada(
                    "Nenhuma licenca instalada."
            );
        }

        return validarArquivo(arquivoLicenca);
    }

    // =====================================
    // VALIDAR ASSINATURA E COMPUTADOR
    // =====================================

    public LicenseValidationResult validarArquivo(
            Path arquivo
    ) {

        LicenseValidationResult resultado =
                validator.validar(arquivo);

        if (!resultado.valid()) {
            return resultado;
        }

        try {

            String idAtual = idSource.obter();

            if (idAtual == null || idAtual.isBlank()) {

                return LicenseValidationResult.rejeitada(
                        "Identificacao do computador indisponivel."
                );
            }

            String idLicenciado =
                    resultado.license().installationId();

            if (!idAtual.equals(idLicenciado)) {

                return LicenseValidationResult.rejeitada(
                        "Licenca emitida para outro computador."
                );
            }

            return new LicenseValidationResult(
                    true,
                    "Licenca valida para este computador.",
                    resultado.license()
            );

        } catch (InstallationIdException e) {

            return LicenseValidationResult.rejeitada(
                    "Falha ao identificar o computador: "
                            + e.getMessage()
            );
        }
    }

    // =====================================
    // IMPORTAR E ATIVAR UMA LICENCA
    // =====================================

    public LicenseValidationResult ativar(
            Path origem
    ) throws IOException {

        if (origem == null) {

            return LicenseValidationResult.rejeitada(
                    "Selecione um arquivo de licenca."
            );
        }

        // Antes de copiar, verifica se a
        // licenca realmente autoriza a maquina.

        LicenseValidationResult resultado =
                validarArquivo(origem);

        if (!resultado.valid()) {
            return resultado;
        }

        Path pasta = arquivoLicenca.getParent();

        Files.createDirectories(pasta);

        Path temporario = Files.createTempFile(
                pasta,
                "auto-nota-license-",
                ".tmp"
        );

        try {

            // Copia a licenca para um arquivo
            // temporario dentro da pasta final.

            Files.copy(
                    origem,
                    temporario,
                    StandardCopyOption.REPLACE_EXISTING
            );

            // Confere novamente os bytes copiados.
            // Uma licenca invalida nao substituira
            // a licenca que ja estiver instalada.

            LicenseValidationResult copiaValidada =
                    validarArquivo(temporario);

            if (!copiaValidada.valid()) {
                return copiaValidada;
            }

            // Substituicao atomica, quando
            // suportada pelo sistema de arquivos.

            try {

                Files.move(
                        temporario,
                        arquivoLicenca,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );

            } catch (AtomicMoveNotSupportedException e) {

                Files.move(
                        temporario,
                        arquivoLicenca,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            // Confirma o resultado apos instalar.
            return verificarInstalada();

        } finally {

            Files.deleteIfExists(temporario);
        }
    }
}
