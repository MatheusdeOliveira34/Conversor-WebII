package br.edu.fileconverter.converter;

import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.exception.UnsupportedConversionException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Índice dos conversores registrados no Spring, por origem e destino. */
@Component
public class ConverterRegistry {

    private final Map<FileFormat, Map<FileFormat, DocumentConverter>> converters =
            new EnumMap<>(FileFormat.class);

    /** Usado pelo Spring: funciona mesmo sem nenhum conversor registrado. */
    @Autowired
    public ConverterRegistry(ObjectProvider<DocumentConverter> converters) {
        this(converters.orderedStream().toList());
    }

    public ConverterRegistry(List<DocumentConverter> converters) {
        converters.forEach(this::register);
    }

    private void register(DocumentConverter converter) {
        if (converter.source() == converter.target()) {
            throw new IllegalStateException(
                    "Conversor com origem igual ao destino: " + converter.getClass().getName());
        }
        DocumentConverter previous = converters
                .computeIfAbsent(converter.source(), format -> new EnumMap<>(FileFormat.class))
                .putIfAbsent(converter.target(), converter);
        if (previous != null) {
            throw new IllegalStateException("Conversor duplicado para " + converter.source()
                    + " → " + converter.target() + ": " + previous.getClass().getName()
                    + " e " + converter.getClass().getName());
        }
    }

    /** Conversor do par pedido; lança {@link UnsupportedConversionException} se não houver (RN01). */
    public DocumentConverter find(FileFormat source, FileFormat target) {
        DocumentConverter converter = source == target
                ? null
                : converters.getOrDefault(source, Map.of()).get(target);
        if (converter == null) {
            throw new UnsupportedConversionException(source, target);
        }
        return converter;
    }

    /** Destinos por origem, na ordem do enum {@link FileFormat}. */
    public Map<FileFormat, Set<FileFormat>> supportedConversions() {
        Map<FileFormat, Set<FileFormat>> result = new EnumMap<>(FileFormat.class);
        converters.forEach((source, byTarget) -> result.put(
                source, Collections.unmodifiableSet(EnumSet.copyOf(byTarget.keySet()))));
        return Collections.unmodifiableMap(result);
    }
}
