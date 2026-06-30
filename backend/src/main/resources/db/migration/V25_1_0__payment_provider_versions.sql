alter table payment_providers
    rename provider_key to payment_provider_definition_key;

alter table payment_providers
    add column payment_provider_definition_version integer not null default 1;