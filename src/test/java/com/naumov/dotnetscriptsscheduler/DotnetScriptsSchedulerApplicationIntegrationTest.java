package com.naumov.dotnetscriptsscheduler;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;

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
        String jobCreateRequest = """
                {
                    "requestId": "request-0",
                    "senderId": "sender-0",
                    "payload": {
                        "script": "Console.WriteLine(\\"Hello from from script\\");",
                        "agentType": "linux-amd64-dotnet-7"
                    }
                }
                """;

        MockHttpServletRequestBuilder jobCreationRequest = post("/jobs")
                .content(jobCreateRequest.getBytes(StandardCharsets.UTF_8))
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