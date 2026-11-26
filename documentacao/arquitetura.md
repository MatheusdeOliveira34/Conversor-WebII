# Arquitetura — FileConverter

Organização do back-end e do front-end, comunicação entre eles, contrato da API, padrão de nomes e tratamento de erros.

## 1. Visão geral e stack

Cliente-servidor em um único repositório Git: a pasta `frontend/` (Vue 2) chama a API REST da pasta `backend/` (Spring Boot) por HTTP. O back-end é stateless e não tem banco.

```text
fileconverter/
├── documentacao/   visao.md, requisitos.md, casos-uso.md, arquitetura.md, interface.md
├── backend/        projeto Maven Spring Boot (pom.xml, src/main/java, src/test/java)
├── frontend/       projeto Vue CLI (package.json, src/, tests/unit/)
├── .gitignore
└── README.md
```

### Camadas

```text
 Navegador (Vue 2)                         Servidor (Spring Boot)
+--------------------+   HTTP / JSON   +--------------------------------------------------+
| App.vue            |  multipart -->  | ConversionController   (recebe/devolve DTOs)     |
|  └ conversionApi.js| <-- JSON (DTO)  |        │  ConversionRequest / ConversionResponse |
+--------------------+                 |        ▼                                         |
                                       | ConversionMapper  ◄── único que conhece DTO      |
                                       |        │              e domínio ao mesmo tempo   |
                                       |        ▼  SourceDocument / ConvertedDocument     |
                                       | ConversionService ── FileTypeDetector            |
                                       |        │                                         |
                                       |        ▼                                         |
                                       | ConverterRegistry ── DocumentConverter (x6)      |
                                       |                                                  |
                                       | GlobalExceptionHandler ── qualquer exceção       |
                                       |                           vira ErrorResponse     |
                                       +--------------------------------------------------+
```

O DTO só existe entre front e controller; o mapper é a única peça que conhece DTO e domínio ao mesmo tempo. Qualquer exceção sobe até o handler global e vira `ErrorResponse`.

| Camada | Tecnologia | Versão |
| --- | --- | --- |
| Front-end | Vue + Axios + Vue CLI | 2.7 / 1.x / 5 |
| Back-end | Java + Spring Boot (Web, Validation) | 17 / 3.x |
| Word e Excel | Apache POI (`poi-ooxml`) | 5.x |
| Leitura de PDF | Apache PDFBox | 3.x |
| Escrita de PDF | OpenPDF | 2.x |
| Testes | JUnit 5, AssertJ, MockMvc / Jest, Vue Test Utils | — / 1.x |

## 2. Organização do back-end

Pacotes por camada técnica dentro de `backend/src/main/java/br/edu/fileconverter/`:

```text
br.edu.fileconverter
├── FileConverterApplication.java
├── config/        CorsConfig, PoiSecurityConfig
├── controller/    ConversionController
├── dto/           ConversionRequest, ConversionResponse, SupportedConversionResponse, ErrorResponse
├── mapper/        ConversionMapper
├── domain/        FileFormat, SourceDocument, ConvertedDocument
├── service/       ConversionService, FileTypeDetector
├── converter/     DocumentConverter, ConverterRegistry, DocxToPdfConverter, DocxToXlsxConverter,
│                  PdfToDocxConverter, PdfToXlsxConverter, XlsxToPdfConverter, XlsxToDocxConverter
└── exception/     ConversionException, UnsupportedConversionException, InvalidFileException,
                   GlobalExceptionHandler (@RestControllerAdvice)
```

| Pacote | Responsabilidade |
| --- | --- |
| `config` | CORS para a origem do front e limites de segurança do POI |
| `controller` | Recebe o multipart, monta o `ConversionRequest` e devolve DTOs; sem regra de negócio |
| `dto` | Records que trafegam na fronteira da API |
| `mapper` | Traduz DTO ↔ domínio (sanitiza nome, resolve formato, base64) |
| `domain` | Modelo interno usado pelo serviço e pelos conversores |
| `service` | Orquestra: detecta o formato real, escolhe o conversor, mede a duração |
| `converter` | Um conversor por par origem → destino, registrados automaticamente pelo Spring |
| `exception` | Exceções de domínio e tradução para `ErrorResponse` |

Limite de upload configurado em `backend/src/main/resources/application.yml` (`spring.servlet.multipart.max-file-size: 10MB`).

## 3. Organização do front-end

Sem Vuex e sem Vue Router: uma tela, e o estado cabe no componente raiz.

```text
frontend/
├── public/index.html
├── src/
│   ├── main.js
│   ├── App.vue
│   ├── assets/styles/   variables.css, base.css
│   ├── components/      FileDropzone.vue, FormatSelector.vue, ResultPanel.vue
│   ├── services/        conversionApi.js
│   └── utils/           download.js, fileFormat.js
├── tests/unit/          App.spec.js, conversionApi.spec.js, fileFormat.spec.js,
│                        FileDropzone.spec.js, FormatSelector.spec.js
├── .env.development     VUE_APP_API_URL=http://localhost:8080
├── package.json         dependências e scripts (serve, build, test:unit)
├── vue.config.js        servidor de desenvolvimento na porta 8081
├── jest.config.js       preset do Vue CLI; Axios resolvido para o build CommonJS
└── babel.config.js
```

| Arquivo | Responsabilidade |
| --- | --- |
| `src/App.vue` | Estado da tela (`file`, `targetFormat`, `conversions`, `loadingFormats`, `formatsError`, `loading`, `result`, `error`) e orquestração; chama `getFormats()` ao abrir (UC02) |
| `src/components/FileDropzone.vue` | Clique/arrastar (`<label>` ligado ao `<input type="file">`), valida extensão, tamanho e arquivo vazio; emite `file-selected` (arquivo, formato) ou `error` (mensagem) |
| `src/components/FormatSelector.vue` | Recebe `options` (só os destinos válidos da origem) via prop, mostra PDF / Word / Excel como botões de rádio e emite `input` (funciona com `v-model`) |
| `src/components/ResultPanel.vue` | Mostra sucesso ou erro, emite `download` e `reset` |
| `src/services/conversionApi.js` | Axios com `baseURL` de `VUE_APP_API_URL`; `getFormats()` e `convert(file, target)` |
| `src/utils/download.js` | Converte base64 em `Blob` e dispara o download via `URL.createObjectURL` |
| `src/utils/fileFormat.js` | Mapa extensão → formato, limite de 10 MB (espelhando o back-end), `validateFile`, `formatFileSize` e `targetFormatsFor` |
| `src/assets/styles/variables.css` | Design tokens (cores, tipografia, espaçamento) de [`interface.md`](interface.md) |
| `src/assets/styles/base.css` | Reset leve, fonte Inter, cor de fundo e foco visível global |

## 4. Comunicação entre front-end e back-end

- HTTP/JSON via Axios; upload em `multipart/form-data`; contrato na seção 5.
- Em desenvolvimento: back em `localhost:8080`, front em `localhost:8081`.
- CORS liberado no back-end (`CorsConfig`) apenas para a origem do front, configurável por `app.cors.allowed-origin`.
- Validação no front é conveniência; a validação que vale é a do back-end.

## 5. Contrato da API, DTOs e mapper

Duas rotas, ambas sob `/api/v1/conversions`. Erros sempre no mesmo formato `ErrorResponse`.

| Método | Rota | Entrada | Saída (sucesso) | Erros |
| --- | --- | --- | --- | --- |
| GET | `/api/v1/conversions/formats` | — | 200 `List<SupportedConversionResponse>` | 500 |
| POST | `/api/v1/conversions` | `multipart/form-data`: `file` + `targetFormat` | 200 `ConversionResponse` | 400, 413, 415, 422, 500 |

### Exemplos

```http
GET /api/v1/conversions/formats

200 OK
[
  { "sourceFormat": "DOCX", "targetFormats": ["PDF", "XLSX"] },
  { "sourceFormat": "PDF",  "targetFormats": ["DOCX", "XLSX"] },
  { "sourceFormat": "XLSX", "targetFormats": ["PDF", "DOCX"] }
]
```

```http
POST /api/v1/conversions
Content-Type: multipart/form-data

file=@relatorio.docx
targetFormat=PDF

200 OK
{
  "fileName": "relatorio.pdf",
  "contentType": "application/pdf",
  "sourceFormat": "DOCX",
  "targetFormat": "PDF",
  "sizeBytes": 48213,
  "durationMs": 312,
  "contentBase64": "JVBERi0xLjcK..."
}
```

```http
422 Unprocessable Entity
{
  "code": "CORRUPTED_FILE",
  "message": "O arquivo está corrompido ou não pôde ser lido.",
  "path": "/api/v1/conversions",
  "timestamp": "2026-11-24T21:40:00Z"
}
```

### DTOs (Java 17 records)

```java
// Request: montado no controller a partir do multipart
public record ConversionRequest(
        @NotNull MultipartFile file,
        @NotNull FileFormat targetFormat) {}

public record ConversionResponse(
        String fileName, String contentType,
        FileFormat sourceFormat, FileFormat targetFormat,
        long sizeBytes, long durationMs, String contentBase64) {}

public record SupportedConversionResponse(
        FileFormat sourceFormat, Set<FileFormat> targetFormats) {}

public record ErrorResponse(
        String code, String message, String path, Instant timestamp) {}
```

### Modelo de domínio

```java
public enum FileFormat {
    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    // extension(), mimeType(), fromExtension(String)
}

public record SourceDocument(String baseName, FileFormat format, byte[] content) {}

public record ConvertedDocument(String baseName, FileFormat format, byte[] content, Duration duration) {}

public interface DocumentConverter {
    FileFormat source();
    FileFormat target();
    byte[] convert(byte[] input) throws ConversionException;
}
```

### Mapper

`ConversionMapper` (um `@Component` escrito à mão) traduz entre as camadas. Foi escolhido mapper manual em vez de MapStruct porque há lógica (detectar formato, sanitizar nome, base64) e só três DTOs; MapStruct fica como evolução.

| Método | De → Para | O que faz |
| --- | --- | --- |
| `toDomain(ConversionRequest)` | DTO → `SourceDocument` | Lê bytes, sanitiza nome, resolve formato pela extensão (extensão desconhecida lança `InvalidFileException` com `UNSUPPORTED_FILE_TYPE`) |
| `toResponse(ConvertedDocument, FileFormat source)` | Domínio → `ConversionResponse` | Monta nome final, MIME, tamanho, duração e base64 |
| `toResponse(FileFormat, Set<FileFormat>)` | Domínio → `SupportedConversionResponse` | Lista de destinos por origem, na ordem do enum |

Sanitização do nome (RN08): remove o caminho (`/` e `\`) e a extensão, mantém só letras, números, `-`, `_` e espaço, e corta em 100 caracteres. Se nada sobrar, usa `arquivo`. Exemplo: `../../etc/passwd.docx` → `passwd`.

Trade-off do base64: aumenta o payload em ~33% (10 MB viram ~13,3 MB), mas mantém o padrão de DTO de resposta em JSON e facilita o tratamento uniforme no front. Alternativa: devolver `byte[]` com `Content-Disposition` — mais eficiente, sem DTO de resposta.

## 6. Padrão de nomenclatura

| Item | Padrão | Exemplo |
| --- | --- | --- |
| Pastas da raiz e de documentação | minúsculas, kebab-case | `documentacao/casos-uso.md` |
| Pacotes Java | minúsculas, singular, por camada | `br.edu.fileconverter.converter` |
| Classes Java | PascalCase + sufixo do papel | `ConversionController`, `ConversionService`, `ConversionMapper` |
| DTOs | sufixo `Request` / `Response` | `ConversionRequest`, `ErrorResponse` |
| Conversores | `<Origem>To<Destino>Converter` | `DocxToPdfConverter` |
| Exceções | sufixo `Exception` | `UnsupportedConversionException` |
| Métodos e variáveis | camelCase, em inglês | `toResponse`, `targetFormat` |
| Constantes e enums | UPPER_SNAKE_CASE | `MAX_FILE_SIZE`, `FileFormat.DOCX` |
| Testes Java | `<Classe>Test` | `DocxToPdfConverterTest` |
| Componentes Vue | PascalCase, duas+ palavras | `FileDropzone.vue` |
| Serviços e utilitários JS | camelCase | `conversionApi.js` |
| Testes JS | `<Componente>.spec.js` | `FormatSelector.spec.js` |
| Eventos Vue | kebab-case | `file-selected` |
| Rotas REST | `/api/v1/` + substantivo no plural, kebab-case | `/api/v1/conversions/formats` |
| Commits | Conventional Commits, em inglês | `feat(backend): add docx to pdf converter` |
| Branches | `tipo/descricao-curta` | `feat/pdf-to-xlsx` |

## 7. Tratamento de erros

Um `@RestControllerAdvice` (`GlobalExceptionHandler`) traduz cada exceção em `ErrorResponse`. Stack trace só no log, nunca na resposta.

| Situação | Exceção | HTTP | `code` | Mensagem ao usuário |
| --- | --- | --- | --- | --- |
| Campo `file` ou `targetFormat` ausente / destino inválido | `MissingServletRequestPartException`, `MethodArgumentTypeMismatchException` | 400 | `INVALID_REQUEST` | Requisição inválida |
| Arquivo vazio | `InvalidFileException` | 400 | `EMPTY_FILE` | O arquivo está vazio |
| Origem igual ao destino ou par inexistente | `UnsupportedConversionException` | 400 | `UNSUPPORTED_CONVERSION` | Conversão não suportada |
| Acima de 10 MB | `MaxUploadSizeExceededException` | 413 | `FILE_TOO_LARGE` | Arquivo maior que 10 MB |
| Extensão ou assinatura não suportada / divergente | `InvalidFileException` | 415 | `UNSUPPORTED_FILE_TYPE` | Use PDF, DOCX ou XLSX |
| Arquivo ilegível | `ConversionException` (causa `IOException`, `NotOfficeXmlFileException`) | 422 | `CORRUPTED_FILE` | Arquivo corrompido |
| PDF com senha | `ConversionException` (causa `InvalidPasswordException`) | 422 | `PROTECTED_FILE` | PDF protegido por senha |
| PDF sem texto / planilha vazia | `ConversionException` | 422 | `NO_CONTENT` | Nada para converter |
| Qualquer outro | `Exception` | 500 | `INTERNAL_ERROR` | Erro inesperado, tente novamente |
