package de.aivot.prosuna.backend.codeLists.repositories;

import de.aivot.prosuna.backend.codeLists.entities.CodeListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CodeListRepository extends JpaRepository<CodeListEntity, String>, JpaSpecificationExecutor<CodeListEntity> {
}
