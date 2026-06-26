package com.arquivodigital.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class EnrollmentResponse {
    /** CN atribuído ao dispositivo. */
    private String deviceId;
    /** Certificado + chave do dispositivo, em PKCS#12, codificado em Base64. */
    private String p12Base64;
    /** Password do PKCS#12 (a app guarda-a para carregar o certificado). */
    private String password;
}