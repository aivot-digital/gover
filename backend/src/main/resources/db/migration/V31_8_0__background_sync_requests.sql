-- Synchronization requests of code lists and storage providers are stored with their records, so a lost
-- broker message only delays a synchronization until a periodic sweep publishes the request again.
-- The requested option whether entries missing from the source are kept must therefore be stored as well.
alter table code_lists
    add column sync_keep_outdated boolean not null default false;

-- The run that claimed a requested synchronization and when it started. Only this run may record its
-- result, under a row lock, so a newer request, a changed source or a run that took over is never
-- overwritten. A run that started before the stale threshold is considered interrupted, e.g. by a
-- restart, and another run may take it over. Synchronizations that were running when an earlier
-- version stopped have no run ID and are taken over as well.
alter table code_lists
    add column sync_run_id  uuid        null,
    add column sync_started timestamptz null;

alter table storage_providers
    add column sync_run_id  uuid        null,
    add column sync_started timestamptz null;
