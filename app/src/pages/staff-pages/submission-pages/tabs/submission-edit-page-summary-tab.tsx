import React, { useEffect, useState } from 'react';
import {Form} from '../../../../models/entities/form';
import {SummaryDispatcherComponent} from '../../../../components/summary-dispatcher.component';
import {flattenElements} from '../../../../utils/flatten-elements';
import {SubmissionDetailsResponseDTO} from '../../../../modules/submissions/dtos/submission-details-response-dto';
import {AlertComponent} from '../../../../components/alert/alert-component';
import {useDispatch} from 'react-redux';
import {FormsApiService} from '../../../../modules/forms/forms-api-service';
import {useApi} from '../../../../hooks/use-api';
import {hydrateFromDerivation} from '../../../../slices/app-slice';
import {LoadingPlaceholder} from '../../../../components/loading-placeholder/loading-placeholder';
import {Typography} from '@mui/material';
import {ExpandableCodeBlock} from '../../../../components/expandable-code-block/expandable-code-block';

interface SubmissionEditPageSummaryTabProps {
    form: Form;
    submission: SubmissionDetailsResponseDTO;
}

export function SubmissionEditPageSummaryTab(props: SubmissionEditPageSummaryTabProps) {
    const api = useApi();
    const dispatch = useDispatch();
    const allElements = flattenElements(props.form.root);

    const [isBusy, setIsBusy] = useState(false);
    const [derivationError, setDerivationError] = useState<string>();

    useEffect(() => {
        setIsBusy(true);
        setDerivationError(undefined);

        new FormsApiService(api)
            .determineFormState(props.form.id, props.submission.customerInput, {
                stepsToValidate: ['ALL'],
                stepsToCalculateVisibilities: ['ALL'],
                stepsToCalculateValues: ['ALL'],
                stepsToCalculateOverrides: ['ALL'],
            })
            .then((formState) => {
                dispatch(hydrateFromDerivation(formState));
            })
            .catch((error) => {
                console.error(error);
                setDerivationError('Fehler beim Ermitteln des Formularzustands.');
            })
            .finally(() => {
                setIsBusy(false);
            });
    }, []);

    if (isBusy) {
        return (
            <LoadingPlaceholder
                message="Formulardaten werden geladen"
            />
        );
    }

    if (derivationError) {
        return (
            <>
                <AlertComponent color="error">
                    {derivationError}
                </AlertComponent>

                <Typography
                    variant="h6"
                    component="div"
                    sx={{
                        mt: 2,
                    }}
                >
                    Daten des Antrags
                </Typography>
                <ExpandableCodeBlock
                    value={JSON.stringify(props.submission.customerInput, null, 2)}
                />
            </>
        );
    }

    return (
        <>
            {
                props.form.root.children.map((model, index) => (
                    <SummaryDispatcherComponent
                        allElements={allElements}
                        key={model.id + index.toString()}
                        element={model}
                        allowStepNavigation={false}
                        showTechnical={true}
                    />
                ))
            }
        </>
    );
}
