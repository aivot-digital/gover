import {normalizeElementDefinition} from '../../../utils/normalize-element-definition';
import {getCustomerPageSurfaceColor} from '../../../theming/customer-page-surface';
import {Alert, Badge, Box, CircularProgress, Dialog, DialogContent, Paper, ThemeProvider, Typography, useTheme} from '@mui/material';
import React, {useEffect, useLayoutEffect, useMemo, useRef, useState} from 'react';
import {showDialog} from '../../../slices/app-slice';
import {selectUser} from '../../../slices/user-slice';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {
    selectDevToolsTab,
    setDevToolsTab,
    toggleAutoScrollForSteps,
    toggleComponentTree,
    toggleElementContextMenu,
} from '../../../slices/admin-settings-slice';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {useCurrentState} from '../../../hooks/use-current-state';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ElementTree} from '../../../components/element-tree-2/element-tree';
import type {ElementPanelDraft} from '../../../components/element-tree-2/element-panel-draft';
import {HelpDialog, HelpDialogId} from '../../../dialogs/help-dialog/help.dialog';
import {PrivacyDialog, PrivacyDialogId} from '../../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog, AccessibilityDialogId} from '../../../dialogs/accessibility-dialog/accessibility-dialog';
import VisibilityOffOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/VisibilityOff';
import VisibilityOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Visibility';
import RemoveDoneOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/RemoveDone';
import DoneAllOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/DoneAll';
import {
    showApiErrorSnackbar,
    showEditorSaveErrorSnackbar,
    showErrorSnackbar,
    showSuccessSnackbar,
    showWarningSnackbar,
} from '../../../slices/snackbar-slice';
import UndoIcon from '@aivot/mui-material-symbols-400-n25-outlined/Undo';
import RedoIcon from '@aivot/mui-material-symbols-400-n25-outlined/Redo';
import Save from '@aivot/mui-material-symbols-400-n25-outlined/Save';
import {DeveloperTools} from '../../../components/developer-tools/developer-tools';
import {
    AuthoredElementValues,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    getLiteralElementValue,
    literalAuthoredValue,
} from '../../../models/element-data';
import {RootState} from '../../../store.staff';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {Allotment} from 'allotment';
import {useElementSize} from '../../../utils/element-size';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage, type ShellLoadingMessage} from '../../../slices/shell-slice';
import {PrefillFormDialog} from '../../../dialogs/prefill-form-dialog/prefill-form-dialog';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import QrCode from '@aivot/mui-material-symbols-400-n25-outlined/QrCode';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import Settings from '@aivot/mui-material-symbols-400-n25-outlined/Settings';
import {useElementConfigurationErrors} from '../hooks/use-element-configuration-errors';
import {type Action} from '../../../components/actions/actions-props';
import {useElementEditorNavigation} from '../../../hooks/use-element-editor-navigation';
import Link from '@aivot/mui-material-symbols-400-n25-outlined/Link';
import PictureAsPdf from '@aivot/mui-material-symbols-400-n25-outlined/PictureAsPdf';
import Draw from '@aivot/mui-material-symbols-400-n25-outlined/Draw';
import AccountTree from '@aivot/mui-material-symbols-400-n25-outlined/AccountTree';
import SwipeVertical from '@aivot/mui-material-symbols-400-n25-outlined/SwipeVertical';
import TouchApp from '@aivot/mui-material-symbols-400-n25-outlined/TouchApp';
import BugReport from '@aivot/mui-material-symbols-400-n25-outlined/BugReport';
import {ElementDisplayContext} from '../../../data/element-type/element-child-options';
import {
    ElementTreeInlineEditorContextProvider,
} from '../../../components/element-tree-2/components/element-tree-inline-editor-context';
import {AnyElement} from '../../../models/elements/any-element';
import {useConfirm} from '../../../providers/confirm-provider';
import {generateComponentTitle} from '../../../utils/generate-component-title';
import {isAnyElementWithChildren} from '../../../models/elements/any-element-with-children';
import {cloneElement} from '../../../utils/clone-element';
import {ProcessNodeEntity} from '../../process/entities/process-node-entity';
import {ProcessNodeApiService} from '../../process/services/process-node-api-service';
import {useHasProcessPermission} from '../../permissions/hooks/use-permissions';
import {Permission} from '../../../data/permissions/permission';
import {ElementType} from '../../../data/element-type/element-type';
import {getSingleUseSectionAddDisabledReason} from '../../../data/element-type/single-use-section-types';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {
    FormDetailsPageMoreMenu,
    FormDetailsPageMoreMenuItem,
} from '../../forms/pages/details/components/form-details-page-more-menu';
import {
    ElementDerivationContext,
    type ElementDerivationContextHandle,
} from '../components/element-derivation-context';
import {useChangeBlocker} from '../../../hooks/use-change-blocker-2';
import {AddElementDialog} from '../../../dialogs/add-element-dialog/add-element-dialog';
import {ProcessEntity} from '../../process/entities/process-entity';
import {ProcessVersionEntity} from '../../process/entities/process-version-entity';
import {ProcessDefinitionApiService} from '../../process/services/process-definition-api-service';
import {ProcessDefinitionVersionApiService} from '../../process/services/process-definition-version-api-service';
import {FormHeaderComponent} from '../../../components/form/form-header-component';
import {FormLayoutElement} from '../../../models/elements/form-layout-element';
import {RootStructureActionsContextProvider} from '../../../components/form/root-structure-actions-context';
import {RootComponentFooter} from '../../../components/form/root-component-footer';
import {SUBMIT_EVENT} from '../../../components/form/root.component.view';
import {ProcessTestClaimApiService} from '../../process/services/process-test-claim-api-service';
import {walkAuthoredElementValues} from '../../../utils/element-data-utils';
import {FileUploadElementItem, isFileUploadElementItem} from '../../../models/elements/form/input/file-upload-element';
import {Submitted} from '../../../components/submitted/submitted';
import {setCurrentStep} from '../../../slices/stepper-slice';
import {createCustomerPath} from '../../../utils/url-path-utils';
import {ProcessTestClaimEntity} from '../../process/entities/process-test-claim-entity';
import {downloadQrCode} from '../../../utils/download-qrcode';
import {downloadBlobFile, uploadTextFile} from '../../../utils/download-utils';
import {useNotImplemented} from '../../../hooks/use-not-implemented';
import {ViewDispatcherMode} from '../../../components/view-dispatcher/view-dispatcher.context';
import {ProcessStatus} from '../../process/enums/process-status';
import type {ResolvedThemeDTO, Theme as AppTheme} from '../../themes/models/theme';
import {FormTriggerApiService} from '../../forms/services/form-trigger-api-service';
import {createAppTheme} from '../../../theming/themes';
import {BaseTheme} from '../../../theming/base-theme';
import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';
import {XdfApiService} from '../../xdf/v1/xdf-api-service';
import Code from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import {IdentityProvidersApiService} from '../../identity/identity-providers-api-service';
import {
    IdentityConfigElementOptionWithProvider,
    IdentityConfigElementSlot,
    IdentityConfigElementSlotWithProviders,
} from '../../../models/elements/form/input/identity-config-element';
import IdentityPlatform from '@aivot/mui-material-symbols-400-n25-outlined/IdentityPlatform';
import {useRecordRecentSearchItem} from '../../search/hooks/use-record-recent-search-item';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {
    FormIdentitySelectionControls,
} from '../../identity/components/form-identity-selection-controls/form-identity-selection-controls';
import type {IdentitySlot} from '../../identity/models/identity-slot';
import {normalizeUiDefinitionForStorage} from '../../../utils/ui-definition-utils';
import {useApi} from '../../../hooks/use-api';
import {ThemesApiService} from '../../themes/themes-api-service';
import {AssetsApiService} from '../../assets/assets-api-service';
import {VDepartmentShadowedApiService} from '../../departments/services/v-department-shadowed-api-service';
import {Chip} from '../../../components/chip/chip';
import {quoteString} from '../../../utils/string-utils';
import {PaymentRequestOverview} from '../../payment/components/payment-request-overview';
import {isApiError} from '../../../models/api-error';
import {useEditorDraft} from '../../../hooks/use-editor-draft';
import {useEditorDraftVisit} from '../../../hooks/use-editor-draft-visit';
import {useMergeConflictResolution} from '../../../hooks/use-merge-conflict-resolution';
import {formLayoutMergeSpec} from '../utils/form-layout-merge-spec';
import {mergeFormDraft, rebaseOpenElementDraft} from '../utils/merge-form-draft';
import {
    canPendingSaveStillCommit, type EditorSaveCheckpoint, pendingSaveCanChangeStored, persistEditorSave, rebaseAfterSave, signInToSaveEditorMessage,
} from '../../../utils/editor-save-rebase';
import {editorValuesEqual} from '../../../utils/variable-reference-merge';
import {resolveThemeLogoKey} from '../../../theming/resolve-theme-logo';
import {RichtextComponent} from '../../../components/richtext/richtext.component';
import {resolvePrintablePdfFilename} from '../../forms/utils/printable-pdf-filename';

export const DialogSearchParam = 'dialog';

const FormLayoutFieldKey = 'formLayout';
const IdentitiesFieldKey = 'identities';
const EditorLoadErrorMessage = 'Der Formulareditor konnte nicht geladen werden.';
const MergedSaveMessage = 'Ihre Änderungen wurden mit dem zwischenzeitlich gespeicherten Stand zusammengeführt und gespeichert.';

function createEditorLoadError(error: unknown) {
    return {
        status: isApiError(error) ? error.status : 500,
        message: isApiError(error) && error.displayableToUser ? error.message : EditorLoadErrorMessage,
    };
}

function getIdentityDisplayName(identity: Pick<IdentityConfigElementSlot, 'title'>): string {
    const title = identity.title?.trim();

    return title != null && title.length > 0 ? title : 'Unbenannte Identität';
}

function cloneFormLayoutSnapshot<T extends FormLayoutElement>(element: T): T {
    return normalizeElementDefinition(JSON.parse(JSON.stringify(element)) as T);
}

function getStoredFormLayout(node: ProcessNodeEntity): FormLayoutElement | null {
    return normalizeElementDefinition(getLiteralElementValue<FormLayoutElement>(node.configuration, FormLayoutFieldKey)) ?? null;
}

function sameFormLayout(left: FormLayoutElement | null, right: FormLayoutElement | null): boolean {
    return editorValuesEqual(normalizeUiDefinitionForStorage(left), normalizeUiDefinitionForStorage(right));
}

function toEditableFormLayout(layout: FormLayoutElement | null, rootId?: string): FormLayoutElement {
    if (layout != null) return layout;
    const empty = generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement;
    return rootId == null ? empty : {...empty, id: rootId};
}

export function FormNodeEditorPage() {
    const {nodeId = ''} = useParams<{nodeId: string}>();
    const userId = useAppSelector(selectUser)?.id;
    if (userId == null) return null;
    // A different account or form owns its own layout, open dialogs and outstanding callbacks.
    return <FormNodeEditor key={JSON.stringify([userId, nodeId])} nodeId={nodeId}/>;
}

function FormNodeEditor({nodeId}: {nodeId: string}) {
    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const api = useApi();

    const outerTheme = useTheme();

    const [showRootAddElementDialog, setShowRootAddElementDialog] = useState(false);

    const [node, setNode, currentNode] = useCurrentState<ProcessNodeEntity | null>(null);
    const [formLayout, setFormLayout, currentLayout] = useCurrentState<FormLayoutElement | null>(null);
    const [panelDraft, setPanelDraft, currentPanel] = useCurrentState<ElementPanelDraft | null>(null);
    const panelEditRevision = useRef(0);
    const [saveCheckpoint, setSaveCheckpoint, currentCheckpoint] = useCurrentState<EditorSaveCheckpoint<FormLayoutElement | null> | undefined>(undefined);
    const activeSave = useRef<AbortController | null>(null);
    const [isSaving, setIsSaving] = useState(false);
    const visit = useRef<AbortSignal | null>(null);
    const configuredFormSlug = node == null ? undefined : getLiteralElementValue<string>(node.configuration, 'formSlug');

    const [process, setProcess] = useState<ProcessEntity | null>(null);
    const [processVersion, setProcessVersion] = useState<ProcessVersionEntity | null>(null);
    const [testClaim, setTestClaim] = useState<ProcessTestClaimEntity | null>(null);
    const testClaimRef = useRef<ProcessTestClaimEntity | null>(null);
    const [formTheme, setFormTheme] = useState<ResolvedThemeDTO>();
    const [draftPreviewThemeChain, setDraftPreviewThemeChain] = useState<AppTheme[] | null>(null);

    const [identityMappingInformation, setIdentityMappingInformation] = useState<IdentityConfigElementSlotWithProviders[]>([]);
    const [showIdentityDialog, setShowIdentityDialog] = useState(false);
    const [identitySlots, setIdentitySlots] = useState<IdentitySlot[]>([]);
    const [isLoadingIdentitySlots, setIsLoadingIdentitySlots] = useState(false);
    const [identitySlotsLoadFailed, setIdentitySlotsLoadFailed] = useState(false);
    const configuredIdentitySlots = useMemo(() => (
        node == null ? undefined : getLiteralElementValue<IdentityConfigElementSlot[]>(node.configuration, IdentitiesFieldKey)
    ) ?? [], [node]);
    const sortedIdentitySlots = useMemo(() => {
        return identitySlots
            .map((slot, index) => ({
                slot,
                index,
            }))
            .sort((a, b) => Number(a.slot.isOptional) - Number(b.slot.isOptional) || a.index - b.index)
            .map(({slot}) => slot);
    }, [identitySlots]);
    const identitySelectionApi = useMemo(() => {
        const formSlug = node == null ? undefined : getLiteralElementValue<string>(node.configuration, 'formSlug');
        if (process == null || node == null || formSlug == null) {
            return null;
        }

        return new FormTriggerApiService().createIdentitySelectionApi(
            process.slug,
            formSlug,
            node.id,
            testClaim?.accessKey,
        );
    }, [node, process, testClaim]);

    const [startedProcessAccessInfo, setStartedProcessAccessInfo] = useState<{
        processInstanceAccessKey: string;
        paymentRequired: boolean;
    } | null>(null);

    const storedFormLayout = useMemo(() => node == null ? null : getStoredFormLayout(node), [node]);
    const hasLayoutChanges = useMemo(() => !sameFormLayout(storedFormLayout, formLayout), [storedFormLayout, formLayout]);
    useLayoutEffect(() => {
        if (saveCheckpoint?.phase === 'confirmed' && !hasLayoutChanges && panelDraft == null) {
            setSaveCheckpoint(undefined);
        }
    }, [saveCheckpoint, hasLayoutChanges, panelDraft, setSaveCheckpoint]);
    const hasLayoutToSave = hasLayoutChanges || saveCheckpoint?.phase === 'pending';
    const isEditable = processVersion?.status === ProcessStatus.Drafted;
    const draftNodeReady = node?.id === Number(nodeId) && processVersion != null &&
        processVersion.processId === node.processId && processVersion.processVersion === node.processVersion;
    const visitKey = draftNodeReady && isEditable ? nodeId : null;
    const {checked, isCurrent, finish, canEdit} = useEditorDraftVisit(visitKey);
    const checkingDraft = isEditable && !checked;

    const {
        dialog: changeBlockerDialog,
        hasChanged,
    } = useChangeBlocker({
        original: false,
        edited: hasLayoutToSave || panelDraft != null,
        isActive: checked,
        visitKey,
        onConfirmNavigation: async () => {
            if (!canEdit()) return;
            activeSave.current?.abort();
            activeSave.current = null;
            setIsSaving(false);
            await discardForNavigation();
        },
    });

    const {
        resolvePrepared,
        dialog: mergeConflictDialog,
    } = useMergeConflictResolution();

    useRecordRecentSearchItem(ServerEntityType.ProcessNodes, node?.id.toString());

    useEffect(() => {
        if (node == null) {
            return;
        }

        new IdentityProvidersApiService()
            .listAll()
            .then((page) => {
                const mappedIdentities = getLiteralElementValue<IdentityConfigElementSlot[]>(node.configuration, IdentitiesFieldKey);

                if (mappedIdentities == null || mappedIdentities.length === 0) {
                    return [];
                }

                const identityMappingInformation: IdentityConfigElementSlotWithProviders[] = [];
                for (const identity of mappedIdentities) {
                    const updatedOptions: IdentityConfigElementOptionWithProvider[] = (identity.options ?? [])
                        .map((opt) => ({
                            ...opt,
                            provider: page.content.find(idp => idp.key === opt.identityProviderKey)!,
                        }))
                        .filter((opt) => opt.provider != null);

                    if (updatedOptions.length > 0) {
                        identityMappingInformation.push({
                            ...identity,
                            options: updatedOptions,
                        });
                    }
                }

                return identityMappingInformation;
            })
            .then(setIdentityMappingInformation)
            .catch((err) => {
                dispatch(showApiErrorSnackbar(err, 'Beim Laden der Identitätsanbieter ist ein unbekannter Fehler aufgetreten'));
            });
    }, [node]);

    useEffect(() => {
        if (!showIdentityDialog) {
            return;
        }

        const formSlug = node == null ? undefined : getLiteralElementValue<string>(node.configuration, 'formSlug');
        if (node == null || process == null || testClaim == null || typeof formSlug !== 'string' || formSlug.length === 0) {
            setIdentitySlots([]);
            setIsLoadingIdentitySlots(false);
            setIdentitySlotsLoadFailed(false);
            return;
        }

        let isCancelled = false;
        setIdentitySlots([]);
        setIsLoadingIdentitySlots(true);
        setIdentitySlotsLoadFailed(false);

        new FormTriggerApiService()
            .getIdentitySlots(process.slug, formSlug, testClaim.accessKey)
            .then((slots) => {
                if (!isCancelled) {
                    setIdentitySlots(slots);
                }
            })
            .catch((error) => {
                if (!isCancelled) {
                    setIdentitySlotsLoadFailed(true);
                    dispatch(showApiErrorSnackbar(
                        error,
                        'Die Identitäten für den Formulartest konnten nicht geladen werden.',
                    ));
                }
            })
            .finally(() => {
                if (!isCancelled) {
                    setIsLoadingIdentitySlots(false);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [dispatch, node, process, showIdentityDialog, testClaim]);

    useEffect(() => {
        let cancelled = false;
        const nodeIdInt = parseInt(nodeId);
        dispatch(setCurrentStep(0));
        new ProcessNodeApiService()
            .retrieve(nodeIdInt)
            .then((node) => {
                if (cancelled) {
                    return;
                }
                let uiElement = getLiteralElementValue<FormLayoutElement>(node.configuration, FormLayoutFieldKey);
                if (uiElement == null) {
                    uiElement = generateElementWithDefaultValues(ElementType.FormLayout);
                }

                setNode(node);
                setFormLayout(cloneFormLayoutSnapshot(uiElement as FormLayoutElement));
            })
            .catch((err) => {
                if (!cancelled) {
                    dispatch(setErrorMessage(createEditorLoadError(err)));
                }
            });
        return () => {
            cancelled = true;
        };
    }, [dispatch, nodeId]);

    useEffect(() => {
        if (node == null) {
            setProcess(null);
            setProcessVersion(null);
            setTestClaim(null);
            testClaimRef.current = null;
            return;
        }

        let cancelled = false;
        Promise.all([
            new ProcessDefinitionApiService()
                .retrieve(node.processId),
            new ProcessDefinitionVersionApiService()
                .retrieve({
                    processDefinitionId: node.processId,
                    processDefinitionVersion: node.processVersion,
                }),
            new ProcessTestClaimApiService()
                .listAll({
                    processId: node.processId,
                    processVersion: node.processVersion,
                }),
        ])
            .then(([process, version, testClaims]) => {
                if (cancelled) {
                    return;
                }
                setProcess(process);
                setProcessVersion(version);
                if (testClaims.content.length > 0) {
                    setTestClaim(testClaims.content[0]);
                    testClaimRef.current = testClaims.content[0];
                } else {
                    setTestClaim(null);
                    testClaimRef.current = null;
                }
            })
            .catch((err) => {
                if (!cancelled) {
                    dispatch(setErrorMessage(createEditorLoadError(err)));
                }
            });
        return () => {
            cancelled = true;
        };
    }, [node]);

    useEffect(() => {
        if (process == null || processVersion == null || configuredFormSlug == null) {
            setFormTheme(undefined);
            return;
        }

        let isCancelled = false;

        new FormTriggerApiService()
            .getFormTheme(
                process.slug,
                configuredFormSlug,
                processVersion.processVersion,
                testClaim?.accessKey,
            )
            .then((theme) => {
                if (!isCancelled) {
                    setFormTheme(theme);
                }
            })
            .catch((error) => {
                console.error('Error loading form preview theme:', error);
                if (!isCancelled) {
                    setFormTheme(undefined);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [configuredFormSlug, node, process, processVersion, testClaim]);

    const hasFormLayout = formLayout != null;
    const selectedProcessVersionThemeId = processVersion?.themeId ?? null;
    const selectedResponsibleDepartmentId = formLayout?.responsibleDepartmentId ?? null;
    const selectedManagingDepartmentId = formLayout?.managingDepartmentId ?? null;
    const processDepartmentId = process?.departmentId ?? null;

    useEffect(() => {
        if (!hasFormLayout) {
            setDraftPreviewThemeChain(null);
            return;
        }

        let isCancelled = false;

        // The public theme endpoint resolves persisted data only. Resolve draft department changes here
        // so the preview reflects unsaved form configuration immediately.
        setDraftPreviewThemeChain([]);

        const themesApi = new ThemesApiService();
        const departmentsApi = new VDepartmentShadowedApiService();

        const appendTheme = async (themeChain: AppTheme[], themeId: number | null | undefined) => {
            if (themeId == null) {
                return;
            }

            try {
                themeChain.push(await themesApi.retrieve(themeId));
            } catch (error) {
                console.error('Error loading draft form preview theme:', error);
            }
        };

        const appendDepartmentTheme = async (themeChain: AppTheme[], departmentId: number | null | undefined) => {
            if (departmentId == null) {
                return;
            }

            try {
                const department = await departmentsApi.retrieve(departmentId);
                await appendTheme(themeChain, department.themeId);
            } catch (error) {
                console.error('Error loading draft form preview department theme:', error);
            }
        };

        (async () => {
            const themeChain: AppTheme[] = [];

            await appendTheme(themeChain, selectedProcessVersionThemeId);
            await appendDepartmentTheme(themeChain, selectedResponsibleDepartmentId);
            await appendDepartmentTheme(themeChain, selectedManagingDepartmentId);
            await appendDepartmentTheme(themeChain, processDepartmentId);
            themeChain.push(AppConfig.systemTheme);

            if (!isCancelled) {
                setDraftPreviewThemeChain(themeChain);
            }
        })();

        return () => {
            isCancelled = true;
        };
    }, [
        api,
        hasFormLayout,
        selectedProcessVersionThemeId,
        selectedResponsibleDepartmentId,
        selectedManagingDepartmentId,
        processDepartmentId,
    ]);

    const [searchParams] = useSearchParams();
    const metaDialogName = useMemo(() => searchParams.get(DialogSearchParam), [searchParams]);

    const {
        navigateToElementEditor,
        currentEditedElementId,
        closeElementEditor,
    } = useElementEditorNavigation();

    const [showPrefillDialog, setShowPrefillDialog] = useState(false);
    const [showMoreMenuAtEl, setShowMoreMenuAtEl] = useState<HTMLElement | null>(null);
    const showDeveloperTools = useAppSelector(selectDevToolsTab);
    const [highlightElementId, setHighlightElementId] = useState<string | null>(null);
    const [highlightElementSignal, setHighlightElementSignal] = useState(0);
    const [hoveredTreeElementId, setHoveredTreeElementId] = useState<string | null>(null);
    const [openAddSectionSignal, setOpenAddSectionSignal] = useState(0);

    const [authoredElementValues, setAuthoredElementValues] = useState<AuthoredElementValues>({});
    const [derivedData, setDerivedData] = useState<DerivedRuntimeElementData>(createDerivedRuntimeElementData());
    const elementDerivationContextRef = useRef<ElementDerivationContextHandle>(null);
    const [disableVisibility, setDisableVisibility] = useState(false);
    const [disableValidation, setDisableValidation] = useState(false);

    useEffect(() => {
        setDisableVisibility(false);
        setDisableValidation(false);
    }, [nodeId]);

    const {
        disableAutoScrollForSteps,
        disableElementContextMenu,
        hideComponentTree,
    } = useAppSelector((state: RootState) => state.adminSettings);

    const [pastLoadedForm, setPastLoadedForm] = useState<FormLayoutElement[]>([]);
    const hasPastLoadedForm = useMemo(() => pastLoadedForm.length > 0, [pastLoadedForm]);

    const [futureLoadedForm, setFutureLoadedForm] = useState<FormLayoutElement[]>([]);
    const hasFutureLoadedForm = useMemo(() => futureLoadedForm.length > 0, [futureLoadedForm]);

    const metaDialog = useAppSelector((state) => state.app.showDialog);

    const notImplemented = useNotImplemented();

    useLayoutEffect(() => {
        const controller = new AbortController();
        visit.current = controller.signal;
        setShowRootAddElementDialog(false);
        setShowMoreMenuAtEl(null);
        setIsSaving(false);
        return () => {
            controller.abort();
            activeSave.current?.abort();
            activeSave.current = null;
        };
    }, [isCurrent]);

    const {
        loadDraft,
        clearDraft,
        discardForNavigation,
        draftLogoutSignal, isDraftRecoveryCurrent, beginManualSave, confirmManualSave, backUpDraft,
    } = useEditorDraft<FormLayoutElement | null, ElementPanelDraft>({
        draftKey: visitKey == null ? null : `process-node/${nodeId}/form-layout`,
        base: storedFormLayout,
        baseLockVersion: node?.lockVersion,
        edited: formLayout,
        context: panelDraft ?? undefined,
        contextRevision: panelEditRevision.current,
        hasChangesWithoutContext: hasLayoutChanges,
        saveCheckpoint,
        hasChanged,
        enabled: checked,
    });

    // Revisit recovery only on a different editor load, not on a save that updates the same node.
    useEffect(() => {
        if (node == null || formLayout == null || !isCurrent()) {
            return;
        }
        const controller = new AbortController();
        const signal = draftLogoutSignal ? AbortSignal.any([controller.signal, draftLogoutSignal]) : controller.signal;
        if (signal.aborted) {
            finish();
            return;
        }
        draftLogoutSignal?.addEventListener('abort', finish);
        (async () => {
            const draft = await loadDraft();
            if (draft == null || signal.aborted || !isCurrent()) {
                return;
            }

            const checkpoint = draft.saveCheckpoint;
            // The GET may precede the outstanding PUT. Even a reversion to the value we just
            // read needs an explicit decision until a later write rules out that PUT.
            const stillPending = canPendingSaveStillCommit(checkpoint, draft.baseLockVersion, node.lockVersion) &&
                pendingSaveCanChangeStored(checkpoint, storedFormLayout, formLayoutMergeSpec);
            const outcome = await resolvePrepared({
                merge: (choices) => {
                    const merged = mergeFormDraft({
                        base: draft.base, mine: draft.mine, theirs: storedFormLayout,
                        editableRoot: formLayout, panel: draft.context, saveCheckpoint: checkpoint,
                    }, choices);
                    return stillPending ? {...merged, changed: true} : merged;
                },
                mode: 'restore',
                signal,
                storedStateChanged: draft.baseLockVersion !== (node.lockVersion ?? null),
            });
            if (!await isDraftRecoveryCurrent() || signal.aborted || !isCurrent()) {
                return;
            }
            const recoveredPanel = outcome.status === 'apply' ? outcome.value.panel : undefined;
            if (recoveredPanel) {
                navigateToElementEditor(recoveredPanel.base.id, recoveredPanel.tab);
            } else if (draft.context && currentEditedElementId === draft.context.base.id) {
                closeElementEditor();
            }
            if (outcome.status === 'apply') {
                const {layout} = outcome.value;
                if (!sameFormLayout(layout, formLayout)) {
                    applyFormLayout(toEditableFormLayout(layout));
                }
                if (stillPending) setSaveCheckpoint(checkpoint);
                setPanelDraft(recoveredPanel ?? null);
            } else {
                // Discarded, or nothing left to restore because the changes were saved elsewhere.
                await clearDraft().catch(() => {
                    if (isCurrent() && !signal.aborted) {
                        dispatch(showErrorSnackbar('Die zwischengespeicherten Änderungen konnten nicht gelöscht werden.'));
                    }
                });
            }
        })()
            .catch(console.error)
            .finally(() => {
                if (!controller.signal.aborted) {
                    finish();
                }
            });
        return () => {
            controller.abort();
            draftLogoutSignal?.removeEventListener('abort', finish);
        };
    }, [isCurrent, finish, loadDraft, draftLogoutSignal, isDraftRecoveryCurrent]);

    // Validated here, not only in the tree, because the settings button also shows the errors while the tree is hidden.
    const configurationErrors = useElementConfigurationErrors(formLayout, isEditable);
    const hasRootConfigurationErrors = Object.keys(configurationErrors?.properties ?? {}).length > 0;
    const canUpdateProcess = useHasProcessPermission(node?.processId, Permission.PROCESS_DEFINITION_UPDATE);
    const previewTheme = useMemo(() => {
        const activeFormTheme = draftPreviewThemeChain?.[0] ?? (
            draftPreviewThemeChain == null ?
                formTheme :
                undefined
        );

        if (activeFormTheme == null) {
            return outerTheme;
        }

        return createAppTheme(activeFormTheme, BaseTheme, outerTheme.palette.mode);
    }, [draftPreviewThemeChain, formTheme, outerTheme]);
    const {
        ref: containerRef,
        size: containerSize,
    } = useElementSize<HTMLDivElement>();
    const developerToolsMinHeight = 280;
    const developerToolsMaxHeight = containerSize.height > 0 ? Math.max(developerToolsMinHeight, Math.floor(containerSize.height * 0.5)) : undefined;

    useEffect(() => {
        dispatch(showDialog(metaDialogName ?? undefined));
    }, [metaDialogName]);

    const handleUndo = () => {
        if (!canEdit() || formLayout == null || pastLoadedForm.length === 0) {
            return;
        }

        const previousForm = pastLoadedForm[pastLoadedForm.length - 1];
        setPastLoadedForm((currentPastLoadedForm) => currentPastLoadedForm.slice(0, -1));
        setFutureLoadedForm((currentFutureLoadedForm) => [
            ...currentFutureLoadedForm,
            cloneFormLayoutSnapshot(formLayout),
        ]);
        setFormLayout(cloneFormLayoutSnapshot(previousForm));
    };

    const handleRedo = () => {
        if (!canEdit() || formLayout == null || futureLoadedForm.length === 0) {
            return;
        }

        const nextForm = futureLoadedForm[futureLoadedForm.length - 1];
        setFutureLoadedForm((currentFutureLoadedForm) => currentFutureLoadedForm.slice(0, -1));
        setPastLoadedForm((currentPastLoadedForm) => [
            ...currentPastLoadedForm,
            cloneFormLayoutSnapshot(formLayout),
        ]);
        setFormLayout(cloneFormLayoutSnapshot(nextForm));
    };

    const acceptStoredForm = (stored: ProcessNodeEntity, submitted: FormLayoutElement) => {
        const latest = currentLayout.current;
        if (latest == null) return;

        const confirmed = getStoredFormLayout(stored);
        const rebased = rebaseAfterSave(submitted, latest, confirmed, formLayoutMergeSpec);
        const layout = toEditableFormLayout(rebased.value, latest.id);
        const panel = rebaseOpenElementDraft(currentPanel.current, latest, layout);

        setNode(stored);
        setFormLayout(layout);
        setPanelDraft(panel.draft);
        setSaveCheckpoint(rebased.checkpoint ?? (panel.needsDecision
            ? {phase: 'confirmed', submitted} : undefined));

        // Undo and redo snapshots predate the acknowledgement as well. Preserve independent
        // server changes when undoing a later edit; never replay an ambiguous structural change.
        if (!sameFormLayout(submitted, confirmed)) {
            const rebaseHistory = (history: FormLayoutElement[]) => history.flatMap((entry) => {
                const next = rebaseAfterSave(submitted, entry, confirmed, formLayoutMergeSpec);
                return next.checkpoint ? [] : [toEditableFormLayout(next.value, layout.id)];
            });
            setPastLoadedForm(rebaseHistory);
            setFutureLoadedForm(rebaseHistory);
        }
    };

    const runningSave = useRef<Promise<ProcessNodeEntity | null> | null>(null);

    /** The return value is the confirmed server version used by export and test submission. */
    const saveForm = (): Promise<ProcessNodeEntity | null> => {
        // A second PUT would carry the same lock version and conflict, so export and test wait for the running
        // save of this visit instead. A save that left with an earlier visit is no longer shared.
        if (activeSave.current == null || runningSave.current == null) {
            const save: Promise<ProcessNodeEntity | null> = performSaveForm().finally(() => {
                if (runningSave.current === save) runningSave.current = null;
            });
            runningSave.current = save;
        }
        return runningSave.current;
    };

    const performSaveForm = async (): Promise<ProcessNodeEntity | null> => {
        const submitted = currentLayout.current;
        const original = currentNode.current;
        if (!canEdit() || original == null || submitted == null || activeSave.current != null) {
            return null;
        }

        const controller = new AbortController();
        let signal = controller.signal;
        activeSave.current = controller;
        setIsSaving(true);
        const priorCheckpoint = currentCheckpoint.current;
        let resetCheckpoint = true;
        try {
            const token = await beginManualSave();
            if (signal.aborted) return null;
            if (!token) {
                dispatch(showErrorSnackbar(signInToSaveEditorMessage));
                return null;
            }
            signal = AbortSignal.any([signal, token.signal]);
            const api = new ProcessNodeApiService();
            const result = await persistEditorSave<FormLayoutElement | null, ProcessNodeEntity>({
                original, submitted, checkpoint: priorCheckpoint, signal, resolvePrepared,
                requireServerConfirmation: token.resumesDrafts,
                valueOf: getStoredFormLayout,
                specFor: () => formLayoutMergeSpec,
                read: () => api.retrieve(original.id),
                write: (base, layout, reserveVersion) => {
                    const update = reserveVersion ? base : {
                        ...base,
                        configuration: {
                            ...base.configuration,
                            [FormLayoutFieldKey]: literalAuthoredValue(normalizeUiDefinitionForStorage(layout)),
                        },
                    };
                    return api.update(base.id, update, {query: {onlyConfigSave: FormLayoutFieldKey}, abortBeforeSend: signal});
                },
                setPending: setSaveCheckpoint,
            });
            signal.throwIfAborted();
            if (result.status === 'cancel') return null;
            // If a dispatched write lost its response, keep its checkpoint for recovery.
            resetCheckpoint = false;
            if (result.status === 'unknown') throw result.error;
            acceptStoredForm(result.stored, submitted);
            if (result.status === 'discarded') return null;
            await confirmManualSave(token);
            if (signal.aborted) return null;
            if (result.merged) dispatch(showSuccessSnackbar(MergedSaveMessage));
            return result.stored;
        } catch (error) {
            if (!signal.aborted) throw error;
            return null;
        } finally {
            if (activeSave.current === controller) {
                if (resetCheckpoint) setSaveCheckpoint(priorCheckpoint);
                activeSave.current = null;
                setIsSaving(false);
            }
        }
    };

    const handleSave = async (): Promise<void> => {
        try {
            await saveForm();
        } catch (err) {
            void showEditorSaveErrorSnackbar(err, 'Fehler beim Speichern der Änderungen', backUpDraft).then(dispatch);
        }
    };

    const onBackToProcess = () => {
        if (node == null) {
            return;
        }

        navigate(`/processes/${node.processId}/versions/${node.processVersion}/nodes/${node.id}`);
    };

    const scrollContainerRef = useRef<HTMLDivElement>(null);

    const publicFormLink = createCustomerPath(`/form/${process?.slug}/${configuredFormSlug}${testClaim != null ? `?test-claim=${testClaim.accessKey}` : ''}`);

    const showLoadingForVisit = (message: ShellLoadingMessage, signal: AbortSignal) => {
        // Release the shell indicator when leaving. A late completion must not clear the next page's indicator.
        const clear = () => dispatch(clearLoadingMessage());
        signal.addEventListener('abort', clear, {once: true});
        dispatch(setLoadingMessage(message));
        return () => {
            signal.removeEventListener('abort', clear);
            if (!signal.aborted) clear();
        };
    };

    const handleImportFromXDF = async () => {
        const signal = visit.current;
        if (!canEdit() || signal == null) {
            return;
        }

        let xmlContent: string | null;
        try {
            const conf = await confirm({
                title: 'XDatenfeld-Schema importieren',
                children: (
                    <>
                        <Typography>
                            Sie sind im Begriff, ein XDatenfeld-Schema zu importieren.
                        </Typography>
                        <Typography>
                            Beim Import werden ggf. bereits bestehende Formularelemente im Editor durch die importierte
                            Struktur ersetzt.
                            Möchten Sie den Vorgang wirklich fortsetzen?
                        </Typography>
                    </>
                ),
                confirmButtonText: 'Ja, mit dem Import fortfahren',
            });

            if (!conf || !canEdit()) {
                return;
            }

            xmlContent = await uploadTextFile('text/xml');

            if (xmlContent == null || !canEdit()) {
                return;
            }
        } catch (err) {
            if (canEdit()) dispatch(showApiErrorSnackbar(err, 'Beim Importieren des XDatenfeld-Schemas ist ein Fehler aufgetreten.'));
            return;
        }

        const stopLoading = showLoadingForVisit({
            blocking: true,
            estimatedTime: 1000,
            message: 'Importiere XDF',
        }, signal);

        try {
            const transformed = await new XdfApiService().xdfTransform(xmlContent);

            if (canEdit()) setFormLayout(transformed);
        } catch (err) {
            if (canEdit()) dispatch(showApiErrorSnackbar(err, 'Beim Importieren des XDatenfeld-Schemas ist ein Fehler aufgetreten.'));
        } finally {
            stopLoading();
        }
    };

    const handleCopyPublicFormLink = async () => {
        try {
            const success = await copyToClipboardText(publicFormLink);
            if (!success) {
                throw new Error('copy failed');
            }
            dispatch(showSuccessSnackbar('Formularlink in Zwischenablage kopiert'));
        } catch (err) {
            console.error(err);
            dispatch(showErrorSnackbar('Formularlink konnte nicht kopiert werden'));
        }
    };

    const handleDownloadPublicQrCode = async () => {
        try {
            await downloadQrCode(publicFormLink, `qr-code-${nodeId}.png`);
            dispatch(showSuccessSnackbar('QR-Code wurde als PNG heruntergeladen!'));
        } catch (err) {
            console.error(err);
            dispatch(showErrorSnackbar('Fehler beim Herunterladen des QR-Codes!'));
        }
    };

    const downloadPersistedPdfFile = async (stored: ProcessNodeEntity, signal: AbortSignal): Promise<void> => {
        const filename = resolvePrintablePdfFilename(getStoredFormLayout(stored)?.publicTitle, stored.name);

        const stopLoading = showLoadingForVisit({
            blocking: false,
            estimatedTime: 1500,
            message: 'Vordruck wird generiert',
        }, signal);

        try {
            const blob = await new FormTriggerApiService()
                .downloadPrintablePdf(stored.id);

            if (signal.aborted) return;
            downloadBlobFile(filename, blob);
            dispatch(showSuccessSnackbar('Der Vordruck wurde erfolgreich erstellt und der Download gestartet.'));
        } catch (err) {
            if (signal.aborted) return;
            console.error(err);
            dispatch(showApiErrorSnackbar(err, 'Fehler beim Generieren des Vordrucks'));
        } finally {
            stopLoading();
        }
    };

    const handleDownloadPdfFile = async (): Promise<void> => {
        const signal = visit.current;
        if (checkingDraft || node?.id !== Number(nodeId) || signal == null || signal.aborted) {
            return;
        }

        let confirmed = node;

        if (hasLayoutToSave) {
            const saveNow = await confirm({
                title: 'Ungespeicherte Änderungen',
                children: (
                    <Typography>
                        Sie haben aktuell ungespeicherte Änderungen.
                        Der Vordruck wird aus der gespeicherten Formularversion erzeugt.
                        Speichern Sie Ihre Änderungen, bevor Sie die PDF-Datei herunterladen.
                    </Typography>
                ),
                confirmButtonText: 'Speichern und Vordruck exportieren',
            });

            if (saveNow && !signal.aborted) {
                try {
                    const saved = await saveForm();
                    if (saved == null || signal.aborted) return;
                    confirmed = saved;
                } catch (err) {
                    console.error(err);
                    void showEditorSaveErrorSnackbar(err, 'Fehler beim Speichern der Änderungen', backUpDraft).then(dispatch);
                    return;
                }
            } else {
                return;
            }
        }

        await downloadPersistedPdfFile(confirmed, signal);
    };

    const applyFormLayout = (element: FormLayoutElement) => {
        const previous = currentLayout.current;
        if (previous != null) {
            setPastLoadedForm((currentPastLoadedForm) => [
                ...currentPastLoadedForm,
                cloneFormLayoutSnapshot(previous),
            ]);
        }
        setFutureLoadedForm([]);
        setFormLayout(cloneFormLayoutSnapshot(element));
    };

    const handlePatch = (element: FormLayoutElement) => {
        if (canEdit()) applyFormLayout(element);
    };

    const handleCloneElement = (element: AnyElement) => {
        if (!canEdit() || formLayout == null) {
            return;
        }

        const disabledReason = getSingleUseSectionAddDisabledReason(formLayout, element.type);
        if (disabledReason != null) {
            dispatch(showErrorSnackbar(disabledReason));
            return;
        }

        function cloneElementRecursive<T extends AnyElement>(currentElement: T): T {
            if (isAnyElementWithChildren(currentElement) && currentElement.children != null) {
                const clonedChildIndex = currentElement
                    .children
                    .findIndex(child => child.id == element.id);

                if (clonedChildIndex !== -1) {
                    const clone = cloneElement(element);

                    const updatedChildren = [
                        ...currentElement.children,
                    ];
                    updatedChildren.splice(clonedChildIndex + 1, 0, clone);
                    dispatch(showSuccessSnackbar(`${generateComponentTitle(element)} wurde erfolgreich dupliziert.`));
                    return {
                        ...currentElement,
                        children: updatedChildren,
                    };
                } else {
                    return {
                        ...currentElement,
                        children: currentElement
                            .children
                            .map(child => cloneElementRecursive(child)),
                    };
                }
            } else {
                return currentElement;
            }
        }

        handlePatch(cloneElementRecursive(formLayout));
    };

    const handleDeleteElement = (element: AnyElement) => {
        if (!canEdit() || formLayout == null) {
            return;
        }

        confirm({
            isDestructive: true,
            title: 'Element wirklich löschen',
            children: (
                <Typography>
                    Wollen Sie das Element <strong>{generateComponentTitle(element)}</strong> wirklich löschen?
                </Typography>
            ),
        })
            .then((conf) => {
                const latest = currentLayout.current;
                if (!conf || !canEdit() || latest == null) {
                    return;
                }

                function deleteElementRecursive<T extends AnyElement>(currentElement: T): T {
                    if (isAnyElementWithChildren(currentElement) && currentElement.children != null) {
                        return {
                            ...currentElement,
                            children: currentElement
                                .children
                                .filter(child => child.id !== element.id)
                                .map(child => deleteElementRecursive(child)),
                        };
                    } else {
                        return currentElement;
                    }
                }

                handlePatch(deleteElementRecursive(latest));
            });
    };

    const handleOpenElement = (element: AnyElement, tab?: string | null) => {
        navigateToElementEditor(element.id, tab);
    };

    const handleHighlightElementInTree = (element: AnyElement) => {
        setHighlightElementId(element.id);
        setHighlightElementSignal((prev) => prev + 1);
        navigateToElementEditor(element.id, null);
    };

    const openIdentityDialog = () => {
        setIdentitySlots([]);
        setIdentitySlotsLoadFailed(false);
        setShowIdentityDialog(true);
    };

    const closeIdentityDialog = () => {
        setShowIdentityDialog(false);
        setIdentitySlots([]);
        setIsLoadingIdentitySlots(false);
        setIdentitySlotsLoadFailed(false);
    };

    const moreMenuItems: FormDetailsPageMoreMenuItem[] = [
        {
            label: 'Öffentl. Link in Zwischenablage kopieren',
            icon: <Link/>,
            onClick: () => {
                void handleCopyPublicFormLink();
            },
        },
        {
            label: 'QR-Code mit öffentl. Link herunterladen',
            icon: <QrCode/>,
            onClick: () => {
                void handleDownloadPublicQrCode();
            },
        },
        'separator',
        {
            label: 'Vordruck exportieren (.pdf)',
            icon: <PictureAsPdf/>,
            onClick: () => {
                void handleDownloadPdfFile();
            },
        },
        {
            label: 'Formular vorbefüllen',
            icon: <Draw/>,
            onClick: () => {
                notImplemented();
                // setShowPrefillDialog(true);
            },
        },
        {
            label: 'XDatenfeld-Schema importieren',
            icon: <Code/>,
            onClick: handleImportFromXDF,
            disabled: !checked,
        },
        'separator',
        {
            type: 'toggle',
            label: 'Formularstruktur anzeigen',
            icon: <AccountTree/>,
            checked: !hideComponentTree,
            onToggle: () => {
                dispatch(toggleComponentTree());
            },
        },
        {
            type: 'toggle',
            label: 'Autom. Scrollen aktivieren',
            icon: <SwipeVertical/>,
            checked: !disableAutoScrollForSteps,
            onToggle: () => {
                dispatch(toggleAutoScrollForSteps());
            },
        },
        {
            type: 'toggle',
            label: 'Element-Kontextmenü aktivieren',
            icon: <TouchApp/>,
            checked: !disableElementContextMenu,
            onToggle: () => {
                dispatch(toggleElementContextMenu());
            },
        },
        {
            label: 'Testidentitäten auswählen',
            icon: <IdentityPlatform/>,
            onClick: openIdentityDialog,
            visible: configuredIdentitySlots.length > 0,
            disabled: testClaim == null,
            disabledTooltip: 'Um Identitäten für den Formulartest auszuwählen, muss sich der Prozess im Testmodus befinden.',
        },
        {
            label: 'Entwicklerwerkzeuge öffnen',
            icon: <BugReport/>,
            onClick: () => {
                dispatch(setDevToolsTab(showDeveloperTools ?? 0));
            },
        },
    ];

    const headerActions: Action[] = [
        {
            tooltip: 'Änderung rückgängig machen',
            icon: <UndoIcon/>,
            onClick: handleUndo,
            disabled: !hasPastLoadedForm || !checked,
        },
        {
            tooltip: 'Änderung wiederherstellen',
            icon: <RedoIcon/>,
            onClick: handleRedo,
            disabled: !hasFutureLoadedForm || !checked,
        },
        'separator' as const,
        {
            tooltip: disableValidation ? 'Validierungen aktivieren' : 'Validierungen deaktivieren',
            icon: disableValidation ? <DoneAllOutlinedIcon/> : <RemoveDoneOutlinedIcon/>,
            onClick: () => {
                setDisableValidation((current) => !current);
            },
        },
        {
            tooltip: disableVisibility ? 'Sichtbarkeiten aktivieren' : 'Sichtbarkeiten deaktivieren',
            icon: disableVisibility ? <VisibilityOutlinedIcon/> : <VisibilityOffOutlinedIcon/>,
            onClick: () => {
                setDisableVisibility((current) => !current);
            },
        },
        'separator' as const,
        {
            tooltip: hasRootConfigurationErrors ?
                'Formular-Einstellungen öffnen (fehlerhafte Konfiguration)' :
                'Formular-Einstellungen öffnen',
            icon: (
                <Badge
                    color="error"
                    variant="dot"
                    overlap="circular"
                    invisible={!hasRootConfigurationErrors}
                >
                    <Settings/>
                </Badge>
            ),
            disabled: checkingDraft,
            onClick: () => {
                if (formLayout == null) {
                    return;
                }

                navigateToElementEditor(formLayout.id);
            },
        },
        {
            tooltip: 'Weitere Optionen',
            icon: <MoreVert/>,
            disabled: checkingDraft,
            onClick: (event: React.MouseEvent<HTMLButtonElement>) => {
                setShowMoreMenuAtEl(event.currentTarget);
            },
        },
        'separator' as const,
        {
            label: 'Zurück zum Prozess',
            onClick: onBackToProcess,
            variant: 'text' as const,
            activeStyle: {
                whiteSpace: 'nowrap',
            },
        },
        {
            label: 'Speichern',
            tooltip: 'Änderungen am Formular speichern',
            icon: <Save/>,
            iconPosition: 'start' as const,
            onClick: handleSave,
            variant: 'contained' as const,
            disabled: !hasLayoutToSave || !checked || isSaving,
        },
    ];

    if (formLayout == null || node == null || process == null || processVersion == null || !draftNodeReady) {
        return;
    }

    // Use the locally resolved draft chain so unsaved theme assignments are reflected immediately.
    const resolveDraftLogoUrl = (colorScheme: 'light' | 'dark'): string | null => {
        if (draftPreviewThemeChain == null) {
            return colorScheme === 'dark'
                ? formTheme?.logoUrlDark ?? null
                : formTheme?.logoUrl ?? null;
        }

        const activeTheme = draftPreviewThemeChain[0];
        if (activeTheme == null) {
            return null;
        }

        const logoKey = resolveThemeLogoKey(activeTheme, colorScheme);
        if (logoKey != null) {
            return AssetsApiService.useAssetLink(logoKey);
        }
        return null;
    };
    const formLogoUrl = resolveDraftLogoUrl('light');
    const formLogoUrlDark = resolveDraftLogoUrl('dark');

    const handleSubmitEvent = async (values: AuthoredElementValues, event: string): Promise<void> => {
        const signal = visit.current;
        if (checkingDraft || event != SUBMIT_EVENT || node.id !== Number(nodeId) || signal == null || signal.aborted) {
            return;
        }

        let errorMessage = 'Das Absenden des Formulars konnte nicht vorbereitet werden.';
        let stopLoading: (() => void) | undefined;
        let confirmedLayout = formLayout;
        let formSlug = configuredFormSlug;
        const showMissingSlug = () => confirm({
            title: 'URL-Segment fehlt',
            children: (
                <Typography>
                    Für dieses Formular fehlt ein URL-Segment.
                    Öffnen Sie die Eigenschaften des Formulareingangs und konfigurieren Sie das URL-Segment des Formulars.
                </Typography>
            ),
            confirmButtonText: 'Ok',
            hideCancelButton: true,
        });
        try {
            if (disableValidation) {
                const confirmProcess = await confirm({
                    title: 'Fortfahren ohne Validierungen',
                    children: (
                        <>
                            <Typography>
                                Sie haben die Validierungen für dieses Formular deaktiviert.
                                Es können ungültige oder fehlende Eingaben vorliegen, die zu Fehlern beim Absenden des
                                Formulars führen können.
                                Nur wenn alle Felder gültig sind, kann das Formular korrekt abgesendet werden.
                                Andernfalls wird die Übermittlung automatisch abgelehnt.
                            </Typography>
                            <Typography
                                sx={{
                                    mt: 2,
                                }}
                            >
                                Sind Sie sicher, dass Sie fortfahren möchten?
                            </Typography>
                        </>
                    ),
                    confirmButtonText: 'Ja, fortfahren',
                });
                signal.throwIfAborted();
                if (!confirmProcess) {
                    return;
                }
            }

            if (!formSlug) {
                await showMissingSlug();
                return;
            }

            // Check if changes exist. If so, ask for saving them.
            if (hasLayoutToSave) {
                const saveNow = await confirm({
                    title: 'Ungespeicherte Änderungen',
                    children: (
                        <Typography>
                            Sie haben aktuell ungespeicherte Änderungen.
                            Diese müssen gespeichert werden, damit sie beim Absenden des Formulars berücksichtigt
                            werden.
                            Sie können die Änderungen jetzt speichern.
                        </Typography>
                    ),
                    confirmButtonText: 'Jetzt speichern',
                });
                signal.throwIfAborted();
                if (!saveNow) {
                    dispatch(showWarningSnackbar('Das Absenden des Formulars wurde abgebrochen, da es ungespeicherte Änderungen gibt.'));
                    return;
                }

                errorMessage = 'Fehler beim Speichern der Änderungen';
                const saved = await saveForm();
                if (saved == null || signal.aborted) return;
                confirmedLayout = toEditableFormLayout(getStoredFormLayout(saved), formLayout.id);
                formSlug = getLiteralElementValue<string>(saved.configuration, 'formSlug');
                if (!formSlug) {
                    await showMissingSlug();
                    return;
                }
            }

            errorMessage = 'Der Testmodus für das Formular konnte nicht vorbereitet werden.';
            const testClaimApi = new ProcessTestClaimApiService();
            let testClaim = await testClaimApi
                .listAll({
                    processId: node.processId,
                    processVersion: node.processVersion,
                })
                .then(response => {
                    return response.content.length > 0 ? response.content[0] : null;
                });
            signal.throwIfAborted();

            if (testClaim == null) {
                const createTestClaim = await confirm({
                    title: 'Nicht im Testmodus',
                    children: (
                        <Typography>
                            Der Prozess, für den Sie das Formular absenden möchten, befindet
                            sich <strong>nicht</strong> im Testmodus.
                            Sie können den Prozess jetzt in den Testmodus versetzen, um das Formular absenden zu können.
                        </Typography>
                    ),
                    confirmButtonText: 'Testmodus starten',
                });
                signal.throwIfAborted();
                if (!createTestClaim) {
                    dispatch(showWarningSnackbar('Das Absenden des Formulars wurde abgebrochen, da der Prozess nicht im Testmodus ist.'));
                    return;
                }

                testClaim = await testClaimApi
                    .create({
                        ...testClaimApi.initialize(),
                        processId: node.processId,
                        processVersion: node.processVersion,
                    });
                signal.throwIfAborted();
                setTestClaim(testClaim);
                testClaimRef.current = testClaim;
            }

            errorMessage = 'Beim Berechnen der Kosten ist ein unbekannter Fehler aufgetreten.';
            const costs = await new FormTriggerApiService()
                .calculateCosts(process.slug, formSlug, values, {
                    testClaim: testClaim.accessKey,
                });
            signal.throwIfAborted();
            const paymentRequired = costs.totalCost > 0;

            if (paymentRequired) {
                const proceedWithPaymentRequirements = await confirm({
                    title: 'Zahlung erforderlich',
                    children: (
                        <PaymentRequestOverview
                            request={costs}
                        />
                    ),
                    confirmButtonText: 'Fortfahren',
                });
                signal.throwIfAborted();
                if (!proceedWithPaymentRequirements) {
                    dispatch(showWarningSnackbar('Das Absenden des Formulars wurde abgebrochen.'));
                    return;
                }
            }

            errorMessage = 'Die Anlagen konnten nicht für das Absenden vorbereitet werden.';
            const formData = new FormData();
            formData.append('inputs', JSON.stringify(values));

            const files: FileUploadElementItem[] = [];
            walkAuthoredElementValues(confirmedLayout, values, (element, value) => {
                if (element.type === ElementType.FileUpload && Array.isArray(value) && value.length > 0 && isFileUploadElementItem(value[0])) {
                    files.push(...value);
                }
            });

            for (const file of files) {
                const response = await fetch(file.uri, {signal});
                if (!response.ok) {
                    throw new Error(`Failed to load attachment ${file.name}`);
                }
                const blob = await response.blob();
                formData.append('files', blob, file.name);
                formData.append('fileUris', file.uri);
            }

            signal.throwIfAborted();
            stopLoading = showLoadingForVisit({
                blocking: true,
                estimatedTime: 1000,
                message: 'Formular wird abgesendet',
            }, signal);
            errorMessage = 'Beim Absenden des Formulars ist ein Fehler aufgetreten';

            const startRes = await new FormTriggerApiService()
                .submitForm(process.slug, formSlug, formData, {
                    testClaim: testClaim.accessKey,
                });
            signal.throwIfAborted();
            setStartedProcessAccessInfo({
                processInstanceAccessKey: startRes.startedProcessAccessKey,
                paymentRequired: paymentRequired,
            });
        } catch (err) {
            // Of the requests above, only saving the form layout is checked against a lock version.
            if (!signal.aborted) void showEditorSaveErrorSnackbar(err, errorMessage, backUpDraft).then(dispatch);
        } finally {
            stopLoading?.();
        }
    };

    return (
        <PageWrapper
            title={`${process.internalTitle} - ${node.name ?? 'Formulareingang'}`}
            fullWidth={true}
            fullHeight={true}
        >
            <Box
                ref={containerRef}
                sx={{
                    height: '100vh',
                    '--focus-border': (theme) => theme.palette.secondary.main,
                }}
            >
                <Allotment vertical>
                    <RootStructureActionsContextProvider
                        value={{
                            canAddAtRoot: checked && isAnyElementWithChildren(formLayout),
                            openAddAtRootDialog: () => {
                                if (canEdit()) setShowRootAddElementDialog(true);
                            },
                        }}
                    >
                        <Allotment>
                            <Allotment.Pane minSize={732}>
                                {/* Working Area */}
                                <Box
                                    sx={{
                                        display: 'flex',
                                        flexDirection: 'column',
                                        height: '100%',
                                        px: 2,
                                        pt: 2,
                                        overflow: 'hidden',
                                    }}
                                >
                                    <GenericPageHeader
                                        title={`${process.internalTitle} - ${node.name ?? 'Formulareingang'}`}
                                        icon={ModuleIcons.forms}
                                        actions={headerActions}
                                    />

                                    <Paper
                                        inert={checkingDraft}
                                        aria-busy={checkingDraft}
                                        sx={{
                                            overflowY: 'auto',
                                            flex: 1,
                                            mt: 2,
                                            minHeight: 0,
                                            borderTopLeftRadius: 10,
                                            borderTopRightRadius: 10,
                                            borderBottomLeftRadius: 0,
                                            borderBottomRightRadius: 0,
                                        }}
                                        ref={scrollContainerRef}
                                    >
                                        <ThemeProvider theme={previewTheme}>
                                            <Box
                                                sx={{
                                                    backgroundColor: getCustomerPageSurfaceColor,
                                                    color: 'text.primary',
                                                    minHeight: '100%',
                                                    display: 'flex',
                                                    flexDirection: 'column',
                                                }}
                                            >
                                                <FormHeaderComponent
                                                    form={formLayout}
                                                    version={processVersion}
                                                    logoUrl={formLogoUrl}
                                                    logoUrlDark={formLogoUrlDark}
                                                    onDeleteFormData={() => {
                                                        dispatch(setCurrentStep(0));
                                                        setAuthoredElementValues({});
                                                        setStartedProcessAccessInfo(null);
                                                        setIdentitySlots([]);
                                                        IdentityProvidersApiService.clearIdentity(node.id);
                                                    }}
                                                />

                                                {
                                                    startedProcessAccessInfo == null &&
                                                    <ElementTreeInlineEditorContextProvider
                                                        value={{
                                                            cloneElement: handleCloneElement,
                                                            deleteElement: handleDeleteElement,
                                                            navigateToElementEditor: handleOpenElement,
                                                            highlightElementInTree: handleHighlightElementInTree,
                                                            editable: checked,
                                                        }}
                                                    >
                                                        <ElementDerivationContext
                                                            ref={elementDerivationContextRef}
                                                            scrollContainerRef={scrollContainerRef}
                                                            element={formLayout}
                                                            authoredElementValues={authoredElementValues}
                                                            onAuthoredElementValuesChange={setAuthoredElementValues}
                                                            onEvent={handleSubmitEvent}
                                                            onDerivedDataChange={setDerivedData}
                                                            onDerive={(values, derivationOptions, abort) => new ProcessNodeApiService()
                                                                .deriveUiDefinition(
                                                                    node.id,
                                                                    FormLayoutFieldKey,
                                                                    // The backend only derives an unsaved form for users who may update the node.
                                                                    // Everyone else previews the stored form.
                                                                    isEditable && canUpdateProcess ? formLayout : null,
                                                                    values,
                                                                    derivationOptions,
                                                                    abort,
                                                                )}
                                                            mode={ViewDispatcherMode.Editor}
                                                            displayContext={ElementDisplayContext.CustomerFacing}
                                                            disableValidation={disableValidation}
                                                            disableVisibilities={disableVisibility}
                                                            highlightedElementId={hoveredTreeElementId}
                                                        />
                                                    </ElementTreeInlineEditorContextProvider>
                                                }
                                                {
                                                    startedProcessAccessInfo != null &&
                                                    <Submitted
                                                        startedProcessAccessKey={startedProcessAccessInfo.processInstanceAccessKey}
                                                        paymentRequired={startedProcessAccessInfo.paymentRequired}
                                                        formElement={formLayout}
                                                        processSlug={process.slug}
                                                        formSlug={configuredFormSlug ?? null}
                                                        processVersion={processVersion.processVersion}
                                                    />
                                                }

                                                <RootComponentFooter
                                                    form={formLayout}
                                                    version={processVersion}
                                                    logoUrl={formLogoUrl}
                                                    logoUrlDark={formLogoUrlDark}
                                                />

                                                <HelpDialog
                                                    onHide={() => dispatch(showDialog(undefined))}
                                                    open={metaDialog === HelpDialogId}
                                                    form={formLayout}
                                                    version={processVersion}
                                                />

                                                <PrivacyDialog
                                                    onHide={() => dispatch(showDialog(undefined))}
                                                    open={metaDialog === PrivacyDialogId}
                                                    form={formLayout}
                                                    version={processVersion}
                                                />

                                                <ImprintDialog
                                                    onHide={() => dispatch(showDialog(undefined))}
                                                    open={metaDialog === ImprintDialogId}
                                                    form={formLayout}
                                                    version={processVersion}
                                                />

                                                <AccessibilityDialog
                                                    onHide={() => dispatch(showDialog(undefined))}
                                                    open={metaDialog === AccessibilityDialogId}
                                                    form={formLayout}
                                                    version={processVersion}
                                                />
                                            </Box>
                                        </ThemeProvider>
                                    </Paper>
                                </Box>
                            </Allotment.Pane>
                            {/* Element Tree */}
                            {
                                (!hideComponentTree || panelDraft != null) && !checkingDraft &&
                                (
                                    <Allotment.Pane
                                        minSize={480}
                                        preferredSize={480}
                                    >
                                        <Paper
                                            sx={{
                                                boxShadow: '0px 4px 15px rgba(0, 0, 0, 0.1)',
                                                borderLeft: '1px solid',
                                                borderLeftColor: 'divider',
                                                borderRadius: 0,
                                                position: 'relative',
                                                height: '100%',
                                                overflow: 'hidden',
                                            }}
                                        >
                                            <ElementTree
                                                value={formLayout}
                                                panelDraft={panelDraft}
                                                onPanelDraftChange={(draft, reason) => {
                                                    if (!canEdit()) return;
                                                    if (reason !== 'normalize' && !editorValuesEqual(draft?.mine, currentPanel.current?.mine)) {
                                                        panelEditRevision.current++;
                                                    }
                                                    setPanelDraft(draft);
                                                }}
                                                onChange={handlePatch}
                                                editable={checked}
                                                displayContext={ElementDisplayContext.CustomerFacing}
                                                allowElementIdEditing={false}
                                                highlightElementId={highlightElementId}
                                                highlightElementSignal={highlightElementSignal}
                                                onHoveredElementIdChange={setHoveredTreeElementId}
                                                openRootAddElementSignal={openAddSectionSignal}
                                                identityMappingInformation={identityMappingInformation}
                                                configurationErrors={configurationErrors}
                                            />
                                        </Paper>
                                    </Allotment.Pane>
                                )}

                        </Allotment>
                    </RootStructureActionsContextProvider>

                    {showDeveloperTools !== undefined && (
                        <Allotment.Pane
                            minSize={developerToolsMinHeight}
                            maxSize={developerToolsMaxHeight}
                            preferredSize={420}
                        >
                            <Box
                                inert={checkingDraft}
                                sx={{
                                    height: '100%',
                                    overflow: 'hidden',
                                }}
                            >
                                <DeveloperTools
                                    dataLabel={node.name ?? ''}
                                    rootElement={formLayout!}
                                    elementData={authoredElementValues}
                                    onElementDataChange={(elementData) => {
                                        void elementDerivationContextRef.current?.replaceAuthoredElementValues(elementData);
                                    }}
                                    derivedData={derivedData}
                                />
                            </Box>

                        </Allotment.Pane>
                    )}

                </Allotment>
            </Box>

            <FormDetailsPageMoreMenu
                anchorEl={checkingDraft ? null : showMoreMenuAtEl}
                onClose={() => {
                    setShowMoreMenuAtEl(null);
                }}
                items={moreMenuItems}
            />

            <PrefillFormDialog
                form={formLayout}
                open={showPrefillDialog}
                onClose={() => {
                    setShowPrefillDialog(false);
                }}
            />

            <AddElementDialog
                show={checked && showRootAddElementDialog}
                parentType={formLayout.type}
                parentElement={formLayout}
                allParents={[formLayout]}
                onAddElements={(elements) => {
                    if (isAnyElementWithChildren(formLayout)) {
                        handlePatch({
                            ...formLayout,
                            children: [
                                ...(formLayout.children ?? []) as any[],
                                ...elements,
                            ],
                        });
                    }
                    setShowRootAddElementDialog(false);
                }}
                onClose={() => {
                    setShowRootAddElementDialog(false);
                }}
                displayContext={ElementDisplayContext.CustomerFacing}
            />

            <Dialog
                open={showIdentityDialog}
                onClose={closeIdentityDialog}
                fullWidth={true}
                maxWidth="md"
            >
                <DialogTitleWithClose onClose={closeIdentityDialog}>
                    Identitäten für den Formulartest auswählen
                </DialogTitleWithClose>
                <DialogContent>
                    <Typography
                        variant="body2"
                        component="div"
                        sx={{
                            maxWidth: 600
                        }}
                    >
                        Diese Auswahl dient nur zum Testen im Formulareditor. Sie können Identitäten und den zugehörigen
                        Kommunikationsweg festlegen; Nutzer:innen sehen später die normale Identitätsauswahl des
                        Formulars und nicht diesen Dialog.
                    </Typography>
                    <Typography
                        variant="body2"
                        sx={{
                            color: "text.secondary",
                            maxWidth: 600,
                            marginTop: 2,
                            marginBottom: 4
                        }}>
                        Um die Auswahl vollständig zurückzusetzen, können Sie das formularspezifische
                        Drei-Punkte-Menü verwenden und {quoteString('Eingaben verwerfen')} auswählen.
                    </Typography>

                    {isLoadingIdentitySlots &&
                        <Box sx={{display: 'flex', justifyContent: 'center', py: 6}}>
                            <CircularProgress size={32}/>
                        </Box>}

                    {!isLoadingIdentitySlots && identitySlotsLoadFailed &&
                        <Alert severity="error">
                            Die Identitäten für den Formulartest konnten nicht geladen werden. Schließen Sie den Dialog
                            und versuchen Sie es erneut.
                        </Alert>}

                    {!isLoadingIdentitySlots && !identitySlotsLoadFailed && sortedIdentitySlots.length === 0 &&
                        <Alert severity="info">
                            Für diesen Formulartest sind keine Identitäten konfiguriert.
                        </Alert>}

                    {!isLoadingIdentitySlots && !identitySlotsLoadFailed && sortedIdentitySlots.map((slot) => (
                        <Box
                            key={slot.id}
                            sx={{mb: 4}}
                        >
                            <Box>
                                <Typography variant="caption">
                                    Identität
                                </Typography>
                                <Box
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        flexWrap: 'wrap',
                                        columnGap: 1.25,
                                        rowGap: 0.5,
                                        mt: 0.25,
                                    }}
                                >
                                    <Typography
                                        variant="h5"
                                        component="h3"
                                    >
                                        {getIdentityDisplayName(slot)}
                                    </Typography>

                                    <Chip
                                        mode="soft"
                                        label={slot.isRequired ? 'Verpflichtend' : 'Optional'}
                                        color={slot.isRequired ? 'warning' : 'info'}
                                        size="small"
                                    />
                                </Box>
                            </Box>

                            {slot.description != null && slot.description.trim().length > 0 &&
                                <RichtextComponent
                                    content={slot.description}
                                    sx={{mt: 2}}
                                />}

                            {
                                identitySelectionApi != null &&
                                <FormIdentitySelectionControls
                                    slot={slot}
                                    api={identitySelectionApi}
                                    onChange={(nextSlot) => {
                                        setIdentitySlots(currentSlots => currentSlots.map(currentSlot => (
                                            currentSlot.id === nextSlot.id ? nextSlot : currentSlot
                                        )));
                                    }}
                                />
                            }
                        </Box>
                    ))}
                </DialogContent>
            </Dialog>

            {changeBlockerDialog}
            {mergeConflictDialog}
        </PageWrapper>
    );
}
