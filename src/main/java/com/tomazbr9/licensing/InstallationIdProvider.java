package com.tomazbr9.licensing;

import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.HexFormat;
import java.util.Locale;

import java.util.concurrent.TimeUnit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InstallationIdProvider {

    private static final Pattern REGISTRY_MACHINE_GUID =
            Pattern.compile(
                    "^\\s*MachineGuid\\s+REG_SZ\\s+([^\\s]+)\\s*$",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
            );

    private InstallationIdProvider() {
    }

    public static String obter()
            throws InstallationIdException {

        String sistema = System.getProperty(
                "os.name", ""
        ).toLowerCase(Locale.ROOT);

        try {
            if (sistema.contains("win")) {

                return derivar(
                        "WINDOWS",
                        lerIdentificadorWindows()
                );
            }

            if (sistema.contains("linux")) {

                return derivar(
                        "LINUX",
                        lerIdentificadorLinux()
                );
            }

            throw new InstallationIdException(
                    "Sistema operacional nao suportado: "
                            + sistema
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new InstallationIdException(
                    "Consulta ao sistema interrompida.", e
            );

        } catch (IOException e) {

            throw new InstallationIdException(
                    "Nao foi possivel identificar "
                            + "este computador.", e
            );
        }
    }

    // Metodo acessivel aos testes do mesmo pacote.
    // Permite testar sem consultar identificadores reais.

    static String derivar(
            String origem,
            String identificador
    ) {

        if (!"WINDOWS".equals(origem)
                && !"LINUX".equals(origem)) {

            throw new IllegalArgumentException(
                    "Origem do identificador invalida."
            );
        }

        if (identificador == null
                || identificador.isBlank()) {

            throw new IllegalArgumentException(
                    "Identificador ausente."
            );
        }

        String normalizado = identificador
                .strip()
                .toLowerCase(Locale.ROOT);

        // Separa os identificadores do Auto-Nota
        // de outros usos dos mesmos dados.

        String entrada = "AUTO_NOTA_MACHINE_V1\n"
                + origem + "\n"
                + normalizado;

        byte[] hash;

        try {
            hash = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            entrada.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 nao disponivel.", e
            );
        }

        // Utilizamos 128 bits do hash
        // para formar um codigo legivel.

        String hexadecimal = HexFormat.of()
                .withUpperCase()
                .formatHex(hash, 0, 16);

        return "AN1-"
                + hexadecimal.substring(0, 8) + "-"
                + hexadecimal.substring(8, 16) + "-"
                + hexadecimal.substring(16, 24) + "-"
                + hexadecimal.substring(24, 32);
    }

    // ========================================
    // LINUX
    // ========================================

    private static String lerIdentificadorLinux()
            throws IOException {

        Path[] caminhos = {
                Path.of("/etc/machine-id"),
                Path.of("/var/lib/dbus/machine-id")
        };

        IOException ultimaFalha = null;

        for (Path arquivo : caminhos) {

            if (!Files.isRegularFile(arquivo)) {
                continue;
            }

            try {
                String identificador = Files.readString(
                        arquivo,
                        StandardCharsets.UTF_8
                ).strip();

                if (identificador.matches(
                        "(?i)[0-9a-f]{32}"
                ) && !identificador.matches("0{32}")) {

                    return identificador;
                }

            } catch (IOException e) {
                ultimaFalha = e;
            }
        }

        throw new IOException(
                "Identificador persistente "
                        + "do Linux indisponivel.",
                ultimaFalha
        );
    }

    // ========================================
    // WINDOWS
    // ========================================

    private static String lerIdentificadorWindows()
            throws IOException, InterruptedException {

        String systemRoot =
                System.getenv("SystemRoot");

        String executavel =
                systemRoot == null || systemRoot.isBlank()
                        ? "reg.exe"
                        : Path.of(
                        systemRoot,
                        "System32",
                        "reg.exe"
                ).toString();

        Process processo = new ProcessBuilder(
                executavel,
                "query",
                "HKLM\\SOFTWARE\\Microsoft\\Cryptography",
                "/v",
                "MachineGuid"
        )
                .redirectErrorStream(true)
                .start();

        try {
            boolean terminou = processo.waitFor(
                    5,
                    TimeUnit.SECONDS
            );

            if (!terminou) {

                processo.destroyForcibly();

                throw new IOException(
                        "Consulta ao Registro excedeu o prazo."
                );
            }

            String saida;

            try (InputStream input =
                         processo.getInputStream()) {

                saida = new String(
                        input.readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }

            if (processo.exitValue() != 0) {
                throw new IOException(
                        "Consulta ao Registro do Windows falhou."
                );
            }

            Matcher matcher =
                    REGISTRY_MACHINE_GUID.matcher(saida);

            if (!matcher.find()) {
                throw new IOException(
                        "MachineGuid nao encontrado."
                );
            }

            String identificador =
                    matcher.group(1).strip();

            String apenasHex =
                    identificador.replace("-", "");

            if (!apenasHex.matches(
                    "(?i)[0-9a-f]{32}"
            ) || apenasHex.matches("0{32}")) {

                throw new IOException(
                        "MachineGuid invalido."
                );
            }

            return identificador;

        } finally {

            if (processo.isAlive()) {
                processo.destroyForcibly();
            }
        }
    }
}
