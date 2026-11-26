package br.edu.fileconverter.dto;

import br.edu.fileconverter.domain.FileFormat;

/** Arquivo convertido em base64, com metadados para o download no front-end. */
public record ConversionResponse(
        String fileName,
        String contentType,
        FileFormat sourceFormat,
        FileFormat targetFormat,
        long sizeBytes,
        long durationMs,
        String contentBase64) {
}
