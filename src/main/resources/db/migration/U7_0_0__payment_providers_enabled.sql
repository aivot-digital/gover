-- drop the is_enabled column from the payment_providers table
alter table public.payment_providers
    drop column is_enabled;
