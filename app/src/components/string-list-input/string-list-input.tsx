import {Alert, Box, Button, FormControl, FormHelperText, FormLabel, IconButton, Paper, Table, TableBody, TableCell, TableContainer, TableRow, TextField, Tooltip, Typography} from '@mui/material';
import {useState} from 'react';
import {TextFieldComponent} from '../text-field/text-field-component';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';

interface StringListInputProps {
    label: string;
    hint: string;
    addLabel: string;
    noItemsHint: string;
    value?: string[];
    onChange: (ls: string[] | undefined) => void;
    allowEmpty: boolean;
    disabled?: boolean;
}

export function StringListInput({
                                    label,
                                    hint,
                                    addLabel,
                                    noItemsHint,
                                    value,
                                    onChange,
                                    allowEmpty,
                                    disabled,
                                }: StringListInputProps) {
    const [rawMode, setRawMode] = useState(false);
    const [rawBuffer, setRawBuffer] = useState<string>();
    const isValueEmpty = !allowEmpty && (value == null || value.length === 0);
    const hasEmptyItem = value && value.some(val => val.trim().length === 0);

    return (
        <FormControl
            error={hasEmptyItem || isValueEmpty}
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
                    !disabled &&
                    <Tooltip
                        title="Modus umschalten"
                    >
                        <IconButton
                            size="small"
                            onClick={() => setRawMode(!rawMode)}
                        >
                            <SwapHorizOutlinedIcon/>
                        </IconButton>
                    </Tooltip>
                }

                <FormLabel
                    sx={{ml: 1}}
                >
                    {label}
                </FormLabel>

                {
                    !disabled &&
                    <Button
                        sx={{
                            ml: 'auto',
                        }}
                        startIcon={
                            <AddOutlinedIcon/>
                        }
                        onClick={() => onChange([...(value ?? []), ''])}
                    >
                        {addLabel}
                    </Button>
                }
            </Box>

            {
                (!value || value.length === 0) &&
                <Alert
                    sx={{
                        my: 4,
                    }}
                    severity={isValueEmpty ? 'error' : 'info'}
                >
                    <Typography>
                        {noItemsHint}
                    </Typography>
                </Alert>
            }

            {
                !rawMode &&
                value &&
                value.length > 0 &&
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
                                value.map((val, index) => (
                                    <TableRow key={index}>
                                        <TableCell>
                                            <TextField
                                                fullWidth
                                                size="small"
                                                margin="dense"
                                                value={val}
                                                onChange={event => {
                                                    const updatedValue = [...value];
                                                    updatedValue[index] = event.target.value;
                                                    onChange(updatedValue);
                                                }}
                                                onBlur={() => {
                                                    if (value != null) {
                                                        onChange(value.map(val => val.trim()));
                                                    }
                                                }}
                                                error={val.length === 0}
                                                helperText={val.length === 0 ? 'Bitte geben Sie einen Text ein, oder entfernen Sie diese Zeile.' : undefined}
                                                disabled={disabled}
                                            />
                                        </TableCell>
                                        {
                                            !disabled &&
                                            <TableCell>
                                                <Button
                                                    color="error"
                                                    startIcon={
                                                        <DeleteForeverOutlinedIcon/>
                                                    }
                                                    onClick={() => {
                                                        const updatedValue = [...value];
                                                        updatedValue.splice(index, 1);
                                                        onChange(updatedValue);
                                                    }}
                                                >
                                                    Entfernen
                                                </Button>
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
                rawMode &&
                <TextFieldComponent
                    label="Einträge"
                    placeholder={'Option 1\nOption 2\nOption 3'}
                    value={rawBuffer ?? (value ?? []).join('\n')}
                    onChange={val => setRawBuffer(val ?? '')}
                    onBlur={val => {
                        setRawBuffer(undefined);
                        onChange(val != null ? val.split('\n').map(l => l.trim()) : undefined);
                    }}
                    multiline
                />
            }

            <FormHelperText sx={{mt: 2}}>
                {
                    hasEmptyItem ? 'Jede Zeile muss einen Wert enthalten. Befüllen Sie fehlende Werte oder entfernen Sie die entsprechenden Zeilen.' : (isValueEmpty ? 'Bitte fügen Sie mindestens einen Wert hinzu.' : hint)
                }
            </FormHelperText>
        </FormControl>
    );
}
