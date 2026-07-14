# Fluxos Completos da Aplicação

**Arquivo Digital de Documentários | Grupo 06**

Este documento explica, **passo a passo**, o que acontece em **cada funcionalidade** — desde o toque no
ecrã do telemóvel até à base de dados e de volta. Mostra **que funções são chamadas** em cada ponto.

---

## Índice

- [Como ler este documento](#como-ler-este-documento)
- [Os 3 componentes](#os-3-componentes)
- **Autenticação**
  - [1. Criar conta (registo)](#1-criar-conta-registo)
  - [2. Fazer login](#2-fazer-login)
  - [3. Enrollment do certificado (automático)](#3-enrollment-do-certificado-automático)
- **Conteúdos**
  - [4. Abrir a página inicial](#4-abrir-a-página-inicial)
  - [5. Pesquisar](#5-pesquisar)
  - [6. Publicar um vídeo (upload)](#6-publicar-um-vídeo-upload) ⭐
  - [7. Assistir a um documentário (streaming)](#7-assistir-a-um-documentário-streaming)
  - [8. Avaliar (like/dislike → estrelas)](#8-avaliar-likedislike--estrelas)
  - [9. Guardar para ver offline (download)](#9-guardar-para-ver-offline-download)
  - [10. Minha Lista e Histórico](#10-minha-lista-e-histórico)
  - [11. Notificações](#11-notificações)
- **Lives (WebRTC)**
  - [12. Fazer uma live (emissor)](#12-fazer-uma-live-emissor)
  - [13. Assistir a uma live (espectador)](#13-assistir-a-uma-live-espectador)
  - [14. Chat e reações na live](#14-chat-e-reações-na-live)
- **Backoffice (admin)**
  - [15. Login no backoffice](#15-login-no-backoffice)
  - [16. Gerir utilizadores](#16-gerir-utilizadores)
  - [17. Gerir documentários](#17-gerir-documentários)
  - [18. Certificados e revogação](#18-certificados-e-revogação)
  - [19. Logs e rastreabilidade](#19-logs-e-rastreabilidade)
- [Tabela resumo: endpoint → controller → service](#tabela-resumo-endpoint--controller--service)

---

## Como ler este documento

Cada fluxo usa este formato:

```
📱 ANDROID (o que o utilizador faz)
      │  Ficheiro/método chamado
      ▼
🌐 PEDIDO HTTP (endpoint)
      │
      ▼
🖥️ BACKEND (Controller → Service → Repositório/BD)
      │
      ▼
◀ RESPOSTA (o que volta e o que a app faz com isso)
```

**Legenda:** 📱 app Android · 🖥️ backend Spring Boot · 🌐 painel web (backoffice) · 🗄️ base de dados

---

## Os 3 componentes

```
   📱 App Android          🌐 Backoffice Web
   (utilizadores)          (administradores)
          │                        │
          │   HTTPS + mTLS         │   HTTPS
          └───────────┬────────────┘
                      ▼
            🖥️  BACKEND (Spring Boot)
             Controllers → Services → Repositories
                      │
                      ▼
              🗄️ PostgreSQL + ficheiros (uploads/)
```

- **Todos os pedidos** passam por dois filtros de segurança:
  1. `ClientCertFilter` → lê o certificado do dispositivo (identifica o aparelho)
  2. `JwtFilter` → lê o token JWT (identifica o utilizador e o seu *role*)

---

# AUTENTICAÇÃO

## 1. Criar conta (registo)

```
📱 RegisterActivity — utilizador preenche nome, email, password e toca "Registar"
      │  RetrofitClient.getInstance(this).getApi().registar(new RegisterRequest(...))
      ▼
🌐 POST /api/auth/registar     { nome, email, password }
      │
      ▼
🖥️ AuthController.registar()
      └─► AuthService.registar(request, ip)
            1. Verifica se o email já existe        → utilizadorRepository.existsByEmail()
            2. Cifra a password                     → passwordEncoder.encode()  (BCrypt)
            3. Cria o utilizador (role = USER)      → utilizadorRepository.save()
            4. Cria uma sessão                      → sessaoService.criar()
            5. Gera o token                         → jwtUtil.gerarToken()
            6. Regista no log                       → logService.registar(AcaoLog.REGISTO, ...)
      ▼
◀ HTTP 201 { token, sessaoId, utilizador{ id, nome, email, role } }
      │
📱 SessionManager.saveSession(token, utilizador)   → guarda em SharedPreferences
📱 Vai para HomeActivity  →  dispara o ENROLLMENT (ver fluxo 3)
```

> A password **nunca** é guardada em texto simples — só o *hash* BCrypt.

---

## 2. Fazer login

```
📱 LoginActivity — email + password → "Entrar"
      │  api.login(new LoginRequest(email, password))
      ▼
🌐 POST /api/auth/login
      │
      ▼
🖥️ AuthController.login()
      └─► AuthService.login(request, ip, userAgent)
            1. Procura o utilizador pelo email       → utilizadorRepository.findByEmail()
            2. Compara a password                    → passwordEncoder.matches()
            3. Verifica se a conta está ativa
            4. Cria a sessão (máx. 5 por utilizador) → sessaoService.criar()
            5. Gera o JWT (contém: email, userId, role, sessaoId)
            6. Log                                   → logService.registar(AcaoLog.LOGIN, ...)
      ▼
◀ HTTP 200 { token, sessaoId, utilizador }
      │
📱 SessionManager.saveSession()
📱 HomeActivity → DeviceEnrollment.ensureEnrolled(this)   ← fluxo 3
```

**A partir daqui**, o `AuthInterceptor` (OkHttp) **injeta automaticamente** o header em cada pedido:
```
Authorization: Bearer eyJhbGciOi...
```

---

## 3. Enrollment do certificado (automático)

Acontece **uma vez**, logo após o primeiro login.

```
📱 HomeActivity.onCreate() → DeviceEnrollment.ensureEnrolled(ctx)
      │  1. Já existe device.p12?  → SessionManager.hasDeviceCert()
      │     SIM → não faz nada
      │     NÃO → continua:
      │  2. Gera um deviceId estável (ex.: and-f63a8bc5ddea)
      ▼
🌐 POST /api/enrollment    { deviceId, deviceName }   (com o JWT)
      │
      ▼
🖥️ EnrollmentController.enrolar()
      └─► EnrollmentService.enrolar(deviceId, deviceName, dono, ip)
            1. Gera par de chaves        → openssl genrsa
            2. Cria o CSR                → openssl req
            3. ASSINA com a CA ✍️        → openssl x509 -CA ca.crt -CAkey ca.key
            4. Empacota em PKCS#12       → openssl pkcs12
            5. Regista na BD             → certRepo.save(CertificadoDispositivo)
            6. Log                       → logService.registar(AcaoLog.CERT_REGISTADO, ...)
      ▼
◀ HTTP 200 { deviceId, p12Base64, password }
      │
📱 SessionManager.saveDeviceCert(bytes, password)   → guarda device.p12 (armazenamento interno)
📱 TlsConfig.reload(ctx)   → a partir de agora TODOS os pedidos apresentam o certificado
```

**Efeito:** cada pedido seguinte é identificado pelo dispositivo → aparece na coluna **Dispositivo** dos Logs.
📖 Detalhes completos: [`pki/README.md`](pki/README.md)

---

# CONTEÚDOS

## 4. Abrir a página inicial

```
📱 HomeActivity.loadData()  — faz VÁRIOS pedidos em paralelo:
      │
      ├─🌐 GET /api/documentarios?pagina=0&tamanho=20
      │        → DocumentarioController.listar() → DocumentarioService.listar()
      │        → 📱 escolherDestaque(docs)  ← ranking: visualizações → estrelas → recência
      │        → setupHero(doc)  (o vídeo em destaque)
      │
      ├─🌐 GET /api/categorias
      │        → CategoriaController.listar()
      │        → 📱 para cada categoria: addCategoryRow() (carrossel horizontal)
      │
      ├─🌐 GET /api/documentarios/mais-vistos
      │        → DocumentarioController.maisVistos() → DocumentarioService.listarMaisVistos()
      │          (ordena por: ⭐ estrelas → 👁️ visualizações → 📅 mais antigo primeiro; limita a 10)
      │        → 📱 secção "Top 10"
      │
      └─🌐 GET /api/notificacoes   (de 20 em 20 segundos)
               → 📱 badge do sino + push das novas (NotificationHelper.mostrar)
```

---

## 5. Pesquisar

```
📱 SearchActivity — o utilizador escreve o termo
      │  api.pesquisar(q, categoriaId, pagina, tamanho)
      ▼
🌐 GET /api/documentarios/pesquisa?q=angola&categoriaId=8
      ▼
🖥️ DocumentarioController.pesquisar()
      └─► DocumentarioService.pesquisar(q, categoriaId, ano, pagina, tamanho)
            → documentarioRepository (query com filtros)
      ▼
◀ PageResponse<DocumentarioResponse>
📱 DocumentarioListAdapter mostra os resultados
```

---

## 6. Publicar um vídeo (upload) ⭐

**O fluxo mais rico** — tem uma parte **síncrona** (rápida) e outra **assíncrona** (demorada).

### Parte 1 — Upload (síncrona, o utilizador espera)

```
📱 UploadActivity — escolhe ficheiro + título, descrição, ano, categoria → "Publicar documentário"
      │  api.uploadDocumentario(ficheiro, titulo, descricao, ano, categoriaId)   [multipart]
      ▼
🌐 POST /api/documentarios/upload      (multipart/form-data)
      ▼
🖥️ DocumentarioController.upload()
      └─► DocumentarioService.upload(ficheiro, request, utilizador, ip)
            1. Guarda o ficheiro no disco   → fileStorageUtil.guardar()  → uploads/originais/
            2. Cria o registo na BD com status = PENDENTE
            3. Log                          → logService.registar(AcaoLog.UPLOAD, ...)
            4. Agenda a compressão:
                 TransactionSynchronization.afterCommit() {
                     compressaoService.comprimirAsync(doc.getId());   ← corre em BACKGROUND
                 }
      ▼
◀ HTTP 201 { id, titulo, status: "PENDENTE", ... }     ← responde JÁ, sem esperar pela compressão
📱 Mostra "Enviado! A processar…"
```

### Parte 2 — Processamento (assíncrona, em background)

```
🖥️ CompressaoService.comprimirAsync(documentarioId)     @Async
      │
      1. status = PROCESSANDO                → documentarioRepository.save()
      │
      2. 🎬 COMPRIME O VÍDEO
         ffmpegUtil.comprimirVideoH265(original, comprimido)
         (H.265/libx265 → ficheiro muito menor, sem perda visível)
      │
      3. 🖼️ EXTRAI A CAPA
         ffmpegUtil.extrairThumbnail(original, thumbnail)     (filtro "thumbnail" do FFmpeg)
      │
      4. ⏱️ OBTÉM A DURAÇÃO
         ffmpegUtil.obterDuracao(original)
      │
      5. status = PRONTO   (+ tamanhos, taxa de compressão, codec, tempo)
      │
      6. 💬 GERA AS LEGENDAS
         whisperUtil.gerarLegendas(original, legendas.vtt)    (transcrição automática do áudio)
      │
      7. Grava tudo                          → documentarioRepository.save()
      │
      8. 🔔 NOTIFICA
         notificacaoService.notificarUploadConcluido(doc)        → ao AUTOR
         notificacaoService.notificarNovoVideoNaCategoria(doc)   → a quem gosta daquela categoria
```

```
📱 O autor recebe a notificação "Upload concluído" (o sino + push)
📱 Em "Meus Documentários" o estado passa de PROCESSANDO → PRONTO
🌐 No backoffice, o admin vê o relatório de compressão (original vs comprimido, % poupado)
```

> Se o FFmpeg falhar, o status fica `ERRO` e o admin pode **Reprocessar** no backoffice.

---

## 7. Assistir a um documentário (streaming)

```
📱 DocumentarioDetailActivity → botão "Assistir" → openPlayer()
      │  1. api.registarVisualizacao(docId)        ← regista no histórico
      │  2. Abre PlayerActivity com a urlStreaming
      ▼
🌐 POST /api/historico/{docId}
      └─► HistoricoController.registar() → HistoricoService.registar(docId, utilizador)
            → guarda no histórico + documentarioRepository.incrementarVisualizacoes(docId)
      │
📱 PlayerActivity — ExoPlayer (Media3)
      │  player.setMediaItem(MediaItem com urlStreaming + legendas VTT)
      ▼
🌐 GET /api/streaming/{id}       com header:  Range: bytes=0-1048575
      ▼
🖥️ StreamingController.stream()
      └─► StreamingService — lê o ficheiro comprimido
            • SEM Range  → HTTP 200 + vídeo completo
            • COM Range  → HTTP 206 (Partial Content) + só o pedaço pedido
      ▼
◀ Bytes do vídeo (streaming progressivo)
📱 O vídeo começa a tocar sem esperar pelo download completo; o "seek" (avançar/recuar)
   funciona porque o servidor suporta Range.
```

**Legendas:** `GET /api/streaming/{id}/legendas` → ficheiro `.vtt` → o ExoPlayer mostra-as.

---

## 8. Avaliar (like/dislike → estrelas)

```
📱 DocumentarioDetailActivity → toca no ícone de gosto
      │  api.avaliar(docId, new AvaliacaoRequest(valor))    valor: +1 (like) / -1 (dislike)
      ▼
🌐 POST /api/avaliacoes/{docId}     { valor: 1 }
      ▼
🖥️ AvaliacaoController.avaliar()
      └─► AvaliacaoService.avaliar(docId, request, utilizador)
            1. Já avaliou? → atualiza (só 1 avaliação por utilizador/vídeo)
            2. Grava       → avaliacaoRepository.save()
            3. Notifica o dono → notificacaoService.notificarLikeRecebido(doc, quemGostou)
      ▼
◀ AvaliacaoResponse { likes, dislikes, mediaEstrelas }
```

**Como nascem as ⭐ estrelas:** não são votos diretos — são **calculadas a partir dos likes**, com
suavização (*m-estimate*), em `DocumentarioService.calcularMediaEstrelas(likes, dislikes)`:

```
proporção = (likes + peso × baseline) / (likes + dislikes + peso)
estrelas  = proporção × 5
```
> Com `baseline=0.7` e `peso=5`: um vídeo **sem votos** começa em **3.5 ⭐** (neutro), e as estrelas
> vão-se ajustando à medida que chegam votos reais. Evita que 1 único like dê logo 5 ⭐.

---

## 9. Guardar para ver offline (download)

```
📱 DocumentarioDetailActivity → botão "Guardar" → handleDownload()
      │  1. Verifica o espaço:  usado + tamanho > limite? → avisa e pára
      │     (SessionManager.getUsedDownloadSpace() / getDownloadStorageLimit())
      │  2. DownloadsActivity.startDownload(ctx, doc, token)
      ▼
📱 Android DownloadManager  (serviço do sistema)
      │  request = new DownloadManager.Request(urlDownload)
      │  request.addRequestHeader("Authorization", "Bearer " + token)
      ▼
🌐 GET /api/documentarios/{id}/download
      ▼
🖥️ DocumentarioController.download()
      └─► documentarioService.incrementarDownloads(id, utilizador, ip)
            1. downloads++                → documentarioRepository.incrementarDownloads()
            2. Log                        → AcaoLog.DOWNLOAD
            3. 🔔 Notifica o DONO do vídeo → notificacaoService.notificarDownloadRecebido()
      ▼
◀ O ficheiro de vídeo
      │
📱 Guardado em: Android/data/.../files/Movies/doc_{id}.mp4
📱 O BroadcastReceiver (ACTION_DOWNLOAD_COMPLETE) → notificação "Download concluído"
📱 Ao reproduzir, o player usa o ficheiro LOCAL (file://) se existir; senão faz streaming
```

**Limite de espaço** (nas Definições): máx. = 50% do armazenamento do telemóvel, entre 1 GB e 50 GB.

---

## 10. Minha Lista e Histórico

### Minha Lista ("ver mais tarde")
```
📱 Botão "Minha Lista" → handleMyList()
      │  (atualiza já localmente = resposta instantânea; depois sincroniza)
      ▼
🌐 POST /api/lista/{docId}      (ou DELETE para remover)
      └─► MinhaListaController.adicionar() → MinhaListaService → minhaListaRepository.save()
      │
📱 Se a API falhar → desfaz a alteração local (rollback otimista)
```

### Histórico
```
🌐 POST /api/historico/{docId}   ← chamado automaticamente ao abrir o player
      └─► HistoricoService.registar() → grava + incrementa visualizações

🌐 GET  /api/historico           ← HistoryActivity lista o que já assististe
🌐 DELETE /api/historico         ← limpar histórico
```

---

## 11. Notificações

**Como são criadas** (no backend, por eventos):

| Evento | Função chamada | Quem recebe |
|---|---|---|
| Upload ficou PRONTO | `notificarUploadConcluido(doc)` | O autor |
| Novo vídeo numa categoria | `notificarNovoVideoNaCategoria(doc)` | Quem gosta dessa categoria |
| Alguém deu like | `notificarLikeRecebido(doc, quemGostou)` | O dono do vídeo |
| Alguém fez download | `notificarDownloadRecebido(doc, quem)` | O dono do vídeo |
| Começou uma live | `notificarLive(...)` | Todos |

**Como chegam ao telemóvel** (*polling*, de 20 em 20 segundos):

```
📱 HomeActivity.checkNotifications()   ← Runnable a cada 20s
      ▼
🌐 GET /api/notificacoes
      └─► NotificacaoController.listar() → lista do utilizador
      ▼
◀ [ { id, tipo, titulo, mensagem, lida } ]
      │
📱 1. Conta as não lidas → mostra o badge vermelho no sino 🔔
📱 2. Para as NOVAS (id > último visto) → NotificationHelper.mostrar()  → push na bandeja
📱 3. Guarda o último id visto (evita repetir)
```

> ℹ️ **Limitação honesta:** é *polling* (só funciona com a app aberta). Um sistema de produção usaria
> **FCM (Firebase Cloud Messaging)** para push real mesmo com a app fechada.

---

# LIVES (WebRTC)

As lives **não passam pelo servidor de vídeo** — o áudio/vídeo vai **direto** entre os telemóveis
(P2P via WebRTC). O servidor só faz a **sinalização** (apresenta os pares) via WebSocket.

**Protocolo** (`SignalingHandler`, em `wss://.../ws/signaling`):

| Mensagem enviada | Significado |
|---|---|
| `{type:"start", titulo}` | O emissor inicia uma live |
| `{type:"join", liveId}` | Um espectador entra |
| `{type:"offer", target, sdp}` | Proposta de ligação (SDP) |
| `{type:"answer", target, sdp}` | Resposta à proposta |
| `{type:"ice", target, candidate}` | Candidato de rede (ICE) |
| `{type:"chat" / "reaction"}` | Mensagem / reação |
| `{type:"stop"}` / `{type:"leave"}` | Terminar / sair |

---

## 12. Fazer uma live (emissor)

```
📱 GoLiveActivity — o utilizador escreve o título e toca "Iniciar"
      │
      1. Pede permissões (câmara + microfone)
      2. Captura o vídeo local (WebRTC) e mostra na pré-visualização
      3. SignalingClient.connect()      → abre o WebSocket  wss://.../ws/signaling  (com o JWT)
      4. SignalingClient.startLive(titulo)   → envia {type:"start", titulo}
      ▼
🖥️ SignalingHandler.handleTextMessage() → case "start" → aoIniciar(session, msg)
      1. Cria a LiveSession (id único)      → LiveRegistry.criar()
      2. Marca esta sessão como EMISSOR
      3. 🔔 notificacaoService.notificarLive(...)   → avisa todos os utilizadores
      ▼
◀ {type:"started", liveId, selfId}
📱 A live está no ar. A app fica à espera de espectadores.

--- quando alguém entra ---
🖥️ ◀ {type:"viewer-join", viewerId}
📱 O emissor cria uma PeerConnection para esse espectador:
      → SignalingClient.sendOffer(viewerId, sdp)     {type:"offer", target: viewerId, sdp}
      ← recebe {type:"answer"} e troca {type:"ice"} …
      ▼
   🔗 Ligação P2P estabelecida → o vídeo flui DIRETO para o espectador
   (o contador de espectadores sobe)

--- terminar ---
📱 "Terminar" → {type:"stop"} → SignalingHandler.aoTerminar() → LiveRegistry remove a live
```

---

## 13. Assistir a uma live (espectador)

```
📱 LivesActivity — GET /api/lives → LiveController.listar() → lista de lives a decorrer
      │  o utilizador toca numa live
      ▼
📱 LiveViewerActivity
      1. SignalingClient.connect()
      2. SignalingClient.join(liveId)      → {type:"join", liveId}
      ▼
🖥️ SignalingHandler → case "join" → aoEntrar(session, msg)
      1. Encontra a LiveSession            → LiveRegistry.buscar(liveId)
      2. Adiciona o espectador
      3. Avisa o EMISSOR                   → {type:"viewer-join", viewerId}
      ▼
◀ {type:"joined", selfId}
      │
📱 Recebe {type:"offer"} do emissor
   → cria a PeerConnection, responde com {type:"answer"} e troca {type:"ice"}
      ▼
   🔗 Ligação P2P → o vídeo do emissor aparece no ecrã 🎥
```

> **Importante:** o servidor **nunca vê o vídeo** — só reencaminha as mensagens de sinalização
> (`reencaminhar()`). O vídeo vai telemóvel↔telemóvel.

---

## 14. Chat e reações na live

```
📱 Escreve no chat → {type:"chat", texto}
      ▼
🖥️ SignalingHandler → case "chat" → aoChat(session, msg)
      → faz BROADCAST da mensagem a TODOS os participantes daquela live
      ▼
📱 Todos (emissor + espectadores) recebem → ChatAdapter mostra a mensagem

📱 Toca numa reação (❤️ 👍) → {type:"reaction", ...}
      → aoReacao() → broadcast → animação no ecrã de todos
```

> Ao contrário do vídeo (P2P), o **chat passa pelo servidor** — é ele que distribui a todos.

---

# BACKOFFICE (admin)

O backoffice é **HTML/CSS/JS puro**, servido pelo próprio backend em `/admin/`. Usa a **mesma API**
(`/api/...`) que a app, mas com um utilizador de role **ADMIN**.

## 15. Login no backoffice

```
🌐 https://<ip>:8080/admin/index.html  → formulário de login
      │  app.js:  api('/auth/login', 'POST', {email, password})
      ▼
🖥️ AuthController.login()  (o MESMO endpoint da app)
      ▼
◀ { token, utilizador { role } }
      │
🌐 app.js verifica:  if (data.utilizador.role !== 'ADMIN') → "Não tem autorização"
🌐 Guarda o token em localStorage → todos os pedidos seguintes levam  Authorization: Bearer …
```

> Um utilizador `USER` **é barrado no painel** — e mesmo que forçasse, o backend recusaria
> (`@PreAuthorize("hasRole('ADMIN')")`).

---

## 16. Gerir utilizadores

```
🌐 Separador "Utilizadores" → app.js: loadUsers()
      ▼  GET /api/utilizadores?pagina=0&tamanho=10
🖥️ UtilizadorController.listar()   @PreAuthorize("hasRole('ADMIN')")
      ▼
◀ Lista paginada → renderUsers() desenha a tabela

Ações disponíveis:
  ✏️ Editar   → PUT /api/utilizadores/{id}     → adminActualizar()  (nome, email, password, role, ativo)
  🗑️ Eliminar → DELETE /api/utilizadores/{id}  → eliminar()
  📊 Atividade→ GET /api/utilizadores/{id}/atividade → AtividadeService.atividade()
                (mostra: logs, likes/dislikes, minha lista, histórico, sessões)
  🔑 Revogar sessões → DELETE /api/utilizadores/{id}/sessoes  → invalida o JWT
```

---

## 17. Gerir documentários

```
🌐 Separador "Documentários" → loadDocs()
      ▼  GET /api/documentarios/admin/todos
🖥️ DocumentarioController.listarTodosAdmin()

Ações:
  ⬆️ Carregar    → doUpload()  → POST /api/documentarios/upload  (o admin também publica!)
  ▶️ Assistir    → watchDoc()  → abre um <video> com a urlStreaming
  📊 Relatório   → GET /api/documentarios/{id}/relatorio-compressao
                   → mostra: tamanho original vs comprimido, % poupado, codec, tempo
  🔄 Reprocessar → POST /api/documentarios/{id}/recomprimir  → corre o pipeline outra vez
  🗑️ Eliminar    → DELETE /api/documentarios/{id}  → apaga o registo E os ficheiros
```

O **Dashboard** mostra estatísticas globais:
`GET /api/documentarios/admin/estatisticas` → `DocumentarioService.estatisticas()`
→ espaço total poupado pela compressão, taxa média, gráficos por estado e por categoria.

---

## 18. Certificados e revogação

```
🌐 Separador "Certificados" → loadCerts()
      ▼  GET /api/certificados
🖥️ CertificadoController.listar() → CertificadoService.listar()
      ▼
◀ Lista: dispositivo, dono, série, validade, ÚLTIMA UTILIZAÇÃO, estado

🚫 REVOGAR:
🌐 Botão "Revogar" → revokeCert()
      ▼  POST /api/certificados/{id}/revogar
🖥️ CertificadoService.revogar(id, admin, ip)
      → estado = REVOGADO;  revogadoEm = agora;  log CERT_REVOGADO
      ▼
📱 No PEDIDO SEGUINTE daquele telemóvel:
   🖥️ ClientCertFilter lê o CN → estaRevogadoPorDevice() → true
      → ❌ HTTP 403 "Certificado do dispositivo revogado"    (dispositivo bloqueado ⚡ imediato)
```

📖 Explicação completa: [`pki/README.md`](pki/README.md)

---

## 19. Logs e rastreabilidade

**Como um log é criado** — qualquer serviço chama:
```java
logService.registar(AcaoLog.UPLOAD, "Enviou o documentário: X", utilizador, ip);
```

E o `LogService.registar()` acrescenta **automaticamente** o dispositivo:
```java
.dispositivo(DispositivoContext.get())   // ← preenchido pelo ClientCertFilter
```

**A cadeia completa:**
```
📱 App faz um pedido (com certificado + JWT)
      ▼
🖥️ ClientCertFilter  → lê o CN do certificado → DispositivoContext.set("and-f63a8bc5ddea")
🖥️ JwtFilter         → lê o token → identifica o utilizador
🖥️ Service executa a ação → logService.registar(...)
      ▼
🗄️ Tabela logs:  { acao, detalhe, utilizador, DISPOSITIVO, ip, timestamp }
      ▼
🌐 Backoffice → Logs → renderLogs()
      → tabela: Data | Utilizador | DISPOSITIVO | Ação | Detalhe | IP
```

> **É isto que responde a "quem fez o quê na rede":**
> *"O utilizador **Solene**, no dispositivo **and-f63a8bc5ddea**, fez **UPLOAD** às **03:34**, do IP **192.168.100.235**."*

---

## Tabela resumo: endpoint → controller → service

| Funcionalidade | Endpoint | Controller | Service |
|---|---|---|---|
| Registo | `POST /api/auth/registar` | `AuthController.registar` | `AuthService.registar` |
| Login | `POST /api/auth/login` | `AuthController.login` | `AuthService.login` |
| Enrollment | `POST /api/enrollment` | `EnrollmentController.enrolar` | `EnrollmentService.enrolar` |
| Listar docs | `GET /api/documentarios` | `DocumentarioController.listar` | `DocumentarioService.listar` |
| Pesquisar | `GET /api/documentarios/pesquisa` | `.pesquisar` | `DocumentarioService.pesquisar` |
| Top 10 | `GET /api/documentarios/mais-vistos` | `.maisVistos` | `.listarMaisVistos` |
| Upload | `POST /api/documentarios/upload` | `.upload` | `DocumentarioService.upload` → `CompressaoService.comprimirAsync` |
| Streaming | `GET /api/streaming/{id}` | `StreamingController.stream` | `StreamingService` |
| Download | `GET /api/documentarios/{id}/download` | `.download` | `.incrementarDownloads` |
| Avaliar | `POST /api/avaliacoes/{docId}` | `AvaliacaoController.avaliar` | `AvaliacaoService.avaliar` |
| Minha Lista | `POST /api/lista/{docId}` | `MinhaListaController.adicionar` | `MinhaListaService` |
| Histórico | `POST /api/historico/{docId}` | `HistoricoController.registar` | `HistoricoService.registar` |
| Notificações | `GET /api/notificacoes` | `NotificacaoController.listar` | `NotificacaoService` |
| Lives (lista) | `GET /api/lives` | `LiveController.listar` | `LiveRegistry` |
| Lives (sinalização) | `wss://…/ws/signaling` | `SignalingHandler` | `LiveRegistry` / `LiveSession` |
| Certificados | `GET /api/certificados` | `CertificadoController.listar` | `CertificadoService.listar` |
| Revogar cert. | `POST /api/certificados/{id}/revogar` | `.revogar` | `CertificadoService.revogar` |
| Logs | `GET /api/logs` | `LogController.listar` | `LogService.listarTodos` |

---

## Ver também

- [README do Backend](README.md)
- [README do Frontend Android](../Arquivo-Digital-de-Document-rios/README.md)
- [Guia dos Certificados (PKI/mTLS)](pki/README.md)