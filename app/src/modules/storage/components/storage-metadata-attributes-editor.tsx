import {Box, Stack, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';
import {StorageProviderEntity} from '../entities/storage-provider-entity';

interface StorageMetadataAttributesEditorProps {
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

export function StorageMetadataAttributesEditor({storageProvider, metadata}: StorageMetadataAttributesEditorProps) {
    const initialValues = useMemo<Record<string, string>>(() => {
        const values: Record<string, string> = {};
        for (const attribute of storageProvider.metadataAttributes) {
            values[attribute.key] = normalizeMetadataValue(metadata[attribute.key]);
        }
        return values;
    }, [storageProvider, metadata]);

    const [values, setValues] = useState<Record<string, string>>(initialValues);

    useEffect(() => {
        setValues(initialValues);
    }, [initialValues]);

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
                        value={values[attribute.key] ?? ''}
                        hint={attribute.description}
                        onChange={(value) => {
                            setValues((prev) => ({
                                ...prev,
                                [attribute.key]: value ?? '',
                            }));
                        }}
                    />
                </Box>
            ))}
        </Stack>
    );
}
