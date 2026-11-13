-- Correct the database default without changing existing, editable template titles.
ALTER TABLE presets
    ALTER COLUMN title SET DEFAULT 'Unbenannte Vorlage';
