alter table identity_providers
    add column unique_id_attribute varchar(255) null;

update identity_providers
set unique_id_attribute = 'bPK2'
where type in (1, 2);

update identity_providers
set unique_id_attribute = 'preferred_username'
where type in (3, 4);

update identity_providers
set unique_id_attribute = 'sub',
    attributes = case
                     when exists (select 1
                                  from jsonb_array_elements(attributes) as attribute
                                  where attribute ->> 'keyInData' = 'sub')
                         then attributes
                     else attributes || jsonb_build_array(jsonb_build_object(
                             'label', 'Subject Identifier',
                             'description', 'Eindeutige Kennung der Identität beim Nutzerkontenanbieter.',
                             'keyInData', 'sub',
                             'displayAttribute', false
                                               ))
        end
where type = 0;

alter table identity_providers
    alter column unique_id_attribute set not null;
