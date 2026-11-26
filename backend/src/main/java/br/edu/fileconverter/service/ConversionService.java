package br.edu.fileconverter.service;

import br.edu.fileconverter.converter.ConverterRegistry;
import br.edu.fileconverter.converter.DocumentConverter;
import br.edu.fileconverter.domain.ConvertedDocument;
import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.domain.SourceDocument;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Regras da conversão; trabalha só com o modelo de domínio, nunca com DTOs. */
@Service
public class ConversionService {

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

        return new ConvertedDocument(source.baseName(), target, content, duration);
    }
}
