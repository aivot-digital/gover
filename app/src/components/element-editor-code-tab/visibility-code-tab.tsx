import React, {useMemo, useReducer, useRef} from 'react';
import {ConditionSetOperator} from '../../data/condition-set-operator';
import {VisibilityCodeTabProps} from './visibility-code-tab-props';
import {BaseCodeTab} from './base-code-tab';
import {ConditionOperator} from '../../data/condition-operator';
import {CodeEditor} from '../code-editor/code-editor';
import {CodeTabNoCodeEditor} from '../code-tab-no-code-editor';
import {NoCodeDataType} from '../../data/no-code-data-type';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import LocationSearchingIcon from '@mui/icons-material/LocationSearching';
import {SelectElementDialog} from '../../dialogs/select-element-dialog/select-element-dialog';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import {createLowCodeContextType} from '../../utils/create-low-code-context-type';
import {ReferenceCheck} from './components/reference-check/reference-check';
import {ElementVisibilityFunction} from '../../models/elements/element-visibility-function';
import {editor} from 'monaco-editor';
import {NoCodeEditorWrapper} from './components/no-code-editor-wrapper/no-code-editor-wrapper';

const exampleVisibilityCode = `(function(){
    // Hier kann der Code eingefügt werden, der bestimmt, ob das Element sichtbar ist.
    // Gibt die Funktion true zurück, wird das Element angezeigt, andernfalls nicht.
    return true;
})();`;

export function VisibilityCodeTab(props: VisibilityCodeTabProps) {
    const dispatch = useAppDispatch();

    const {
        allElements,
        element,
        onChange,
    } = props;

    const {
        visibility: _visibility,
    } = element;

    const visibility: ElementVisibilityFunction = useMemo(() => _visibility ?? {
        type: undefined,
        requirements: undefined,
        conditionSet: undefined,
        javascriptCode: undefined,
        noCode: undefined,
        referencedIds: undefined,
    }, [_visibility]);

    const hasVisibilityFunction = useMemo(() => {
        return visibility.type != null;
    }, [visibility]);

    const editorRef = useRef<editor.IStandaloneCodeEditor>(undefined);
    const [showElementSelectDialog, toggleShowElementSelectDialog] = useReducer((state) => !state, false);

    const handleChange = (patch: Partial<ElementVisibilityFunction>) => {
        onChange({
            visibility: {
                ...visibility,
                ...patch,
            },
        });
    };

    return (
        <>
            <BaseCodeTab
                label="Sichtbarkeit"
                description="Hier können Sie die Sichtbarkeit des Elements dynamisch bestimmen. Dies ist besonders nützlich, wenn die Sichtbarkeit des Elements von den Nutzereingaben abhängt oder wenn Sie eine komplexe Logik implementieren möchten."
                editable={props.editable}
                allowsNoCode={true}
                allowsExpression={true}
                requirements={visibility.requirements ?? undefined}
                onRequirementsChange={(req) => {
                    handleChange({
                        requirements: req,
                    });
                }}
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
                                noCode: undefined,
                                javascriptCode: undefined,
                            });
                            break;
                        case 'code':
                            handleChange({
                                type: 'Javascript',
                                conditionSet: undefined,
                                noCode: undefined,
                                javascriptCode: {
                                    code: exampleVisibilityCode,
                                },
                            });
                            break;
                        case 'expression':
                            handleChange({
                                type: 'NoCode',
                                conditionSet: undefined,
                                noCode: undefined,
                                javascriptCode: undefined,
                            });
                            break;
                    }
                }}
                onDeleteFunction={() => {
                    handleChange({
                        javascriptCode: undefined,
                        conditionSet: undefined,
                        noCode: undefined,
                        referencedIds: undefined,
                        type: undefined,
                    });
                }}
                hasFunction={hasVisibilityFunction}
            >
                {
                    visibility.type === 'Javascript' && (
                        <CodeEditor
                            value={visibility.javascriptCode?.code ?? undefined}
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
                    visibility.type === 'ConditionSet' && (
                        <CodeTabNoCodeEditor
                            parents={props.parents}
                            element={props.element}
                            func={{
                                requirements: '',
                                conditionSet: visibility.conditionSet ?? undefined,
                            }}
                            onChange={(updatedFunc) => {
                                handleChange({
                                    conditionSet: updatedFunc.conditionSet,
                                });
                            }}
                            shouldReturnString={false}
                            editable={props.editable}
                        />
                    )
                }
                {
                    visibility.type === 'NoCode' && (
                        <NoCodeEditorWrapper
                            parents={props.parents}
                            noCode={visibility.noCode}
                            onChange={(noCode) => {
                                handleChange({
                                    noCode: noCode,
                                });
                            }}
                            editable={props.editable}
                            desiredReturnType={NoCodeDataType.Runtime}
                        />
                    )
                }

                <ReferenceCheck
                    allElements={allElements}
                    element={props.element}
                    lowCodeOld={[]}
                    lowCode={visibility.javascriptCode?.code != null ? [visibility.javascriptCode.code] : []}
                    noCodeOld={visibility.conditionSet != null ? [visibility.conditionSet] : []}
                    noCode={visibility.noCode != null ? [visibility.noCode] : []}
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
