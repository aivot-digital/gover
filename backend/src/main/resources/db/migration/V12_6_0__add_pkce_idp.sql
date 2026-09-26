-- Add pkce_method column to identity_providers table for PKCE support
alter table identity_providers
    add column pkce_method varchar(24) null;

-- Update existing default identity providers to set pkce_method to 'S256'
update identity_providers
set pkce_method = 'S256'
where type <> 0; -- 0 is for CUSTOM type