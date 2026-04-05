import React, {useMemo, useReducer, useRef} from 'react';
import {BaseCodeTab} from '../../element-editor-code-tab/base-code-tab';
import {CodeEditor} from '../../code-editor/code-editor';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {SelectElementDialog} from '../../../dialogs/select-element-dialog/select-element-dialog';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import LocationSearchingIcon from '@mui/icons-material/LocationSearching';
import {createLowCodeContextType} from '../../../utils/create-low-code-context-type';
import {ReferenceCheck} from '../../element-editor-code-tab/components/reference-check/reference-check';
import {editor} from 'monaco-editor';
import {ElementValueFunction} from '../../../models/elements/element-value-function';
import {NoCodeDataTypeMap} from '../../../modules/nocode/data/no-code-data-type-map';
import {NoCodeEditorWrapper} from '../../element-editor-code-tab/components/no-code-editor-wrapper/no-code-editor-wrapper';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import {BaseInputElement} from '../../../models/elements/form/base-input-element';
import {useElementTreeContext} from '../element-tree-context';
import {useElementTreeEditorContext} from './element-tree-editor-context';

const exampleValueCode = `(function(){
    // Diese Funktion wird aufgerufen, um einen Wert für das Element zu berechnen.
    // Der Wert wird dann in der Anzeige des Elements verwendet und muss dem Typ des Elements entsprechen.
    return null;
})();`;

export function ElementTreeEditorContentTabValue() {
    const dispatch = useAppDispatch();

    const {
        editable,
        allElements,
    } = useElementTreeContext();

    const {
        currentElement,
        onChangeCurrentElement,
        parents,
    } = useElementTreeEditorContext<BaseInputElement<any>>();

    const {
        value: _value,
    } = currentElement;

    const value: ElementValueFunction = useMemo(() => _value ?? {
        type: undefined,
        requirements: undefined,
        javascriptCode: undefined,
        noCode: undefined,
        referencedIds: undefined,
    }, [_value]);

    const hasValueFunction = useMemo(() => {
        return value.type != null;
    }, [value]);

    const editorRef = useRef<editor.IStandaloneCodeEditor>(undefined);
    const [showElementSelectDialog, toggleShowElementSelectDialog] = useReducer((state) => !state, false);

    const handleChange = (patch: Partial<ElementValueFunction>) => {
        onChangeCurrentElement({
            ...currentElement,
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
                editable={editable}
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
                                type: 'Javascript',
                                noCode: undefined,
                                javascriptCode: {
                                    code: exampleValueCode,
                                },
                            });
                            break;
                        case 'expression':
                            handleChange({
                                type: 'NoCode',
                                noCode: undefined,
                                javascriptCode: undefined,
                            });
                            break;
                    }
                }}
                onDeleteFunction={() => {
                    handleChange({
                        javascriptCode: undefined,
                        noCode: undefined,
                        referencedIds: undefined,
                        type: undefined,
                    });
                }}
                hasFunction={hasValueFunction}
            >
                {
                    value.type === 'Javascript' && (
                        <CodeEditor
                            value={value.javascriptCode?.code ?? undefined}
                            onChange={(code) => {
                                handleChange({
                                    javascriptCode: {
                                        code: code,
                                    },
                                });
                            }}
                            actions={editable ? [
                                {
                                    tooltip: 'Element-ID nachschlagen',
                                    icon: <LocationSearchingIcon />,
                                    onClick: toggleShowElementSelectDialog,
                                },
                            ] : []}
                            disabled={!editable}
                            typeHints={[{
                                name: 'Context',
                                content: createLowCodeContextType(parents[0]),
                            }]}
                            onEditorMount={(mountedEditor) => {
                                editorRef.current = mountedEditor;
                            }}
                        />
                    )
                }
                {
                    value.type === 'NoCode' && (
                        <NoCodeEditorWrapper
                            parents={parents}
                            noCode={value.noCode}
                            onChange={(noCode) => {
                                handleChange({
                                    noCode: noCode,
                                });
                            }}
                            editable={editable}
                            desiredReturnType={NoCodeDataTypeMap[currentElement.type as keyof typeof NoCodeDataTypeMap]}
                            label="Dynamischer Wert"
                            hint="Der Ausdruck muss den Wert für das Element zurückliefern."
                        />
                    )
                }

                <ReferenceCheck
                    allElements={allElements}
                    element={currentElement}
                    lowCodeOld={[]}
                    lowCode={value.javascriptCode?.code != null ? [value.javascriptCode.code] : []}
                    noCodeOld={[]}
                    noCode={value.noCode != null ? [value.noCode] : []}
                />
            </BaseCodeTab>

            <SelectElementDialog
                allElements={allElements}
                open={showElementSelectDialog}
                onSelect={(element) => {
                    const currentEditor = editorRef.current;
                    const selection = currentEditor?.getSelection();

                    if (currentEditor != null && selection != null) {
                        const editOperation: editor.IIdentifiedSingleEditOperation = {
                            range: selection,
                            text: element.id,
                            forceMoveMarkers: true,
                        };

                        currentEditor.executeEdits('my-source', [editOperation]);
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
