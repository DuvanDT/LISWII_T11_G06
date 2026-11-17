package com.example.gymclass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Prueba de integración: REST -> puerto de entrada -> dominio -> JPA -> H2. */
@SpringBootTest
@AutoConfigureMockMvc
class GymClassApiIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String postAndGetId(String url, String json) throws Exception {
        String body = mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(body);
        return node.get("id").asText();
    }

    @Test
    void shouldRegisterScheduleBookAndCancel() throws Exception {
        String memberId = postAndGetId("/api/members",
                "{\"fullName\":\"Ana Gomez\",\"email\":\"ana.it@mail.com\",\"plan\":\"PREMIUM\"}");
        String sessionId = postAndGetId("/api/sessions",
                "{\"title\":\"Morning Yoga\",\"type\":\"YOGA\",\"startsAt\":\"2030-12-15T09:00:00\",\"endsAt\":\"2030-12-15T10:00:00\",\"capacity\":2}");

        mvc.perform(post("/api/sessions/{s}/bookings/{m}", sessionId, memberId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mvc.perform(post("/api/sessions/{s}/bookings/{m}", sessionId, memberId))
                .andExpect(status().isBadRequest());

        mvc.perform(delete("/api/sessions/{s}/bookings/{m}", sessionId, memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableSpots").value(2));
    }

    @Test
    void shouldSuspendMember() throws Exception {
        String memberId = postAndGetId("/api/members",
                "{\"fullName\":\"Luis Perez\",\"email\":\"luis.it@mail.com\",\"plan\":\"BASIC\"}");

        mvc.perform(patch("/api/members/{id}/suspend", memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void shouldReturn404WhenMemberDoesNotExist() throws Exception {
        mvc.perform(get("/api/members/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenPayloadIsInvalid() throws Exception {
        mvc.perform(post("/api/members").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"malo\",\"plan\":\"BASIC\"}"))
                .andExpect(status().isBadRequest());
    }
}
