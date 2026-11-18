package com.tomazbr9.licensing;

public class InstallationIdException extends Exception {

    public InstallationIdException(String message) {
        super(message);
    }

    public InstallationIdException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
