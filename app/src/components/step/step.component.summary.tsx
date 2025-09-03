import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Typography from '@mui/material/Typography';
import {type StepElement} from '../../models/elements/steps/step-element';
import Tooltip from '@mui/material/Tooltip';
import React from 'react';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {setCurrentStep} from '../../slices/stepper-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedForm} from '../../slices/app-slice';
import {getStepIcon} from '../../data/step-icons';
import {type BaseSummaryProps} from '../../summaries/base-summary';
import EditNoteOutlinedIcon from '@mui/icons-material/EditNoteOutlined';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';

export function StepComponentSummary(props: BaseSummaryProps<StepElement, any>) {
    const {
        model,
        showTechnical,
        allowStepNavigation,
        elementData,
    } = props;

    const {
        children,
    } = model;

    const dispatch = useAppDispatch();
    const application = useAppSelector(selectLoadedForm);

    // FIXME: This is no a good solution.
    const index = application?.rootElement.children?.findIndex(step => step.id === model.id);

    const handleNavigateToStep = () => {
        if (index != null) {
            dispatch(setCurrentStep(index + 1)); // Add 1 to skip general information page
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
                    color="primary"
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
                            elementData={elementData}
                        />
                    ))
            }
        </>
    );
}
