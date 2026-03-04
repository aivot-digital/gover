import {Stack, Typography} from '@mui/material';
import React from 'react';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {StorageProviderEntity} from '../entities/storage-provider-entity';

interface StorageMetadataAttributesViewProps {
    storageProvider: StorageProviderEntity;
    metadata: Record<string, unknown>;
}

function normalizeMetadataValue(value: unknown): string {
    if (value == null) {
        return '';
    }
    if (typeof value === 'string') {
        return value;
    }
    return String(value);
}

export function StorageMetadataAttributesView({storageProvider, metadata}: StorageMetadataAttributesViewProps) {
    if (storageProvider.metadataAttributes.length === 0) {
        return (
            <Typography color="text.secondary">
                Für diesen Speicheranbieter sind keine Metadatenattribute konfiguriert.
            </Typography>
        );
    }

    return (
        <Stack spacing={2}>
            {storageProvider.metadataAttributes.map((attribute) => (
                <TextFieldComponent
                    key={attribute.key}
                    label={attribute.label}
                    value={normalizeMetadataValue(metadata[attribute.key])}
                    hint={attribute.description}
                    onChange={() => {}}
                    disabled
                />
            ))}
        </Stack>
    );
}
