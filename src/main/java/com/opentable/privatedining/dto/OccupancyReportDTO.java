package com.opentable.privatedining.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.bson.types.ObjectId;

import io.swagger.v3.oas.annotations.media.Schema;

public class OccupancyReportDTO {

	@Schema(description = "The ID of the restaurant", example = "REST-789")
	private ObjectId restaurantId;

	@Schema(description = "The ID of the specific space, or 'ALL' for aggregate", example = "123e4567-e89b-12d3-a456-426614174000")
	private String spaceId;

	@Schema(description = "The list of 30-minute occupancy snapshots")
	private List<OccupancySnapshotDTO> intervals;

	public OccupancyReportDTO() {
	}

	public OccupancyReportDTO(ObjectId restaurantId, UUID spaceId, List<OccupancySnapshotDTO> intervals) {
		this.restaurantId = restaurantId;
		this.spaceId = Objects.isNull(spaceId) ? "ALL" : spaceId.toString();
		this.intervals = intervals;
	}

	public ObjectId getRestaurantId() {
		return restaurantId;
	}

	public void setRestaurantId(ObjectId restaurantId) {
		this.restaurantId = restaurantId;
	}

	public String getSpaceId() {
		return spaceId;
	}

	public void setSpaceId(String spaceId) {
		this.spaceId = spaceId;
	}

	public List<OccupancySnapshotDTO> getIntervals() {
		return intervals;
	}

	public void setIntervals(List<OccupancySnapshotDTO> intervals) {
		this.intervals = intervals;
	}
}