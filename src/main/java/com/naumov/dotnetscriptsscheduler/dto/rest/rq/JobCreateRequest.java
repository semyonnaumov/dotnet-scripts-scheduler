package com.naumov.dotnetscriptsscheduler.dto.rest.rq;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class JobCreateRequest {
    private String requestId;
    private String senderId;
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
