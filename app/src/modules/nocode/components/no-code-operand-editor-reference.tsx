import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import Functions from '@aivot/mui-material-symbols-400-outlined/dist/functions/Functions';
import {isNoCodeReference, NoCodeReference} from '../../../models/functions/no-code-expression';
import {SelectFieldComponent} from '../../../components/select-field/select-field-component';
import {generateComponentPath, generateComponentTitle} from '../../../utils/generate-component-title';
import MyLocation from '@aivot/mui-material-symbols-400-outlined/dist/my-location/MyLocation';
import DatabaseSearch from '@aivot/mui-material-symbols-400-outlined/dist/database-search/DatabaseSearch';
import {ElementWithParents} from '../../../utils/flatten-elements';
import {SelectElementDialog} from '../../../dialogs/select-element-dialog/select-element-dialog';
import {useMemo, useState} from 'react';
import {SelectFieldComponentOption} from '../../../components/select-field/select-field-component-option';

interface NoCodeOperandEditorReferenceProps {
    allElements: ElementWithParents[];
    label: string;
    hint?: string;
    value: NoCodeReference;
    onChange: (value: NoCodeReference | undefined) => void;
    onAddEnclosingExpression: () => void;
}

export function NoCodeOperandEditorReference(props: NoCodeOperandEditorReferenceProps) {
    const {
        allElements,
        label,
        hint,
        value: operand,
        onChange,
        onAddEnclosingExpression,
    } = props;

    const {
        elementId,
    } = operand;

    const allElementOptions: SelectFieldComponentOption[] = useMemo(() => {
        return allElements
            .map((e) => ({
                label: generateComponentTitle(e.element),
                subLabel: generateComponentPath(e.parents),
                value: e.element.id,
            }));
    }, [allElements]);

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
            />

            <SelectElementDialog
                allElements={allElements}
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
