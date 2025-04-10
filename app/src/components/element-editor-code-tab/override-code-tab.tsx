import React, {useMemo, useReducer} from 'react';
import {BaseCodeTab} from './base-code-tab';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {CodeEditor} from '../code-editor/code-editor';
import {OverrideCodeTabProps} from './override-code-tab-props';
import {useLogger} from '../../hooks/use-logging';
import LocationSearchingIcon from '@mui/icons-material/LocationSearching';
import {SelectElementDialog} from '../../dialogs/select-element-dialog/select-element-dialog';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';

const exampleLegacyOverrideCode = `/**
 * Diese Funktion wird aufgerufen, die Struktur des Elements zu überschreiben.
 * Die Funktion muss eine gültige Elementstruktur zurückgeben.
 *
 * @param{Data} data Die Nutzereingaben
 * @param{CurrentElement} element Das aktuelle Element
 * @param{string} id Die ID des aktuellen Elements\
 */
function main(data, element, id) {
    console.log(data, element, id);
    return {
        ...element,
    };
}`;

const exampleOverrideCode = `(function(){
    // Hier kann der Code eingefügt werden, um die Struktur des Elements zu überschreiben.
    // Die Funktion muss eine gültige Elementstruktur zurückgeben.
    return {
        ...ctx.element,
    };
})();`;

export function OverrideCodeTab(props: OverrideCodeTabProps) {
    const log = useLogger('OverrideCodeTab');
    const dispatch = useAppDispatch();
    const [showElementSelectDialog, toggleShowElementSelectDialog] = useReducer((state) => !state, false);

    const hasOverrideFunction = useMemo(() => {
        return (
            isStringNotNullOrEmpty(props.element.patchElement?.code) ||
            isStringNotNullOrEmpty(props.element.overrideCode?.code)
        );
    }, [props.element]);

    return (
        <>
            <BaseCodeTab
                label="Elementstruktur überschreiben"
                requirements={props.element.patchElement?.requirements}
                onRequirementsChange={(req) => {
                    props.onChange({
                        patchElement: {
                            ...props.element.patchElement,
                            requirements: req ?? '',
                        },
                    });
                }}
                onDeleteFunction={() => {
                    props.onChange({
                        patchElement: {
                            requirements: props.element.patchElement?.requirements ?? '',
                        },
                        overrideCode: undefined,
                        overrideExpression: undefined,
                    });
                }}
                editable={props.editable}
                allowsNoCode={false}
                allowsExpression={false}
                onSelectFunction={(type) => {
                    switch (type) {
                        case 'legacy-code':
                            props.onChange({
                                patchElement: {
                                    requirements: props.element.patchElement?.requirements ?? '',
                                    code: exampleLegacyOverrideCode,
                                    conditionSet: undefined,
                                },
                                overrideCode: undefined,
                                overrideExpression: undefined,
                            });
                            break;
                        case 'legacy-condition':
                            log.error('Legacy condition set is not supported for overrides.');
                            break;
                        case 'code':
                            props.onChange({
                                patchElement: {
                                    requirements: props.element.patchElement?.requirements ?? '',
                                    code: undefined,
                                    conditionSet: undefined,
                                },
                                overrideCode: {
                                    code: exampleOverrideCode,
                                },
                                overrideExpression: undefined,
                            });
                            break;
                        case 'expression':
                            log.error('Expression is not yet supported for overrides.');
                            break;
                    }
                }}
                hasFunction={hasOverrideFunction}
            >
                {
                    props.element.patchElement?.code != null && (
                        <CodeEditor
                            value={props.element.patchElement.code}
                            onChange={(code) => {
                                props.onChange({
                                    patchElement: {
                                        requirements: props.element.patchElement?.requirements ?? '',
                                        code: code,
                                    },
                                });
                            }}
                            actions={props.editable ? [
                                {
                                    tooltip: 'Element-ID nachschlagen',
                                    icon: <LocationSearchingIcon />,
                                    onClick: toggleShowElementSelectDialog,
                                },
                            ] : []}
                            disabled={!props.editable}
                        />
                    )
                }
                {
                    props.element.overrideCode?.code != null && (
                        <CodeEditor
                            value={props.element.overrideCode.code}
                            onChange={(code) => {
                                props.onChange({
                                    overrideCode: {
                                        code: code,
                                    },
                                });
                            }}
                            actions={props.editable ? [
                                {
                                    tooltip: 'Element-ID nachschlagen',
                                    icon: <LocationSearchingIcon />,
                                    onClick: toggleShowElementSelectDialog,
                                },
                            ] : []}
                            disabled={!props.editable}
                        />
                    )
                }
            </BaseCodeTab>

            <SelectElementDialog
                open={showElementSelectDialog}
                onSelect={(element) => {
                    navigator.clipboard.writeText(element.id);
                    toggleShowElementSelectDialog();
                    dispatch(showSuccessSnackbar('Element-ID kopiert'));
                }}
                onClose={toggleShowElementSelectDialog}
            />
        </>
    );
}
