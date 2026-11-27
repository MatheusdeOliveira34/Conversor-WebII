package br.edu.fileconverter.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.MultipartConfigElement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** RNF04/RNF07: nenhum upload aceito pode ser gravado em disco pelo servidor. */
@SpringBootTest
class MultipartConfigTest {

    @Autowired
    private MultipartConfigElement multipartConfig;

    @Test
    void uploadsWithinLimitStayInMemory() {
        assertThat(multipartConfig.getMaxFileSize()).isEqualTo(10L * 1024 * 1024);
        // acima do threshold o Tomcat grava em arquivo temporário; ele precisa ser maior que o limite
        assertThat((long) multipartConfig.getFileSizeThreshold())
                .isGreaterThan(multipartConfig.getMaxFileSize());
    }
}
