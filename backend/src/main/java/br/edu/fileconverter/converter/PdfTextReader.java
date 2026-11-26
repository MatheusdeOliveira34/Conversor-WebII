package br.edu.fileconverter.converter;

import br.edu.fileconverter.exception.ConversionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;

/**
 * Leitura de texto de PDF com PDFBox, compartilhada pelos conversores que partem de PDF.
 * Traduz as falhas de leitura em {@link ConversionException} (RN04, RN05).
 */
final class PdfTextReader {

    /** Caracteres de controle que o XML do Word/Excel não aceita (mantém só o tab). */
    private static final String INVALID_XML_CHARS = "[\\p{Cntrl}&&[^\\t]]";

    private PdfTextReader() {
    }

    /** Linhas de texto de cada página, na ordem. Lança {@code NO_CONTENT} se o PDF não tiver texto. */
    static List<List<String>> readPages(byte[] input) {
        try (PDDocument document = Loader.loadPDF(input)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            List<List<String>> pages = new ArrayList<>();
            boolean hasText = false;
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                List<String> lines = toLines(stripper.getText(document));
                hasText |= lines.stream().anyMatch(line -> !line.isBlank());
                pages.add(lines);
            }
            if (!hasText) {
                throw new ConversionException(ConversionException.NO_CONTENT,
                        "PDF sem texto. Arquivos escaneados (imagem) não podem ser convertidos.");
            }
            return pages;
        } catch (InvalidPasswordException e) {
            throw new ConversionException(ConversionException.PROTECTED_FILE,
                    "O PDF está protegido por senha.", e);
        } catch (IOException e) {
            throw new ConversionException(ConversionException.CORRUPTED_FILE,
                    "O arquivo está corrompido ou não pôde ser lido.", e);
        }
    }

    private static List<String> toLines(String pageText) {
        List<String> lines = new ArrayList<>();
        for (String line : pageText.split("\\R", -1)) {
            lines.add(line.replaceAll(INVALID_XML_CHARS, "").stripTrailing());
        }
        // remove linhas vazias do fim da página (o stripper sempre termina com quebra de linha)
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
        return lines;
    }
}
