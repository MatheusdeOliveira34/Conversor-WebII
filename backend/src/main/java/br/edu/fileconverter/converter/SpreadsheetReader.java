package br.edu.fileconverter.converter;

import br.edu.fileconverter.exception.ConversionException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Leitura de XLSX com Apache POI, compartilhada pelos conversores que partem de planilha.
 * Cada aba vira uma {@link SheetTable} com os valores como o Excel exibe (RN06);
 * linhas e abas vazias são descartadas (RN07).
 */
final class SpreadsheetReader {

    /** Uma aba já normalizada: só linhas com algum valor, todas com o mesmo número de colunas. */
    record SheetTable(String name, List<List<String>> rows, int columns) {
    }

    private static final Locale PT_BR = new Locale("pt", "BR");

    private SpreadsheetReader() {
    }

    /** Abas com dados, na ordem da pasta de trabalho. Lança {@code NO_CONTENT} se todas forem vazias. */
    static List<SheetTable> readSheets(byte[] input) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(input))) {
            DataFormatter formatter = new DataFormatter(PT_BR);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            List<SheetTable> sheets = new ArrayList<>();
            for (Sheet sheet : workbook) {
                SheetTable table = readSheet(sheet, formatter, evaluator);
                if (table != null) {
                    sheets.add(table);
                }
            }
            if (sheets.isEmpty()) {
                throw new ConversionException(ConversionException.NO_CONTENT,
                        "Planilha sem dados. Todas as abas estão vazias.");
            }
            return sheets;
        } catch (NotOfficeXmlFileException | POIXMLException | IOException e) {
            throw new ConversionException(ConversionException.CORRUPTED_FILE,
                    "O arquivo está corrompido ou não pôde ser lido.", e);
        }
    }

    private static SheetTable readSheet(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
        List<List<String>> rows = new ArrayList<>();
        int firstColumn = Integer.MAX_VALUE;
        int lastColumn = -1;

        for (Row row : sheet) {
            List<String> values = new ArrayList<>();
            boolean hasValue = false;
            for (int col = 0; col < Math.max(row.getLastCellNum(), 0); col++) {
                String value = format(row.getCell(col), formatter, evaluator);
                values.add(value);
                if (!value.isBlank()) {
                    hasValue = true;
                    firstColumn = Math.min(firstColumn, col);
                    lastColumn = Math.max(lastColumn, col);
                }
            }
            if (hasValue) {
                rows.add(values);
            }
        }
        if (rows.isEmpty()) {
            return null; // RN07: aba vazia é ignorada
        }

        // corta as colunas vazias das bordas e iguala o tamanho de todas as linhas
        List<List<String>> normalized = new ArrayList<>();
        for (List<String> values : rows) {
            List<String> line = new ArrayList<>();
            for (int col = firstColumn; col <= lastColumn; col++) {
                line.add(col < values.size() ? values.get(col) : "");
            }
            normalized.add(line);
        }
        return new SheetTable(sheet.getSheetName(), normalized, lastColumn - firstColumn + 1);
    }

    /** Valor da célula como o Excel exibe; fórmulas exportam o resultado, não a fórmula (RN06). */
    private static String format(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() != CellType.FORMULA) {
            return formatter.formatCellValue(cell).strip();
        }
        try {
            return formatter.formatCellValue(cell, evaluator).strip();
        } catch (RuntimeException e) {
            // função que o POI não sabe calcular: usa o último resultado salvo pelo Excel
            return cachedFormulaResult(cell, formatter).strip();
        }
    }

    private static String cachedFormulaResult(Cell cell, DataFormatter formatter) {
        return switch (cell.getCachedFormulaResultType()) {
            case NUMERIC -> formatter.formatRawCellContents(cell.getNumericCellValue(),
                    cell.getCellStyle().getDataFormat(), cell.getCellStyle().getDataFormatString());
            case STRING -> cell.getStringCellValue();
            case BOOLEAN -> cell.getBooleanCellValue() ? "VERDADEIRO" : "FALSO";
            case ERROR -> FormulaError.forInt(cell.getErrorCellValue()).getString();
            default -> "";
        };
    }
}
