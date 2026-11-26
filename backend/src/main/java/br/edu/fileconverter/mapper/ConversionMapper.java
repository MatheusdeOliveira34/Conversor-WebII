package br.edu.fileconverter.mapper;

import br.edu.fileconverter.domain.ConvertedDocument;
import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.domain.SourceDocument;
import br.edu.fileconverter.dto.ConversionRequest;
import br.edu.fileconverter.dto.ConversionResponse;
import br.edu.fileconverter.dto.SupportedConversionResponse;
import br.edu.fileconverter.exception.InvalidFileException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Única classe que conhece DTO e domínio ao mesmo tempo. */
@Component
public class ConversionMapper {

    static final int MAX_BASE_NAME_LENGTH = 100;
    static final String DEFAULT_BASE_NAME = "arquivo";

    /** DTO → domínio: lê os bytes, sanitiza o nome e resolve o formato pela extensão. */
    public SourceDocument toDomain(ConversionRequest request) {
        String originalName = request.file().getOriginalFilename();
        String fileName = stripPath(originalName == null ? "" : originalName);

        int dot = fileName.lastIndexOf('.');
        String extension = dot >= 0 ? fileName.substring(dot + 1) : "";
        String rawBaseName = dot >= 0 ? fileName.substring(0, dot) : fileName;

        FileFormat format = FileFormat.fromExtension(extension)
                .orElseThrow(() -> new InvalidFileException(
                        InvalidFileException.UNSUPPORTED_FILE_TYPE,
                        "Formato não suportado. Use PDF, DOCX ou XLSX."));

        return new SourceDocument(sanitizeBaseName(rawBaseName), format, readBytes(request));
    }

    /** Domínio → DTO: monta nome final, MIME, tamanho, duração e base64. */
    public ConversionResponse toResponse(ConvertedDocument document, FileFormat source) {
        FileFormat target = document.format();
        return new ConversionResponse(
                document.baseName() + "." + target.extension(),
                target.mimeType(),
                source,
                target,
                document.content().length,
                document.duration().toMillis(),
                Base64.getEncoder().encodeToString(document.content()));
    }

    /** Domínio → DTO: destinos de uma origem, na ordem do enum. */
    public SupportedConversionResponse toResponse(FileFormat source, Set<FileFormat> targets) {
        Set<FileFormat> ordered = targets.isEmpty()
                ? EnumSet.noneOf(FileFormat.class)
                : EnumSet.copyOf(targets);
        return new SupportedConversionResponse(source, Collections.unmodifiableSet(ordered));
    }

    /**
     * RN08: mantém só letras, números, '-', '_' e espaço, até 100 caracteres.
     * Se nada sobrar, usa "arquivo".
     */
    static String sanitizeBaseName(String rawBaseName) {
        String cleaned = rawBaseName
                .replaceAll("[^\\p{L}\\p{N} _-]", "")
                .replaceAll("\\s+", " ")
                .trim();
        if (cleaned.length() > MAX_BASE_NAME_LENGTH) {
            cleaned = cleaned.substring(0, MAX_BASE_NAME_LENGTH).trim();
        }
        return cleaned.isEmpty() ? DEFAULT_BASE_NAME : cleaned;
    }

    /** Remove qualquer caminho (Unix ou Windows) e fica só com o nome do arquivo. */
    private static String stripPath(String name) {
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return name.substring(slash + 1);
    }

    private static byte[] readBytes(ConversionRequest request) {
        try {
            return request.file().getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o arquivo enviado", e);
        }
    }
}
