import {
    Box,
    Button,
    FormControl,
    FormHelperText,
    InputLabel,
    MenuItem,
    Select,
    TextField,
    Typography
} from '@mui/material';
import React, {FormEvent, useCallback, useState} from 'react';
import {fetchSystemConfig, selectSystemConfig} from '../../../../../slices/system-config-slice';
import {SystemConfigsService} from '../../../../../services/system-configs.service';
import strings from './application-settings-strings.json';
import {Localization} from '../../../../../locale/localization';
import {useAppSelector} from '../../../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../../../hooks/use-app-dispatch';

const __ = Localization(strings);

export interface SettingsGenericConfigProps {
    fields: (string | {
        label: string;
        placeholder?: string;
        hint?: string;
        key: string;
        multiline?: boolean;
        options?: [string, string][];
        validate?: (value: string) => string | null;
    })[];
}

// TODO: Refactor this settings because its an overkill for the use case.

export function SettingsGenericConfig({fields}: SettingsGenericConfigProps) {
    const dispatch = useAppDispatch();
    const config = useAppSelector(selectSystemConfig);

    const [editedConfig, setEditedConfig] = useState<{ [key: string]: string }>();

    const handleSubmit = useCallback(async (event: FormEvent) => {
        event.preventDefault();

        if (editedConfig != null) {
            for (const field of fields) {
                if (typeof field === 'string') {
                    continue;
                }

                if (editedConfig[field.key] !== config[field.key]) {
                    // TODO: Fehler abfangen

                    await SystemConfigsService.update(field.key, {
                        id: 0,
                        key: field.key,
                        value: editedConfig[field.key],
                    });
                }
            }
            setEditedConfig(undefined);
            dispatch(fetchSystemConfig());
            (document.activeElement as HTMLInputElement)?.blur();
        }

        return false;
    }, [fields, editedConfig, config, dispatch]);

    return (
        <form onSubmit={handleSubmit}>
            {
                fields.map((field, index) => {
                    if (typeof field === 'string') {
                        return (
                            <Typography
                                key={field}
                                sx={index > 0 ? {mt: 2} : undefined}
                                variant="subtitle1"
                            >
                                {field}
                            </Typography>
                        );
                    } else if (field.options != null) {
                        return (
                            <FormControl
                                key={field.key}
                                fullWidth
                                margin="normal"
                            >
                                <InputLabel>
                                    {field.label}
                                </InputLabel>
                                <Select
                                    label={field.label}
                                    value={(editedConfig ?? config)[field.key] ?? ''}
                                    onChange={event => {
                                        setEditedConfig({
                                            ...(editedConfig ?? config),
                                            [field.key]: event.target.value,
                                        });
                                    }}
                                >
                                    {
                                        field.options.map(([value, label]) => (
                                            <MenuItem
                                                key={label}
                                                value={value}
                                            >
                                                {label}
                                            </MenuItem>
                                        ))
                                    }
                                </Select>
                                {
                                    field.hint &&
                                    <FormHelperText>
                                        {field.hint}
                                    </FormHelperText>
                                }
                            </FormControl>
                        );
                    } else {
                        return (
                            <TextField
                                key={field.key}
                                required
                                label={field.label}
                                placeholder={field.placeholder}
                                value={(editedConfig ?? config)[field.key] ?? ''}
                                onChange={event => {
                                    setEditedConfig({
                                        ...(editedConfig ?? config),
                                        [field.key]: event.target.value,
                                    });
                                }}
                                onBlur={() => {
                                    if (editedConfig != null) {
                                        setEditedConfig({
                                            ...(editedConfig ?? config),
                                            [field.key]: editedConfig[field.key].trim(),
                                        });
                                    }
                                }}
                                multiline={field.multiline}
                                rows={field.multiline ? 3 : undefined}
                                helperText={field.hint}
                            />
                        );
                    }
                })
            }

            <Box sx={{mt: 2}}>
                <Button
                    type="submit"
                    disabled={editedConfig == null}
                >
                    {__.saveLabel}
                </Button>
                <Button
                    type="button"
                    disabled={editedConfig == null}
                    onClick={() => {
                        setEditedConfig(undefined);
                    }}
                >
                    {__.resetLabel}
                </Button>
            </Box>
        </form>
    );
}
