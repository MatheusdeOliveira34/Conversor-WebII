package br.edu.fileconverter.controller;

import br.edu.fileconverter.domain.ConvertedDocument;
import br.edu.fileconverter.domain.FileFormat;
import br.edu.fileconverter.domain.SourceDocument;
import br.edu.fileconverter.dto.ConversionRequest;
import br.edu.fileconverter.dto.ConversionResponse;
import br.edu.fileconverter.dto.SupportedConversionResponse;
import br.edu.fileconverter.mapper.ConversionMapper;
import br.edu.fileconverter.service.ConversionService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Fronteira HTTP: recebe e devolve DTOs, sem regra de negócio. */
@RestController
@RequestMapping("/api/v1/conversions")
public class ConversionController {

    private final ConversionService service;
    private final ConversionMapper mapper;

    public ConversionController(ConversionService service, ConversionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @GetMapping("/formats")
    public List<SupportedConversionResponse> formats() {
        return service.supportedConversions().entrySet().stream()
                .map(entry -> mapper.toResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ConversionResponse convert(
            @RequestPart("file") MultipartFile file,
            @RequestParam("targetFormat") FileFormat targetFormat) {
        ConversionRequest request = new ConversionRequest(file, targetFormat);
        SourceDocument source = mapper.toDomain(request);
        ConvertedDocument converted = service.convert(source, request.targetFormat());
        return mapper.toResponse(converted, source.format());
    }
}
