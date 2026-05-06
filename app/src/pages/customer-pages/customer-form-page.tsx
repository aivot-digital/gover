import {useParams, useSearchParams} from 'react-router-dom';
import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Box, ThemeProvider, useTheme} from '@mui/material';
import {showDialog} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {Theme} from '../../modules/themes/models/theme';
import {useApi} from '../../hooks/use-api';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {selectIdentityId} from '../../slices/identity-slice';
import {AuthoredElementValues, DerivedRuntimeElementData, ElementDerivationResponse} from '../../models/element-data';
import {clearLoadingMessage, setErrorMessage, setLoadingMessage} from '../../slices/shell-slice';
import {isApiError} from '../../models/api-error';
import {FormLayoutElement} from '../../models/elements/form-layout-element';
import {BaseApiService} from '../../services/base-api-service';
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
import {AnyElement} from '../../models/elements/any-element';
import {flattenElements} from '../../utils/flatten-elements';
import {RootComponentFooter} from '../../components/form/root-component-footer';
import {ElementDerivationContext} from '../../modules/elements/components/element-derivation-context';
import {SUBMIT_EVENT} from '../../components/form/root.component.view';
import {FileUploadElementItem, isFileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {walkAuthoredElementValues} from '../../utils/element-data-utils';
import {ElementType} from '../../data/element-type/element-type';
import {Submitted} from '../../components/submitted/submitted';

export const TestClaimSearchParam = 'test-claim';
export const DialogSearchParam = 'dialog';

interface RetrieveResponse {
    layoutElement: FormLayoutElement;
    node: ProcessNodeEntity;
    process: ProcessEntity;
    version: ProcessVersionEntity;
}

export function CustomerFormPage() {
    const baseTheme = useTheme();
    const api = useApi();

    const [searchParams] = useSearchParams();
    const testClaimKey = useMemo(() => searchParams.get(TestClaimSearchParam), [searchParams]);
    const metaDialogName = useMemo(() => searchParams.get(DialogSearchParam), [searchParams]);

    const {
        processAccessKey,
        formSlug,
    } = useParams<{
        processAccessKey: string;
        formSlug: string;
    }>();

    const dispatch = useAppDispatch();

    const scrollContainerRef = useRef<HTMLDivElement>(null);

    const [data, setData] = useState<RetrieveResponse | null>(null);
    const [allElements, setAllElements] = useState<AnyElement[] | null>(null);

    const [authoredElementValues, setAuthoredElementValues] = useState<AuthoredElementValues>({});

    const [startedProcessAccessKey, setStartedProcessAccessKey] = useState<string | null>(null);

    useEffect(() => {
        if (processAccessKey == null || formSlug == null) {
            return;
        }

        new BaseApiService()
            .get<RetrieveResponse>(`/api/public/forms/v1/${processAccessKey}/${formSlug}/`, {
                query: {
                    'test-claim': testClaimKey,
                },
            })
            .then((res) => {
                setData(res);
                setAllElements(flattenElements(res.layoutElement, false));
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
    }, [processAccessKey, formSlug, testClaimKey]);

    const metaDialog = useAppSelector((state) => state.app.showDialog);
    const provider = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const identityId = useAppSelector(selectIdentityId);

    const [theme, setTheme] = useState<Theme>();

    if (data == null || allElements == null) {
        return null;
    }

    console.log(data);

    const {
        layoutElement,
        node,
        process,
        version,
    } = data;

    const handleSubmitEvent = async (values: AuthoredElementValues, event: string) => {
        if (event !== SUBMIT_EVENT) {
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
                    `/api/public/forms/v1/${process?.accessKey}/${node.configuration.formSlug}/submit/`,
                    formData,
                    {
                        query: {
                            'test-claim': testClaimKey,
                        },
                    },
                );

            setStartedProcessAccessKey(startRes.startedProcessAccessKey);
        } finally {
            dispatch(clearLoadingMessage());
        }
    };

    const handleDerive = (values: AuthoredElementValues, skipErrorsForElements: string[]) => {
        return new BaseApiService()
            .post<AuthoredElementValues, ElementDerivationResponse>(`/api/public/forms/v1/${processAccessKey}/${formSlug}/derive/`, values, {
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

    return (
        <ThemeProvider theme={baseTheme}>
            <SnackbarProvider>
                <MetaElement
                    faviconUrl={'' /* TODO: new FormApiService().getFormFaviconLink(form.form.slug, form.version.version)*/}
                    title={layoutElement.tabTitle ?? layoutElement.headline ?? ''}
                    titlePrefix={provider}
                />

                <Box
                    sx={{
                        backgroundColor: 'white',
                    }}
                    ref={scrollContainerRef}
                >
                    <FormHeaderComponent
                        form={layoutElement}
                        node={node}
                        process={process}
                        version={version}
                        onDeleteFormData={() => {
                            dispatch(setCurrentStep(0));
                            setAuthoredElementValues({});
                            setStartedProcessAccessKey(null);
                        }}
                    />

                    {
                        startedProcessAccessKey == null &&
                        <ElementDerivationContext
                            element={layoutElement}
                            authoredElementValues={authoredElementValues}
                            onAuthoredElementValuesChange={setAuthoredElementValues}
                            onEvent={handleSubmitEvent}
                            onDeriveOverride={handleDerive}
                        />
                    }
                    {
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

    /*
    const handleSetElementData = (data: AuthoredElementValues, storeData: boolean = true) => {
        setAuthoredElementValues(data);

        if (storeData && form != null) {
            // CustomerInputService
            //     .storeCustomerInput(form.form.slug, form.version.version, form.version.rootElement, data);
        }
    };

    useEffect(() => {
        dispatch(showDialog(metaDialogName ?? undefined));
    }, [metaDialogName]);

    useEffect(() => {
        if (slug == null) {
            return;
        }

        new FormApiService()
            .retrieveBySlugAndVersion(slug, version, identityId)
            .then((application) => {
                const form = formCitizenDetailsResponseDTO(application);
                // TODO: dispatch(updateLoadedForm(form));
            })
            .catch(err => {
                if (err.status === 404) {
                    dispatch(setErrorMessage({
                        status: 404,
                        message: 'Das angeforderte Formular wurde nicht gefunden.',
                    }));
                } else if (isApiError(err) && err.displayableToUser) {
                    dispatch(setErrorMessage({
                        status: err.status,
                        message: err.message,
                    }));
                } else {
                    dispatch(setErrorMessage({
                        status: 500,
                        message: 'Beim Laden des Formulars ist ein unbekannter Fehler aufgetreten.',
                    }));
                    console.error(err);
                }
            });
    }, [slug, api, identityId]);

    useEffect(() => {
        if (slug == null) {
            return;
        }

        new FormApiService()
            .getFormTheme(slug, version != null ? parseInt(version) : undefined)
            .then(setTheme)
            .catch(() => {
                // Ignore theme loading errors
            });
    }, [slug, version]);

    return null;


    const _theme = useMemo(() => {
        return createAppTheme(theme, baseTheme);
    }, [theme, baseTheme]);

    if (form == null) {
        return (
            <LoadingPlaceholder />
        );
    } else {
        const allElements = flattenElements(form.version.rootElement);
        const pageTitle = stringOrUndefined(form.version.rootElement.tabTitle) ??
            stringOrUndefined(form.version.publicTitle) ??
            stringOrUndefined(form.version.rootElement.headline) ??
            '';

        return (
            <ThemeProvider theme={_theme}>
                <SnackbarProvider>
                    <MetaElement
                        faviconUrl={new FormApiService().getFormFaviconLink(form.form.slug, form.version.version)}
                        title={pageTitle}
                        titlePrefix={provider}
                    />

                    <Box
                        sx={{
                            backgroundColor: 'white',
                        }}
                    >
                        <ViewDispatcherComponent
                            rootElement={form.version.rootElement}
                            allElements={allElements}
                            element={form.version.rootElement}
                            isBusy={false}
                            isDeriving={false}
                            mode="viewer"
                            authoredElementValues={authoredElementValues}
                            derivedData={derivedData}
                            onAuthoredElementValuesChange={(data) => handleSetElementData(data)}
                            onDerivedDataChange={setDerivedData}
                            derivationTriggerIdQueue={[]}
                            disableVisibility={false}
                        />
                    </Box>

                    <HelpDialog
                        onHide={() => dispatch(showDialog(undefined))}
                        open={metaDialog === HelpDialogId}
                        form={{} as any}
                    />

                    <PrivacyDialog
                        onHide={() => dispatch(showDialog(undefined))}
                        open={metaDialog === PrivacyDialogId}
                        form={{} as any}
                    />

                    <ImprintDialog
                        onHide={() => dispatch(showDialog(undefined))}
                        open={metaDialog === ImprintDialogId}
                        form={{} as any}
                    />

                    <AccessibilityDialog
                        onHide={() => dispatch(showDialog(undefined))}
                        open={metaDialog === AccessibilityDialogId}
                        form={{} as any}
                    />
                </SnackbarProvider>
            </ThemeProvider>
        );
    }
     */
}
