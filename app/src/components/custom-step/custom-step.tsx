import {Alert, AlertTitle, Box, Button, Step, StepContent, StepLabel, type StepProps, useTheme} from '@mui/material';
import React, {useEffect, useRef} from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {getStepIcon} from '../../data/step-icons';
import {isStepElement} from '../../models/elements/steps/step-element';
import {getElementNameForType} from '../../data/element-type/element-names';
import CheckCircleTwoToneIcon from '@mui/icons-material/CheckCircleTwoTone';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import SendOutlinedIcon from '@mui/icons-material/SendOutlined';
import {type CustomStepProps} from './custom-step-props';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectDisableAutoScrollForSteps} from '../../slices/admin-settings-slice';

export function CustomStep({
                               stepIndex,
                               isFirstStep,
                               isLastStep,
                               active,
                               step,
                               children,
                               onNext,
                               onPrevious,
                               validatedWithErrors,
                               ...passTroughProps
                           }: CustomStepProps & StepProps) {
    const theme = useTheme();
    const disableAutoScroll = useAppSelector(selectDisableAutoScrollForSteps);

    const ref = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (active === true && !disableAutoScroll) {
            setTimeout(() => {
                if (ref.current != null && step.type !== ElementType.IntroductionStep) {
                    ref.current.scrollIntoView({
                        behavior: 'smooth',
                        block: 'start',
                    });
                }
            }, 800);
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
                        sx={{
                            fontSize: '2rem',
                            marginLeft: '4px',
                            color: props.active ? theme.palette.primary.main : 'rgba(0, 0, 0, 0.4)',
                        }}
                    />
                )}
                sx={{
                    [theme.breakpoints.down('md')]: {
                        '.MuiStepLabel-label': {
                            ml: 1,
                        },
                    },
                }}

            >
                <span>
                    {
                        isStepElement(step) ? step.title ?? 'Unbenannter Abschnitt' : getElementNameForType(step.type)
                    }
                </span>
                <Box
                    className="completed-step-suffix"
                    sx={{ml: 0.75}}
                >
                    <CheckCircleTwoToneIcon
                        sx={{
                            color: theme.palette.primary.main,
                            transform: 'translateY(5px)',
                        }}
                    />
                </Box>
            </StepLabel>
            <StepContent
                sx={{
                    [theme.breakpoints.down('md')]: {
                        pl: 4,
                    },
                }}
            >
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
                        mt: 3,
                        mb: 4,
                        flexDirection: 'column',
                        [theme.breakpoints.up('md')]: {
                            flexDirection: 'row',
                            mt: 6,
                            mb: 7,
                        },
                    }}
                >
                    {
                        (onNext != null) &&
                        <Button
                            variant="contained"
                            onClick={onNext}
                            size="large"
                            color="primary"
                            endIcon={isLastStep ? <SendOutlinedIcon/> : <ArrowForwardOutlinedIcon/>}
                        >
                            {
                                isFirstStep && 'Antrag beginnen'
                            }
                            {
                                isLastStep && 'Antrag verbindlich einreichen'
                            }
                            {
                                !isFirstStep && !isLastStep && 'Weiter'
                            }
                        </Button>
                    }
                    {
                        !isFirstStep &&
                        (onPrevious != null) &&
                        <Button
                            onClick={onPrevious}
                            variant="outlined"
                            size="large"
                            startIcon={<ArrowBackOutlinedIcon/>}
                            sx={{
                                mt: 2,
                                [theme.breakpoints.up('md')]: {
                                    mt: 0,
                                },
                            }}
                        >
                            Zum vorherigen Abschnitt
                        </Button>
                    }
                </Box>
            </StepContent>
        </Step>
    );
}
