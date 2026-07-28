package com.praveen.www.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(properties = {
        "reservation.ttl-seconds=1",
        "reservation.expiry-sweep-ms=200"
})
class ReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Reserve then confirm permanently consumes stock")
    void reserveThenConfirm_shouldConsumeStock_whenHappyPath() throws Exception {
        // Arrange
        seedProduct("SKU-IT-1", "Flash Item", 10);

        // Act
        String reservationId = reserve("SKU-IT-1", 4);
        mockMvc.perform(post("/api/v1/reservations/{id}/confirm", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // Assert
        mockMvc.perform(get("/api/v1/products/{productId}", "SKU-IT-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity", is(6)));
    }

    @Test
    @DisplayName("Reserve then cancel returns stock to available inventory")
    void reserveThenCancel_shouldReleaseStock_whenCancelled() throws Exception {
        // Arrange
        seedProduct("SKU-IT-2", "Cancel Item", 10);

        // Act
        String reservationId = reserve("SKU-IT-2", 4);
        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", reservationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELLED")));

        // Assert
        mockMvc.perform(get("/api/v1/products/{productId}", "SKU-IT-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity", is(10)));
    }

    @Test
    @DisplayName("Insufficient stock returns 409 conflict")
    void reserve_shouldReturnConflict_whenInsufficientStock() throws Exception {
        // Arrange
        seedProduct("SKU-IT-3", "Scarce Item", 2);

        // Act & Assert
        mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"SKU-IT-3","quantity":5}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INSUFFICIENT_STOCK")));
    }

    @Test
    @DisplayName("Double confirm of the same reservation returns 409")
    void confirm_shouldReturnConflict_whenAlreadyConfirmed() throws Exception {
        // Arrange
        seedProduct("SKU-IT-4", "Double Confirm", 5);
        String reservationId = reserve("SKU-IT-4", 2);
        mockMvc.perform(post("/api/v1/reservations/{id}/confirm", reservationId))
                .andExpect(status().isOk());

        // Act & Assert
        mockMvc.perform(post("/api/v1/reservations/{id}/confirm", reservationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_RESERVATION_STATE")));
    }

    @Test
    @DisplayName("Double cancel of the same reservation returns 409")
    void cancel_shouldReturnConflict_whenAlreadyCancelled() throws Exception {
        // Arrange
        seedProduct("SKU-IT-5", "Double Cancel", 5);
        String reservationId = reserve("SKU-IT-5", 2);
        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", reservationId))
                .andExpect(status().isOk());

        // Act & Assert
        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", reservationId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_RESERVATION_STATE")));
    }

    @Test
    @DisplayName("Expired reservation is released by the scheduled sweep")
    void expirySweep_shouldReleaseStock_whenReservationPastTtl() throws Exception {
        // Arrange
        seedProduct("SKU-IT-6", "TTL Item", 8);
        reserve("SKU-IT-6", 3);

        mockMvc.perform(get("/api/v1/products/{productId}", "SKU-IT-6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity", is(5)));

        // Act — wait past TTL (1s) plus a sweep interval
        Thread.sleep(1600);

        // Assert — poll briefly for sweep to release stock
        boolean released = false;
        for (int i = 0; i < 10; i++) {
            MvcResult result = mockMvc.perform(get("/api/v1/products/{productId}", "SKU-IT-6"))
                    .andExpect(status().isOk())
                    .andReturn();
            int available = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("availableQuantity").asInt();
            if (available == 8) {
                released = true;
                break;
            }
            Thread.sleep(250);
        }
        assertTrue(released, "Expected expiry sweep to restore available quantity to 8");
    }

    @Test
    @DisplayName("Concurrent reservations never oversell available stock")
    void reserve_shouldNeverOversell_whenConcurrentRequestsCompete() throws Exception {
        // Arrange
        seedProduct("SKU-IT-7", "Concurrent Item", 10);
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictCount = new AtomicInteger();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"productId":"SKU-IT-7","quantity":1}
                                        """))
                        .andReturn();
                int status = result.getResponse().getStatus();
                if (status == 201) {
                    successCount.incrementAndGet();
                } else if (status == 409) {
                    conflictCount.incrementAndGet();
                }
                return null;
            });
        }

        // Act
        List<Future<Void>> futures = executor.invokeAll(tasks);
        for (Future<Void> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Assert
        assertEquals(10, successCount.get());
        assertEquals(10, conflictCount.get());
        mockMvc.perform(get("/api/v1/products/{productId}", "SKU-IT-7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity", is(0)));
    }

    @Test
    @DisplayName("Unknown reservation id returns 404")
    void confirm_shouldReturnNotFound_whenReservationMissing() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/reservations/{id}/confirm", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESERVATION_NOT_FOUND")));
    }

    private void seedProduct(String productId, String name, int quantity) throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","name":"%s","availableQuantity":%d}
                                """.formatted(productId, name, quantity)))
                .andExpect(status().isCreated());
    }

    private String reserve(String productId, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":"%s","quantity":%d}
                                """.formatted(productId, quantity)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }
}
