import {
    Box,
    Button,
    CircularProgress,
    Collapse,
    Step,
    StepContent,
    StepLabel,
    type StepProps,
    useTheme,
} from '@mui/material';
import React, {useEffect, useRef} from 'react';
import {getStepIcon} from '../../data/step-icons';
import {ElementType} from '../../data/element-type/element-type';
import {isStepElement} from '../../models/elements/steps/step-element';
import {getElementNameForType} from '../../data/element-type/element-names';
import CheckCircleFilledIcon from '@aivot/mui-material-symbols-400-n25-outlined/CheckCircleFilled';
import ArrowForwardOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import ArrowBackOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ArrowBack';
import SendOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Send';
import {type CustomStepProps} from './custom-step-props';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectDisableAutoScrollForSteps} from '../../slices/admin-settings-slice';
import {useViewDispatcherContext} from '../view-dispatcher/view-dispatcher.context';
import {getPreviewHighlightStyles} from '../view-dispatcher/preview-highlight-styles';

export function CustomStep(props: CustomStepProps & StepProps) {
    const {
        stepIndex,
        isFirstStep,
        isLastStep,
        isSubmitStep,
        active,
        completed = false,
        step,
        children,
        onNext,
        onPrevious,
        navDirection,
        stepRefs,
        scrollContainerRef,
        isBusy,
        isDeriving,
        ...passTroughProps
    } = props;

    const theme = useTheme();
    const disableAutoScroll = useAppSelector(selectDisableAutoScrollForSteps);
    const {
        highlightedElementId,
    } = useViewDispatcherContext();
    const isIntroductionStep = step.type === ElementType.IntroductionStep;
    const isHighlightedInPreview = highlightedElementId === step.id;

    const ref = useRef<HTMLDivElement>(null);
    const headingRef = useRef<HTMLDivElement>(null);
    stepRefs.current[stepIndex] = ref;

    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    const handleExit = () => {
        if (!disableAutoScroll) {
            if (navDirection === 'next' && ref.current) {
                // shift scrollTo to next render cycle for compatibility with the editor
                setTimeout(() => {
                    (scrollContainerRef?.current ?? window).scrollTo({
                        top: ref.current?.offsetTop,
                        behavior: prefersReducedMotion ? 'auto' : 'smooth',
                    });
                }, 0);
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
            if (headingRef.current) {
                headingRef.current.focus();
            }
        }
    }, [active, disableAutoScroll, stepIndex, ref]);

    const Icon = getStepIcon(step);
    return (
        <Step
            {...passTroughProps}
            completed={completed}
            ref={ref}
            sx={[
                (theme) => getPreviewHighlightStyles(theme, isHighlightedInPreview),
                {
                    '& .MuiStepContent-last': {
                        position: 'relative',
                    },
                    '& .MuiStepContent-last::before': {
                        content: '""',
                        width: '1px',
                        backgroundColor: 'divider',
                        top: 0,
                        bottom: 0,
                        left: 0,
                        position: 'absolute',
                    },
                    '& .Mui-disabled ~ .MuiStepContent-last::before': {
                        display: 'none',
                    },
                },
                ...(Array.isArray(passTroughProps.sx) ? passTroughProps.sx : [passTroughProps.sx]),
            ]}
        >
            <StepLabel
                StepIconComponent={() => (
                    <Icon
                        sx={{
                            fontSize: '2rem',
                            marginLeft: '4px',
                            color: active
                                ? theme.palette.primary.main
                                : completed
                                    ? theme.palette.text.primary
                                    : theme.palette.text.secondary,
                        }}
                    />
                )}
                sx={{
                    [theme.breakpoints.down('md')]: {
                        '.MuiStepLabel-label': {
                            ml: 1,
                        },
                    },
                    '.MuiStepLabel-label': {
                        pt: 0,
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
                {
                    completed &&
                    <Box
                        component="span"
                        className="completed-step-suffix"
                        sx={{ml: 0.75, display: 'inline-flex'}}
                    >
                        <CheckCircleFilledIcon
                            sx={{
                                color: theme.palette.primary.main,
                                transform: 'translateY(5px)',
                            }}
                        />
                    </Box>
                }
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
                        onNext != null &&
                        !isLastStep &&
                        !isSubmitStep &&
                        <Button
                            variant="contained"
                            onClick={onNext}
                            size="large"
                            color="primary"
                            disabled={isBusy || isDeriving}
                            endIcon={(isBusy || isDeriving) ? <CircularProgress
                                size="1em"
                                color="inherit"
                            /> : (isLastStep ? <SendOutlinedIcon/> : <ArrowForwardOutlinedIcon/>)}
                        >
                            {
                                isDeriving && 'Berechne…'
                            }
                            {
                                isIntroductionStep && !isDeriving && 'Starten'
                            }
                            {
                                !isIntroductionStep && !isDeriving && 'Weiter'
                            }
                        </Button>
                    }
                    {
                        onNext != null &&
                        isLastStep &&
                        <Button
                            variant="contained"
                            onClick={onNext}
                            size="large"
                            color="primary"
                            disabled={isBusy || isDeriving}
                            endIcon={(isBusy || isDeriving) ? <CircularProgress
                                size="1em"
                                color="inherit"
                            /> : (isLastStep ? <SendOutlinedIcon/> : <ArrowForwardOutlinedIcon/>)}
                        >
                            {
                                isDeriving && 'Berechne…'
                            }
                            {
                                isLastStep && !isDeriving && 'Verbindlich einreichen'
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
                            startIcon={(isBusy || isDeriving) ? <CircularProgress
                                size="1em"
                                color="inherit"
                            /> : <ArrowBackOutlinedIcon/>}
                            sx={{
                                mt: 2,
                                [theme.breakpoints.up('md')]: {
                                    mt: 0,
                                },
                            }}
                            disabled={isBusy || isDeriving}
                        >
                            Zum vorherigen Abschnitt
                        </Button>
                    }
                </Box>
            </StepContent>
        </Step>
    );
}
