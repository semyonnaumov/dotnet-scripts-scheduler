package com.naumov.dotnetscriptsscheduler.dto.rest.rq;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Valid
public final class JobCreateRequest {
    @NotBlank
    private String requestId;
    @NotBlank
    private String senderId;
    @NotNull
    @Valid
    private JobRequestPayload payload;

    @Override
    public String toString() {
        return "JobCreateRequest{" +
                "requestId='" + requestId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", payload=" + payload +
                '}';
    }
}
