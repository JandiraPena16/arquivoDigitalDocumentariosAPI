# Arquivo Digital de Documentários — API

**Grupo 06 | Projecto Final Multimédia 2026**  
Backend REST em Spring Boot para a plataforma de arquivo digital de documentários.

---

## Índice

1. [Requisitos do Sistema](#1-requisitos-do-sistema)
2. [Instalação dos Requisitos](#2-instalação-dos-requisitos)
3. [Configuração do Projeto](#3-configuração-do-projeto)
4. [Como Executar](#4-como-executar)
5. [Como Testar no Swagger](#5-como-testar-no-swagger)
6. [Guia de Testes por Funcionalidade](#6-guia-de-testes-por-funcionalidade)
7. [Estrutura da API](#7-estrutura-da-api)
8. [Credenciais Padrão](#8-credenciais-padrão)

---

## 1. Requisitos do Sistema

### Verificar se já tens instalado

| Ferramenta | Versão Mínima | Como verificar |
|---|---|---|
| Java JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| PostgreSQL | 14+ | `psql --version` |
| FFmpeg | qualquer | `ffmpeg -version` |

Executa cada comando no terminal. Se aparecer erro ou "command not found", segue as instruções de instalação abaixo.

---

## 2. Instalação dos Requisitos

### Java 17

**Windows:**
1. Acede a https://adoptium.net
2. Descarrega **Temurin 17 LTS** para Windows x64
3. Executa o instalador `.msi`
4. Verifica: `java -version` → deve mostrar `openjdk 17...`

**Verificação após instalar:**
```bash
java -version
# Esperado: openjdk version "17.x.x"
```

---

### Maven

**Windows:**
1. Acede a https://maven.apache.org/download.cgi
2. Descarrega o **Binary zip archive** (ex: `apache-maven-3.9.x-bin.zip`)
3. Extrai para `C:\maven`
4. Adiciona `C:\maven\bin` à variável de ambiente `PATH`
5. Verifica: `mvn -version`

**Alternativa**: Se usas IntelliJ IDEA ou VS Code com extensões Java, o Maven pode já estar incluído.

---

### PostgreSQL

**Windows:**
1. Acede a https://www.postgresql.org/download/windows/
2. Descarrega e instala o installer (versão 16 ou 15)
3. Durante a instalação:
   - **Password do superuser (postgres)**: define como `postgres` (ou outra — guarda bem!)
   - **Port**: 5432 (padrão)
   - Inclui **pgAdmin 4** e **Stack Builder**

**Verificar instalação:**
```bash
psql --version
# Esperado: psql (PostgreSQL) 16.x
```

**Criar a base de dados:**
```bash
# Abre o psql ou usa pgAdmin
psql -U postgres -c "CREATE DATABASE arquivo_digital;"
```

Ou em pgAdmin: click direito em "Databases" → "Create" → "Database..." → Nome: `arquivo_digital`

---

### FFmpeg

**Windows:**
1. Acede a https://ffmpeg.org/download.html → Windows → **BtbN's builds**
2. Descarrega `ffmpeg-master-latest-win64-gpl.zip`
3. Extrai para `C:\ffmpeg`
4. Adiciona `C:\ffmpeg\bin` à variável de ambiente `PATH`

**Verificar:**
```bash
ffmpeg -version
ffprobe -version
```

> **Nota**: Se não tiveres FFmpeg, o servidor inicia na mesma, mas o upload ficará em estado `PENDENTE` e a compressão não funcionará. Os ficheiros ainda ficam guardados e podem fazer streaming com o ficheiro original.

---

## 3. Configuração do Projeto

Abre o ficheiro `src/main/resources/application.properties` e verifica/ajusta:

```properties
# Base de dados — ajusta username e password conforme a tua instalação
spring.datasource.url=jdbc:postgresql://localhost:5432/arquivo_digital
spring.datasource.username=postgres
spring.datasource.password=postgres

# JWT — podes manter o valor padrão
arquivo.jwt.secret=3cfa76ef14937c1c0ea519f8fc057a80fcd04a7420f8e8bcd0a7567c272e007b
arquivo.jwt.expiracao-ms=86400000

# FFmpeg — se o ffmpeg não está no PATH, coloca o caminho completo
arquivo.ffmpeg.path=ffmpeg
arquivo.ffmpeg.ffprobe-path=ffprobe
# Exemplo Windows: arquivo.ffmpeg.path=C:\\ffmpeg\\bin\\ffmpeg.exe
```

---

## 4. Como Executar

### Opção A — Maven no terminal

```bash
# Na pasta raiz do projeto (onde está o pom.xml)
mvn spring-boot:run
```

### Opção B — Compilar e executar o JAR

```bash
mvn clean package -DskipTests
java -jar target/arquivo-digital-documentarios-1.0.0.jar
```

### O que acontece ao iniciar

O servidor:
1. Cria as tabelas na base de dados automaticamente (Hibernate DDL)
2. Cria as pastas `uploads/` para armazenar os ficheiros
3. Cria o utilizador admin (se não existir): `admin@arquivo.ao` / `Admin@2024`
4. Cria 8 categorias de documentários
5. Fica disponível em `http://localhost:8080`

**Log esperado:**
```
Admin criado: admin@arquivo.ao / Admin@2024
Categorias iniciais verificadas/criadas
```

---

## 5. Como Testar no Swagger

### Passo 1 — Abrir o Swagger UI

Abre o browser e acede a:
```
http://localhost:8080/swagger-ui.html
```

Verás a interface com todos os endpoints organizados por grupo:
- **Autenticação** — login, registo, sessões
- **Documentários** — upload, pesquisa, download
- **Streaming** — stream de vídeo
- **Categorias** — listagem e gestão
- **Utilizadores** — perfil e administração
- **Logs** — registo de actividade

---

### Passo 2 — Fazer Login e Obter o Token

1. Expande a secção **Autenticação**
2. Clica em `POST /api/auth/login`
3. Clica em **"Try it out"**
4. No corpo do pedido, coloca:
```json
{
  "email": "admin@arquivo.ao",
  "password": "Admin@2024"
}
```
5. Clica em **"Execute"**
6. Na resposta (HTTP 200), copia o valor do campo `"token"`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "sessaoId": "uuid-da-sessao",
  "utilizador": { ... }
}
```

---

### Passo 3 — Autorizar o Swagger com o Token

1. Clica no botão **"Authorize"** (cadeado) no topo direito da página
2. Na janela que abre, no campo **"Value"**, escreve:
```
Bearer eyJhbGciOiJIUzI1NiJ9...
```
(substitui pelo token copiado)

3. Clica em **"Authorize"** e depois **"Close"**

A partir deste momento, todos os pedidos no Swagger incluirão automaticamente o token JWT.

---

### Passo 4 — Testar endpoints protegidos

Com o token configurado, podes testar qualquer endpoint. O cadeado fechado indica que o endpoint requer autenticação.

---

## 6. Guia de Testes por Funcionalidade

### Registo de utilizador

```
POST /api/auth/registar
Body:
{
  "nome": "João Silva",
  "email": "joao@email.com",
  "password": "Senha@123"
}
```

### Upload de Documentário

O upload usa `multipart/form-data`. O Swagger mostra campos individuais:

1. No Swagger, expande `POST /api/documentarios/upload`
2. Clica "Try it out"
3. Em `ficheiro`: clica "Choose File" e seleciona o teu ficheiro de vídeo (mp4, avi, mkv, etc.)
4. Preenche os campos de texto individualmente:
   - `titulo` → ex: `Angola: Memória e Identidade`
   - `descricao` → ex: `Documentário sobre a história angolana` (opcional)
   - `ano` → ex: `2024` (opcional)
   - `categoriaId` → ex: `8` (opcional — usa `GET /api/categorias` para ver os IDs)
5. Clica **Execute** → recebes HTTP 201 com os dados do documentário

**Nota**: O status inicial será `PENDENTE`. O servidor inicia a compressão em segundo plano (FFmpeg). Verifica o progresso em `GET /api/documentarios/{id}` até o status ser `PRONTO`.

### Verificar Estado da Compressão

```
GET /api/documentarios/{id}
```
Aguarda o campo `"status"` mudar de `PENDENTE` → `PROCESSANDO` → `PRONTO`

### Relatório de Compressão

```
GET /api/documentarios/{id}/relatorio-compressao
```
Retorna:
- Tamanho original vs comprimido
- Taxa de compressão (%)
- Tempo de processamento
- Codec utilizado
- Qualidade percebida

### Streaming de Vídeo

```
GET /api/streaming/{id}
```
- **Sem Range header**: devolve o vídeo completo
- **Com Range header**: devolve o chunk pedido (HTTP 206)

No Swagger, em "Parameters", clica "Add item" no header `Range` e testa:
```
Range: bytes=0-1048575
```

### Pesquisa de Documentários

```
GET /api/documentarios/pesquisa?q=angola&categoriaId=8&ano=2024
```

### Gestão de Sessões (Segurança)

Ver sessões activas:
```
GET /api/auth/sessoes
```

Revogar sessão específica (logout forçado):
```
DELETE /api/auth/sessoes/{sessaoId}
```

Logout em todos os dispositivos:
```
DELETE /api/auth/sessoes
```

### Logs de Actividade (apenas ADMIN)

```
GET /api/logs
GET /api/logs?acao=LOGIN
GET /api/logs?utilizadorId=1
```

### Gerir Utilizadores (apenas ADMIN)

```
GET  /api/utilizadores
PUT  /api/utilizadores/{id}
DELETE /api/utilizadores/{id}
```

---

## 7. Estrutura da API

### Endpoints Públicos (sem autenticação)

| Método | URL | Descrição |
|---|---|---|
| POST | `/api/auth/registar` | Registar novo utilizador |
| POST | `/api/auth/login` | Login |
| GET | `/api/categorias` | Listar categorias |
| GET | `/api/documentarios` | Listar documentários |
| GET | `/api/documentarios/pesquisa` | Pesquisar |
| GET | `/api/documentarios/{id}` | Ver documentário |
| GET | `/api/streaming/{id}` | Stream de vídeo |
| GET | `/api/streaming/{id}/thumbnail` | Thumbnail |

### Endpoints Autenticados (qualquer utilizador)

| Método | URL | Descrição |
|---|---|---|
| POST | `/api/auth/logout` | Logout |
| GET | `/api/auth/sessoes` | Minhas sessões |
| DELETE | `/api/auth/sessoes/{id}` | Revogar sessão |
| POST | `/api/documentarios/upload` | Upload |
| GET | `/api/documentarios/meus` | Meus documentários |
| PUT | `/api/documentarios/{id}` | Editar (se for dono) |
| DELETE | `/api/documentarios/{id}` | Eliminar (se for dono) |
| GET | `/api/documentarios/{id}/download` | Download |
| GET | `/api/utilizadores/perfil` | Ver perfil |
| PUT | `/api/utilizadores/perfil` | Editar perfil |
| PUT | `/api/utilizadores/perfil/password` | Alterar password |
| GET | `/api/logs/meus` | Meus logs |

### Endpoints Admin

| Método | URL | Descrição |
|---|---|---|
| POST | `/api/categorias` | Criar categoria |
| PUT | `/api/categorias/{id}` | Editar categoria |
| DELETE | `/api/categorias/{id}` | Eliminar categoria |
| GET | `/api/utilizadores` | Listar utilizadores |
| PUT | `/api/utilizadores/{id}` | Editar utilizador |
| DELETE | `/api/utilizadores/{id}` | Eliminar utilizador |
| GET | `/api/logs` | Ver todos os logs |
| DELETE | `/api/logs` | Limpar logs |
| POST | `/api/documentarios/{id}/recomprimir` | Recomprimir |

---

## 8. Credenciais Padrão

| Utilizador | Email | Password | Role |
|---|---|---|---|
| Administrador | `admin@arquivo.ao` | `Admin@2024` | ADMIN |

> Cria utilizadores normais via `POST /api/auth/registar`

---

## Estrutura de Pastas do Projeto

```
arquivoDigitalDocumentariosAPI/
├── pom.xml
├── README.md
└── src/main/java/com/arquivodigital/
    ├── ArquivoDigitalApplication.java
    ├── config/          → CORS, Swagger, Async, DataSeeder
    ├── controller/      → AuthController, DocumentarioController, StreamingController...
    ├── dto/
    │   ├── request/     → LoginRequest, RegisterRequest, DocumentarioRequest...
    │   └── response/    → AuthResponse, DocumentarioResponse, CompressaoRelatorioResponse...
    ├── entity/          → Utilizador, Sessao, Documentario, Categoria, Log
    ├── exception/
    │   ├── custom/      → ResourceNotFoundException, NegocioException...
    │   └── handler/     → GlobalExceptionHandler
    ├── mapper/          → MapStruct mappers (Entity ↔ DTO)
    ├── repository/      → JPA repositories
    ├── security/        → JwtUtil, JwtFilter, SecurityConfig, UserDetailsImpl
    ├── service/         → AuthService, DocumentarioService, StreamingService...
    └── util/            → FFmpegUtil, FileStorageUtil
```

---

## Segurança Implementada

- **JWT Bearer Tokens** — autenticação stateless
- **Sessões no servidor** — cada login cria um registo em BD, pode ser revogado
- **BCrypt** — hash seguro de passwords
- **RBAC** — `ROLE_ADMIN` e `ROLE_USER`
- **CORS** configurado para aceitar pedidos do Android
- **Logs de auditoria** — todas as acções ficam registadas

## Compressão Implementada

- **Vídeo**: H.264 (libx264) com CRF 23, preset `medium`
- **Áudio**: AAC 128kbps
- **Thumbnail**: JPEG 320px extraído ao segundo 10
- **Processamento assíncrono** — o upload responde imediatamente, a compressão corre em background
- **Relatório comparativo** disponível em `/api/documentarios/{id}/relatorio-compressao`
