package com.naumov.dotnetscriptsscheduler.controller;

import com.naumov.dotnetscriptsscheduler.dto.rest.DtoMapper;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import com.naumov.dotnetscriptsscheduler.dto.rest.rs.*;
import com.naumov.dotnetscriptsscheduler.model.Job;
import com.naumov.dotnetscriptsscheduler.service.JobService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping(value = "/jobs", produces = APPLICATION_JSON_VALUE)
@Validated
public class JobsController {
    private static final Logger LOGGER = LogManager.getLogger(JobsController.class);
    private final JobService jobService;
    private final DtoMapper dtoMapper;

    @Autowired
    public JobsController(JobService jobService, DtoMapper dtoMapper) {
        this.jobService = jobService;
        this.dtoMapper = dtoMapper;
    }

    @PostMapping(consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<JobCreateResponse> createJob(@Valid @RequestBody JobCreateRequest jobCreateRequest) {
        LOGGER.info("Received job creation request {}", jobCreateRequest.getRequestId());

        Optional<Job> jobOptional = jobService.findJobByRequestId(jobCreateRequest.getRequestId());
        if (jobOptional.isEmpty()) {
            Job newJob = jobService.createJob(dtoMapper.fromJobCreateRequest(jobCreateRequest));
            return ResponseEntity.status(HttpStatus.CREATED).body(dtoMapper.toJobCreateResponse(newJob));
        }

        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).body(dtoMapper.toJobCreateResponse(jobOptional.get()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobGetResponse> getJob(@NotBlank @PathVariable("id") String jobId) {
        LOGGER.info("Received job request for job {}", jobId);

        Optional<Job> jobOptional = jobService.findJob(jobId);

        return jobOptional.map(job -> ResponseEntity.status(HttpStatus.OK).body(dtoMapper.toJobGetResponse(job)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/{id}/request")
    public ResponseEntity<JobGetRequestResponse> getJobRequest(@NotBlank @PathVariable("id") String jobId) {
        LOGGER.info("Received job request request for job {}", jobId);

        // TODO
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<JobGetStatusResponse> getJobStatus(@NotBlank @PathVariable("id") String jobId) {
        LOGGER.info("Received job status request for job {}", jobId);

        // TODO
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<JobGetResultResponse> getJobResult(@NotBlank @PathVariable("id") String jobId) {
        LOGGER.info("Received job result request for job {}", jobId);

        // TODO
        return ResponseEntity.status(HttpStatus.OK).build();
    }

    @DeleteMapping("/{id}/")
    public ResponseEntity<?> deleteJob(@NotBlank @PathVariable("id") String jobId) {
        LOGGER.info("Received job deletion request for job {}", jobId);

        if (jobService.deleteJob(jobId)) {
            return ResponseEntity.status(HttpStatus.OK).build();
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            MissingServletRequestParameterException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<DefaultErrorResponse> handleBadRequestExceptions(Exception e) {
        LOGGER.info("Received bad request", e);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new DefaultErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<DefaultErrorResponse> handleAllOtherExceptions(Exception e) {
        LOGGER.error("Failed to process request", e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DefaultErrorResponse(e.getMessage()));
    }
}
