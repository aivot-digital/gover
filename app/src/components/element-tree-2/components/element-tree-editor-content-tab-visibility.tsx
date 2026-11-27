import {LegacyNoCodeMigrationReference} from '../../../modules/nocode/components/legacy-no-code-migration-reference';
import {hasLegacyNoCode} from '../../../modules/nocode/hooks/use-legacy-no-code-migration';
import React, {useMemo, useReducer, useRef, useState} from 'react';
import {BaseCodeTab} from '../../element-editor-code-tab/base-code-tab';
import {CodeEditor} from '../../code-editor/code-editor';
import {NoCodeDataType} from '../../../data/no-code-data-type';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import LocationSearchingIcon from '@aivot/mui-material-symbols-400-n25-outlined/LocationSearching';
import {SelectElementDialog} from '../../../dialogs/select-element-dialog/select-element-dialog';
import {showErrorSnackbar, showSuccessSnackbar} from '../../../slices/snackbar-slice';
import {createLowCodeContextType} from '../../../utils/create-low-code-context-type';
import {ReferenceCheck} from '../../element-editor-code-tab/components/reference-check/reference-check';
import {ElementVisibilityFunction} from '../../../models/elements/element-visibility-function';
import {editor} from 'monaco-editor';
import {
    NoCodeEditorWrapper,
} from '../../element-editor-code-tab/components/no-code-editor-wrapper/no-code-editor-wrapper';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import {AnyElement} from '../../../models/elements/any-element';
import {useElementTreeContext} from '../element-tree-context';
import {useElementTreeEditorContext} from './element-tree-editor-context';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';

const exampleVisibilityCode = `(function(){
    // Hier kann der Code eingefügt werden, der bestimmt, ob das Element sichtbar ist.
    // Gibt die Funktion true zurück, wird das Element angezeigt, andernfalls nicht.
    return true;
})();`;

export function ElementTreeEditorContentTabVisibility<T extends AnyElement>() {
    const dispatch = useAppDispatch();

    const {
        editable,
        allElements,
        displayContext,
    } = useElementTreeContext();

    const {
        noCodeValidationRequest,
        currentElement,
        onChangeCurrentElement,
        parents,
        legacyMigrations,
        startLegacyMigration,
    } = useElementTreeEditorContext<T>();

    const {
        visibility: _visibility,
    } = currentElement;

    const visibility: ElementVisibilityFunction = useMemo(() => _visibility ?? {
        type: undefined,
        requirements: undefined,
        conditionSet: undefined,
        javascriptCode: undefined,
        noCode: undefined,
        referencedIds: undefined,
    }, [_visibility]);

    const hasVisibilityFunction = useMemo(() => {
        return visibility.type != null || hasLegacyNoCode(visibility);
    }, [visibility]);

    const [toolbarContainer, setToolbarContainer] = useState<HTMLDivElement | null>(null);
    const editorRef = useRef<editor.IStandaloneCodeEditor>(undefined);
    const [showElementSelectDialog, toggleShowElementSelectDialog] = useReducer((state) => !state, false);

    const handleChange = (patch: Partial<ElementVisibilityFunction>) => {
        onChangeCurrentElement({
            ...currentElement,
            visibility: {
                ...visibility,
                ...patch,
            },
        });
    };

    return (
        <>
            <BaseCodeTab
                functionToolbarRef={visibility.type === 'NoCode' || visibility.type === 'Javascript' && editable ? setToolbarContainer : undefined}
                label="Sichtbarkeit"
                description="Hier können Sie die Sichtbarkeit des Elements dynamisch bestimmen. Dies ist besonders nützlich, wenn die Sichtbarkeit des Elements von den Nutzereingaben abhängt oder wenn Sie eine komplexe Logik implementieren möchten."
                editable={editable}
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
                            if (hasLegacyNoCode(visibility)) {
                                startLegacyMigration?.('visibility');
                                break;
                            }
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
                functionActionsDisabled={legacyMigrations?.visibility != null}
                hasFunction={hasVisibilityFunction}
            >
                {
                    visibility.type === 'Javascript' && (
                        <CodeEditor
                            toolbarContainer={toolbarContainer}
                            value={visibility.javascriptCode?.code ?? undefined}
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
                <LegacyNoCodeMigrationReference section="visibility"/>
                {
                    visibility.type === 'NoCode' && (
                        <NoCodeEditorWrapper
                            toolbarContainer={toolbarContainer}
                            emptyDescription="Legen Sie fest, unter welchen Bedingungen dieses Element sichtbar ist."
                            validationRequest={noCodeValidationRequest}
                            required
                            parents={parents}
                            noCode={visibility.noCode}
                            onChange={(noCode) => {
                                handleChange({
                                    noCode: noCode,
                                });
                            }}
                            editable={editable}
                            desiredReturnType={NoCodeDataType.Runtime}
                            label="Sichtbarkeit"
                            hint='Der Ausdruck muss einen Wahrheitswert (Boolean) zurückgeben. Wenn der Ausdruck "Wahr" ergibt, wird das Element angezeigt; andernfalls wird es ausgeblendet.'
                            contextType={displayContext === ElementDisplayContext.StaffFacing ? 'BOTH' : 'FORM'}
                        />
                    )
                }

                <ReferenceCheck
                    allElements={allElements}
                    element={currentElement}
                    lowCodeOld={[]}
                    lowCode={visibility.javascriptCode?.code != null ? [visibility.javascriptCode.code] : []}
                    noCodeOld={visibility.conditionSet != null ? [visibility.conditionSet] : []}
                    noCode={visibility.noCode != null ? [visibility.noCode] : []}
                />
            </BaseCodeTab>

            <SelectElementDialog
                primaryActionLabel="Element-ID verwenden"
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
