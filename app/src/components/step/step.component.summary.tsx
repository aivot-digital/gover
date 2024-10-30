import {Box, IconButton, Typography} from '@mui/material';
import {StepElement} from '../../models/elements/steps/step-element';
import Tooltip from '@mui/material/Tooltip';
import React from 'react';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {setCurrentStep} from '../../slices/stepper-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedForm} from '../../slices/app-slice';
import {getStepIcon} from '../../data/step-icons';
import {BaseSummaryProps} from '../../summaries/base-summary';
import EditNoteOutlinedIcon from '@mui/icons-material/EditNoteOutlined';

export function StepComponentSummary({model, allowStepNavigation}: BaseSummaryProps<StepElement, any>) {
    const dispatch = useAppDispatch();
    const application = useAppSelector(selectLoadedForm);

    // FIXME: This is no a good solution.
    const index = application?.root.children?.findIndex(step => step.id === model.id);

    const handleNavigateToStep = () => {
        if (index != null) {
            dispatch(setCurrentStep(index + 1)); // Add 1 to skip general information page
        }
    };

    const Icon = getStepIcon(model);
    return (
        <Box
            sx={{
                display: 'flex',
                alignItems: 'center',
                mt: 4,
                mb: 1.5,
            }}
        >
            <Typography
                component={'h3'}
                variant="h6"
                color="primary"
            >
                <Icon sx={{marginRight: '8px', fontSize: '1rem', transform: 'scale(1.6) translateY(1px)'}} />
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
    );
}
