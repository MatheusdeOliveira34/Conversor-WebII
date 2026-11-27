package br.edu.fileconverter.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.fileconverter.exception.ConversionException;
import br.edu.fileconverter.support.TestDocuments;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class PdfToXlsxConverterTest {

    private final PdfToXlsxConverter converter = new PdfToXlsxConverter();

    // CT01 (PDF → XLSX)
    @Test
    void convertsEachLineToRowAndSplitsColumnsOnWideSpaces() throws IOException {
        byte[] pdf = TestDocuments.pdfPages("Produto     Quantidade\nCaneta     10", "Caderno     3");

        byte[] xlsx = converter.convert(pdf);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            Sheet sheet = workbook.getSheet(PdfToXlsxConverter.SHEET_NAME);
            assertThat(sheet.getPhysicalNumberOfRows()).isEqualTo(3);
            assertRow(sheet.getRow(0), "Produto", "Quantidade");
            assertRow(sheet.getRow(1), "Caneta", "10");
            assertRow(sheet.getRow(2), "Caderno", "3");
        }
    }

    @Test
    void keepsLineWithoutColumnsInSingleCell() throws IOException {
        byte[] xlsx = converter.convert(TestDocuments.pdf("Texto corrido sem colunas"));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            assertRow(workbook.getSheet(PdfToXlsxConverter.SHEET_NAME).getRow(0), "Texto corrido sem colunas");
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

    private static void assertRow(Row row, String... expected) {
        assertThat(row.getPhysicalNumberOfCells()).isEqualTo(expected.length);
        for (int i = 0; i < expected.length; i++) {
            assertThat(row.getCell(i).getStringCellValue()).isEqualTo(expected[i]);
        }
    }
}
