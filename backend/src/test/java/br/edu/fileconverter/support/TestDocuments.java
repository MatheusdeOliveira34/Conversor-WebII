package br.edu.fileconverter.support;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;

/** Gera arquivos de teste em memória, sem binários no repositório. */
public final class TestDocuments {

    private TestDocuments() {
    }

    /** DOCX com um parágrafo e uma tabela 2x2. */
    public static byte[] docx(String paragraph) {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText(paragraph);
            XWPFTable table = document.createTable(2, 2);
            table.getRow(0).getCell(0).setText("Produto");
            table.getRow(0).getCell(1).setText("Quantidade");
            table.getRow(1).getCell(0).setText("Caneta");
            table.getRow(1).getCell(1).setText("10");
            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** DOCX sem nenhum texto. */
    public static byte[] emptyDocx() {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** XLSX com uma aba e uma célula. */
    public static byte[] xlsx(String cell) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet("Dados").createRow(0).createCell(0).setCellValue(cell);
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * XLSX com três abas: "Vendas" (cabeçalho, uma linha e uma fórmula =B2*C2 que dá 25),
     * "Vazia" (sem nada) e "Resumo" (uma célula).
     */
    public static byte[] salesWorkbook() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            org.apache.poi.ss.usermodel.Sheet sales = workbook.createSheet("Vendas");
            org.apache.poi.ss.usermodel.Row header = sales.createRow(0);
            header.createCell(0).setCellValue("Produto");
            header.createCell(1).setCellValue("Quantidade");
            header.createCell(2).setCellValue("Preço");
            header.createCell(3).setCellValue("Total");
            org.apache.poi.ss.usermodel.Row line = sales.createRow(1);
            line.createCell(0).setCellValue("Caneta");
            line.createCell(1).setCellValue(10);
            line.createCell(2).setCellValue(2.5);
            line.createCell(3).setCellFormula("B2*C2");
            workbook.createSheet("Vazia");
            workbook.createSheet("Resumo").createRow(0).createCell(0).setCellValue("Fechamento do mês");
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** XLSX com uma aba sem nenhuma célula. */
    public static byte[] emptyWorkbook() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.createSheet("Planilha1");
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** PDF com um parágrafo. */
    public static byte[] pdf(String paragraph) {
        return pdfPages(paragraph);
    }

    /** PDF com uma página por argumento; cada linha ({@code \n}) vira um parágrafo. */
    public static byte[] pdfPages(String... pages) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();
        for (int i = 0; i < pages.length; i++) {
            if (i > 0) {
                document.newPage();
            }
            for (String line : pages[i].split("\n")) {
                document.add(new Paragraph(line));
            }
        }
        document.close();
        return out.toByteArray();
    }

    /** PDF com uma página em branco, sem nenhum texto (simula um PDF escaneado). */
    public static byte[] blankPdf() {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** PDF que exige a senha {@code segredo} para abrir. */
    public static byte[] protectedPdf() {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            document.addPage(new PDPage());
            StandardProtectionPolicy policy =
                    new StandardProtectionPolicy("dono", "segredo", new AccessPermission());
            policy.setEncryptionKeyLength(128);
            document.protect(policy);
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
