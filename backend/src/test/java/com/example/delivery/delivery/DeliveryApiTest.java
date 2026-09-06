package com.example.delivery.delivery;

import com.example.delivery.geocoding.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired DeliveryRepository deliveries;
    @Autowired GeocodeCacheRepository cache;
    @MockitoBean GeocodingProvider provider;

    @BeforeEach void clearDatabase() { deliveries.deleteAll(); cache.deleteAll(); }

    private String body(String ref, String address, String status) throws Exception {
        return json.writeValueAsString(new DeliveryRequest(ref, address, status == null ? null : DeliveryStatus.valueOf(status)));
    }

    @Test void completeWorkflowIncludesCacheOutageFiltersUpdateRetryAndDelete() throws Exception {
        var origin = new GeocodingProvider.Place(new Coordinates(-6.1751, 106.8650), "Monas, Jakarta");
        when(provider.lookup("monas jakarta")).thenReturn(Optional.of(origin));
        var created = mvc.perform(post("/api/deliveries").contentType(MediaType.APPLICATION_JSON)
                .content(body("DO-001", "Monas Jakarta", null)))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.zone").value("LOCAL"))
                .andExpect(jsonPath("$.distanceKm").value(0))
                .andExpect(jsonPath("$.estimate.costIdr").value(25000))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.geocodeSource").value("NOMINATIM"))
                .andReturn();
        long id = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(post("/api/deliveries").contentType(MediaType.APPLICATION_JSON)
                .content(body("DO-002", " MONAS   Jakarta ", null)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.geocodeSource").value("CACHE"));
        verify(provider, times(1)).lookup("monas jakarta");
        assertThat(cache.count()).isEqualTo(1);

        when(provider.lookup("surabaya")).thenThrow(new ProviderUnavailableException("503"));
        var unresolved = mvc.perform(post("/api/deliveries").contentType(MediaType.APPLICATION_JSON)
                .content(body("DO-003", "Surabaya", null)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.zone").value("UNKNOWN"))
                .andExpect(jsonPath("$.distanceKm").isEmpty()).andExpect(jsonPath("$.estimate").isEmpty()).andReturn();
        long unknownId = json.readTree(unresolved.getResponse().getContentAsString()).get("id").asLong();
        clearInvocations(provider);
        mvc.perform(get("/api/deliveries")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/deliveries").param("zone", "LOCAL").param("status", "PLANNED"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
        mvc.perform(put("/api/deliveries/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body("DO-001", "Monas Jakarta", "IN_TRANSIT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.zone").value("LOCAL"));
        verifyNoInteractions(provider);

        doReturn(Optional.of(new GeocodingProvider.Place(new Coordinates(-7.2575, 112.7521), "Surabaya")))
                .when(provider).lookup("surabaya");
        mvc.perform(post("/api/deliveries/" + unknownId + "/geocode"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.zone").value("LONG_HAUL"));
        mvc.perform(put("/api/deliveries/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body("DO-001", "Surabaya", "IN_TRANSIT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.zone").value("LONG_HAUL"))
                .andExpect(jsonPath("$.geocodeSource").value("CACHE"));
        when(provider.lookup("unavailable destination")).thenThrow(new ProviderUnavailableException("503"));
        mvc.perform(put("/api/deliveries/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body("DO-001", "Unavailable destination", "IN_TRANSIT")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.zone").value("UNKNOWN"))
                .andExpect(jsonPath("$.destLat").isEmpty()).andExpect(jsonPath("$.destLng").isEmpty())
                .andExpect(jsonPath("$.distanceKm").isEmpty()).andExpect(jsonPath("$.geocodedAt").isEmpty());
        mvc.perform(delete("/api/deliveries/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/deliveries/" + id)).andExpect(status().isNotFound());
    }

    @Test void validatesInputAndMissingIdsWithoutCallingProvider() throws Exception {
        mvc.perform(post("/api/deliveries").contentType(MediaType.APPLICATION_JSON).content(body(" ", "", null)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.orderRef").exists())
                .andExpect(jsonPath("$.errors.destAddress").exists());
        mvc.perform(post("/api/deliveries").contentType(MediaType.APPLICATION_JSON).content(body("X".repeat(51), "Monas", null)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/deliveries").param("zone", "INVALID")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/deliveries/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/deliveries/999999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/deliveries/999999").contentType(MediaType.APPLICATION_JSON).content(body("DO-001", "Monas", null)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/deliveries/999999")).andExpect(status().isNotFound());
        mvc.perform(post("/api/deliveries/999999/geocode")).andExpect(status().isNotFound());
        verifyNoInteractions(provider);
    }
}
