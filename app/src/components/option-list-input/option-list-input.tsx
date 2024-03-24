import {Alert, Box, Button, FormControl, FormHelperText, FormLabel, IconButton, Paper, Table, TableBody, TableCell, TableContainer, TableRow, TextField, Tooltip, Typography} from '@mui/material';
import React, {useReducer, useState} from 'react';
import {TextFieldComponent} from '../text-field/text-field-component';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {type OptionListInputProps} from './option-list-input-props';

export function OptionListInput(props: OptionListInputProps): JSX.Element {
    const [textInputMode, toggleTextInputMode] = useReducer((state: boolean) => !state, false);
    const [textInputBuffer, setTextInputBuffer] = useState<string>();

    const options = props.value ?? [];
    const isEditable = props.disabled !== true;
    const isRequired = !props.allowEmpty;

    const hasNotEnoughItems = isRequired && options.length === 0;
    const hasEmptyField = options.some((option) => option.value.length === 0 || option.label.length === 0);
    const hasDuplicateLabels = new Set(options.map((opt) => opt.label)).size !== options.length;
    const hasDuplicateValues = new Set(options.map((opt) => opt.value)).size !== options.length;

    const handleAdd = (): void => {
        props.onChange([
            ...options,
            {
                label: '',
                value: '',
            },
        ]);
    };

    const handleRemove = (index: number): void => {
        const updatedValue = [...options];
        updatedValue.splice(index, 1);
        props.onChange(updatedValue);
    };

    return (
        <FormControl
            error={hasNotEnoughItems || hasEmptyField || hasDuplicateLabels || hasDuplicateValues}
            component={Paper}
            sx={{p: 2}}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                }}
            >

                {
                    isEditable &&
                    <Tooltip
                        title="Modus umschalten"
                    >
                        <IconButton
                            size="small"
                            onClick={toggleTextInputMode}
                        >
                            <SwapHorizOutlinedIcon/>
                        </IconButton>
                    </Tooltip>
                }

                <FormLabel
                    sx={{ml: 1}}
                >
                    {props.label}
                </FormLabel>

                {
                    isEditable &&
                    !textInputMode &&
                    <Button
                        sx={{
                            ml: 'auto',
                        }}
                        startIcon={
                            <AddOutlinedIcon/>
                        }
                        onClick={handleAdd}
                    >
                        {props.addLabel}
                    </Button>
                }
            </Box>

            {
                !textInputMode &&
                options.length > 0 &&
                <TableContainer>
                    <Table
                        size="small"
                        sx={{
                            '& td': {
                                border: 0,
                            },
                        }}
                    >
                        <TableBody>
                            {
                                options.map((option, index) => (
                                    <TableRow key={index}>
                                        <TableCell>
                                            <TextField
                                                fullWidth
                                                label="Beschriftung"
                                                size="small"
                                                margin="dense"
                                                value={option.label}
                                                onChange={(event) => {
                                                    const updatedValue = [...options];
                                                    updatedValue[index] = {
                                                        ...option,
                                                        label: event.target.value ?? '',
                                                    };
                                                    props.onChange(updatedValue);
                                                }}
                                                onBlur={() => {
                                                    if (option.value.length === 0) {
                                                        const updatedValue = [...options];
                                                        updatedValue[index] = {
                                                            ...option,
                                                            value: option.label,
                                                        };
                                                        props.onChange(updatedValue);
                                                    }
                                                }}
                                                error={option.label.length === 0}
                                                helperText={option.label.length === 0 ? 'Bitte geben Sie einen Text ein, oder entfernen Sie diese Zeile.' : undefined}
                                                disabled={!isEditable}
                                            />
                                        </TableCell>

                                        <TableCell>
                                            <TextField
                                                fullWidth
                                                label="Wert"
                                                size="small"
                                                margin="dense"
                                                value={option.value}
                                                onChange={(event) => {
                                                    const updatedValue = [...options];
                                                    updatedValue[index] = {
                                                        ...option,
                                                        value: event.target.value ?? '',
                                                    };
                                                    props.onChange(updatedValue);
                                                }}
                                                error={option.value.length === 0}
                                                helperText={option.value.length === 0 ? 'Bitte geben Sie einen Text ein, oder entfernen Sie diese Zeile.' : undefined}
                                                disabled={!isEditable}
                                            />
                                            {/* TODO: Check if other option has the same value */}
                                        </TableCell>

                                        {
                                            isEditable &&
                                            <TableCell>
                                                <Tooltip title="Entfernen">
                                                    <IconButton
                                                        color="error"
                                                        onClick={() => {
                                                            handleRemove(index);
                                                        }}
                                                    >
                                                        <DeleteForeverOutlinedIcon/>
                                                    </IconButton>
                                                </Tooltip>
                                            </TableCell>
                                        }
                                    </TableRow>
                                ))
                            }
                        </TableBody>
                    </Table>
                </TableContainer>
            }

            {
                textInputMode &&
                <TextFieldComponent
                    label="Einträge"
                    placeholder={'Beschriftung 1|Wert 1\nBeschriftung 2|Wert 2\nBeschriftung 3|Wert 3'}
                    value={textInputBuffer != null ? textInputBuffer : options.map((opt) => `${opt.label}|${opt.value}`).join('\n')}
                    onChange={(val) => {
                        setTextInputBuffer(val ?? '');
                    }}
                    onBlur={(val) => {
                        setTextInputBuffer(undefined);

                        if (val != null) {
                            const lines = val.split('\n');
                            const values = lines.map((ln) => {
                                const parts = ln.split('|');
                                return {
                                    value: (parts[1] ?? '').trim(),
                                    label: (parts[0] ?? '').trim(),
                                };
                            });
                            props.onChange(values);
                        } else {
                            props.onChange(undefined);
                        }
                    }}
                    multiline
                />
            }

            {
                hasEmptyField &&
                !hasNotEnoughItems &&
                !hasDuplicateLabels &&
                !hasDuplicateValues &&
                <FormHelperText sx={{mt: 2}}>
                    Jede Zeile muss einen Wert enthalten. Befüllen Sie fehlende Werte oder entfernen Sie die entsprechenden Zeilen.
                </FormHelperText>
            }

            {
                !hasEmptyField &&
                hasNotEnoughItems &&
                !hasDuplicateLabels &&
                !hasDuplicateValues &&
                <FormHelperText sx={{mt: 2}}>
                    Bitte fügen Sie mindestens einen Wert hinzu.
                </FormHelperText>
            }

            {
                !hasEmptyField &&
                !hasNotEnoughItems &&
                hasDuplicateLabels &&
                !hasDuplicateValues &&
                <FormHelperText sx={{mt: 2}}>
                    Es gibt mindestens zwei Einträge mit der gleichen Beschriftung. Bitte ändern Sie die Beschriftungen so, dass sie eindeutig sind.
                </FormHelperText>
            }

            {
                !hasEmptyField &&
                !hasNotEnoughItems &&
                !hasDuplicateLabels &&
                hasDuplicateValues &&
                <FormHelperText sx={{mt: 2}}>
                    Es gibt mindestens zwei Einträge mit dem gleichen Wert. Bitte ändern Sie die Werte so, dass sie eindeutig sind.
                </FormHelperText>
            }

            {
                !hasEmptyField &&
                !hasNotEnoughItems &&
                hasDuplicateLabels &&
                hasDuplicateValues &&
                <FormHelperText sx={{mt: 2}}>
                    Es gibt mindestens zwei Einträge mit dem gleichen Wert oder gleicher Beschriftung. Bitte ändern Sie die Werte und Beschriftungen so, dass sie eindeutig sind.
                </FormHelperText>
            }

            {
                !hasEmptyField &&
                !hasNotEnoughItems &&
                !hasDuplicateLabels &&
                !hasDuplicateValues &&
                <FormHelperText sx={{mt: 2}}>
                    {props.hint}
                </FormHelperText>
            }

            {
                options.length === 0 &&
                <Alert
                    sx={{
                        my: 4,
                    }}
                    severity={isRequired ? 'error' : 'info'}
                >
                    <Typography>
                        {props.noItemsHint}
                    </Typography>
                </Alert>
            }
        </FormControl>
    );
}
