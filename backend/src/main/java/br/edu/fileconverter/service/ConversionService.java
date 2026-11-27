package br.edu.fileconverter.service;

import br.edu.fileconverter.converter.ConverterRegistry;
import br.edu.fileconverter.converter.DocumentConverter;
import br.edu.fileconverter.domain.ConvertedDocument;
import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.domain.SourceDocument;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Regras da conversão; trabalha só com o modelo de domínio, nunca com DTOs. */
@Service
public class ConversionService {

    private static final Logger log = LoggerFactory.getLogger(ConversionService.class);

    private final ConverterRegistry registry;
    private final FileTypeDetector detector;

    public ConversionService(ConverterRegistry registry, FileTypeDetector detector) {
        this.registry = registry;
        this.detector = detector;
    }

    /** Destinos por origem, a partir dos conversores registrados (RF11). */
    public Map<FileFormat, Set<FileFormat>> supportedConversions() {
        return registry.supportedConversions();
    }

    /**
     * Confere o conteúdo contra a extensão (RN02, RN03), escolhe o conversor (RN01)
     * e mede quanto tempo a conversão levou. Tudo em memória (RNF04).
     */
    public ConvertedDocument convert(SourceDocument source, FileFormat target) {
        detector.validate(source);
        DocumentConverter converter = registry.find(source.format(), target);

        long start = System.nanoTime();
        byte[] content = converter.convert(source.content());
        Duration duration = Duration.ofNanos(System.nanoTime() - start);

        // RNF12: só metadados, nunca o nome nem o conteúdo do arquivo
        log.info("Conversão {} → {}: {} bytes → {} bytes em {} ms",
                source.format(), target, source.content().length, content.length, duration.toMillis());

        return new ConvertedDocument(source.baseName(), target, content, duration);
    }
}
