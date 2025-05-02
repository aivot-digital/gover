import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Container, Dialog, DialogContent, Stepper, useTheme} from '@mui/material';
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
import {
    addError,
    clearErrors,
    dequeueDerivationTriggerId,
    hydrateFromDerivation,
    hydrateFromDerivationWithoutErrors,
    selectCustomerInputs,
    selectDerivationTriggerIdQueue,
    selectLoadedForm,
    selectVisibilies,
    updateCustomerInput,
} from '../../slices/app-slice';
import {nextStep, previousStep, selectCurrentStep, selectUpcomingStepDirection, setCurrentStep} from '../../slices/stepper-slice';
import {ElementType} from '../../data/element-type/element-type';
import {removeLoadingSnackbar, showErrorSnackbar, showLoadingSnackbar} from '../../slices/snackbar-slice';
import {useLogger} from '../../hooks/use-logging';
import {type FileUploadElementItem, isFileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import ProjectPackage from '../../../package.json';
import {type BaseViewProps} from '../../views/base-view';
import {withAsyncWrapper} from '../../utils/with-async-wrapper';
import GppGoodOutlinedIcon from '@mui/icons-material/GppGoodOutlined';
import {CustomStep} from '../custom-step/custom-step';
import {useApi} from '../../hooks/use-api';
import {useSearchParams} from 'react-router-dom';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {DerivationStepIdentifiers, FormsApiService} from '../../modules/forms/forms-api-service';
import {SubmissionListResponseDTO} from '../../modules/submissions/dtos/submission-list-response-dto';
import {SubmissionStatus} from '../../modules/submissions/enums/submission-status';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {useSingleUpdateEffect} from '../../hooks/use-single-update-effect';
import {ApiError, isApiError} from '../../models/api-error';
import {selectIdentityId} from '../../slices/identity-slice';

const SubmissionIdSearchParam = 'submissionId';

const checkTimeoutMinMs = 1000;

export function RootComponentView({
                                      allElements,
                                      element,
                                      scrollContainerRef,
                                      mode,
                                  }: BaseViewProps<RootElement, void>) {
    const log = useLogger('RootComponentView');
    const api = useApi();
    const theme = useTheme();
    const [searchParams, setSearchParams] = useSearchParams();

    const dispatch = useAppDispatch();
    const form = useAppSelector(selectLoadedForm);
    const customerInput = useAppSelector(selectCustomerInputs);
    const adminSettings = useAppSelector((state) => state.adminSettings);
    const currentStep = useAppSelector(selectCurrentStep);
    const upcomingStepDirection = useAppSelector(selectUpcomingStepDirection);
    const visibilities = useAppSelector(selectVisibilies);
    const derivationTriggerIdQueue = useAppSelector(selectDerivationTriggerIdQueue);
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

    // Determine the steps to be displayed.
    const visibleChildSteps = useMemo(() => {
        if (children == null) {
            return [];
        }

        return children
            .filter((stepElement) => {
                return visibilities[stepElement.id] ?? true;
            });
    }, [children, visibilities]);

    // Collecting all steps including the fixed steps
    const allSteps = useMemo(() => [
        introductionStep,
        ...visibleChildSteps,
        summaryStep,
        submitStep,
    ], [introductionStep, summaryStep, submitStep, visibleChildSteps]);

    // Determine the total number of steps
    const totalStepCount = useMemo(() => {
        return allSteps.length;
    }, [allSteps]);

    const stepRefs = useRef(allSteps.map(() => React.createRef<HTMLDivElement>()));

    // Check if a form derivation is necessary, when the form is loaded and some of the steps have derivable aspects.
    useSingleUpdateEffect(() => {
        if (form != null && form.root.children.some(step => hasDerivableAspects(step, true))) {
            determineFormState(false, 'busy', ['ALL']);
        }
    }, [form]);

    // Set basic submission data from query params.
    // This is used, when redirected from payment provider.
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
                    .submit(form.id, customerInput, identityId);
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

    const determineFormState = async (validate: boolean, blockingMode: 'busy' | 'deriving', stepOverride?: DerivationStepIdentifiers): Promise<boolean> => {
        log.debug('Determine form state.');

        // Can't derive state for null application
        if (form == null) {
            log.warn('Cannot determine form state: No form was loaded.');
            return false;
        }

        const currentStepId = allSteps[currentStep]?.id as string | undefined;

        if (currentStepId == null) {
            return false;
        }

        const nextStepId = allSteps[currentStep + 1]?.id as string | undefined;

        const otherStepsToDerive = form.root.children
            .filter(step => hasDerivableAspects(step, true))
            .map(step => step.id);

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
                    customerInput,
                    {
                        stepsToValidate: stepOverride ?? ((adminSettings.disableValidation || !validate) ? ['NONE'] : [currentStepId]),
                        stepsToCalculateVisibilities: stepOverride ?? (adminSettings.disableVisibility ? ['NONE'] : (nextStepId != null ? [currentStepId, nextStepId, ...otherStepsToDerive] : [currentStepId, ...otherStepsToDerive])),
                        stepsToCalculateValues: stepOverride ?? (nextStepId != null ? [currentStepId, nextStepId] : [currentStepId]),
                        stepsToCalculateOverrides: stepOverride ?? (nextStepId != null ? [currentStepId, nextStepId] : [currentStepId]),
                    },
                ),
                after: async (derivationResult) => {
                    if (validate) {
                        dispatch(hydrateFromDerivation(derivationResult));
                    } else {
                        dispatch(hydrateFromDerivationWithoutErrors(derivationResult));
                    }
                },
            }).finally(() => {
                blocker(false);

                if (blockingMode === 'deriving') {
                    dispatch(removeLoadingSnackbar());
                }
            });

            return Object.keys(derivationResult.errors).length === 0 || adminSettings.disableValidation;
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
                    const totalFileSize = Object.keys(customerInput)
                        .map((c) => customerInput[c])
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
                        .calculateCosts(form!.id, customerInput);
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
                                allSteps
                                    .map((step, index) => (
                                        <CustomStep
                                            key={index}
                                            step={step}
                                            stepIndex={index}
                                            isFirstStep={index === 0}
                                            isLastStep={index === allSteps.length - 1}
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
                                step={{
                                    id: '',
                                    type: ElementType.SubmittedStep,
                                    appVersion: ProjectPackage.version,
                                }}
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
                            title="Ihre Änderungen werden lokal auf Ihrem Gerät gespeichert. Das Löschen Ihrer lokalen Daten oder Cookies kann einen Verlust Ihres Entwurfs zur Folge haben."
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

