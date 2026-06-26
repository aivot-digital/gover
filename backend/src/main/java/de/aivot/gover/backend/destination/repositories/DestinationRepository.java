package de.aivot.gover.backend.destination.repositories;

import de.aivot.gover.backend.destination.entities.Destination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DestinationRepository extends JpaRepository<Destination, Integer>, JpaSpecificationExecutor<Destination> {
}
