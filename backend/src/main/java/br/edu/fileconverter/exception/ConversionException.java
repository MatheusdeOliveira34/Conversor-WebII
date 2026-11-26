package br.edu.fileconverter.exception;

/**
 * Falha ao ler ou converter um arquivo válido. O {@code code} é o mesmo que vai no
 * {@code ErrorResponse} (ex.: {@code CORRUPTED_FILE}, {@code PROTECTED_FILE}, {@code NO_CONTENT}).
 */
public class ConversionException extends RuntimeException {

    public static final String CORRUPTED_FILE = "CORRUPTED_FILE";
    public static final String PROTECTED_FILE = "PROTECTED_FILE";
    public static final String NO_CONTENT = "NO_CONTENT";

    private final String code;

    public ConversionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ConversionException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
