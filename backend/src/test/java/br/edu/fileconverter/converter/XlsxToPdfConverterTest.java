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

class XlsxToPdfConverterTest {

    private final XlsxToPdfConverter converter = new XlsxToPdfConverter();

    // CT01 (XLSX → PDF)
    @Test
    void convertsEachSheetToTitledTableWithFormulaValues() throws IOException {
        byte[] pdf = converter.convert(TestDocuments.salesWorkbook());

        try (PDDocument document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Vendas", "Produto", "Caneta", "10", "2,5", "25", "Resumo",
                    "Fechamento do mês");
            assertThat(text).doesNotContain("B2*C2", "Vazia"); // RN06 e RN07
            assertThat(document.getNumberOfPages()).isEqualTo(2); // uma página por aba com dados
        }
    }

    @Test
    void rejectsWorkbookWithoutData() {
        assertThatThrownBy(() -> converter.convert(TestDocuments.emptyWorkbook()))
                .isInstanceOf(ConversionException.class)
                .extracting("code")
                .isEqualTo(ConversionException.NO_CONTENT);
    }

    @Test
    void rejectsCorruptedFile() {
        byte[] garbage = "isso não é uma planilha".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> converter.convert(garbage))
                .isInstanceOf(ConversionException.class)
                .extracting("code")
                .isEqualTo(ConversionException.CORRUPTED_FILE);
    }
}
