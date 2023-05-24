package io.naraway.prologue.security.exception.handler;

import io.naraway.accent.domain.message.FailureMessage;
import io.naraway.accent.util.json.JsonSerializable;
import io.naraway.accent.util.json.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class FailureResponse implements JsonSerializable {
    //
    private boolean requestFailed;
    private FailureMessage failureMessage;

    public FailureResponse(FailureMessage failureMessage) {
        //
        this.requestFailed = true;
        this.failureMessage = failureMessage;
    }

    @Override
    public String toString() {
        //
        return toJson();
    }

    public static FailureResponse fromJson(String json) {
        //
        return JsonUtil.fromJson(json, FailureResponse.class);
    }
}
