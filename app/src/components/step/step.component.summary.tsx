import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import {type StepElement} from '../../models/elements/steps/step-element';
import Tooltip from '@mui/material/Tooltip';
import React, {useMemo} from 'react';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {setCurrentStep} from '../../slices/stepper-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedForm} from '../../slices/app-slice';
import {getStepIcon} from '../../data/step-icons';
import {type BaseSummaryProps} from '../../summaries/base-summary';
import EditNoteOutlinedIcon from '@mui/icons-material/EditNoteOutlined';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import {extractVisibleFormSteps} from '../../utils/visible-form-steps';

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
    const application = useAppSelector(selectLoadedForm);

    const stepIndex = useMemo(() => {
        if (application == null) {
            return -1;
        }

        const visibleSteps = extractVisibleFormSteps(application.version.rootElement.children, derivedData);
        return visibleSteps.findIndex(step => step.id === model.id);
    }, [application, derivedData, model.id]);

    const handleNavigateToStep = () => {
        if (stepIndex !== -1) {
            dispatch(setCurrentStep(stepIndex));
        }
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
                    (allowStepNavigation == null || allowStepNavigation === true) &&
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
