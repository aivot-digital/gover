import {Box, Button, Collapse, Step, StepContent, StepLabel, type StepProps, useTheme} from '@mui/material';
import React, {useEffect, useRef} from 'react';
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
import {ErrorAlert} from '../error-alert/error-alert';
import ReactMarkdown from 'react-markdown';

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
                               navDirection,
                               stepRefs,
                               scrollContainerRef,
                               ...passTroughProps
                           }: CustomStepProps & StepProps) {
    const theme = useTheme();
    const disableAutoScroll = useAppSelector(selectDisableAutoScrollForSteps);

    const ref = useRef<HTMLDivElement>(null);
    const headingRef = useRef<HTMLDivElement>(null);
    stepRefs.current[stepIndex] = ref;

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const handleExit = () => {
        if (!disableAutoScroll) {
            if (navDirection === 'next' && ref.current) {
                (scrollContainerRef?.current ?? window).scrollTo({
                    top: ref.current.offsetTop,
                    behavior: prefersReducedMotion ? 'auto' : 'smooth',
                });
            }
        }
    };

    const handleEnter = () => {
        if (!disableAutoScroll) {
            const previousStepIndex = stepIndex - 1;
            headingRef.current?.focus();
            if (navDirection === 'previous' && previousStepIndex >= 0 && stepRefs.current[previousStepIndex]?.current) {
                (scrollContainerRef?.current ?? window).scrollTo({
                    top: stepRefs.current[previousStepIndex].current?.offsetTop ?? 0,
                    behavior: prefersReducedMotion ? 'auto' : 'smooth',
                });
                // Scrolling back to the first element if there are no preceding elements left
            } else if (navDirection === 'previous' && ref.current) {
                (scrollContainerRef?.current ?? window).scrollTo({
                    top: ref.current.offsetTop,
                    behavior: prefersReducedMotion ? 'auto' : 'smooth',
                });
            }
        }
    };

    // Scrolling to step title in submitted step
    useEffect(() => {
        if (active && !disableAutoScroll && stepIndex === -1 && ref.current) {
            (scrollContainerRef?.current ?? window).scrollTo({
                top: ref.current.offsetTop,
                behavior: prefersReducedMotion ? 'auto' : 'smooth',
            });
        }
    }, [active, disableAutoScroll, stepIndex, ref]);

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
                            color: props.active ? theme.palette.primary.main : 'rgba(0, 0, 0, 0.55)',
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
                <h2
                    style={{
                        fontSize: 'inherit',
                        fontWeight: 'inherit',
                        lineHeight: 'inherit',
                        margin: 0,
                        display: 'inline',
                    }}
                    ref={headingRef}
                    tabIndex={-1}
                >
                    {
                        isStepElement(step) ? step.title ?? 'Unbenannter Abschnitt' : getElementNameForType(step.type)
                    }
                </h2>
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
                TransitionComponent={Collapse}
                TransitionProps={{onEnter: handleEnter, onExit: handleExit}}
                transitionDuration={prefersReducedMotion ? 0 : 1000}
                sx={{
                    [theme.breakpoints.down('md')]: {
                        pl: 4,
                    },
                }}
            >
                <div>
                    {children}
                </div>

                <ErrorAlert />

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
                            endIcon={isLastStep ? <SendOutlinedIcon /> : <ArrowForwardOutlinedIcon />}
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
                            startIcon={<ArrowBackOutlinedIcon />}
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
