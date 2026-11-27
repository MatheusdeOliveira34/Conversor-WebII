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

Limite de upload configurado em `backend/src/main/resources/application.yml` (`spring.servlet.multipart.max-file-size: 10MB`). O `file-size-threshold` fica em 11 MB, acima do limite: assim o Tomcat nunca grava o upload em arquivo temporário e todo arquivo aceito fica só em memória (RNF04, RNF07).

### Detecção de formato e registro de conversores

- **`FileTypeDetector`** decide o formato pelo conteúdo (RF03, RN02): PDF começa com `%PDF-`; DOCX e XLSX são ZIP (`PK\3\4`) e se diferenciam pela entrada `word/document.xml` ou `xl/workbook.xml`. Os nomes das entradas ficam sem compressão dentro do ZIP, então a busca é feita nos bytes crus, sem descompactar. Arquivo vazio lança `InvalidFileException` com `EMPTY_FILE`; formato desconhecido ou diferente da extensão lança `UNSUPPORTED_FILE_TYPE`.
- **`ConverterRegistry`** recebe do Spring todos os beans `DocumentConverter` e os indexa por origem → destino. `find(source, target)` lança `UnsupportedConversionException` quando origem e destino são iguais ou o par não tem conversor; dois conversores para o mesmo par impedem a aplicação de subir.
- **`ConversionService`** é a única porta da camada de serviço para o controller; `supportedConversions()` alimenta o `GET /formats`.

### Fluxo do `POST /api/v1/conversions`

1. `ConversionController` recebe `file` e `targetFormat` do multipart e monta o `ConversionRequest`.
2. `ConversionMapper.toDomain` sanitiza o nome, resolve o formato pela extensão e gera o `SourceDocument`.
3. `ConversionService.convert` chama `FileTypeDetector.validate` (conteúdo × extensão), pega o conversor no `ConverterRegistry`, converte e mede a duração, devolvendo um `ConvertedDocument`.
4. `ConversionMapper.toResponse` monta o `ConversionResponse` com nome final, MIME, tamanho, duração e base64.

Cada conversor lança `ConversionException` com `CORRUPTED_FILE` quando não consegue ler a entrada e `NO_CONTENT` quando não há texto nem tabela para converter. O `DocxToPdfConverter` (C1) percorre parágrafos e tabelas do Word na ordem em que aparecem e os escreve em PDF A4 com OpenPDF.

Os conversores que partem de PDF (C3 e C4) usam a classe interna `PdfTextReader`, que abre o PDF com PDFBox, extrai o texto página por página (ordenado pela posição na página) e traduz as falhas: PDF com senha → `PROTECTED_FILE` (RN04), PDF sem texto extraível → `NO_CONTENT` (RN05), PDF ilegível → `CORRUPTED_FILE`.

- **`PdfToDocxConverter` (C3):** um parágrafo por linha; a primeira linha de cada página, a partir da segunda, começa com quebra de página.
- **`PdfToXlsxConverter` (C4):** uma aba chamada `PDF`; cada linha não vazia vira uma linha da planilha, com as colunas separadas por tab ou por dois ou mais espaços seguidos. Os valores são gravados como texto.

- **`DocxToXlsxConverter` (C2):** o texto solto do Word vai para a aba `Texto`, um parágrafo por linha; cada tabela vira uma aba própria (`Tabela 1`, `Tabela 2`...). Parágrafos e tabelas vazios são ignorados; sem nenhum dos dois, `NO_CONTENT`.

Os conversores que partem de XLSX (C5 e C6) usam a classe interna `SpreadsheetReader`, que abre a planilha com POI e devolve cada aba como uma tabela de textos:

- valores formatados como o Excel exibe, em pt-BR (ex.: `2,5`); células com fórmula exportam o valor calculado, nunca a fórmula (RN06). Se o POI não souber calcular alguma função, usa o último resultado salvo pelo Excel;
- linhas sem nenhum valor e colunas vazias nas bordas são descartadas; abas vazias são ignoradas e, se todas forem vazias, a conversão falha com `NO_CONTENT` (“Planilha sem dados”, RN07);
- arquivo ilegível → `CORRUPTED_FILE`.

- **`XlsxToPdfConverter` (C5):** uma página por aba, com o nome da aba em negrito como título e os dados numa tabela. A primeira linha se repete quando a tabela quebra de página; com 7 colunas ou mais a página fica deitada (A4 paisagem).
- **`XlsxToDocxConverter` (C6):** para cada aba, um título em negrito com o nome da aba seguido de uma tabela do Word com os dados.

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
├── tests/unit/          App.spec.js, conversionApi.spec.js, fileFormat.spec.js, download.spec.js,
│                        FileDropzone.spec.js, FormatSelector.spec.js, ResultPanel.spec.js
├── .env.development     VUE_APP_API_URL=http://localhost:8080
├── package.json         dependências e scripts (serve, build, test:unit)
├── vue.config.js        servidor de desenvolvimento na porta 8081
├── jest.config.js       preset do Vue CLI; Axios resolvido para o build CommonJS
└── babel.config.js
```

| Arquivo | Responsabilidade |
| --- | --- |
| `src/App.vue` | Estado da tela (`file`, `targetFormat`, `conversions`, `loadingFormats`, `formatsError`, `loading`, `result`, `error`, `downloadUrl`) e orquestração; chama `getFormats()` ao abrir (UC02) e `convert()` no botão "Converter"; `reset()` volta ao estado inicial (UC03) e libera a URL do download anterior. Mensagens ficam numa região `aria-live="polite"` |
| `src/components/FileDropzone.vue` | Clique/arrastar (`<label>` ligado ao `<input type="file">`), valida extensão, tamanho e arquivo vazio; emite `file-selected` (arquivo, formato) ou `error` (mensagem) |
| `src/components/FormatSelector.vue` | Recebe `options` (só os destinos válidos da origem) via prop, mostra PDF / Word / Excel como botões de rádio e emite `input` (funciona com `v-model`) |
| `src/components/ResultPanel.vue` | Mostra sucesso (nome, tamanho, duração) ou a mensagem de erro; emite `download` ("Baixar novamente"), `retry` ("Tentar novamente") e `reset` ("Nova conversão"); botões desabilitados pela prop `disabled` |
| `src/services/conversionApi.js` | Axios com `baseURL` de `VUE_APP_API_URL`; `getFormats()`, `convert(file, target)` e `errorMessage(error)`, que usa o `message` do `ErrorResponse` (ou "Servidor indisponível. Tente novamente." sem resposta) |
| `src/utils/download.js` | Converte base64 em `Blob`, cria a URL com `URL.createObjectURL` e dispara o download; a URL fica guardada para "Baixar novamente" e é liberada com `URL.revokeObjectURL` (`revokeDownloadUrl`) na nova conversão |
| `src/utils/fileFormat.js` | Mapa extensão → formato, limite de 10 MB (espelhando o back-end), `validateFile`, `formatFileSize` e `targetFormatsFor` |
| `src/assets/styles/variables.css` | Design tokens (cores, tipografia, espaçamento) de [`interface.md`](interface.md) |
| `src/assets/styles/base.css` | Reset leve, fonte Inter, cor de fundo, foco visível global, `.sr-only` e botões compartilhados (`.btn--primary`, `.btn--outline`) |

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
  { "sourceFormat": "PDF",  "targetFormats": ["DOCX", "XLSX"] },
  { "sourceFormat": "DOCX", "targetFormats": ["PDF", "XLSX"] },
  { "sourceFormat": "XLSX", "targetFormats": ["PDF", "DOCX"] }
]
```

A lista sai do `ConverterRegistry`, sempre na ordem do enum `FileFormat` (PDF, DOCX, XLSX), e só traz os pares que têm conversor registrado.

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
| Campo `file` ou `targetFormat` ausente / destino inválido / requisição que não é multipart | `MissingServletRequestPartException`, `MissingServletRequestParameterException`, `MethodArgumentTypeMismatchException`, `HttpMediaTypeNotSupportedException`, `MultipartException` | 400 | `INVALID_REQUEST` | Requisição inválida |
| Arquivo vazio | `InvalidFileException` | 400 | `EMPTY_FILE` | O arquivo está vazio |
| Origem igual ao destino ou par inexistente | `UnsupportedConversionException` | 400 | `UNSUPPORTED_CONVERSION` | Conversão não suportada |
| Acima de 10 MB | `MaxUploadSizeExceededException` | 413 | `FILE_TOO_LARGE` | Arquivo maior que 10 MB |
| Extensão ou assinatura não suportada / divergente | `InvalidFileException` | 415 | `UNSUPPORTED_FILE_TYPE` | Use PDF, DOCX ou XLSX |
| Arquivo ilegível | `ConversionException` (causa `IOException`, `NotOfficeXmlFileException`) | 422 | `CORRUPTED_FILE` | Arquivo corrompido |
| PDF com senha | `ConversionException` (causa `InvalidPasswordException`) | 422 | `PROTECTED_FILE` | PDF protegido por senha |
| PDF sem texto / planilha vazia | `ConversionException` | 422 | `NO_CONTENT` | Nada para converter |
| Rota inexistente | `NoResourceFoundException` | 404 | `NOT_FOUND` | Rota não encontrada |
| Método HTTP errado (ex.: `GET /api/v1/conversions`) | `HttpRequestMethodNotSupportedException` | 405 | `METHOD_NOT_ALLOWED` | Método não permitido |
| Qualquer outro | `Exception` | 500 | `INTERNAL_ERROR` | Erro inesperado, tente novamente |

Nas exceções de domínio (`InvalidFileException`, `UnsupportedConversionException`, `ConversionException`), a mensagem da resposta é a da própria exceção, já escrita em português para o usuário (ex.: “PDF sem texto. Arquivos escaneados (imagem) não podem ser convertidos.”). Nas exceções do Spring, a mensagem é fixa. Falhas de conversão (422) são registradas no log como aviso; erros 500 são registrados com o stack trace.

O limite de 10 MB só é aplicado pelo Tomcat ao ler o multipart. Por isso `server.tomcat.max-swallow-size` está em 20 MB: o servidor lê o resto do upload grande demais e consegue entregar o 413 ao navegador, em vez de cortar a conexão.

### Segurança e observabilidade

- **Zip bomb (RNF08):** `PoiSecurityConfig` configura o `ZipSecureFile` do POI na subida da aplicação. Nenhuma entrada do DOCX/XLSX pode descompactar para mais de 100 vezes o seu tamanho compactado nem passar de 100 MB. Um arquivo que viole isso falha na leitura e volta como `CORRUPTED_FILE`.
- **Log de conversões (RNF12):** `ConversionService` registra cada conversão concluída com origem, destino, tamanho de entrada e de saída e duração, por exemplo `Conversão DOCX → PDF: 48213 bytes → 31077 bytes em 312 ms`. O nome e o conteúdo do arquivo não aparecem no log.
