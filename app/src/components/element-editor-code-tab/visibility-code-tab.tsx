import React, {useMemo, useReducer} from 'react';
import {ConditionSetOperator} from '../../data/condition-set-operator';
import {VisibilityCodeTabProps} from './visibility-code-tab-props';
import {BaseCodeTab} from './base-code-tab';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {ConditionOperator} from '../../data/condition-operator';
import {CodeEditor} from '../code-editor/code-editor';
import {CodeTabNoCodeEditor} from '../code-tab-no-code-editor';
import {ExpressionEditorWrapper} from './components/expression-editor-wrapper/expression-editor-wrapper';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import LocationSearchingIcon from '@mui/icons-material/LocationSearching';
import {SelectElementDialog} from '../../dialogs/select-element-dialog/select-element-dialog';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';

const exampleLegacyVisibilityCode = `/**
 * Diese Funktion wird aufgerufen, um zu überprüfen, ob das Element sichtbar ist.
 * Gibt die Funktion true zurück, wird das Element angezeigt, andernfalls nicht.
 *
 * @param{Data} data Die Nutzereingaben
 * @param{CurrentElement} element Das aktuelle Element
 * @param{string} id Die ID des aktuellen Elements\
 */
function main(data, element, id) {
    console.log(data, element, id);
    return true;
}`;

const exampleVisibilityCode = `(function(){
    // Hier kann der Code eingefügt werden, der bestimmt, ob das Element sichtbar ist.
    // Gibt die Funktion true zurück, wird das Element angezeigt, andernfalls nicht.
    return true;
})();`;

export function VisibilityCodeTab(props: VisibilityCodeTabProps) {
    const dispatch = useAppDispatch();
    const [showElementSelectDialog, toggleShowElementSelectDialog] = useReducer((state) => !state, false);

    const hasVisibilityFunction = useMemo(() => {
        return (
                props.element.isVisible != null && (
                    isStringNotNullOrEmpty(props.element.isVisible.code) ||
                    (
                        props.element.isVisible.conditionSet != null &&
                        (props.element.isVisible.conditionSet.conditions != null && props.element.isVisible.conditionSet.conditions.length > 0) ||
                        (props.element.isVisible.conditionSet?.conditionsSets != null && props.element.isVisible.conditionSet.conditionsSets.length > 0)
                    )
                )
            ) ||
            isStringNotNullOrEmpty(props.element.visibilityCode?.code) ||
            props.element.visibilityExpression != null;
    }, [props.element]);

    return (
        <>
            <BaseCodeTab
                label="Sichtbarkeit"
                requirements={props.element.isVisible?.requirements}
                onRequirementsChange={(req) => {
                    props.onChange({
                        isVisible: {
                            ...props.element.isVisible,
                            requirements: req ?? '',
                        },
                    });
                }}
                onDeleteFunction={() => {
                    props.onChange({
                        isVisible: {
                            requirements: props.element.isVisible?.requirements ?? '',
                        },
                        visibilityCode: undefined,
                        visibilityExpression: undefined,
                    });
                }}
                editable={props.editable}
                allowsNoCode={true}
                allowsExpression={true}
                onSelectFunction={(type) => {
                    switch (type) {
                        case 'legacy-code':
                            props.onChange({
                                isVisible: {
                                    requirements: props.element.isVisible?.requirements ?? '',
                                    code: exampleLegacyVisibilityCode,
                                    conditionSet: undefined,
                                },
                                visibilityCode: undefined,
                                visibilityExpression: undefined,
                            });
                            break;
                        case 'legacy-condition':
                            props.onChange({
                                isVisible: {
                                    requirements: props.element.isVisible?.requirements ?? '',
                                    code: undefined,
                                    conditionSet: {
                                        operator: ConditionSetOperator.Any,
                                        conditions: [
                                            {
                                                reference: '',
                                                operator: ConditionOperator.Equals,
                                                value: '',
                                                conditionUnmetMessage: '',
                                            },
                                        ],
                                        conditionsSets: [],
                                        conditionSetUnmetMessage: '',
                                    },
                                },
                                visibilityCode: undefined,
                                visibilityExpression: undefined,
                            });
                            break;
                        case 'code':
                            props.onChange({
                                isVisible: {
                                    requirements: props.element.isVisible?.requirements ?? '',
                                    code: undefined,
                                    conditionSet: undefined,
                                },
                                visibilityCode: {
                                    code: exampleVisibilityCode,
                                },
                                visibilityExpression: undefined,
                            });
                            break;
                        case 'expression':
                            props.onChange({
                                isVisible: {
                                    requirements: props.element.isVisible?.requirements ?? '',
                                    code: undefined,
                                    conditionSet: undefined,
                                },
                                visibilityCode: undefined,
                                visibilityExpression: {
                                    operatorIdentifier: '',
                                    operands: [],
                                },
                            });
                            break;
                    }
                }}
                hasFunction={hasVisibilityFunction}
            >
                {
                    props.element.isVisible?.code != null && (
                        <CodeEditor
                            value={props.element.isVisible.code}
                            onChange={(code) => {
                                props.onChange({
                                    isVisible: {
                                        requirements: props.element.isVisible?.requirements ?? '',
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
                    props.element.visibilityCode?.code != null && (
                        <CodeEditor
                            value={props.element.visibilityCode.code}
                            onChange={(code) => {
                                props.onChange({
                                    visibilityCode: {
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
                    props.element.isVisible?.conditionSet != null && (
                        <CodeTabNoCodeEditor
                            parents={props.parents}
                            element={props.element}
                            func={props.element.isVisible}
                            onChange={(updatedFunc) => {
                                props.onChange({
                                    isVisible: updatedFunc,
                                });
                            }}
                            shouldReturnString={false}
                            editable={props.editable}
                        />
                    )
                }
                {
                    props.element.visibilityExpression != null && (
                        <ExpressionEditorWrapper
                            parents={props.parents}
                            expression={props.element.visibilityExpression}
                            onChange={(expression) => {
                                props.onChange({
                                    visibilityExpression: expression,
                                });
                            }}
                            editable={props.editable}
                            desiredReturnType={NoCodeDataType.Boolean}
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
