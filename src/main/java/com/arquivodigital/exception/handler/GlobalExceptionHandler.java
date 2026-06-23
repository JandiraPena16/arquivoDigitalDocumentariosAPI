package com.arquivodigital.exception.handler;

import com.arquivodigital.dto.response.ErroResponse;
import com.arquivodigital.exception.custom.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErroResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(EmailJaExisteException.class)
    public ResponseEntity<ErroResponse> handleEmailExiste(EmailJaExisteException ex, HttpServletRequest req) {
        return build(HttpStatus.CONFLICT, "Conflito", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErroResponse> handleNegocio(NegocioException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, "Erro de negócio", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(SessaoInvalidaException.class)
    public ResponseEntity<ErroResponse> handleSessao(SessaoInvalidaException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Sessão inválida", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErroResponse> handleStorage(FileStorageException ex, HttpServletRequest req) {
        log.error("Erro de armazenamento: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de armazenamento", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(CompressaoException.class)
    public ResponseEntity<ErroResponse> handleCompressao(CompressaoException ex, HttpServletRequest req) {
        log.error("Erro de compressão: {}", ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro de compressão", ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", "Email ou password incorrectos", req.getRequestURI());
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErroResponse> handleDisabled(DisabledException ex, HttpServletRequest req) {
        return build(HttpStatus.UNAUTHORIZED, "Conta desactivada", "A sua conta está desactivada. Contacte o administrador.", req.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(HttpStatus.FORBIDDEN, "Acesso negado", "Não tem permissão para aceder a este recurso", req.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> detalhes = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalhes.put(error.getField(), error.getDefaultMessage());
        }
        ErroResponse erro = ErroResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .erro("Erro de validação")
                .mensagem("Um ou mais campos são inválidos")
                .path(req.getRequestURI())
                .timestamp(LocalDateTime.now())
                .detalhes(detalhes)
                .build();
        return ResponseEntity.badRequest().body(erro);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResponse> handleMaxUpload(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Ficheiro demasiado grande", "O tamanho máximo permitido é 2GB", req.getRequestURI());
    }

    /**
     * Cliente (ex.: ExoPlayer) fechou a ligação a meio do streaming de vídeo.
     * É normal e a resposta já foi enviada — apenas ignoramos, sem poluir os logs.
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleClientAbort(AsyncRequestNotUsableException ex) {
        log.debug("Cliente fechou a ligação durante o streaming (normal): {}", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneral(Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}: {}", req.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Ocorreu um erro inesperado. Tente novamente.", req.getRequestURI());
    }

    private ResponseEntity<ErroResponse> build(HttpStatus status, String erro, String mensagem, String path) {
        return ResponseEntity.status(status).body(
                ErroResponse.builder()
                        .status(status.value())
                        .erro(erro)
                        .mensagem(mensagem)
                        .path(path)
                        .timestamp(LocalDateTime.now())
                        .build()
        );
    }
}
