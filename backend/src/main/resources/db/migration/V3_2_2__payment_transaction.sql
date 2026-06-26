-- add table for payment transactions

create table payment_transactions
(
    key                  varchar(36) primary key,
    payment_provider_key varchar(36)  not null references payment_providers (key),
    payment_request      jsonb        not null,
    payment_information  jsonb        null,
    payment_error        text         null,
    redirect_url         varchar(255) not null,
    created              timestamp    not null,
    updated              timestamp    not null
)