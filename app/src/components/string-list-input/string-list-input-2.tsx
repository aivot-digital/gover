import {Box, FormControl, FormLabel, Paper, SxProps} from '@mui/material';
import React, {useState} from 'react';
import {TextFieldComponent} from '../text-field/text-field-component';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import {Actions} from '../actions/actions';
import {TableFieldComponent2} from '../table-field/table-field-component-2';

interface StringListInputProps {
    label: string;
    hint: string;
    addLabel: string;
    noItemsHint: string;
    value?: string[];
    onChange: (ls: string[] | undefined) => void;
    allowEmpty: boolean;
    disabled?: boolean;
    sx?: SxProps;
    error?: string;
}

export function StringListInput2(props: StringListInputProps) {
    const {
        sx,
        label,
        hint,
        addLabel,
        noItemsHint,
        value,
        onChange,
        allowEmpty,
        disabled,
        error,
    } = props;

    const [rawMode, setRawMode] = useState(false);
    const [rawBuffer, setRawBuffer] = useState<string>();
    const isValueEmpty = !allowEmpty && (value == null || value.length === 0);
    const hasEmptyItem = (value ?? []).some((val) => val.trim().length === 0);

    if (rawMode && !disabled) {
        return (
            <FormControl
                error={hasEmptyItem || isValueEmpty}
                component={Paper}
                elevation={0}
                sx={{
                    my: 0,
                    ...sx,
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                    }}
                >
                    <FormLabel>
                        {label}
                    </FormLabel>

                    <Actions
                        sx={{
                            ml: 'auto',
                        }}
                        actions={[{
                            icon: <SwapHorizOutlinedIcon />,
                            tooltip: 'Modus umschalten',
                            onClick: () => {
                                setRawMode(false);
                            },
                        }]}
                    />
                </Box>

                <TextFieldComponent
                    label="Einträge"
                    placeholder={'Option 1\nOption 2\nOption 3'}
                    value={rawBuffer ?? (value ?? []).join('\n')}
                    onChange={(val) => {
                        setRawBuffer(val ?? '');
                    }}
                    onBlur={(val) => {
                        setRawBuffer(undefined);
                        onChange(val != null ? val.split('\n').map((l) => l.trim()) : undefined);
                    }}
                    multiline
                    sx={{
                        mt: 1,
                        mb: 0,
                    }}
                />
            </FormControl>
        );
    } else {
        return (
            <TableFieldComponent2
                sx={sx}
                label={label}
                hint={hint}
                error={error}
                fields={[{
                    label: 'Eintrag',
                    type: 'string',
                    key: 'value',
                    disabled: disabled,
                }]}
                disabled={disabled}
                createDefaultRow={() => ({
                    value: '',
                })}
                value={(value ?? []).map(val => ({
                    value: val,
                }))}
                onChange={(val) => {
                    onChange(val == null ? undefined : val.map((v) => v.value));
                }}
                actions={[{
                    icon: <SwapHorizOutlinedIcon />,
                    tooltip: 'Modus umschalten',
                    onClick: () => {
                        setRawMode(true);
                    },
                    visible: !disabled,
                }]}
            />
        );
    }
}
