package br.edu.fileconverter.converter;

import br.edu.fileconverter.converter.SpreadsheetReader.SheetTable;
import br.edu.fileconverter.domain.FileFormat;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * C5 — XLSX → PDF: cada aba com dados vira uma tabela no PDF, com o nome da aba como título.
 * Fórmulas exportam só o valor calculado; cores e gráficos não são levados.
 */
@Component
public class XlsxToPdfConverter implements DocumentConverter {

    /** A partir deste número de colunas a página fica deitada, para a tabela caber. */
    static final int LANDSCAPE_FROM_COLUMNS = 7;

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font CELL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);

    @Override
    public FileFormat source() {
        return FileFormat.XLSX;
    }

    @Override
    public FileFormat target() {
        return FileFormat.PDF;
    }

    @Override
    public byte[] convert(byte[] input) {
        List<SheetTable> sheets = SpreadsheetReader.readSheets(input);
        int maxColumns = sheets.stream().mapToInt(SheetTable::columns).max().orElse(1);
        Rectangle pageSize = maxColumns >= LANDSCAPE_FROM_COLUMNS ? PageSize.A4.rotate() : PageSize.A4;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document pdf = new Document(pageSize);
        PdfWriter.getInstance(pdf, out);
        pdf.open();
        for (int i = 0; i < sheets.size(); i++) {
            SheetTable sheet = sheets.get(i);
            if (i > 0) {
                pdf.newPage();
            }
            Paragraph title = new Paragraph(sheet.name(), TITLE_FONT);
            title.setSpacingAfter(8);
            pdf.add(title);
            pdf.add(toPdfTable(sheet));
        }
        pdf.close();
        return out.toByteArray();
    }

    private static PdfPTable toPdfTable(SheetTable sheet) {
        PdfPTable table = new PdfPTable(sheet.columns());
        table.setWidthPercentage(100);
        if (sheet.rows().size() > 1) {
            // repete a primeira linha se a tabela quebrar de página; com uma linha só o OpenPDF
            // trataria a tabela inteira como cabeçalho e não desenharia nada
            table.setHeaderRows(1);
        }
        for (List<String> row : sheet.rows()) {
            for (String value : row) {
                PdfPCell cell = new PdfPCell(new Phrase(value, CELL_FONT));
                cell.setPadding(3);
                table.addCell(cell);
            }
        }
        return table;
    }
}
