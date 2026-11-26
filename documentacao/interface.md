# Interface — FileConverter

Paleta, tipografia, organização da tela e diretrizes de UI. A organização do código do front-end (componentes, serviços e utilitários) está em [`arquitetura.md`](arquitetura.md), seção 3.

Identidade limpa e utilitária: fundo claro, um azul de ação e uma cor por formato de arquivo para o usuário reconhecer o tipo de relance.

## 1. Paleta de cores

Definida como variáveis CSS em `frontend/src/assets/styles/variables.css`. Todos os pares texto/fundo atendem contraste WCAG AA (≥ 4,5:1).

| Token | Hex | Uso |
| --- | --- | --- |
| `--color-primary` | #2563EB | Botão “Converter”, foco, seleção do formato |
| `--color-primary-hover` | #1D4ED8 | Hover e estado pressionado |
| `--color-bg` | #F8FAFC | Fundo da página |
| `--color-surface` | #FFFFFF | Cartão central, área de upload |
| `--color-border` | #CBD5E1 | Bordas, tracejado da área de upload |
| `--color-text` | #0F172A | Texto principal |
| `--color-text-muted` | #475569 | Textos auxiliares (tamanho, dicas) |
| `--color-success` | #15803D | Alerta de conversão concluída |
| `--color-error` | #B91C1C | Alertas de erro e validação |
| `--color-pdf` | #DC2626 | Ícone e etiqueta PDF |
| `--color-docx` | #1E40AF | Ícone e etiqueta Word |
| `--color-xlsx` | #15803D | Ícone e etiqueta Excel |

## 2. Tipografia

Fonte **Inter**, instalada pelo pacote npm `@fontsource/inter` (sem chamada a servidor externo), com fallback `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`.

| Elemento | Tamanho | Peso |
| --- | --- | --- |
| Título “FileConverter” | 28 px | 700 |
| Subtítulo | 16 px | 400 |
| Corpo e botões | 16 px | 500 (botões) / 400 |
| Rótulos (“Converter para”) | 14 px | 600 |
| Texto auxiliar (tamanho do arquivo, dicas) | 13 px | 400 |

## 3. Organização da tela

Uma tela única, com um cartão central de até 560 px de largura:

```text
+------------------------------------------+
|  FileConverter                           |
|  PDF • Word • Excel                      |
|                                          |
|  +- - - - - - - - - - - - - - - - - - +  |
|  |  Arraste o arquivo aqui            |  |
|  |  ou clique para escolher           |  |
|  |  PDF, DOCX ou XLSX até 10 MB       |  |
|  +- - - - - - - - - - - - - - - - - - +  |
|                                          |
|  [DOCX] relatorio.docx  48 KB  Remover   |
|                                          |
|  Converter para:  ( PDF )  ( Excel )     |
|                                          |
|  [           Converter           ]       |
|                                          |
|  Conversão concluída. Baixar novamente   |
|  Nova conversão                          |
+------------------------------------------+
```

### Estados da tela

| Estado | O que aparece |
| --- | --- |
| Inicial | Área de upload; seletor e botão ocultos |
| Arquivo selecionado | Cartão do arquivo, seletor com destinos válidos, botão desabilitado até escolher destino |
| Convertendo | Spinner no botão, texto “Convertendo…”, controles desabilitados |
| Sucesso | Alerta verde com “Baixar novamente” e “Nova conversão” |
| Erro | Alerta vermelho com a mensagem do erro (tabela de erros em [`arquitetura.md`](arquitetura.md), seção 7) e “Tentar novamente” |

## 4. Diretrizes de UI

- Espaçamento em múltiplos de 8 px; cantos arredondados de 8 px; sombra leve apenas no cartão central.
- Uma única ação principal por estado, sempre o botão azul preenchido; ações secundárias como link ou botão contornado.
- Responsivo: abaixo de 600 px o cartão ocupa a largura toda com margem de 16 px e os botões de formato empilham.
- Acessibilidade: tudo operável por teclado (a área de upload é um `<label>` ligado ao `<input type="file">`), foco visível com contorno azul de 2 px, mensagens em região `aria-live="polite"`.
- Cor nunca é o único sinal: etiquetas de formato têm texto (PDF, DOCX, XLSX) e alertas têm ícone e texto.
- Textos em português, curtos e no imperativo (“Converter”, “Baixar novamente”); erros dizem o que aconteceu e o que fazer.
- Aviso fixo sob o seletor: “A conversão leva texto e tabelas; imagens e formatação podem se perder.”
