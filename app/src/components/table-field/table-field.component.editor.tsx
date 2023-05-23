import {Box, Button, Checkbox, FormControlLabel, Grid, TextField, Typography} from '@mui/material';
import {TableFieldComponentColumnModel, TableFieldElement} from '../../models/elements/form/input/table-field-element';
import {faPlus} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {isStringNullOrEmpty} from "../../utils/string-utils";
import {BaseEditorProps} from "../../editors/base-editor";

export function TableFieldComponentEditor(props: BaseEditorProps<TableFieldElement>) {
    const columnLabelErrors = makeColumnLabelErrors(props.element.fields);
    const minRequiredError = (
        props.element.minimumRequiredRows != null &&
        props.element.maximumRows != null &&
        props.element.maximumRows > 0 &&
        props.element.minimumRequiredRows > props.element.maximumRows
    );

    return (
        <>
            {
                props.element.required &&
                <TextField
                    value={props.element.minimumRequiredRows?.toString() ?? ''}
                    label="Mindestanzahl der hinzuzufügenden Zeilen"
                    margin="normal"
                    helperText={minRequiredError ? 'Sie fordern mehr Zeilen als Sie maximal zulassen.' : 'Geben Sie 0 ein, um keine Mindestanzahl zu fordern'}
                    onChange={event => {
                        if (event.target.value === '') {
                            props.onPatch({
                                minimumRequiredRows: undefined,
                            });
                            return;
                        }
                        let val = parseInt(event.target.value ?? '0');
                        if (isNaN(val)) {
                            val = 0;
                        }
                        props.onPatch({
                            minimumRequiredRows: val,
                        });
                    }}
                    onBlur={() => {
                        if (props.element.minimumRequiredRows == null || props.element.minimumRequiredRows === 0) {
                            props.onPatch({
                                required: false,
                            });
                        }
                    }}
                    error={minRequiredError}
                />
            }

            <TextField
                value={props.element.maximumRows?.toString() ?? ''}
                label="Maximalanzahl der hinzuzufügenden Zeilen"
                margin="normal"
                helperText={minRequiredError ? 'Sie fordern mehr Zeilen als Sie maximal zulassen.' : 'Geben Sie 0 ein, um keine Maximalanzahl zu fordern.'}
                onChange={event => {
                    if (event.target.value === '') {
                        props.onPatch({
                            maximumRows: undefined,
                        });
                        return;
                    }
                    let val = parseInt(event.target.value ?? '0');
                    if (isNaN(val)) {
                        val = 0;
                    }
                    props.onPatch({
                        maximumRows: val,
                    });
                }}
                error={minRequiredError}
            />

            <Typography
                variant="subtitle1"
                sx={{mt: 4}}
            >
                Spalten
            </Typography>
            {
                (props.element.fields ?? []).map((column, index) => {
                    const onChange = (patch: Partial<TableFieldComponentColumnModel>) => {
                        const patchedList = [...(props.element.fields ?? [])];
                        patchedList[index] = {
                            ...patchedList[index],
                            ...patch,
                        }
                        props.onPatch({
                            fields: patchedList,
                        });
                    };
                    return (
                        <Box
                            sx={{mt: 2}}
                            key={index}
                        >
                            <Grid
                                container
                                spacing={1}
                            >
                                <Grid
                                    item
                                    xs={4}
                                >
                                    <TextField
                                        label="Titel"
                                        margin="normal"
                                        value={column.label}
                                        onChange={event => onChange({
                                            label: event.target.value,
                                        })}
                                        onBlur={() => onChange({
                                            label: (column.label ?? '').trim(),
                                        })}
                                        error={columnLabelErrors[index] != null}
                                        helperText={columnLabelErrors[index]}
                                    />
                                </Grid>
                                <Grid
                                    item
                                    xs={4}
                                >
                                    <TextField
                                        label="Platzhalter"
                                        margin="normal"
                                        value={column.placeholder ?? ''}
                                        onChange={event => onChange({
                                            placeholder: event.target.value,
                                        })}
                                        onBlur={() => onChange({
                                            placeholder: (column.placeholder ?? '').trim(),
                                        })}
                                    />
                                </Grid>
                                {
                                    column.datatype === 'number' &&
                                    <Grid
                                        item
                                        xs={4}
                                    >
                                        <TextField
                                            label="Dezimalstellen"
                                            margin="normal"
                                            value={column.decimalPlaces?.toFixed() ?? ''}
                                            onChange={event => {
                                                if (event.target.value === '') {
                                                    onChange({
                                                        decimalPlaces: undefined,
                                                    });
                                                } else {
                                                    let val = parseInt(event.target.value ?? '0');
                                                    if (isNaN(val)) {
                                                        val = 0;
                                                    }
                                                    onChange({
                                                        decimalPlaces: val,
                                                    });
                                                }
                                            }}
                                        />
                                    </Grid>
                                }
                            </Grid>
                            <Box sx={{display: 'flex', justifyContent: 'space-between'}}>
                                <Box>
                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={column.datatype === 'number'}
                                                onChange={event => onChange({
                                                    datatype: event.target.checked ? 'number' : 'string',
                                                })}
                                            />
                                        }
                                        label="Zahl-Angabe"
                                    />
                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={column.optional ?? false}
                                                onChange={event => onChange({
                                                    optional: event.target.checked,
                                                })}
                                            />
                                        }
                                        label="Optionale Angabe"
                                    />

                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={column.disabled ?? false}
                                                onChange={event => onChange({
                                                    disabled: event.target.checked,
                                                })}
                                            />
                                        }
                                        label="Eingabe deaktiviert"
                                    />
                                </Box>
                                <Button
                                    color="error"
                                    onClick={() => {
                                        const updatedFields = [...(props.element.fields ?? [])];
                                        updatedFields.splice(index, 1);
                                        props.onPatch({
                                            fields: updatedFields,
                                        });
                                    }}
                                >
                                    Spalte löschen
                                </Button>
                            </Box>
                        </Box>
                    );
                })
            }
            <Button
                sx={{mt: 2}}
                startIcon={<FontAwesomeIcon icon={faPlus}/>}
                variant="contained"
                onClick={() => {
                    props.onPatch({
                        fields: [
                            ...(props.element.fields ?? []),
                            {
                                label: 'Neue Spalte',
                                datatype: 'string',
                            }
                        ],
                    })
                }}
            >
                Spalte Hinzufügen
            </Button>
        </>
    );
}

function makeColumnLabelErrors(fields?: TableFieldComponentColumnModel[]): (string | null)[] {
    if (fields == null) {
        return [];
    }
    const errors: (string | null)[] = [];
    const labelSet = new Set<string>();
    for (const field of fields) {
        if (isStringNullOrEmpty(field.label) || field.label.length < 3) {
            errors.push('Bitte geben Sie einen Titel mit mindestens drei Zeichen ein.');
        } else if (labelSet.has(field.label)) {
            errors.push('Eine Spalte mit diesem Titel existiert bereits. Bitte geben Sie einen einzigartigen Titel ein.');
        } else {
            errors.push(null);
        }
        labelSet.add(field.label);
    }
    return errors;
}
