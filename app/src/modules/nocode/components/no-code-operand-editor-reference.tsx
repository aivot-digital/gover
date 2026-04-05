import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import {isNoCodeReference, NoCodeReference} from '../../../models/functions/no-code-expression';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {generateComponentPath, generateComponentTitle} from '../../../utils/generate-component-title';
import MyLocation from '@aivot/mui-material-symbols-400-outlined/dist/my-location/MyLocation';
import DatabaseSearch from '@aivot/mui-material-symbols-400-outlined/dist/database-search/DatabaseSearch';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {SelectElementDialog} from '../../../dialogs/select-element-dialog/select-element-dialog';
import {useMemo, useState} from 'react';
import {SelectFieldComponentOption} from '../../../components/select-field/select-field-component-option';
import {elementMatchesDesiredNoCodeDataType} from '../data/no-code-data-type-map';

interface NoCodeOperandEditorReferenceProps {
    allElements: ElementWithParents[];
    label: string;
    hint?: string;
    value: NoCodeReference;
    onChange: (value: NoCodeReference | undefined) => void;
    desiredType: NoCodeDataType;
    onAddEnclosingExpression: () => void;
}

export function NoCodeOperandEditorReference(props: NoCodeOperandEditorReferenceProps) {
    const {
        allElements,
        label,
        hint,
        value: operand,
        onChange,
        desiredType,
        onAddEnclosingExpression,
    } = props;

    const selectableElements = useMemo(() => {
        return allElements;
            // Remove this for now, because we have no current concept of how to limit this compatability.
            //.filter((e) => elementMatchesDesiredNoCodeDataType(e.element.type, desiredType));
    }, [allElements, desiredType]);

    const allElementOptions: SelectFieldComponentOption[] = useMemo(() => {
        return selectableElements
            .map((e) => ({
                label: generateComponentTitle(e.element),
                subLabel: generateComponentPath(e.parents),
                value: e.element.id,
            }));
    }, [selectableElements]);

    const [showSelectElementDialog, setShowSelectElementDialog] = useState(false);

    return (
        <>
            <SelectFieldComponent
                label={`${label ?? ''} — (Wert eines Elementes)`}
                value={operand.elementId ?? ''}
                onChange={(val) => {
                    onChange({
                        ...operand,
                        elementId: val,
                    });
                }}
                hint={hint}
                options={allElementOptions}
                startIcon={<MyLocation />}
                endAction={[
                    {
                        tooltip: 'Diesen Verweis auf ein Element löschen',
                        icon: <Delete />,
                        onClick: () => {
                            onChange(undefined);
                        },
                    },
                    {
                        tooltip: 'Element für diesen Verweis auswählen',
                        icon: <DatabaseSearch />,
                        onClick: () => {
                            setShowSelectElementDialog(true);
                        },
                    },
                    {
                        tooltip: 'Diesen Verweis mit einem Ausdruck verknüpfen',
                        icon: <Functions />,
                        onClick: onAddEnclosingExpression,
                    },
                ]}
                muiPassTroughProps={{
                    margin: 'none',
                }}
                emptyStatePlaceholder="Keine passenden Elemente verfügbar"
            />

            <SelectElementDialog
                allElements={allElements}
                desiredType={desiredType}
                open={showSelectElementDialog}
                onSelect={(element) => {
                    setShowSelectElementDialog(false);
                    if (isNoCodeReference(operand)) {
                        onChange({
                            ...operand,
                            elementId: element.id,
                        });
                    }
                }}
                onClose={() => setShowSelectElementDialog(false)}
            />
        </>
    );
}
