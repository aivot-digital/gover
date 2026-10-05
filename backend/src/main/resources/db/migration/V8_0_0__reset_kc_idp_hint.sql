update identity_providers
set additional_params = '[
    {
        "key": "kc_idp_hint",
        "value": "bayernid"
    }
]'
where name = 'BayernID'
  and type = 1
  and is_test_provider = false;

update identity_providers
set additional_params = '[
    {
        "key": "kc_idp_hint",
        "value": "bundid"
    }
]'
where name = 'BundID'
  and type = 2
  and is_test_provider = false;

update identity_providers
set additional_params = '[
    {
        "key": "kc_idp_hint",
        "value": "schleswigholstein"
    }
]'
where name = 'Servicekonto Schleswig-Holstein'
  and type = 3
  and is_test_provider = false;


update identity_providers
set additional_params = '[
    {
        "key": "kc_idp_hint",
        "value": "muk"
    }
]'
where name = 'Mein Unternehmenskonto'
  and type = 4
  and is_test_provider = false;