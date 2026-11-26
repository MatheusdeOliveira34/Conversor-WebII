package br.edu.fileconverter.converter;

import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.exception.ConversionException;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.ooxml.POIXMLException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

/**
 * C1 — DOCX → PDF: parágrafos e tabelas do Word escritos em PDF, na ordem em que aparecem.
 * Imagens, cabeçalho/rodapé e fontes personalizadas não são levados.
 */
@Component
public class DocxToPdfConverter implements DocumentConverter {

    private static final Font TEXT_FONT = FontFactory.getFont(FontFactory.HELVETICA, 11);

    @Override
    public FileFormat source() {
        return FileFormat.DOCX;
    }

    @Override
    public FileFormat target() {
        return FileFormat.PDF;
    }

    @Override
    public byte[] convert(byte[] input) {
        try (XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(input))) {
            List<IBodyElement> elements = docx.getBodyElements();
            if (!hasContent(elements)) {
                throw new ConversionException(ConversionException.NO_CONTENT,
                        "O documento não tem texto nem tabelas para converter.");
            }
            return writePdf(elements);
        } catch (NotOfficeXmlFileException | POIXMLException | IOException e) {
            throw new ConversionException(ConversionException.CORRUPTED_FILE,
                    "O arquivo está corrompido ou não pôde ser lido.", e);
        }
    }

    private static byte[] writePdf(List<IBodyElement> elements) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4);
        PdfWriter.getInstance(pdf, out);
        pdf.open();
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph paragraph) {
                String text = paragraph.getText();
                // parágrafo vazio vira linha em branco, preservando o espaçamento do texto
                pdf.add(new Paragraph(text.isBlank() ? " " : text, TEXT_FONT));
            } else if (element instanceof XWPFTable table) {
                PdfPTable pdfTable = toPdfTable(table);
                if (pdfTable != null) {
                    pdf.add(pdfTable);
                }
            }
        }
        pdf.close();
        return out.toByteArray();
    }

    private static PdfPTable toPdfTable(XWPFTable table) {
        int columns = table.getRows().stream()
                .mapToInt(row -> row.getTableCells().size())
                .max()
                .orElse(0);
        if (columns == 0) {
            return null;
        }
        PdfPTable pdfTable = new PdfPTable(columns);
        pdfTable.setWidthPercentage(100);
        pdfTable.setSpacingBefore(8);
        pdfTable.setSpacingAfter(8);
        for (XWPFTableRow row : table.getRows()) {
            List<XWPFTableCell> cells = row.getTableCells();
            for (int i = 0; i < columns; i++) {
                String text = i < cells.size() ? cells.get(i).getText() : "";
                PdfPCell cell = new PdfPCell(new Phrase(text, TEXT_FONT));
                cell.setPadding(4);
                pdfTable.addCell(cell);
            }
        }
        return pdfTable;
    }

    private static boolean hasContent(List<IBodyElement> elements) {
        for (IBodyElement element : elements) {
            if (element instanceof XWPFParagraph paragraph && !paragraph.getText().isBlank()) {
                return true;
            }
            if (element instanceof XWPFTable table && !table.getText().isBlank()) {
                return true;
            }
        }
        return false;
    }
}
