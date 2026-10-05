-- add cascade delete property to form_revisions table

alter table form_revisions
    drop constraint form_revisions_form_id_fkey,
    add constraint form_revisions_form_id_fkey
        foreign key (form_id)
        references forms (id)
        on delete cascade;