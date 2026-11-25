import {useLegacyNoCodeMigration} from '../../../modules/nocode/hooks/use-legacy-no-code-migration';
import {useApi} from '../../../hooks/use-api';
import {NoCodeApiService} from '../../../services/no-code-api-service';
import {elementNoCodeFunctions, validateNoCode} from '../../../modules/nocode/utils/validate-no-code';
import {noCodeEditorElements} from '../../../modules/nocode/utils/no-code-editor-elements';
import {Alert, Box, Breadcrumbs, Drawer, Tab, Tabs, Typography} from '@mui/material';
import React, {useLayoutEffect, useMemo, useRef, useState} from 'react';
import {createTheme, ThemeProvider, useTheme} from '@mui/material/styles';
import {AnyElement} from '../../../models/elements/any-element';
import {useElementTreeContext} from '../element-tree-context';
import {useConfirm} from '../../../providers/confirm-provider';
import {
    createElementEditorNavigationLink,
    useElementEditorNavigation,
} from '../../../hooks/use-element-editor-navigation';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {getElementNameForType} from '../../../data/element-type/element-names';
import {ElementTreeEditorContextProvider} from './element-tree-editor-context';
import {DefaultTabs} from '../../element-editor/default-tabs';
import {ElementIsInput} from '../../../data/element-type/element-is-input';
import {ElementTreeEditorContentDispatcher} from './element-tree-editor-content-dispatcher';
import {Actions} from '../../actions/actions';
import ContentCopy from '@aivot/mui-material-symbols-400-n25-outlined/ContentCopy';
import {ElementType} from '../../../data/element-type/element-type';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {ElementsApiService} from '../../../modules/elements/elements-api-service';
import Delete from '@aivot/mui-material-symbols-400-n25-outlined/Delete';
import {isAnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {validateDataKeyPath} from '../../../utils/data-key-input';
import {suggestDestinationKey} from './element-tree-editor-content-tab-properties-utils';
import {getPanelDraftParents} from '../element-panel-draft';
import {editorValuesEqual} from '../../../utils/variable-reference-merge';
import {mergeRecalculatedReferencedIds} from '../../../utils/ui-definition-utils';

interface ElementTreeEditorProps<T extends AnyElement> {
    open: boolean;
    parents: Array<AnyElement>;
    value: T;
    onChange: (value: T) => void;
    onDelete: () => void;
    onCancel: () => void;
    onClone: () => void;
}

export function ElementTreeEditor<T extends AnyElement>(props: ElementTreeEditorProps<T>): React.ReactNode | null {
    const showConfirm = useConfirm();
    const api = useApi();
    const [noCodeValidationIndex, setNoCodeValidationIndex] = useState(0);
    const [noCodeValidationRequest, setNoCodeValidationRequest] = useState(0);
    const [noCodeSaveError, setNoCodeSaveError] = useState<string | null>(null);
    const theme = useTheme();

    const {
        currentEditorTab,
        navigateToEditorTab,
        navigateToElementEditor,
    } = useElementEditorNavigation();

    const {
        editable,
        parentModalZIndex,
        displayContext,
        identityMappingInformation,
        newElementIds,
        markElementApplied,
        allElements,
        panelDraft,
        onPanelDraftChange,
        applyPanelDraft,
    } = useElementTreeContext();

    const {
        open,
        parents,
        value,
        onChange,
        onDelete,
        onCancel,
        onClone,
    } = props;

    const panel = open && panelDraft?.base.id === value.id ? panelDraft : null;
    const [localElement, setLocalElement] = useState<T>();
    const updatedElement = panel == null ? localElement : panel.mine as T;
    const migration = useLegacyNoCodeMigration<T>(updatedElement ?? value, (element) => storeCurrentElement(element),
        onPanelDraftChange == null ? undefined : {
            migrations: panel?.legacyMigrations ?? {},
            original: (panel?.base ?? value) as T,
            onChange: (element, migrations) => storeCurrentElement(element, undefined, migrations),
        });
    const [isBusy, setIsBusy] = useState<boolean>(false);
    const [localKeyUnlocked, setDataKeyUnlocked] = useState(false);
    const [localAutomaticKey, setAutomaticDataKey] = useState(false);
    const dataKeyUnlocked = panel?.dataKeyUnlocked ?? localKeyUnlocked;
    const automaticDataKey = panel?.automaticDataKey ?? localAutomaticKey;
    const [dataKeyValidationRequested, setDataKeyValidationRequested] = useState(false);
    const editGeneration = useRef(0);
    const currentTree = useRef({panel, applyPanelDraft});
    useLayoutEffect(() => { currentTree.current = {panel, applyPanelDraft}; }, [panel, applyPanelDraft]);

    const hasSummaryAsParent = useMemo(() => {
        return parents.some(p => p.type === ElementType.SummaryLayout);
    }, [parents]);

    // Only a newly added field follows its title. A loaded empty key deliberately keeps values in element data.
    // Snapshot this policy per editor session; ordinary parent updates must not reset an in-progress edit.
    useLayoutEffect(() => {
        if (open) {
            const automatic = panel?.automaticDataKey ?? (displayContext !== ElementDisplayContext.DataObjectSchema &&
                newElementIds?.has(value.id) === true && isAnyInputElement(value) && !value.destinationKey);
            setAutomaticDataKey(automatic);
            setDataKeyUnlocked(panel?.dataKeyUnlocked ?? false);
            setDataKeyValidationRequested(false);
            const generatedKey = automatic && isAnyInputElement(value)
                ? suggestDestinationKey(value.label, {element: value, parents}, allElements)
                : null;
            setLocalElement(generatedKey == null ? undefined : {...value, destinationKey: generatedKey});
            setIsBusy(false);
            setNoCodeSaveError(null);
            setNoCodeValidationRequest(0);
            setNoCodeValidationIndex(0);
            migration.reset();
        }
        return () => { editGeneration.current++; };
    }, [open, value.id]);

    const storeCurrentElement = (updated: T, reason?: 'normalize' | 'structure', migrations = migration.migrations) => {
        if (onPanelDraftChange != null && (!open || !editable)) return;
        setNoCodeSaveError(null);
        const previous = migration.current;
        let trackTitle = automaticDataKey;
        if (reason !== 'structure' && automaticDataKey && isAnyInputElement(previous) && isAnyInputElement(updated)) {
            if (updated.destinationKey !== previous.destinationKey) {
                trackTitle = false;
            } else if (updated.label !== previous.label) {
                updated = {...updated, destinationKey: suggestDestinationKey(updated.label, {element: updated, parents}, allElements)};
            }
        }
        // A later keystroke supersedes an in-flight reference calculation for the previous value.
        editGeneration.current++;
        setIsBusy(false);
        setAutomaticDataKey(trackTitle);
        if (onPanelDraftChange == null || (reason === 'normalize' && panel == null)) {
            setLocalElement(updated);
            return;
        }
        const base = panel?.base ?? value;
        const hasPendingStructure = panel?.detached || !!panel?.pendingMovedRemovals?.length;
        if (!hasPendingStructure && editorValuesEqual(updated, base)) {
            setLocalElement(undefined);
            onPanelDraftChange(null);
            return;
        }
        if (!hasPendingStructure && localElement != null && editorValuesEqual(updated, localElement)) {
            onPanelDraftChange(null);
            return;
        }
        onPanelDraftChange({
            ...panel,
            base,
            mine: updated,
            parents: panel?.parents ?? getPanelDraftParents(parents, value.id),
            tab: currentEditorTab ?? DefaultTabs.properties,
            automaticDataKey: trackTitle,
            dataKeyUnlocked,
            legacyMigrations: Object.keys(migrations).length > 0 ? migrations : undefined,
        }, reason);
    };

    const changeCurrentElement = (updated: T, reason?: 'normalize' | 'structure') => {
        if (onPanelDraftChange == null) {
            if (!editable) return;
            setNoCodeSaveError(null);
            migration.update(updated);
        } else {
            storeCurrentElement(updated, reason);
        }
    };

    const handleSetCurrentTab = (newTab: string): void => {
        if (panel != null) onPanelDraftChange?.({...panel, tab: newTab});
        navigateToEditorTab(newTab);
    };

    const handleSave = async (): Promise<void> => {
        if (isBusy || !editable) return;
        const current = migration.current;
        setDataKeyValidationRequested(true);
        if (displayContext !== ElementDisplayContext.DataObjectSchema && isAnyInputElement(current) &&
            validateDataKeyPath(current.destinationKey) != null) {
            handleSetCurrentTab(DefaultTabs.properties);
            return;
        }
        if (updatedElement == null && Object.keys(migration.migrations).length === 0) {
            markElementApplied?.(value.id);
            handleClose();
            return;
        }

        setIsBusy(true);
        const generation = editGeneration.current;
        setNoCodeSaveError(null);
        const elementApi = new ElementsApiService();
        let candidate = current;
        for (;;) {
            const candidateMigrations = panel == null ? migration.migrations : currentTree.current.panel?.legacyMigrations ?? {};
            // Incomplete expressions are applied and reported as configuration errors of the element. Only the new version
            // of a legacy rule must be complete, because applying it replaces the legacy rule.
            const functions = elementNoCodeFunctions(candidate)
                .filter(({section}) => section !== 'value' && candidateMigrations[section] != null);
            if (functions.length > 0) {
                try {
                    const operators = await new NoCodeApiService(api).getNoCodeOperators();
                    if (generation !== editGeneration.current) return;
                    const elements = noCodeEditorElements(parents);
                    const invalid = functions.find(({operand}) => !validateNoCode(operand, operators, elements).isValid);
                    if (invalid != null) {
                        setNoCodeValidationIndex(functions.filter((fn) => fn.section === invalid.section).indexOf(invalid));
                        setNoCodeValidationRequest((current) => current + 1);
                        setNoCodeSaveError('Vervollständigen Sie den No-Code-Ausdruck, bevor Sie das Element übernehmen.');
                        navigateToEditorTab(invalid.section);
                        setIsBusy(false);
                        return;
                    }
                } catch {
                    if (generation !== editGeneration.current) return;
                    setNoCodeSaveError('Die No-Code-Ausdrücke konnten nicht geprüft werden. Versuchen Sie es erneut.');
                    setIsBusy(false);
                    return;
                }
            }
            // If the calculation fails, keep the references already present on this element.
            const recalculated = await elementApi.recalculateReferencedIds(candidate).catch(() => candidate);
            if (generation !== editGeneration.current) return;
            const latest = currentTree.current;
            if (panel != null && latest.panel != null && !editorValuesEqual(candidate, latest.panel.mine)) {
                // A form save rebased the open edit while its references were being calculated.
                candidate = latest.panel.mine as T;
                continue;
            }
            setIsBusy(false);
            if (panel != null && latest.panel == null) {
                // The confirmed layout already contains this edit.
                onCancel();
            } else if (panel != null && latest.panel != null && latest.applyPanelDraft != null) {
                // The backend may serialize additional default/null properties. Only reference IDs come from it.
                const applied = mergeRecalculatedReferencedIds(candidate, recalculated);
                if (!latest.applyPanelDraft(latest.panel, applied)) return;
                onPanelDraftChange?.(null);
                onCancel();
            } else {
                onChange(recalculated);
                if (panel != null) onPanelDraftChange?.(null);
            }
            setDataKeyUnlocked(false);
            markElementApplied?.(value.id);
            return;
        }
    };

    const confirmDiscard = async (): Promise<boolean> => {
        const generation = editGeneration.current;
        if (updatedElement != null || Object.keys(migration.migrations).length > 0) {
            const confirmed = await showConfirm({
                theme: drawerTheme,
                confirmButtonColor: 'error',
                title: 'Änderungen verwerfen?',
                confirmButtonText: 'Ja, verwerfen',
                children: (
                    <div>
                        Möchten Sie die Änderungen an diesem{' '}
                        {value.type === ElementType.FormLayout ? 'Formular' : 'Element'} wirklich verwerfen? Diese Aktion kann nicht
                        rückgängig gemacht werden.
                    </div>
                ),
            });

            if (!confirmed || generation !== editGeneration.current) return false;
        }
        return true;
    };

    const handleClose = async (): Promise<boolean> => {
        if (!await confirmDiscard()) return false;
        editGeneration.current++;
        migration.reset();
        if (panel != null) onPanelDraftChange?.(null);
        onCancel();
        return true;
    };

    const handleClone = async () => {
        if (panel != null && !await confirmDiscard()) return;
        setIsBusy(true);
        try {
            onClone();
        } finally {
            setIsBusy(false);
        }
    };

    const handleElementLink = (event: React.MouseEvent) => {
        if (panel == null || event.defaultPrevented || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        const link = event.target instanceof Element ? event.target.closest<HTMLAnchorElement>('a[href^="#"]') : null;
        if (link == null || link.target === '_blank') return;
        const [elementId, tab] = link.hash.slice(1).split('/');
        if (!elementId || elementId === value.id) return;
        event.preventDefault();
        void handleClose().then((closed) => {
            if (closed) navigateToElementEditor(elementId, tab ?? null);
        });
    };

    const changeKeyLock = (unlocked: boolean) => {
        setDataKeyUnlocked(unlocked);
        // Manual control ends title tracking, even if the user later locks the key again.
        if (unlocked) setAutomaticDataKey(false);
        if (panel != null) {
            onPanelDraftChange?.({...panel, dataKeyUnlocked: unlocked, automaticDataKey: unlocked ? false : automaticDataKey});
        }
    };

    const isRoot = parents.length === 0;
    const shownElement = panel != null && panel.mine.type !== value.type ? panel.mine : value;
    const componentTitle = useMemo(() => generateComponentTitle(shownElement), [shownElement]);
    const typeName = useMemo(() => getElementNameForType(shownElement.type), [shownElement.type]);
    const showTypeSuffix = useMemo(() => componentTitle.trim() !== typeName.trim(), [componentTitle, typeName]);

    const drawerTheme = useMemo(() => {
        if (parentModalZIndex == null) {
            return theme;
        }

        // Move the whole drawer modal layer above the parent dialog and keep nested dialogs above the drawer.
        const drawerZIndex = Math.max(theme.zIndex.drawer, parentModalZIndex + 10);
        const modalZIndex = Math.max(theme.zIndex.modal, drawerZIndex + 10);

        if (drawerZIndex === theme.zIndex.drawer && modalZIndex === theme.zIndex.modal) {
            return theme;
        }

        return createTheme(theme, {
            zIndex: {
                ...theme.zIndex,
                drawer: drawerZIndex,
                modal: modalZIndex,
            },
        });
    }, [parentModalZIndex, theme]);

    return (
        <ThemeProvider theme={drawerTheme}>
            <Drawer
                anchor="right"
                open={open}
                slotProps={{
                    paper: {
                        sx: {
                            width: {
                                xs: '100%',
                                sm: '100%',
                                md: '90%',
                                lg: '85%',
                                xl: '75%',
                            },
                            maxWidth: '1720px',
                        },
                    },
                }}
                onClose={handleClose}
                onClick={handleElementLink}
            >
                <Box
                    sx={{
                        height: '100vh',
                        display: 'flex',
                        flexDirection: 'column',
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            px: 2,
                            pt: 1,
                            pb: 0.5,
                        }}
                    >
                        <Typography
                            variant="h6"
                            component="div"
                            sx={{
                                overflow: 'hidden',
                                textOverflow: 'ellipsis',
                                whiteSpace: 'nowrap',
                                mr: 2,
                            }}
                        >
                            {componentTitle}
                            {
                                showTypeSuffix &&
                                <>
                                    <Typography
                                        component="span"
                                        sx={{
                                            color: 'text.secondary',
                                            fontSize: 'inherit',
                                            px: 0.5
                                        }}>
                                        –
                                    </Typography>
                                    <Typography
                                        component="span"
                                        sx={{
                                            color: 'text.secondary',
                                            fontSize: 'inherit'
                                        }}>
                                        {typeName}
                                    </Typography>
                                </>
                            }
                        </Typography>

                        <Breadcrumbs
                            sx={{
                                ml: 'auto',
                                color: 'text.secondary',
                                '& a': {
                                    color: 'text.secondary',
                                    textDecoration: 'none',
                                    '&:hover': {
                                        textDecoration: 'underline',
                                    },
                                },
                            }}
                            maxItems={3}
                        >
                            {
                                props
                                    .parents
                                    .map((element) => (
                                        <a
                                            key={element.id}
                                            href={createElementEditorNavigationLink(element.id)}
                                        >
                                            {generateComponentTitle(element)}
                                        </a>
                                    ))
                            }
                        </Breadcrumbs>
                    </Box>

                    <Tabs
                        value={currentEditorTab}
                        onChange={(_, newTab) => {
                            handleSetCurrentTab(newTab);
                        }}
                        variant="scrollable"
                        scrollButtons="auto"
                    >
                        <Tab
                            label="Eigenschaften"
                            value={DefaultTabs.properties}
                        />

                        {
                            ElementIsInput[shownElement.type] &&
                            identityMappingInformation != null &&
                            identityMappingInformation.length > 0 &&
                            <Tab
                                label="Identitätsdaten"
                                value={DefaultTabs.metadata}
                            />
                        }

                        <Box
                            sx={{
                                height: 24,
                                alignSelf: 'center',
                                borderLeft: '1px solid',
                                borderColor: 'divider',
                                mx: 1,
                            }}
                        />

                        {
                            // The root of an element tree cannot have visibility.
                            !isRoot &&
                            displayContext != ElementDisplayContext.DataObjectSchema &&
                            <Tab
                                label="Sichtbarkeit"
                                value={DefaultTabs.visibility}
                            />
                        }

                        {
                            ElementIsInput[shownElement.type] &&
                            !hasSummaryAsParent &&
                            displayContext != ElementDisplayContext.DataObjectSchema &&
                            <Tab
                                label="Validierung"
                                value={DefaultTabs.validation}
                            />
                        }

                        {
                            ElementIsInput[shownElement.type] &&
                            displayContext != ElementDisplayContext.DataObjectSchema &&
                            shownElement.type != ElementType.FileUpload &&
                            <Tab
                                label="Dynamischer Wert"
                                value={DefaultTabs.value}
                            />
                        }

                        {
                            // The root of an element tree cannot have a dynamic structure.
                            !isRoot &&
                            displayContext != ElementDisplayContext.DataObjectSchema &&
                            <Tab
                                label="Dynamische Struktur"
                                value={DefaultTabs.patch}
                            />
                        }

                        {
                            !isRoot &&
                            displayContext != ElementDisplayContext.DataObjectSchema &&
                            <Box
                                sx={{
                                    height: 24,
                                    alignSelf: 'center',
                                    borderLeft: '1px solid',
                                    borderColor: 'divider',
                                    mx: 1,
                                }}
                            />
                        }

                        <Tab
                            label="Elementstruktur"
                            value={DefaultTabs.structure}
                        />
                    </Tabs>

                    <Box
                        sx={{
                            flex: '1',
                            overflowY: 'scroll',
                            borderTop: '1px solid',
                            borderTopColor: 'divider',
                            p: 2,
                        }}
                    >
                        <ElementTreeEditorContextProvider
                            value={{
                                currentElement: migration.current,
                                noCodeValidationRequest,
                                noCodeValidationIndex,
                                legacyMigrations: migration.migrations,
                                startLegacyMigration: (section) => {
                                    if (editable && !isBusy) migration.start(section);
                                },
                                cancelLegacyMigration: async (section) => {
                                    if (!editable || isBusy) return;
                                    const draft = migration.migrations[section];
                                    if (draft == null) return;
                                    if (JSON.stringify(draft.draft) !== JSON.stringify(draft.initial) && !await showConfirm({
                                        theme: drawerTheme,
                                        isDestructive: true,
                                        title: 'Umstellung abbrechen?',
                                        confirmButtonText: 'Neue Fassung verwerfen',
                                        children: 'Die neue Fassung wird verworfen. Die bisherige Regel und andere Änderungen am Element bleiben erhalten.',
                                    })) return;
                                    migration.cancel(section);
                                    setNoCodeSaveError(null);
                                },
                                originalElement: panel?.base ?? value,
                                dataKeyUnlocked,
                                onUnlockDataKey: () => changeKeyLock(true),
                                onLockDataKey: () => changeKeyLock(false),
                                dataKeyValidationRequested,
                                onChangeCurrentElement: changeCurrentElement,
                                parents: parents,
                            }}
                        >
                            <ElementTreeEditorContentDispatcher
                                element={migration.current}
                                currentTab={currentEditorTab}
                                onChange={(changedElement) => {
                                    changeCurrentElement({
                                        ...migration.current,
                                        ...changedElement,
                                    }, 'structure');
                                }}
                                editable={editable}
                            />
                        </ElementTreeEditorContextProvider>
                    </Box>

                    {noCodeSaveError != null && <Alert severity="error"
                                                       sx={{
                                                           mx: 2,
                                                           mb: 1,
                                                       }}>{noCodeSaveError}</Alert>}
                    <Box
                        sx={{
                            pt: 2,
                            pb: 2.5,
                            px: 2,
                            display: 'flex',
                            borderTop: '1px solid',
                            borderTopColor: 'divider',
                            gap: 2,
                        }}
                    >
                        <Actions
                            actions={[
                                {
                                    label: 'Übernehmen',
                                    onClick: handleSave,
                                    disabled: isBusy || (updatedElement == null && Object.keys(migration.migrations).length === 0),
                                    visible: editable,
                                    variant: 'contained',
                                },
                                {
                                    icon: null,
                                    label: editable ? 'Abbrechen' : 'Schließen',
                                    onClick: handleClose,
                                    disabled: isBusy,
                                },
                            ]}
                        />

                        <Actions
                            sx={{
                                ml: 'auto',
                            }}
                            actions={[
                                {
                                    icon: <ContentCopy/>,
                                    iconPosition: 'start',
                                    label: 'Duplizieren',
                                    onClick: handleClone,
                                    visible: editable && !isRoot && !panel?.detached,
                                    disabled: isBusy,
                                },
                                {
                                    icon: <Delete/>,
                                    iconPosition: 'start',
                                    label: 'Löschen',
                                    visible: !isRoot && !panel?.detached,
                                    onClick: onDelete,
                                    disabled: isBusy || !editable,
                                    variant: 'outlined',
                                    color: 'error',
                                },
                            ]}
                        />
                    </Box>
                </Box>
            </Drawer>
        </ThemeProvider>
    );
}
