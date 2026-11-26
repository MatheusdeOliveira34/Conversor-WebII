package br.edu.fileconverter.dto;

import java.time.Instant;

/** Formato único de erro devolvido pela API. */
public record ErrorResponse(
        String code,
        String message,
        String path,
        Instant timestamp) {
}
