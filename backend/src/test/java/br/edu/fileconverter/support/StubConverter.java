package br.edu.fileconverter.support;

import br.edu.fileconverter.converter.DocumentConverter;
import br.edu.fileconverter.domain.FileFormat;
import java.util.List;

/** Conversor falso para testar registry e endpoints sem depender dos conversores reais. */
public record StubConverter(FileFormat source, FileFormat target) implements DocumentConverter {

    @Override
    public byte[] convert(byte[] input) {
        return input;
    }

    /** Os seis pares da matriz de conversões. */
    public static List<DocumentConverter> allSixPairs() {
        return List.of(
                new StubConverter(FileFormat.DOCX, FileFormat.PDF),
                new StubConverter(FileFormat.DOCX, FileFormat.XLSX),
                new StubConverter(FileFormat.PDF, FileFormat.DOCX),
                new StubConverter(FileFormat.PDF, FileFormat.XLSX),
                new StubConverter(FileFormat.XLSX, FileFormat.PDF),
                new StubConverter(FileFormat.XLSX, FileFormat.DOCX));
    }
}
