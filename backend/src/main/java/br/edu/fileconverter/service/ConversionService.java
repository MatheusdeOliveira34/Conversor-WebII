package br.edu.fileconverter.service;

import br.edu.fileconverter.converter.ConverterRegistry;
import br.edu.fileconverter.domain.FileFormat;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Regras da conversão; trabalha só com o modelo de domínio, nunca com DTOs. */
@Service
public class ConversionService {

    private final ConverterRegistry registry;

    public ConversionService(ConverterRegistry registry) {
        this.registry = registry;
    }

    /** Destinos por origem, a partir dos conversores registrados (RF11). */
    public Map<FileFormat, Set<FileFormat>> supportedConversions() {
        return registry.supportedConversions();
    }
}
