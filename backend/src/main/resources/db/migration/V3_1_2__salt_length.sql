-- update salt length to 24
alter table secrets
    alter column salt type varchar(24);