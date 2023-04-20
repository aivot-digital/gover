import {Box, Button, Checkbox, FormControl, FormControlLabel, Grid, TextField, Typography} from '@mui/material';
import {TableFieldComponentColumnModel, TableFieldElement} from '../../models/elements/./form/./input/table-field-element';
import {BaseEditorProps} from '../_lib/base-editor-props';
import {faPlus} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {isNullOrEmpty} from '../../utils/is-null-or-empty';

export function TableFieldComponentEditor(props: BaseEditorProps<TableFieldElement>) {
    const columnLabelErrors = makeColumnLabelErrors(props.component.fields);
    const minRequiredError = (
        props.component.minimumRequiredRows != null &&
        props.component.maximumRows != null &&
        props.component.maximumRows > 0 &&
        props.component.minimumRequiredRows > props.component.maximumRows
    );

    return (
        <>
            <TextField
                value={props.component.label ?? ''}
                label="Titel"
                margin="normal"
                onChange={event => props.onPatch({
                    label: event.target.value,
                })}
            />

            <TextField
                value={props.component.hint ?? ''}
                label="Hinweis"
                margin="normal"
                onChange={event => props.onPatch({
                    hint: event.target.value,
                })}
            />

            <FormControl>
            <FormControlLabel
                control={
                    <Checkbox
                        checked={props.component.required ?? false}
                        onChange={event => props.onPatch({
                            required: event.target.checked,
                            minimumRequiredRows: event.target.checked ? 1 : undefined,
                        })}
                    />
                }
                label="Pflichtangabe"
            />
            </FormControl>

        <FormControl>
            <FormControlLabel
                control={
                    <Checkbox
                        checked={props.component.disabled ?? false}
                        onChange={event => props.onPatch({
                            disabled: event.target.checked,
                        })}
                    />
                }
                label="Eingabe deaktiviert"
            />
        </FormControl>

            {
                props.component.required &&
                <TextField
                    value={props.component.minimumRequiredRows?.toString() ?? ''}
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
                        if (props.component.minimumRequiredRows == null || props.component.minimumRequiredRows === 0) {
                            props.onPatch({
                                required: false,
                            });
                        }
                    }}
                    error={minRequiredError}
                />
            }

            <TextField
                value={props.component.maximumRows?.toString() ?? ''}
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
                (props.component.fields ?? []).map((column, index) => {
                    const onChange = (patch: Partial<TableFieldComponentColumnModel>) => {
                        const patchedList = [...(props.component.fields ?? [])];
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
                                        const updatedFields = [...(props.component.fields ?? [])];
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
                startIcon={<FontAwesomeIcon icon={faPlus} />}
                variant="contained"
                onClick={() => {
                    props.onPatch({
                        fields: [
                            ...(props.component.fields ?? []),
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
        if (isNullOrEmpty(field.label) || field.label.length < 3) {
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
