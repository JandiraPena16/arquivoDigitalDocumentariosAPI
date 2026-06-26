package com.arquivodigital.security;

/**
 * Guarda, por thread (pedido HTTP), o identificador do dispositivo (CN do certificado de cliente).
 * É preenchido pelo {@link ClientCertFilter} e lido pelo LogService para rastreabilidade.
 */
public final class DispositivoContext {

    private static final ThreadLocal<String> ATUAL = new ThreadLocal<>();

    private DispositivoContext() {}

    public static void set(String deviceId) { ATUAL.set(deviceId); }

    public static String get() { return ATUAL.get(); }

    public static void clear() { ATUAL.remove(); }
}