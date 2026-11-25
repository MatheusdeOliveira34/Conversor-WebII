# FileConverter

Aplicação web de página única que converte arquivos entre PDF, Word (.docx) e Excel (.xlsx), sem login e sem banco de dados: o arquivo entra, é convertido em memória e volta para o usuário.

- **Back-end:** Java 17 + Spring Boot 3 (API REST), pasta [`backend/`](backend/)
- **Front-end:** Vue 2.7 + Axios + Vue CLI 5, pasta [`frontend/`](frontend/)
- **Documentação:** pasta [`documentacao/`](documentacao/)

**Equipe:** \[Nome 1\] e \[Nome 2\] — a preencher.

## Estrutura

```text
fileconverter/
├── documentacao/   visao.md, requisitos.md, casos-uso.md, arquitetura.md, interface.md
├── backend/        projeto Maven Spring Boot (pom.xml, src/main/java, src/test/java)
├── frontend/       projeto Vue CLI (package.json, src/, tests/unit/)
├── .gitignore
└── README.md
```

## Pré-requisitos

- JDK 17 (com `JAVA_HOME` apontando para ele). O Maven não precisa estar instalado: o projeto usa o Maven Wrapper (`mvnw`).
- Node.js 18 ou superior e npm.

## Como rodar

### Back-end (porta 8080)

```bash
cd backend
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

A API fica em `http://localhost:8080/api/v1/conversions`.

### Front-end (porta 8081)

```bash
cd frontend
npm install
npm run serve
```

A tela fica em `http://localhost:8081`. A URL da API é lida de `VUE_APP_API_URL` em `frontend/.env.development`.

## Testes

```bash
cd backend && ./mvnw test           # JUnit 5, AssertJ, MockMvc
cd frontend && npm run test:unit    # Jest, Vue Test Utils 1
```

## Documentação

| Arquivo | Conteúdo |
| --- | --- |
| [`visao.md`](documentacao/visao.md) | Problema, público-alvo, proposta de valor e matriz de conversões |
| [`requisitos.md`](documentacao/requisitos.md) | Requisitos funcionais, não funcionais e regras de negócio |
| [`casos-uso.md`](documentacao/casos-uso.md) | Atores e casos de uso |
| [`arquitetura.md`](documentacao/arquitetura.md) | Back-end, front-end, contrato da API, nomes e tratamento de erros |
| [`interface.md`](documentacao/interface.md) | Paleta, tipografia, organização da tela e diretrizes de UI |

## Plano de commits

12 commits, 6 de cada integrante, de quarta 25/11 a sexta 27/11. Cada commit entrega uma parte que compila e passa nos testes, em uma branch própria com Pull Request para `main`.

| Turno | Integrante A (back-end) | Integrante B (front-end) |
| --- | --- | --- |
| Qua · manhã | A1 — repositório + requisitos e arquitetura | B1 — visão, casos de uso e interface |
| Qua · tarde | A2 — esqueleto da API, DTOs e mapper | B2 — esqueleto Vue, estilos e layout |
| Qua · noite | A3 — detector, registry e `GET /formats` | B3 — upload e seleção de formato |
| Qui · manhã | A4 — DOCX → PDF e `POST /conversions` | B4 — enviar, baixar e mostrar resultado |
| Qui · tarde | A5 — conversores PDF → DOCX e PDF → XLSX | B5 — conversores XLSX → PDF, XLSX → DOCX e DOCX → XLSX |
| Sex · manhã | A6 — tratamento de erros global | B6 — nova conversão, responsivo e acessibilidade |
