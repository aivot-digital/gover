package de.aivot.prosuna.backend.dataObject.repositories;

import de.aivot.prosuna.backend.dataObject.entities.DataObjectSchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DataObjectSchemaRepository extends JpaRepository<DataObjectSchemaEntity, String>, JpaSpecificationExecutor<DataObjectSchemaEntity> {

}
