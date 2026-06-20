package com.arquivodigital.exception.custom;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String mensagem) {
        super(mensagem);
    }
    public FileStorageException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
