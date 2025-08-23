package de.aivot.GoverBackend.dataObject.repositories;

import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntity;
import de.aivot.GoverBackend.dataObject.entities.DataObjectItemEntityId;
import de.aivot.GoverBackend.dataObject.entities.DataObjectSchemaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DataObjectItemRepository extends JpaRepository<DataObjectItemEntity, DataObjectItemEntityId>, JpaSpecificationExecutor<DataObjectItemEntity> {
    @Query(
            value = "SELECT MAX( CAST( id as INT ) ) FROM data_object_items WHERE schema_key = :schema_key",
            nativeQuery = true
    )
    Integer getMaxIdBySchemaKey(@Param("schema_key") String schemaKey, @Param("parts_to_replace") String[] partsToReplace);

    List<DataObjectItemEntity> findAllBySchemaKey(@Param("schema_key") String schemaKey);
}
