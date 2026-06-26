import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import {type StepElement} from '../../models/elements/steps/step-element';
import Tooltip from '@mui/material/Tooltip';
import React, {useMemo} from 'react';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {setCurrentStep} from '../../slices/stepper-slice';
import {getStepIcon} from '../../data/step-icons';
import {type BaseSummaryProps} from '../../summaries/base-summary';
import EditNoteOutlinedIcon from '@mui/icons-material/EditNoteOutlined';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import {useViewDispatcherContext} from '../view-dispatcher/view-dispatcher.context';
import {resolveSummaryStepIndex} from '../../utils/resolve-summary-step-index';

export function StepComponentSummary(props: BaseSummaryProps<StepElement, any>) {
    const {
        model,
        showTechnical,
        allowStepNavigation,
        authoredElementValues,
        derivedData,
    } = props;

    const {
        children,
    } = model;

    const dispatch = useAppDispatch();
    const {
        rootElement,
    } = useViewDispatcherContext();

    const stepIndex = useMemo(() => {
        return resolveSummaryStepIndex(rootElement, derivedData, model.id);
    }, [derivedData, model.id, rootElement]);

    const canNavigateToStep = (allowStepNavigation == null || allowStepNavigation === true) && stepIndex !== -1;

    const handleNavigateToStep = () => {
        if (stepIndex === -1) {
            return;
        }

        dispatch(setCurrentStep(stepIndex));
    };

    const Icon = getStepIcon(model);
    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                    mt: 4,
                    mb: 1.5,
                }}
            >
                <Typography
                    component="h3"
                    variant="h5"
                >
                    <Icon
                        sx={{
                            marginRight: '8px',
                            fontSize: '1rem',
                            transform: 'scale(1.6) translateY(1px)',
                        }}
                    />
                    &nbsp;
                    {
                        model.title ? model.title : 'Unbenannter Abschnitt'
                    }
                </Typography>
                {
                    canNavigateToStep &&
                    <Tooltip
                        title="Diesen Abschnitt bearbeiten"
                        arrow
                        placement="top"
                    >
                        <IconButton
                            onClick={handleNavigateToStep}
                            size="small"
                            sx={{
                                ml: 'auto',
                                color: '#BFBFBF',
                            }}
                        >
                            <EditNoteOutlinedIcon />
                        </IconButton>
                    </Tooltip>
                }
            </Box>
            {
                (children ?? [])
                    .map((model) => (
                        <SummaryDispatcherComponent
                            key={model.id}
                            element={model}
                            showTechnical={showTechnical}
                            allowStepNavigation={allowStepNavigation}
                            authoredElementValues={authoredElementValues}
                            derivedData={derivedData}
                        />
                    ))
            }
        </>
    );
}
