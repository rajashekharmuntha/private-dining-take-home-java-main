package com.opentable.privatedining.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.opentable.privatedining.dto.OccupancyReportDTO;
import com.opentable.privatedining.exception.BusinessRuleException;
import com.opentable.privatedining.exception.GlobalExceptionHandler;
import com.opentable.privatedining.exception.SpaceNotFoundException;
import com.opentable.privatedining.service.AnalyticsService;

@WebMvcTest({ AnalyticsController.class, GlobalExceptionHandler.class })
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    void getOccupancyReport_WhenValidRequest_ShouldReturn200() throws Exception {
        // Given
    	ObjectId restaurantId = new ObjectId();
        OccupancyReportDTO mockReport = new OccupancyReportDTO(restaurantId, null, new ArrayList<>());

        when(analyticsService.getOccupancyReport(any(), any(), any(), any(), any()))
                .thenReturn(mockReport);

        // When & Then
        mockMvc.perform(get("/v2/analytics/restaurant/{restaurantId}/occupancy", restaurantId)
                .param("start", "2026-01-20T09:00:00")
                .param("end", "2026-01-21T22:00:00")
                .param("interval", "30"))
                .andExpect(status().isOk());
    }
    
    @Test
    void getOccupancyReport_WhenExceeds31Days_ShouldReturn400() throws Exception {
        // Given
    	ObjectId restaurantId = new ObjectId();
        
        // Mocking the BusinessRuleException we added for the 31-day limit
        when(analyticsService.getOccupancyReport(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException("Report range cannot exceed 31 days."));
        when(analyticsService.getOccupancyReport(any(), any(), any(), any()))
        .thenThrow(new BusinessRuleException("Report range cannot exceed 31 days."));

        // When & Then
        mockMvc.perform(get("/v2/analytics/restaurant/{restaurantId}/occupancy", restaurantId)
                .param("start", "2026-01-01T00:00:00")
                .param("end", "2026-02-15T00:00:00") // 45 days later
                .param("interval", "60"))
                .andExpect(status().isBadRequest());
    }
    


    @Test
    void getOccupancyReport_WhenHighDensityRequest_ShouldReturn400() throws Exception {
        // Given
    	ObjectId restaurantId = new ObjectId();
        
        // Mocking the "1,000 slots" density guardrail
        when(analyticsService.getOccupancyReport(any(), any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException("The requested granularity is too high for this date range."));
        when(analyticsService.getOccupancyReport(any(), any(), any(), any()))
        .thenThrow(new BusinessRuleException("The requested granularity is too high for this date range."));

        // When & Then
        mockMvc.perform(get("/v2/analytics/restaurant/{restaurantId}/occupancy", restaurantId)
                .param("start", "2026-01-01T09:00:00")
                .param("end", "2026-01-31T22:00:00")
                .param("interval", "1")) // 1-minute interval across 31 days = too many slots
                .andExpect(status().isBadRequest());
    }
    
    
    @Test
    void getSpaceOccupancy_Success() throws Exception {
    	ObjectId restaurantId = new ObjectId();
        UUID spaceId = UUID.randomUUID();
        
        OccupancyReportDTO mockReport = new OccupancyReportDTO(restaurantId, spaceId, new ArrayList<>());

        when(analyticsService.getOccupancyReport(eq(restaurantId), eq(spaceId), any(), any(), any()))
                .thenReturn(mockReport);

        mockMvc.perform(get("/v2/analytics/restaurant/{restaurantId}/space/{spaceId}/occupancy", restaurantId, spaceId)
                .param("start", "2026-01-20T09:00:00")
                .param("end", "2026-01-20T22:00:00"))
                .andExpect(status().isOk());
    }
    
    @Test
    void getRestaurantOccupancy_Success() throws Exception {
    	ObjectId restaurantId = new ObjectId();
        
        // When spaceId is null, service should return "ALL" or null in the DTO
        OccupancyReportDTO mockReport = new OccupancyReportDTO(restaurantId, null, new ArrayList<>());
        
        when(analyticsService.getOccupancyReport(eq(restaurantId), isNull(), any(), any()))
        .thenReturn(mockReport);

        mockMvc.perform(get("/v2/analytics/restaurant/{restaurantId}/occupancy", restaurantId)
                .param("start", "2026-01-20T09:00:00")
                .param("end", "2026-01-20T22:00:00"))
                .andExpect(status().isOk());
    }
    
    @Test
    void getOccupancy_WhenRangeTooWide_ShouldReturn400() throws Exception {
    	ObjectId restaurantId = new ObjectId();

        when(analyticsService.getOccupancyReport(any(), any(), any(), any()))
                .thenThrow(new BusinessRuleException("Report range cannot exceed 31 days."));

        mockMvc.perform(get("/v2/analytics/restaurant/{restaurantId}/occupancy", restaurantId)
                .param("start", "2026-01-01T09:00:00")
                .param("end", "2026-03-01T09:00:00")) // ~60 days
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void getSpaceOccupancy_WhenSpaceNotFound_ShouldReturn404() throws Exception {
    	ObjectId restaurantId = new ObjectId();
    	UUID spaceId = UUID.randomUUID();

        when(analyticsService.getOccupancyReport(any(), any(), any(), any(), any()))
                .thenThrow(new SpaceNotFoundException("Space not found"));

        mockMvc.perform(get("/v2/analytics/restaurant/{restaurantId}/space/{spaceId}/occupancy", restaurantId, spaceId)
                .param("start", "2026-01-20T09:00:00")
                .param("end", "2026-01-20T22:00:00"))
                .andExpect(status().isNotFound());
    }
}