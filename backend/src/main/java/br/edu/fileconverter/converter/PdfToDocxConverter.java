package br.edu.fileconverter.converter;

import br.edu.fileconverter.domain.FileFormat;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Component;

/**
 * C3 — PDF → DOCX: texto extraído por página, um parágrafo por linha e quebra de página
 * entre as páginas do PDF. Layout, imagens e tabelas não são reconstruídos.
 */
@Component
public class PdfToDocxConverter implements DocumentConverter {

    @Override
    public FileFormat source() {
        return FileFormat.PDF;
    }

    @Override
    public FileFormat target() {
        return FileFormat.DOCX;
    }

    @Override
    public byte[] convert(byte[] input) {
        List<List<String>> pages = PdfTextReader.readPages(input);

        try (XWPFDocument docx = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < pages.size(); i++) {
                List<String> lines = pages.get(i);
                if (lines.isEmpty()) {
                    lines = List.of("");
                }
                for (int j = 0; j < lines.size(); j++) {
                    XWPFParagraph paragraph = docx.createParagraph();
                    // a primeira linha de cada página (menos a primeira) começa em página nova
                    paragraph.setPageBreak(i > 0 && j == 0);
                    paragraph.createRun().setText(lines.get(j));
                }
            }
            docx.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao gerar o DOCX", e);
        }
    }
}
