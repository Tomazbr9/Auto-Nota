package com.tomazbr9.licensing;

import com.tomazbr9.util.AppPaths;

import java.io.IOException;

import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import java.security.GeneralSecurityException;

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

        LicenseService servico = new LicenseService(
                LicenseValidator.criarPadrao(),
                InstallationIdProvider::obter,
                caminhoPadrao()
        );

        servico.migrarLicencaAnteriorSeNecessario();

        return servico;
    }

    public static Path caminhoPadrao() {
        return AppPaths.licenca();
    }

    public Path obterCaminhoLicenca() {
        return arquivoLicenca;
    }

    // =====================================
    // MIGRACAO DA LICENCA ANTERIOR
    // =====================================

    private void migrarLicencaAnteriorSeNecessario() {

        if (!AppPaths.isWindows()
                || Files.exists(arquivoLicenca)) {
            return;
        }

        String localAppData =
                System.getenv("LOCALAPPDATA");

        if (localAppData == null
                || localAppData.isBlank()) {
            return;
        }

        Path licencaAnterior = Path.of(
                localAppData,
                "AutoNota",
                "license.lic"
        );

        if (!Files.isRegularFile(licencaAnterior)) {
            return;
        }

        try {
            LicenseValidationResult resultado =
                    ativar(licencaAnterior);

            if (resultado.valid()) {

                System.out.println(
                        "Licenca anterior migrada para: "
                                + arquivoLicenca
                );

            } else {

                System.err.println(
                        "Licenca anterior nao foi migrada: "
                                + resultado.message()
                );
            }

        } catch (IOException | SecurityException e) {

            // Nao impede a abertura da tela
            // de ativacao. O usuario ainda pode
            // importar sua licenca manualmente.

            System.err.println(
                    "Nao foi possivel migrar a licenca: "
                            + e.getMessage()
            );
        }
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
    // VALIDAR ARQUIVO E COMPUTADOR
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
    // IMPORTAR E ATIVAR LICENCA
    // =====================================

    public LicenseValidationResult ativar(
            Path origem
    ) throws IOException {

        if (origem == null) {

            return LicenseValidationResult.rejeitada(
                    "Selecione um arquivo de licenca."
            );
        }

        // Confere antes de instalar.

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
            Files.copy(
                    origem,
                    temporario,
                    StandardCopyOption.REPLACE_EXISTING
            );

            LicenseValidationResult copiaValidada =
                    validarArquivo(temporario);

            if (!copiaValidada.valid()) {
                return copiaValidada;
            }

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

            return verificarInstalada();

        } finally {
            Files.deleteIfExists(temporario);
        }
    }
}
