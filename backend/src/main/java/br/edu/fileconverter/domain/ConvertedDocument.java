package br.edu.fileconverter.domain;

import java.time.Duration;

/** Resultado de uma conversão, com o tempo que ela levou. */
public record ConvertedDocument(String baseName, FileFormat format, byte[] content, Duration duration) {
}
