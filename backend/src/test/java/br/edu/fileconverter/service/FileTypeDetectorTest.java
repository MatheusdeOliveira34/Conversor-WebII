package br.edu.fileconverter.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.domain.SourceDocument;
import br.edu.fileconverter.exception.InvalidFileException;
import br.edu.fileconverter.support.TestDocuments;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

// CT02
class FileTypeDetectorTest {

    private final FileTypeDetector detector = new FileTypeDetector();

    @Test
    void detectsPdf() {
        assertThat(detector.detect(TestDocuments.pdf("Olá"))).isEqualTo(FileFormat.PDF);
    }

    @Test
    void detectsDocx() {
        assertThat(detector.detect(TestDocuments.docx("Olá"))).isEqualTo(FileFormat.DOCX);
    }

    @Test
    void detectsXlsx() {
        assertThat(detector.detect(TestDocuments.xlsx("Olá"))).isEqualTo(FileFormat.XLSX);
    }

    @Test
    void rejectsTextFileRenamedToPdf() {
        byte[] text = "apenas um texto qualquer".getBytes(StandardCharsets.UTF_8);
        SourceDocument renamed = new SourceDocument("notas", FileFormat.PDF, text);

        assertThatThrownBy(() -> detector.validate(renamed))
                .isInstanceOf(InvalidFileException.class)
                .extracting("code")
                .isEqualTo(InvalidFileException.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsExtensionThatDoesNotMatchContent() {
        SourceDocument docxAsXlsx = new SourceDocument("planilha", FileFormat.XLSX, TestDocuments.docx("Olá"));

        assertThatThrownBy(() -> detector.validate(docxAsXlsx))
                .isInstanceOf(InvalidFileException.class)
                .extracting("code")
                .isEqualTo(InvalidFileException.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsZipThatIsNotOfficeDocument() {
        byte[] plainZip = {0x50, 0x4B, 0x03, 0x04, 1, 2, 3, 4, 5, 6};

        assertThatThrownBy(() -> detector.detect(plainZip))
                .isInstanceOf(InvalidFileException.class)
                .extracting("code")
                .isEqualTo(InvalidFileException.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> detector.detect(new byte[0]))
                .isInstanceOf(InvalidFileException.class)
                .extracting("code")
                .isEqualTo(InvalidFileException.EMPTY_FILE);
    }

    @Test
    void acceptsMatchingExtensionAndContent() {
        SourceDocument docx = new SourceDocument("relatorio", FileFormat.DOCX, TestDocuments.docx("Olá"));

        assertThatCode(() -> detector.validate(docx)).doesNotThrowAnyException();
    }
}
