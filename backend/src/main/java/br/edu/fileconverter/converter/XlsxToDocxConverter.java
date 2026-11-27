package br.edu.fileconverter.converter;

import br.edu.fileconverter.converter.SpreadsheetReader.SheetTable;
import br.edu.fileconverter.domain.FileFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.springframework.stereotype.Component;

/**
 * C6 — XLSX → DOCX: cada aba com dados vira um título (nome da aba) e uma tabela no Word.
 * Fórmulas exportam só o valor; gráficos e mesclagens não são levados.
 */
@Component
public class XlsxToDocxConverter implements DocumentConverter {

    static final int TITLE_FONT_SIZE = 14;

    @Override
    public FileFormat source() {
        return FileFormat.XLSX;
    }

    @Override
    public FileFormat target() {
        return FileFormat.DOCX;
    }

    @Override
    public byte[] convert(byte[] input) {
        List<SheetTable> sheets = SpreadsheetReader.readSheets(input);

        try (XWPFDocument docx = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (SheetTable sheet : sheets) {
                XWPFRun title = docx.createParagraph().createRun();
                title.setBold(true);
                title.setFontSize(TITLE_FONT_SIZE);
                title.setText(sheet.name());

                XWPFTable table = docx.createTable(sheet.rows().size(), sheet.columns());
                table.setWidth("100%");
                for (int r = 0; r < sheet.rows().size(); r++) {
                    List<String> row = sheet.rows().get(r);
                    for (int c = 0; c < sheet.columns(); c++) {
                        table.getRow(r).getCell(c).setText(row.get(c));
                    }
                }
                docx.createParagraph(); // espaço entre uma aba e a próxima
            }
            docx.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o DOCX", e);
        }
    }
}
