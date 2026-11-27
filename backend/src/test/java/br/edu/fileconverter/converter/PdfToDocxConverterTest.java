package br.edu.fileconverter.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.fileconverter.exception.ConversionException;
import br.edu.fileconverter.support.TestDocuments;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;

class PdfToDocxConverterTest {

    private final PdfToDocxConverter converter = new PdfToDocxConverter();

    // CT01 (PDF → DOCX)
    @Test
    void convertsEachLineToParagraphWithPageBreakBetweenPages() throws IOException {
        byte[] pdf = TestDocuments.pdfPages("Relatório de vendas\nTotal do mês", "Segunda página");

        byte[] docx = converter.convert(pdf);

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<String> texts = document.getParagraphs().stream().map(XWPFParagraph::getText).toList();
            assertThat(texts).containsExactly("Relatório de vendas", "Total do mês", "Segunda página");

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            assertThat(paragraphs.get(0).isPageBreak()).isFalse();
            assertThat(paragraphs.get(2).isPageBreak()).isTrue();
        }
    }

    @Test
    void rejectsPasswordProtectedPdf() {
        assertThatThrownBy(() -> converter.convert(TestDocuments.protectedPdf()))
                .isInstanceOf(ConversionException.class)
                .extracting("code")
                .isEqualTo(ConversionException.PROTECTED_FILE);
    }

    @Test
    void rejectsPdfWithoutText() {
        assertThatThrownBy(() -> converter.convert(TestDocuments.blankPdf()))
                .isInstanceOf(ConversionException.class)
                .extracting("code")
                .isEqualTo(ConversionException.NO_CONTENT);
    }

    @Test
    void rejectsCorruptedPdf() {
        byte[] garbage = "%PDF-1.7 isso não é um pdf de verdade".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> converter.convert(garbage))
                .isInstanceOf(ConversionException.class)
                .extracting("code")
                .isEqualTo(ConversionException.CORRUPTED_FILE);
    }
}
