package de.aivot.GoverBackend.form.repositories;

import de.aivot.GoverBackend.form.entities.FormEditorEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FormEditorRepository extends Repository<FormEditorEntity, Integer> {
    @Query(value = """
                select distinct on (form_id) form_id, form_version, full_name, timestamp
                from form_revisions
                         join users on user_id = users.id
                where form_id in :formIds
                order by form_id, timestamp desc;
            """,
            nativeQuery = true
    )
    List<FormEditorEntity> findAllByFormIdIn(@Param("formIds") List<Integer> formId);

    @Query(value = """
                select distinct on (form_id, form_version) form_id, form_version, full_name, timestamp
                from form_revisions
                         join users on user_id = users.id
                where form_id = :formId
                order by form_id, form_version, timestamp desc;
            """,
            nativeQuery = true
    )
    List<FormEditorEntity> findAllByFormId(@Param("formId") Integer formId);
}
