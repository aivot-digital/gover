import {Box, Dialog, DialogContent, Paper, ThemeProvider, Typography, useTheme} from '@mui/material';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {showDialog} from '../../../slices/app-slice';
import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import {
    selectDevToolsTab,
    setDevToolsTab,
    toggleAutoScrollForSteps,
    toggleComponentTree,
    toggleElementContextMenu,
    toggleValidation,
    toggleVisibility,
} from '../../../slices/admin-settings-slice';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {ElementTree} from '../../../components/element-tree-2/element-tree';
import {HelpDialog, HelpDialogId} from '../../../dialogs/help-dialog/help.dialog';
import {PrivacyDialog, PrivacyDialogId} from '../../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog, AccessibilityDialogId} from '../../../dialogs/accessibility-dialog/accessibility-dialog';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import RemoveDoneOutlinedIcon from '@mui/icons-material/RemoveDoneOutlined';
import DoneAllOutlinedIcon from '@mui/icons-material/DoneAllOutlined';
import {
    showApiErrorSnackbar,
    showErrorSnackbar,
    showSuccessSnackbar,
    showWarningSnackbar,
} from '../../../slices/snackbar-slice';
import UndoIcon from '@mui/icons-material/Undo';
import RedoIcon from '@mui/icons-material/Redo';
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
import {clearLoadingMessage, setLoadingMessage} from '../../../slices/shell-slice';
import {PrefillFormDialog} from '../../../dialogs/prefill-form-dialog/prefill-form-dialog';
import {copyToClipboardText} from '../../../utils/copy-to-clipboard';
import OpenInNew from '@aivot/mui-material-symbols-400-outlined/dist/open-in-new/OpenInNew';
import QrCode from '@aivot/mui-material-symbols-400-outlined/dist/qr-code/QrCode';
import MoreVert from '@aivot/mui-material-symbols-400-outlined/dist/more-vert/MoreVert';
import Settings from '@aivot/mui-material-symbols-400-outlined/dist/settings/Settings';
import {type Action} from '../../../components/actions/actions-props';
import {useElementEditorNavigation} from '../../../hooks/use-element-editor-navigation';
import Preview from '@aivot/mui-material-symbols-400-outlined/dist/preview/Preview';
import Link from '@aivot/mui-material-symbols-400-outlined/dist/link/Link';
import Contract from '@aivot/mui-material-symbols-400-outlined/dist/contract/Contract';
import Draw from '@aivot/mui-material-symbols-400-outlined/dist/draw/Draw';
import AccountTree from '@aivot/mui-material-symbols-400-outlined/dist/account-tree/AccountTree';
import SwipeVertical from '@aivot/mui-material-symbols-400-outlined/dist/swipe-vertical/SwipeVertical';
import TouchApp from '@aivot/mui-material-symbols-400-outlined/dist/touch-app/TouchApp';
import BugReport from '@aivot/mui-material-symbols-400-outlined/dist/bug-report/BugReport';
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
import {BaseApiService} from '../../../services/base-api-service';
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
import type {Theme as AppTheme} from '../../themes/models/theme';
import {FormTriggerApiService} from '../../forms/services/form-trigger-api-service';
import {createAppTheme} from '../../../theming/themes';
import {BaseTheme} from '../../../theming/base-theme';
import {addEntityHistoryItem} from '../../../slices/entity-history-slice';
import {ServerEntityType} from '../../../shells/staff/data/server-entity-type';
import {XdfApiService} from '../../xdf/v1/xdf-api-service';
import Code from '@aivot/mui-material-symbols-400-outlined/dist/code/Code';
import {IdentityProvidersApiService} from '../../identity/identity-providers-api-service';
import {
    IdentityConfigElementOptionWithProvider,
    IdentityConfigElementSlot,
    IdentityConfigElementSlotWithProviders,
} from '../../../models/elements/form/input/identity-config-element';
import IdentityPlatform from '@aivot/mui-material-symbols-400-outlined/dist/identity-platform/IdentityPlatform';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {IdentityButton} from '../../identity/components/identity-button/identity-button';
import {normalizeUiDefinitionForStorage} from '../../../utils/ui-definition-utils';

export const DialogSearchParam = 'dialog';

const FormLayoutFieldKey = 'formLayout';
const IdentitiesFieldKey = 'identities';

function cloneFormLayoutSnapshot<T extends FormLayoutElement>(element: T): T {
    return JSON.parse(JSON.stringify(element)) as T;
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

    const outerTheme = useTheme();

    const [showRootAddElementDialog, setShowRootAddElementDialog] = useState(false);

    const [node, setNode] = useState<ProcessNodeEntity | null>(null);
    const [formLayout, setFormLayout] = useState<FormLayoutElement | null>(null);

    const [process, setProcess] = useState<ProcessEntity | null>(null);
    const [processVersion, setProcessVersion] = useState<ProcessVersionEntity | null>(null);
    const [testClaim, setTestClaim] = useState<ProcessTestClaimEntity | null>(null);
    const testClaimRef = useRef<ProcessTestClaimEntity | null>(null);
    const [formTheme, setFormTheme] = useState<AppTheme>();

    const [identityMappingInformation, setIdentityMappingInformation] = useState<IdentityConfigElementSlotWithProviders[]>([]);
    const [showIdentityDialog, setShowIdentityDialog] = useState(false);

    const [startedProcessAccessKey, setStartedProcessAccessKey] = useState<string | null>(null);

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
        dispatch(addEntityHistoryItem({
            link: location.pathname,
            title: node.name ?? 'Formular',
            type: ServerEntityType.ProcessNodes,
        }));
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
                dispatch(showApiErrorSnackbar(err, 'Die UI-Definition konnte nicht geladen werden.'));
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

    const {
        disableVisibility,
        disableValidation,
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
        if (formTheme == null) {
            return outerTheme;
        }

        return createAppTheme(formTheme, BaseTheme);
    }, [formTheme, outerTheme]);

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

    const handleSave = () => {
        if (node == null || formLayout == null) {
            return;
        }

        const formLayoutForStorage = normalizeUiDefinitionForStorage(formLayout);

        return new ProcessNodeApiService()
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
            })
            .then((updated) => {
                setNode(updated);
                setFormLayout(
                    updated.configuration[FormLayoutFieldKey] ??
                    generateElementWithDefaultValues(ElementType.FormLayout) as FormLayoutElement
                );
            });
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
        try {
            const conf = await confirm({
                title: 'XDF-Import',
                children: (
                    <>
                        <Typography>
                            Sie sind in Begriff ein XDatenfeld-Schema zu importieren.
                        </Typography>
                        <Typography>
                            Beim Import werden alle bestehenden Felder dieses Formulars vollständig überschrieben.
                            Möchten Sie den Vorgang wirklich fortsetzen?
                        </Typography>
                    </>
                ),
                confirmButtonText: 'Ja, mit dem Import fortfahren',
            });

            if (!conf) {
                return;
            }

            const xmlContent = await uploadTextFile('text/xml');

            if (xmlContent == null) {
                return;
            }

            dispatch(setLoadingMessage({
                blocking: true,
                estimatedTime: 1000,
                message: 'Importiere XDF',
            }));

            const transformed = await new XdfApiService().xdfTransform(xmlContent);

            setFormLayout(transformed);

            dispatch(clearLoadingMessage());
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Beim Importieren des XDatenfeld-Schemas ist ein Fehler aufgetreten.'));
        }
    };

    const handleOpenPreview = () => {
        window.open(publicFormLink, '_blank', 'noopener,noreferrer');
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

    const handleDownloadPdfFile = async () => {
        if (node == null) {
            return;
        }

        const formSlug = typeof node.configuration.formSlug === 'string' && node.configuration.formSlug.length > 0
            ? node.configuration.formSlug
            : `form-trigger-${node.id}`;

        dispatch(setLoadingMessage({
            blocking: false,
            estimatedTime: 1500,
            message: 'Vordruck wird generiert',
        }));

        try {
            const blob = await new FormTriggerApiService()
                .downloadPrintablePdf(node.id);

            downloadBlobFile(`${formSlug}-${node.processVersion}.pdf`, blob);
        } catch (err) {
            console.error(err);
            dispatch(showApiErrorSnackbar(err, 'Fehler beim Generieren des Vordrucks'));
        } finally {
            dispatch(clearLoadingMessage());
        }
    };

    const handlePatch = (element: FormLayoutElement) => {
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

    const moreMenuItems: FormDetailsPageMoreMenuItem[] = [
        {
            label: 'Vorschau in neuem Tab öffnen',
            icon: <Preview/>,
            endIcon: <OpenInNew fontSize="small"/>,
            onClick: handleOpenPreview,
        },
        'separator',
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
            onClick: handleDownloadPdfFile,
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
            label: 'Mit Identitätsanbieter anmelden',
            icon: <IdentityPlatform/>,
            onClick: () => {
                setShowIdentityDialog(true);
            },
            visible: identityMappingInformation.length > 0,
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
                dispatch(toggleValidation());
            },
        },
        {
            tooltip: disableVisibility ? 'Sichtbarkeiten aktivieren' : 'Sichtbarkeiten deaktivieren',
            icon: disableVisibility ? <VisibilityOutlinedIcon/> : <VisibilityOffOutlinedIcon/>,
            onClick: () => {
                dispatch(toggleVisibility());
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
            tooltip: 'Zurück zum Prozess',
            icon: ModuleIcons.processes,
            onClick: onBackToProcess,
            variant: 'contained' as const,
            activeStyle: {ml: 1},
        },
        {
            label: 'Speichern',
            tooltip: 'Änderungen speichern',
            icon: null,
            onClick: handleSave,
            variant: 'contained' as const,
            activeStyle: {ml: 1},
            disabled: !hasChanged,
        },
    ];

    if (formLayout == null || node == null || process == null || processVersion == null) {
        return;
    }

    const formAssetQueryParams = new URLSearchParams({
        version: processVersion.processVersion.toString(),
    });
    if (testClaim != null) {
        formAssetQueryParams.set('test-claim', testClaim.accessKey);
    }

    const formLogoUrl = `/api/public/form/${process.slug}/${node.configuration.formSlug}/logo/?${formAssetQueryParams.toString()}`;

    const handleSubmitEvent = async (values: AuthoredElementValues, event: string): Promise<void> => {
        if (event != SUBMIT_EVENT) {
            return;
        }

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

            await handleSave();
        }

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

        const formData = new FormData();
        formData.append('inputs', JSON.stringify(values));

        const files: FileUploadElementItem[] = [];
        walkAuthoredElementValues(formLayout, values, (element, value) => {
            if (element.type === ElementType.FileUpload && Array.isArray(value) && value.length > 0 && isFileUploadElementItem(value[0])) {
                files.push(...value);
            }
        });

        for (const file of files) {
            const blob = await fetch(file.uri).then((r) => r.blob());
            formData.append('files', blob, file.name);
            formData.append('fileUris', file.uri);
        }

        dispatch(setLoadingMessage({
            blocking: true,
            estimatedTime: 1000,
            message: 'Formular wird abgesendet',
        }));

        try {
            const startRes = await new BaseApiService()
                .postFormData<{
                    startedProcessAccessKey: string;
                }>(
                    `/api/public/form/${process?.slug}/${node.configuration.formSlug}/submit/`,
                    formData,
                    {
                        query: {
                            'test-claim': testClaimRef.current?.accessKey,
                        },
                    },
                );

            setStartedProcessAccessKey(startRes.startedProcessAccessKey);
        } catch (err) {
            dispatch(showApiErrorSnackbar(err, 'Beim Absenden des Formulars ist ein Fehler aufgetreten'));
        } finally {
            dispatch(clearLoadingMessage());
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
                                            <FormHeaderComponent
                                                form={formLayout}
                                                node={node}
                                                process={process}
                                                version={processVersion}
                                                logoUrl={formLogoUrl}
                                                onDeleteFormData={() => {
                                                    dispatch(setCurrentStep(0));
                                                    setAuthoredElementValues({});
                                                    setStartedProcessAccessKey(null);
                                                    IdentityProvidersApiService.clearIdentity(node.id);
                                                }}
                                            />

                                            {
                                                startedProcessAccessKey == null &&
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
                                                startedProcessAccessKey != null &&
                                                <Submitted
                                                    startedProcessAccessKey={startedProcessAccessKey}
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
                                            />

                                            <HelpDialog
                                                onHide={() => dispatch(showDialog(undefined))}
                                                open={metaDialog === HelpDialogId}
                                                form={formLayout}
                                            />

                                            <PrivacyDialog
                                                onHide={() => dispatch(showDialog(undefined))}
                                                open={metaDialog === PrivacyDialogId}
                                                form={formLayout}
                                            />

                                            <ImprintDialog
                                                onHide={() => dispatch(showDialog(undefined))}
                                                open={metaDialog === ImprintDialogId}
                                                form={formLayout}
                                            />

                                            <AccessibilityDialog
                                                onHide={() => dispatch(showDialog(undefined))}
                                                open={metaDialog === AccessibilityDialogId}
                                                form={formLayout}
                                            />
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
                                                borderLeft: '1px solid #E0E7E0',
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
                onAddElement={(e) => {
                    if (isAnyElementWithChildren(formLayout)) {
                        handlePatch({
                            ...formLayout,
                            children: [
                                ...(formLayout.children ?? []) as any[],
                                e,
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
                onClose={() => {
                    setShowIdentityDialog(false);
                }}
                fullWidth={true}
                maxWidth="md"
            >
                <DialogTitleWithClose
                    onClose={() => {
                        setShowIdentityDialog(false);
                    }}
                >
                    Mit Identitätsanbieter Anmelden
                </DialogTitleWithClose>
                <DialogContent>
                    <Typography
                        variant="body2"
                        component="div"
                        maxWidth={600}
                        marginBottom={4}
                    >
                        Sie können sich zu Testzwecken für jede der Konfigurierten Identitäten mit einem
                        Identitätsanbieter anmelden. Um Ihren Authentifizierungsstatus zurückzusetzen, können Sie das
                        Formularspezifische Drei-Punkte-Menü verwenden
                        und <strong>Alle Antragsdaten löschen</strong>auswählen.
                    </Typography>

                    {
                        identityMappingInformation
                            .map((idm) => (
                                <Box
                                    key={idm.id}
                                    sx={{
                                        mb: 4,
                                    }}
                                >
                                    <Typography>
                                        Identität <strong>{idm.title}</strong>
                                    </Typography>

                                    {
                                        (idm.options ?? [])
                                            .map((opt) => (
                                                <IdentityButton
                                                    isAuthenticated={false}
                                                    relatedProcessNodeId={node.id}
                                                    identityId={idm.id ?? ''}
                                                    identityProviderKey={opt.provider.key}
                                                    identityProviderAssetKey={opt.provider.iconAssetKey}
                                                    additionalScopes={opt.additionalScopes ?? []}
                                                    identityProviderName={opt.provider.name}
                                                    identityProviderType={opt.provider.type}
                                                />
                                            ))
                                    }
                                </Box>
                            ))
                    }
                </DialogContent>
            </Dialog>

            {changeBlockerDialog}
        </PageWrapper>
    );
}
