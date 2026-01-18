package com.opentable.privatedining.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "restaurants")
public class Restaurant {

	@Id
	private ObjectId id;
	private String name;
	private String address;
	private String cuisineType;
	private Integer capacity;

	private List<Space> spaces;

	private LocalTime openingTime;
	private LocalTime closingTime;
	private LocalDateTime lastReservationAt;
	
	@Version
    private Long version;

	public Restaurant() {
		this.spaces = new ArrayList<>();
	}

	public Restaurant(String name, String address, String cuisineType, Integer capacity) {
		this.name = name;
		this.address = address;
		this.cuisineType = cuisineType;
		this.capacity = capacity;
		this.spaces = new ArrayList<>();
	}
	
	public Restaurant(String name, String address, String cuisineType, Integer capacity
			, LocalTime openingTime, LocalTime closingTime, LocalDateTime lastReservationAt) {
		this(name, address, cuisineType, capacity);
		this.openingTime = openingTime;
		this.closingTime = closingTime;
		this.lastReservationAt = lastReservationAt;
	}

	public ObjectId getId() {
		return id;
	}

	public void setId(ObjectId id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCuisineType() {
		return cuisineType;
	}

	public void setCuisineType(String cuisineType) {
		this.cuisineType = cuisineType;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}

	public List<Space> getSpaces() {
		return spaces;
	}

	public void setSpaces(List<Space> spaces) {
		this.spaces = spaces;
	}

	public LocalTime getOpeningTime() {
		return openingTime;
	}

	public void setOpeningTime(LocalTime openingTime) {
		this.openingTime = openingTime;
	}

	public LocalTime getClosingTime() {
		return closingTime;
	}

	public void setClosingTime(LocalTime closingTime) {
		this.closingTime = closingTime;
	}

	public LocalDateTime getLastReservationAt() {
		return lastReservationAt;
	}

	public void setLastReservationAt(LocalDateTime lastReservationAt) {
		this.lastReservationAt = lastReservationAt;
	}
	
	

}