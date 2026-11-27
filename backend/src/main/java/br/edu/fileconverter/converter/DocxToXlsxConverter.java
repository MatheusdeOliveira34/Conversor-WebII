package br.edu.fileconverter.converter;

import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.exception.ConversionException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

/**
 * C2 — DOCX → XLSX: cada tabela do Word vira uma aba ("Tabela 1", "Tabela 2"...) e o texto
 * solto vai para a aba "Texto", um parágrafo por linha. A formatação do texto se perde.
 */
@Component
public class DocxToXlsxConverter implements DocumentConverter {

    static final String TEXT_SHEET = "Texto";
    static final String TABLE_SHEET_PREFIX = "Tabela ";

    @Override
    public FileFormat source() {
        return FileFormat.DOCX;
    }

    @Override
    public FileFormat target() {
        return FileFormat.XLSX;
    }

    @Override
    public byte[] convert(byte[] input) {
        List<String> paragraphs = new ArrayList<>();
        List<XWPFTable> tables = new ArrayList<>();

        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(input))) {
            for (IBodyElement element : docx.getBodyElements()) {
                if (element instanceof XWPFParagraph paragraph && !paragraph.getText().isBlank()) {
                    paragraphs.add(paragraph.getText().strip());
                } else if (element instanceof XWPFTable table && !table.getText().isBlank()) {
                    tables.add(table);
                }
            }
            if (paragraphs.isEmpty() && tables.isEmpty()) {
                throw new ConversionException(ConversionException.NO_CONTENT,
                        "O documento não tem texto nem tabelas para converter.");
            }
            return writeWorkbook(paragraphs, tables);
        } catch (NotOfficeXmlFileException | POIXMLException | IOException e) {
            throw new ConversionException(ConversionException.CORRUPTED_FILE,
                    "O arquivo está corrompido ou não pôde ser lido.", e);
        }
    }

    private static byte[] writeWorkbook(List<String> paragraphs, List<XWPFTable> tables) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!paragraphs.isEmpty()) {
                Sheet sheet = workbook.createSheet(TEXT_SHEET);
                for (int i = 0; i < paragraphs.size(); i++) {
                    sheet.createRow(i).createCell(0).setCellValue(paragraphs.get(i));
                }
                sheet.setColumnWidth(0, 100 * 256); // ~100 caracteres, para o texto ficar legível
            }
            for (int t = 0; t < tables.size(); t++) {
                Sheet sheet = workbook.createSheet(TABLE_SHEET_PREFIX + (t + 1));
                List<XWPFTableRow> rows = tables.get(t).getRows();
                for (int r = 0; r < rows.size(); r++) {
                    Row row = sheet.createRow(r);
                    List<XWPFTableCell> cells = rows.get(r).getTableCells();
                    for (int c = 0; c < cells.size(); c++) {
                        row.createCell(c).setCellValue(cells.get(c).getText().strip());
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
