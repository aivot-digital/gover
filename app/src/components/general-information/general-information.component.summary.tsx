import Box from '@mui/material/Box';
import IconButton from '@mui/material/IconButton';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';
import EditNoteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/EditNote';
import React, {useMemo} from 'react';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {type BaseSummaryProps} from '../../summaries/base-summary';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {setCurrentStep} from '../../slices/stepper-slice';
import {useViewDispatcherContext} from '../view-dispatcher/view-dispatcher.context';
import {resolveSummaryStepIndex} from '../../utils/resolve-summary-step-index';
import {getStepIcon} from '../../data/step-icons';
import {getElementName} from '../../data/element-type/element-names';

export function GeneralInformationComponentSummary(props: BaseSummaryProps<IntroductionStepElement, any>) {
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
                    {getElementName(model)}
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
