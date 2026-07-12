# Sistema de Certificados (PKI / mTLS) — Guia Completo

**Arquivo Digital de Documentários | Grupo 06**

Este documento explica, de forma detalhada e acessível, **como funcionam os certificados** no projeto:
o que é a Autoridade Certificadora, onde tudo está instalado, como os certificados são atribuídos,
que poderes tem o administrador e — muito importante — **o que os certificados fazem e o que NÃO fazem**.

---

## Índice

1. [A ideia base (analogia)](#1-a-ideia-base-analogia)
2. [Os 3 certificados e onde estão instalados](#2-os-3-certificados-e-onde-estão-instalados)
3. [A Autoridade Certificadora (CA)](#3-a-autoridade-certificadora-ca)
4. [Como os certificados são atribuídos (enrollment)](#4-como-os-certificados-são-atribuídos-enrollment)
5. [Precisa de autorização do administrador?](#5-precisa-de-autorização-do-administrador)
6. [⚠️ Permissões: certificado ≠ permissões](#6-️-permissões-certificado--permissões)
7. [Os poderes do administrador](#7-os-poderes-do-administrador)
8. [Revogação — o que acontece tecnicamente](#8-revogação--o-que-acontece-tecnicamente)
9. [Limitações honestas (e como fechá-las)](#9-limitações-honestas-e-como-fechá-las)
10. [Resumo rápido](#10-resumo-rápido)

---

## 1. A ideia base (analogia)

| No mundo real | No nosso sistema |
|---|---|
| **Cartório/notário** que emite bilhetes de identidade | **A CA** (Autoridade Certificadora) |
| **Placa oficial da loja** (prova que é a loja verdadeira, não uma cópia) | **Certificado do servidor** |
| **Bilhete de identidade do telemóvel** | **Certificado do dispositivo** |
| **Crachá que diz o que podes fazer lá dentro** | **Token JWT + role (USER/ADMIN)** ← *é outra coisa!* |

Guarda bem a última linha — é a chave da secção 6.

---

## 2. Os 3 certificados e onde estão instalados

### 🖥️ No BACKEND (servidor)

```
arquivoDigitalDocumentariosAPI/pki/
├── ca/
│   ├── ca.crt      ← certificado da CA (PÚBLICO — distribui-se)
│   └── ca.key      ← 🔒 CHAVE PRIVADA DA CA (a "joia da coroa")
├── server/
│   ├── server-keystore.p12   ← certificado do SERVIDOR (o Spring Boot serve HTTPS com este)
│   └── truststore.p12        ← contém a CA → VALIDA os certificados dos dispositivos
└── devices/
    └── and-f63a8bc5ddea/     ← cópia do certificado emitido para cada telemóvel
```

Os **metadados** de cada certificado ficam na base de dados, na tabela **`certificados_dispositivo`**
(é o que o backoffice lista em *Certificados*).

### 📱 No FRONTEND (app Android) — dois ficheiros, sítios MUITO diferentes

| Ficheiro | Onde está | Como lá chega | É igual em todos? |
|---|---|---|---|
| **`arquivo_ca.crt`** (a CA) | `app/src/main/res/raw/` → **dentro do APK** | Copiado por ti antes de compilar | ✅ **Sim** — igual em todos os telemóveis |
| **`device.p12`** (certificado próprio) | Armazenamento **interno privado** da app:<br>`/data/data/isptec.multimedia.../files/device.p12` | **Descarregado do servidor** no *enrollment* | ❌ **Não** — **único por telemóvel** |

> A password do `device.p12` é guardada nas **SharedPreferences** da app.
> O certificado do dispositivo **NUNCA está no APK** — é obtido em runtime.

**Para que serve cada um:**
- `arquivo_ca.crt` → a app **confia no servidor** (valida o certificado dele) → 🛡️ anti-MITM
- `device.p12` → a app **prova a sua identidade** ao servidor → 🔍 rastreabilidade

---

## 3. A Autoridade Certificadora (CA)

### Qual é a função dela?
Ser a **raiz de confiança** de todo o sistema. É quem **assina** certificados — e tudo o que ela assina
é considerado legítimo. É como um **cartório**: sozinho, um documento não vale nada; com o carimbo do
cartório, passa a ser oficial.

### Onde está exatamente?
**Não é um serviço à parte** — são **ficheiros no servidor** (`pki/ca/`), usados pelo backend.
Foi criada uma única vez pelo script `gerar_pki.sh`.

> ⚠️ **A `ca.key` é o ponto mais crítico do sistema.** Quem a tiver pode fabricar certificados falsos
> que o servidor aceitaria. Por isso está protegida pelo `.gitignore` (nunca vai para o Git).

### O que faz exatamente? (4 funções)

| # | Função | Como acontece no nosso sistema |
|---|---|---|
| **1** | **Ser a raiz de confiança** | Tem um certificado *auto-assinado* (`ca.crt`). É o único em que a app confia (`res/raw/arquivo_ca.crt`) |
| **2** | **Assinar o certificado do SERVIDOR** | O telemóvel passa a saber que fala com o servidor **verdadeiro** → 🛡️ bloqueia *man-in-the-middle* |
| **3** | **Assinar o certificado de cada DISPOSITIVO** | No *enrollment*, o `EnrollmentService` usa a `ca.key` (via OpenSSL) para **assinar** o certificado daquele telemóvel |
| **4** | **Servir de base à revogação** | Como sabe o que emitiu, é possível listar e invalidar certificados no backoffice |

**Quem "usa" a CA é o backend:** quando um telemóvel faz *enrollment*, o `EnrollmentService` pega na
`ca.key`, assina o certificado novo e devolve-o.

---

## 4. Como os certificados são atribuídos (enrollment)

Acontece **automaticamente no 1.º login**. Passo a passo:

```
📱 App                                       🖥️ Servidor (tem a CA)
 │
 │ 1. Utilizador faz login  ────────────────►  devolve JWT ✅
 │
 │ 2. "Ainda não tenho device.p12"
 │    Gera um deviceId estável (and-f63a8bc5ddea)
 │
 │ 3. POST /api/enrollment  ────────────────►  4. Valida o JWT (tem de estar autenticado)
 │    {deviceId, deviceName}                    5. Gera par de chaves + certificado
 │                                              6. ASSINA com a ca.key  ✍️
 │                                              7. Regista na BD (certificados_dispositivo)
 │                                              8. Empacota em PKCS#12
 │ 9. Recebe o .p12 (Base64 + password) ◄──────
 │
 │ 10. Guarda em device.p12 (armazenamento interno)
 │ 11. TlsConfig.reload() → passa a apresentá-lo em TODAS as ligações
 ▼
```

A partir daqui, **cada pedido** leva o certificado no *handshake* TLS. O servidor lê o **CN**
(= `and-f63a8bc5ddea`), sabe qual é o aparelho, e grava-o nos **logs** → **rastreabilidade**.

**Ficheiros envolvidos:**
- Backend: `EnrollmentController`, `EnrollmentService`, `CertificadoDispositivo` (entidade)
- Android: `utils/DeviceEnrollment.java`, `utils/TlsConfig.java`, `utils/SessionManager.java`

---

## 5. Precisa de autorização do administrador?

### **NÃO.** É automático. 🤖

O único requisito é que o utilizador esteja **autenticado** — o endpoint `/api/enrollment` exige um
**JWT válido**. Não há aprovação manual.

**O papel do admin é *depois*, não antes:**
- 👁️ **Ver** todos os certificados emitidos
- 🚫 **Revogar** um certificado (telemóvel perdido/roubado/suspeito)

> **Modelo:** *emissão automática, controlo à posteriori.* É o mais prático.
> Se quisesses **aprovação prévia**, bastaria acrescentar um estado `PENDENTE` que o admin aprova.

---

## 6. ⚠️ Permissões: certificado ≠ permissões

### Esta é a confusão mais comum. Os certificados **NÃO dão permissões aos utilizadores.**

São **duas camadas completamente separadas**:

| | 🎫 **Certificado (mTLS)** | 🪪 **Token JWT + Role** |
|---|---|---|
| Responde a | *"**QUE DISPOSITIVO** se está a ligar?"* | *"**QUEM** é o utilizador e **O QUE PODE** fazer?"* |
| Camada | **Transporte** (TLS, no handshake) | **Aplicação** (dentro do pedido) |
| Identifica | O **aparelho** | A **pessoa** |
| Duração | Longa (2 anos) | Curta (expira, renova no login) |
| **Dá permissões?** | ❌ **Não** | ✅ **Sim** (USER / ADMIN) |
| Onde é verificado | `ClientCertFilter` | `@PreAuthorize("hasRole('ADMIN')")` |

**Exemplo concreto no código:**
```java
@PreAuthorize("hasRole('ADMIN')")   // ← olha para o ROLE do JWT, NÃO para o certificado
public ResponseEntity<List<CertificadoResponse>> listar() { ... }
```

Um utilizador com role `USER` é barrado do backoffice **pelo JWT**, mesmo tendo um certificado válido.

### Então o certificado serve para quê?

1. 🔐 **Encriptar** a ligação (TLS)
2. 🛡️ **Impedir man-in-the-middle** (a app só confia em certificados assinados pela nossa CA)
3. 🔍 **Rastreabilidade** — saber *que aparelho* fez cada ação
4. 🚫 **Bloquear dispositivos revogados** — o único "poder" do certificado sobre o acesso

---

## 7. Os poderes do administrador

| Poder | Onde (backoffice) | O que faz |
|---|---|---|
| 👁️ **Ver** todos os certificados | *Certificados* | Dispositivo, dono, série, validade, **última utilização**, estado |
| 🚫 **Revogar** um certificado | Botão *Revogar* | **Bloqueia aquele dispositivo** |
| 📥 **Descarregar a CA** | Botão *Descarregar CA* | Para instalar em novos dispositivos/browsers |
| 🔑 **Revogar sessões** de um utilizador | *Utilizadores → Atividade → Sessões* | Invalida o token JWT (força novo login) |

---

## 8. Revogação — o que acontece tecnicamente

**Sim, o admin pode "retirar a permissão" de um certificado.** É isso que o botão **Revogar** faz:

```
1. Admin clica "Revogar" no backoffice
        │
        ▼
2. POST /api/certificados/{id}/revogar
        │
        ▼
3. CertificadoService.revogar():
      estado = REVOGADO
      revogadoEm = agora
      (regista no log: CERT_REVOGADO)
        │
        ▼
4. 📱 O telemóvel faz o pedido SEGUINTE (apresenta o seu certificado)
        │
        ▼
5. ClientCertFilter lê o CN → consulta a BD → está REVOGADO?
        │
        ▼
6. ❌ HTTP 403 — "Certificado do dispositivo revogado"
```

```java
// ClientCertFilter.java
if (certificadoService.estaRevogadoPorDevice(deviceId)) {
    response.sendError(403, "Certificado do dispositivo revogado");  // 🚪 porta fechada
}
```

**Características:**
- ⚡ **Imediato** — não é preciso reiniciar o servidor
- 🔒 Vale **mesmo que o utilizador seja admin** e saiba a password
- 📄 O certificado **continua no telemóvel**, mas o servidor **recusa-o**

**Caso de uso real:** telemóvel roubado → o admin revoga → o ladrão não acede, mesmo com a app
instalada e a sessão iniciada.

---

## 9. Limitações honestas (e como fechá-las)

### A revogação é verificada na **aplicação**, não no TLS
Usamos o nosso `ClientCertFilter` (consulta a BD), e **não** listas CRL/OCSP verificadas pelo Tomcat.
O efeito prático é o mesmo (dispositivo bloqueado), mas é bom saber a diferença.

### A brecha do `client-auth=want`
O servidor está em **`client-auth=want`** (certificado **opcional**) — foi uma decisão consciente, para
o **backoffice continuar a abrir no browser** (um browser não apresenta certificado de cliente).

Consequência: alguém malicioso poderia **simplesmente não apresentar o certificado** e ainda assim usar
a API com um JWT válido.

**Como fechar completamente:**
1. ✅ **Revogar também as sessões** do utilizador no backoffice → invalida o JWT → dispositivo fica
   **totalmente** fora. *(O admin já tem este poder — é a solução mais simples.)*
2. 🔒 Tornar o certificado **obrigatório** nos endpoints da app móvel (o filtro rejeita pedidos sem certificado).
3. 🔒 Usar `client-auth=need` com uma **porta separada** para o backoffice.

> **Se o professor perguntar:** *"a revogação bloqueia o dispositivo; para bloqueio total combina-se com
> a revogação de sessões — e num cenário estrito usaríamos `client-auth=need` com uma porta separada
> para o backoffice."*

---

## 10. Resumo rápido

> 🔑 **O certificado é a chave da porta do prédio** — deixa-te ligar e diz **de que aparelho** vens.
> 🪪 **O token JWT é o crachá lá dentro** — diz **quem és** e **o que podes fazer**.
>
> Precisas dos dois — mas fazem coisas diferentes.

| Pergunta | Resposta curta |
|---|---|
| Onde está a CA? | Ficheiros no servidor: `pki/ca/` (`ca.crt` + `ca.key`) |
| O que a CA faz? | Assina o certificado do servidor e o de cada dispositivo |
| Quem atribui os certificados? | O servidor, **automaticamente**, no 1.º login (*enrollment*) |
| Precisa de aprovação do admin? | ❌ Não — a emissão é automática |
| O admin tem poder? | ✅ Sim: **ver**, **revogar**, descarregar a CA, revogar sessões |
| Pode retirar a permissão de um certificado? | ✅ Sim — **Revogar** → o dispositivo leva **403** no pedido seguinte |
| Os certificados dão permissões (USER/ADMIN)? | ❌ **Não** — isso vem do **JWT + role** |
| Onde está o certificado no telemóvel? | `device.p12` no armazenamento interno (não no APK) |
| Onde está a CA no telemóvel? | `res/raw/arquivo_ca.crt` (dentro do APK) |

---

## Ver também

- [README do Backend](../README.md) — secção 10 (Segurança PKI / mTLS e Enrollment)
- [README do Frontend Android](../../Arquivo-Digital-de-Document-rios/README.md) — secção 7 (Segurança no Cliente)
- Scripts: `gerar_pki.sh`, `emitir_dispositivo.sh`, `testar.sh`