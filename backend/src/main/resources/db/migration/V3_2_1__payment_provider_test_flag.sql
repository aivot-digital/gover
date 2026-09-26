-- ad test column to payment_providers table
alter table payment_providers
    add column is_test_provider bool not null default false;