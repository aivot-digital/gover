import React, {useId} from 'react';
import {Box, Grid, MenuItem, Select, Tooltip} from '@mui/material';
import {type GridRenderCellParams} from '@mui/x-data-grid';
import Warning from '@aivot/mui-material-symbols-400-n25-outlined/Warning';
import {TableFieldComponent2} from '../../../components/table-field/table-field-component-2';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {AlertComponent} from '../../../components/alert/alert-component';
import {CodeListSourceType, isCodeListSyncable} from '../enums/code-list-source-type';
import {type CodeList, type CodeListColumnMapping, getAvailableColumnMappings, getDuplicateCodeListColumnError} from '../models/code-list';

interface CodeListColumnMappingsInputProps {
    codeList: CodeList;
    // The saved mappings, to warn about column keys that references may still use.
    savedColumnMappings: CodeListColumnMapping[];
    onChange: (patch: Pick<CodeList, 'columnMappings' | 'keyColumnKey'>) => void;
    disabled?: boolean;
    error?: string;
    keyColumnError?: string;
}

export function CodeListColumnMappingsInput(props: CodeListColumnMappingsInputProps) {
    const {codeList, savedColumnMappings, onChange, disabled, error, keyColumnError} = props;
    const sourceHintId = useId();
    const isManual = codeList.sourceType === CodeListSourceType.Manual;
    const hasExternalSource = isCodeListSyncable(codeList.sourceType);
    const mappings = codeList.columnMappings;
    const availableMappings = getAvailableColumnMappings(codeList);
    const keyColumnOptions = availableMappings.map((mapping) => ({
        label: mapping.name,
        subLabel: mapping.key,
        value: mapping.key,
    }));
    const boundIndexes = new Set(availableMappings.map((mapping) => mapping.index));
    const sourceOptions = codeList.columns.map((source, index) => ({value: index + 1, label: `${index + 1} – ${source}`}));
    const unusedSource = sourceOptions.find((option) => !boundIndexes.has(option.value));
    const removedKeys = savedColumnMappings
        .map((mapping) => mapping.key)
        .filter((key) => !mappings.some((mapping) => mapping.key === key));
    const duplicateError = getDuplicateCodeListColumnError(mappings, codeList.columns);

    const handleRowsChange = (rows: CodeListColumnMapping[] | null) => {
        // Unsaved manual columns have no stored cells yet. Close gaps left by deleting one of those drafts.
        let nextIndex = codeList.columns.length + 1;
        const columnMappings = (rows ?? []).map((mapping) =>
            isManual && mapping.index != null && mapping.index > codeList.columns.length
                ? {...mapping, index: nextIndex++}
                : mapping,
        );
        // The key column follows a renamed key in the same row, so the list keeps its key column.
        const keyColumnRow = mappings.findIndex((row) => row.key === codeList.keyColumnKey);
        const renamedKeyColumn = keyColumnRow >= 0 && columnMappings.length === mappings.length
            ? columnMappings[keyColumnRow].key
            : codeList.keyColumnKey;
        onChange({
            columnMappings,
            keyColumnKey: columnMappings.some((mapping) => mapping.key === renamedKeyColumn) ? renamedKeyColumn : null,
        });
    };

    const createDefaultRow = (): CodeListColumnMapping => {
        if (hasExternalSource && unusedSource != null) {
            const name = codeList.columns[unusedSource.value - 1].trim();
            let key = name;
            for (let suffix = 2; mappings.some((mapping) => mapping.key === key); suffix++) {
                key = `${name}_${suffix}`;
            }
            return {key, name, index: unusedSource.value};
        }
        // Append beyond existing cells so a newly added manual column does not reuse deleted data.
        const nextIndex = Math.max(codeList.columns.length, ...mappings.map((mapping) => mapping.index ?? 0)) + 1;
        return {key: '', name: '', index: isManual ? nextIndex : null};
    };

    const renderSource = ({row, id, tabIndex}: GridRenderCellParams<CodeListColumnMapping>) => {
        const option = sourceOptions.find((source) => source.value === row.index);
        const missing = hasExternalSource && option == null && row.formerSourceColumn != null;
        const hint = missing ? `Die bisherige Quellspalte „${row.formerSourceColumn}“ fehlt. Wählen Sie eine andere Quellspalte oder löschen Sie die Zuordnung.` : undefined;
        const hintId = `${sourceHintId}-${id}`;
        return (
            <>
                <Select<number | ''>
                    size="small"
                    fullWidth
                    displayEmpty
                    disabled={disabled || sourceOptions.length === 0}
                    value={option?.value ?? ''}
                    inputProps={{
                        'aria-label': `${hasExternalSource ? 'Quellspalte' : 'Position'} für ${row.name || row.key}`,
                        'aria-describedby': missing ? hintId : undefined,
                        tabIndex,
                    }}
                    renderValue={() => {
                        if (option) return option.label;
                        if (!missing) return hasExternalSource ? 'Keine Quellspalte zugeordnet' : 'Nicht in der Quelle vorhanden';
                        return (
                            <Tooltip title={hint}>
                                <Box component="span" sx={{display: 'inline-flex', alignItems: 'center', gap: 1, color: 'warning.dark'}}>
                                    <Warning fontSize="small"/>
                                    „{row.formerSourceColumn}“ fehlt in der Quelle
                                </Box>
                            </Tooltip>
                        );
                    }}
                    onChange={(event) => {
                        const index = event.target.value === '' ? null : Number(event.target.value);
                        handleRowsChange(mappings.map((mapping, rowIndex) => rowIndex === id
                            ? {...mapping, index, formerSourceColumn: index == null ? mapping.formerSourceColumn : null}
                            : mapping));
                    }}
                >
                    {!hasExternalSource && <MenuItem value="">Nicht in der Quelle vorhanden</MenuItem>}
                    {sourceOptions.map((source) => (
                        <MenuItem key={source.value} value={source.value}>{source.label}</MenuItem>
                    ))}
                </Select>
                {missing && <span hidden id={hintId}>{hint}</span>}
            </>
        );
    };

    return (
        <Grid container columnSpacing={4}>
            {removedKeys.length > 0 && (
                <Grid size={{xs: 12}}>
                    <AlertComponent color="warning" sx={{mt: 2, mb: 0}}>
                        Auswahlfelder, Skripte und Prozessschritte verweisen über den Spaltenschlüssel auf Spalten.
                        Verweise auf {removedKeys.map((key) => `„${key}“`).join(', ')} werden nicht angepasst.
                        Passen Sie diese Verweise nach dem Speichern selbst an.
                    </AlertComponent>
                </Grid>
            )}

            <Grid size={{xs: 12}}>
                <TableFieldComponent2<CodeListColumnMapping>
                    label="Spalten"
                    hint={hasExternalSource
                        ? 'Wählen Sie für jede Spalte der Codeliste aus, welche Quellspalte ihre Werte liefert. Die Einträge zeigen die Spalten in der Reihenfolge dieser Tabelle.'
                        : 'Der Spaltenschlüssel identifiziert eine Spalte in Auswahlfeldern und Skripten. Der Anzeigename wird überall angezeigt, wo die Spalte ausgewählt wird.'}
                    error={duplicateError ?? error}
                    noRowsPlaceholder="Noch keine Spalten definiert."
                    disableColumnSorting
                    fields={[
                        {key: 'key', label: 'Spaltenschlüssel', required: true, disabled},
                        {key: 'name', label: 'Anzeigename', required: true, disabled},
                        ...(!isManual ? [{
                            key: 'index' as const,
                            label: hasExternalSource ? 'Quellspalte' : 'Position',
                            required: hasExternalSource,
                            disabled: true, // The select writes the assignment directly to the form.
                            renderCell: renderSource,
                        }] : []),
                    ]}
                    createDefaultRow={createDefaultRow}
                    value={mappings}
                    onChange={handleRowsChange}
                    disabled={disabled}
                    required={isManual}
                    addLabel="Spalte hinzufügen"
                    addDisabledReason={hasExternalSource && unusedSource == null ? 'Keine weitere Quellspalte verfügbar' : undefined}
                />
            </Grid>

            <Grid size={{xs: 12, lg: 6}}>
                <SelectFieldComponent
                    label="Schlüsselspalte"
                    hint="Die Werte dieser Spalte identifizieren die Einträge, zum Beispiel wenn beim Synchronisieren veraltete Einträge beibehalten werden. Sie müssen eindeutig sein."
                    value={codeList.keyColumnKey ?? undefined}
                    onChange={(keyColumnKey) => onChange({columnMappings: mappings, keyColumnKey: keyColumnKey ?? null})}
                    options={keyColumnOptions}
                    required={isManual || keyColumnOptions.length > 0}
                    error={keyColumnError}
                    disabled={disabled || keyColumnOptions.length === 0}
                    emptyStatePlaceholder="Noch keine Spalten vorhanden"
                />
            </Grid>
        </Grid>
    );
}
