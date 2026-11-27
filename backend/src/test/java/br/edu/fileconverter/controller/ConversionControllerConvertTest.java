package br.edu.fileconverter.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.support.TestDocuments;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

/** POST /api/v1/conversions com a aplicação inteira e o conversor real. */
@SpringBootTest
@AutoConfigureMockMvc
class ConversionControllerConvertTest {

    @Autowired
    private MockMvc mockMvc;

    // CT07
    @Test
    void convertsDocxToPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "relatorio.docx", FileFormat.DOCX.mimeType(), TestDocuments.docx("Relatório"));

        String body = mockMvc.perform(multipart("/api/v1/conversions")
                        .file(file)
                        .param("targetFormat", "PDF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("relatorio.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.sourceFormat").value("DOCX"))
                .andExpect(jsonPath("$.targetFormat").value("PDF"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        byte[] pdf = Base64.getDecoder().decode(JsonPath.<String>read(body, "$.contentBase64"));
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(JsonPath.<Integer>read(body, "$.sizeBytes")).isEqualTo(pdf.length);
    }

    // CT06 com os conversores reais: a matriz completa da visao.md
    @Test
    void listsAllSixRealConversions() throws Exception {
        mockMvc.perform(get("/api/v1/conversions/formats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].sourceFormat").value("PDF"))
                .andExpect(jsonPath("$[0].targetFormats", contains("DOCX", "XLSX")))
                .andExpect(jsonPath("$[1].sourceFormat").value("DOCX"))
                .andExpect(jsonPath("$[1].targetFormats", contains("PDF", "XLSX")))
                .andExpect(jsonPath("$[2].sourceFormat").value("XLSX"))
                .andExpect(jsonPath("$[2].targetFormats", contains("PDF", "DOCX")));
    }

    @Test
    void convertsXlsxToDocx() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "vendas.xlsx", FileFormat.XLSX.mimeType(), TestDocuments.salesWorkbook());

        mockMvc.perform(multipart("/api/v1/conversions").file(file).param("targetFormat", "DOCX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("vendas.docx"))
                .andExpect(jsonPath("$.sourceFormat").value("XLSX"))
                .andExpect(jsonPath("$.targetFormat").value("DOCX"));
    }
}
