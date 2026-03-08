import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import DatabaseSearch from '@aivot/mui-material-symbols-400-outlined/dist/database-search/DatabaseSearch';
import {Box} from '@mui/material';
import {
    isNoCodeNodeDataReference,
    NoCodeInstanceDataReference,
    NoCodeNodeDataReference,
    NoCodeProcessDataReference,
    isNoCodeProcessDataReference,
} from '../../../models/functions/no-code-expression';
import {TextFieldComponent} from '../../../components/text-field/text-field-component';

interface NoCodeOperandEditorProcessDataReferenceProps {
    label: string;
    hint?: string;
    value: NoCodeProcessDataReference | NoCodeInstanceDataReference | NoCodeNodeDataReference;
    onChange: (value: NoCodeProcessDataReference | NoCodeInstanceDataReference | NoCodeNodeDataReference | undefined) => void;
    onAddEnclosingExpression: () => void;
}

export function NoCodeOperandEditorProcessDataReference(props: NoCodeOperandEditorProcessDataReferenceProps) {
    const {
        label,
        hint,
        value,
        onChange,
        onAddEnclosingExpression,
    } = props;

    const isProcessDataReference = isNoCodeProcessDataReference(value);
    const isNodeDataReference = isNoCodeNodeDataReference(value);
    const sourceLabel = isProcessDataReference
        ? 'Vorgangsdaten ($)'
        : isNodeDataReference
            ? 'Elementdaten (_)'
            : 'Geschützte Vorgangsdaten ($$)';

    return (
        <Box sx={{display: 'flex', flexDirection: 'column', gap: 1}}>
            {
                isNodeDataReference &&
                <TextFieldComponent
                    label="Datenschlüssel"
                    hint="Der Datenschlüssel des Prozessknotens, dessen Ergebnis gelesen werden soll."
                    value={value.nodeDataKey ?? undefined}
                    onChange={(nodeDataKey) => onChange({
                        ...value,
                        nodeDataKey: nodeDataKey ?? undefined,
                    })}
                    muiPassTroughProps={{margin: 'none'}}
                />
            }

            <TextFieldComponent
                label={`${label ?? ''} — (${sourceLabel})`}
                hint={hint}
                value={value.path ?? undefined}
                onChange={(path) => {
                    onChange({
                        ...value,
                        path: path ?? undefined,
                    });
                }}
                startIcon={<DatabaseSearch />}
                endAction={[
                    {
                        icon: <Delete />,
                        tooltip: 'Diesen Vorgangsdaten-Verweis löschen',
                        onClick: () => onChange(undefined),
                    },
                    {
                        tooltip: 'Diesen Verweis mit einem Ausdruck verknüpfen',
                        icon: <Functions />,
                        onClick: onAddEnclosingExpression,
                    },
                ]}
                muiPassTroughProps={{margin: 'none'}}
            />
        </Box>
    );
}
