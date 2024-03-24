package de.aivot.GoverBackend.repositories;

import de.aivot.GoverBackend.models.entities.Destination;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DestinationRepository extends JpaRepository<Destination, Integer> {
}
