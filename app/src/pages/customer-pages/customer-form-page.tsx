import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import React, {useEffect, useMemo, useState} from 'react';
import {
    Box,
    Button,
    Container,
    Grid,
    Paper,
    Skeleton,
    Step,
    StepContent,
    StepLabel,
    Stepper,
    ThemeProvider,
    Typography,
    useTheme,
} from '@mui/material';
import {alpha} from '@mui/material/styles';
import {showDialog} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {Theme} from '../../modules/themes/models/theme';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {
    AuthoredElementValues,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    ElementDerivationResponse,
} from '../../models/element-data';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../slices/shell-slice';
import {isApiError} from '../../models/api-error';
import {FormLayoutElement} from '../../models/elements/form-layout-element';
import {BaseApiService} from '../../services/base-api-service';
import {CustomerInputService} from '../../services/customer-input-service';
import {SnackbarProvider} from 'notistack';
import {MetaElement} from '../../components/meta-element/meta-element';
import {setCurrentStep} from '../../slices/stepper-slice';
import {FormHeaderComponent} from '../../components/form/form-header-component';
import {ProcessNodeEntity} from '../../modules/process/entities/process-node-entity';
import {ProcessEntity} from '../../modules/process/entities/process-entity';
import {ProcessVersionEntity} from '../../modules/process/entities/process-version-entity';
import {HelpDialog, HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {PrivacyDialog, PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialog, ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import {AccessibilityDialog, AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {RootComponentFooter} from '../../components/form/root-component-footer';
import {ElementDerivationContext} from '../../modules/elements/components/element-derivation-context';
import {SUBMIT_EVENT} from '../../components/form/root.component.view';
import {FileUploadElementItem, isFileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {walkAuthoredElementValues} from '../../utils/element-data-utils';
import {ElementType} from '../../data/element-type/element-type';
import {Submitted} from '../../components/submitted/submitted';
import {DialogSearchParam, TestClaimSearchParam} from '../../modules/forms/constants/form-trigger-search-params';
import {
    FormTriggerApiService,
    type FormIdentitySlot,
} from '../../modules/forms/services/form-trigger-api-service';
import {createAppTheme} from '../../theming/themes';
import {BaseTheme} from '../../theming/base-theme';
import {IdentityProvidersApiService} from '../../modules/identity/identity-providers-api-service';
import {RichtextComponent} from '../../components/richtext/richtext.component';
import {
    FormIdentitySelectionControls,
} from '../../modules/identity/components/form-identity-selection-controls/form-identity-selection-controls';
import ArrowForward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import {CustomerInputLoader} from '../../dialogs/customer-input-loader/customer-input-loader';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {Chip} from '../../components/chip/chip';
import RestorePageIcon from '@aivot/mui-material-symbols-400-n25-outlined/RestorePage';
import InfoOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import AccountCircleOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/AccountCircle';
import ErrorOutlineOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Error';
import {PaymentRequestOverview} from '../../modules/payment/components/payment-request-overview';
import {showApiErrorSnackbar, showWarningSnackbar} from '../../slices/snackbar-slice';
import {useConfirm} from '../../providers/confirm-provider';
import {InstantIso} from '../../utils/temporal-types';
import {formatInstantInApplicationTimeZone} from '../../utils/temporal-utils';

interface RetrieveResponse {
    layoutElement: FormLayoutElement;
    node: ProcessNodeEntity;
    process: ProcessEntity;
    version: ProcessVersionEntity;
    identitySlots: FormIdentitySlot[];
}

const CustomerFormLoadErrorMessage = 'Das Formular konnte nicht geladen werden.';

function createCustomerFormLoadError(error: unknown) {
    if (!isApiError(error)) {
        return {
            status: 500,
            message: CustomerFormLoadErrorMessage,
        };
    }

    if (error.status === 404) {
        return {
            status: 404,
            message: 'Das Formular konnte nicht gefunden werden',
        };
    }

    return {
        status: error.status,
        message: error.displayableToUser ? error.message : CustomerFormLoadErrorMessage,
    };
}

export function CustomerFormPage() {
    const baseTheme = useTheme();

    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const confirm = useConfirm();
    const testClaimKey = useMemo(() => searchParams.get(TestClaimSearchParam), [searchParams]);
    const metaDialogName = useMemo(() => searchParams.get(DialogSearchParam), [searchParams]);

    const {
        processSlug,
        formSlug,
    } = useParams<{
        processSlug: string;
        formSlug: string;
    }>();

    const dispatch = useAppDispatch();

    const [data, setData] = useState<RetrieveResponse | null>(null);

    const [authoredElementValues, setAuthoredElementValues] = useState<AuthoredElementValues>({});
    const [derivedData, setDerivedData] = useState<DerivedRuntimeElementData>(createDerivedRuntimeElementData());
    const [derivedDataVersion, setDerivedDataVersion] = useState(0);
    const [pendingStepRestore, setPendingStepRestore] = useState<{
        stepId: string | null;
        stepIndex: number;
        minimumDerivedDataVersion: number;
    } | null>(null);

    const [startedProcessAccessInfo, setStartedProcessAccessInfo] = useState<{
        processInstanceAccessKey: string;
        paymentRequired: boolean;
    } | null>(null);
    const [dismissAuthentication, setDismissAuthentication] = useState(false);
    const [customerInputLoaderResolved, setCustomerInputLoaderResolved] = useState(false);

    useEffect(() => {
        dispatch(showDialog(metaDialogName ?? undefined));
    }, [dispatch, metaDialogName]);

    useEffect(() => {
        if (processSlug == null || formSlug == null) {
            return;
        }

        new BaseApiService()
            .get<RetrieveResponse>(`/api/public/form/${processSlug}/${formSlug}/`, {
                query: {
                    'test-claim': testClaimKey,
                },
            })
            .then((res) => {
                if (res.process.slug !== processSlug) {
                    navigate(`/form/${res.process.slug}/${formSlug}${window.location.search}`, {
                        replace: true,
                    });
                }

                setData(res);
                setAuthoredElementValues({});
                setDerivedData(createDerivedRuntimeElementData());
                setDerivedDataVersion(0);
                setPendingStepRestore(null);
                setStartedProcessAccessInfo(null);
                setDismissAuthentication(false);
                setCustomerInputLoaderResolved(false);
                dispatch(setCurrentStep(0));
            })
            .catch((err) => {
                dispatch(setErrorMessage(createCustomerFormLoadError(err)));
            });
    }, [processSlug, formSlug, testClaimKey, navigate, dispatch]);

    const metaDialog = useAppSelector((state) => state.app.showDialog);
    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    const [theme, setTheme] = useState<Theme>();

    const {
        layoutElement,
        node,
        process,
        version,
    } = data ?? {};

    useEffect(() => {
        if (process == null || node == null || version == null || node.configuration.formSlug == null) {
            setTheme(undefined);
            return;
        }

        let isCancelled = false;

        new FormTriggerApiService()
            .getFormTheme(
                process.slug,
                node.configuration.formSlug,
                undefined,
                testClaimKey ?? undefined,
            )
            .then((res) => {
                if (!isCancelled) {
                    setTheme(res);
                }
            })
            .catch((error) => {
                console.error('Error loading form theme:', error);
                if (!isCancelled) {
                    setTheme(undefined);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [node, process, testClaimKey, version]);

    const resolvedTheme = useMemo(() => {
        if (theme == null) {
            return baseTheme;
        }

        return createAppTheme(theme, BaseTheme, baseTheme.palette.mode);
    }, [baseTheme, theme]);

    const handleSubmitEvent = async (values: AuthoredElementValues, event: string) => {
        if (event !== SUBMIT_EVENT || layoutElement == null || node == null || process == null || version == null) {
            return;
        }

        const resolvedFormSlug = node.configuration.formSlug ?? formSlug;
        if (resolvedFormSlug == null) {
            return;
        }

        let errorMessage = 'Das Absenden des Formulars konnte nicht vorbereitet werden.';
        let loadingStarted = false;
        try {
            errorMessage = 'Beim Berechnen der Kosten ist ein unbekannter Fehler aufgetreten.';
            const costs = await new FormTriggerApiService()
                .calculateCosts(process.slug, resolvedFormSlug, values, {
                    testClaim: testClaimKey ?? undefined,
                });
            const paymentRequired = costs.totalCost > 0;

            if (paymentRequired) {
                errorMessage = 'Die Zahlungsanforderung konnte nicht vorbereitet werden.';
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
            walkAuthoredElementValues(layoutElement, values, (element, value) => {
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
            errorMessage = 'Beim Absenden des Formulars ist ein Fehler aufgetreten.';

            const startRes = await new FormTriggerApiService()
                .submitForm(process.slug, resolvedFormSlug, formData, {
                    testClaim: testClaimKey ?? undefined,
                });

            setStartedProcessAccessInfo({
                processInstanceAccessKey: startRes.startedProcessAccessKey,
                paymentRequired: paymentRequired,
            });

            try {
                CustomerInputService.cleanCustomerInput(process.slug, resolvedFormSlug, version.processVersion);
            } catch (cleanupError) {
                console.error('Error cleaning submitted customer input:', cleanupError);
            }
        } catch (error) {
            dispatch(showApiErrorSnackbar(error, errorMessage));
        } finally {
            if (loadingStarted) {
                dispatch(clearLoadingMessage());
            }
        }
    };

    const handleDerive = (values: AuthoredElementValues, skipErrorsForElements: string[]) => {
        const resolvedProcessSlug = process?.slug ?? processSlug;
        const resolvedFormSlug = node?.configuration.formSlug ?? formSlug;
        if (resolvedProcessSlug == null || resolvedFormSlug == null) {
            return Promise.resolve(createDerivedRuntimeElementData());
        }

        return new BaseApiService()
            .post<AuthoredElementValues, ElementDerivationResponse>(`/api/public/form/${resolvedProcessSlug}/${resolvedFormSlug}/derive/`, values, {
                query: {
                    'test-claim': testClaimKey,
                    skipErrorsFor: skipErrorsForElements,
                    skipVisibilitiesFor: [],
                    skipValuesFor: [],
                    skipOverridesFor: [],
                },
            })
            .then((res) => {
                return res.elementData;
            });
    };

    const handleAuthoredElementValuesChange = (nextAuthoredElementValues: AuthoredElementValues) => {
        setAuthoredElementValues(nextAuthoredElementValues);

        if (process == null || node == null || version == null || layoutElement == null) {
            return;
        }

        const resolvedFormSlug = node.configuration.formSlug ?? formSlug;
        if (resolvedFormSlug == null) {
            return;
        }

        CustomerInputService.storeCustomerInput(
            process.slug,
            resolvedFormSlug,
            version.processVersion,
            layoutElement,
            nextAuthoredElementValues,
        );
    };

    if (data == null || layoutElement == null || node == null || process == null || version == null) {
        return null;
    }

    const resolvedFormSlug = node.configuration.formSlug ?? formSlug;
    if (resolvedFormSlug == null) {
        return null;
    }

    const formAssetQueryParams = new URLSearchParams();
    if (testClaimKey != null) {
        formAssetQueryParams.set('test-claim', testClaimKey);
    }

    const formAssetQuery = formAssetQueryParams.toString();
    const formLogoUrl = `/api/public/form/${process.slug}/${resolvedFormSlug}/logo/?${formAssetQuery}`;
    const darkLogoQueryParams = new URLSearchParams(formAssetQueryParams);
    darkLogoQueryParams.set('color-scheme', 'dark');
    const formLogoUrlDark = `/api/public/form/${process.slug}/${resolvedFormSlug}/logo/?${darkLogoQueryParams.toString()}`;
    const formFaviconUrl = `/api/public/form/${process.slug}/${resolvedFormSlug}/favicon/?${formAssetQuery}`;
    const customerInputDraft = CustomerInputService.loadCustomerInputDraft(process.slug, resolvedFormSlug, version.processVersion);
    const showFormFlow = data.identitySlots.length === 0 || dismissAuthentication;

    return (
        <ThemeProvider theme={resolvedTheme}>
            <SnackbarProvider>
                <MetaElement
                    faviconUrl={formFaviconUrl}
                    title={layoutElement.tabTitle ?? layoutElement.publicTitle ?? ''}
                    titlePrefix={provider}
                />

                <Box
                    sx={{
                        backgroundColor: 'background.default',
                    }}
                >
                    <FormHeaderComponent
                        form={layoutElement}
                        node={node}
                        process={process}
                        version={version}
                        logoUrl={formLogoUrl}
                        logoUrlDark={formLogoUrlDark}
                        onDeleteFormData={() => {
                            dispatch(setCurrentStep(0));
                            CustomerInputService.cleanCustomerInput(process.slug, resolvedFormSlug, version.processVersion);
                            setAuthoredElementValues({});
                            setDerivedData(createDerivedRuntimeElementData());
                            setDerivedDataVersion(0);
                            setPendingStepRestore(null);
                            setStartedProcessAccessInfo(null);
                            setDismissAuthentication(false);
                            setCustomerInputLoaderResolved(true);
                            IdentityProvidersApiService.clearIdentity(node.id);
                            setData((currentData) => {
                                if (currentData == null) {
                                    return null;
                                }

                                return {
                                    ...currentData,
                                    identitySlots: currentData.identitySlots.map((slot) => ({
                                        ...slot,
                                        identityType: null,
                                        emailAddress: null,
                                        isReady: false,
                                        communication: null,
                                        availableIdentityProviders: slot.availableIdentityProviders.map((provider) => ({
                                            ...provider,
                                            isAuthenticatedWithThis: false,
                                        })),
                                    })),
                                };
                            });
                        }}
                    />

                    {
                        data.identitySlots.length > 0 &&
                        !dismissAuthentication &&
                        <AuthPlaceholder
                            relatedProcessNodeId={node.id}
                            processSlug={process.slug}
                            formSlug={resolvedFormSlug}
                            testClaim={testClaimKey ?? undefined}
                            identitySlots={data.identitySlots}
                            customerInputDraftDate={customerInputDraft?.date ?? null}
                            onIdentitySlotChange={(nextSlot) => {
                                setData(currentData => currentData == null ? null : {
                                    ...currentData,
                                    identitySlots: currentData.identitySlots.map(slot => (
                                        slot.id === nextSlot.id ? nextSlot : slot
                                    )),
                                });
                            }}
                            onDismiss={() => {
                                setDismissAuthentication(true);
                            }}
                        />
                    }

                    {
                        showFormFlow &&
                        startedProcessAccessInfo == null &&
                        !customerInputLoaderResolved &&
                        customerInputDraft != null &&
                        <CustomerFormSkeleton />
                    }

                    {
                        showFormFlow &&
                        startedProcessAccessInfo == null &&
                        !customerInputLoaderResolved &&
                        <CustomerInputLoader
                            processSlug={process.slug}
                            formSlug={resolvedFormSlug}
                            version={version.processVersion}
                            rootElement={layoutElement}
                            onElementDataLoad={setAuthoredElementValues}
                            onResolved={() => {
                                setCustomerInputLoaderResolved(true);
                            }}
                            isBusy={false}
                        />
                    }

                    {
                        showFormFlow &&
                        startedProcessAccessInfo == null &&
                        customerInputLoaderResolved &&
                        <ElementDerivationContext
                            element={layoutElement}
                            authoredElementValues={authoredElementValues}
                            derivedData={derivedData}
                            onDerivedDataChange={(nextDerivedData) => {
                                setDerivedData(nextDerivedData);
                                setDerivedDataVersion((current) => current + 1);
                            }}
                            onAuthoredElementValuesChange={handleAuthoredElementValuesChange}
                            onEvent={handleSubmitEvent}
                            onDeriveOverride={handleDerive}
                        />
                    }
                    {
                        showFormFlow &&
                        startedProcessAccessInfo != null &&
                        <Submitted
                            startedProcessAccessKey={startedProcessAccessInfo.processInstanceAccessKey}
                            paymentRequired={startedProcessAccessInfo.paymentRequired}
                            formElement={layoutElement}
                            node={node}
                            process={process}
                            version={version}
                        />
                    }

                    <RootComponentFooter
                        form={layoutElement}
                        node={node}
                        process={process}
                        version={version}
                        logoUrl={formLogoUrl}
                        logoUrlDark={formLogoUrlDark}
                    />
                </Box>

                <HelpDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === HelpDialogId}
                    form={layoutElement}
                    version={version}
                />

                <PrivacyDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === PrivacyDialogId}
                    form={layoutElement}
                    version={version}
                />

                <ImprintDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === ImprintDialogId}
                    form={layoutElement}
                    version={version}
                />

                <AccessibilityDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === AccessibilityDialogId}
                    form={layoutElement}
                    version={version}
                />
            </SnackbarProvider>
        </ThemeProvider>
    );
}

interface AuthPlaceholderProps {
    relatedProcessNodeId: number;
    processSlug: string;
    formSlug: string;
    testClaim?: string;
    identitySlots: RetrieveResponse['identitySlots'];
    customerInputDraftDate: InstantIso | null;
    onIdentitySlotChange: (slot: FormIdentitySlot) => void;
    onDismiss: () => void;
}

function getIdentityDisplayName(identity: { title: string | null }): string {
    const title = identity.title?.trim();

    return title != null && title.length > 0 ? title : 'Unbenannte Identität';
}

function AuthPlaceholder(props: AuthPlaceholderProps) {
    const {
        customerInputDraftDate,
        formSlug,
        identitySlots,
        onIdentitySlotChange,
        onDismiss,
        processSlug,
        relatedProcessNodeId,
        testClaim,
    } = props;
    const [pendingSlotIds, setPendingSlotIds] = useState<Set<string>>(new Set());

    const authRequired = identitySlots
        .some(slot => slot.isRequired);
    const allSelectedIdentitiesReady = identitySlots.every(slot => (
        slot.identityType == null
            ? slot.isOptional
            : slot.isReady && !pendingSlotIds.has(slot.id)
    ));
    const someIdentitySelected = identitySlots
        .some(slot => slot.identityType != null);
    const sortedIdentitySlots = useMemo(() => (
        identitySlots
            .map((slot, index) => ({
                slot,
                index,
            }))
            .sort((a, b) => Number(b.slot.isRequired) - Number(a.slot.isRequired) || a.index - b.index)
            .map(({slot}) => slot)
    ), [identitySlots]);

    return (
        <Container
            maxWidth="lg"
            sx={{
                pt: {
                    xs: 5,
                    md: 8,
                },
                pb: {
                    xs: 10,
                    md: 16,
                },
            }}
        >
            <Grid
                container
                spacing={3}
            >
                <Grid size={{xs: 12, md: 10, lg: 8}} sx={{mb: 2}}>
                    <Typography
                        variant="h2"
                        component="div"
                    >
                        Identität auswählen
                    </Typography>

                    {
                        authRequired
                            ? (
                                <Typography
                                    sx={{
                                        mt: 1,
                                        maxWidth: 680,
                                    }}
                                >
                                    Für dieses Formular ist eine Identität erforderlich. Wählen Sie für jeden als verpflichtend
                                    gekennzeichneten Eintrag ein Nutzerkonto oder, sofern angeboten, eine E-Mail-Adresse aus.
                                </Typography>
                            )
                            : (
                                <Typography
                                    sx={{
                                        mt: 1,
                                        maxWidth: 680,
                                    }}
                                >
                                    Sie können ein Nutzerkonto oder eine angebotene E-Mail-Adresse verwenden. Alternativ können
                                    Sie das Formular ohne Identitätsangabe ausfüllen.
                                </Typography>
                            )
                    }
                </Grid>

                {
                    sortedIdentitySlots
                        .map(slot => (
                            <Grid
                                key={slot.id}
                                size={{
                                    xs: 12,
                                    md: 6,
                                }}
                            >
                                <Paper
                                    variant="outlined"
                                    sx={{
                                        height: '100%',
                                        p: {
                                            xs: 2,
                                            md: 2.5,
                                        },
                                        borderColor: 'divider',
                                        backgroundColor: 'background.paper',
                                    }}
                                >
                                    <Box
                                        sx={{
                                            display: 'flex',
                                            flexDirection: 'column',
                                            height: '100%',
                                        }}
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
                                                    variant="h4"
                                                    component="h2"
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

                                        {
                                            isStringNotNullOrEmpty(slot.description) &&
                                            <RichtextComponent
                                                content={slot.description}
                                                sx={{
                                                    mt: 2,
                                                }}
                                            />
                                        }

                                        {
                                            slot.isRequired &&
                                            <Typography
                                                variant="body2"
                                                sx={{
                                                    color: "text.secondary",
                                                    mt: 2
                                                }}>
                                                Eine der nachfolgenden Möglichkeiten ist zwingend erforderlich.
                                            </Typography>
                                        }

                                        {
                                            slot.isOptional &&
                                            <Typography
                                                variant="body2"
                                                sx={{
                                                    color: "text.secondary",
                                                    mt: 2
                                                }}>
                                                Eine der nachfolgenden Möglichkeiten kann optional verwendet werden.
                                            </Typography>
                                        }

                                        <FormIdentitySelectionControls
                                            slot={slot}
                                            processSlug={processSlug}
                                            formSlug={formSlug}
                                            relatedProcessNodeId={relatedProcessNodeId}
                                            testClaim={testClaim}
                                            onChange={onIdentitySlotChange}
                                            onPendingChange={(pending) => {
                                                setPendingSlotIds(current => {
                                                    const next = new Set(current);
                                                    if (pending) {
                                                        next.add(slot.id);
                                                    } else {
                                                        next.delete(slot.id);
                                                    }
                                                    return next;
                                                });
                                            }}
                                        />
                                    </Box>
                                </Paper>
                            </Grid>
                        ))
                }

                {
                    customerInputDraftDate != null &&
                    <Grid size={{xs: 12, md: 6}}>
                        <CustomerInputDraftTeaser date={customerInputDraftDate} />
                    </Grid>
                }

                <Grid size={{xs: 12}}>
                    <Box
                        sx={{
                            display: 'flex',
                            justifyContent: 'flex-start',
                            flexWrap: 'wrap',
                            gap: 1.5,
                            mt: 4,
                        }}
                    >
                        {
                            !authRequired &&
                            !someIdentitySelected &&
                            <Button
                                variant="contained"
                                endIcon={<ArrowForward/>}
                                onClick={onDismiss}
                                sx={{
                                    width: {
                                        xs: '100%',
                                        sm: 'auto',
                                    },
                                }}
                            >
                                Ohne Anmeldung fortfahren
                            </Button>
                        }
                        {
                            (authRequired || someIdentitySelected) &&
                            <Button
                                variant="contained"
                                endIcon={<ArrowForward/>}
                                onClick={onDismiss}
                                disabled={!allSelectedIdentitiesReady}
                                sx={{
                                    width: {
                                        xs: '100%',
                                        sm: 'auto',
                                    },
                                }}
                            >
                                Mit Formular fortfahren
                            </Button>
                        }
                    </Box>
                </Grid>
            </Grid>
        </Container>
    );
}

function CustomerInputDraftTeaser(props: { date: InstantIso }) {
    const {
        date,
    } = props;

    return (
        <Paper
            variant="outlined"
            role="note"
            sx={(theme) => ({
                height: '100%',
                p: {
                    xs: 2,
                    md: 2.5,
                },
                borderColor: alpha(theme.palette.text.primary, 0.16),
                backgroundColor: alpha(theme.palette.text.primary, 0.025),
            })}
        >
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    height: '100%',
                    pt: .5,
                }}
            >
                <Typography
                    variant="caption"
                    component="p"
                >
                    Lokaler Speicher
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
                        variant="h4"
                        component="h2"
                    >
                        Gespeicherter Entwurf vorhanden
                    </Typography>

                    <RestorePageIcon
                        color="primary"
                        sx={{
                            fontSize: 32,
                            flexShrink: 0,
                        }}
                    />
                </Box>

                <Typography
                    variant="body2"
                    sx={{
                        color: "text.secondary",
                        mt: 1.12
                    }}>
                    Sie können im nächsten Schritt entscheiden, ob Sie diesen Entwurf weiterbearbeiten oder neu
                    beginnen möchten.
                </Typography>

                <Typography
                    variant="body2"
                    sx={{
                        color: "text.secondary",
                        mt: 'auto',
                        pt: 2
                    }}>
                    Zuletzt bearbeitet: {formatInstantInApplicationTimeZone(
                        date,
                        'dd.MM.yyyy, HH:mm',
                    )} Uhr
                </Typography>
            </Box>
        </Paper>
    );
}

function CustomerFormSkeletonStepIcon(props: {
    active: boolean;
    Icon: React.ElementType;
}) {
    const {
        active,
        Icon,
    } = props;
    const theme = useTheme();

    return (
        <Icon
            sx={{
                fontSize: '2rem',
                marginLeft: '4px',
                color: active
                    ? alpha(theme.palette.primary.main, 0.45)
                    : alpha(theme.palette.text.primary, 0.38),
            }}
        />
    );
}

function CustomerFormSkeleton() {
    const theme = useTheme();
    const skeletonSx = {
        bgcolor: alpha(theme.palette.text.primary, 0.075),
    };

    const stepSkeletons = [
        {
            Icon: InfoOutlinedIcon,
            width: 230,
        },
        {
            Icon: AccountCircleOutlinedIcon,
            width: 210,
        },
        {
            Icon: ErrorOutlineOutlinedIcon,
            width: 170,
        },
    ];

    return (
        <Box
            component="main"
            aria-hidden="true"
        >
            <Container
                sx={{
                    mt: 5,
                    mb: 5,
                    minHeight: '66vh',
                }}
            >
                <Stepper
                    activeStep={0}
                    orientation="vertical"
                    sx={{
                        mt: 8,
                        mb: 10,
                        ml: '20px',
                        [theme.breakpoints.down('md')]: {
                            mt: 5,
                            mb: 6,
                            ml: 0,
                        },
                        '& .MuiStepConnector-line': {
                            borderColor: alpha(theme.palette.text.primary, 0.16),
                        },
                    }}
                >
                    {
                        stepSkeletons.map((step, index) => (
                            <Step
                                key={index}
                                expanded={index === 0}
                            >
                                <StepLabel
                                    sx={{
                                        [theme.breakpoints.down('md')]: {
                                            '.MuiStepLabel-label': {
                                                ml: 1,
                                            },
                                        },
                                        '.MuiStepLabel-label': {
                                            pt: 0,
                                        },
                                    }}
                                    slots={{
                                        stepIcon: () => (
                                            <CustomerFormSkeletonStepIcon
                                                active={index === 0}
                                                Icon={step.Icon}
                                            />
                                        )
                                    }}
                                >
                                    <Skeleton
                                        animation={false}
                                        variant="rounded"
                                        width={step.width}
                                        height={22}
                                        sx={skeletonSx}
                                    />
                                </StepLabel>

                                {
                                    index === 0 &&
                                    <StepContent
                                        sx={{
                                            [theme.breakpoints.down('md')]: {
                                                pl: 4,
                                            },
                                        }}
                                    >
                                        <Grid
                                            container
                                            spacing={3}
                                            sx={{pt: 1}}
                                        >
                                            {
                                                [0, 1, 2, 3].map((fieldIndex) => (
                                                    <Grid
                                                        key={fieldIndex}
                                                        size={{
                                                            xs: 12,
                                                            md: fieldIndex === 2 ? 12 : 8,
                                                        }}
                                                    >
                                                        <Skeleton
                                                            animation={false}
                                                            variant="rounded"
                                                            width={fieldIndex === 1 ? '28%' : '42%'}
                                                            height={14}
                                                            sx={skeletonSx}
                                                        />
                                                        <Skeleton
                                                            animation={false}
                                                            variant="rounded"
                                                            width="100%"
                                                            height={fieldIndex === 2 ? 88 : 48}
                                                            sx={{
                                                                ...skeletonSx,
                                                                mt: 1,
                                                            }}
                                                        />
                                                    </Grid>
                                                ))
                                            }
                                        </Grid>

                                        <Box
                                            sx={{
                                                display: 'flex',
                                                justifyContent: 'space-between',
                                                mt: {
                                                    xs: 3,
                                                    md: 6,
                                                },
                                                mb: {
                                                    xs: 4,
                                                    md: 7,
                                                },
                                                flexDirection: {
                                                    xs: 'column',
                                                    md: 'row',
                                                },
                                            }}
                                        >
                                            <Skeleton
                                                animation={false}
                                                variant="rounded"
                                                width={118}
                                                height={42}
                                                sx={skeletonSx}
                                            />
                                        </Box>
                                    </StepContent>
                                }
                            </Step>
                        ))
                    }
                </Stepper>
            </Container>

            <Container
                sx={{
                    textAlign: 'left',
                    marginTop: 0,
                    mb: 8,
                    [theme.breakpoints.up('md')]: {
                        textAlign: 'right',
                        marginTop: '-80px',
                    },
                }}
            >
                <Skeleton
                    animation={false}
                    variant="rounded"
                    width={300}
                    height={32}
                    sx={{
                        ...skeletonSx,
                        display: 'inline-block',
                    }}
                />
            </Container>
        </Box>
    );
}
