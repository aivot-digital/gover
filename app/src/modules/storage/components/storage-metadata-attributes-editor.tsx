import {Box, Stack, Typography} from '@mui/material';
import React from 'react';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {StorageProviderEntity} from '../entities/storage-provider-entity';

interface StorageMetadataAttributesEditorProps {
    storageProvider: StorageProviderEntity;
    metadata: Record<string, unknown>;
    onChange: (metadata: Record<string, unknown>) => void;
    disabled?: boolean;
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

export function StorageMetadataAttributesEditor({storageProvider, metadata, onChange, disabled}: StorageMetadataAttributesEditorProps) {
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
                <Box key={attribute.key}>
                    <TextFieldComponent
                        label={attribute.label}
                        value={normalizeMetadataValue(metadata[attribute.key])}
                        hint={attribute.description}
                        disabled={disabled}
                        onChange={(value) => {
                            const nextMetadata = {
                                ...metadata,
                            };

                            if (value == null || value.trim().length === 0) {
                                delete nextMetadata[attribute.key];
                            } else {
                                nextMetadata[attribute.key] = value;
                            }

                            onChange(nextMetadata);
                        }}
                    />
                </Box>
            ))}
        </Stack>
    );
}
