package com.opentable.privatedining.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

public class SpaceDTO {

    @Schema(description = "Unique identifier for the space", example = "123e4567-e89b-12d3-a456-426614174000", type = "string")
    private UUID id;

    @Schema(description = "Name of the space", example = "Private Dining Room A")
    private String name;

    @Schema(description = "Minimum capacity for the space", example = "2")
    private Integer minCapacity;

    @Schema(description = "Maximum capacity for the space", example = "12")
    private Integer maxCapacity;
    
    @Schema(description = "Time slot duration in minutes for the space", example = "60")
    private Integer slotDurationMins;
    
    private LocalDateTime endTime;

    public SpaceDTO() {}

    public SpaceDTO(String name, Integer minCapacity, Integer maxCapacity) {
        this.name = name;
        this.minCapacity = minCapacity;
        this.maxCapacity = maxCapacity;
    }

    public SpaceDTO(UUID id, String name, Integer minCapacity, Integer maxCapacity) {
        this.id = id;
        this.name = name;
        this.minCapacity = minCapacity;
        this.maxCapacity = maxCapacity;
    }
    
    
    public SpaceDTO(String name, Integer minCapacity, Integer maxCapacity, Integer slotDurationMins) {
    	this(name, minCapacity, maxCapacity);
		this.slotDurationMins = slotDurationMins;
	}

    public SpaceDTO(UUID id, String name, Integer minCapacity, Integer maxCapacity, Integer slotDurationMins) {
    	this(id, name, minCapacity, maxCapacity);
		this.slotDurationMins = slotDurationMins;
	}
    

	public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMinCapacity() {
        return minCapacity;
    }

    public void setMinCapacity(Integer minCapacity) {
        this.minCapacity = minCapacity;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

	public Integer getSlotDurationMins() {
		return slotDurationMins;
	}

	public void setSlotDurationMins(Integer slotDurationMins) {
		this.slotDurationMins = slotDurationMins;
	}
}