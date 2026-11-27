package br.edu.fileconverter.config;

import jakarta.annotation.PostConstruct;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.context.annotation.Configuration;

/**
 * RNF08: proteção contra zip bomb ao abrir DOCX/XLSX. O POI recusa entradas do ZIP que
 * se expandem demais em relação ao tamanho compactado ou que passam do tamanho máximo.
 */
@Configuration
public class PoiSecurityConfig {

    /** Uma entrada não pode descompactar para mais de 100x o seu tamanho compactado. */
    static final double MIN_INFLATE_RATIO = 0.01;

    /** Nenhuma entrada do ZIP pode passar de 100 MB descompactada. */
    static final long MAX_ENTRY_SIZE = 100L * 1024 * 1024;

    @PostConstruct
    void configureZipLimits() {
        ZipSecureFile.setMinInflateRatio(MIN_INFLATE_RATIO);
        ZipSecureFile.setMaxEntrySize(MAX_ENTRY_SIZE);
    }
}
