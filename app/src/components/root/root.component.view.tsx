import React, {useEffect, useMemo, useRef, useState} from 'react';
import Box from '@mui/material/Box';
import Container from '@mui/material/Container';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import Stepper from '@mui/material/Stepper';
import {useTheme} from '@mui/material/styles';
import {type RootElement} from '../../models/elements/root-element';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import Chip from '@mui/material/Chip';
import Tooltip from '@mui/material/Tooltip';
import {Submitted} from '../submitted/submitted';
import {SummaryAttachmentsTooLargeKey} from '../summary/summary.component.view';
import {SubmitPaymentDataKey} from '../submit/submit.component.view';
import {ProcessingDataLoaderComponentView} from '../processing-data-loader/processing-data-loader.component.view';
import {CustomerInputService} from '../../services/customer-input-service';
import {RootComponentFooter} from './root-component-footer';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedForm} from '../../slices/app-slice';
import {
    nextStep,
    previousStep,
    selectCurrentStep,
    selectUpcomingStepDirection,
    setCurrentStep,
} from '../../slices/stepper-slice';
import {ElementType} from '../../data/element-type/element-type';
import {removeLoadingSnackbar, showErrorSnackbar, showLoadingSnackbar} from '../../slices/snackbar-slice';
import {type FileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {type BaseViewProps} from '../../views/base-view';
import {withAsyncWrapper} from '../../utils/with-async-wrapper';
import GppGoodOutlinedIcon from '@mui/icons-material/GppGoodOutlined';
import {CustomStep} from '../custom-step/custom-step';
import {Api, useApi} from '../../hooks/use-api';
import {useSearchParams} from 'react-router-dom';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {SubmissionListResponseDTO} from '../../modules/submissions/dtos/submission-list-response-dto';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {useSingleUpdateEffect} from '../../hooks/use-single-update-effect';
import {ApiError, isApiError} from '../../models/api-error';
import {StepElement} from '../../models/elements/steps/step-element';
import {IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {SummaryStepElement} from '../../models/elements/steps/summary-step-element';
import {SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {
    AuthoredElementValues,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
    isDerivedRuntimeElementData,
} from '../../models/element-data';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {SubmittedStepElement} from '../../models/elements/steps/submitted-step-element';
import {collectErrors, ErrorAlert} from '../error-alert/error-alert';
import {ElementWithParents, flattenElementsWithParents} from '../../utils/flatten-elements';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {resolveVisibility, walkAuthoredElementValues} from '../../utils/element-data-utils';
import {isElementChangedByTrigger} from '../../utils/element-reference-utils';
import {IdentityCustomerInputKey} from '../../modules/identity/constants/identity-customer-input-key';
import {IdentityData} from '../../modules/identity/models/identity-data';
import {CustomerInputLoader} from '../../dialogs/customer-input-loader/customer-input-loader';
import {addDerivationLogItems} from '../../slices/logging-slice';
import {RootComponentHeader} from './root-component-header';
import {FormEntity} from '../../modules/forms/entities/form-entity';
import {FormVersionEntity} from '../../modules/forms/entities/form-version-entity';
import {FormApiService} from '../../modules/forms/services/form-api-service';

type AnyStepElement =
    StepElement
    | IntroductionStepElement
    | SummaryStepElement
    | SubmitStepElement
    | SubmittedStepElement;

const SubmissionIdSearchParam = 'submissionId';

const checkTimeoutMinMs = 1000;

function extractVisibleSteps(children: StepElement[] | null | undefined, derivedData: DerivedRuntimeElementData): AnyStepElement[] {
    if (children == null) {
        return [];
    }

    const visibleChildren = [];
    for (const child of children) {
        if (resolveVisibility(child, derivedData)) {
            visibleChildren.push(child);
        }
    }

    return visibleChildren;
}

function extractCurrentStep(currentStep: number, allVisibleSteps: AnyStepElement[]) {
    if (currentStep < 0 || currentStep >= allVisibleSteps.length) {
        return null;
    }
    return allVisibleSteps[currentStep];
}

function clearDerivedErrorsRecursively(derivedData: DerivedRuntimeElementData): DerivedRuntimeElementData {
    return {
        ...derivedData,
        elementStates: Object.fromEntries(
            Object.entries(derivedData.elementStates).map(([elementId, state]) => [
                elementId,
                {
                    ...state,
                    error: null,
                    subStates: state?.subStates?.map((subState) => clearDerivedErrorsRecursively({
                        effectiveValues: {},
                        elementStates: subState ?? {},
                    }).elementStates) ?? null,
                },
            ]),
        ),
    };
}

function withDerivedDataOverride(
    derivedData: DerivedRuntimeElementData,
    elementId: string,
    options: {
        error?: string | null;
        value?: any;
        valueSource?: 'Authored' | 'Derived';
    },
): DerivedRuntimeElementData {
    return {
        ...derivedData,
        effectiveValues: options.value === undefined ? derivedData.effectiveValues : {
            ...derivedData.effectiveValues,
            [elementId]: options.value,
        },
        elementStates: {
            ...derivedData.elementStates,
            [elementId]: {
                visible: derivedData.elementStates[elementId]?.visible ?? true,
                override: derivedData.elementStates[elementId]?.override ?? null,
                subStates: derivedData.elementStates[elementId]?.subStates ?? null,
                valueSource: options.valueSource ?? derivedData.elementStates[elementId]?.valueSource ?? 'Derived',
                error: options.error ?? null,
            },
        },
    };
}


export function RootComponentView(props: BaseViewProps<RootElement, void>) {
    const {
        rootElement,
        allElements,
        element,
        scrollContainerRef,
        mode,
        authoredElementValues,
        derivedData,
        onAuthoredElementValuesChange,
        onElementBlur,
        onDerivedDataChange,
        disableVisibility,
    } = props;

    const api = useApi();
    const theme = useTheme();
    const [searchParams, setSearchParams] = useSearchParams();

    const dispatch = useAppDispatch();

    // TODO: internalize these information
    const form = useAppSelector(selectLoadedForm);
    const adminSettings = useAppSelector((state) => state.adminSettings);
    const currentStep = useAppSelector(selectCurrentStep);
    const upcomingStepDirection = useAppSelector(selectUpcomingStepDirection);

    const [derivationTriggerIdQueue, setDerivationTriggerIdQueue] = useState<string[]>([]);

    const [isBusy, setIsBusy] = useState(false);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const [submission, setSubmission] = useState<SubmissionListResponseDTO>();

    // The `isDeriving` state is used to indicate if the form is currently deriving its state.
    // Deriving is active when there are items in the `derivationTriggerIdQueue`.
    const isDeriving = useMemo(() => {
        return derivationTriggerIdQueue.length > 0;
    }, [derivationTriggerIdQueue]);

    // Deconstruct the element to get the steps.
    const {
        children,
    } = element;

    // Collecting all steps including the fixed steps
    const allVisibleSteps = useMemo(() => extractVisibleSteps(children as StepElement[], derivedData), [children, derivedData]);

    // Extract the current step based on the current step index and all visible steps
    const currentStepElement = useMemo(() => extractCurrentStep(currentStep, allVisibleSteps), [currentStep, allVisibleSteps]);

    // Determine the total number of steps
    const totalStepCount = useMemo(() => allVisibleSteps.length, [allVisibleSteps]);

    // Create a ref for each step to allow scrolling to the step when it becomes active.
    const stepRefs = useRef(allVisibleSteps.map(() => React.createRef<HTMLDivElement>()));

    // Check if a form derivation is necessary, when the form is loaded and some of the steps have derivable aspects.
    useSingleUpdateEffect(() => {
        if (element.children == null) {
            return;
        }

        const stepsWithDerivableAspectsExist = element
            .children
            .some(step => hasDerivableAspects(step, true));

        if (stepsWithDerivableAspectsExist) {
            determineFormState({
                triggeringElementIds: [],
                performValidation: false,
                forceAll: true,
                lookahead: false,
            });
        }
    }, [element]);

    // Set basic submission data from query params.
    // This is used when redirected from payment provider.
    useEffect(() => {
        const submissionId = searchParams.get(SubmissionIdSearchParam);

        if (submissionId == null || isStringNullOrEmpty(submissionId)) {
            return;
        }

        setSubmission({
            startedProcessAccessKeys: [submissionId],
        });

        dispatch(setCurrentStep(totalStepCount));
    }, [searchParams, totalStepCount]);

    // Handle the derivation trigger queue.
    // This effect runs whenever there are items in the derivationTriggerIdQueue.
    useEffect(() => {
        if (derivationTriggerIdQueue.length === 0) {
            return;
        }

        determineFormState({
            triggeringElementIds: derivationTriggerIdQueue,
            performValidation: false,
            forceAll: false,
            lookahead: false,
        })
            .finally(() => {
                setDerivationTriggerIdQueue(prevState => {
                    const copy = [...prevState];
                    copy.shift();
                    return copy;
                });
            });
    }, [derivationTriggerIdQueue]);

    const handleNextStep = async () => {
        // Check if the form is loaded. If not, this handler should not run.
        if (form == null) {
            return;
        }

        // Check if the current step is valid
        const currentPageValid = await determineFormState({
            triggeringElementIds: [],
            performValidation: true,
            forceAll: false,
            lookahead: true,
        });

        if (!currentPageValid) {
            return;
        }

        // Check if introduction step
        if (currentStep === 0) {
            dispatch(nextStep());
        }

        // Check if summary step
        else if (currentStep === (totalStepCount - 2)) {
            const uploadSizeValid = await determineUploadSize();
            if (!uploadSizeValid) {
                return;
            }

            const priceCalculated = await calculatePrice();
            if (!priceCalculated) {
                return;
            }

            dispatch(nextStep());
        }

        // Check if submit step
        else if (currentStep === (totalStepCount - 1)) {
            const formsApiService = new FormApiService();

            setIsSubmitting(true);

            const identityId = (authoredElementValues[IdentityCustomerInputKey] as IdentityData | undefined | null)?.identityId;

            let submitResponse: SubmissionListResponseDTO | null = null;
            try {
                submitResponse = await formsApiService
                    .submit({
                        formId: form.form.id,
                        version: form.version.version,
                    }, element, authoredElementValues, identityId);
            } catch (error: ApiError | any) {
                if (isApiError(error) || 'status' in error) {
                    if (isApiError(error) && error.details != null && typeof error.details === 'object' && isDerivedRuntimeElementData(error.details)) {
                        onDerivedDataChange?.(error.details as DerivedRuntimeElementData);
                    } else {
                        switch (error.status) {
                            case 406:
                                dispatch(showErrorSnackbar('Der Antrag konnte nicht korrekt übertragen werden. In den von Ihnen hochgeladenen Dokumenten wurde Schadsoftware erkannt.'));
                                break;
                            default:
                                dispatch(showErrorSnackbar('Der Antrag konnte nicht korrekt übertragen werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                                break;
                        }
                    }
                } else {
                    dispatch(showErrorSnackbar('Der Antrag konnte nicht korrekt übertragen werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                }
            } finally {
                setIsSubmitting(false);
            }

            if (submitResponse != null) {
                setSubmission(submitResponse);
                dispatch(nextStep());
                // Clear possible identity data from search params
                setSearchParams({}, {
                    replace: true,
                });
                CustomerInputService.cleanCustomerInput(form.form.slug, form.version.version);
            }
        }

        // Handle default step
        else {
            dispatch(nextStep());
        }
    };

    const handlePreviousStep = () => {
        dispatch(previousStep());
        onDerivedDataChange?.(clearDerivedErrorsRecursively(derivedData));
    };

    const determineFormState = async (options: {
        performValidation: boolean;
        triggeringElementIds: string[];
        forceAll: boolean;
        lookahead: boolean;
    }): Promise<boolean> => {
        // Can't derive state for null application
        if (form == null || element == null || children == null || currentStepElement == null) {
            return false;
        }

        console.group('Start determining form state with options:', options);

        // Get the id of the current step and the next step.
        const currentStepId: string = currentStepElement.id;
        const nextStepId: string | undefined = allVisibleSteps[currentStep + 1]?.id;

        console.log('Current step id:', currentStepId);
        console.log('Next step id:', nextStepId);

        // Get the list of all steps, which are relevant for the derivation.
        // This includes the introduction step, all children that do not have derivable aspects, the summary step and the submit step.
        const allStepsWithoutDerivableAspects = [
            ...children
                .filter(step => !hasDerivableAspects(step, true))
                .map(step => step.id),
        ];

        const allSteps = [
            ...children
                .map(step => step.id),
        ];

        console.log('Found steps without derivable aspects:', allStepsWithoutDerivableAspects);

        // Determine the list of step ids that should be skipped for errors, visibilities, overrides and values.

        const skipErrorsForStepIds = allSteps
            .filter((stepId) => stepId != currentStepId);

        console.log('Skip errors for step ids:', skipErrorsForStepIds);

        const skipVisibilitiesForStepIds = allStepsWithoutDerivableAspects
            .filter((stepId) => stepId != currentStepId && (!options.lookahead || stepId != nextStepId));

        console.log('Skip visibilities for step ids:', skipVisibilitiesForStepIds);

        const skipOverridesForStepIds = allStepsWithoutDerivableAspects
            .filter((stepId) => stepId != currentStepId && (!options.lookahead || stepId != nextStepId));

        console.log('Skip overrides for step ids:', skipOverridesForStepIds);

        const skipValuesForStepIds = allStepsWithoutDerivableAspects
            .filter((stepId) => stepId != currentStepId && (!options.lookahead || stepId != nextStepId));

        console.log('Skip values for step ids:', skipValuesForStepIds);

        // Check if we should perform validation or visibility derivation.
        const doNotPerformErrorDerivation = !options.performValidation || adminSettings.disableValidation;
        const doNotPerformVisibilityDerivation = adminSettings.disableVisibility;

        // If no derivation is triggered were running in blocking mode 'busy'.
        const mode = (options.triggeringElementIds ?? []).length > 0 ? 'deriving' : 'busy';

        console.log('Determined mode:', mode);

        // Set a busy state if we are in deriving mode or busy mode.
        // If we are in deriving mode, we show a loading snackbar.
        if (mode === 'busy') {
            setIsBusy(true);
        } else {
            dispatch(showLoadingSnackbar('Berechnungen werden durchgeführt…'));
        }

        try {
            const derivationResult = await withAsyncWrapper({
                desiredMinRuntime: 600,
                main: () => new FormApiService().deriveForm(
                    form.form.slug,
                    form.version.version,
                    authoredElementValues,
                    {
                        skipErrorsFor: options.forceAll ? [] : (doNotPerformErrorDerivation ? ['ALL'] : skipErrorsForStepIds),
                        skipVisibilitiesFor: options.forceAll ? [] : (doNotPerformVisibilityDerivation ? ['ALL'] : skipVisibilitiesForStepIds),
                        skipValuesFor: options.forceAll ? [] : (skipValuesForStepIds),
                        skipOverridesFor: options.forceAll ? [] : (skipOverridesForStepIds),
                    },
                ),
            }).finally(() => {
                if (mode === 'busy') {
                    setIsBusy(false);
                } else {
                    dispatch(removeLoadingSnackbar());
                }
            });

            dispatch(addDerivationLogItems(derivationResult.logItems));
            onDerivedDataChange?.(derivationResult.elementData);

            console.groupEnd();

            return collectErrors(currentStepElement, authoredElementValues, derivationResult.elementData).length === 0 || adminSettings.disableValidation;
        } catch (err) {
            console.error(err);
            dispatch(showErrorSnackbar('Dynamische Funktionen konnten nicht ausgewertet werden.'));

            console.groupEnd();

            return adminSettings.disableValidation;
        }
    };

    const determineUploadSize = (): Promise<boolean> => {
        return withAsyncWrapper<undefined, string | null>({
            desiredMinRuntime: checkTimeoutMinMs,
            runtimeCallback: setIsLoading,
            main: () => determineUploadSizeError(
                element,
                authoredElementValues,
                form!.form,
                form!.version,
                api,
            ),
        })
            .then((errorMessage) => {
                if (errorMessage != null) {
                    onDerivedDataChange?.(withDerivedDataOverride(
                        derivedData,
                        SummaryAttachmentsTooLargeKey,
                        {
                            error: errorMessage,
                        },
                    ));
                    return false;
                } else {
                    onDerivedDataChange?.(withDerivedDataOverride(
                        derivedData,
                        SummaryAttachmentsTooLargeKey,
                        {
                            error: null,
                        },
                    ));
                    return true;
                }
            })
            .catch((error) => {
                console.error(error);
                dispatch(showErrorSnackbar('Der Maximalgröße der Anlagen konnte nicht korrekt überprüft werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                return false;
            });
    };

    // Calculates the price for the current form and customer input.
    // Returns false, if the price could not be calculated.
    const calculatePrice = (): Promise<boolean> => {
        if (form == null) {
            return Promise.resolve(false);
        }

        return withAsyncWrapper<undefined, boolean>({
            desiredMinRuntime: checkTimeoutMinMs,
            runtimeCallback: setIsLoading,
            main: async () => {
                try {
                    const calculatedCosts = await new FormApiService()
                        .calculateCosts(form.form.slug, form.version.version, authoredElementValues);
                    onDerivedDataChange?.(withDerivedDataOverride(
                        derivedData,
                        SubmitPaymentDataKey,
                        {
                            value: calculatedCosts,
                            error: null,
                            valueSource: 'Derived',
                        },
                    ));
                } catch (error: any) {
                    console.error(error);
                    dispatch(showErrorSnackbar('Die Kosten konnten nicht korrekt berechnet werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                    // Return false to signal, that an error occurred.
                    return false;
                }
                return true;
            },
        });
    };

    const handleElementDataChange = (nextAuthoredElementValues: AuthoredElementValues, triggeringElementIds: string[]): void => {
        if (currentStepElement == null || children == null) {
            return;
        }

        console.group('Start element data change', nextAuthoredElementValues, triggeringElementIds);

        onAuthoredElementValuesChange(nextAuthoredElementValues, []);

        const flatCurrentElements = flattenElementsWithParents(currentStepElement, [], false);
        const flatChildren = children.map((e, i) => ({
            element: e,
            parents: [element],
            index: i,
        }));

        const allElementsToConsider: ElementWithParents[] = [
            ...flatCurrentElements,
            ...flatChildren,
        ].filter(e => {
            return e.element.visibility != null ||
                e.element.override != null ||
                (isAnyInputElement(e.element) && e.element.value != null);
        });

        const relevantTriggeringElementIds = triggeringElementIds
            .filter((id) => allElementsToConsider.some((element) => isElementChangedByTrigger(element, id)));

        if (relevantTriggeringElementIds.length > 0) {
            console.log('Found relevant triggering element ids:', relevantTriggeringElementIds);

            setDerivationTriggerIdQueue((prev) => [
                ...prev,
                ...relevantTriggeringElementIds,
            ]);
        } else {
            // TODO: @Mo please check if logic is still relevant and migrate to new data structure if that's the case
            /*console.log('No relevant triggering element ids found');

            if (derivationTriggerIdQueue.length > 0) {
                console.log('Setting element data buffer to current element data as no relevant triggering element ids were found, but derivation queue is not empty');
                elementDataBufferRef.current = elementData;
            } else {
                console.log('Setting element data buffer to undefined as no relevant triggering element ids were found and derivation queue is empty');
                elementDataBufferRef.current = undefined;
            }*/
        }

        console.groupEnd();
    };

    return (
        <>
            {
                form != null &&
                <RootComponentHeader
                    form={form.form}
                    version={form.version}
                    onDeleteFormData={() => {
                        onAuthoredElementValuesChange({}, []);
                        onDerivedDataChange?.(createDerivedRuntimeElementData());
                        dispatch(setCurrentStep(0));
                    }}
                />
            }

            {
                form != null &&
                <CustomerInputLoader
                    form={form.form}
                    version={form.version}
                    onElementDataLoad={(data) => {
                        onAuthoredElementValuesChange(data, []);
                        onDerivedDataChange?.(createDerivedRuntimeElementData());
                    }}
                    isBusy={isBusy || isDeriving}
                />
            }

            <main role="main">
                <span
                    aria-live="polite"
                    className="visually-hidden"
                >
                    {/* TODO: Waits for final QS by JP
                        isDeriving != null &&
                        isDeriving &&
                        'Berechnungen werden durchgeführt'
                    */}
                    {
                        isDeriving != null &&
                        !isDeriving &&
                        'Berechnungen fertig. Eingabebereit'
                    }
                </span>

                <Container
                    sx={{
                        mt: 5,
                        mb: 5,
                        /* Remove spacing for richtext components that are immediately preceded by a headline component */
                        '& .MuiGrid-item:has(.headline-component-content) + .MuiGrid-item.MuiGrid-grid-md-12:has(.richtext-component-content)': {
                            paddingTop: 0,
                        },
                        '& .MuiGrid-item:has(.headline-component-content) + .MuiGrid-item.MuiGrid-grid-md-12:has(.richtext-component-content) .richtext-component-content': {
                            marginTop: 0,
                        },
                    }}
                >
                    {
                        currentStep < totalStepCount &&
                        <Stepper
                            sx={{
                                mt: 8,
                                mb: 10,
                                ml: '20px',
                                [theme.breakpoints.down('md')]: {
                                    mt: 5,
                                    mb: 6,
                                    ml: 0,
                                },
                            }}
                            activeStep={currentStep}
                            orientation="vertical"
                        >
                            {
                                allVisibleSteps
                                    .map((step, index) => (
                                        <CustomStep
                                            key={index}
                                            step={step}
                                            stepIndex={index}
                                            isFirstStep={index === 0}
                                            isLastStep={index === allVisibleSteps.length - 1}
                                            isSubmitStep={step.type === ElementType.SubmitStep}
                                            onNext={handleNextStep}
                                            onPrevious={handlePreviousStep}
                                            active={currentStep === index}
                                            navDirection={upcomingStepDirection}
                                            stepRefs={stepRefs}
                                            scrollContainerRef={scrollContainerRef}
                                            isBusy={isBusy}
                                            isDeriving={isDeriving ?? false}
                                        >
                                            <ViewDispatcherComponent
                                                rootElement={rootElement}
                                                allElements={allElements}
                                                element={step}
                                                isBusy={isBusy}
                                                isDeriving={isDeriving ?? false}
                                                mode={mode}
                                                authoredElementValues={authoredElementValues}
                                                derivedData={derivedData}
                                                onAuthoredElementValuesChange={handleElementDataChange}
                                                onElementBlur={onElementBlur}
                                                onDerivedDataChange={onDerivedDataChange}
                                                scrollContainerRef={scrollContainerRef}
                                                disableVisibility={disableVisibility}
                                                derivationTriggerIdQueue={derivationTriggerIdQueue}
                                            />

                                            <ErrorAlert
                                                element={step}
                                                authoredElementValues={authoredElementValues}
                                                derivedData={derivedData}
                                            />
                                        </CustomStep>
                                    ))
                            }
                        </Stepper>
                    }

                    {
                        currentStep === totalStepCount &&
                        <Stepper
                            sx={{
                                mt: 10,
                                mb: 12,
                                ml: '20px',
                                [theme.breakpoints.down('md')]: {
                                    mt: 5,
                                    mb: 6,
                                    ml: 0,
                                },
                            }}
                            orientation="vertical"
                        >
                            <CustomStep
                                stepIndex={-1}
                                isFirstStep={false}
                                isLastStep={false}
                                isSubmitStep={false}
                                step={generateElementWithDefaultValues(ElementType.SubmittedStep) as SubmittedStepElement}
                                title="Ihr Antrag wurde erfolgreich eingereicht"
                                active
                                navDirection={upcomingStepDirection}
                                stepRefs={stepRefs}
                                scrollContainerRef={scrollContainerRef}
                                isBusy={isBusy}
                                isDeriving={isDeriving ?? false}
                            >
                                {
                                    submission != null &&
                                    form != null &&
                                    <Submitted
                                        submission={submission}
                                        form={form.form}
                                        version={form.version}
                                    />
                                }
                            </CustomStep>
                        </Stepper>
                    }
                </Container>

                {
                    currentStep < totalStepCount &&
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
                        <Tooltip
                            title="Ihre Angaben werden lokal auf diesem Gerät zwischengespeichert. Das Löschen Ihrer lokalen Daten oder Cookies kann einen Verlust Ihres Entwurfs zur Folge haben."
                            arrow
                        >
                            <Chip
                                sx={{
                                    pl: 1,
                                    pr: 1,
                                    cursor: 'help',
                                }}
                                icon={<Box
                                    component="span"
                                    sx={{
                                        color: (theme) => theme.palette.primary.main,
                                        transform: 'translateY(2px)',
                                    }}
                                ><GppGoodOutlinedIcon fontSize="small"/></Box>}
                                label="Lokal auf Ihrem Gerät zwischengespeichert"
                                variant="outlined"
                            />
                        </Tooltip>
                    </Container>
                }
            </main>

            {
                form != null &&
                <RootComponentFooter
                    form={form.form}
                    version={form.version}
                />
            }

            <Dialog
                open={isLoading}
                fullWidth
            >
                <DialogContent>
                    <ProcessingDataLoaderComponentView
                        message="Ihre angegebenen Daten werden abschließend noch einmal überprüft"
                    />
                </DialogContent>
            </Dialog>

            <Dialog
                open={isSubmitting}
                fullWidth
            >
                <DialogContent>
                    <ProcessingDataLoaderComponentView
                        message="Ihr Antrag wird sicher an die zuständige bzw. bewirtschaftende Stelle übermittelt"
                    />
                </DialogContent>
            </Dialog>
        </>
    );
}

/**
 * Returns null if the file sizes are within the limits, returns an error message if the total file size exceeds the maximum allowed size.
 */
async function determineUploadSizeError(element: RootElement, elementData: AuthoredElementValues, form: FormEntity, version: FormVersionEntity, api: Api): Promise<string | null> {
    let totalFileSize = 0;

    walkAuthoredElementValues(
        element,
        elementData,
        (element, value) => {
            if (element.type === ElementType.FileUpload && value != null) {
                totalFileSize += (value as FileUploadElementItem[])
                    .reduce((acc, item) => acc + item.size, 0);
            }
        },
    );

    if (totalFileSize === 0) {
        // No file uploads, so no size check needed
        return null;
    }

    const {maxFileSize} = await new FormApiService()
        .getMaxFileSize(form.slug, version.version);

    const maxFileSizeBytes = maxFileSize * 1000 * 1000;

    if (totalFileSize > maxFileSizeBytes) {
        return `Die Gesamtgröße der von Ihnen hinzugefügten Anlagen überschreitet das Maximum von ${maxFileSize.toFixed(0)} Megabyte.`;
    }

    return null;
}
