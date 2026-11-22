-- Rename only the unchanged local storage providers created by the seed.
-- Keep customized names, descriptions, paths and other providers intact.
UPDATE storage_providers
SET name = 'Lokale Dateien und Medien',
    description = 'Dieser Speicheranbieter ermöglicht das Speichern von Dateien und Medien auf dem lokalen Dateisystem des Servers.',
    updated = now()
WHERE storage_provider_definition_key = 'de.aivot.core.local_disk_storage'
  AND storage_provider_definition_version = 1
  AND type = 0
  AND NOT system_provider
  AND NOT test_provider
  AND configuration #>> '{root,value}' = './data/assets/'
  AND name = 'Lokale Dokumente & Medien'
  AND description = 'Dieser Speicheranbieter ermöglicht das Speichern von Dokumenten und Medien auf dem lokalen Dateisystem des Servers.';

UPDATE storage_providers
SET name = 'Lokale Vorgangsanlagen',
    description = 'Dieser Speicheranbieter ermöglicht das Speichern von Vorgangsanlagen auf dem lokalen Dateisystem des Servers.',
    updated = now()
WHERE storage_provider_definition_key = 'de.aivot.core.local_disk_storage'
  AND storage_provider_definition_version = 1
  AND type = 1
  AND NOT system_provider
  AND NOT test_provider
  AND configuration #>> '{root,value}' = './data/attachments/'
  AND name = 'Lokale Vorgangsdokumente'
  AND description = 'Dieser Speicheranbieter ermöglicht das Speichern von Vorgangsdokumenten auf dem lokalen Dateisystem des Servers.';
