package io.naraway.prologue.security.exception.handler;

import io.naraway.accent.domain.message.FailureMessage;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.NONE)
public class StageExceptions {
    //
    private static final String X_RESULT = "X-Nara-Result";
    private static final String X_EXCEPTION = "X-Nara-Exception";
    private static final String X_EXCEPTION_CODE = "X-Nara-Exception-Code";
    private static final String X_EXCEPTION_MESSAGE = "X-Nara-Exception-Message";

    public static FailureMessage failureMessage(Exception e) {
        //
        if (log.isWarnEnabled()) {
            log.warn(e.getMessage(), e);
        }

        FailureMessage failureMessage = new FailureMessage(e);

        String exceptionCode = StageExceptionContext.get();
        if (exceptionCode != null) {
            failureMessage.setExceptionCode(exceptionCode);
        }

        return failureMessage;
    }

    public static HttpHeaders httpHeaders(Exception e) {
        //
        HttpHeaders httpHeaders = new HttpHeaders();

        httpHeaders.set(HttpHeaders.CONTENT_TYPE,
                String.format("%s; charset=%s", MediaType.APPLICATION_JSON_VALUE, StandardCharsets.UTF_8.name()));
        httpHeaders.set(X_RESULT, "false");
        httpHeaders.set(X_EXCEPTION, e.getClass().getSimpleName());

        String exceptionCode = StageExceptionContext.get();
        if (exceptionCode != null) {
            httpHeaders.set(X_EXCEPTION_CODE, exceptionCode);
        }

        String exceptionMessage = Optional.ofNullable(e.getMessage()).orElseGet(() -> "Unknown exception occurred");
        httpHeaders.set(X_EXCEPTION_MESSAGE,
                exceptionMessage.replaceAll("[\n\r]", ""));

        return httpHeaders;
    }

    public static void clearStageExceptionContext() {
        //
        if (StageExceptionContext.exists()) {
            StageExceptionContext.clear();
        }
    }
}
