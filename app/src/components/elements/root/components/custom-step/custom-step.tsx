import {Alert, AlertTitle, Box, Button, Step, StepContent, StepLabel, StepProps, useTheme} from '@mui/material';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faArrowLeft, faArrowRight} from '@fortawesome/pro-regular-svg-icons';
import {faCheckCircle} from '@fortawesome/pro-solid-svg-icons';
import React, {useEffect, useRef} from 'react';
import {ElementType} from '../../../../../data/element-type/element-type';
import {AnyStepElement} from '../../../../../models/elements/./steps/any-step-element';
import {getStepIcon} from '../../../../../data/step-icons';
import {isStepElement} from '../../../../../models/elements/./steps/step-element';
import {ElementNames} from '../../../../../data/element-type/element-names';

interface CustomStepProps {
    step: AnyStepElement;
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

    return (
        <Step
            {...passTroughProps}
            ref={ref}
        >
            <StepLabel
                StepIconComponent={(props) => (
                    <FontAwesomeIcon
                        fixedWidth
                        icon={getStepIcon(step)}
                        size="2x"
                        color={props.active ? theme.palette.primary.main : 'rgba(0, 0, 0, 0.4)'}
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
                    <FontAwesomeIcon
                        fixedWidth
                        icon={faCheckCircle}
                        color={theme.palette.primary.main}
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
                                endIcon={<FontAwesomeIcon icon={faArrowRight}/>}
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
                            startIcon={<FontAwesomeIcon icon={faArrowLeft}/>}
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
