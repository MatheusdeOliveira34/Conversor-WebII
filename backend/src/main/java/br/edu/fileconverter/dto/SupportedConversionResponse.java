package br.edu.fileconverter.dto;

import br.edu.fileconverter.domain.FileFormat;
import java.util.Set;

/** Destinos possíveis para um formato de origem. */
public record SupportedConversionResponse(
        FileFormat sourceFormat,
        Set<FileFormat> targetFormats) {
}
