package com.opentable.privatedining.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public class OccupancySnapshotDTO {

	@Schema(description = "The specific 30-minute time bucket", example = "2026-01-15T18:00:00")
	private LocalDateTime timestamp;

	@Schema(description = "Total headcount currently booked for this time bucket", example = "15")
	private Integer currentHeadcount;

	@Schema(description = "Total available capacity for the space(s) at this time", example = "20")
	private Integer totalCapacity;

	@Schema(description = "Percentage of capacity utilized", example = "75.0")
	private Double occupancyPercentage;

	public OccupancySnapshotDTO() {
	}

	public OccupancySnapshotDTO(LocalDateTime timestamp, Integer currentHeadcount, Integer totalCapacity,
			Double occupancyPercentage) {
		this.timestamp = timestamp;
		this.currentHeadcount = currentHeadcount;
		this.totalCapacity = totalCapacity;
		this.occupancyPercentage = occupancyPercentage;
	}

	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	public Integer getCurrentHeadcount() {
		return currentHeadcount;
	}

	public void setCurrentHeadcount(Integer currentHeadcount) {
		this.currentHeadcount = currentHeadcount;
	}

	public Integer getTotalCapacity() {
		return totalCapacity;
	}

	public void setTotalCapacity(Integer totalCapacity) {
		this.totalCapacity = totalCapacity;
	}

	public Double getOccupancyPercentage() {
		return occupancyPercentage;
	}

	public void setOccupancyPercentage(Double occupancyPercentage) {
		this.occupancyPercentage = occupancyPercentage;
	}
}