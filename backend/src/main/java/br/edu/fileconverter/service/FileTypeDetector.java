package br.edu.fileconverter.service;

import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.domain.SourceDocument;
import br.edu.fileconverter.exception.InvalidFileException;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * Identifica o formato real pelo conteúdo (magic bytes), RF03 e RN02.
 *
 * <ul>
 *   <li>PDF: começa com {@code %PDF-}.</li>
 *   <li>DOCX e XLSX: são ZIP (começam com {@code PK\3\4}); o DOCX tem a entrada
 *       {@code word/document.xml} e o XLSX tem {@code xl/workbook.xml}.</li>
 * </ul>
 *
 * Os nomes das entradas de um ZIP ficam gravados sem compressão, então a busca é feita nos
 * bytes crus, sem descompactar nada (não há risco de zip bomb aqui).
 */
@Component
public class FileTypeDetector {

    private static final byte[] PDF_SIGNATURE = ascii("%PDF-");
    private static final byte[] ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] DOCX_ENTRY = ascii("word/document.xml");
    private static final byte[] XLSX_ENTRY = ascii("xl/workbook.xml");

    /** Formato detectado pelo conteúdo; lança {@link InvalidFileException} se vazio ou desconhecido. */
    public FileFormat detect(byte[] content) {
        if (content == null || content.length == 0) {
            throw new InvalidFileException(InvalidFileException.EMPTY_FILE, "O arquivo está vazio.");
        }
        if (startsWith(content, PDF_SIGNATURE)) {
            return FileFormat.PDF;
        }
        if (startsWith(content, ZIP_SIGNATURE)) {
            if (contains(content, DOCX_ENTRY)) {
                return FileFormat.DOCX;
            }
            if (contains(content, XLSX_ENTRY)) {
                return FileFormat.XLSX;
            }
        }
        throw unsupported("Formato não suportado. Use PDF, DOCX ou XLSX.");
    }

    /** Garante que o conteúdo é do mesmo formato que a extensão indica (RN02). */
    public void validate(SourceDocument document) {
        FileFormat detected = detect(document.content());
        if (detected != document.format()) {
            throw unsupported("A extensão do arquivo não confere com o conteúdo.");
        }
    }

    private static InvalidFileException unsupported(String message) {
        return new InvalidFileException(InvalidFileException.UNSUPPORTED_FILE_TYPE, message);
    }

    private static boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean contains(byte[] content, byte[] pattern) {
        outer:
        for (int i = 0; i <= content.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (content[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
