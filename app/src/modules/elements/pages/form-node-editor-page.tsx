import {Alert, Box, CircularProgress, Dialog, DialogContent, Paper, ThemeProvider, Typography, useTheme} from '@mui/material';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {showDialog} from '../../../slices/app-slice';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {
    selectDevToolsTab,
    setDevToolsTab,
    toggleAutoScrollForSteps,
    toggleComponentTree,
    toggleElementContextMenu,
} from '../../../slices/admin-settings-slice';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ElementTree} from '../../../components/element-tree-2/element-tree';
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
} from '../../../models/element-data';
import {RootState} from '../../../store.staff';
import {PageWrapper} from '../../../components/page-wrapper/page-wrapper';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {GenericPageHeader} from '../../../components/generic-page-header/generic-page-header';
import {Allotment} from 'allotment';
import {useElementSize} from '../../../utils/element-size';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {PrefillFormDialog} from '../../../dialogs/prefill-form-dialog/prefill-form-dialog';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import QrCode from '@aivot/mui-material-symbols-400-n25-outlined/QrCode';
import MoreVert from '@aivot/mui-material-symbols-400-n25-outlined/MoreVert';
import Settings from '@aivot/mui-material-symbols-400-n25-outlined/Settings';
import {type Action} from '../../../components/actions/actions-props';
import {useElementEditorNavigation} from '../../../hooks/use-element-editor-navigation';
import Link from '@aivot/mui-material-symbols-400-n25-outlined/Link';
import Contract from '@aivot/mui-material-symbols-400-n25-outlined/Contract';
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
import {ElementType} from '../../../data/element-type/element-type';
import {getSingleUseSectionAddDisabledReason} from '../../../data/element-type/single-use-section-types';
import {generateElementWithDefaultValues} from '../../../utils/generate-element-with-default-values';
import {
    FormDetailsPageMoreMenu,
    FormDetailsPageMoreMenuItem,
} from '../../forms/pages/details/components/form-details-page-more-menu';
import {ElementDerivationContext} from '../components/element-derivation-context';
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
import {createApiPath, createCustomerPath} from '../../../utils/url-path-utils';
import {ProcessTestClaimEntity} from '../../process/entities/process-test-claim-entity';
import {downloadQrCode} from '../../../utils/download-qrcode';
import {downloadBlobFile, uploadTextFile} from '../../../utils/download-utils';
import {useNotImplemented} from '../../../hooks/use-not-implemented';
import {ViewDispatcherMode} from '../../../components/view-dispatcher/view-dispatcher.context';
import {ProcessStatus} from '../../process/enums/process-status';
import type {Theme as AppTheme} from '../../themes/models/theme';
import {FormTriggerApiService, type FormIdentitySlot} from '../../forms/services/form-trigger-api-service';
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
import {SearchItemService} from '../../search/search-item-service';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {
    FormIdentitySelectionControls,
} from '../../identity/components/form-identity-selection-controls/form-identity-selection-controls';
import {normalizeUiDefinitionForStorage} from '../../../utils/ui-definition-utils';
import {useApi} from '../../../hooks/use-api';
import {ThemesApiService} from '../../themes/themes-api-service';
import {AssetsApiService} from '../../assets/assets-api-service';
import {VDepartmentShadowedApiService} from '../../departments/services/v-department-shadowed-api-service';
import {Chip} from '../../../components/chip/chip';
import {quoteString} from '../../../utils/string-utils';
import {PaymentRequestOverview} from '../../payment/components/payment-request-overview';
import {isApiError} from '../../../models/api-error';
import {resolveThemeChainLogoKey} from '../../../theming/resolve-theme-logo';
import {RichtextComponent} from '../../../components/richtext/richtext.component';

export const DialogSearchParam = 'dialog';

const FormLayoutFieldKey = 'formLayout';
const IdentitiesFieldKey = 'identities';
const PrintablePdfFallbackFilenameBase = 'Formulareingang';
const PrintablePdfFilenameBaseMaxLength = 120;
const EditorLoadErrorMessage = 'Der Formulareditor konnte nicht geladen werden.';

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
    return JSON.parse(JSON.stringify(element)) as T;
}

function sanitizePrintablePdfFilenameBase(value: string): string {
    return value
        .replace(/\.pdf$/i, '')
        .replace(/[<>:"/\\|?*]/g, '')
        .split('')
        .filter((char) => char.charCodeAt(0) >= 32)
        .join('')
        .replace(/\s+/g, ' ')
        .trim()
        .replace(/\.+$/g, '')
        .slice(0, PrintablePdfFilenameBaseMaxLength)
        .trim();
}

function resolvePrintablePdfFilename(layout: FormLayoutElement | null, node: ProcessNodeEntity): string {
    const candidates = [
        layout?.publicTitle,
        node.name,
        PrintablePdfFallbackFilenameBase,
    ];


    for (const candidate of candidates) {
        if (typeof candidate !== 'string') {
            continue;
        }

        console.log(layout, candidates);

        const filenameBase = sanitizePrintablePdfFilenameBase(candidate);
        if (filenameBase.length > 0) {
            return `${filenameBase}.pdf`;
        }
    }

    return `${PrintablePdfFallbackFilenameBase}.pdf`;
}

export function FormNodeEditorPage() {
    const {
        nodeId = '',
    } = useParams<{
        nodeId: string;
        fieldKey: string;
        elementType: string;
    }>();

    const navigate = useNavigate();
    const dispatch = useAppDispatch();
    const confirm = useConfirm();
    const api = useApi();

    const outerTheme = useTheme();

    const [showRootAddElementDialog, setShowRootAddElementDialog] = useState(false);

    const [node, setNode] = useState<ProcessNodeEntity | null>(null);
    const [formLayout, setFormLayout] = useState<FormLayoutElement | null>(null);

    const [process, setProcess] = useState<ProcessEntity | null>(null);
    const [processVersion, setProcessVersion] = useState<ProcessVersionEntity | null>(null);
    const [testClaim, setTestClaim] = useState<ProcessTestClaimEntity | null>(null);
    const testClaimRef = useRef<ProcessTestClaimEntity | null>(null);
    const [formTheme, setFormTheme] = useState<AppTheme>();
    const [draftPreviewThemeChain, setDraftPreviewThemeChain] = useState<AppTheme[] | null>(null);

    const [identityMappingInformation, setIdentityMappingInformation] = useState<IdentityConfigElementSlotWithProviders[]>([]);
    const [showIdentityDialog, setShowIdentityDialog] = useState(false);
    const [identitySlots, setIdentitySlots] = useState<FormIdentitySlot[]>([]);
    const [isLoadingIdentitySlots, setIsLoadingIdentitySlots] = useState(false);
    const [identitySlotsLoadFailed, setIdentitySlotsLoadFailed] = useState(false);
    const configuredIdentitySlots = useMemo(() => (
        node?.configuration[IdentitiesFieldKey] as IdentityConfigElementSlot[] | null | undefined
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

    const [startedProcessAccessInfo, setStartedProcessAccessInfo] = useState<{
        processInstanceAccessKey: string;
        paymentRequired: boolean;
    } | null>(null);

    const {
        dialog: changeBlockerDialog,
        hasChanged,
    } = useChangeBlocker({
        original: normalizeUiDefinitionForStorage(node?.configuration[FormLayoutFieldKey] as FormLayoutElement | null | undefined),
        edited: normalizeUiDefinitionForStorage(formLayout),
    });

    useEffect(() => {
        if (node == null) {
            return;
        }
        new SearchItemService()
            .recordRecentSearchItem({
                id: node.id.toString(),
                originTable: ServerEntityType.ProcessNodes,
            })
            .catch(() => {
            });
    }, [node]);

    useEffect(() => {
        if (node == null) {
            return;
        }

        new IdentityProvidersApiService()
            .listAll()
            .then((page) => {
                const mappedIdentities = node.configuration[IdentitiesFieldKey] as IdentityConfigElementSlot[] | null | undefined;

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

        const formSlug = node?.configuration.formSlug;
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
        const nodeIdInt = parseInt(nodeId);
        dispatch(setCurrentStep(0));
        new ProcessNodeApiService()
            .retrieve(nodeIdInt)
            .then((node) => {
                let uiElement = node.configuration[FormLayoutFieldKey];
                if (uiElement == null) {
                    uiElement = generateElementWithDefaultValues(ElementType.FormLayout);
                }

                setPastLoadedForm([]);
                setFutureLoadedForm([]);
                setNode(node);
                setFormLayout(cloneFormLayoutSnapshot(uiElement as FormLayoutElement));
            })
            .catch((err) => {
                dispatch(setErrorMessage(createEditorLoadError(err)));
            });
    }, [dispatch, nodeId]);

    useEffect(() => {
        if (node == null) {
            setProcess(null);
            setProcessVersion(null);
            setTestClaim(null);
            testClaimRef.current = null;
            return;
        }

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
                dispatch(setErrorMessage(createEditorLoadError(err)));
            });
    }, [node]);

    useEffect(() => {
        if (process == null || processVersion == null || node?.configuration.formSlug == null) {
            setFormTheme(undefined);
            return;
        }

        let isCancelled = false;

        new FormTriggerApiService()
            .getFormTheme(
                process.slug,
                node.configuration.formSlug,
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
    }, [node, process, processVersion, testClaim]);

    const hasFormLayout = formLayout != null;
    const selectedProcessVersionThemeId = processVersion?.themeId ?? null;
    const selectedResponsibleDepartmentId = formLayout?.responsibleDepartmentId ?? null;
    const selectedManagingDepartmentId = formLayout?.managingDepartmentId ?? null;

    useEffect(() => {
        if (!hasFormLayout) {
            setDraftPreviewThemeChain(null);
            return;
        }

        let isCancelled = false;

        // The public theme endpoint resolves persisted data only. Resolve draft department changes here
        // so the preview reflects unsaved form configuration immediately.
        setDraftPreviewThemeChain([]);

        const themesApi = new ThemesApiService(api);
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
    ]);

    const [searchParams] = useSearchParams();
    const metaDialogName = useMemo(() => searchParams.get(DialogSearchParam), [searchParams]);

    const {
        navigateToElementEditor,
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

    const isEditable = processVersion?.status === ProcessStatus.Drafted;
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
        if (formLayout == null || pastLoadedForm.length === 0) {
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
        if (formLayout == null || futureLoadedForm.length === 0) {
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

    const saveForm = async (): Promise<void> => {
        if (node == null || formLayout == null || !isEditable) {
            return;
        }

        const formLayoutForStorage = normalizeUiDefinitionForStorage(formLayout);

        const updated = await new ProcessNodeApiService()
            .update(node.id, {
                ...node,
                configuration: {
                    ...node.configuration,
                    [FormLayoutFieldKey]: formLayoutForStorage,
                },
            }, {
                query: {
                    onlyConfigSave: FormLayoutFieldKey,
                },
            });

        setNode(updated);
        setFormLayout(
            updated.configuration[FormLayoutFieldKey] ??
            generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement,
        );
    };

    const handleSave = async (): Promise<void> => {
        try {
            await saveForm();
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Fehler beim Speichern der Änderungen'));
        }
    };

    const onBackToProcess = () => {
        if (node == null) {
            return;
        }

        navigate(`/processes/${node.processId}/versions/${node.processVersion}/nodes/${node.id}`);
    };

    const scrollContainerRef = useRef<HTMLDivElement>(null);

    const publicFormLink = createCustomerPath(`/form/${process?.slug}/${node?.configuration.formSlug}${testClaim != null ? `?test-claim=${testClaim.accessKey}` : ''}`);

    const handleImportFromXDF = async () => {
        if (!isEditable) {
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
                            Beim Import werden ggf. bereits bestehende Formularfelder im Editor durch die importierte
                            Struktur ersetzt.
                            Möchten Sie den Vorgang wirklich fortsetzen?
                        </Typography>
                    </>
                ),
                confirmButtonText: 'Ja, mit dem Import fortfahren',
            });

            if (!conf) {
                return;
            }

            xmlContent = await uploadTextFile('text/xml');

            if (xmlContent == null) {
                return;
            }
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Beim Importieren des XDatenfeld-Schemas ist ein Fehler aufgetreten.'));
            return;
        }

        dispatch(setLoadingMessage({
            blocking: true,
            estimatedTime: 1000,
            message: 'Importiere XDF',
        }));

        try {
            const transformed = await new XdfApiService().xdfTransform(xmlContent);

            setFormLayout(transformed);
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Beim Importieren des XDatenfeld-Schemas ist ein Fehler aufgetreten.'));
        } finally {
            dispatch(clearLoadingMessage());
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

    const downloadPersistedPdfFile = async (filenameLayout: FormLayoutElement | null): Promise<void> => {
        if (node == null) {
            return;
        }

        const filename = resolvePrintablePdfFilename(filenameLayout, node);

        dispatch(setLoadingMessage({
            blocking: false,
            estimatedTime: 1500,
            message: 'Vordruck wird generiert',
        }));

        try {
            const blob = await new FormTriggerApiService()
                .downloadPrintablePdf(node.id);

            downloadBlobFile(filename, blob);
            dispatch(showSuccessSnackbar('Der Vordruck wurde erfolgreich erstellt und der Download gestartet.'));
        } catch (err) {
            console.error(err);
            dispatch(showApiErrorSnackbar(err, 'Fehler beim Generieren des Vordrucks'));
        } finally {
            dispatch(clearLoadingMessage());
        }
    };

    const handleDownloadPdfFile = async (): Promise<void> => {
        if (node == null) {
            return;
        }

        let filenameLayout = formLayout;

        if (hasChanged) {
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

            if (saveNow) {
                try {
                    await saveForm();
                } catch (err) {
                    console.error(err);
                    dispatch(showApiErrorSnackbar(err, 'Fehler beim Speichern der Änderungen'));
                    return;
                }
            } else {
                return;
            }
        }

        await downloadPersistedPdfFile(filenameLayout);
    };

    const handlePatch = (element: FormLayoutElement) => {
        if (!isEditable) {
            return;
        }

        if (formLayout != null) {
            setPastLoadedForm((currentPastLoadedForm) => [
                ...currentPastLoadedForm,
                cloneFormLayoutSnapshot(formLayout),
            ]);
        }
        setFutureLoadedForm([]);
        setFormLayout(cloneFormLayoutSnapshot(element));
    };

    const handleCloneElement = (element: AnyElement) => {
        if (formLayout == null) {
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
        if (formLayout == null) {
            return;
        }

        confirm({
            title: 'Element wirklich löschen',
            children: (
                <Typography>
                    Wollen Sie das Element <strong>{generateComponentTitle(element)}</strong> wirklich löschen?
                </Typography>
            ),
        })
            .then((conf) => {
                if (!conf) {
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

                handlePatch(deleteElementRecursive(formLayout));
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
            icon: <Contract/>,
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
            disabled: !isEditable,
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
            disabled: !hasPastLoadedForm || !isEditable,
        },
        {
            tooltip: 'Änderung wiederherstellen',
            icon: <RedoIcon/>,
            onClick: handleRedo,
            disabled: !hasFutureLoadedForm || !isEditable,
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
            tooltip: 'Formular-Einstellungen öffnen',
            icon: <Settings/>,
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
            onClick: (event: React.MouseEvent<HTMLButtonElement>) => {
                setShowMoreMenuAtEl(event.currentTarget);
            },
        },
        'separator' as const,
        {
            label: 'Zurück zum Prozess',
            onClick: onBackToProcess,
            variant: 'text' as const,
        },
        {
            label: 'Speichern',
            tooltip: 'Änderungen am Formular speichern',
            icon: <Save/>,
            iconPosition: 'start' as const,
            onClick: handleSave,
            variant: 'contained' as const,
            disabled: !hasChanged || !isEditable,
        },
    ];

    if (formLayout == null || node == null || process == null || processVersion == null) {
        return;
    }

    const formAssetQueryParams = new URLSearchParams({
        version: processVersion.processVersion.toString(),
    });
    formAssetQueryParams.set('theme-id', processVersion.themeId?.toString() ?? 'default');
    if (testClaim != null) {
        formAssetQueryParams.set('test-claim', testClaim.accessKey);
    }

    // Use the locally resolved draft chain for logos as well. The public form logo endpoint is based
    // on the persisted form and the system logo should only appear when no custom theme is resolved.
    const resolveDraftLogoUrl = (colorScheme: 'light' | 'dark'): string | null => {
        if (draftPreviewThemeChain == null) {
            const queryParams = new URLSearchParams(formAssetQueryParams);
            if (colorScheme === 'dark') {
                queryParams.set('color-scheme', 'dark');
            }
            return `/api/public/form/${process.slug}/${node.configuration.formSlug}/logo/?${queryParams.toString()}`;
        }

        if (draftPreviewThemeChain.length === 0) {
            return createApiPath(
                `/api/public/system/logo/${colorScheme === 'dark' ? '?color-scheme=dark' : ''}`,
            );
        }

        const logoKey = resolveThemeChainLogoKey(draftPreviewThemeChain, colorScheme);
        return logoKey == null ? null : AssetsApiService.useAssetLink(logoKey);
    };
    const formLogoUrl = resolveDraftLogoUrl('light');
    const formLogoUrlDark = resolveDraftLogoUrl('dark');

    const handleSubmitEvent = async (values: AuthoredElementValues, event: string): Promise<void> => {
        if (event != SUBMIT_EVENT) {
            return;
        }

        let errorMessage = 'Das Absenden des Formulars konnte nicht vorbereitet werden.';
        let loadingStarted = false;
        try {
            if (disableValidation) {
                const confirmProcess = await confirm({
                    title: 'Fortfahren ohne Validierungen',
                    children: (
                        <>
                            <Typography>
                                Sie haben die Validierungen für dieses Formular deaktiviert.
                                Es können ungültige oder fehlenden Eingaben vorliegen, die zu Fehlern beim Absenden des
                                Formulars führen können.
                                Nur wenn alle Felder gültig sind, kann das Formular korrekt abgesendet werden.
                                Andernfalls wird die Einreichung automatisch abgelehnt.
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

                if (!confirmProcess) {
                    return;
                }
            }

            // Check if a slug is configured and break if no slug is present because we cannot submit data without a slug
            if (node.configuration.formSlug == null || node.configuration.formSlug === '') {
                await confirm({
                    title: 'Keine Formular-URL vergeben',
                    children: (
                        <Typography>
                            Sie haben für dieses Formular keine Formular-URL konfiguriert.
                            Öffnen Sie die Eigenschaften des entsprechenden Formular-Eingangselements und konfigurieren
                            Sie eine Formular-URL, damit dieses Formular abgesendet werden kann.
                        </Typography>
                    ),
                    confirmButtonText: 'Ok',
                    hideCancelButton: true,
                });
                return;
            }

            // Check if changes exist. If so, ask for saving them.
            if (hasChanged) {
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

                if (!saveNow) {
                    dispatch(showWarningSnackbar('Das Absenden des Formulars wurde abgebrochen, da es ungespeicherte Änderungen gibt.'));
                    return;
                }

                errorMessage = 'Fehler beim Speichern der Änderungen';
                await saveForm();
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

            if (testClaim == null) {
                const createTestClaim = await confirm({
                    title: 'Nicht im Test-Modus',
                    children: (
                        <Typography>
                            Der Prozess, für den Sie das Formular absenden möchten, befindet
                            sich <strong>nicht</strong> im Testmodus.
                            Sie können den Prozess jetzt in den Testmodus versetzen, um das Formular absenden zu können.
                        </Typography>
                    ),
                    confirmButtonText: 'Testmodus starten',
                });

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
                setTestClaim(testClaim);
                testClaimRef.current = testClaim;
            }

            errorMessage = 'Beim Berechnen der Kosten ist ein unbekannter Fehler aufgetreten.';
            const costs = await new FormTriggerApiService()
                .calculateCosts(process.slug, node.configuration.formSlug, values, {
                    testClaim: testClaim.accessKey,
                });
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

                if (!proceedWithPaymentRequirements) {
                    dispatch(showWarningSnackbar('Das Absenden des Formulars wurde abgebrochen.'));
                    return;
                }
            }

            errorMessage = 'Die Anhänge konnten nicht für das Absenden vorbereitet werden.';
            const formData = new FormData();
            formData.append('inputs', JSON.stringify(values));

            const files: FileUploadElementItem[] = [];
            walkAuthoredElementValues(formLayout, values, (element, value) => {
                if (element.type === ElementType.FileUpload && Array.isArray(value) && value.length > 0 && isFileUploadElementItem(value[0])) {
                    files.push(...value);
                }
            });

            for (const file of files) {
                const response = await fetch(file.uri);
                if (!response.ok) {
                    throw new Error(`Failed to load attachment ${file.name}`);
                }
                const blob = await response.blob();
                formData.append('files', blob, file.name);
                formData.append('fileUris', file.uri);
            }

            dispatch(setLoadingMessage({
                blocking: true,
                estimatedTime: 1000,
                message: 'Formular wird abgesendet',
            }));
            loadingStarted = true;
            errorMessage = 'Beim Absenden des Formulars ist ein Fehler aufgetreten';

            const startRes = await new FormTriggerApiService()
                .submitForm(process.slug, node.configuration.formSlug, formData, {
                    testClaim: testClaim.accessKey,
                });

            setStartedProcessAccessInfo({
                processInstanceAccessKey: startRes.startedProcessAccessKey,
                paymentRequired: paymentRequired,
            });
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, errorMessage));
        } finally {
            if (loadingStarted) {
                dispatch(clearLoadingMessage());
            }
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
                            canAddAtRoot: isAnyElementWithChildren(formLayout),
                            openAddAtRootDialog: () => {
                                setShowRootAddElementDialog(true);
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
                                            <Box>
                                                <FormHeaderComponent
                                                    form={formLayout}
                                                    node={node}
                                                    process={process}
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
                                                            editable: isEditable,
                                                        }}
                                                    >
                                                        <ElementDerivationContext
                                                            element={formLayout}
                                                            authoredElementValues={authoredElementValues}
                                                            onAuthoredElementValuesChange={setAuthoredElementValues}
                                                            onEvent={handleSubmitEvent}
                                                            onDerivedDataChange={setDerivedData}
                                                            mode={ViewDispatcherMode.Editor}
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
                                                        node={node}
                                                        process={process}
                                                        version={processVersion}
                                                    />
                                                }

                                                <RootComponentFooter
                                                    form={formLayout}
                                                    node={node}
                                                    process={process}
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
                                !hideComponentTree &&
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
                                                onChange={handlePatch}
                                                editable={isEditable}
                                                displayContext={ElementDisplayContext.CitizenFacing}
                                                allowElementIdEditing={false}
                                                highlightElementId={highlightElementId}
                                                highlightElementSignal={highlightElementSignal}
                                                onHoveredElementIdChange={setHoveredTreeElementId}
                                                openRootAddElementSignal={openAddSectionSignal}
                                                identityMappingInformation={identityMappingInformation}
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
                                        /*dispatch(setLoadingMessage({
                                            message: 'Element-Daten werden importiert',
                                            blocking: true,
                                            estimatedTime: 500,
                                        }));
                                         */

                                        setAuthoredElementValues(elementData);
                                        /*
                                        withDelay(
                                            formService
                                                .deriveForm(
                                                    loadedForm.form.slug,
                                                    loadedForm.version.version,
                                                    elementData,
                                                    {
                                                        skipErrorsFor: ['ALL'],
                                                        skipVisibilitiesFor: disableVisibility ? ['ALL'] : [],
                                                        skipValuesFor: [],
                                                        skipOverridesFor: [],
                                                    },
                                                ), 500)
                                            .then((state) => {
                                                setAuthoredElementValues(elementData);
                                                setDerivedData(state.elementData);
                                                dispatch(addDerivationLogItems(state.logItems));
                                            })
                                            .finally(() => {
                                                dispatch(setLoadingMessage(undefined));
                                            });

                                         */
                                    }}
                                    derivedData={derivedData}
                                />
                            </Box>

                        </Allotment.Pane>
                    )}

                </Allotment>
            </Box>

            <FormDetailsPageMoreMenu
                anchorEl={showMoreMenuAtEl}
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
                show={showRootAddElementDialog}
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
                displayContext={ElementDisplayContext.CitizenFacing}
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
                        Drei-Punkte-Menü verwenden und {quoteString('Alle Formulardaten löschen')} auswählen.
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

                            <FormIdentitySelectionControls
                                slot={slot}
                                processSlug={process.slug}
                                formSlug={node.configuration.formSlug}
                                relatedProcessNodeId={node.id}
                                testClaim={testClaim?.accessKey}
                                onChange={(nextSlot) => {
                                    setIdentitySlots(currentSlots => currentSlots.map(currentSlot => (
                                        currentSlot.id === nextSlot.id ? nextSlot : currentSlot
                                    )));
                                }}
                            />
                        </Box>
                    ))}
                </DialogContent>
            </Dialog>

            {changeBlockerDialog}
        </PageWrapper>
    );
}
