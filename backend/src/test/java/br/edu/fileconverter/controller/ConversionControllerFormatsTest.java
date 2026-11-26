package br.edu.fileconverter.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.edu.fileconverter.config.CorsConfig;
import br.edu.fileconverter.converter.ConverterRegistry;
import br.edu.fileconverter.mapper.ConversionMapper;
import br.edu.fileconverter.service.ConversionService;
import br.edu.fileconverter.support.StubConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Endpoint de formatos com os seis pares registrados por conversores falsos. */
@WebMvcTest(ConversionController.class)
@Import({ConversionService.class, ConversionMapper.class, CorsConfig.class,
        ConversionControllerFormatsTest.Converters.class})
class ConversionControllerFormatsTest {

    @TestConfiguration
    static class Converters {
        @Bean
        ConverterRegistry converterRegistry() {
            return new ConverterRegistry(StubConverter.allSixPairs());
        }
    }

    @Autowired
    private MockMvc mockMvc;

    // CT06
    @Test
    void listsThreeSourcesWithTwoTargetsEach() throws Exception {
        mockMvc.perform(get("/api/v1/conversions/formats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].sourceFormat").value("PDF"))
                .andExpect(jsonPath("$[0].targetFormats.length()").value(2))
                .andExpect(jsonPath("$[1].sourceFormat").value("DOCX"))
                .andExpect(jsonPath("$[1].targetFormats[0]").value("PDF"))
                .andExpect(jsonPath("$[1].targetFormats[1]").value("XLSX"))
                .andExpect(jsonPath("$[2].sourceFormat").value("XLSX"))
                .andExpect(jsonPath("$[2].targetFormats.length()").value(2));
    }

    @Test
    void allowsFrontEndOrigin() throws Exception {
        mockMvc.perform(get("/api/v1/conversions/formats").header("Origin", "http://localhost:8081"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    @Test
    void blocksOtherOrigins() throws Exception {
        mockMvc.perform(get("/api/v1/conversions/formats").header("Origin", "http://site-malicioso.com"))
                .andExpect(status().isForbidden());
    }
}
