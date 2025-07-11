import React, {useEffect, useMemo, useRef, useState} from 'react';
import Container from '@mui/material/Container';
import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import Stepper from '@mui/material/Stepper';
import useTheme from '@mui/material/styles/useTheme';
import {type RootElement} from '../../models/elements/root-element';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import Chip from '@mui/material/Chip';
import Tooltip from '@mui/material/Tooltip';
import {Submitted} from '../submitted/submitted';
import {SummaryAttachmentsTooLargeKey} from '../summary/summary.component.view';
import {SubmitPaymentDataKey} from '../submit/submit.component.view';
import {ProcessingDataLoaderComponentView} from '../processing-data-loader/processing-data-loader.component.view';
import {CustomerInputService} from '../../services/customer-input-service';
import {AppFooter} from '../app-footer/app-footer';
import {AppMode} from '../../data/app-mode';
import {AppHeader} from '../app-header/app-header';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {addError, clearErrors, dequeueDerivationTriggerId, selectLoadedForm, updateCustomerInput} from '../../slices/app-slice';
import {nextStep, previousStep, selectCurrentStep, selectUpcomingStepDirection, setCurrentStep} from '../../slices/stepper-slice';
import {ElementType} from '../../data/element-type/element-type';
import {removeLoadingSnackbar, showErrorSnackbar, showLoadingSnackbar} from '../../slices/snackbar-slice';
import {type FileUploadElementItem, isFileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import {type BaseViewProps} from '../../views/base-view';
import {withAsyncWrapper} from '../../utils/with-async-wrapper';
import GppGoodOutlinedIcon from '@mui/icons-material/GppGoodOutlined';
import {CustomStep} from '../custom-step/custom-step';
import {useApi} from '../../hooks/use-api';
import {useSearchParams} from 'react-router-dom';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {FormsApiService} from '../../modules/forms/forms-api-service';
import {SubmissionListResponseDTO} from '../../modules/submissions/dtos/submission-list-response-dto';
import {SubmissionStatus} from '../../modules/submissions/enums/submission-status';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {useSingleUpdateEffect} from '../../hooks/use-single-update-effect';
import {ApiError, isApiError} from '../../models/api-error';
import {selectIdentityId} from '../../slices/identity-slice';
import {StepElement} from '../../models/elements/steps/step-element';
import {IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {SummaryStepElement} from '../../models/elements/steps/summary-step-element';
import {SubmitStepElement} from '../../models/elements/steps/submit-step-element';
import {ElementData} from '../../models/element-data';
import {generateElementWithDefaultValues} from '../../utils/generate-element-with-default-values';
import {SubmittedStepElement} from '../../models/elements/steps/submitted-step-element';
import {collectErrors, ErrorAlert} from '../error-alert/error-alert';
import {flattenElements} from '../../utils/flatten-elements';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';

const SubmissionIdSearchParam = 'submissionId';

const checkTimeoutMinMs = 1000;

export function RootComponentView(props: BaseViewProps<RootElement, void>) {
    const {
        allElements,
        element,
        scrollContainerRef,
        mode,
        elementData,
        onElementDataChange,
        onElementBlur,
        disableVisibility,
        derivationTriggerIdQueue,
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

    const identityId = useAppSelector(selectIdentityId);

    const [isBusy, setIsBusy] = useState(false);
    const [isDeriving, setIsDeriving] = useState<boolean | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    const [submission, setSubmission] = useState<SubmissionListResponseDTO>();

    // Deconstruct the element to get the steps.
    const {
        children,
        introductionStep,
        summaryStep,
        submitStep,
    } = element;

    // Collecting all steps including the fixed steps
    const allVisibleSteps: (StepElement | IntroductionStepElement | SummaryStepElement | SubmitStepElement)[] = useMemo(() => {
        if (children == null || introductionStep == null || summaryStep == null || submitStep == null) {
            return [];
        }

        return [
            introductionStep,
            ...children.filter((stepElement) => {
                return elementData[stepElement.id]?.isVisible ?? true;
            }),
            summaryStep,
            submitStep,
        ];
    }, [children, introductionStep, summaryStep, submitStep]);

    const currentStepElement = useMemo(() => {
        if (currentStep < 0 || currentStep >= allVisibleSteps.length) {
            return null;
        }
        return allVisibleSteps[currentStep];
    }, [currentStep, allVisibleSteps]);

    // Determine the total number of steps
    const totalStepCount = useMemo(() => {
        return allVisibleSteps.length;
    }, [allVisibleSteps]);

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
            determineFormState(false, 'busy', true);
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
            id: submissionId,
            assigneeId: '',
            destinationId: 0,
            destinationSuccess: false,
            fileNumber: '',
            formId: 0,
            isTestSubmission: false,
            status: SubmissionStatus.OpenForManualWork,
            created: new Date().toISOString(),
        });

        dispatch(setCurrentStep(totalStepCount));
    }, [searchParams, totalStepCount]);

    useEffect(() => {
        if (derivationTriggerIdQueue.length === 0) {
            return;
        }

        determineFormState(false, 'deriving')
            .finally(() => {
                dispatch(dequeueDerivationTriggerId());
            });
    }, [derivationTriggerIdQueue]);

    const handleNextStep = async () => {
        // Check if the form is loaded. If not, this handler should not run.
        if (form == null) {
            return;
        }

        // Clear existing errors
        dispatch(clearErrors());

        // Check if the current step is valid
        const currentPageValid = await determineFormState(true, 'busy');

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

            const formsApiService = new FormsApiService(api);

            setIsSubmitting(true);

            let submitResponse: SubmissionListResponseDTO | null = null;
            try {
                submitResponse = await formsApiService
                    .submit(form.id, elementData, identityId);
            } catch (error: ApiError | any) {
                if (isApiError(error) || 'status' in error) {
                    switch (error.status) {
                        case 406:
                            dispatch(showErrorSnackbar('Der Antrag konnte nicht korrekt übertragen werden. In den von Ihnen hochgeladenen Dokumenten wurde Schadsoftware erkannt.'));
                            break;
                        default:
                            dispatch(showErrorSnackbar('Der Antrag konnte nicht korrekt übertragen werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                            break;
                    }
                } else {
                    dispatch(showErrorSnackbar('Der Antrag konnte nicht korrekt übertragen werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                }
            } finally {
                setIsSubmitting(false);
            }

            if (submitResponse != null) {
                setSubmission({
                    id: submitResponse.id,
                    assigneeId: '',
                    created: '',
                    destinationId: 0,
                    destinationSuccess: false,
                    fileNumber: '',
                    formId: form.id,
                    isTestSubmission: false,
                    status: SubmissionStatus.Pending,
                });
                dispatch(nextStep());
                // Clear possible identity data from search params
                setSearchParams({});
                CustomerInputService.cleanCustomerInput(form);
            }
        }

        // Handle default step
        else {
            dispatch(nextStep());
        }
    };

    const determineFormState = async (validate: boolean, blockingMode: 'busy' | 'deriving', forceAll: boolean = false): Promise<boolean> => {
        // Can't derive state for null application
        if (form == null || form.root == null || form.root.children == null) {
            return false;
        }

        const currentStepId = allVisibleSteps[currentStep]?.id as string | undefined;

        if (currentStepId == null) {
            return false;
        }

        const currentStepElement = allVisibleSteps[currentStep];

        const nextStepId = allVisibleSteps[currentStep + 1]?.id as string | undefined;

        const allStepsWithoutDerivableAspects = [
            form.root.introductionStep?.id ?? '',
            ...form.root.children
                .filter(step => !hasDerivableAspects(step, true))
                .map(step => step.id),
            form.root.summaryStep?.id ?? '',
            form.root.submitStep?.id ?? '',
        ];

        const skipErrorsForStepIds = allStepsWithoutDerivableAspects
            .filter((stepId) => stepId != currentStepId);
        const skipVisibilitiesForStepIds = allStepsWithoutDerivableAspects
            .filter((stepId) => stepId != currentStepId && stepId != nextStepId);
        const skipOverridesForStepIds = allStepsWithoutDerivableAspects
            .filter((stepId) => stepId != currentStepId && stepId != nextStepId);
        const skipValuesForStepIds = allStepsWithoutDerivableAspects
            .filter((stepId) => stepId != currentStepId && stepId != nextStepId);

        const doNotPerformErrorDerivation = !validate || adminSettings.disableValidation;
        const doNotPerformVisibilityDerivation = adminSettings.disableVisibility;


        const blocker = blockingMode === 'busy' ? setIsBusy : setIsDeriving;

        blocker(true);

        if (blockingMode === 'deriving') {
            dispatch(showLoadingSnackbar('Berechnungen werden durchgeführt…'));
        }

        try {
            const derivationResult = await withAsyncWrapper({
                desiredMinRuntime: 600,
                main: () => new FormsApiService(api).determineFormState(
                    form.id,
                    elementData,
                    {
                        skipErrorsFor: forceAll ? [] : (doNotPerformErrorDerivation ? ['ALL'] : skipErrorsForStepIds),
                        skipVisibilitiesFor: forceAll ? [] : (doNotPerformVisibilityDerivation ? ['ALL'] : skipVisibilitiesForStepIds),
                        skipValuesFor: forceAll ? [] : (skipValuesForStepIds),
                        skipOverridesFor: forceAll ? [] : (skipOverridesForStepIds),
                    },
                ),
            }).finally(() => {
                blocker(false);

                if (blockingMode === 'deriving') {
                    dispatch(removeLoadingSnackbar());
                }
            });

            onElementDataChange(derivationResult, [element.id]);

            return collectErrors(currentStepElement, derivationResult).length === 0 || adminSettings.disableValidation;
        } catch (err) {
            console.error(err);
            dispatch(showErrorSnackbar('Dynamische Funktionen konnten nicht ausgewertet werden.'));

            return adminSettings.disableValidation;
        }
    };

    const determineUploadSize = (): Promise<boolean> => {
        return withAsyncWrapper<undefined, boolean>({
            desiredMinRuntime: checkTimeoutMinMs,
            runtimeCallback: setIsLoading,
            main: async () => {
                try {
                    const {maxFileSize} = await new FormsApiService(api).getMaxFileSize(form!.id);
                    const maxFileSizeBytes = maxFileSize * 1000 * 1000;
                    const totalFileSize = Object.keys(elementData)
                        .map((c) => elementData[c])
                        .filter((c) => Array.isArray(c) && c.length > 0 && isFileUploadElementItem(c[0]))
                        .map((c) => c.reduce((acc: number, item: FileUploadElementItem) => acc + item.size, 0))
                        .reduce((acc, size) => acc + size, 0);
                    if (totalFileSize > maxFileSizeBytes) {
                        dispatch(addError({
                            key: SummaryAttachmentsTooLargeKey,
                            error: `Die Gesamtgröße der von Ihnen hinzugefügten Anlagen überschreitet das Maximum von ${maxFileSize.toFixed(0)} Megabyte.`,
                        }));

                        return false;
                    }
                    return true;
                } catch (error) {
                    console.error(error);
                    dispatch(showErrorSnackbar('Der Maximalgröße der Anlagen konnte nicht korrekt überprüft werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                    return false;
                }
            },
        });
    };

    // Calculates the price for the current form and customer input.
    // Returns false, if the price could not be calculated.
    const calculatePrice = (): Promise<boolean> => {
        return withAsyncWrapper<undefined, boolean>({
            desiredMinRuntime: checkTimeoutMinMs,
            runtimeCallback: setIsLoading,
            main: async () => {
                try {
                    const calculatedCosts = await new FormsApiService(api)
                        .calculateCosts(form!.id, elementData);
                    dispatch(updateCustomerInput({
                        key: SubmitPaymentDataKey,
                        value: calculatedCosts,
                    }));
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

    const handleElementDataChange = (elementData: ElementData, triggeringElementIds: string[]): void => {
        if (form == null || currentStepElement == null || element.children == null) {
            return;
        }

        const allElementsToConsider = [
            ...flattenElements(currentStepElement),
            ...element.children,
        ].filter(e => {
            return e.visibility != null ||
                e.override != null ||
                (isAnyInputElement(e) && e.value != null);
        });

        let shouldDerive = false;
        for (const element of allElementsToConsider) {
            for (const triggeringElementId of triggeringElementIds) {
                shouldDerive = (element.visibility?.referencedIds?.includes(triggeringElementId) ||
                    element.override?.referencedIds?.includes(triggeringElementId) ||
                    (isAnyInputElement(element) && element.value?.referencedIds?.includes(triggeringElementId))) ?? false;

                if (shouldDerive) {
                    break;
                }
            }
            if (shouldDerive) {
                break;
            }
        }

        if (shouldDerive) {
            setIsDeriving(true);

            const currentStepId = currentStepElement.id;

            const skipSteps = [
                element.introductionStep?.id ?? '',
                ...element.children
                    .filter(step => !hasDerivableAspects(step, true))
                    .map(step => step.id),
                element.summaryStep?.id ?? '',
                element.submitStep?.id ?? '',
            ].filter((stepId) => stepId != currentStepId);

            const doNotPerformVisibilityDerivation = adminSettings.disableVisibility;

            onElementDataChange(elementData, []);

            withAsyncWrapper({
                desiredMinRuntime: 600,
                main: async () => {
                    return await new FormsApiService(api)
                        .determineFormState(
                            form.id,
                            elementData,
                            {
                                skipErrorsFor: ['ALL'],
                                skipVisibilitiesFor: doNotPerformVisibilityDerivation ? ['ALL'] : skipSteps,
                                skipValuesFor: skipSteps,
                                skipOverridesFor: skipSteps,
                            },
                        );
                },
            })
                .then((derivedElementData) => {
                    function mergeData(...ed: ElementData[]) {
                        return ed.reduce((acc, curr) => {
                            Object.keys(curr).forEach((key) => {
                                if (acc[key] == null) {
                                    acc[key] = curr[key];
                                } else if (Array.isArray(acc[key]) && Array.isArray(curr[key])) {
                                    acc[key] = [...acc[key], ...curr[key]];
                                } else {
                                    acc[key] = curr[key];
                                }
                            });
                            return acc;
                        }, {} as ElementData);
                    }

                    onElementDataChange(derivedElementData, []);
                })
                .finally(() => {
                    setIsDeriving(false);
                });
        } else {
            onElementDataChange(elementData, []);
        }
    };

    return (
        <>
            <AppHeader mode={AppMode.Customer} />

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
                        /* Remove spacing for richtext components that are immediately preceded by a headline component  */
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
                                            onNext={handleNextStep}
                                            onPrevious={() => {
                                                dispatch(clearErrors());
                                                dispatch(previousStep());
                                            }}
                                            active={currentStep === index}
                                            navDirection={upcomingStepDirection}
                                            stepRefs={stepRefs}
                                            scrollContainerRef={scrollContainerRef}
                                            isBusy={isBusy}
                                            isDeriving={isDeriving ?? false}
                                        >
                                            <ViewDispatcherComponent
                                                allElements={allElements}
                                                element={step}
                                                isBusy={isBusy}
                                                isDeriving={isDeriving ?? false}
                                                mode={mode}
                                                elementData={elementData}
                                                onElementDataChange={handleElementDataChange}
                                                onElementBlur={onElementBlur}
                                                scrollContainerRef={scrollContainerRef}
                                                disableVisibility={disableVisibility}
                                                derivationTriggerIdQueue={derivationTriggerIdQueue}
                                            />

                                            <ErrorAlert
                                                element={step}
                                                elementData={elementData}
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
                                        form={form}
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
                                icon={<span
                                    style={{
                                        color: 'var(--hw-primary)',
                                        transform: 'translateY(2px)',
                                    }}
                                ><GppGoodOutlinedIcon fontSize="small" /></span>}
                                label="Lokal auf Ihrem Gerät zwischengespeichert"
                                variant="outlined"
                            />
                        </Tooltip>
                    </Container>
                }
            </main>

            <AppFooter
                mode={AppMode.Customer}
            />

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

