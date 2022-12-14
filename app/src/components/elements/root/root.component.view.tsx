import React, {useCallback, useState} from 'react';
import {Container, Dialog, DialogContent, StepLabel, Stepper, useTheme} from '@mui/material';
import {RootElement} from '../../../models/elements/root-element';
import {addError, resetErrors} from '../../../slices/customer-input-errors-slice';
import {ViewDispatcherComponent} from '../../view-dispatcher.component';
import {isComponentValid} from '../../../utils/is-component-valid';
import {CustomStep} from './components/custom-step/custom-step';
import {PrivacyUserInputKey} from '../../general-information/general-information.component.view';
import {isComponentVisible} from '../../../utils/is-component-visible';
import {BaseViewProps} from '../../_lib/base-view-props';
import Chip from '@mui/material/Chip';
import {faShieldCheck} from '@fortawesome/pro-light-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import Tooltip from '@mui/material/Tooltip';
import {Submitted} from '../../static-components/submitted/submitted';
import {SummaryUserInputKey} from '../../summary/summary.component.view';
import {SubmitHumanKey} from '../../submit/submit.component.view';
import {
    ProcessingDataLoaderComponentView
} from '../../static-components/processing-data-loader/processing-data-loader.component.view';
import {UserInputService} from '../../../services/user-input.service';
import {ApplicationService} from '../../../services/application.service';
import {AppFooter} from '../../app-footer/app-footer';
import {AppMode} from '../../../data/app-mode';
import {AppHeader} from '../../app-header/app-header';
import {useAppDispatch} from '../../../hooks/use-app-dispatch';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectLoadedApplication} from '../../../slices/app-slice';
import {selectCustomerInput} from '../../../slices/customer-input-slice';
import {nextStep, previousStep, selectCurrentStep} from '../../../slices/stepper-slice';
import {ElementType} from '../../../data/element-type/element-type';
import {showErrorSnackbar} from '../../../slices/snackbar-slice';
import {useLogging} from "../../../hooks/use-logging";
import {ElementNames} from "../../../data/element-type/element-names";

export function RootComponentView({element}: BaseViewProps<RootElement, void>) {
    const theme = useTheme();
    const [$debug] = useLogging();

    const dispatch = useAppDispatch();
    const application = useAppSelector(selectLoadedApplication);
    const customerData = useAppSelector(selectCustomerInput);
    const adminSettings = useAppSelector(state => state.adminSettings);
    const currentStep = useAppSelector(selectCurrentStep);

    const [isSubmitting, setIsSubmitting] = useState(false);
    const [pdfLink, setPdfLink] = useState('');

    const [validatedWithErrors, setValidatedWithErrors] = useState(false);

    const isCurrentPageValid = useCallback(() => {
        $debug.start('isCurrentPageValid');

        if (adminSettings.disableValidation) {
            $debug.log('Validation disabled');
            $debug.end();
            return true;
        }

        const steps = element.children ?? [];

        let isValid = true;

        if (currentStep === 0) {
            $debug.log(`Testing ${ElementNames[ElementType.IntroductionStep]}`);

            if (customerData[PrivacyUserInputKey] == null || customerData[PrivacyUserInputKey] === false) {
                dispatch(addError({
                    key: PrivacyUserInputKey,
                    error: 'Bitte akzeptieren Sie die Hinweise zum Datenschutz.',
                }));

                isValid = false;
            }
        }

        else if (currentStep === steps.length + 1) {
            $debug.log(`Testing ${ElementNames[ElementType.SummaryStep]}`);

            if (customerData[SummaryUserInputKey] == null || customerData[SummaryUserInputKey] === false) {
                dispatch(addError({
                    key: SummaryUserInputKey,
                    error: 'Bitte best??tigen Sie, dass Sie die Zusammenfassung Ihres Antrages gepr??ft haben.',
                }));


                isValid = false;
            }
        }

        else if (currentStep === steps.length + 2) {
            $debug.log(`Testing ${ElementNames[ElementType.SubmitStep]}`);

            if (customerData[SubmitHumanKey] == null || customerData[SubmitHumanKey] === false) {
                dispatch(addError({
                    key: SubmitHumanKey,
                    error: 'Bitte best??tigen Sie, dass Sie ein Mensch sind.',
                }));

                isValid = false;
            }
        }

        else {
            const step = steps[currentStep - 1];
            if (step != null) {
                isValid = isComponentValid($debug, dispatch, step, customerData);
            }
        }

        $debug.log(`${ElementNames[ElementType.SubmitStep]} is ${isValid ? '' : 'in'}valid`);
        $debug.end();

        return isValid;
    }, [dispatch, adminSettings, element, currentStep, customerData]);

    const handleNextStep = () => {
        $debug.start('handleNextStep');

        dispatch(resetErrors());
        if (isCurrentPageValid()) {
            if (currentStep === (steps.length - 1)) {
                if (application != null) {
                    setIsSubmitting(true);
                    ApplicationService.submit(application, customerData)
                        .then(pdfLink => {
                            setValidatedWithErrors(false);
                            setPdfLink(pdfLink);
                            dispatch(nextStep());
                            UserInputService.cleanUserInput(application);
                        })
                        .catch(error => {
                            console.error(error);
                            dispatch(showErrorSnackbar('Der Antrag konnte nicht korrekt ??bertragen werden. Bitte probieren Sie es zu einem sp??teren Zeitpunkt erneut.'));
                        })
                        .finally(() => {
                            setIsSubmitting(false);
                        });
                } else {
                    throw new Error('Cannot submit customer data: No application data loaded.');
                }
            } else {
                setValidatedWithErrors(false);
                dispatch(nextStep());
            }
        } else {
            currentStep !== 0 &&
            currentStep !== (element.children ?? []).length + 1 &&
            currentStep !== (element.children ?? []).length + 2 &&
            setValidatedWithErrors(true)
        }

        $debug.end();
    }

    const steps = [
        element.introductionStep,
        ...element.children,
        element.summaryStep,
        element.submitStep,
    ];

    return (
        <>
            <AppHeader mode={AppMode.Customer}/>

            <Container sx={{mt: 5, mb: 5}}>
                {
                    currentStep < steps.length &&
                    <Stepper
                        sx={{mt: 10, mb: 12, ml: '20px'}}
                        activeStep={currentStep}
                        orientation="vertical"
                    >
                        {
                            steps
                                .filter(step => isComponentVisible(step.id, step, customerData))
                                .map((step, index) => (
                                    <CustomStep
                                        key={index}
                                        step={step}
                                        nextLabel={index > 0 ? (index === steps.length - 1 ? 'Antrag verbindlich einreichen' : 'Weiter') : 'Antrag beginnen'}
                                        onNext={handleNextStep}
                                        previousLabel={index > 0 ? 'Zur??ck zum vorherigen Schritt' : undefined}
                                        onPrevious={() => {
                                            dispatch(previousStep());
                                            setValidatedWithErrors(false)
                                        }}
                                        active={currentStep === index}
                                        validatedWithErrors={validatedWithErrors}
                                    >
                                        <ViewDispatcherComponent
                                            model={step}
                                        />
                                    </CustomStep>
                                ))
                        }

                    </Stepper>
                }

                {
                    currentStep === steps.length &&
                    <Stepper
                        sx={{mt: 10, mb: 12, ml: '20px'}}
                        orientation="vertical"
                    >
                        <CustomStep
                            step={{
                                id: '',
                                type: ElementType.SubmittedStep,
                                pdfLink: pdfLink
                            }}
                            title="Ihr Antrag wurde erfolgreich eingereicht"
                        >
                            <Submitted pdfLink={pdfLink}/>
                        </CustomStep>
                    </Stepper>
                }
            </Container>

            {
                currentStep < steps.length &&
                <Container
                    sx={{
                        textAlign: 'center',
                        marginTop: '-80px',
                        mb: 8,
                        [theme.breakpoints.up('md')]: {
                            textAlign: 'right',
                        }
                    }}
                >
                    <Tooltip
                        title="Ihre ??nderungen werden lokal auf Ihrem Ger??t gespeichert. Das L??schen Ihrer lokalen Daten oder Cookies kann einen Verlust Ihres Entwurfs zur Folge haben."
                        arrow
                    >
                        <Chip
                            sx={{pl: 1, pr: 1, cursor: 'help'}}
                            icon={<span style={{color: 'var(--hw-primary)'}}><FontAwesomeIcon
                                icon={faShieldCheck}
                                size={'lg'}
                            /></span>}
                            label="Lokal auf Ihrem Ger??t zwischengespeichert"
                            variant="outlined"
                        />
                    </Tooltip>
                </Container>
            }

            <AppFooter
                mode={AppMode.Customer}
            />

            <Dialog
                open={isSubmitting}
                fullWidth
            >
                <DialogContent>
                    <ProcessingDataLoaderComponentView
                        message="Ihr Antrag wird sicher an die zust??ndige/bewirtschaftende Stelle ??bermittelt"
                    />
                </DialogContent>
            </Dialog>
        </>
    );
}

