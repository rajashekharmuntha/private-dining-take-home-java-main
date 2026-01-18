package com.opentable.privatedining.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "reservation")
public class ReservationConfig {

	// These act as hard coded defaults if not found in YML.
	private String openingTime = "09:00";
	private String closingTime = "22:00";
	private int defaultDurationMins = 90;

	public String getOpeningTime() {
		return openingTime;
	}

	public void setOpeningTime(String openingTime) {
		this.openingTime = openingTime;
	}

	public String getClosingTime() {
		return closingTime;
	}

	public void setClosingTime(String closingTime) {
		this.closingTime = closingTime;
	}

	public int getDefaultDurationMins() {
		return defaultDurationMins;
	}

	public void setDefaultDurationMins(int defaultDurationMins) {
		this.defaultDurationMins = defaultDurationMins;
	}
}