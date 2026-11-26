package br.edu.fileconverter.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
}
