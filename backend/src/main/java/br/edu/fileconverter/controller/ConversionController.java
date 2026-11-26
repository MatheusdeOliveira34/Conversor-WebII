package br.edu.fileconverter.controller;

import br.edu.fileconverter.dto.SupportedConversionResponse;
import br.edu.fileconverter.mapper.ConversionMapper;
import br.edu.fileconverter.service.ConversionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
