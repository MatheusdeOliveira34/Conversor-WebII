package br.edu.fileconverter.dto;

import br.edu.fileconverter.domain.FileFormat;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/** Requisição de conversão, montada no controller a partir do multipart. */
public record ConversionRequest(
        @NotNull MultipartFile file,
        @NotNull FileFormat targetFormat) {
}
