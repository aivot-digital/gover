import React, {useEffect, useState} from 'react';
import {Container, Dialog, DialogContent, Stepper, useTheme} from '@mui/material';
import {RootElement} from '../../models/elements/root-element';
import {addError, resetErrors, selectCustomerInputErrors} from '../../slices/customer-input-errors-slice';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {isElementValid} from '../../utils/is-element-valid';
import {CustomStep} from './components/custom-step/custom-step';
import {PrivacyUserInputKey} from '../general-information/general-information.component.view';
import {isElementVisible} from '../../utils/is-element-visible';
import Chip from '@mui/material/Chip';
import Tooltip from '@mui/material/Tooltip';
import {Submitted} from '../static-components/submitted/submitted';
import {SummaryAttachmentsTooLargeKey, SummaryUserInputKey} from '../summary/summary.component.view';
import {SubmitHumanKey} from '../submit/submit.component.view';
import {ProcessingDataLoaderComponentView} from '../static-components/processing-data-loader/processing-data-loader.component.view';
import {UserInputService} from '../../services/user-input-service';
import {ApplicationService} from '../../services/application-service';
import {AppFooter} from '../app-footer/app-footer';
import {AppMode} from '../../data/app-mode';
import {AppHeader} from '../app-header/app-header';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedApplication} from '../../slices/app-slice';
import {resetUserInput, selectCustomerInput} from '../../slices/customer-input-slice';
import {nextStep, previousStep, selectCurrentStep} from '../../slices/stepper-slice';
import {ElementType} from '../../data/element-type/element-type';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {useLogging} from "../../hooks/use-logging";
import {ElementNames} from "../../data/element-type/element-names";
import {FileUploadElementItem, isFileUploadElementItem} from "../../models/elements/form/input/file-upload-element";
import ProjectPackage from '../../../package.json';
import {BaseViewProps} from "../../views/base-view";
import {withTimeout} from "../../utils/with-timeout";
import {SubmissionListDto} from "../../models/entities/submission-list-dto";
import GppGoodOutlinedIcon from '@mui/icons-material/GppGoodOutlined';

const checkTimeoutMinMs = 2000;
const submissionTimeoutMinMs = 3000;

export function RootComponentView({
                                      allElements,
                                      element,
                                  }: BaseViewProps<RootElement, void>) {
    const theme = useTheme();
    const [$debug] = useLogging();

    const dispatch = useAppDispatch();
    const application = useAppSelector(selectLoadedApplication);
    const customerData = useAppSelector(selectCustomerInput);
    const adminSettings = useAppSelector(state => state.adminSettings);
    const currentStep = useAppSelector(selectCurrentStep);
    const errors = useAppSelector(selectCustomerInputErrors);

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [pdfLink, setPdfLink] = useState('');

    const [validatedWithErrors, setValidatedWithErrors] = useState(false);

    const visibleChildSteps = (element.children ?? []).filter(elem => adminSettings.disableVisibility || isElementVisible(undefined, allElements, elem.id, elem, customerData));

    const generalInformationStepIndex = 0;
    const summaryStepIndex = visibleChildSteps.length + 1;
    const submitStepIndex = summaryStepIndex + 1;

    const allSteps = [
        element.introductionStep,
        ...visibleChildSteps,
        element.summaryStep,
        element.submitStep,
    ];

    useEffect(() => {
        if (validatedWithErrors && Object.keys(errors).length === 0) {
            setValidatedWithErrors(false);
        }
    }, [errors]);

    const isCurrentPageValid = async () => {
        $debug.start('isCurrentPageValid');

        if (adminSettings.disableValidation) {
            $debug.log('Validation disabled');
            $debug.end();
            return true;
        }

        let isValid = true;

        if (currentStep === generalInformationStepIndex) {
            $debug.log(`Testing ${ElementNames[ElementType.IntroductionStep]}`);

            if (customerData[PrivacyUserInputKey] == null || customerData[PrivacyUserInputKey] === false) {
                dispatch(addError({
                    key: PrivacyUserInputKey,
                    error: 'Bitte akzeptieren Sie die Hinweise zum Datenschutz.',
                }));

                isValid = false;
            }
        } else if (currentStep === summaryStepIndex) {
            $debug.log(`Testing ${ElementNames[ElementType.SummaryStep]}`);

            if (customerData[SummaryUserInputKey] == null || customerData[SummaryUserInputKey] === false) {
                dispatch(addError({
                    key: SummaryUserInputKey,
                    error: 'Bitte bestätigen Sie, dass Sie die Zusammenfassung Ihres Antrages geprüft haben.',
                }));

                isValid = false;
            }

            if (isValid) {
                await withTimeout<number | null>(
                    () => setIsLoading(true),
                    async () => {
                        try {
                            const {maxFileSize} = await ApplicationService.getMaxFileSize(application!);
                            const maxFileSizeBytes = maxFileSize * 1000 * 1000;
                            const totalFileSize = Object.keys(customerData)
                                .map(c => customerData[c])
                                .filter(c => Array.isArray(c) && c.length > 0 && isFileUploadElementItem(c[0]))
                                .map(c => c.reduce((acc: number, item: FileUploadElementItem) => acc + item.size, 0))
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
            }
        } else if (currentStep === submitStepIndex) {
            $debug.log(`Testing ${ElementNames[ElementType.SubmitStep]}`);

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

        $debug.log(`${ElementNames[ElementType.SubmitStep]} is ${isValid ? '' : 'in'}valid`);
        $debug.end();

        return isValid;
    };

    const handleNextStep = async () => {
        $debug.start('handleNextStep');

        dispatch(resetErrors());
        const currentPageValid = await isCurrentPageValid();

        if (currentPageValid) {
            if (currentStep === (allSteps.length - 1)) {
                if (application != null) {
                    await withTimeout<SubmissionListDto | null>(
                        () => setIsSubmitting(true),
                        async () => {
                            try {
                                return await ApplicationService.submit(application, customerData);
                            } catch (error) {
                                console.error(error);
                                dispatch(showErrorSnackbar('Der Antrag konnte nicht korrekt übertragen werden. Bitte probieren Sie es zu einem späteren Zeitpunkt erneut.'));
                                return null;
                            }
                        },
                        (submission) => {
                            if (submission != null) {
                                setValidatedWithErrors(false);
                                setPdfLink(`/api/public/prints/${submission.id}`);
                                dispatch(nextStep());
                                dispatch(resetUserInput());
                                UserInputService.cleanUserInput(application);
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

        $debug.end();
    };

    return (
        <>
            <AppHeader mode={AppMode.Customer}/>

            <Container sx={{
                mt: 5,
                mb: 5,
            }}>
                {
                    currentStep < allSteps.length &&
                    <Stepper
                        sx={{
                            mt: 10,
                            mb: 12,
                            ml: '20px',
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
                                        nextLabel={index > 0 ? (index === allSteps.length - 1 ? 'Antrag verbindlich einreichen' : 'Weiter') : 'Antrag beginnen'}
                                        onNext={handleNextStep}
                                        previousLabel={index > 0 ? 'Zurück zum vorherigen Schritt' : undefined}
                                        onPrevious={() => {
                                            dispatch(previousStep());
                                            setValidatedWithErrors(false);
                                        }}
                                        active={currentStep === index}
                                        validatedWithErrors={validatedWithErrors}
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
                        }}
                        orientation="vertical"
                    >
                        <CustomStep
                            step={{
                                id: '',
                                type: ElementType.SubmittedStep,
                                appVersion: ProjectPackage.version,
                                pdfLink: pdfLink,
                            }}
                            title="Ihr Antrag wurde erfolgreich eingereicht"
                        >
                            <Submitted
                                allElements={allElements}
                                pdfLink={pdfLink}
                            />
                        </CustomStep>
                    </Stepper>
                }
            </Container>

            {
                currentStep < allSteps.length &&
                <Container
                    sx={{
                        textAlign: 'center',
                        marginTop: '-80px',
                        mb: 8,
                        [theme.breakpoints.up('md')]: {
                            textAlign: 'right',
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
                            icon={<span style={{
                                color: 'var(--hw-primary)',
                                transform: 'translateY(1px)',
                            }}><GppGoodOutlinedIcon fontSize={"small"}/></span>}
                            label="Lokal auf Ihrem Gerät zwischengespeichert"
                            variant="outlined"
                        />
                    </Tooltip>
                </Container>
            }

            <AppFooter
                mode={AppMode.Customer}
            />

            <Dialog
                open={isLoading}
                fullWidth
            >
                <DialogContent>
                    <ProcessingDataLoaderComponentView
                        message="Ihre Daten werden final überprüft"
                    />
                </DialogContent>
            </Dialog>

            <Dialog
                open={isSubmitting}
                fullWidth
            >
                <DialogContent>
                    <ProcessingDataLoaderComponentView
                        message="Ihr Antrag wird sicher an die zuständige/bewirtschaftende Stelle übermittelt"
                    />
                </DialogContent>
            </Dialog>
        </>
    );
}

