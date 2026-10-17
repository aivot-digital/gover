-- As a result of the update of the Keycloak with our new Keycloak Setup Sidecar,
-- we need to update the default scopes for our identity providers.
-- Each default identity provider now has its own custom scope that needs to be added
-- to the existing default scopes.

update identity_providers
set default_scopes = '[
    "email",
    "profile",
    "openid",
    "bayernid"
]'
where type = 1; -- BayernID

update identity_providers
set default_scopes = '[
    "email",
    "profile",
    "openid",
    "bundid"
]'
where type = 2; -- BundID

update identity_providers
set default_scopes = '[
    "email",
    "profile",
    "openid",
    "schleswigholstein"
]'
where type = 3; -- Servicekonto Schleswig-Holstein
update identity_providers
set default_scopes = '[
    "email",
    "profile",
    "openid",
    "muk"
]'
where type = 4; -- Mein Unternehmenskonto (MUK)