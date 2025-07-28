import React, {useMemo, useReducer, useRef} from 'react';
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
import {createLowCodeContextType} from '../../utils/create-low-code-context-type';
import {ReferenceCheck} from './components/reference-check/reference-check';
import {ElementVisibilityFunction} from '../../models/elements/element-visibility-function';
import {editor} from 'monaco-editor';
import {ElementValueFunction} from '../../models/elements/element-value-function';

const exampleValueCode = `(function(){
    // Diese Funktion wird aufgerufen, um einen Wert für das Element zu berechnen.
    // Der Wert wird dann in der Anzeige des Elements verwendet und muss dem Typ des Elements entsprechen.
    return null;
})();`;

export function ValueCodeTab(props: ValueCodeTabProps) {
    const dispatch = useAppDispatch();

    const {
        element,
        onChange,
    } = props;

    const {
        value: _value,
    } = element;

    const value: ElementValueFunction = useMemo(() => _value ?? {
        requirements: undefined,
        javascriptCode: undefined,
        expression: undefined,
        referencedIds: undefined,
    }, [_value]);

    const hasValueFunction = useMemo(() => {
        return (
            isStringNotNullOrEmpty(value.javascriptCode?.code) ||
            value.expression != null
        );
    }, [value]);

    const editorRef = useRef<editor.IStandaloneCodeEditor>(undefined);
    const [showElementSelectDialog, toggleShowElementSelectDialog] = useReducer((state) => !state, false);

    const handleChange = (patch: Partial<ElementValueFunction>) => {
        onChange({
            value: {
                ...value,
                ...patch,
            },
        });
    };

    return (
        <>
            <BaseCodeTab
                label="Dynamischer Wert"
                description="Hier können Sie einen dynamischen Wert für das Element definieren. Dieser Wert wird in der Anzeige des Elements verwendet und kann von den Nutzereingaben abhängen."
                editable={props.editable}
                allowsNoCode={false}
                allowsExpression={true}
                requirements={value.requirements ?? undefined}
                onRequirementsChange={(req) => {
                    handleChange({
                        requirements: req,
                    });
                }}
                onSelectFunction={(type) => {
                    switch (type) {
                        case 'code':
                            handleChange({
                                expression: undefined,
                                javascriptCode: {
                                    code: exampleValueCode,
                                },
                            });
                            break;
                        case 'expression':
                            handleChange({
                                expression: {
                                    type: "NoCodeExpression",
                                    operatorIdentifier: '',
                                    operands: [],
                                },
                                javascriptCode: undefined,
                            });
                            break;
                    }
                }}
                onDeleteFunction={() => {
                    handleChange({
                        javascriptCode: undefined,
                        expression: undefined,
                        referencedIds: undefined,
                    });
                }}
                hasFunction={hasValueFunction}
            >
                {
                    value.javascriptCode != null && (
                        <CodeEditor
                            value={value.javascriptCode.code ?? undefined}
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
                    value.expression != null && (
                        <ExpressionEditorWrapper
                            parents={props.parents}
                            expression={value.expression}
                            onChange={(expression) => {
                                handleChange({
                                    expression: expression,
                                });
                            }}
                            editable={props.editable}
                            desiredReturnType={NoCodeDataType.Any}
                        />
                    )
                }

                <ReferenceCheck
                    element={props.element}
                    lowCodeOld={[]}
                    lowCode={value.javascriptCode?.code != null ? [value.javascriptCode.code] : []}
                    noCodeOld={[]}
                    noCode={value.expression != null ? [value.expression] : []}
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
