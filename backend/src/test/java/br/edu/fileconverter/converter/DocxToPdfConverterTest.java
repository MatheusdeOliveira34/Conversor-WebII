package br.edu.fileconverter.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.fileconverter.exception.ConversionException;
import br.edu.fileconverter.support.TestDocuments;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class DocxToPdfConverterTest {

    private final DocxToPdfConverter converter = new DocxToPdfConverter();

    // CT01 (DOCX → PDF)
    @Test
    void convertsParagraphsAndTablesToPdf() throws IOException {
        byte[] pdf = converter.convert(TestDocuments.docx("Relatório de vendas"));

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Relatório de vendas", "Produto", "Quantidade", "Caneta", "10");
        }
    }

    @Test
    void rejectsCorruptedFile() {
        byte[] garbage = "isso não é um docx".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> converter.convert(garbage))
                .isInstanceOf(ConversionException.class)
                .extracting("code")
                .isEqualTo(ConversionException.CORRUPTED_FILE);
    }

    @Test
    void rejectsDocumentWithoutContent() {
        assertThatThrownBy(() -> converter.convert(TestDocuments.emptyDocx()))
                .isInstanceOf(ConversionException.class)
                .extracting("code")
                .isEqualTo(ConversionException.NO_CONTENT);
    }
}
