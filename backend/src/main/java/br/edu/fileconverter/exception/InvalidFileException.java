package br.edu.fileconverter.exception;

/**
 * Arquivo rejeitado antes da conversão. O {@code code} é o mesmo que vai no
 * {@code ErrorResponse} (ex.: {@code EMPTY_FILE}, {@code UNSUPPORTED_FILE_TYPE}).
 */
public class InvalidFileException extends RuntimeException {

    public static final String EMPTY_FILE = "EMPTY_FILE";
    public static final String UNSUPPORTED_FILE_TYPE = "UNSUPPORTED_FILE_TYPE";

    private final String code;

    public InvalidFileException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
