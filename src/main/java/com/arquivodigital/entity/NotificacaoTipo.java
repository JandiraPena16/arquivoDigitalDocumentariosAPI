package com.arquivodigital.entity;

public enum NotificacaoTipo {
    NOVO_VIDEO,         // novo vídeo numa categoria que o utilizador gostou
    UPLOAD_CONCLUIDO,   // o upload do utilizador ficou pronto
    LIKE_RECEBIDO,      // alguém deu like num vídeo do utilizador
    DOWNLOAD_RECEBIDO,  // alguém descarregou um vídeo do utilizador
    LIVE                // uma transmissão ao vivo começou
}
