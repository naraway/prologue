package io.naraway.prologue.security.exception.handler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.NoSuchElementException;

@RestControllerAdvice
public class StageExceptionHandler {
    //
    @ExceptionHandler(NoSuchElementException.class)
    protected ResponseEntity<FailureResponse> handleNoSuchElementException(NoSuchElementException e) {
        //
        FailureResponse failureResponse = new FailureResponse(StageExceptions.failureMessage(e));
        HttpHeaders exceptionHttpHeader = StageExceptions.httpHeaders(e);
        StageExceptions.clearStageExceptionContext();

        return new ResponseEntity<>(failureResponse, exceptionHttpHeader, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<FailureResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        //
        FailureResponse failureResponse = new FailureResponse(StageExceptions.failureMessage(e));
        HttpHeaders exceptionHttpHeader = StageExceptions.httpHeaders(e);
        StageExceptions.clearStageExceptionContext();

        return new ResponseEntity<>(failureResponse, exceptionHttpHeader, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<FailureResponse> handleAccessDeniedException(AccessDeniedException e) {
        //
        FailureResponse failureResponse = new FailureResponse(StageExceptions.failureMessage(e));
        HttpHeaders exceptionHttpHeader = StageExceptions.httpHeaders(e);
        StageExceptions.clearStageExceptionContext();

        return new ResponseEntity<>(failureResponse, exceptionHttpHeader, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<FailureResponse> handleAuthenticationException(AuthenticationException e) {
        //
        FailureResponse failureResponse = new FailureResponse(StageExceptions.failureMessage(e));
        HttpHeaders exceptionHttpHeader = StageExceptions.httpHeaders(e);
        StageExceptions.clearStageExceptionContext();

        return new ResponseEntity<>(failureResponse, exceptionHttpHeader, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalStateException.class)
    protected ResponseEntity<FailureResponse> handleIllegalStateException(IllegalStateException e) {
        //
        FailureResponse failureResponse = new FailureResponse(StageExceptions.failureMessage(e));
        HttpHeaders exceptionHttpHeader = StageExceptions.httpHeaders(e);
        StageExceptions.clearStageExceptionContext();

        return new ResponseEntity<>(failureResponse, exceptionHttpHeader, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(JwtException.class)
    protected ResponseEntity<FailureResponse> handleJwtException(Exception e) {
        //
        FailureResponse failureResponse = new FailureResponse(StageExceptions.failureMessage(e));
        HttpHeaders exceptionHttpHeader = StageExceptions.httpHeaders(e);
        StageExceptions.clearStageExceptionContext();

        return new ResponseEntity<>(failureResponse, exceptionHttpHeader, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SecurityException.class)
    protected ResponseEntity<FailureResponse> handleSecurityException(Exception e) {
        //
        FailureResponse failureResponse = new FailureResponse(StageExceptions.failureMessage(e));
        HttpHeaders exceptionHttpHeader = StageExceptions.httpHeaders(e);
        StageExceptions.clearStageExceptionContext();

        return new ResponseEntity<>(failureResponse, exceptionHttpHeader, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler
    protected ResponseEntity<FailureResponse> handleException(Exception e) {
        //
        FailureResponse failureResponse = new FailureResponse(StageExceptions.failureMessage(e));
        HttpHeaders exceptionHttpHeader = StageExceptions.httpHeaders(e);
        StageExceptions.clearStageExceptionContext();

        return new ResponseEntity<>(failureResponse, exceptionHttpHeader, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
