package org.nakrut.exception;

import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(
            ResourceNotFoundException exception,
            WebRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ProblemDetail> handleResourceConflict(
            ResourceConflictException exception,
            WebRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Resource Conflict",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            WebRequest request
    ) {
        log.warn("Database constraint violation while processing request", exception);
        return problem(
                HttpStatus.CONFLICT,
                "Data Integrity Conflict",
                "Request conflicts with existing data",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(
            Exception exception,
            WebRequest request
    ) {
        log.error("unexpected error while processing request", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred",
                request
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<FieldValidationError> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldValidationError(error.getField(), error.getDefaultMessage()))
                .toList();

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                "Request validation failed",
                request
        );
        problem.setProperty("errors", errors);

        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Malformed Request",
                "Request body is missing or malformed",
                request
        );
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String detail = "Request parameter has an invalid value";
        if (exception instanceof MethodArgumentTypeMismatchException argumentException) {
            detail = "Parameter '" + argumentException.getName() + "' has an invalid value";
        }

        ProblemDetail problem = createProblem(
                HttpStatus.BAD_REQUEST,
                "Invalid Parameter",
                detail,
                request
        );
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            WebRequest request
    ) {
        return ResponseEntity.status(status)
                .body(createProblem(status, title, detail, request));
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String title,
            String detail,
            WebRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        if (request instanceof ServletWebRequest servletWebRequest) {
            problem.setInstance(URI.create(servletWebRequest.getRequest().getRequestURI()));
        }
        return problem;
    }
}
