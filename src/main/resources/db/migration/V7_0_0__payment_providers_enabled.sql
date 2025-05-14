-- add is_enabled column to the payment_providers table
alter table payment_providers
    add column is_enabled bool not null default false;

-- set the value of existing providers for is_enabled to true
update payment_providers set is_enabled = true;