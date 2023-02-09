package com.naumov.dotnetscriptsscheduler.dto.rest.rs;

import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class JobGetRequestResponse {
    private JobCreateRequest request;

    @Override
    public String toString() {
        return "JobGetRequestResponse{" +
                "request=" + request +
                '}';
    }
}
