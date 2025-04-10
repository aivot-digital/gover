import React, {useMemo, useReducer} from 'react';
import {ConditionSetOperator} from '../../data/condition-set-operator';
import {BaseCodeTab} from './base-code-tab';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {ConditionOperator} from '../../data/condition-operator';
import {CodeEditor} from '../code-editor/code-editor';
import {CodeTabNoCodeEditor} from '../code-tab-no-code-editor';
import {ExpressionEditorWrapper} from './components/expression-editor-wrapper/expression-editor-wrapper';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {ValidationCodeTabProps} from './validation-code-tab-props';
import {Box, Button, Typography} from '@mui/material';
import {TextFieldComponent} from '../text-field/text-field-component';
import {Actions} from '../actions/actions';
import DeleteOutlinedIcon from '@mui/icons-material/DeleteOutlined';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {SelectElementDialog} from '../../dialogs/select-element-dialog/select-element-dialog';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import LocationSearchingIcon from '@mui/icons-material/LocationSearching';

const exampleLegacyValidationCode = `/**
 * Diese Funktion wird aufgerufen, um zu überprüfen, ob das Element valide ist.
 * Gibt die Funktion einen String zurück, wird dieser als Fehler angezeigt.
 * Gibt die Funktion null zurück, ist das Element valide.
 *
 * @param{Data} data Die Nutzereingaben
 * @param{CurrentElement} element Das aktuelle Element
 * @param{string} id Die ID des aktuellen Elements\
 */
function main(data, element, id) {
    console.log(data, element, id);
    return null;
}`;

const exampleValidationCode = `(function(){
    // Hier kann der Code eingefügt werden, der bestimmt, ob das Element valide ist.
    // Gibt die Funktion einen String zurück, wird dieser als Fehler angezeigt.
    // Gibt die Funktion null zurück, ist das Element valide.
    return null;
})();`;

export function ValidationCodeTab(props: ValidationCodeTabProps) {
    const dispatch = useAppDispatch();
    const [showElementSelectDialog, toggleShowElementSelectDialog] = useReducer((state) => !state, false);

    const hasValidationFunction = useMemo(() => {
        return (
            isStringNotNullOrEmpty(props.element.validate?.code) ||
            props.element.validate?.conditionSet != null ||
            isStringNotNullOrEmpty(props.element.validationCode?.code) ||
            props.element.validationExpressions != null
        );
    }, [props.element]);

    return (
        <>
            <BaseCodeTab
                label="Validierung"
                requirements={props.element.validate?.requirements}
                onRequirementsChange={(req) => {
                    props.onChange({
                        validate: {
                            ...props.element.validate,
                            requirements: req ?? '',
                        },
                    });
                }}
                onDeleteFunction={() => {
                    props.onChange({
                        validate: {
                            requirements: props.element.validate?.requirements ?? '',
                        },
                        validationCode: undefined,
                        validationExpressions: undefined,
                    });
                }}
                editable={props.editable}
                allowsNoCode={true}
                allowsExpression={true}
                onSelectFunction={(type) => {
                    switch (type) {
                        case 'legacy-code':
                            props.onChange({
                                validate: {
                                    requirements: props.element.validate?.requirements ?? '',
                                    code: exampleLegacyValidationCode,
                                    conditionSet: undefined,
                                },
                                validationCode: undefined,
                                validationExpressions: undefined,
                            });
                            break;
                        case 'legacy-condition':
                            props.onChange({
                                validate: {
                                    requirements: props.element.validate?.requirements ?? '',
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
                                validationCode: undefined,
                                validationExpressions: undefined,
                            });
                            break;
                        case 'code':
                            props.onChange({
                                validate: {
                                    requirements: props.element.validate?.requirements ?? '',
                                    code: undefined,
                                    conditionSet: undefined,
                                },
                                validationCode: {
                                    code: exampleValidationCode,
                                },
                                validationExpressions: undefined,
                            });
                            break;
                        case 'expression':
                            props.onChange({
                                validate: {
                                    requirements: props.element.validate?.requirements ?? '',
                                    code: undefined,
                                    conditionSet: undefined,
                                },
                                validationCode: undefined,
                                validationExpressions: [
                                    {
                                        expression: {
                                            operatorIdentifier: '',
                                            operands: [],
                                        },
                                        message: '',
                                    },
                                ],
                            });
                            break;
                    }
                }}
                hasFunction={hasValidationFunction}
            >
                {
                    props.element.validate?.code != null && (
                        <CodeEditor
                            value={props.element.validate.code}
                            onChange={(code) => {
                                props.onChange({
                                    validate: {
                                        requirements: props.element.validate?.requirements ?? '',
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
                    props.element.validationCode?.code != null && (
                        <CodeEditor
                            value={props.element.validationCode.code}
                            onChange={(code) => {
                                props.onChange({
                                    validationCode: {
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
                    props.element.validate?.conditionSet != null && (
                        <CodeTabNoCodeEditor
                            parents={props.parents}
                            element={props.element}
                            func={props.element.validate}
                            onChange={(updatedFunc) => {
                                props.onChange({
                                    validate: updatedFunc,
                                });
                            }}
                            shouldReturnString={true}
                            editable={props.editable}
                        />
                    )
                }
                {
                    props.element.validationExpressions != null && (
                        <>
                            <Box>
                                {
                                    props.element.validationExpressions
                                        .map(({message, expression}, index) => (
                                            <Box
                                                key={index}
                                                sx={{
                                                    mb: 4,
                                                }}
                                            >
                                                <Box
                                                    sx={{
                                                        display: 'flex',
                                                        alignItems: 'center',
                                                    }}
                                                >
                                                    <Typography
                                                        variant="subtitle1"
                                                        sx={{
                                                            mr: 'auto',
                                                        }}
                                                    >
                                                        Validierung {index + 1}
                                                    </Typography>

                                                    <Actions
                                                        actions={[
                                                            {
                                                                icon: <DeleteOutlinedIcon />,
                                                                tooltip: 'Delete Expression',
                                                                onClick: () => {
                                                                    const updatedValidationExpressions = [
                                                                        ...props.element.validationExpressions ?? [],
                                                                    ];
                                                                    updatedValidationExpressions.splice(index, 1);
                                                                    props.onChange({
                                                                        validationExpressions: updatedValidationExpressions,
                                                                    });
                                                                },
                                                            },
                                                        ]}
                                                    />
                                                </Box>

                                                <Box
                                                    sx={{
                                                        ml: 2,
                                                    }}
                                                >
                                                    <TextFieldComponent
                                                        label="Fehlermeldung"
                                                        value={message}
                                                        onChange={errorMessage => {
                                                            const updatedValidationExpressions = [
                                                                ...props.element.validationExpressions ?? [],
                                                            ];
                                                            updatedValidationExpressions[index] = {
                                                                message: errorMessage ?? '',
                                                                expression: updatedValidationExpressions[index].expression,
                                                            };
                                                            props.onChange({
                                                                validationExpressions: updatedValidationExpressions,
                                                            });
                                                        }}
                                                        hint="Diese Fehlermeldung wird angezeigt, sollte die Bedingung nicht zutreffen"
                                                        disabled={!props.editable}
                                                    />

                                                    <ExpressionEditorWrapper
                                                        label="Bedingung"
                                                        hint="Hier kann eine Bedingung definiert werden, die bestimmt, ob das Element valide ist."
                                                        parents={props.parents}
                                                        expression={expression}
                                                        onChange={(expression) => {
                                                            const updatedValidationExpressions = [
                                                                ...props.element.validationExpressions ?? [],
                                                            ];
                                                            updatedValidationExpressions[index] = {
                                                                message: updatedValidationExpressions[index].message,
                                                                expression: expression,
                                                            };
                                                            props.onChange({
                                                                validationExpressions: updatedValidationExpressions,
                                                            });
                                                        }}
                                                        editable={props.editable}
                                                        desiredReturnType={NoCodeDataType.Boolean}
                                                    />
                                                </Box>
                                            </Box>
                                        ))
                                }
                            </Box>

                            <Button
                                variant="outlined"
                                fullWidth
                                onClick={() => {
                                    props.onChange({
                                        validationExpressions: [
                                            ...props.element.validationExpressions ?? [],
                                            {
                                                expression: {
                                                    operatorIdentifier: '',
                                                    operands: [],
                                                },
                                                message: '',
                                            },
                                        ],
                                    });
                                }}
                                startIcon={<AddOutlinedIcon />}
                            >
                                Validierung hinzufügen
                            </Button>
                        </>
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
