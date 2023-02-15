package com.naumov.dotnetscriptsscheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobCreateRequest;
import com.naumov.dotnetscriptsscheduler.dto.rest.rq.JobRequestPayload;
import com.naumov.dotnetscriptsscheduler.service.WorkerTypesService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@DirtiesContext
@AutoConfigureMockMvc
class DotnetScriptsSchedulerApplicationIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private WorkerTypesService workerTypesService;

    @Test
    public void contextLoads() {
    }

    @Test
    public void actuatorHealthy() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", Matchers.is("UP")));
    }

    @Test
    public void swaggerUiUp() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Swagger UI")));
    }


    @Test
    public void jobCreateResponds() throws Exception {
        JobRequestPayload payload = JobRequestPayload.builder()
                .script("Console.WriteLine(\"Hello from from script\");")
                .agentType(workerTypesService.getDefaultWorkerType())
                .build();

        JobCreateRequest jobCreateRequest = JobCreateRequest.builder()
                .requestId("request-0")
                .senderId("sender-0")
                .payload(payload)
                .build();

        MockHttpServletRequestBuilder jobCreationRequest = post("/jobs")
                .content(objectMapper.writeValueAsString(jobCreateRequest))
                .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(jobCreationRequest)
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.jobId", notNullValue()));

        // second request with similar requestId must have response status code 200
        mvc.perform(jobCreationRequest)
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.jobId", notNullValue()));
    }
}