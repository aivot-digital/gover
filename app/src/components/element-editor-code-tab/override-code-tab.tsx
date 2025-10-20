import React, {useMemo, useReducer, useRef} from 'react';
import {BaseCodeTab} from './base-code-tab';
import {CodeEditor} from '../code-editor/code-editor';
import {OverrideCodeTabProps} from './override-code-tab-props';
import LocationSearchingIcon from '@mui/icons-material/LocationSearching';
import {SelectElementDialog} from '../../dialogs/select-element-dialog/select-element-dialog';
import {showSuccessSnackbar} from '../../slices/snackbar-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {ReferenceCheck} from './components/reference-check/reference-check';
import {createLowCodeContextType} from '../../utils/create-low-code-context-type';
import {ElementOverrideFunction} from '../../models/elements/element-override-function';
import {editor} from 'monaco-editor';

const exampleOverrideCode = `(function(){
    // Hier kann der Code eingefügt werden, um die Struktur des Elements zu überschreiben.
    // Die Funktion muss eine gültige Elementstruktur zurückgeben.
    return {
        ...element,
    };
})();`;

export function OverrideCodeTab(props: OverrideCodeTabProps) {
    const dispatch = useAppDispatch();

    const {
        allElements,
        element,
        onChange,
    } = props;

    const {
        override: _override,
    } = element;

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
        onChange({
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
                editable={props.editable}
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
                    override.type === 'NoCode' && (
                        <div>
                            No-Code Überschreibungsfunktionen sind derzeit nicht unterstützt.
                        </div>
                    )
                }

                <ReferenceCheck
                    allElements={allElements}
                    element={element}
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
