# -*- coding: utf-8 -*-
"""Gera o diagrama de comunicacao tempo-espaco (cliente / servidor / backoffice)."""
import math
from PIL import Image, ImageDraw, ImageFont

W, H = 1700, 1860
BG = (255, 255, 255)
INK = (26, 32, 44)
MUTED = (110, 120, 135)
BLUE = (41, 121, 255)      # cliente
GREEN = (22, 140, 90)      # servidor
PURPLE = (124, 58, 237)    # backoffice / live
RED = (220, 50, 50)        # perda
GREY = (130, 138, 150)

img = Image.new("RGB", (W, H), BG)
d = ImageDraw.Draw(img)

def F(size, bold=False, italic=False):
    name = "arialbd.ttf" if bold else ("ariali.ttf" if italic else "arial.ttf")
    try:
        return ImageFont.truetype("C:/Windows/Fonts/" + name, size)
    except Exception:
        return ImageFont.load_default()

f_title = F(34, bold=True)
f_sub   = F(19)
f_head  = F(20, bold=True)
f_msg   = F(17)
f_note  = F(17, italic=True)
f_phase = F(21, bold=True)
f_leg   = F(17)

LX = {"A": 210, "S": 690, "B": 1160, "BO": 1540}
TOP, BOT = 205, 1640

d.text((W // 2, 42), "Diagrama de Comunicação Tempo–Espaço", font=f_title, fill=INK, anchor="ma")
d.text((W // 2, 88), "Arquivo Digital de Documentários — reprodução, streaming e perda de pacotes",
       font=f_sub, fill=MUTED, anchor="ma")

heads = [
    ("A",  "CLIENTE A\n(app / emissor)", BLUE),
    ("S",  "SERVIDOR\n(Spring Boot)",    GREEN),
    ("B",  "CLIENTE B\n(espectador)",    BLUE),
    ("BO", "BACKOFFICE\n(admin)",        PURPLE),
]
for key, label, col in heads:
    x = LX[key]
    d.rounded_rectangle([x - 105, 130, x + 105, 190], radius=10, fill=col)
    d.multiline_text((x, 143), label, font=f_head, fill=(255, 255, 255),
                     anchor="ma", align="center", spacing=3)
    y = TOP
    while y < BOT:
        d.line([(x, y), (x, min(y + 9, BOT))], fill=(195, 202, 212), width=2)
        y += 16

d.text((58, TOP - 32), "TEMPO", font=f_head, fill=MUTED)
d.line([(70, TOP), (70, BOT)], fill=(210, 216, 226), width=3)
d.polygon([(70, BOT + 14), (62, BOT), (78, BOT)], fill=(210, 216, 226))


def faixa(y1, y2, titulo, cor):
    d.rounded_rectangle([95, y1, W - 30, y2], radius=12, outline=cor, width=2)
    d.rectangle([95, y1, 101, y2], fill=cor)
    d.text((118, y1 + 9), titulo, font=f_phase, fill=cor)


def seta(x1, y1, x2, y2, cor, txt, perdida=False, largura=3, cima=True, tracejada=False):
    if tracejada:
        n = 30
        for i in range(0, n, 2):
            xa = x1 + (x2 - x1) * i / n;        ya = y1 + (y2 - y1) * i / n
            xb = x1 + (x2 - x1) * (i + 1) / n;  yb = y1 + (y2 - y1) * (i + 1) / n
            d.line([(xa, ya), (xb, yb)], fill=cor, width=largura)
    else:
        d.line([(x1, y1), (x2, y2)], fill=cor, width=largura)

    if perdida:
        mx, my = (x1 + x2) / 2, (y1 + y2) / 2
        d.line([(mx - 16, my - 16), (mx + 16, my + 16)], fill=RED, width=7)
        d.line([(mx - 16, my + 16), (mx + 16, my - 16)], fill=RED, width=7)
    else:
        ang = math.atan2(y2 - y1, x2 - x1)
        L = 16
        d.polygon([(x2, y2),
                   (x2 - L * math.cos(ang - 0.42), y2 - L * math.sin(ang - 0.42)),
                   (x2 - L * math.cos(ang + 0.42), y2 - L * math.sin(ang + 0.42))], fill=cor)

    tx, ty = (x1 + x2) / 2, (y1 + y2) / 2 + (-26 if cima else 11)
    d.text((tx, ty), txt, font=f_msg, fill=(RED if perdida else cor), anchor="ma")


def nota(x, y, txt, cor):
    d.text((x, y), txt, font=f_note, fill=cor, anchor="ma")


A, S, B, BO = LX["A"], LX["S"], LX["B"], LX["BO"]

# ── 1 · AUTENTICAÇÃO ──────────────────────────────────────────────────────
faixa(215, 385, "1 · AUTENTICAÇÃO  (TLS + mTLS)", GREEN)
seta(A, 275, S, 295, BLUE,  "[1]  TLS handshake + certificado do dispositivo")
seta(S, 330, A, 350, GREEN, "[2]  JWT (token) + 200 OK", cima=False)

# ── 2 · VOD ───────────────────────────────────────────────────────────────
faixa(405, 925, "2 · STREAMING SOB DEMANDA (VOD)  —  HTTP sobre TCP  (fiável)", BLUE)
seta(A, 465, S, 483, BLUE,  "[3]  GET /api/streaming/12    Range: bytes=0-1048575")
seta(S, 520, A, 538, GREEN, "[4]  HTTP 206 Partial Content — bloco 1  (OK)", cima=False)
seta(S, 590, A, 608, GREEN, "[5]  bloco 2  (OK)", cima=False)
seta(S, 665, A, 683, GREEN, "[6]  bloco 3  ->  PACOTE PERDIDO", perdida=True, cima=False)
seta(A, 745, S, 763, RED,   "[7]  TCP: falta o ACK  ->  pede retransmissao")
seta(S, 810, A, 828, GREEN, "[8]  bloco 3 (RETRANSMITIDO)  (OK)", cima=False)
nota(W // 2 - 130, 870, ">> Reprodução CONTÍNUA — o buffer do ExoPlayer absorve o atraso.  Nada se perde.", GREEN)

# ── 3 · LIVE ──────────────────────────────────────────────────────────────
faixa(945, 1450, "3 · TEMPO REAL / LIVE  —  WebRTC: sinalização (TCP) + média (UDP/SRTP, P2P)", PURPLE)
seta(A, 1000, S, 1016, BLUE, "[9]  WebSocket: {type:\"start\"}   (inicia a live)")
seta(B, 1055, S, 1070, BLUE, "[10]  {type:\"join\"}   (espectador entra)")
seta(S, 1105, A, 1120, GREEN, "[11]  {type:\"viewer-join\"}", cima=False)
# sinalização passa PELO servidor (2 troços tracejados)
seta(A, 1160, S, 1172, GREY, "[12]  offer / ICE", tracejada=True)
seta(S, 1172, B, 1186, GREY, "reencaminha  (só sinalização)", tracejada=True)
# média P2P — não passa pelo servidor
seta(A, 1245, B, 1265, PURPLE, "[13]  VÍDEO + ÁUDIO — SRTP sobre UDP   (P2P: NÃO passa pelo servidor)", largura=6)
seta(A, 1325, B, 1345, PURPLE, "[14]  pacote  ->  PERDIDO", perdida=True, cima=False)
nota(W // 2 - 130, 1395, "!! UDP NÃO retransmite -> artefacto / frame perdido, mas SEM atraso.  Prioriza a LATÊNCIA.", RED)

# ── 4 · BACKOFFICE ────────────────────────────────────────────────────────
faixa(1470, 1640, "4 · BACKOFFICE  —  rastreabilidade", PURPLE)
seta(BO, 1525, S, 1543, PURPLE, "[15]  GET /api/logs")
seta(S, 1580, BO, 1598, GREEN,  "[16]  utilizador + DISPOSITIVO (CN do certificado) + IP + hora", cima=False)

# ── Legenda ───────────────────────────────────────────────────────────────
LY = 1690
d.rounded_rectangle([95, LY, W - 30, LY + 145], radius=12, fill=(246, 248, 252))
d.text((122, LY + 14), "COMO SE COMPORTA A PERDA DE PACOTES", font=f_head, fill=INK)
d.line([(122, LY + 52), (146, LY + 52)], fill=BLUE, width=6)
d.text((162, LY + 42), "VOD (TCP): deteta a perda e RETRANSMITE  ->  imagem sem falhas; o custo é possível buffering/atraso.",
       font=f_leg, fill=INK)
d.line([(122, LY + 88), (146, LY + 88)], fill=PURPLE, width=6)
d.text((162, LY + 78), "LIVE (UDP/SRTP): NÃO retransmite  ->  pode haver artefactos, mas mantém a latência baixa (tempo real).",
       font=f_leg, fill=INK)
d.line([(126, LY + 118), (142, LY + 134)], fill=RED, width=5)
d.line([(126, LY + 134), (142, LY + 118)], fill=RED, width=5)
d.text((162, LY + 114), "= pacote perdido na rede.", font=f_leg, fill=INK)

out = "diagrama-comunicacao.png"
img.save(out)
print("Gerado:", out, img.size)