import {BaseSummaryProps} from '../_lib/base-summary-props';
import {Box, IconButton, Typography} from '@mui/material';
import {StepElement} from '../../models/elements/./steps/step-element';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faPenToSquare} from '@fortawesome/pro-light-svg-icons';
import Tooltip from '@mui/material/Tooltip';
import React from 'react';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {setCurrentStep} from '../../slices/stepper-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedApplication} from '../../slices/app-slice';
import {getStepIcon} from '../../data/step-icons';

export function StepComponentSummary({model}: BaseSummaryProps<StepElement>) {
    const dispatch = useAppDispatch();
    const application = useAppSelector(selectLoadedApplication);

    // FIXME: This is no a good solution.
    const index = application?.root.children?.findIndex(step => step.id === model.id);

    const handleNavigateToStep = () => {
        if (index != null) {
            dispatch(setCurrentStep(index + 1)); // Add 1 to skip general information page
        }
    };

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
                variant="h6"
                color="primary"
            >
                <FontAwesomeIcon
                    icon={getStepIcon(model)}
                    style={{
                        marginRight: '6px',
                    }}
                />
                &nbsp;
                {
                    model.title ? model.title : 'Unbenannter Abschnitt'
                }
            </Typography>
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
                    <FontAwesomeIcon
                        icon={faPenToSquare}
                    />
                </IconButton>
            </Tooltip>
        </Box>
    );
}
