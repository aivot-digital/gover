import React, {useMemo, useReducer, useRef} from 'react';
import {BaseCodeTab} from '../../element-editor-code-tab/base-code-tab';
import {CodeEditor} from '../../code-editor/code-editor';
import LocationSearchingIcon from '@mui/icons-material/LocationSearching';
import {SelectElementDialog} from '../../../dialogs/select-element-dialog/select-element-dialog';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ReferenceCheck} from '../../element-editor-code-tab/components/reference-check/reference-check';
import {createLowCodeContextType} from '../../../utils/create-low-code-context-type';
import {ElementOverrideFunction} from '../../../models/elements/element-override-function';
import {editor} from 'monaco-editor';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import {AnyElement} from '../../../models/elements/any-element';
import {useElementTreeContext} from '../element-tree-context';
import {useElementTreeEditorContext} from './element-tree-editor-context';

const exampleOverrideCode = `(function(){
    // Hier kann der Code eingefügt werden, um die Struktur des Elements zu überschreiben.
    // Die Funktion muss eine gültige Elementstruktur zurückgeben.
    return {
        ...element,
    };
})();`;

export function ElementTreeEditorContentTabOverride<T extends AnyElement>() {
    const dispatch = useAppDispatch();

    const {
        editable,
        allElements,
    } = useElementTreeContext();

    const {
        currentElement,
        onChangeCurrentElement,
        parents,
    } = useElementTreeEditorContext<T>();

    const {
        override: _override,
    } = currentElement;

    const override: ElementOverrideFunction = useMemo(() => _override ?? {
        type: undefined,
        requirements: undefined,
        javascriptCode: undefined,
        fieldNoCodeMap: undefined,
        referencedIds: undefined,
    }, [_override]);

    const hasOverrideFunction = useMemo(() => {
        return override.type != null;
    }, [override]);

    const editorRef = useRef<editor.IStandaloneCodeEditor>(undefined);
    const [showElementSelectDialog, toggleShowElementSelectDialog] = useReducer((state) => !state, false);

    const handleChange = (patch: Partial<ElementOverrideFunction>) => {
        onChangeCurrentElement({
            ...currentElement,
            override: {
                ...override,
                ...patch,
            },
        });
    };

    return (
        <>
            <BaseCodeTab
                label="Dynamische Struktur"
                description="Hier können Sie die Struktur des Elements dynamisch anpassen bzw. überschreiben. Dies ist besonders nützlich, wenn die Struktur des Elements von den Nutzereingaben abhängt oder wenn Sie eine komplexe Logik implementieren möchten."
                editable={editable}
                allowsNoCode={false}
                allowsExpression={false}
                requirements={override.requirements ?? undefined}
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
                                javascriptCode: {
                                    code: exampleOverrideCode,
                                },
                                fieldNoCodeMap: undefined,
                            });
                            break;
                        case 'expression':
                            handleChange({
                                type: 'NoCode',
                                javascriptCode: undefined,
                                fieldNoCodeMap: {},
                            });
                            break;
                    }
                }}
                onDeleteFunction={() => {
                    handleChange({
                        javascriptCode: undefined,
                        fieldNoCodeMap: undefined,
                        referencedIds: undefined,
                        type: undefined,
                    });
                }}
                hasFunction={hasOverrideFunction}
            >
                {
                    override.type === 'Javascript' && (
                        <CodeEditor
                            value={override.javascriptCode?.code ?? undefined}
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
                                    icon: <LocationSearchingIcon/>,
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
                    override.type === 'NoCode' && (
                        <div>
                            No-Code Überschreibungsfunktionen sind derzeit nicht unterstützt.
                        </div>
                    )
                }

                <ReferenceCheck
                    allElements={allElements}
                    element={currentElement}
                    lowCodeOld={[]}
                    lowCode={override.javascriptCode?.code != null ? [override.javascriptCode.code] : []}
                    noCodeOld={[]}
                    noCode={[]}
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
