package br.edu.fileconverter.exception;

import br.edu.fileconverter.domain.FileFormat;

/** Origem igual ao destino ou par origem → destino sem conversor registrado. */
public class UnsupportedConversionException extends RuntimeException {

    public static final String CODE = "UNSUPPORTED_CONVERSION";

    private final FileFormat source;
    private final FileFormat target;

    public UnsupportedConversionException(FileFormat source, FileFormat target) {
        super("Conversão não suportada: " + source + " → " + target);
        this.source = source;
        this.target = target;
    }

    public FileFormat getSource() {
        return source;
    }

    public FileFormat getTarget() {
        return target;
    }
}
