package com.tomazbr9.util;

import java.nio.file.Path;
import java.util.Locale;

public final class AppPaths {

    private AppPaths() {
    }

    public static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    public static Path pastaDados() {

        if (isWindows()) {
            String programData = System.getenv("ProgramData");

            if (programData == null || programData.isBlank()) {
                throw new IllegalStateException(
                        "Variavel ProgramData nao encontrada."
                );
            }

            return Path.of(programData, "AutoNota")
                    .toAbsolutePath()
                    .normalize();
        }

        // Somente para desenvolvimento no Linux.
        String xdgDataHome = System.getenv("XDG_DATA_HOME");

        Path base;

        if (xdgDataHome != null
                && !xdgDataHome.isBlank()
                && Path.of(xdgDataHome).isAbsolute()) {

            base = Path.of(xdgDataHome);

        } else {

            base = Path.of(
                    System.getProperty("user.home"),
                    ".local",
                    "share"
            );
        }

        return base.resolve("auto-nota")
                .toAbsolutePath()
                .normalize();
    }

    public static Path banco() {
        return pastaDados().resolve("auto_nota.db");
    }

    public static Path licenca() {
        return pastaDados().resolve("license.lic");
    }
}
