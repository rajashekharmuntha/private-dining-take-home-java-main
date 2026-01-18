package com.opentable.privatedining.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opentable.privatedining.dto.OccupancyReportDTO;
import com.opentable.privatedining.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v2/analytics")
@Tag(name = "Analytics", description = "Occupancy reporting for restaurant owners")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@GetMapping("/restaurant/{restaurantId}/occupancy")
	@Operation(summary = "Get occupancy trend", description = "Returns a 30 or 60 minute breakdown of occupancy for a given date range.")
	@ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OccupancyReportDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters, given date range."),
            @ApiResponse(responseCode = "404", description = "Restaurant not found"),
    })
	public ResponseEntity<OccupancyReportDTO> getOccupancyReportByRestaurant(@PathVariable String restaurantId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@RequestParam(defaultValue = "30") Integer interval) {
		ObjectId restaurantObjectId = new ObjectId(restaurantId);
		int effectiveInterval = (interval == 60) ? 60 : 30;
		OccupancyReportDTO report = analyticsService.getOccupancyReport(restaurantObjectId, start, end, effectiveInterval);
		return ResponseEntity.ok(report);
	}

	@GetMapping("/restaurant/{restaurantId}/space/{spaceId}/occupancy")
	@Operation(summary = "Get occupancy trend", description = "Returns a 30 or 60 minute breakdown of occupancy for a given date range.")
	@ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservation created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = OccupancyReportDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid parameters, given date range."),
            @ApiResponse(responseCode = "404", description = "Restaurant or space not found"),})
	public ResponseEntity<OccupancyReportDTO> getOccupancyReportByRestaurantAndSpace(
			@PathVariable(required = true) String restaurantId, @PathVariable(required = true) UUID spaceId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
			@RequestParam(defaultValue = "30") Integer interval) {
		ObjectId restaurantObjectId = new ObjectId(restaurantId);
		OccupancyReportDTO report = analyticsService.getOccupancyReport(restaurantObjectId, spaceId, start, end, interval);
		return ResponseEntity.ok(report);
	}
}