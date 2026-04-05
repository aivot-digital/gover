import React, {useMemo, useReducer, useRef} from 'react';
import {ConditionSetOperator} from '../../data/condition-set-operator';
import {BaseCodeTab} from './base-code-tab';
import {ConditionOperator} from '../../data/condition-operator';
import {CodeEditor} from '../code-editor/code-editor';
import {CodeTabNoCodeEditor} from '../code-tab-no-code-editor';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {ValidationCodeTabProps} from './validation-code-tab-props';
import {Box, Button, Typography} from '@mui/material';
import {TextFieldComponent} from '../text-field/text-field-component';
import {Actions} from '../actions/actions';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {SelectElementDialog} from '../../dialogs/select-element-dialog/select-element-dialog';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import LocationSearchingIcon from '@mui/icons-material/LocationSearching';
import {createLowCodeContextType} from '../../utils/create-low-code-context-type';
import {ReferenceCheck} from './components/reference-check/reference-check';
import {editor} from 'monaco-editor';
import {ElementValidationFunction} from '../../models/elements/element-validation-function';
import {NoCodeEditorWrapper} from './components/no-code-editor-wrapper/no-code-editor-wrapper';
import Delete from '@aivot/mui-material-symbols-400-outlined/dist/delete/Delete';
import {copyToClipboardText} from '../../utils/copy-to-clipboard';

const exampleValidationCode = `(function(){
    // Hier kann der Code eingefügt werden, der bestimmt, ob das Element valide ist.
    // Gibt die Funktion einen String zurück, wird dieser als Fehler angezeigt.
    // Gibt die Funktion null zurück, ist das Element valide.
    return null;
})();`;

export function ValidationCodeTab(props: ValidationCodeTabProps) {
    const dispatch = useAppDispatch();

    const {
        allElements,
        element,
        onChange,
    } = props;

    const {
        validation: _validation,
    } = element;

    const validation: ElementValidationFunction = useMemo(() => _validation ?? {
        type: undefined,
        requirements: undefined,
        conditionSet: undefined,
        noCodeList: undefined,
        javascriptCode: undefined,
        referencedIds: undefined,
    }, [_validation]);

    const hasValidationFunction = useMemo(() => {
        return validation.type != null;
    }, [validation]);

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
                        noCodeList: undefined,
                        javascriptCode: undefined,
                        referencedIds: undefined,
                        type: undefined,
                    });
                }}
                editable={props.editable}
                allowsNoCode={true}
                allowsExpression={true}
                onSelectFunction={(type) => {
                    switch (type) {
                        case 'legacy-condition':
                            handleChange({
                                type: 'ConditionSet',
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
                                noCodeList: undefined,
                                javascriptCode: undefined,
                            });
                            break;
                        case 'code':
                            handleChange({
                                type: 'Javascript',
                                conditionSet: undefined,
                                noCodeList: undefined,
                                javascriptCode: {
                                    code: exampleValidationCode,
                                },
                            });
                            break;
                        case 'expression':
                            handleChange({
                                type: 'NoCode',
                                conditionSet: undefined,
                                noCodeList: [
                                    {
                                        noCode: undefined,
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
                    validation.type === 'Javascript' && (
                        <CodeEditor
                            value={validation.javascriptCode?.code ?? undefined}
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
                    validation.type === 'ConditionSet' && (
                        <CodeTabNoCodeEditor
                            parents={props.parents}
                            element={props.element}
                            func={{
                                requirements: '',
                                conditionSet: validation.conditionSet ?? undefined,
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
                    validation.type === 'NoCode' && (
                        <>
                            <Box>
                                {
                                    (validation.noCodeList ?? [])
                                        .map(({message, noCode}, index) => (
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
                                                                icon: <Delete />,
                                                                tooltip: 'Delete Expression',
                                                                onClick: () => {
                                                                    const updatedValidationExpressions = [
                                                                        ...(validation.noCodeList ?? []),
                                                                    ];
                                                                    updatedValidationExpressions.splice(index, 1);
                                                                    handleChange({
                                                                        noCodeList: updatedValidationExpressions,
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
                                                                ...(validation.noCodeList ?? []),
                                                            ];
                                                            updatedValidationExpressions[index] = {
                                                                message: errorMessage ?? '',
                                                                noCode: updatedValidationExpressions[index].noCode,
                                                            };
                                                            handleChange({
                                                                noCodeList: updatedValidationExpressions,
                                                            });
                                                        }}
                                                        hint="Diese Fehlermeldung wird angezeigt, sollte die Bedingung nicht zutreffen"
                                                        disabled={!props.editable}
                                                    />

                                                    <NoCodeEditorWrapper
                                                        label="Bedingung"
                                                        hint="Hier kann eine Bedingung definiert werden, die bestimmt, ob das Element valide ist."
                                                        parents={props.parents}
                                                        noCode={noCode}
                                                        onChange={(noCode) => {
                                                            const updatedValidationExpressions = [
                                                                ...(validation.noCodeList ?? []),
                                                            ];
                                                            updatedValidationExpressions[index] = {
                                                                message: updatedValidationExpressions[index].message,
                                                                noCode: noCode,
                                                            };
                                                            handleChange({
                                                                noCodeList: updatedValidationExpressions,
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
                                        noCodeList: [
                                            ...(validation.noCodeList ?? []),
                                            {
                                                noCode: null,
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
                    allElements={allElements}
                    element={props.element}
                    lowCodeOld={[]}
                    lowCode={validation.javascriptCode?.code != null ? [validation.javascriptCode.code] : []}
                    noCodeOld={validation.conditionSet != null ? [validation.conditionSet] : []}
                    noCode={validation.noCodeList?.map(value => value.noCode) ?? []}
                />
            </BaseCodeTab>
            <SelectElementDialog
                allElements={allElements}
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
                        copyToClipboardText(element.id).then((success) => {
                            if (success) {
                                dispatch(showSuccessSnackbar('Element-ID kopiert'));
                            } else {
                                dispatch(showErrorSnackbar('Element-ID konnte nicht kopiert werden'));
                            }
                        });
                    }

                    toggleShowElementSelectDialog();
                }}
                onClose={toggleShowElementSelectDialog}
            />
        </>
    );
}
