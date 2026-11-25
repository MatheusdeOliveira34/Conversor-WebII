# Requisitos — FileConverter

Requisitos funcionais, não funcionais e regras de negócio. As conversões citadas (C1 a C6) estão na matriz de [`visao.md`](visao.md); os casos de uso, em [`casos-uso.md`](casos-uso.md).

Prioridades: **Alta** (sem isso o sistema não cumpre o objetivo), **Média** (importante para qualidade ou usabilidade), **Baixa** (desejável).

## 1. Requisitos funcionais

| ID | Requisito | Prioridade |
| --- | --- | --- |
| RF01 | O sistema deve permitir selecionar um arquivo por clique ou arrastar e soltar | Alta |
| RF02 | O sistema deve aceitar apenas arquivos .pdf, .docx e .xlsx | Alta |
| RF03 | O sistema deve identificar o formato de origem pela extensão e pela assinatura do arquivo (magic bytes) | Alta |
| RF04 | O sistema deve exibir somente os formatos de destino válidos para a origem detectada | Alta |
| RF05 | O sistema deve converter o arquivo para o formato escolhido conforme a matriz de conversões | Alta |
| RF06 | O sistema deve disponibilizar o arquivo convertido para download com nome `<nome-original>.<nova-extensão>` | Alta |
| RF07 | O sistema deve exibir nome, tamanho e tipo do arquivo selecionado antes da conversão | Média |
| RF08 | O sistema deve exibir indicador de progresso durante a conversão e bloquear novo envio até terminar | Média |
| RF09 | O sistema deve exibir mensagens de erro compreensíveis (tipo inválido, arquivo grande, arquivo corrompido, conversão não suportada) | Alta |
| RF10 | O sistema deve permitir limpar a seleção e iniciar nova conversão | Baixa |
| RF11 | O back-end deve expor um endpoint que lista as conversões suportadas, usado pelo front-end para montar as opções | Média |

## 2. Requisitos não funcionais

| ID | Categoria | Requisito | Prioridade |
| --- | --- | --- | --- |
| RNF01 | Tecnologia | Front-end em Vue 2.7 (Options API), Axios e CSS simples; build com Vue CLI 5 | Alta |
| RNF02 | Tecnologia | Back-end em Java 17 e Spring Boot 3.x expondo API REST, build Maven | Alta |
| RNF03 | Arquitetura | Camadas Controller → Service → Converter, com DTOs na fronteira e mapper entre DTO e domínio | Alta |
| RNF04 | Persistência | Nenhum banco de dados; arquivos processados apenas em memória e descartados após a resposta | Alta |
| RNF05 | Desempenho | Arquivo de até 2 MB convertido em menos de 5 s em máquina de desenvolvimento | Média |
| RNF06 | Limites | Tamanho máximo de upload: 10 MB (configurável em `application.yml`) | Alta |
| RNF07 | Segurança | Validação de extensão + magic bytes; nome de arquivo sanitizado; sem gravar em disco; CORS restrito à origem do front | Alta |
| RNF08 | Segurança | Proteção contra zip bomb em DOCX/XLSX via `ZipSecureFile` do POI | Média |
| RNF09 | Usabilidade | Uma única tela, utilizável em desktop e celular (layout responsivo), seguindo [`interface.md`](interface.md) | Média |
| RNF10 | Manutenibilidade | Novo par de conversão adicionado criando uma classe que implementa `DocumentConverter`, sem alterar controller ou service | Média |
| RNF11 | Qualidade | Testes unitários para cada conversor e mapper; teste de integração do endpoint com MockMvc | Média |
| RNF12 | Observabilidade | Log de cada conversão (formato origem/destino, tamanho, duração) sem registrar o conteúdo | Baixa |
| RNF13 | Versionamento | Código em um único repositório Git com `backend/`, `frontend/` e `documentacao/`; 6 commits de cada integrante distribuídos ao longo do desenvolvimento | Alta |

## 3. Regras de negócio

| ID | Regra |
| --- | --- |
| RN01 | Formato de destino deve ser diferente do formato de origem |
| RN02 | O formato de origem é decidido pelo conteúdo do arquivo; se extensão e assinatura divergirem, o arquivo é rejeitado |
| RN03 | Arquivo vazio (0 bytes) é rejeitado |
| RN04 | PDF protegido por senha é rejeitado com mensagem específica |
| RN05 | PDF sem texto extraível (escaneado) gera erro “PDF sem texto” em vez de arquivo vazio |
| RN06 | Em XLSX → PDF/DOCX, células com fórmula exportam o valor calculado, formatado como o Excel exibe |
| RN07 | Abas de planilha vazias são ignoradas; se todas forem vazias, a conversão falha com “planilha sem dados” |
| RN08 | O nome do arquivo de saída mantém o nome original (sanitizado: só letras, números, `-`, `_` e espaço, até 100 caracteres) e troca a extensão |
