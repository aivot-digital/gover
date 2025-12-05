-- Create example users
insert into users (id,
                   email,
                   first_name,
                   last_name,
                   global_role,
                   deleted_in_idp,
                   verified,
                   enabled)
values ('00000000-0000-0000-0000-000000000000',
        'micha.maus@entenhausen.de',
        'Micha',
        'Maus',
        0,
        false,
        true,
        true),
       ('00000000-0000-0000-0000-000000000001',
        'doerte.duck@entenhausen.de',
        'Dörte',
        'Duck',
        1,
        false,
        true,
        true),
       ('00000000-0000-0000-0000-000000000002',
        'sascha.saiga@entenhausen.de',
        'Sascha',
        'Saiga',
        0,
        false,
        true,
        true)
on conflict (id) do update
    set email          = excluded.email,
        first_name     = excluded.first_name,
        last_name      = excluded.last_name,
        global_role    = excluded.global_role,
        deleted_in_idp = excluded.deleted_in_idp,
        verified       = excluded.verified,
        enabled        = excluded.enabled;

-- Create roles
insert into user_roles (id,
                        name,
                        description,
                        department_permission_edit,
                        team_permission_edit,
                        form_permission_create,
                        form_permission_read,
                        form_permission_edit,
                        form_permission_delete,
                        form_permission_annotate,
                        form_permission_publish,
                        process_permission_create,
                        process_permission_read,
                        process_permission_edit,
                        process_permission_delete,
                        process_permission_annotate,
                        process_permission_publish,
                        process_instance_permission_create,
                        process_instance_permission_read,
                        process_instance_permission_edit,
                        process_instance_permission_delete,
                        process_instance_permission_annotate,
                        created,
                        updated)
values (1,
        'Abteilungsleiter:in',
        'Leiter:in einer Abteilung mit vollen Rechten',
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        now(),
        now()),
       (2,
        'Prüfer:in',
        'Benutzer:in mit Prüfrechten für Formulare',
        false,
        false,
        false,
        true,
        false,
        false,
        true,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        now(),
        now()),
       (3,
        'Formularersteller:in',
        'Benutzer:in mit Rechten zum Erstellen und Bearbeiten von Formularen',
        false,
        false,
        true,
        true,
        true,
        true,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        now(),
        now()),
       (4,
        'Formularveröffentlicher:in',
        'Benutzer:in mit Rechten zum Prüfen und Veröffentlichen von Formularen',
        false,
        false,
        false,
        true,
        false,
        false,
        true,
        true,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        now(),
        now())
on conflict (id) do update
    set name                                 = excluded.name,
        description                          = excluded.description,
        department_permission_edit           = excluded.department_permission_edit,
        team_permission_edit                 = excluded.team_permission_edit,
        form_permission_create               = excluded.form_permission_create,
        form_permission_read                 = excluded.form_permission_read,
        form_permission_edit                 = excluded.form_permission_edit,
        form_permission_delete               = excluded.form_permission_delete,
        form_permission_annotate             = excluded.form_permission_annotate,
        form_permission_publish              = excluded.form_permission_publish,
        process_permission_create            = excluded.process_permission_create,
        process_permission_read              = excluded.process_permission_read,
        process_permission_edit              = excluded.process_permission_edit,
        process_permission_delete            = excluded.process_permission_delete,
        process_permission_annotate          = excluded.process_permission_annotate,
        process_permission_publish           = excluded.process_permission_publish,
        process_instance_permission_create   = excluded.process_instance_permission_create,
        process_instance_permission_read     = excluded.process_instance_permission_read,
        process_instance_permission_edit     = excluded.process_instance_permission_edit,
        process_instance_permission_delete   = excluded.process_instance_permission_delete,
        process_instance_permission_annotate = excluded.process_instance_permission_annotate,
        updated                              = now();

-- fix id sequence for user_roles
select setval('user_roles_id_seq',
                (select max(id) from user_roles));

-- Create example themes
insert into themes (id,
                    name,
                    main,
                    main_dark,
                    accent,
                    success,
                    info,
                    warning,
                    error)
values (1,
        'Entenhausen',
        '#004777',
        '#00253D',
        '#A30000',
        '#28a745',
        '#17a2b8',
        '#ffc107',
        '#dc3545'),
       (2,
        'Tanzmaus',
        '#F34213',
        '#9B2808',
        '#B09D1C',
        '#28a745',
        '#17a2b8',
        '#ffc107',
        '#dc3545')
on conflict (id) do update
    set name      = excluded.name,
        main      = excluded.main,
        main_dark = excluded.main_dark,
        accent    = excluded.accent,
        success   = excluded.success,
        info      = excluded.info,
        warning   = excluded.warning,
        error     = excluded.error;

-- fix id sequence for themes
select setval('themes_id_seq',
                (select max(id) from themes));

-- Create example departments
insert into departments (id,
                         name,
                         address,
                         imprint,
                         common_privacy,
                         common_accessibility,
                         technical_support_address,
                         special_support_address,
                         department_mail,
                         theme_id,
                         technical_support_phone,
                         technical_support_info,
                         special_support_phone,
                         special_support_info,
                         additional_info,
                         parent_department_id,
                         created,
                         updated)
values (1,
        'Stadtverwaltung Entenhausen',
        E'Marktplatz 1\n12345 Entenhausen',
        'Impressum der Stadtverwaltung Entenhausen',
        'Datenschutzhinweis der Stadtverwaltung Entenhausen',
        'Barrierefreiheitshinweis der Stadtverwaltung Entenhausen',
        'it@entenhausen.de',
        'info@entenhausen.de',
        null,
        1,
        '+49 123 4567890',
        'Mo. bis Fr. 10:00 Uhr bis 16:00 Uhr',
        '+49 123 0987654',
        'Mo. bis Fr. 08:00 Uhr bis 12:00 Uhr',
        'Zusätzliche Informationen zur Stadtverwaltung Entenhausen',
        null,
        now(),
        now()),
       (2,
        'IT',
        null,
        null,
        null,
        null,
        null,
        'it@entenhausen.de',
        null,
        null,
        null,
        null,
        '+49 123 0987654',
        'Mo. bis Fr. 10:00 Uhr bis 16:00 Uhr',
        'Bitte wenden Sie sich bei IT-Problemen an diese Abteilung.',
        1,
        now(),
        now()),
       (3,
        'Finanzamt',
        E'An der Goldgrube 4\n12345 Entenhausen',
        null,
        null,
        null,
        null,
        'finanzen@entenhausen.de',
        null,
        null,
        null,
        null,
        '+49 321 1234567',
        E'Mo. bis Fr. 09:00 Uhr bis 12:00 Uhr\nDo. 14:00 Uhr bis 16:00 Uhr',
        null,
        1,
        now(),
        now()),
       (4,
        'Sozialamt',
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        1,
        now(),
        now()),
       (5,
        'Kita Tanzmaus',
        E'Tanzmausweg 5\n12345 Entenhausen',
        null,
        null,
        null,
        null,
        'mail@kita-tanzmaus.de',
        null,
        2,
        null,
        null,
        '01701234567',
        'Mo. bis Fr. 06:00 Uhr bis 16:00 Uhr',
        null,
        4,
        now(),
        now())
on conflict (id) do update
    set name                      = excluded.name,
        address                   = excluded.address,
        imprint                   = excluded.imprint,
        common_privacy            = excluded.common_privacy,
        common_accessibility      = excluded.common_accessibility,
        technical_support_address = excluded.technical_support_address,
        special_support_address   = excluded.special_support_address,
        department_mail           = excluded.department_mail,
        theme_id                  = excluded.theme_id,
        technical_support_phone   = excluded.technical_support_phone,
        technical_support_info    = excluded.technical_support_info,
        special_support_phone     = excluded.special_support_phone,
        special_support_info      = excluded.special_support_info,
        additional_info           = excluded.additional_info,
        parent_department_id      = excluded.parent_department_id,
        updated                   = now();

-- fix id sequence for departments
select setval('departments_id_seq',
                (select max(id) from departments));

-- create memberships for departments
insert into department_memberships (id,
                                    department_id,
                                    user_id,
                                    created,
                                    updated)
values (1,
        2,
        '00000000-0000-0000-0000-000000000000',
        now(),
        now()),
       (2,
        3,
        '00000000-0000-0000-0000-000000000001',
        now(),
        now()),
       (3,
        4,
        '00000000-0000-0000-0000-000000000001',
        now(),
        now()),
       (4,
        5,
        '00000000-0000-0000-0000-000000000002',
        now(),
        now())
on conflict (id) do update
    set department_id = excluded.department_id,
        user_id       = excluded.user_id,
        updated       = now();

-- fix id sequence for department_memberships
select setval('department_memberships_id_seq',
                (select max(id) from department_memberships));

-- assign user roles to memberships
insert into user_role_assignments (id,
                                   department_membership_id,
                                   team_membership_id,
                                   user_role_id,
                                   created)
values (1, 1, null, 1, now()),
       (2, 2, null, 4, now()),
       (3, 3, null, 2, now()),
       (4, 4, null, 3, now())
on conflict (id) do update
    set department_membership_id = excluded.department_membership_id,
        user_role_id             = excluded.user_role_id;

-- fix id sequence for user_role_assignments
select setval('user_role_assignments_id_seq',
                (select max(id) from user_role_assignments));

-- create teams
insert into teams (id,
                   name)
values (1,
        'Entwicklungshelfer'),
       (2,
        'Prüfteam')
on conflict (id) do update
    set name = excluded.name;

-- fix id sequence for teams
select setval('teams_id_seq',
                (select max(id) from teams));

-- create team memberships
insert into team_memberships (id,
                              team_id,
                              user_id,
                              created,
                              updated)
values (1,
        1,
        '00000000-0000-0000-0000-000000000002',
        now(),
        now())
on conflict (id) do update
    set team_id = excluded.team_id,
        user_id = excluded.user_id,
        updated = now();

-- fix id sequence for team_memberships
select setval('team_memberships_id_seq',
                (select max(id) from team_memberships));

-- assign the default system theme
insert into system_configs (key,
                            value)
values ('SystemTheme', '1')
on conflict (key) do update
    set value = excluded.value;

-- create example forms
insert into forms (id,
                   developing_department_id,
                   slug,
                   internal_title,
                   published_version,
                   drafted_version,
                   version_count,
                   updated,
                   created)
values (1,
        2,
        'it-support-anfrage',
        'IT-Support Anfrage',
        null,
        null,
        0,
        now(),
        now()),
       (2,
        2,
        'anmeldung-kita-tanzmaus',
        'Anmeldung Kita Tanzmaus',
        null,
        null,
        0,
        now(),
        now()),
       (3,
        3,
        'antrag-auf-steuerstundung',
        'Antrag auf Steuerstundung',
        null,
        null,
        0,
        now(),
        now())
on conflict (id) do update
    set developing_department_id = excluded.developing_department_id,
        slug                     = excluded.slug,
        internal_title           = excluded.internal_title,
        published_version        = excluded.published_version,
        drafted_version          = excluded.drafted_version,
        version_count            = excluded.version_count,
        updated                  = now();

-- fix id sequence for forms
select setval('applications_id_seq',
                (select max(id) from forms));

-- create example form versions
insert into form_versions (form_id,
                           version,
                           status,
                           type,
                           legal_support_department_id,
                           technical_support_department_id,
                           imprint_department_id,
                           privacy_department_id,
                           accessibility_department_id,
                           destination_id,
                           theme_id,
                           pdf_template_key,
                           payment_provider_key,
                           payment_purpose,
                           payment_description,
                           payment_products,
                           identity_providers,
                           identity_verification_required,
                           customer_access_hours,
                           submission_retention_weeks,
                           root_element,
                           created,
                           updated,
                           published,
                           revoked,
                           public_title,
                           managing_department_id,
                           responsible_department_id)
values (2,
        1,
        0,
        0,
        4,
        2,
        5,
        5,
        5,
        null,
        2,
        null,
        null,
        '',
        '',
        '[]',
        '[]',
        false,
        4,
        4,
        '{
            "id": "rt_CvlaWtIvI6",
            "type": 0,
            "children": [
                {
                    "id": "st_RY6ejgCfU1",
                    "icon": "personWithChild",
                    "type": 1,
                    "title": "Angaben zum Kind",
                    "children": [
                        {
                            "id": "tx_eB9pMtXQZH",
                            "type": 15,
                            "label": "Vorname",
                            "weight": 6.0,
                            "disabled": false,
                            "required": true,
                            "technical": false,
                            "isMultiline": false,
                            "testProtocolSet": {
                                "professionalTest": {
                                    "userId": "494c4d4c-db52-4863-8b35-5d9c836b61f9",
                                    "timestamp": "2025-12-02T11:11:30.359Z"
                                }
                            }
                        },
                        {
                            "id": "tx_ltyX8l7kYK",
                            "type": 15,
                            "label": "Nachname",
                            "weight": 6.0,
                            "disabled": false,
                            "required": true,
                            "technical": false,
                            "isMultiline": false,
                            "testProtocolSet": {
                                "professionalTest": {
                                    "userId": "494c4d4c-db52-4863-8b35-5d9c836b61f9",
                                    "timestamp": "2025-12-02T11:11:30.359Z"
                                }
                            }
                        },
                        {
                            "id": "dt_Z1AvQijge4",
                            "mode": "day",
                            "type": 5,
                            "label": "Geburtsdatum",
                            "weight": 12.0,
                            "disabled": false,
                            "required": true,
                            "technical": false,
                            "testProtocolSet": {
                                "professionalTest": {
                                    "userId": "494c4d4c-db52-4863-8b35-5d9c836b61f9",
                                    "timestamp": "2025-12-02T11:11:30.359Z"
                                }
                            }
                        }
                    ],
                    "testProtocolSet": {
                        "professionalTest": {
                            "userId": "494c4d4c-db52-4863-8b35-5d9c836b61f9",
                            "timestamp": "2025-12-02T11:11:30.359Z"
                        }
                    }
                },
                {
                    "id": "st_5SHCGKuObs",
                    "icon": "document",
                    "type": 1,
                    "title": "Abgaben zur Anmeldung",
                    "children": [
                        {
                            "id": "dt_9PqF06EBtX",
                            "mode": "day",
                            "type": 5,
                            "label": "Gewünschtes Startdatum",
                            "weight": 12.0,
                            "disabled": false,
                            "required": true,
                            "technical": false,
                            "testProtocolSet": {
                                "professionalTest": {
                                    "userId": "494c4d4c-db52-4863-8b35-5d9c836b61f9",
                                    "timestamp": "2025-12-02T11:11:30.359Z"
                                }
                            }
                        }
                    ],
                    "testProtocolSet": {
                        "professionalTest": {
                            "userId": "494c4d4c-db52-4863-8b35-5d9c836b61f9",
                            "timestamp": "2025-12-02T11:11:30.359Z"
                        }
                    }
                }
            ],
            "submitStep": {
                "id": "sb_bqWWonEBio",
                "type": 18,
                "weight": 12.0,
                "textPreSubmit": "Sie können Ihren Antrag nun verbindlich bei der zuständigen/bewirtschaftenden Stelle einreichen. Nach der Einreichung können Sie sich den Antrag für Ihre Unterlagen herunterladen oder zusenden lassen.",
                "textPostSubmit": "Sie können Ihren Antrag herunterladen oder sich per E-Mail zuschicken lassen. Wir empfehlen Ihnen, den Antrag anschließend zu Ihren Unterlagen zu nehmen.",
                "testProtocolSet": {
                    "professionalTest": {
                        "userId": "494c4d4c-db52-4863-8b35-5d9c836b61f9",
                        "timestamp": "2025-12-02T11:11:30.359Z"
                    }
                }
            },
            "privacyText": "Bitte beachten Sie die {privacy}Hinweise zum Datenschutz{/privacy}.",
            "summaryStep": {
                "id": "sm_3KsA1SnAMf",
                "type": 19,
                "weight": 12.0,
                "testProtocolSet": {
                    "professionalTest": {
                        "userId": "494c4d4c-db52-4863-8b35-5d9c836b61f9",
                        "timestamp": "2025-12-02T11:11:30.359Z"
                    }
                }
            },
            "testProtocolSet": {
                "professionalTest": {
                    "userId": "494c4d4c-db52-4863-8b35-5d9c836b61f9",
                    "timestamp": "2025-12-02T11:11:30.358Z"
                }
            },
            "introductionStep": {
                "id": "in_iHFMVG1UH4",
                "type": 17,
                "weight": 12.0,
                "testProtocolSet": {
                    "professionalTest": {
                        "userId": "494c4d4c-db52-4863-8b35-5d9c836b61f9",
                        "timestamp": "2025-12-02T11:11:30.359Z"
                    }
                }
            }
        }',
        now(),
        now(),
        null,
        null,
        E'Anmeldung zur\nKindertagesstätte\nTanzmaus',
        4,
        5)
on conflict (form_id,
    version) do update
    set status                          = excluded.status,
        type                            = excluded.type,
        legal_support_department_id     = excluded.legal_support_department_id,
        technical_support_department_id = excluded.technical_support_department_id,
        imprint_department_id           = excluded.imprint_department_id,
        privacy_department_id           = excluded.privacy_department_id,
        accessibility_department_id     = excluded.accessibility_department_id,
        destination_id                  = excluded.destination_id,
        theme_id                        = excluded.theme_id,
        pdf_template_key                = excluded.pdf_template_key,
        payment_provider_key            = excluded.payment_provider_key,
        payment_purpose                 = excluded.payment_purpose,
        payment_description             = excluded.payment_description,
        payment_products                = excluded.payment_products,
        identity_providers              = excluded.identity_providers,
        identity_verification_required  = excluded.identity_verification_required,
        customer_access_hours           = excluded.customer_access_hours,
        submission_retention_weeks      = excluded.submission_retention_weeks,
        root_element                    = excluded.root_element,
        updated                         = now(),
        published                       = excluded.published,
        revoked                         = excluded.revoked,
        public_title                    = excluded.public_title,
        managing_department_id          = excluded.managing_department_id,
        responsible_department_id       = excluded.responsible_department_id;

-- update forms with draft and version info
update forms
set drafted_version = 1,
    version_count   = 1
where id = 2;

-- create resource access controls
insert into resource_access_controls (id,
                                      source_department_id,
                                      source_team_id,
                                      target_form_id,
                                      form_permission_read,
                                      form_permission_edit,
                                      form_permission_annotate)
values (1,
        5,
        null,
        2,
        true,
        false,
        true),
       (2,
        null,
        1,
        3,
        true,
        true,
        false),
       (3,
        null,
        2,
        3,
        true,
        false,
        false)
on conflict (id) do update
    set source_department_id     = excluded.source_department_id,
        target_form_id           = excluded.target_form_id,
        form_permission_read     = excluded.form_permission_read,
        form_permission_annotate = excluded.form_permission_annotate;

-- fix id sequence for resource_access_controls
select setval('resource_access_controls_id_seq',
                (select max(id) from resource_access_controls));

-- create example data object schema and items
insert into data_object_schemas (key,
                                 name,
                                 description,
                                 id_gen,
                                 schema,
                                 created,
                                 updated,
                                 display_fields)
values ('kita_kinder',
        'Kita Kinder',
        'Kinder in der Kita',
        '__SERIAL__',
        '{
            "id": "kind",
            "type": 3,
            "weight": 12.0,
            "children": [
                {
                    "id": "vorname",
                    "type": 15,
                    "label": "Vorname",
                    "weight": 6.0,
                    "disabled": false,
                    "required": true,
                    "technical": false,
                    "isMultiline": false
                },
                {
                    "id": "nachname",
                    "type": 15,
                    "label": "Nachname",
                    "weight": 6.0,
                    "disabled": false,
                    "required": true,
                    "technical": false,
                    "isMultiline": false
                },
                {
                    "id": "geburtstag",
                    "mode": "day",
                    "type": 5,
                    "label": "Geburtstag",
                    "weight": 12.0,
                    "disabled": false,
                    "required": true,
                    "technical": false
                }
            ]
        }',
        now(),
        now(),
        '{vorname,nachname}')
on conflict (key) do update
    set name           = excluded.name,
        description    = excluded.description,
        id_gen         = excluded.id_gen,
        schema         = excluded.schema,
        updated        = now(),
        display_fields = excluded.display_fields;

insert into data_object_items (schema_key,
                               id,
                               data,
                               created,
                               updated,
                               deleted)
values ('kita_kinder',
        '1',
        '{
            "vorname": "Erwin",
            "nachname": "Erpel",
            "geburtstag": "1997-12-11T23:06:32Z"
        }',
        now(),
        now(),
        null),
       ('kita_kinder',
        '2',
        '{
            "vorname": "Edna",
            "nachname": "Quack",
            "geburtstag": "1998-05-08T23:06:32Z"
        }',
        now(),
        now(),
        null)
on conflict (schema_key,
    id) do update
    set data    = excluded.data,
        updated = now(),
        deleted = excluded.deleted;
