package br.edu.fileconverter.domain;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/** Formatos de arquivo suportados, com extensão e MIME type. */
public enum FileFormat {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final String extension;
    private final String mimeType;

    FileFormat(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }

    public String extension() {
        return extension;
    }

    public String mimeType() {
        return mimeType;
    }

    /** Resolve o formato pela extensão, sem diferenciar maiúsculas e aceitando ponto inicial. */
    public static Optional<FileFormat> fromExtension(String extension) {
        if (extension == null) {
            return Optional.empty();
        }
        String normalized = extension.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        String value = normalized;
        return Arrays.stream(values())
                .filter(format -> format.extension.equals(value))
                .findFirst();
    }
}
