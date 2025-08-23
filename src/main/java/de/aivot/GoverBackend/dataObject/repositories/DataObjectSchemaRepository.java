package de.aivot.GoverBackend.dataObject.repositories;

import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DataObjectSchemaRepository extends JpaRepository<DataObjectSchemaEntity, String>, JpaSpecificationExecutor<DataObjectSchemaEntity> {

}
