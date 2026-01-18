package com.opentable.privatedining.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opentable.privatedining.dto.ReservationDTO;
import com.opentable.privatedining.mapper.ReservationMapper;
import com.opentable.privatedining.model.Reservation;
import com.opentable.privatedining.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/v2/reservations")
@Tag(name = "Reservation V2", description = "Enhanced Reservation API for optimized reservation.")
public class ReservationControllerV2 {

	private static final Logger logger = LoggerFactory.getLogger(ReservationService.class);
	
	private final ReservationService reservationService;
	private final ReservationMapper reservationMapper;

	public ReservationControllerV2(ReservationService reservationService, ReservationMapper reservationMapper) {
		super();
		this.reservationService = reservationService;
		this.reservationMapper = reservationMapper;
	}

	@PostMapping
	@Operation(summary = "Create optimized reservation.", description = "Create a new reservation ")
	@ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Reservation created successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ReservationDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid party size for the space capacity, out of capacity."),
            @ApiResponse(responseCode = "404", description = "Restaurant or space not found"),
            @ApiResponse(responseCode = "409", description = "Reservation time slot conflicts with existing reservation")
    })
	public ResponseEntity<ReservationDTO> createReservation(@Parameter(description = "Reservation object to be created", required = true)
	@RequestBody ReservationDTO reservationDTO) {
		Reservation reservationToBeCreated = reservationMapper.toModel(reservationDTO);
		Reservation createdReservation = reservationService.createReservationV2(reservationToBeCreated);
		logger.info("Reservation created : " + createdReservation);
		return ResponseEntity.status(HttpStatus.CREATED).body(reservationMapper.toDTO(createdReservation));
	}
}
