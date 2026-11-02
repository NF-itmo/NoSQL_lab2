package org.controllers;

import jakarta.validation.Valid;
import org.controllers.DTO.CreateLocationRequest;
import org.controllers.DTO.GetLocationResponse;
import org.controllers.DTO.UpdateLocationRequest;
import org.models.Location;
import org.services.LocationsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/locations")
public class LocationsController {
    private final LocationsService locationsService;

    public LocationsController(LocationsService locationsService) {
        this.locationsService = locationsService;
    }

    @GetMapping
    public List<GetLocationResponse> getLocations() {
        return locationsService.getAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetLocationResponse> createLocation(
            @RequestBody @Valid CreateLocationRequest request
    ) {
        return ResponseEntity.ok(toResponse(locationsService.create(
                request.getHall(),
                request.getRack(),
                request.getShelf()
        )));
    }

    @PutMapping("/{locationId}")
    @PreAuthorize("isAuthenticated()")
    public GetLocationResponse updateLocation(
            @PathVariable String locationId,
            @RequestBody @Valid UpdateLocationRequest request
    ) {
        return toResponse(locationsService.update(
                locationId,
                request.getHall(),
                request.getRack(),
                request.getShelf()
        ));
    }

    private GetLocationResponse toResponse(Location location) {
        return new GetLocationResponse(
                location.getId(),
                location.getHall(),
                location.getRack(),
                location.getShelf()
        );
    }
}
