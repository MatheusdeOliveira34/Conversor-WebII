# Visão — FileConverter

Problema, público-alvo, proposta de valor, escopo e matriz de conversões. Os requisitos estão em [`requisitos.md`](requisitos.md) e os casos de uso em [`casos-uso.md`](casos-uso.md).

## 1. Visão geral

O FileConverter é uma aplicação web de página única que converte arquivos entre PDF, Word (.docx) e Excel (.xlsx), sem login e sem banco de dados: o arquivo entra, é convertido em memória e volta para o usuário.

**Problema.** Quem precisa trocar o formato de um documento (entregar um relatório em PDF, levar uma tabela do Word para o Excel, editar um PDF no Word) depende de software pago instalado ou de sites com anúncios, cadastro obrigatório e retenção dos arquivos enviados.

**Para quem.** Estudantes, professores e profissionais administrativos que lidam com documentos de escritório e precisam de uma conversão rápida, eventual, sem instalar nada.

**Proposta de valor.**

- Converter em três passos (enviar, escolher formato, baixar) numa única tela.
- Nenhum cadastro e nenhum arquivo guardado: tudo é processado em memória e descartado.
- Uma ferramenta para os três formatos mais comuns, nos dois sentidos.
- Limitações ditas com clareza na tela (a conversão leva texto e tabelas, não o layout).

**Equipe (dupla).** \[Nome 1\] e \[Nome 2\] — a preencher.

**Objetivo.** Entregar, como trabalho acadêmico, um sistema simples e funcional com front-end em Vue 2 e back-end em Spring Boot, demonstrando separação em camadas, DTOs de request/response e mapeamento entre camadas.

### Escopo

**Dentro do escopo.**

- Upload de um arquivo por vez (PDF, DOCX ou XLSX).
- Escolha do formato de destino entre os outros dois formatos.
- Conversão síncrona no servidor e download do resultado.
- Validação de tipo, tamanho e combinação origem/destino.
- Mensagens de erro claras na tela.

**Fora do escopo.**

- Autenticação, cadastro de usuários e perfis.
- Persistência em banco, histórico de conversões e armazenamento em disco.
- Formatos legados (.doc, .xls), imagens, OCR de PDF escaneado.
- Conversão em lote e processamento assíncrono/fila.
- Fidelidade visual total (layout, fontes, imagens) — ver a seção 2.

### Glossário

| Termo | Significado |
| --- | --- |
| Formato de origem | Tipo do arquivo enviado (PDF, DOCX, XLSX) |
| Formato de destino | Tipo do arquivo gerado |
| DOCX | Documento Word moderno (Office Open XML, um ZIP de arquivos XML) |
| XLSX | Planilha Excel moderna (também Office Open XML). É o “Excel em XML” |
| DTO | Data Transfer Object: objeto que trafega na fronteira da API |
| Modelo de domínio | Objeto interno (`SourceDocument`, `ConvertedDocument`) usado pelo serviço |
| Mapper | Classe que traduz DTO ↔ modelo de domínio |
| Conversor | Implementação de `DocumentConverter` para um par origem → destino |

Observação: “Excel em XML” foi interpretado como .xlsx, que já é XML compactado. O formato antigo “XML Spreadsheet 2003” não entra no escopo.

## 2. Matriz de conversões

São seis conversões, todas focadas em **conteúdo (texto e tabelas)**, não em layout. Isso mantém o sistema simples e usa só bibliotecas Java livres.

| Código | Origem → Destino | Como converte | O que se perde |
| --- | --- | --- | --- |
| C1 | DOCX → PDF | Parágrafos e tabelas do Word escritos em PDF (OpenPDF) | Imagens, cabeçalho/rodapé, fontes personalizadas |
| C2 | DOCX → XLSX | Cada tabela do Word vira uma aba; texto solto vai para a aba “Texto”, um parágrafo por linha | Formatação de texto |
| C3 | PDF → DOCX | Texto extraído por página (PDFBox), um parágrafo por linha, quebra de página entre páginas | Layout, imagens, tabelas viram texto |
| C4 | PDF → XLSX | Cada linha de texto vira uma linha da planilha; colunas separadas por 2+ espaços ou tab | Estrutura real de tabela quando o PDF não tem alinhamento claro |
| C5 | XLSX → PDF | Cada aba vira uma tabela no PDF, com o nome da aba como título | Fórmulas (só o valor calculado), cores, gráficos |
| C6 | XLSX → DOCX | Cada aba vira um título + tabela no Word | Fórmulas (só o valor), gráficos, mesclagens |

Bibliotecas: Apache POI (DOCX/XLSX), Apache PDFBox 3 (leitura de PDF), OpenPDF (escrita de PDF). Todas com licença compatível com uso acadêmico e sem serviço externo.

Conversão para o mesmo formato (ex.: PDF → PDF) é rejeitada (RN01 em [`requisitos.md`](requisitos.md)).

Resumo por origem, que é o que a tela mostra no seletor de destino:

| Origem | Destinos oferecidos |
| --- | --- |
| PDF | DOCX, XLSX |
| DOCX | PDF, XLSX |
| XLSX | PDF, DOCX |
