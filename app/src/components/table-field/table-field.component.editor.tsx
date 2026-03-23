import {Box, Button, Checkbox, FormControlLabel, Grid, TextField, Typography} from '@mui/material';
import {TableFieldComponentColumnModel, TableFieldElement} from '../../models/elements/form/input/table-field-element';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {BaseEditorProps} from '../../editors/base-editor';
import {NumberFieldComponent} from '../number-field/number-field-component';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {generateId} from '../../utils/id-utils';

export function TableFieldComponentEditor(props: BaseEditorProps<TableFieldElement, ElementTreeEntity>) {
    const columnLabelErrors = makeColumnLabelErrors(props.element.fields);
    const columnKeyErrors = makeColumnKeyErrors(props.element.fields);
    const minRequiredError = (
        props.element.minimumRequiredRows != null &&
        props.element.maximumRows != null &&
        props.element.maximumRows > 0 &&
        props.element.minimumRequiredRows > props.element.maximumRows
    );

    return (
        <>
            <Grid
                container
                columnSpacing={4}
            >
                {
                    props.element.required &&
                    <Grid
                        size={{
                            xs: 12,
                            lg: 6
                        }}>
                        <NumberFieldComponent
                            value={props.element.minimumRequiredRows ?? undefined}
                            label="Mindestanzahl der hinzuzufügenden Zeilen"
                            hint="Geben Sie 0 ein, um keine Mindestanzahl zu fordern"
                            error={minRequiredError ? 'Sie fordern mehr Zeilen als Sie maximal zulassen.' : undefined}
                            onChange={(val) => {
                                props.onPatch({
                                    minimumRequiredRows: val,
                                });
                            }}
                            disabled={!props.editable}
                        />
                    </Grid>
                }

                <Grid
                    size={{
                        xs: 12,
                        lg: 6
                    }}>
                    <NumberFieldComponent
                        value={props.element.maximumRows ?? undefined}
                        label="Maximalanzahl der hinzuzufügenden Zeilen"
                        hint="Geben Sie 0 ein, um keine Maximalanzahl zu fordern."
                        error={minRequiredError ? 'Sie fordern mehr Zeilen als Sie maximal zulassen.' : undefined}
                        onChange={(val) => {
                            props.onPatch({
                                maximumRows: val,
                            });
                        }}
                        disabled={!props.editable}
                    />
                </Grid>
            </Grid>
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
                        };
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
                                <Grid size={3}>
                                    <TextField
                                        label="Schlüssel"
                                        margin="normal"
                                        value={column.key}
                                        onChange={event => onChange({
                                            key: event.target.value,
                                        })}
                                        onBlur={() => onChange({
                                            key: (column.key ?? '').trim(),
                                        })}
                                        error={columnKeyErrors[index] != null}
                                        helperText={columnKeyErrors[index] ?? undefined}
                                        disabled={!props.editable}
                                    />
                                </Grid>
                                <Grid size={3}>
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
                                        helperText={columnLabelErrors[index] ?? undefined}
                                        disabled={!props.editable}
                                    />
                                </Grid>
                                <Grid size={3}>
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
                                        helperText={"Ein Platzhalter zeigt ein Beispiel für die erwartete Eingabe an, z. B. „hallo@bad-musterstadt.de“ bei einer E-Mail-Adresse."}
                                        disabled={!props.editable}
                                    />
                                </Grid>
                                {
                                    column.datatype === 'number' &&
                                    <Grid size={3}>
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
                                            disabled={!props.editable}
                                        />
                                    </Grid>
                                }
                            </Grid>
                            <Box
                                sx={{
                                    display: 'flex',
                                    justifyContent: 'space-between',
                                }}
                            >
                                <Box>
                                    <FormControlLabel
                                        control={
                                            <Checkbox
                                                checked={column.datatype === 'number'}
                                                onChange={event => onChange({
                                                    datatype: event.target.checked ? 'number' : 'string',
                                                })}
                                                disabled={!props.editable}
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
                                                disabled={!props.editable}
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
                                                disabled={!props.editable}
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
                                    disabled={!props.editable}
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
                startIcon={<AddOutlinedIcon/>}
                variant="contained"
                onClick={() => {
                    props.onPatch({
                        fields: [
                            ...(props.element.fields ?? []),
                            {
                                key: generateId(5),
                                label: 'Neue Spalte',
                                datatype: 'string',
                                placeholder: undefined,
                                optional: true,
                                disabled: false,
                                decimalPlaces: undefined,
                            },
                        ],
                    });
                }}
                disabled={!props.editable}
            >
                Spalte Hinzufügen
            </Button>
        </>
    );
}

function makeColumnLabelErrors(fields?: TableFieldComponentColumnModel[] | null | undefined): (string | null)[] {
    if (fields == null) {
        return [];
    }
    const errors: (string | null)[] = [];
    const labelSet = new Set<string>();
    for (const field of fields) {
        if (isStringNullOrEmpty(field.label) || field.label == null || field.label.length < 3) {
            errors.push('Bitte geben Sie einen Titel mit mindestens drei Zeichen ein.');
        } else if (labelSet.has(field.label)) {
            errors.push('Eine Spalte mit diesem Titel existiert bereits. Bitte geben Sie einen einzigartigen Titel ein.');
        } else {
            errors.push(null);
        }
        labelSet.add(field.label ?? '');
    }
    return errors;
}

function makeColumnKeyErrors(fields?: TableFieldComponentColumnModel[] | null | undefined): (string | null)[] {
    if (fields == null) {
        return [];
    }
    const errors: (string | null)[] = [];
    const keySet = new Set<string>();
    for (const field of fields) {
        const key = (field.key ?? '').trim();
        if (isStringNullOrEmpty(key) || key.length < 3) {
            errors.push('Bitte geben Sie einen Schlüssel mit mindestens drei Zeichen ein.');
        } else if (keySet.has(key)) {
            errors.push('Eine Spalte mit diesem Schlüssel existiert bereits. Bitte geben Sie einen einzigartigen Schlüssel ein.');
        } else {
            errors.push(null);
        }
        keySet.add(key);
    }
    return errors;
}
