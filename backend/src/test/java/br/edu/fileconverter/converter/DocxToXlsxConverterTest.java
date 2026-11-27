package br.edu.fileconverter.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.fileconverter.exception.ConversionException;
import br.edu.fileconverter.support.TestDocuments;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class DocxToXlsxConverterTest {

    private final DocxToXlsxConverter converter = new DocxToXlsxConverter();

    // CT01 (DOCX → XLSX)
    @Test
    void putsTextInTextSheetAndEachTableInItsOwnSheet() throws IOException {
        byte[] xlsx = converter.convert(TestDocuments.docx("Relatório de vendas"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);

            Sheet text = workbook.getSheet(DocxToXlsxConverter.TEXT_SHEET);
            assertThat(text.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Relatório de vendas");

            Sheet table = workbook.getSheet(DocxToXlsxConverter.TABLE_SHEET_PREFIX + 1);
            assertThat(table.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Produto");
            assertThat(table.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Quantidade");
            assertThat(table.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Caneta");
            assertThat(table.getRow(1).getCell(1).getStringCellValue()).isEqualTo("10");
        }
    }

    @Test
    void rejectsDocumentWithoutContent() {
        assertThatThrownBy(() -> converter.convert(TestDocuments.emptyDocx()))
                .isInstanceOf(ConversionException.class)
                .extracting("code")
                .isEqualTo(ConversionException.NO_CONTENT);
    }

    @Test
    void rejectsCorruptedFile() {
        byte[] garbage = "isso não é um docx".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> converter.convert(garbage))
                .isInstanceOf(ConversionException.class)
                .extracting("code")
                .isEqualTo(ConversionException.CORRUPTED_FILE);
    }
}
