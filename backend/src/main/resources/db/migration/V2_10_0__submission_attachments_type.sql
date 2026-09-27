-- update existing table

alter table submission_attachments
    add column type varchar(32) not null default 'customer';