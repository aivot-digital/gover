import React, {useMemo} from 'react';
import Grid from '@mui/material/Grid';
import {TableFieldComponent2} from '../../../components/table-field/table-field-component-2';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {AlertComponent} from '../../../components/alert/alert-component';
import {CodeListSourceType} from '../enums/code-list-source-type';
import {type CodeList, type CodeListColumnMapping, getAvailableColumnMappings} from '../models/code-list';

interface CodeListColumnMappingsInputProps {
    codeList: CodeList;
    // The saved mappings, to warn about column keys that references may still use.
    savedColumnMappings: CodeListColumnMapping[];
    onChange: (patch: Pick<CodeList, 'columnMappings' | 'keyColumnKey'>) => void;
    disabled?: boolean;
    error?: string;
    keyColumnError?: string;
}

// The data grid edits strings, so the position is kept as text while editing.
interface ColumnMappingRow {
    key: string;
    name: string;
    index: string;
}

export function CodeListColumnMappingsInput(props: CodeListColumnMappingsInputProps) {
    const {
        codeList,
        savedColumnMappings,
        onChange,
        disabled,
        error,
        keyColumnError,
    } = props;

    const isManual = codeList.sourceType === CodeListSourceType.Manual;
    const rows = useMemo(() => codeList.columnMappings.map((mapping): ColumnMappingRow => ({
        key: mapping.key,
        name: mapping.name,
        index: mapping.index == null ? '' : String(mapping.index),
    })), [codeList.columnMappings]);

    // Synchronized lists point to the columns of their source. Manual lists can also use the next free position.
    const highestIndex = Math.max(codeList.columns.length, ...codeList.columnMappings.map((mapping) => mapping.index ?? 0));
    const positionOptions = useMemo(() => [
        {value: '', label: isManual ? 'Keine Position' : 'Nicht in der Quelle vorhanden'},
        ...Array.from({length: highestIndex}, (_, position) => ({
            value: String(position + 1),
            label: codeList.columns[position] ? `${position + 1} – ${codeList.columns[position]}` : String(position + 1),
        })),
    ], [codeList.columns, highestIndex, isManual]);

    const keyColumnOptions = useMemo(() => getAvailableColumnMappings(codeList).map((mapping) => ({
        label: mapping.name,
        subLabel: mapping.key,
        value: mapping.key,
    })), [codeList]);

    const removedKeys = savedColumnMappings
        .map((mapping) => mapping.key)
        .filter((key) => !codeList.columnMappings.some((mapping) => mapping.key === key));

    const handleRowsChange = (changedRows: ColumnMappingRow[] | null) => {
        const columnMappings = (changedRows ?? []).map((row): CodeListColumnMapping => ({
            key: row.key ?? '',
            name: row.name ?? '',
            index: row.index == null || row.index === '' ? null : Number.parseInt(row.index, 10),
        }));
        // The key column follows a renamed key in the same row, so the list keeps its key column.
        const keyColumnRow = rows.findIndex((row) => row.key === codeList.keyColumnKey);
        const renamedKeyColumn = keyColumnRow >= 0 && keyColumnRow < columnMappings.length && changedRows?.length === rows.length
            ? columnMappings[keyColumnRow].key
            : codeList.keyColumnKey;

        onChange({
            columnMappings,
            keyColumnKey: columnMappings.some((mapping) => mapping.key === renamedKeyColumn) ? renamedKeyColumn : null,
        });
    };

    return (
        <Grid
            container
            columnSpacing={4}
        >
            {
                removedKeys.length > 0 &&
                <Grid size={{xs: 12}}>
                    <AlertComponent
                        color="warning"
                        sx={{mt: 2, mb: 0}}
                    >
                        Auswahlfelder, Skripte und Prozessschritte verweisen über den Spaltenschlüssel auf Spalten.
                        Verweise auf {removedKeys.map((key) => `„${key}“`).join(', ')} werden nicht angepasst.
                        Passen Sie diese Verweise nach dem Speichern selbst an.
                    </AlertComponent>
                </Grid>
            }

            <Grid size={{xs: 12}}>
                <TableFieldComponent2<ColumnMappingRow>
                    label="Spalten"
                    hint={isManual
                        ? 'Der Spaltenschlüssel identifiziert eine Spalte in Auswahlfeldern und Skripten. Der Anzeigename wird überall angezeigt, wo die Spalte ausgewählt wird.'
                        : 'Der Spaltenschlüssel identifiziert eine Spalte in Auswahlfeldern und Skripten. Bei einer Synchronisierung werden neue Spalten der Quelle ergänzt; eine geänderte Reihenfolge ändert die Zuordnung nicht.'}
                    error={error}
                    noRowsPlaceholder={isManual
                        ? 'Noch keine Spalten definiert.'
                        : 'Die Spalten werden bei der ersten Synchronisierung aus der Quelle übernommen.'}
                    fields={[
                        {key: 'key', label: 'Spaltenschlüssel', required: true, disabled},
                        {key: 'name', label: 'Anzeigename', required: true, disabled},
                        {key: 'index', label: 'Position', disabled, valueOptions: positionOptions},
                    ]}
                    createDefaultRow={() => ({
                        key: '',
                        name: '',
                        index: isManual ? String(highestIndex + 1) : '',
                    })}
                    value={rows}
                    onChange={handleRowsChange}
                    disabled={disabled}
                    required={isManual}
                    addLabel="Spalte hinzufügen"
                />
            </Grid>

            <Grid size={{xs: 12, lg: 6}}>
                <SelectFieldComponent
                    label="Schlüsselspalte"
                    hint="Die Werte dieser Spalte identifizieren die Einträge, zum Beispiel wenn beim Synchronisieren veraltete Einträge beibehalten werden. Sie müssen eindeutig sein."
                    value={codeList.keyColumnKey ?? undefined}
                    onChange={(keyColumnKey) => onChange({
                        columnMappings: codeList.columnMappings,
                        keyColumnKey: keyColumnKey ?? null,
                    })}
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
