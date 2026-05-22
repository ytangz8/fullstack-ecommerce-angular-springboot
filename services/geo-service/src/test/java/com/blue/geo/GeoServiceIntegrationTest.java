package com.blue.geo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-context integration tests for the geo-service.
 *
 * The service already uses an H2 in-memory database as its sole datasource
 * (see application.yml: jdbc:h2:mem:geodb), so no extra @TestPropertySource
 * is needed — this IS the dedicated embedded database.
 *
 * All seed data (4 countries, 21 states) is loaded via src/main/resources/data.sql
 * at context startup and torn down automatically when the context closes.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GeoServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ─── Countries collection ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/countries")
    class GetCountries {

        @Test
        @DisplayName("returns 200 with all 4 seeded countries")
        void returns200WithAllCountries() throws Exception {
            mockMvc.perform(get("/api/countries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.countries").isArray())
                    .andExpect(jsonPath("$._embedded.countries", hasSize(4)));
        }

        @Test
        @DisplayName("every country carries the fields the checkout form requires (id, code, name)")
        void everyCountryHasRequiredCheckoutFields() throws Exception {
            mockMvc.perform(get("/api/countries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.countries[*].id",   everyItem(notNullValue())))
                    .andExpect(jsonPath("$._embedded.countries[*].code", everyItem(notNullValue())))
                    .andExpect(jsonPath("$._embedded.countries[*].name", everyItem(notNullValue())));
        }

        @Test
        @DisplayName("returns exactly the four seeded country codes")
        void containsExactlySeededCountryCodes() throws Exception {
            mockMvc.perform(get("/api/countries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.countries[*].code",
                            containsInAnyOrder("US", "CA", "IN", "BR")));
        }
    }

    // ─── Single country ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/countries/{id}")
    class GetCountryById {

        @Test
        @DisplayName("returns 200 with correct payload for a known id")
        void returns200ForExistingCountry() throws Exception {
            mockMvc.perform(get("/api/countries/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.code").value("US"))
                    .andExpect(jsonPath("$.name").value("United States"));
        }

        @Test
        @DisplayName("returns 404 for a non-existent id — boundary test")
        void returns404ForNonExistentCountry() throws Exception {
            mockMvc.perform(get("/api/countries/999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ─── States search ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/states/search/findByCountryCode")
    class FindStatesByCountryCode {

        @Test
        @DisplayName("returns 200 with 10 US states including California")
        void returnsUsStates() throws Exception {
            mockMvc.perform(get("/api/states/search/findByCountryCode").param("code", "US"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.states").isArray())
                    .andExpect(jsonPath("$._embedded.states", hasSize(10)))
                    .andExpect(jsonPath("$._embedded.states[*].id",   everyItem(notNullValue())))
                    .andExpect(jsonPath("$._embedded.states[*].name", everyItem(notNullValue())))
                    .andExpect(jsonPath("$._embedded.states[?(@.name == 'California')]").exists());
        }

        @Test
        @DisplayName("returns 200 with 4 Canadian provinces including Ontario")
        void returnsCanadaProvinces() throws Exception {
            mockMvc.perform(get("/api/states/search/findByCountryCode").param("code", "CA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.states", hasSize(4)))
                    .andExpect(jsonPath("$._embedded.states[?(@.name == 'Ontario')]").exists());
        }

        @Test
        @DisplayName("returns 200 with empty states array for an unknown country code — boundary test")
        void returnsEmptyResultForUnknownCode() throws Exception {
            // Spring Data REST returns _embedded.states: [] (empty array) when no rows match
            mockMvc.perform(get("/api/states/search/findByCountryCode").param("code", "XX"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$._embedded.states").isArray())
                    .andExpect(jsonPath("$._embedded.states", hasSize(0)));
        }
    }

    // ─── Write-method lockdown ───────────────────────────────────────────────

    @Nested
    @DisplayName("Write methods are disabled (GeoDataRestConfig)")
    class WriteMethodsDisabled {

        @Test
        @DisplayName("POST /api/countries returns 405 — countries are read-only")
        void postToCountriesReturns405() throws Exception {
            mockMvc.perform(post("/api/countries")
                            .contentType("application/json")
                            .content("{\"code\":\"FR\",\"name\":\"France\"}"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("DELETE /api/countries/{id} returns 405 — countries are read-only")
        void deleteCountryReturns405() throws Exception {
            mockMvc.perform(delete("/api/countries/1"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("PUT /api/states/{id} returns 405 — states are read-only")
        void putStateReturns405() throws Exception {
            mockMvc.perform(put("/api/states/1")
                            .contentType("application/json")
                            .content("{\"name\":\"Hacked\"}"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }
}
