package br.edu.fileconverter.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.fileconverter.domain.ConvertedDocument;
import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.domain.SourceDocument;
import br.edu.fileconverter.dto.ConversionRequest;
import br.edu.fileconverter.dto.ConversionResponse;
import br.edu.fileconverter.dto.SupportedConversionResponse;
import br.edu.fileconverter.exception.InvalidFileException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ConversionMapperTest {

    private final ConversionMapper mapper = new ConversionMapper();

    private static ConversionRequest request(String originalName, byte[] content) {
        return new ConversionRequest(
                new MockMultipartFile("file", originalName, "application/octet-stream", content),
                FileFormat.PDF);
    }

    // CT03
    @Test
    void toResponseBuildsFileNameMimeAndBase64() {
        byte[] content = "%PDF-1.7 conteudo".getBytes(StandardCharsets.UTF_8);
        ConvertedDocument converted = new ConvertedDocument(
                "relatorio", FileFormat.PDF, content, Duration.ofMillis(312));

        ConversionResponse response = mapper.toResponse(converted, FileFormat.DOCX);

        assertThat(response.fileName()).isEqualTo("relatorio.pdf");
        assertThat(response.contentType()).isEqualTo("application/pdf");
        assertThat(response.sourceFormat()).isEqualTo(FileFormat.DOCX);
        assertThat(response.targetFormat()).isEqualTo(FileFormat.PDF);
        assertThat(response.sizeBytes()).isEqualTo(content.length);
        assertThat(response.durationMs()).isEqualTo(312);
        assertThat(Base64.getDecoder().decode(response.contentBase64())).isEqualTo(content);
    }

    // CT04
    @Test
    void toDomainSanitizesPathTraversalName() {
        SourceDocument document = mapper.toDomain(request("../../etc/passwd.docx", new byte[] {1}));

        assertThat(document.baseName())
                .isEqualTo("passwd")
                .doesNotContain("/", "\\", "..");
        assertThat(document.format()).isEqualTo(FileFormat.DOCX);
    }

    @Test
    void toDomainStripsWindowsPathAndKeepsAccentsAndSpaces() {
        SourceDocument document = mapper.toDomain(
                request("C:\\Users\\aluno\\Relatório Final (v2).XLSX", new byte[] {1}));

        assertThat(document.baseName()).isEqualTo("Relatório Final v2");
        assertThat(document.format()).isEqualTo(FileFormat.XLSX);
    }

    @Test
    void toDomainKeepsBytes() {
        byte[] content = {1, 2, 3};

        SourceDocument document = mapper.toDomain(request("planilha.pdf", content));

        assertThat(document.content()).isEqualTo(content);
    }

    @Test
    void toDomainRejectsUnsupportedExtension() {
        assertThatThrownBy(() -> mapper.toDomain(request("foto.png", new byte[] {1})))
                .isInstanceOf(InvalidFileException.class)
                .extracting("code")
                .isEqualTo(InvalidFileException.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void toDomainRejectsMissingExtension() {
        assertThatThrownBy(() -> mapper.toDomain(request("relatorio", new byte[] {1})))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void sanitizeBaseNameLimitsLengthAndFallsBackToDefault() {
        assertThat(ConversionMapper.sanitizeBaseName("a".repeat(150)))
                .hasSize(ConversionMapper.MAX_BASE_NAME_LENGTH);
        assertThat(ConversionMapper.sanitizeBaseName("../..")).isEqualTo("arquivo");
        assertThat(ConversionMapper.sanitizeBaseName("")).isEqualTo("arquivo");
    }

    @Test
    void toResponseListsTargetsInEnumOrder() {
        SupportedConversionResponse response =
                mapper.toResponse(FileFormat.DOCX, Set.of(FileFormat.XLSX, FileFormat.PDF));

        assertThat(response.sourceFormat()).isEqualTo(FileFormat.DOCX);
        assertThat(response.targetFormats()).containsExactly(FileFormat.PDF, FileFormat.XLSX);
    }
}
