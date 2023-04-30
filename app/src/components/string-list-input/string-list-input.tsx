import {
    Alert,
    Box,
    Button,
    FormControl,
    FormHelperText,
    FormLabel,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableRow,
    TextField,
    Typography
} from "@mui/material";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faAdd, faTrashCanXmark} from "@fortawesome/pro-light-svg-icons";

interface StringListInputProps {
    label: string;
    hint: string;
    addLabel: string;
    noItemsHint: string;
    value?: string[];
    onChange: (ls: string[] | undefined) => void;
    allowEmpty: boolean;
}

export function StringListInput({
                                    label,
                                    hint,
                                    addLabel,
                                    noItemsHint,
                                    value,
                                    onChange,
                                    allowEmpty
                                }: StringListInputProps) {
    const isValueEmpty = !allowEmpty && (value == null || value.length === 0);
    const hasEmptyItem = value && value.some(val => val.trim().length === 0);

    return (
        <FormControl
            error={hasEmptyItem || isValueEmpty}
            component={Paper}
            sx={{p: 2}}
        >
            <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                <FormLabel>
                    {label}
                </FormLabel>

                <Button
                    startIcon={
                        <FontAwesomeIcon
                            icon={faAdd}
                        />
                    }
                    onClick={() => onChange([...(value ?? []), ''])}
                >
                    {addLabel}
                </Button>
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
                value &&
                value.length > 0 &&
                <TableContainer>
                    <Table
                        size="small"
                        sx={{
                            "& td": {
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
                                            />
                                        </TableCell>
                                        <TableCell>
                                            <Button
                                                color="error"
                                                startIcon={
                                                    <FontAwesomeIcon
                                                        icon={faTrashCanXmark}
                                                    />
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
                                    </TableRow>
                                ))
                            }
                        </TableBody>
                    </Table>
                </TableContainer>
            }

            <FormHelperText sx={{mt: 2}}>
                {
                    hasEmptyItem ? 'Jede Zeile muss einen Wert enthalten. Befüllen Sie fehlende Werte oder entfernen Sie die entsprechenden Zeilen.' : (isValueEmpty ? 'Bitte fügen Sie mindestens einen Wert hinzu.' : hint)
                }
            </FormHelperText>
        </FormControl>
    );
}
