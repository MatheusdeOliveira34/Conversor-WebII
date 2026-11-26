package br.edu.fileconverter.domain;

/** Arquivo recebido, já com nome sanitizado e formato resolvido. */
public record SourceDocument(String baseName, FileFormat format, byte[] content) {
}
