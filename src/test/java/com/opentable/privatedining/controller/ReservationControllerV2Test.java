package com.opentable.privatedining.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opentable.privatedining.dto.ReservationDTO;
import com.opentable.privatedining.exception.BusinessRuleException;
import com.opentable.privatedining.exception.GlobalExceptionHandler;
import com.opentable.privatedining.exception.InsufficientCapacityException;
import com.opentable.privatedining.exception.InvalidPartySizeException;
import com.opentable.privatedining.exception.InvalidReservationTimeException;
import com.opentable.privatedining.exception.SpaceNotFoundException;
import com.opentable.privatedining.mapper.ReservationMapper;
import com.opentable.privatedining.model.Reservation;
import com.opentable.privatedining.service.ReservationService;

@WebMvcTest({ ReservationControllerV2.class, GlobalExceptionHandler.class })
class ReservationControllerV2Test {

	@Autowired
	private MockMvc mockMvc;

	@MockBean
	private ReservationService reservationService;

	@MockBean
	private ReservationMapper reservationMapper;

	@Autowired
	private ObjectMapper objectMapper;


	@Test
	void createReservationV2_WhenValidReservation_ShouldReturnCreatedReservation() throws Exception {
		// Given
		ReservationDTO inputReservationDTO = createTestReservationDTO("customer@example.com", 4);
		Reservation reservation = createTestReservation("customer@example.com", 4);
		Reservation savedReservation = createTestReservation("customer@example.com", 4);
		savedReservation.setId(new ObjectId());
		ReservationDTO savedReservationDTO = createTestReservationDTO("customer@example.com", 4);
		savedReservationDTO.setId(savedReservation.getId().toString());

		when(reservationMapper.toModel(any(ReservationDTO.class))).thenReturn(reservation);
		when(reservationService.createReservationV2(any(Reservation.class))).thenReturn(savedReservation);
		when(reservationMapper.toDTO(any(Reservation.class))).thenReturn(savedReservationDTO);

		// When & Then
		mockMvc.perform(post("/v2/reservations").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(inputReservationDTO))).andExpect(status().isCreated())
				.andExpect(content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.customerEmail").value("customer@example.com"))
				.andExpect(jsonPath("$.partySize").value(4));
	}

	@Test
	void createReservationV2_WhenInvalidPartySize_ShouldReturn400() throws Exception {
		// Given
		ReservationDTO inputReservationDTO = createTestReservationDTO("invalid@example.com", 4);
		Reservation reservation = createTestReservation("invalid@example.com", 4);

		when(reservationMapper.toModel(any(ReservationDTO.class))).thenReturn(reservation);
		when(reservationService.createReservationV2(any(Reservation.class)))
				.thenThrow(new InvalidPartySizeException(4, 2, 3));

		// When & Then
		mockMvc.perform(post("/v2/reservations").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(inputReservationDTO))).andExpect(status().isBadRequest());
	}
	
	@Test
    void createReservationV2_WhenDateInPast_ShouldReturn400() throws Exception {
        // Given
        ReservationDTO inputDTO = createTestReservationDTO("past@example.com", 2);
        // Set time to yesterday
        inputDTO.setStartTime(LocalDateTime.now().minusDays(1)); 

        when(reservationMapper.toModel(any(ReservationDTO.class))).thenReturn(new Reservation());
        
        // Mock the service throwing the specific BusinessRuleException
        when(reservationService.createReservationV2(any(Reservation.class)))
                .thenThrow(new BusinessRuleException("Reservation must be in the future."));

        // When & Then
        mockMvc.perform(post("/v2/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isBadRequest());
    }
	
	@Test
    void createReservationV2_WhenSpaceNotFound_ShouldReturn404() throws Exception {
        // Given
        ReservationDTO inputDTO = createTestReservationDTO("missing@example.com", 2);
    

        when(reservationMapper.toModel(any(ReservationDTO.class))).thenReturn(new Reservation());
        
        // Throwing the specific Domain Exception
        when(reservationService.createReservationV2(any(Reservation.class)))
                .thenThrow(new SpaceNotFoundException("Space not found."));

        // When & Then
        mockMvc.perform(post("/v2/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isNotFound());
    }
	
	@Test
    void createReservationV2_WhenNoStartTime_ShouldReturn400() throws Exception {
        // Given
        ReservationDTO inputDTO = createTestReservationDTO("null@example.com", 2);
        inputDTO.setStartTime(null); // Explicitly null

        // This test specifically checks if your Bean Validation (@NotNull) works
        // or if your Service catches it. 
        when(reservationMapper.toModel(any(ReservationDTO.class))).thenReturn(new Reservation());
        when(reservationService.createReservationV2(any(Reservation.class)))
                .thenThrow(new BusinessRuleException("Reservation start time is required."));

        // When & Then
        mockMvc.perform(post("/v2/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isBadRequest());
    }
	
	@Test
    void createReservationV2_WhenRestaurantIsClosed_ShouldReturn400() throws Exception {
        // Given
        ReservationDTO inputDTO = createTestReservationDTO("closed@example.com", 2);
        
        when(reservationMapper.toModel(any(ReservationDTO.class))).thenReturn(new Reservation());
        when(reservationService.createReservationV2(any(Reservation.class)))
                .thenThrow(new InvalidReservationTimeException("The restaurant is closed at the selected time."));

        // When & Then
        mockMvc.perform(post("/v2/reservations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inputDTO)))
                .andExpect(status().isBadRequest());
    }
	
	@Test
	void createReservationV2_WhenInsufficientCapacity_ShouldReturn409() throws Exception {
	    // Given
	    ReservationDTO inputDTO = createTestReservationDTO("no-room@example.com", 20); // Large party
	    
	    // Stubbing the mapper
	    when(reservationMapper.toModel(any(ReservationDTO.class))).thenReturn(new Reservation());
	    
	    // Stubbing the service to throw the specific capacity exception
	    // Assuming InsufficientCapacityException takes (requested, available)
	    when(reservationService.createReservationV2(any(Reservation.class)))
	            .thenThrow(new InsufficientCapacityException("Reservation conflict")); 

	    // When & Then
	    mockMvc.perform(post("/v2/reservations")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(inputDTO)))
	            .andExpect(status().isConflict()); // 409 Conflict
	}
	

	private Reservation createTestReservation(String customerEmail, int partySize) {
		Reservation reservation = new Reservation();
		reservation.setCustomerEmail(customerEmail);
		reservation.setPartySize(partySize);
		reservation.setRestaurantId(new ObjectId());
		reservation.setSpaceId(UUID.randomUUID());
		reservation.setStartTime(LocalDateTime.now().plusDays(1));
		reservation.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
		reservation.setStatus("CONFIRMED");
		return reservation;
	}

	private ReservationDTO createTestReservationDTO(String customerEmail, int partySize) {
		ReservationDTO reservationDTO = new ReservationDTO();
		reservationDTO.setCustomerEmail(customerEmail);
		reservationDTO.setPartySize(partySize);
		reservationDTO.setRestaurantId(new ObjectId().toString());
		reservationDTO.setSpaceId(UUID.randomUUID());
		reservationDTO.setStartTime(LocalDateTime.now().plusDays(1));
		reservationDTO.setEndTime(LocalDateTime.now().plusDays(1).plusHours(2));
		reservationDTO.setStatus("CONFIRMED");
		return reservationDTO;
	}
}