package br.edu.fileconverter.exception;

import br.edu.fileconverter.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduz toda exceção em {@link ErrorResponse}. O stack trace vai só para o log,
 * nunca para a resposta.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String INVALID_REQUEST = "INVALID_REQUEST";
    static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    static final String NOT_FOUND = "NOT_FOUND";
    static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMediaTypeNotSupportedException.class,
            MultipartException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, INVALID_REQUEST,
                "Requisição inválida. Envie o arquivo e um formato de destino (PDF, DOCX ou XLSX).", request);
    }

    /** Mais específico que {@link MultipartException}, então tem prioridade sobre o handler acima. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, FILE_TOO_LARGE, "Arquivo maior que 10 MB.", request);
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex, HttpServletRequest request) {
        HttpStatus status = InvalidFileException.EMPTY_FILE.equals(ex.getCode())
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        return build(status, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(UnsupportedConversionException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedConversion(
            UnsupportedConversionException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, UnsupportedConversionException.CODE, ex.getMessage(), request);
    }

    @ExceptionHandler(ConversionException.class)
    public ResponseEntity<ErrorResponse> handleConversion(ConversionException ex, HttpServletRequest request) {
        log.warn("Falha na conversão ({}): {}", ex.getCode(), ex.getMessage());
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, NOT_FOUND, "Rota não encontrada.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, METHOD_NOT_ALLOWED, "Método HTTP não permitido nesta rota.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado em {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_ERROR, "Erro inesperado. Tente novamente.", request);
    }

    private static ResponseEntity<ErrorResponse> build(
            HttpStatus status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(code, message, request.getRequestURI(), Instant.now()));
    }
}
