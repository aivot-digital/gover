import React, {useMemo, useReducer} from 'react';
import {ConditionSetOperator} from '../../data/condition-set-operator';
import {BaseCodeTab} from './base-code-tab';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {ConditionOperator} from '../../data/condition-operator';
import {CodeEditor} from '../code-editor/code-editor';
import {CodeTabNoCodeEditor} from '../code-tab-no-code-editor';
import {ExpressionEditorWrapper} from './components/expression-editor-wrapper/expression-editor-wrapper';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {ValueCodeTabProps} from './value-code-tab-props';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {SelectElementDialog} from '../../dialogs/select-element-dialog/select-element-dialog';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import LocationSearchingIcon from '@mui/icons-material/LocationSearching';

const exampleLegacyValueCode = `/**
 * Diese Funktion wird aufgerufen, um einen Wert für das Element zu berechnen.
 * Der Wert wird dann in der Anzeige des Elements verwendet und muss dem Typ des Elements entsprechen.
 *
 * @param{Data} data Die Nutzereingaben
 * @param{CurrentElement} element Das aktuelle Element
 * @param{string} id Die ID des aktuellen Elements\
 */
function main(data, element, id) {
    console.log(data, element, id);
    return null;
}`;

const exampleValueCode = `(function(){
    // Diese Funktion wird aufgerufen, um einen Wert für das Element zu berechnen.
    // Der Wert wird dann in der Anzeige des Elements verwendet und muss dem Typ des Elements entsprechen.
    return null;
})();`;

export function ValueCodeTab(props: ValueCodeTabProps) {
    const dispatch = useAppDispatch();
    const [showElementSelectDialog, toggleShowElementSelectDialog] = useReducer((state) => !state, false);

    const hasVisibilityFunction = useMemo(() => {
        return (
                props.element.computeValue != null && (
                    isStringNotNullOrEmpty(props.element.computeValue.code) ||
                    (
                        props.element.computeValue.conditionSet != null &&
                        (props.element.computeValue.conditionSet.conditions != null && props.element.computeValue.conditionSet.conditions.length > 0) ||
                        (props.element.computeValue.conditionSet?.conditionsSets != null && props.element.computeValue.conditionSet.conditionsSets.length > 0)
                    )
                )
            ) ||
            isStringNotNullOrEmpty(props.element.valueCode?.code) ||
            props.element.valueExpression != null;
    }, [props.element]);

    return (
        <>
            <BaseCodeTab
                label="Dynamischer Wert"
                requirements={props.element.computeValue?.requirements}
                onRequirementsChange={(req) => {
                    props.onChange({
                        computeValue: {
                            ...props.element.computeValue,
                            requirements: req ?? '',
                        },
                    });
                }}
                onDeleteFunction={() => {
                    props.onChange({
                        computeValue: {
                            requirements: props.element.computeValue?.requirements ?? '',
                        },
                        valueCode: undefined,
                        valueExpression: undefined,
                    });
                }}
                editable={props.editable}
                allowsNoCode={false}
                allowsExpression={true}
                onSelectFunction={(type) => {
                    switch (type) {
                        case 'legacy-code':
                            props.onChange({
                                computeValue: {
                                    requirements: props.element.computeValue?.requirements ?? '',
                                    code: exampleLegacyValueCode,
                                    conditionSet: undefined,
                                },
                                valueCode: undefined,
                                valueExpression: undefined,
                            });
                            break;
                        case 'legacy-condition':
                            props.onChange({
                                computeValue: {
                                    requirements: props.element.computeValue?.requirements ?? '',
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
                                valueCode: undefined,
                                valueExpression: undefined,
                            });
                            break;
                        case 'code':
                            props.onChange({
                                computeValue: {
                                    requirements: props.element.computeValue?.requirements ?? '',
                                    code: undefined,
                                    conditionSet: undefined,
                                },
                                valueCode: {
                                    code: exampleValueCode,
                                },
                                valueExpression: undefined,
                            });
                            break;
                        case 'expression':
                            props.onChange({
                                computeValue: {
                                    requirements: props.element.computeValue?.requirements ?? '',
                                    code: undefined,
                                    conditionSet: undefined,
                                },
                                valueCode: undefined,
                                valueExpression: {
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
                    props.element.computeValue?.code != null && (
                        <CodeEditor
                            value={props.element.computeValue.code}
                            onChange={(code) => {
                                props.onChange({
                                    computeValue: {
                                        requirements: props.element.computeValue?.requirements ?? '',
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
                    props.element.valueCode?.code != null && (
                        <CodeEditor
                            value={props.element.valueCode.code}
                            onChange={(code) => {
                                props.onChange({
                                    valueCode: {
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
                    props.element.computeValue?.conditionSet != null && (
                        <CodeTabNoCodeEditor
                            parents={props.parents}
                            element={props.element}
                            func={props.element.computeValue}
                            onChange={(updatedFunc) => {
                                props.onChange({
                                    computeValue: updatedFunc,
                                });
                            }}
                            shouldReturnString={false}
                            editable={props.editable}
                        />
                    )
                }
                {
                    props.element.valueExpression != null && (
                        <ExpressionEditorWrapper
                            parents={props.parents}
                            expression={props.element.valueExpression}
                            onChange={(expression) => {
                                props.onChange({
                                    valueExpression: expression,
                                });
                            }}
                            editable={props.editable}
                            desiredReturnType={NoCodeDataType.Any}
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
