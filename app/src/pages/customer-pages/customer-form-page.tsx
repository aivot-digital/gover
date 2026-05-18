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
import {flattenElements, flattenElementsWithParents} from '../../utils/flatten-elements';
import {RootComponentFooter} from '../../components/form/root-component-footer';
import {ElementDerivationContext} from '../../modules/elements/components/element-derivation-context';
import {SUBMIT_EVENT} from '../../components/form/root.component.view';
import {FileUploadElementItem, isFileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {walkAuthoredElementValues} from '../../utils/element-data-utils';
import {ElementType} from '../../data/element-type/element-type';
import {Submitted} from '../../components/submitted/submitted';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {IdentityIdQueryParam} from '../../modules/identity/constants/identity-id-query-param';
import {IdentityStateQueryParam} from '../../modules/identity/constants/identity-state-query-param';
import {IdentityResultState} from '../../modules/identity/enums/identity-result-state';
import {IdentityProvidersApiService} from '../../modules/identity/identity-providers-api-service';
import {extractVisibleFormSteps} from '../../utils/visible-form-steps';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {isIdentityInputFieldElement} from '../../models/elements/form/input/identity-input-field-element';
import {
    clampStepIndex,
    clearPendingIdentityInputAuthContext,
    getIdentityInputOptionForProvider,
    isElementNestedInReplicatingContainer,
    loadPendingIdentityInputAuthContext,
} from '../../utils/identity-input-field-utils';
import {DialogSearchParam, TestClaimSearchParam} from '../../modules/forms/constants/form-trigger-search-params';

interface RetrieveResponse {
    layoutElement: FormLayoutElement;
    node: ProcessNodeEntity;
    process: ProcessEntity;
    version: ProcessVersionEntity;
}

export function CustomerFormPage() {
    const baseTheme = useTheme();
    const api = useApi();

    const [searchParams, setSearchParams] = useSearchParams();
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
    const [derivedData, setDerivedData] = useState<DerivedRuntimeElementData>(createDerivedRuntimeElementData());
    const [derivedDataVersion, setDerivedDataVersion] = useState(0);
    const [pendingStepRestore, setPendingStepRestore] = useState<{
        stepId: string | null;
        stepIndex: number;
        minimumDerivedDataVersion: number;
    } | null>(null);

    const [startedProcessAccessKey, setStartedProcessAccessKey] = useState<string | null>(null);
    const handledIdentityCallbackRef = useRef<string | null>(null);

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
                setDerivedData(createDerivedRuntimeElementData());
                setDerivedDataVersion(0);
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

    const {
        layoutElement,
        node,
        process,
        version,
    } = data;

    useEffect(() => {
        if (startedProcessAccessKey != null) {
            return;
        }

        const callbackIdentityId = searchParams.get(IdentityIdQueryParam);
        if (callbackIdentityId == null || handledIdentityCallbackRef.current === callbackIdentityId) {
            return;
        }

        handledIdentityCallbackRef.current = callbackIdentityId;

        const callbackStateRaw = searchParams.get(IdentityStateQueryParam);
        const callbackState = callbackStateRaw != null ? parseInt(callbackStateRaw, 10) : NaN;
        const pendingAuthContext = loadPendingIdentityInputAuthContext();

        const cleanupCallbackState = () => {
            clearPendingIdentityInputAuthContext();
            clearIdentityCallbackSearchParams(searchParams, setSearchParams);
        };

        if (pendingAuthContext == null) {
            cleanupCallbackState();
            return;
        }

        if (Number.isNaN(callbackState) || callbackState !== IdentityResultState.Success) {
            dispatch(showErrorSnackbar('Die Identifizierung konnte nicht abgeschlossen werden.'));
            cleanupCallbackState();
            return;
        }

        let isCancelled = false;

        IdentityProvidersApiService
            .fetchIdentity(callbackIdentityId)
            .then((identityData) => {
                if (isCancelled) {
                    return;
                }

                const flattenedElementsWithParents = flattenElementsWithParents(layoutElement, [], false);
                const sourceEntry = flattenedElementsWithParents
                    .find(({element}) => element.id === pendingAuthContext.elementId);

                if (sourceEntry == null || !isIdentityInputFieldElement(sourceEntry.element)) {
                    dispatch(showErrorSnackbar('Das verknuepfte Identitaetselement konnte nicht gefunden werden.'));
                    cleanupCallbackState();
                    return;
                }

                const sourceElement = sourceEntry.element;
                const selectedOption = getIdentityInputOptionForProvider(
                    sourceElement,
                    pendingAuthContext.optionIdentityProviderKey ?? identityData.providerKey,
                );

                let nextAuthoredValues: AuthoredElementValues = {
                    ...(pendingAuthContext.authoredElementValues ?? {}),
                    [sourceElement.id]: {
                        identityProviderKey: identityData.providerKey,
                        identityAttributes: identityData.attributes,
                    },
                };

                nextAuthoredValues = applyIdentityInputAttributeMappings(
                    flattenedElementsWithParents,
                    sourceElement.id,
                    nextAuthoredValues,
                    selectedOption?.attributeMappings ?? [],
                    identityData.attributes,
                );

                setAuthoredElementValues(nextAuthoredValues);

                const nextDerivedDataVersion = derivedDataVersion + 1;
                handleDerive(nextAuthoredValues, ['ALL'])
                    .then((nextDerivedData) => {
                        if (isCancelled) {
                            return;
                        }

                        setDerivedData(nextDerivedData);
                        setDerivedDataVersion((current) => current + 1);
                        setPendingStepRestore({
                            stepId: pendingAuthContext.stepId,
                            stepIndex: pendingAuthContext.stepIndex,
                            minimumDerivedDataVersion: nextDerivedDataVersion,
                        });

                        cleanupCallbackState();
                    })
                    .catch((error) => {
                        console.error('Error deriving restored identity data:', error);
                        if (!isCancelled) {
                            dispatch(showErrorSnackbar('Die Formulardaten konnten nach der Identifizierung nicht aktualisiert werden.'));
                            cleanupCallbackState();
                        }
                    });
            })
            .catch((error) => {
                console.error('Error restoring identity callback data:', error);
                if (!isCancelled) {
                    dispatch(showErrorSnackbar('Die Identifizierungsdaten konnten nicht geladen werden.'));
                    cleanupCallbackState();
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [
        authoredElementValues,
        derivedDataVersion,
        dispatch,
        layoutElement,
        searchParams,
        setSearchParams,
        startedProcessAccessKey,
    ]);

    useEffect(() => {
        if (pendingStepRestore == null || derivedDataVersion < pendingStepRestore.minimumDerivedDataVersion) {
            return;
        }

        const visibleSteps = extractVisibleFormSteps(layoutElement.children, derivedData);
        const restoredStepIndex = pendingStepRestore.stepId == null ?
            -1 :
            visibleSteps.findIndex((step) => step.id === pendingStepRestore.stepId);

        dispatch(setCurrentStep(
            clampStepIndex(
                restoredStepIndex >= 0 ? restoredStepIndex : pendingStepRestore.stepIndex,
                visibleSteps.length,
            ),
        ));
        setPendingStepRestore(null);
    }, [pendingStepRestore, derivedData, derivedDataVersion, dispatch, layoutElement.children]);

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
                            setDerivedData(createDerivedRuntimeElementData());
                            setDerivedDataVersion(0);
                            setPendingStepRestore(null);
                            setStartedProcessAccessKey(null);
                        }}
                    />

                    {
                        startedProcessAccessKey == null &&
                        <ElementDerivationContext
                            element={layoutElement}
                            authoredElementValues={authoredElementValues}
                            derivedData={derivedData}
                            onDerivedDataChange={(nextDerivedData) => {
                                setDerivedData(nextDerivedData);
                                setDerivedDataVersion((current) => current + 1);
                            }}
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
}

function clearIdentityCallbackSearchParams(
    searchParams: URLSearchParams,
    setSearchParams: ReturnType<typeof useSearchParams>[1],
): void {
    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete(IdentityIdQueryParam);
    nextSearchParams.delete(IdentityStateQueryParam);

    setSearchParams(nextSearchParams, {
        replace: true,
    });
}

function applyIdentityInputAttributeMappings(
    flattenedElementsWithParents: ReturnType<typeof flattenElementsWithParents>,
    sourceElementId: string,
    authoredElementValues: AuthoredElementValues,
    attributeMappings: Array<{
        fromIdentityProviderAttribute: string | null | undefined;
        toFormElementWithId: string | null | undefined;
    }>,
    identityAttributes: Record<string, string>,
): AuthoredElementValues {
    const eligibleTargetIds = new Set(
        flattenedElementsWithParents
            .filter(({element, parents}) => (
                element.id !== sourceElementId &&
                isAnyInputElement(element) &&
                !isElementNestedInReplicatingContainer(parents)
            ))
            .map(({element}) => element.id),
    );

    const nextAuthoredElementValues = {
        ...authoredElementValues,
    };

    for (const mapping of attributeMappings) {
        const attributeKey = mapping.fromIdentityProviderAttribute;
        const targetElementId = mapping.toFormElementWithId;
        if (attributeKey == null || targetElementId == null || !eligibleTargetIds.has(targetElementId)) {
            continue;
        }

        const mappedValue = identityAttributes[attributeKey];
        if (mappedValue == null) {
            continue;
        }

        nextAuthoredElementValues[targetElementId] = mappedValue;
    }

    return nextAuthoredElementValues;
}
