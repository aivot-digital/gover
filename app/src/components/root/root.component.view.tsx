import React, {useEffect, useRef, useState} from 'react';
import {Container, Dialog, DialogContent, Stepper, useTheme} from '@mui/material';
import {type RootElement} from '../../models/elements/root-element';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {isElementValid} from '../../utils/is-element-valid';
import {PrivacyUserInputKey} from '../general-information/general-information.component.view';
import {isElementVisible} from '../../utils/is-element-visible';
import Chip from '@mui/material/Chip';
import Tooltip from '@mui/material/Tooltip';
import {Submitted} from '../submitted/submitted';
import {SummaryAttachmentsTooLargeKey, SummaryUserInputKey} from '../summary/summary.component.view';
import {SubmitHumanKey, SubmitPaymentDataKey} from '../submit/submit.component.view';
import {ProcessingDataLoaderComponentView} from '../processing-data-loader/processing-data-loader.component.view';
import {CustomerInputService} from '../../services/customer-input-service';
import {AppFooter} from '../app-footer/app-footer';
import {AppMode} from '../../data/app-mode';
import {AppHeader} from '../app-header/app-header';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {addError, clearCustomerInput, clearDisabled, clearErrors, selectLoadedForm, updateCustomerInput} from '../../slices/app-slice';
import {
    nextStep,
    previousStep,
    selectCurrentStep,
    selectUpcomingStepDirection,
    setCurrentStep
} from '../../slices/stepper-slice';
import {ElementType} from '../../data/element-type/element-type';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {useLogging} from '../../hooks/use-logging';
import {type FileUploadElementItem, isFileUploadElementItem} from '../../models/elements/form/input/file-upload-element';
import ProjectPackage from '../../../package.json';
import {type BaseViewProps} from '../../views/base-view';
import {withTimeout} from '../../utils/with-timeout';
import GppGoodOutlinedIcon from '@mui/icons-material/GppGoodOutlined';
import {CustomStep} from '../custom-step/custom-step';
import {useApi} from '../../hooks/use-api';
import {useFormsApi} from '../../hooks/use-forms-api';
import {BundIdAccessLevel} from '../../data/bund-id-access-level';
import {SubmissionListProjection} from '../../models/entities/submission';
import {IdCustomerDataKey} from '../id-input/id-input';
import {BayernIdAccessLevel} from '../../data/bayern-id-access-level';
import {MukAccessLevel} from '../../data/muk-access-level';
import {ShIdAccessLevel} from '../../data/sh-id-access-level';
import {useSearchParams} from 'react-router-dom';
import {XBezahldienstePaymentRequest} from '../../models/xbezahldienste/x-bezahldienste-payment-request';
import {SubmissionStatus} from '../../data/submission-status';
import {XBezahldienstePaymentStatus} from '../../data/xbezahldienste-payment-status';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';

const SubmissionIdSearchParam = 'submissionId';
const PaymentLinkSearchParam = 'paymentLink';
const ExternalAccessExpired = 'externalAccessExpired';

const checkTimeoutMinMs = 1000;
const submissionTimeoutMinMs = 2000;

export function RootComponentView({
                                      allElements,
                                      element,
                                      scrollContainerRef,
                                  }: BaseViewProps<RootElement, void>) {
    const api = useApi();
    const theme = useTheme();
    const [searchParams, setSearchParams] = useSearchParams();
    const [$debug] = useLogging();

    const dispatch = useAppDispatch();
    const application = useAppSelector(selectLoadedForm);
    const customerData = useAppSelector(state => state.app.inputs);
    const adminSettings = useAppSelector((state) => state.adminSettings);
    const currentStep = useAppSelector(selectCurrentStep);
    const upcomingStepDirection = useAppSelector(selectUpcomingStepDirection);
    const errors = useAppSelector(state => state.app.errors);

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [submission, setSubmission] = useState<SubmissionListProjection>();

    const [validatedWithErrors, setValidatedWithErrors] = useState(false);

    const visibleChildSteps = (element.children ?? []).filter((elem) => adminSettings.disableVisibility || isElementVisible(undefined, allElements, elem.id, elem, customerData));

    const generalInformationStepIndex = 0;
    const summaryStepIndex = visibleChildSteps.length + 1;
    const submitStepIndex = summaryStepIndex + 1;

    const allSteps = [
        element.introductionStep,
        ...visibleChildSteps,
        element.summaryStep,
        element.submitStep,
    ];

    const stepRefs = useRef(allSteps.map(() => React.createRef<HTMLDivElement>()));

    useEffect(() => {
        const submissionId = searchParams.get(SubmissionIdSearchParam);
        const paymentLink = searchParams.get(PaymentLinkSearchParam);
        const externalAccessExpired = searchParams.get(ExternalAccessExpired);

        if (submissionId != null && isStringNotNullOrEmpty(submissionId)) {
            setSubmission({
                id: submissionId,
                formId: 0,
                created: '',
                assigneeId: null,
                fileNumber: null,
                destinationId: null,
                destinationSuccess: null,
                isTestSubmission: false,
                copySent: false,
                copyTries: 0,
                reviewScore: null,
                destinationResult: null,
                destinationTimestamp: null,
                archived: externalAccessExpired === 'true' ? new Date().toISOString() : null,
                status: SubmissionStatus.PaymentPending,
                paymentRequest: null,
                paymentInformation: paymentLink != null ? {
                    status: XBezahldienstePaymentStatus.Initial,
                    transactionRedirectUrl: paymentLink,
                } : {
                    status: XBezahldienstePaymentStatus.Payed,
                },
                paymentError: null,
                paymentProvider: null,
                paymentOriginatorId: null,
                paymentEndpointId: null,
            });
            dispatch(setCurrentStep(allSteps.length));
        }
    }, [searchParams]);

    useEffect(() => {
        if (validatedWithErrors && Object.keys(errors).length === 0) {
            setValidatedWithErrors(false);
        }
    }, [errors]);

    const isCurrentPageValid = async () => {
        if (adminSettings.disableValidation) {
            return true;
        }

        let isValid = true;

        if (currentStep === generalInformationStepIndex) {

            if (customerData[PrivacyUserInputKey] == null || customerData[PrivacyUserInputKey] === false) {
                dispatch(addError({
                    key: PrivacyUserInputKey,
                    error: 'Bitte akzeptieren Sie die Hinweise zum Datenschutz.',
                }));

                isValid = false;
            }

            // TODO: check all service accounts
            if (
                (application?.bayernIdEnabled && application.bayernIdLevel != null && application.bayernIdLevel !== BayernIdAccessLevel.Optional) ||
                (application?.bundIdEnabled && application.bundIdLevel != null && application.bundIdLevel !== BundIdAccessLevel.Optional) ||
                (application?.mukEnabled && application.mukLevel != null && application.mukLevel !== MukAccessLevel.Optional) ||
                (application?.shIdEnabled && application.shIdLevel != null && application.shIdLevel !== ShIdAccessLevel.Optional)
            ) {
                if (customerData[IdCustomerDataKey] == null || customerData[IdCustomerDataKey] === '') {
                    dispatch(addError({
                        key: IdCustomerDataKey,
                        error: 'Sie müssen sich mit einem Servicekonto anmelden, um diesen Antrag ausfüllen zu können.',
                    }));

                    isValid = false;
                }
            }
        } else if (currentStep === summaryStepIndex) {
            if (customerData[SummaryUserInputKey] == null || customerData[SummaryUserInputKey] === false) {
                dispatch(addError({
                    key: SummaryUserInputKey,
                    error: 'Bitte bestätigen Sie, dass Sie die Zusammenfassung Ihres Antrages geprüft haben.',
                }));

                isValid = false;
            }

            if (isValid) {
                await withTimeout<number | null>(
                    () => {
                        setIsLoading(true);
                    },
                    async () => {
                        try {
                            const {maxFileSize} = await useFormsApi(api).getMaxFileSize(application!.id);
                            const maxFileSizeBytes = maxFileSize * 1000 * 1000;
                            const totalFileSize = Object.keys(customerData)
                                .map((c) => customerData[c])
                                .filter((c) => Array.isArray(c) && c.length > 0 && isFileUploadElementItem(c[0]))
                                .map((c) => c.reduce((acc: number, item: FileUploadElementItem) => acc + item.size, 0))
                                .reduce((acc, size) => acc + size, 0);
                            if (totalFileSize > maxFileSizeBytes) {
                                return (maxFileSize);
                            }
                            return null;
                        } catch (error) {
                            console.error(error);
                            dispatch(showErrorSnackbar('Der Maximalgröße der Anlagen konnte nicht korrekt überprüft werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                            return 0;
                        }
                    },
                    (maxFileSizeMb) => {
                        if (maxFileSizeMb != null && maxFileSizeMb != 0) {
                            dispatch(addError({
                                key: SummaryAttachmentsTooLargeKey,
                                error: maxFileSizeMb.toFixed(0),
                            }));

                            isValid = false;
                        }
                        setIsLoading(false);
                    },
                    checkTimeoutMinMs,
                );

                await withTimeout<XBezahldienstePaymentRequest | null>(
                    () => {
                        setIsLoading(true);
                    },
                    async () => {
                        try {
                            return await useFormsApi(api)
                                .calculateCosts(application!.id, customerData);
                        } catch (error: any) {
                            if (error.status !== 404) {
                                console.error(error);
                                dispatch(showErrorSnackbar('Die Kosten konnten nicht korrekt berechnet werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                                isValid = false;
                            }
                            return null;
                        }
                    },
                    (paymentData) => {
                        if (paymentData != null) {
                            dispatch(updateCustomerInput({
                                key: SubmitPaymentDataKey,
                                value: paymentData,
                            }));
                        }
                        setIsLoading(false);
                    },
                    checkTimeoutMinMs,
                );
            }
        } else if (currentStep === submitStepIndex) {
            if (customerData[SubmitHumanKey] == null || customerData[SubmitHumanKey] === false) {
                dispatch(addError({
                    key: SubmitHumanKey,
                    error: 'Bitte bestätigen Sie, dass Sie ein Mensch sind.',
                }));

                isValid = false;
            }
        } else {
            const step = visibleChildSteps[currentStep - 1];
            if (step != null) {
                isValid = isElementValid($debug, undefined, allElements, dispatch, step, customerData);
            }
        }

        return isValid;
    };

    const handleNextStep = async () => {
        dispatch(clearErrors());
        const currentPageValid = await isCurrentPageValid();

        if (currentPageValid) {
            if (currentStep === 0) {
                // Reset Query Params to strip off possible IDP query
                setSearchParams({});
            }

            if (currentStep === (allSteps.length - 1)) {
                if (application != null) {
                    await withTimeout<SubmissionListProjection | null>(
                        () => {
                            setIsSubmitting(true);
                        },
                        async () => {
                            try {
                                return await useFormsApi(api)
                                    .submit(application.id, customerData);
                            } catch (err: any) {
                                if (err.status === 406) {
                                    dispatch(showErrorSnackbar('Der Antrag konnte nicht korrekt übertragen werden. In den von Ihnen hochgeladenen Dokumenten wurde Schadsoftware erkannt.'));
                                } else {
                                    console.error(err);
                                    dispatch(showErrorSnackbar('Der Antrag konnte nicht korrekt übertragen werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                                }
                                return null;
                            }
                        },
                        (submission) => {
                            if (submission != null) {
                                setValidatedWithErrors(false);
                                setSubmission(submission);
                                dispatch(nextStep());
                                dispatch(clearCustomerInput());
                                dispatch(clearErrors());
                                dispatch(clearDisabled());
                                CustomerInputService.cleanCustomerInput(application);
                            }

                            setIsSubmitting(false);
                        },
                        submissionTimeoutMinMs,
                    );
                } else {
                    throw new Error('Cannot submit customer data: No application data loaded.');
                }
            } else {
                setValidatedWithErrors(false);
                dispatch(nextStep());
            }
        } else {
            if (
                currentStep !== 0 &&
                currentStep !== visibleChildSteps.length + 1 &&
                currentStep !== visibleChildSteps.length + 2
            ) {
                setValidatedWithErrors(true);
            }
        }
    };

    return (
        <>
            <AppHeader mode={AppMode.Customer} />

            <main role="main">
                <Container
                    sx={{
                        mt: 5,
                        mb: 5,
                    }}
                >
                    {
                        currentStep < allSteps.length &&
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
                                                setValidatedWithErrors(false);
                                            }}
                                            active={currentStep === index}
                                            validatedWithErrors={validatedWithErrors}
                                            navDirection={upcomingStepDirection}
                                            stepRefs={stepRefs}
                                            scrollContainerRef={scrollContainerRef}
                                        >
                                            <ViewDispatcherComponent
                                                allElements={allElements}
                                                element={step}
                                            />
                                        </CustomStep>
                                    ))
                            }

                        </Stepper>
                    }

                    {
                        currentStep === allSteps.length &&
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
                            >
                                {
                                    submission != null &&
                                    application != null &&
                                    <Submitted
                                        submission={submission}
                                        form={application}
                                    />
                                }
                            </CustomStep>
                        </Stepper>
                    }
                </Container>

                {
                    currentStep < allSteps.length &&
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

