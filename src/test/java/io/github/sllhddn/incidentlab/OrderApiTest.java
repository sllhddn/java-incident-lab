package io.github.sllhddn.incidentlab;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class OrderApiTest extends PostgresIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void platinumCustomerGetsFifteenPercentInsteadOfANullPointerException() throws Exception {
        // Customer 4 is PLATINUM in the demo data: the tier that caused INC-01.
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":4,\"sku\":\"KB-001\",\"quantity\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Hafiz Osman"))
                .andExpect(jsonPath("$.total").value(245.65)); // 289.00 - 43.35
    }

    @Test
    void orderingMoreThanTheStockIsAConflict() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"sku\":\"MN-003\",\"quantity\":99}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Not enough stock for MN-003 (requested 99)"));
    }

    @Test
    void invalidRequestIsABadRequest() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"sku\":\"MN-003\",\"quantity\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownSkuIsNotFound() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":1,\"sku\":\"NOPE\",\"quantity\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsOrders() throws Exception {
        mvc.perform(get("/api/orders")).andExpect(status().isOk());
    }
}
