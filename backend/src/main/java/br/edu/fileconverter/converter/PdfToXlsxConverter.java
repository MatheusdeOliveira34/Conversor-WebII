package br.edu.fileconverter.converter;

import br.edu.fileconverter.domain.FileFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * C4 — PDF → XLSX: cada linha de texto vira uma linha da planilha, numa aba só, e as colunas
 * são separadas por tab ou por dois ou mais espaços seguidos. Linhas em branco são ignoradas.
 * Funciona bem com PDFs de tabelas alinhadas; sem alinhamento claro, cada linha vira uma célula.
 */
@Component
public class PdfToXlsxConverter implements DocumentConverter {

    static final String SHEET_NAME = "PDF";
    private static final Pattern COLUMN_SEPARATOR = Pattern.compile("\\t+| {2,}");

    @Override
    public FileFormat source() {
        return FileFormat.PDF;
    }

    @Override
    public FileFormat target() {
        return FileFormat.XLSX;
    }

    @Override
    public byte[] convert(byte[] input) {
        List<List<String>> pages = PdfTextReader.readPages(input);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(SHEET_NAME);
            int rowIndex = 0;
            for (List<String> lines : pages) {
                for (String line : lines) {
                    if (line.isBlank()) {
                        continue;
                    }
                    Row row = sheet.createRow(rowIndex++);
                    String[] columns = COLUMN_SEPARATOR.split(line.strip());
                    for (int col = 0; col < columns.length; col++) {
                        row.createCell(col).setCellValue(columns[col]);
                    }
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o XLSX", e);
        }
    }
}
