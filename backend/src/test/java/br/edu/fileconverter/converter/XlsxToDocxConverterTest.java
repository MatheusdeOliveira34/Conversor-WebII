package br.edu.fileconverter.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.fileconverter.exception.ConversionException;
import br.edu.fileconverter.support.TestDocuments;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

class XlsxToDocxConverterTest {

    private final XlsxToDocxConverter converter = new XlsxToDocxConverter();

    // CT01 (XLSX → DOCX)
    @Test
    void convertsEachSheetToTitleAndTable() throws IOException {
        byte[] docx = converter.convert(TestDocuments.salesWorkbook());

        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(docx))) {
            List<String> titles = document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .filter(text -> !text.isBlank())
                    .toList();
            assertThat(titles).containsExactly("Vendas", "Resumo"); // aba "Vazia" ignorada (RN07)

            List<XWPFTable> tables = document.getTables();
            assertThat(tables).hasSize(2);
            XWPFTable sales = tables.get(0);
            assertThat(sales.getNumberOfRows()).isEqualTo(2);
            assertThat(sales.getRow(0).getCell(0).getText()).isEqualTo("Produto");
            assertThat(sales.getRow(1).getCell(2).getText()).isEqualTo("2,5");
            assertThat(sales.getRow(1).getCell(3).getText()).isEqualTo("25"); // valor, não a fórmula (RN06)
            assertThat(tables.get(1).getRow(0).getCell(0).getText()).isEqualTo("Fechamento do mês");
        }
    }

    @Test
    void rejectsWorkbookWithoutData() {
        assertThatThrownBy(() -> converter.convert(TestDocuments.emptyWorkbook()))
                .isInstanceOf(ConversionException.class)
                .extracting("code")
                .isEqualTo(ConversionException.NO_CONTENT);
    }
}
