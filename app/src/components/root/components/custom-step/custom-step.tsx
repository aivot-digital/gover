import {Alert, AlertTitle, Box, Button, Step, StepContent, StepLabel, StepProps, useTheme} from '@mui/material';
import React, {useEffect, useRef} from 'react';
import {ElementType} from '../../../../data/element-type/element-type';
import {getStepIcon} from '../../../../data/step-icons';
import {isStepElement, StepElement} from '../../../../models/elements/steps/step-element';
import {ElementNames} from '../../../../data/element-type/element-names';
import {IntroductionStepElement} from "../../../../models/elements/steps/introduction-step-element";
import {SummaryStepElement} from "../../../../models/elements/steps/summary-step-element";
import {SubmitStepElement} from "../../../../models/elements/steps/submit-step-element";
import {SubmittedStepElement} from "../../../../models/elements/steps/submitted-step-element";
import CheckCircleTwoToneIcon from '@mui/icons-material/CheckCircleTwoTone';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';

interface CustomStepProps {
    step: StepElement | IntroductionStepElement | SummaryStepElement | SubmitStepElement | SubmittedStepElement;
    children: React.ReactNode;

    nextLabel?: string;
    previousLabel?: string;

    onNext?: () => void;
    onPrevious?: () => void;

    validatedWithErrors?: boolean;
}

export function CustomStep({
                               active,
                               step,
                               children,
                               nextLabel,
                               previousLabel,
                               onNext,
                               onPrevious,
                               validatedWithErrors,
                               ...passTroughProps
                           }: CustomStepProps & StepProps) {
    const theme = useTheme();

    const ref = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (active) {
            setTimeout(() => {
                if (ref.current != null && step.type !== ElementType.IntroductionStep) {
                    ref.current.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start',
                    });
                }
            }, 500);
        }
    }, [active, step, ref]);

    const Icon = getStepIcon(step);
    return (
        <Step
            {...passTroughProps}
            ref={ref}
        >
            <StepLabel
                StepIconComponent={(props) => (
                    <Icon
                        sx={{fontSize: '2rem', marginLeft: '4px', color: props.active ? theme.palette.primary.main : 'rgba(0, 0, 0, 0.4)'}}
                    />
                )}
            >
                <span>
                    {
                        isStepElement(step) ? step.title ?? 'Unbenannter Abschnitt' : ElementNames[step.type]
                    }
                </span>
                <Box
                    className="completed-step-suffix"
                    sx={{ml: 0.75}}
                >
                    <CheckCircleTwoToneIcon
                        sx={{color: theme.palette.primary.main, transform: 'translateY(5px)'}}
                    />
                </Box>
            </StepLabel>
            <StepContent>
                <div>
                    {children}
                </div>
                {
                    validatedWithErrors &&
                    <Alert
                        severity={'error'}
                        sx={{mt: 4}}
                    >
                        <AlertTitle>Dieser Abschnitt enthält fehlerhafte oder fehlende Angaben</AlertTitle>
                        Fehlerhafte Angaben sind farblich hervorgehoben und enthalten weiterführende Informationen zur
                        Art des Fehlers. <br/>Bitte korrigieren Sie Ihre Angaben um mit dem Antrag fortzufahren.
                    </Alert>
                }
                <Box
                    sx={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        mt: 6,
                        mb: 7,
                        flexDirection: 'column',
                        [theme.breakpoints.up('md')]: {
                            flexDirection: 'row',
                        },
                    }}
                >
                    {
                        nextLabel &&
                        onNext ?
                            <Button
                                variant="contained"
                                onClick={onNext}
                                size="large"
                                color="primary"
                                endIcon={<ArrowForwardOutlinedIcon/>}
                            >
                                {nextLabel}
                            </Button>
                            :
                            <Box/>
                    }
                    {
                        previousLabel &&
                        onPrevious &&
                        <Button
                            onClick={onPrevious}
                            variant="outlined"
                            size="large"
                            startIcon={<ArrowBackOutlinedIcon/>}
                            sx={{
                                mt: 2,
                                [theme.breakpoints.up('md')]: {
                                    mt: 0,
                                }
                            }}
                        >
                            {previousLabel}
                        </Button>
                    }
                </Box>
            </StepContent>
        </Step>
    );
}
