# Casos de uso — FileConverter

Atores, casos de uso, pré e pós-condições, fluxo principal, fluxos alternativos e de exceção. Os requisitos citados (RF) estão em [`requisitos.md`](requisitos.md); as rotas e códigos de erro, em [`arquitetura.md`](arquitetura.md).

## 1. Atores

**Ator único: Usuário** — qualquer pessoa que acessa a página, sem identificação.

## 2. Lista de casos de uso

| ID | Caso de uso | Requisitos |
| --- | --- | --- |
| UC01 | Converter arquivo | RF01–RF09 |
| UC02 | Consultar formatos suportados | RF04, RF11 |
| UC03 | Limpar seleção | RF10 |

## 3. UC01 — Converter arquivo

**Ator:** Usuário.

**Pré-condição:** página carregada e lista de conversões obtida (UC02).

**Pós-condição:** usuário recebe o arquivo convertido; nada fica armazenado no servidor.

### Fluxo principal

1. O usuário arrasta um arquivo para a área de upload ou clica e escolhe um arquivo.
2. O sistema valida a extensão e o tamanho no navegador.
3. O sistema exibe nome, tamanho e tipo detectado.
4. O sistema exibe os formatos de destino válidos (ex.: para DOCX, mostra PDF e Excel).
5. O usuário escolhe o formato de destino e clica em “Converter”.
6. O sistema envia o arquivo e o destino para `POST /api/v1/conversions` e mostra o indicador de progresso.
7. O back-end valida a assinatura, converte e devolve o arquivo em base64 com metadados.
8. O front-end gera o download automaticamente e mostra “Conversão concluída” com botão “Baixar novamente”.

### Fluxos alternativos

- **A1 — Extensão não suportada (passo 2):** o sistema exibe “Formato não suportado. Use PDF, DOCX ou XLSX.” e não habilita o botão.
- **A2 — Arquivo acima de 10 MB (passo 2):** o sistema exibe “Arquivo maior que 10 MB.”.
- **A3 — Usuário troca o arquivo (passo 5):** volta ao passo 2 e limpa o destino escolhido.

### Fluxos de exceção

- **E1 — Assinatura não confere (passo 7):** retorna 415 com código `UNSUPPORTED_FILE_TYPE`.
- **E2 — Arquivo corrompido ou PDF protegido (passo 7):** retorna 422 com `CORRUPTED_FILE` ou `PROTECTED_FILE`.
- **E3 — Conversão não suportada (passo 7):** retorna 400 com `UNSUPPORTED_CONVERSION`.
- **E4 — Erro inesperado (passo 7):** retorna 500 com `INTERNAL_ERROR` e mensagem genérica, sem stack trace.
- **E5 — Servidor indisponível (passo 6):** o front-end exibe “Servidor indisponível. Tente novamente.”.

Nos fluxos E1 a E4 o front-end mostra a `message` do `ErrorResponse` no alerta de erro.

## 4. UC02 — Consultar formatos suportados

**Ator:** Usuário (indireto: a tela dispara a consulta ao abrir).

**Pré-condição:** back-end em execução e acessível pela URL configurada em `VUE_APP_API_URL`.

**Pós-condição:** front-end tem em memória a lista de pares origem → destinos.

### Fluxo principal

1. O usuário abre a página.
2. O front-end chama `GET /api/v1/conversions/formats`.
3. O back-end devolve a lista de `SupportedConversionResponse` a partir dos conversores registrados.
4. O front-end guarda a lista e habilita a área de upload.

### Fluxos alternativos

- **A1 — Resposta lenta (passo 2):** a área de upload mostra “Carregando formatos…” até a resposta chegar.

### Fluxos de exceção

- **E1 — Back-end indisponível ou erro 500 (passo 3):** o sistema exibe “Não foi possível carregar os formatos. Recarregue a página.” e mantém o upload desabilitado.

## 5. UC03 — Limpar seleção

**Ator:** Usuário.

**Pré-condição:** existe um arquivo selecionado ou uma conversão concluída/com erro na tela.

**Pós-condição:** tela no estado inicial; o arquivo convertido anterior não fica acessível.

### Fluxo principal

1. O usuário clica em “Nova conversão” (ou em “Remover” no cartão do arquivo).
2. O sistema limpa arquivo, destino, mensagens e resultado.
3. O sistema libera a URL do download anterior (`URL.revokeObjectURL`).
4. O sistema volta ao estado inicial com a área de upload vazia.

### Fluxos alternativos

- **A1 — Conversão em andamento (passo 1):** os botões “Remover” e “Nova conversão” ficam desabilitados até a resposta chegar.

### Fluxos de exceção

Não há; a ação é local e não chama o back-end.
