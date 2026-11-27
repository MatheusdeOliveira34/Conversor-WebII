package br.edu.fileconverter.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.support.TestDocuments;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Erros do POST /api/v1/conversions sempre como ErrorResponse, com o status da tabela de erros. */
@SpringBootTest
@AutoConfigureMockMvc
class ConversionControllerErrorsTest {

    private static final String URL = "/api/v1/conversions";

    @Autowired
    private MockMvc mockMvc;

    private ResultActions send(String fileName, byte[] content, String targetFormat) throws Exception {
        return mockMvc.perform(multipart(URL)
                .file(new MockMultipartFile("file", fileName, "application/octet-stream", content))
                .param("targetFormat", targetFormat));
    }

    private static void assertErrorBody(ResultActions result, String code) throws Exception {
        result.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value(URL))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(content().string(not(containsString("Exception"))));
    }

    // CT08
    @Test
    void missingFileReturns400() throws Exception {
        ResultActions result = mockMvc.perform(multipart(URL).param("targetFormat", "PDF"))
                .andExpect(status().isBadRequest());
        assertErrorBody(result, "INVALID_REQUEST");
    }

    @Test
    void missingTargetFormatReturns400() throws Exception {
        ResultActions result = mockMvc.perform(multipart(URL)
                        .file(new MockMultipartFile("file", "relatorio.docx", null, TestDocuments.docx("Olá"))))
                .andExpect(status().isBadRequest());
        assertErrorBody(result, "INVALID_REQUEST");
    }

    @Test
    void invalidTargetFormatReturns400() throws Exception {
        ResultActions result = send("relatorio.docx", TestDocuments.docx("Olá"), "TXT")
                .andExpect(status().isBadRequest());
        assertErrorBody(result, "INVALID_REQUEST");
    }

    @Test
    void requestThatIsNotMultipartReturns400() throws Exception {
        ResultActions result = mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        assertErrorBody(result, "INVALID_REQUEST");
    }

    @Test
    void emptyFileReturns400() throws Exception {
        ResultActions result = send("relatorio.docx", new byte[0], "PDF")
                .andExpect(status().isBadRequest());
        assertErrorBody(result, "EMPTY_FILE");
    }

    @Test
    void sameSourceAndTargetReturns400() throws Exception {
        ResultActions result = send("relatorio.pdf", TestDocuments.pdf("Olá"), "PDF")
                .andExpect(status().isBadRequest());
        assertErrorBody(result, "UNSUPPORTED_CONVERSION");
    }

    @Test
    void unsupportedExtensionReturns415() throws Exception {
        ResultActions result = send("foto.png", new byte[] {1, 2, 3}, "PDF")
                .andExpect(status().isUnsupportedMediaType());
        assertErrorBody(result, "UNSUPPORTED_FILE_TYPE");
    }

    // CT10
    @Test
    void randomBytesWithXlsxExtensionNeverReturns500() throws Exception {
        byte[] random = new byte[2048];
        new Random(42).nextBytes(random);

        ResultActions result = send("planilha.xlsx", random, "PDF")
                .andExpect(status().isUnsupportedMediaType());
        assertErrorBody(result, "UNSUPPORTED_FILE_TYPE");
    }

    @Test
    void corruptedDocxReturns422() throws Exception {
        // começa como ZIP e cita word/document.xml, mas não é um DOCX válido
        byte[] fake = ("PK\u0003\u0004 word/document.xml conteúdo quebrado").getBytes();

        ResultActions result = send("relatorio.docx", fake, "PDF")
                .andExpect(status().isUnprocessableEntity());
        assertErrorBody(result, "CORRUPTED_FILE");
    }

    // CT11
    @Test
    void passwordProtectedPdfReturns422() throws Exception {
        ResultActions result = send("contrato.pdf", TestDocuments.protectedPdf(), FileFormat.DOCX.name())
                .andExpect(status().isUnprocessableEntity());
        assertErrorBody(result, "PROTECTED_FILE");
    }

    @Test
    void pdfWithoutTextReturns422() throws Exception {
        ResultActions result = send("escaneado.pdf", TestDocuments.blankPdf(), "DOCX")
                .andExpect(status().isUnprocessableEntity());
        assertErrorBody(result, "NO_CONTENT");
    }

    @Test
    void unknownRouteReturns404AsErrorResponse() throws Exception {
        mockMvc.perform(get("/api/v1/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
