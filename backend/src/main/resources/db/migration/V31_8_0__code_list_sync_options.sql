-- Synchronization requests are stored with the code list, so a lost broker message only delays
-- a synchronization until a periodic sweep publishes the request again. The requested option
-- whether entries missing from the source are kept must therefore be stored as well.
alter table code_lists
    add column sync_keep_outdated boolean not null default false;
