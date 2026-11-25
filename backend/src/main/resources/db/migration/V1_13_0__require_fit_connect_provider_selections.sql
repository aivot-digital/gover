-- Make the former runtime defaults explicit before both selections become required.
-- Preserve configured values and unrelated provider settings.
UPDATE communication_providers
SET configuration = jsonb_set(configuration, '{environment}', '{"type":"Literal","value":"TEST"}'::jsonb, true)
WHERE communication_provider_definition_key = 'de.aivot.core.fit_connect_zbp_communication_provider'
  AND communication_provider_definition_version = 1
  AND jsonb_typeof(configuration) = 'object'
  AND (
      NOT (configuration ? 'environment')
      OR configuration -> 'environment' = 'null'::jsonb
      OR (
          configuration #>> '{environment,type}' = 'Literal'
          AND configuration #>> '{environment,value}' IS NULL
      )
  );

UPDATE communication_providers
SET configuration = jsonb_set(configuration, '{senderDestinationType}', '{"type":"Literal","value":"ONLINE_SERVICE"}'::jsonb, true)
WHERE communication_provider_definition_key = 'de.aivot.core.fit_connect_zbp_communication_provider'
  AND communication_provider_definition_version = 1
  AND jsonb_typeof(configuration) = 'object'
  AND (
      NOT (configuration ? 'senderDestinationType')
      OR configuration -> 'senderDestinationType' = 'null'::jsonb
      OR (
          configuration #>> '{senderDestinationType,type}' = 'Literal'
          AND configuration #>> '{senderDestinationType,value}' IS NULL
      )
  );
