-- Update only unchanged salutation descriptions seeded for BayernID, BundID
-- and Servicekonto Schleswig-Holstein, in both production and test variants.
-- Preserve customized mappings, technical keys and the attribute order.
UPDATE identity_providers AS provider
SET attributes = (
    SELECT jsonb_agg(
                   CASE
                       WHEN attribute @> '{"label":"Anrede","keyInData":"salutation","description":"Formelle Anrede, z. B. Herr, Frau oder Divers."}'::jsonb
                           THEN jsonb_set(
                               attribute,
                               '{description}',
                               to_jsonb('Anrede, wie sie vom Identitätsanbieter übermittelt wird, zum Beispiel Herr oder Frau.'::text),
                               false
                           )
                       ELSE attribute
                       END
                   ORDER BY position
           )
    FROM jsonb_array_elements(provider.attributes) WITH ORDINALITY AS entry(attribute, position)
)
WHERE provider.type IN (1, 2, 3)
  AND jsonb_typeof(provider.attributes) = 'array'
  AND provider.attributes @> '[{"label":"Anrede","keyInData":"salutation","description":"Formelle Anrede, z. B. Herr, Frau oder Divers."}]'::jsonb;
