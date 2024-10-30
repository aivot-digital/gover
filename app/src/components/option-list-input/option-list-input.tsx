import {
    Alert,
    Box,
    Button,
    FormControl,
    FormHelperText,
    FormLabel,
    IconButton,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
    TextField,
    Tooltip,
    Typography,
} from '@mui/material';
import React, {useCallback, useMemo, useReducer, useState} from 'react';
import {TextFieldComponent} from '../text-field/text-field-component';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {type OptionListInputProps} from './option-list-input-props';
import {TableVirtuoso} from 'react-virtuoso';

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

    const handleChange = props.onChange;

    const TableComponents = useMemo(() => ({
        // @ts-ignore
        Scroller: React.forwardRef((props, ref) => <TableContainer {...props} ref={ref} />),
        // @ts-ignore
        Table: (props) => <Table {...props} size="small"
                                 sx={{borderCollapse: 'separate', '& td': {border: 0}}}
        />,
        TableRow: TableRow,
        // @ts-ignore
        TableBody: React.forwardRef((props, ref) => <TableBody {...props} ref={ref} />),
    }), []);

    const renderRow = useCallback((index: any, item: any) => {
        return (
            <>
                <TableCell>
                    <TextField
                        fullWidth
                        label={props.labelLabel ?? 'Beschriftung'}
                        size="small"
                        margin="dense"
                        value={item.label}
                        onChange={(event) => {
                            const updatedValue = [...options];
                            updatedValue[index] = {
                                ...item,
                                label: event.target.value ?? '',
                            };
                            handleChange(updatedValue);
                        }}
                        onBlur={() => {
                            if (item.value.length === 0) {
                                const updatedValue = [...options];
                                updatedValue[index] = {
                                    ...item,
                                    value: item.label,
                                };
                                handleChange(updatedValue);
                            }
                        }}
                        error={item.label.length === 0}
                        helperText={(item.label.length === 0) ? 'Bitte geben Sie einen Text ein, oder entfernen Sie diese Zeile.' : undefined}
                        disabled={!isEditable}
                    />
                </TableCell>

                <TableCell>
                    <TextField
                        fullWidth
                        label={props.keyLabel ?? 'Wert'}
                        size="small"
                        margin="dense"
                        value={item.value}
                        onChange={(event) => {
                            const updatedValue = [...options];
                            updatedValue[index] = {
                                ...item,
                                value: event.target.value ?? '',
                            };
                            handleChange(updatedValue);
                        }}
                        error={item.value.length === 0}
                        helperText={(item.value.length === 0) ? 'Bitte geben Sie einen Text ein, oder entfernen Sie diese Zeile.' : undefined}
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
                                <DeleteForeverOutlinedIcon />
                            </IconButton>
                        </Tooltip>
                    </TableCell>
                }
            </>
        );
    }, [options, handleChange]);


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
                            <SwapHorizOutlinedIcon />
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
                            <AddOutlinedIcon />
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
                <TableVirtuoso
                    style={{height: 380}}
                    data={options}
                    //@ts-ignore
                    components={TableComponents}
                    itemContent={renderRow}
                />
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
                    Jede Zeile muss einen Wert enthalten. Befüllen Sie fehlende Werte oder entfernen Sie die
                    entsprechenden Zeilen.
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
                    Es gibt mindestens zwei Einträge mit der gleichen Beschriftung. Bitte ändern Sie die Beschriftungen
                    so, dass sie eindeutig sind.
                </FormHelperText>
            }

            {
                !hasEmptyField &&
                !hasNotEnoughItems &&
                !hasDuplicateLabels &&
                hasDuplicateValues &&
                <FormHelperText sx={{mt: 2}}>
                    Es gibt mindestens zwei Einträge mit dem gleichen Wert. Bitte ändern Sie die Werte so, dass sie
                    eindeutig sind.
                </FormHelperText>
            }

            {
                !hasEmptyField &&
                !hasNotEnoughItems &&
                hasDuplicateLabels &&
                hasDuplicateValues &&
                <FormHelperText sx={{mt: 2}}>
                    Es gibt mindestens zwei Einträge mit dem gleichen Wert oder gleicher Beschriftung. Bitte ändern Sie
                    die Werte und Beschriftungen so, dass sie eindeutig sind.
                </FormHelperText>
            }

            {
                /*
                !hasEmptyField &&
                !hasNotEnoughItems &&
                !hasDuplicateLabels &&
                !hasDuplicateValues &&
                 */
                <FormHelperText
                    sx={{mt: 2}}
                    error={false}
                >
                    {props.hint} Bitte scrollen Sie ggf. um alle Optionen zu sehen.
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
