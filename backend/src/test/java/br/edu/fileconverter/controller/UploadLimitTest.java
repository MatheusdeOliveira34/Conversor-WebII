package br.edu.fileconverter.controller;

import static org.assertj.core.api.Assertions.assertThat;

import br.edu.fileconverter.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * O limite de upload só é aplicado pelo servidor de verdade (o MockMvc não passa pelo
 * parser de multipart do Tomcat), por isso este teste sobe a aplicação numa porta aleatória.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UploadLimitTest {

    @Autowired
    private TestRestTemplate restTemplate;

    // CT09
    @Test
    void fileLargerThan10MbReturns413() {
        byte[] elevenMb = new byte[11 * 1024 * 1024];
        ByteArrayResource file = new ByteArrayResource(elevenMb) {
            @Override
            public String getFilename() {
                return "grande.pdf";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);
        body.add("targetFormat", "DOCX");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/v1/conversions", new HttpEntity<>(body, headers), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("FILE_TOO_LARGE");
    }
}
