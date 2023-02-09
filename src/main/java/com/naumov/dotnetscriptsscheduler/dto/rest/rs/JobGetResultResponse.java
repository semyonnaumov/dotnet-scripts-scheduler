package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class JobGetResultResponse {
    private JobResult result;

    @Override
    public String toString() {
        return "JobGetResultResponse{" +
                "result=" + result +
                '}';
    }
}
