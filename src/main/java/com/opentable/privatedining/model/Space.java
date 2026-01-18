package com.opentable.privatedining.model;

import java.util.UUID;

import org.springframework.data.annotation.Version;

public class Space {

	private UUID id;
	private String name;
	private Integer minCapacity;
	private Integer maxCapacity;

	// Minimum slot duration in Minutes.
	private Integer slotDurationMins;
	
	@Version
    private Long version;

	public Space() {
		this.id = UUID.randomUUID();
	}

	public Space(String name, Integer minCapacity, Integer maxCapacity) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.minCapacity = minCapacity;
		this.maxCapacity = maxCapacity;
	}
	
	public Space(String name, Integer minCapacity, Integer maxCapacity, Integer slotDurationMins) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.minCapacity = minCapacity;
		this.maxCapacity = maxCapacity;
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
