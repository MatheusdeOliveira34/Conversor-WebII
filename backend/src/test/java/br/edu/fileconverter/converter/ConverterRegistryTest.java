package br.edu.fileconverter.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.exception.UnsupportedConversionException;
import br.edu.fileconverter.support.StubConverter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ConverterRegistryTest {

    private final ConverterRegistry registry = new ConverterRegistry(StubConverter.allSixPairs());

    // CT05
    @Test
    void rejectsSameSourceAndTarget() {
        assertThatThrownBy(() -> registry.find(FileFormat.PDF, FileFormat.PDF))
                .isInstanceOf(UnsupportedConversionException.class);
    }

    @Test
    void rejectsPairWithoutConverter() {
        ConverterRegistry onlyDocxToPdf =
                new ConverterRegistry(List.of(new StubConverter(FileFormat.DOCX, FileFormat.PDF)));

        assertThatThrownBy(() -> onlyDocxToPdf.find(FileFormat.PDF, FileFormat.DOCX))
                .isInstanceOf(UnsupportedConversionException.class);
    }

    @Test
    void findsRegisteredConverter() {
        DocumentConverter converter = registry.find(FileFormat.DOCX, FileFormat.PDF);

        assertThat(converter.source()).isEqualTo(FileFormat.DOCX);
        assertThat(converter.target()).isEqualTo(FileFormat.PDF);
    }

    @Test
    void listsSupportedConversionsInEnumOrder() {
        Map<FileFormat, Set<FileFormat>> supported = registry.supportedConversions();

        assertThat(supported.keySet()).containsExactly(FileFormat.PDF, FileFormat.DOCX, FileFormat.XLSX);
        assertThat(supported.get(FileFormat.DOCX)).containsExactly(FileFormat.PDF, FileFormat.XLSX);
    }

    @Test
    void emptyRegistryListsNothing() {
        assertThat(new ConverterRegistry(List.of()).supportedConversions()).isEmpty();
    }

    @Test
    void rejectsDuplicateConverter() {
        List<DocumentConverter> duplicated = List.of(
                new StubConverter(FileFormat.DOCX, FileFormat.PDF),
                new StubConverter(FileFormat.DOCX, FileFormat.PDF));

        assertThatThrownBy(() -> new ConverterRegistry(duplicated))
                .isInstanceOf(IllegalStateException.class);
    }
}
