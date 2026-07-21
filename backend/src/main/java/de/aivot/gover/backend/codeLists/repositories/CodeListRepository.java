package de.aivot.gover.backend.codeLists.repositories;

import de.aivot.gover.backend.codeLists.entities.CodeListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CodeListRepository extends JpaRepository<CodeListEntity, Integer>, JpaSpecificationExecutor<CodeListEntity> {

}
