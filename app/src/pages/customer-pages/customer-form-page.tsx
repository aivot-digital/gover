import {useNavigate, useParams, useSearchParams} from 'react-router-dom';
import React, {useEffect, useMemo, useState} from 'react';
import {Box, Button, Container, Grid, Paper, ThemeProvider, Typography, useTheme} from '@mui/material';
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
import {FormTriggerApiService} from '../../modules/forms/services/form-trigger-api-service';
import {createAppTheme} from '../../theming/themes';
import {BaseTheme} from '../../theming/base-theme';
import {IdentityProvidersApiService} from '../../modules/identity/identity-providers-api-service';
import {IdentityProviderType} from '../../modules/identity/enums/identity-provider-type';
import {RichtextComponent} from '../../components/richtext/richtext.component';
import {IdentityButton} from '../../modules/identity/components/identity-button/identity-button';
import ArrowForward from '@aivot/mui-material-symbols-400-outlined/dist/arrow-forward/ArrowForward';
import {CustomerInputLoader} from '../../dialogs/customer-input-loader/customer-input-loader';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';
import {Chip} from '../../components/chip/chip';

interface RetrieveResponse {
    layoutElement: FormLayoutElement;
    node: ProcessNodeEntity;
    process: ProcessEntity;
    version: ProcessVersionEntity;
    identitySlots: {
        id: string;
        title: string | null;
        description: string | null;
        isOptional: boolean;
        isRequired: boolean;
        allowsEmail: boolean;
        isAuthenticated: boolean;
        availableIdentityProviders: {
            identityProviderKey: string;
            identityProviderName: string;
            identityProviderAssetKey: string | null;
            identityProviderType: IdentityProviderType;
            isAuthenticatedWithThis: boolean;
            additionalScopes: string[];
        }[];
    }[];
}

export function CustomerFormPage() {
    const baseTheme = useTheme();

    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
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

    const [startedProcessAccessKey, setStartedProcessAccessKey] = useState<string | null>(null);
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
                setStartedProcessAccessKey(null);
                setDismissAuthentication(false);
                setCustomerInputLoaderResolved(false);
                dispatch(setCurrentStep(0));
            })
            .catch((err) => {
                if (isApiError(err)) {
                    if (err.status === 404) {
                        dispatch(setErrorMessage({
                            status: 404,
                            message: 'Das Formular konnte nicht gefunden werden',
                        }));
                    } else if (err.displayableToUser) {
                        dispatch(setErrorMessage({
                            status: err.status,
                            message: err.message,
                        }));
                    } else {
                        dispatch(setErrorMessage({
                            status: err.status,
                            message: 'Ein unbekannter Fehler ist aufgetreten',
                        }));
                    }
                }
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

        return createAppTheme(theme, BaseTheme);
    }, [baseTheme, theme]);

    const handleSubmitEvent = async (values: AuthoredElementValues, event: string) => {
        if (event !== SUBMIT_EVENT || layoutElement == null || node == null || process == null || version == null) {
            return;
        }

        const resolvedFormSlug = node.configuration.formSlug ?? formSlug;
        if (resolvedFormSlug == null) {
            return;
        }

        const formData = new FormData();
        formData.append('inputs', JSON.stringify(values));

        const files: FileUploadElementItem[] = [];
        walkAuthoredElementValues(layoutElement, values, (element, value) => {
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
                    `/api/public/form/${process.slug}/${resolvedFormSlug}/submit/`,
                    formData,
                    {
                        query: {
                            'test-claim': testClaimKey,
                        },
                    },
                );

            CustomerInputService.cleanCustomerInput(process.slug, resolvedFormSlug, version.processVersion);
            setStartedProcessAccessKey(startRes.startedProcessAccessKey);
        } finally {
            dispatch(clearLoadingMessage());
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
    const formFaviconUrl = `/api/public/form/${process.slug}/${resolvedFormSlug}/favicon/?${formAssetQuery}`;

    return (
        <ThemeProvider theme={resolvedTheme}>
            <SnackbarProvider>
                <MetaElement
                    faviconUrl={formFaviconUrl}
                    title={layoutElement.tabTitle ?? layoutElement.headline ?? ''}
                    titlePrefix={provider}
                />

                <Box
                    sx={{
                        backgroundColor: 'white',
                    }}
                >
                    <FormHeaderComponent
                        form={layoutElement}
                        node={node}
                        process={process}
                        version={version}
                        logoUrl={formLogoUrl}
                        onDeleteFormData={() => {
                            dispatch(setCurrentStep(0));
                            CustomerInputService.cleanCustomerInput(process.slug, resolvedFormSlug, version.processVersion);
                            setAuthoredElementValues({});
                            setDerivedData(createDerivedRuntimeElementData());
                            setDerivedDataVersion(0);
                            setPendingStepRestore(null);
                            setStartedProcessAccessKey(null);
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
                                        isAuthenticated: false,
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
                            identitySlots={data.identitySlots}
                            onDismiss={() => {
                                setDismissAuthentication(true);
                            }}
                        />
                    }

                    {
                        (data.identitySlots.length === 0 || dismissAuthentication) &&
                        startedProcessAccessKey == null &&
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
                        (data.identitySlots.length === 0 || dismissAuthentication) &&
                        startedProcessAccessKey == null &&
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
                        (data.identitySlots.length === 0 || dismissAuthentication) &&
                        startedProcessAccessKey != null &&
                        <Submitted
                            startedProcessAccessKey={startedProcessAccessKey}
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
                    />
                </Box>

                <HelpDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === HelpDialogId}
                    form={layoutElement}
                />

                <PrivacyDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === PrivacyDialogId}
                    form={layoutElement}
                />

                <ImprintDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === ImprintDialogId}
                    form={layoutElement}
                />

                <AccessibilityDialog
                    onHide={() => dispatch(showDialog(undefined))}
                    open={metaDialog === AccessibilityDialogId}
                    form={layoutElement}
                />
            </SnackbarProvider>
        </ThemeProvider>
    );
}

interface AuthPlaceholderProps {
    relatedProcessNodeId: number;
    identitySlots: RetrieveResponse['identitySlots'];
    onDismiss: () => void;
}

function getIdentityDisplayName(identity: { title: string | null }): string {
    const title = identity.title?.trim();

    return title != null && title.length > 0 ? title : 'Unbenannte Identität';
}

function AuthPlaceholder(props: AuthPlaceholderProps) {
    const {
        identitySlots,
        onDismiss,
        relatedProcessNodeId,
    } = props;

    const theme = useTheme();

    const authRequired = identitySlots
        .some(slot => slot.isRequired);
    const allRequiredAuthenticated = identitySlots
        .every(slot => slot.isOptional || slot.isAuthenticated);
    const someAuthenticated = identitySlots
        .some(slot => slot.isAuthenticated);
    const sortedIdentitySlots = useMemo(() => {
        return identitySlots
            .map((slot, index) => ({
                slot,
                index,
            }))
            .sort((a, b) => Number(b.slot.isRequired) - Number(a.slot.isRequired) || a.index - b.index)
            .map(({slot}) => slot);
    }, [identitySlots]);

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
                    {
                        authRequired &&
                        <>
                            <Typography
                                variant="h2"
                                component="div"
                            >
                                Anmeldung erforderlich
                            </Typography>

                            <Typography
                                sx={{
                                    mt: 1,
                                    maxWidth: 680,
                                }}
                            >
                                Sie müssen sich mit den nachfolgend als verpflichtend gekennzeichneten Identitäten anmelden.
                                Nach einer erfolgreichen Authentifizierung werden ggf. verfügbare Daten automatisch in das Formular
                                übernommen.
                            </Typography>
                        </>
                    }
                    {
                        !authRequired &&
                        <>
                            <Typography
                                variant="h2"
                                component="div"
                            >
                                Anmeldung optional
                            </Typography>

                            <Typography
                                sx={{
                                    mt: 1,
                                    maxWidth: 680,
                                }}
                            >
                                Sie können sich optional mit einer der nachfolgenden Identitäten anmelden.
                                Nach einer erfolgreichen Authentifizierung werden ggf. verfügbare Daten automatisch in das Formular
                                übernommen. Sie können das Formular auch ohne Anmeldung ausfüllen.
                            </Typography>
                        </>
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
                                        borderColor: slot.isAuthenticated
                                            ? alpha(theme.palette.success.main, 0.45)
                                            : slot.isRequired
                                                ? alpha(theme.palette.warning.main, 0.5)
                                                : alpha(theme.palette.text.primary, 0.16),
                                        backgroundColor: slot.isAuthenticated
                                            ? alpha(theme.palette.success.main, 0.0125)
                                            : slot.isRequired
                                                ? alpha(theme.palette.warning.main, 0.0125)
                                                : undefined,
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
                                                Anmelden als
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
                                                color="text.secondary"
                                                mt={2}
                                            >
                                                Eine Authentifizierung mit einem der nachfolgenden Konten
                                                ist zwingend erforderlich.
                                            </Typography>
                                        }

                                        {
                                            slot.isOptional &&
                                            <Typography
                                                variant="body2"
                                                color="text.secondary"
                                                mt={2}
                                            >
                                                Eine Authentifizierung mit einem der nachfolgenden Konten
                                                ist optional möglich.
                                            </Typography>
                                        }

                                        <Box
                                            sx={{
                                                mt: 2,
                                            }}
                                        >
                                            {
                                                slot.availableIdentityProviders.length === 0
                                                    ? (
                                                        <Box
                                                            sx={(theme) => ({
                                                                mt: 2,
                                                                px: 2,
                                                                py: 1.5,
                                                                border: '1px dashed',
                                                                borderColor: alpha(theme.palette.text.primary, 0.18),
                                                                borderRadius: 1,
                                                                backgroundColor: alpha(theme.palette.text.primary, 0.015),
                                                            })}
                                                        >
                                                            <Typography
                                                                variant="body2"
                                                                color="text.secondary"
                                                            >
                                                                Für diese Identität steht aktuell keine Anmeldemöglichkeit zur Verfügung.
                                                            </Typography>
                                                        </Box>
                                                    )
                                                    : slot
                                                        .availableIdentityProviders
                                                        .map((idp) => (
                                                            <IdentityButton
                                                                key={`${slot.id}-${idp.identityProviderKey}`}
                                                                relatedProcessNodeId={relatedProcessNodeId}
                                                                identityProviderKey={idp.identityProviderKey}
                                                                identityProviderName={idp.identityProviderName}
                                                                identityProviderType={idp.identityProviderType}
                                                                identityProviderAssetKey={idp.identityProviderAssetKey}
                                                                isAuthenticated={idp.isAuthenticatedWithThis}
                                                                identityId={slot.id}
                                                                additionalScopes={idp.additionalScopes}
                                                            />
                                                        ))
                                            }
                                        </Box>
                                    </Box>
                                </Paper>
                            </Grid>
                        ))
                }

                <Grid size={{xs: 12}}>
                    <Box
                        sx={{
                            display: 'flex',
                            justifyContent: 'flex-end',
                        }}
                    >
                        {
                            !authRequired &&
                            !someAuthenticated &&
                            <Button
                                endIcon={<ArrowForward/>}
                                onClick={onDismiss}
                            >
                                Ohne Anmeldung fortfahren
                            </Button>
                        }
                        {
                            authRequired &&
                            <Button
                                endIcon={<ArrowForward/>}
                                onClick={onDismiss}
                                disabled={!allRequiredAuthenticated}
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
