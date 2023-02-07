package com.naumov.dotnetscriptsscheduler.dto;

import com.naumov.dotnetscriptsscheduler.dto.rq.JobCreateRequest;
import com.naumov.dotnetscriptsscheduler.dto.rs.JobCreateResponse;
import com.naumov.dotnetscriptsscheduler.dto.rs.JobGetResponse;
import com.naumov.dotnetscriptsscheduler.model.Job;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    // -------------------------------------------- "From" mappings ----------------------------------------------------
    public Job fromJobCreateRequest(JobCreateRequest jobCreateRequest) {
        return null; // TODO
    }

    // -------------------------------------------- "To" mappings ------------------------------------------------------
    public JobCreateResponse toJobCreateResponse(Job job) {
        return null;  // TODO
    }

    public JobGetResponse toJobGetResponse(Job job) {
        return null;  // TODO
    }
}