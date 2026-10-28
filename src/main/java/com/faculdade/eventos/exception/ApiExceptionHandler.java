package com.faculdade.eventos.exception;

import org.hibernate.query.sqm.PathElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ProblemDetail> tratarRequisicaoInvalida(RequisicaoInvalidaException exception) {
        return criarResposta(HttpStatus.BAD_REQUEST, "Requisição inválida", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> tratarParametroInvalido(MethodArgumentTypeMismatchException exception) {
        return criarResposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                "O parâmetro '" + exception.getName() + "' possui formato ou valor inválido.");
    }

    // Ordenar por um campo que não existe (ex.: ?sort=abc) faz o Hibernate rejeitar a consulta.
    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ProblemDetail> tratarOrdenacaoInvalida(InvalidDataAccessApiUsageException exception) {
        if (exception.getMostSpecificCause() instanceof PathElementException) {
            return criarResposta(HttpStatus.BAD_REQUEST, "Parâmetro inválido",
                    "O parâmetro 'sort' usa um campo que não existe.");
        }
        return tratarErroInesperado(exception);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> tratarErroInesperado(Exception exception) {
        // Preserva o status e os cabeçalhos dos erros HTTP do Spring, como o Allow do 405,
        // trocando a mensagem padrão em inglês por uma em português.
        if (exception instanceof ErrorResponse erroHttp) {
            return ResponseEntity.status(erroHttp.getStatusCode())
                    .headers(erroHttp.getHeaders()).body(problemaHttp(erroHttp.getStatusCode()));
        }
        log.error("Erro inesperado ao processar requisição", exception);
        return criarResposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Não foi possível concluir a operação. Tente novamente mais tarde.");
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException exception
    ) {
        return criarResposta(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                exception.getMessage()
        );
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ProblemDetail> tratarConflito(
            ConflitoException exception
    ) {
        return criarResposta(
                HttpStatus.CONFLICT,
                "Conflito",
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> tratarValidacao(
            MethodArgumentNotValidException exception
    ) {
        Map<String, String> campos = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(
                        erro.getField(),
                        erro.getDefaultMessage()
                ));

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Um ou mais campos estão inválidos"
        );

        problema.setTitle("Erro de validação");
        problema.setProperty("campos", campos);

        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> tratarIntegridade(
            DataIntegrityViolationException exception
    ) {
        return criarResposta(
                HttpStatus.CONFLICT,
                "Conflito de dados",
                "A operação viola uma restrição do banco de dados. "
                        + "Verifique registros duplicados ou vínculos existentes."
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> tratarCorpoInvalido(
            HttpMessageNotReadableException exception
    ) {
        return criarResposta(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido",
                "Envie um JSON válido, com os tipos e formatos esperados."
        );
    }

    private ProblemDetail problemaHttp(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> criarProblema(status, "Requisição inválida",
                    "Verifique os parâmetros e o corpo enviados.");
            case 404 -> criarProblema(status, "Recurso não encontrado",
                    "O endereço solicitado não existe.");
            case 405 -> criarProblema(status, "Método não permitido",
                    "O método HTTP usado não é aceito neste endereço.");
            case 406 -> criarProblema(status, "Formato não aceito",
                    "A API responde somente em JSON.");
            case 415 -> criarProblema(status, "Tipo de conteúdo não suportado",
                    "Envie o corpo da requisição em JSON (application/json).");
            default -> criarProblema(status, "Erro na requisição",
                    "Não foi possível processar a requisição.");
        };
    }

    private ResponseEntity<ProblemDetail> criarResposta(
            HttpStatus status,
            String titulo,
            String detalhe
    ) {
        return ResponseEntity.status(status).body(criarProblema(status, titulo, detalhe));
    }

    private ProblemDetail criarProblema(
            HttpStatusCode status,
            String titulo,
            String detalhe
    ) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                status,
                detalhe
        );

        problema.setTitle(titulo);

        return problema;
    }
}
