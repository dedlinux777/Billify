package com.saas.usage.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UsageHealthController.class)
class UsageHealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getHealth_ShouldReturnSuccess() throws Exception {
        mockMvc.perform(get("/api/usage/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("usage-service"))
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
