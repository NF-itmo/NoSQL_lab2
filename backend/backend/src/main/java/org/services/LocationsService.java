package org.services;

import org.exceptions.NotFoundException;
import org.models.Location;
import org.repository.BooksSearchCacheRepository;
import org.repository.LocationsFirestoreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class LocationsService {
    private final LocationsFirestoreRepository locationsRepository;
    private final BooksSearchCacheRepository booksSearchCacheRepository;

    public LocationsService(
            LocationsFirestoreRepository locationsRepository,
            BooksSearchCacheRepository booksSearchCacheRepository
    ) {
        this.locationsRepository = locationsRepository;
        this.booksSearchCacheRepository = booksSearchCacheRepository;
    }

    public Location create(Integer hall, Integer rack, Integer shelf) {
        return locationsRepository.create(Location.builder()
                .hall(hall)
                .rack(rack)
                .shelf(shelf)
                .build()
        );
    }

    public Location update(String id, Integer hall, Integer rack, Integer shelf) {
        final Location location = locationsRepository.update(
                id,
                Location.builder()
                        .hall(hall)
                        .rack(rack)
                        .shelf(shelf)
                        .build()
        ).orElseThrow(() -> new NotFoundException("Location not found"));

        booksSearchCacheRepository.invalidate();
        return location;
    }

    public Optional<Location> getById(String id) {
        return locationsRepository.getById(id);
    }

    public List<Location> getAll() {
        return locationsRepository.getAll();
    }
}
