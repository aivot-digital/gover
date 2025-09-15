import React, {useMemo, useReducer, useRef} from 'react';
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
import {createLowCodeContextType} from '../../utils/create-low-code-context-type';
import {ReferenceCheck} from './components/reference-check/reference-check';
import {editor} from 'monaco-editor';
import {ElementValidationFunction} from '../../models/elements/element-validation-function';

const exampleValidationCode = `(function(){
    // Hier kann der Code eingefügt werden, der bestimmt, ob das Element valide ist.
    // Gibt die Funktion einen String zurück, wird dieser als Fehler angezeigt.
    // Gibt die Funktion null zurück, ist das Element valide.
    return null;
})();`;

export function ValidationCodeTab(props: ValidationCodeTabProps) {
    const dispatch = useAppDispatch();

    const {
        element,
        onChange,
    } = props;

    const {
        validation: _validation,
    } = element;

    const validation: ElementValidationFunction = useMemo(() => _validation ?? {
        requirements: undefined,
        conditionSet: undefined,
        expression: undefined,
        javascriptCode: undefined,
        referencedIds: undefined,
    }, [_validation]);

    const hasValidationFunction = useMemo(() => {
        return (
            validation.javascriptCode?.code != null ||
            validation.conditionSet != null ||
            validation.expression != null
        );
    }, [element]);

    const editorRef = useRef<editor.IStandaloneCodeEditor>(undefined);
    const [showElementSelectDialog, toggleShowElementSelectDialog] = useReducer((state) => !state, false);

    const handleChange = (patch: Partial<ElementValidationFunction>) => {
        onChange({
            validation: {
                ...validation,
                ...patch,
            },
        });
    };

    return (
        <>
            <BaseCodeTab
                label="Validierung"
                description="Hier können Sie die Validierung des Elements konfigurieren. Hierzu definieren Sie die Regeln, die das Element erfüllen muss, um als valide/gültig zu gelten."
                requirements={validation.requirements ?? undefined}
                onRequirementsChange={(req) => {
                    handleChange({
                        requirements: req ?? '',
                    });
                }}
                onDeleteFunction={() => {
                    handleChange({
                        conditionSet: undefined,
                        expression: undefined,
                        javascriptCode: undefined,
                        referencedIds: undefined,
                    });
                }}
                editable={props.editable}
                allowsNoCode={true}
                allowsExpression={true}
                onSelectFunction={(type) => {
                    switch (type) {
                        case 'legacy-condition':
                            handleChange({
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
                                expression: undefined,
                                javascriptCode: undefined,
                            });
                            break;
                        case 'code':
                            handleChange({
                                conditionSet: undefined,
                                expression: undefined,
                                javascriptCode: {
                                    code: exampleValidationCode,
                                },
                            });
                            break;
                        case 'expression':
                            handleChange({
                                conditionSet: undefined,
                                expression: [
                                    {
                                        expression: {
                                            type: "NoCodeExpression",
                                            operatorIdentifier: '',
                                            operands: [],
                                        },
                                        message: '',
                                    },
                                ],
                                javascriptCode: undefined,
                            });
                            break;
                    }
                }}
                hasFunction={hasValidationFunction}
            >
                {
                    validation.javascriptCode != null && (
                        <CodeEditor
                            value={validation.javascriptCode.code ?? undefined}
                            onChange={(code) => {
                                handleChange({
                                    javascriptCode: {
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
                            typeHints={[{
                                name: 'Context',
                                content: createLowCodeContextType(props.parents[0]),
                            }]}
                            onEditorMount={(editor) => {
                                editorRef.current = editor;
                            }}
                        />
                    )
                }
                {
                    validation.conditionSet != null && (
                        <CodeTabNoCodeEditor
                            parents={props.parents}
                            element={props.element}
                            func={{
                                requirements: '',
                                conditionSet: validation.conditionSet,
                            }}
                            onChange={(updatedFunc) => {
                                handleChange({
                                    conditionSet: updatedFunc.conditionSet,
                                });
                            }}
                            shouldReturnString={true}
                            editable={props.editable}
                        />
                    )
                }
                {
                    validation.expression != null && (
                        <>
                            <Box>
                                {
                                    validation.expression
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
                                                                        ...(validation.expression ?? []),
                                                                    ];
                                                                    updatedValidationExpressions.splice(index, 1);
                                                                    handleChange({
                                                                        expression: updatedValidationExpressions,
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
                                                        value={message ?? undefined}
                                                        onChange={errorMessage => {
                                                            const updatedValidationExpressions = [
                                                                ...(validation.expression ?? []),
                                                            ];
                                                            updatedValidationExpressions[index] = {
                                                                message: errorMessage ?? '',
                                                                expression: updatedValidationExpressions[index].expression,
                                                            };
                                                            handleChange({
                                                                expression: updatedValidationExpressions,
                                                            });
                                                        }}
                                                        hint="Diese Fehlermeldung wird angezeigt, sollte die Bedingung nicht zutreffen"
                                                        disabled={!props.editable}
                                                    />

                                                    <ExpressionEditorWrapper
                                                        label="Bedingung"
                                                        hint="Hier kann eine Bedingung definiert werden, die bestimmt, ob das Element valide ist."
                                                        parents={props.parents}
                                                        expression={expression ?? {
                                                            type: 'NoCodeExpression',
                                                            operatorIdentifier: '',
                                                            operands: [],
                                                        }}
                                                        onChange={(expression) => {
                                                            const updatedValidationExpressions = [
                                                                ...(validation.expression ?? []),
                                                            ];
                                                            updatedValidationExpressions[index] = {
                                                                message: updatedValidationExpressions[index].message,
                                                                expression: expression,
                                                            };
                                                            handleChange({
                                                                expression: updatedValidationExpressions,
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
                                    handleChange({
                                        expression: [
                                            ...(validation.expression ?? []),
                                            {
                                                expression: {
                                                    type: 'NoCodeExpression',
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

                <ReferenceCheck
                    element={props.element}
                    lowCodeOld={[]}
                    lowCode={validation.javascriptCode?.code != null ? [validation.javascriptCode.code] : []}
                    noCodeOld={validation.conditionSet != null ? [validation.conditionSet] : []}
                    noCode={validation.expression?.map(value => value.expression) ?? []}
                />
            </BaseCodeTab>
            <SelectElementDialog
                open={showElementSelectDialog}
                onSelect={(element) => {
                    const _editor = editorRef.current;
                    const _selection = _editor?.getSelection();

                    if (_editor != null && _selection != null) {
                        const editOperation: editor.IIdentifiedSingleEditOperation = {
                            range: _selection,
                            text: element.id,
                            forceMoveMarkers: true,
                        };

                        _editor
                            .executeEdits('my-source', [editOperation]);

                        dispatch(showSuccessSnackbar('Element-ID eingefügt'));
                    } else {
                        navigator.clipboard.writeText(element.id);
                        dispatch(showSuccessSnackbar('Element-ID kopiert'));
                    }

                    toggleShowElementSelectDialog();
                }}
                onClose={toggleShowElementSelectDialog}
            />
        </>
    );
}
