import React from 'react';
import {SummaryStepElement} from '../../models/elements/steps/summary-step-element';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import {Box, Typography} from '@mui/material';
import {BaseViewProps} from '../../views/base-view';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {useViewDispatcherContext} from '../view-dispatcher/view-dispatcher.context';

export const SummaryUserInputKey = '__summary__'; // TODO: Remove
export const SummaryAttachmentsTooLargeKey = '__summary_attachments__'; // TODO: Solve

export function SummaryComponentView(props: BaseViewProps<SummaryStepElement, any>) {
    const {
        isBusy,
        isDeriving,
        value,
        authoredElementValues,
        derivedData,
        setValue,
        errors,
    } = props;

    const {
        rootElement,
    } = useViewDispatcherContext();

    return (
        <>
            <Typography
                sx={{
                    mb: 5,
                    maxWidth: '620px',
                }}
                variant="body2"
            >
                Bitte prüfen Sie die von Ihnen eingegebenen Daten sorgfältig, bevor Sie den Antrag einreichen. Durch
                einen Klick auf das jeweilige Datenfeld gelangen Sie zurück zu dem dazugehörigen Abschnitt um die
                Eingabe zu ändern.
            </Typography>

            <SummaryDispatcherComponent
                key={rootElement.id}
                element={rootElement}
                showTechnical={false}
                allowStepNavigation={true}
                authoredElementValues={authoredElementValues}
                derivedData={derivedData}
            />

            <Typography
                component="h3"
                variant="h5"
                sx={{mt: 6}}
            >
                Bestätigung der Datenprüfung
            </Typography>

            <Typography
                sx={{
                    mt: 1,
                    maxWidth: '660px',
                }}
                variant="body2"
            >
                Bitte bestätigen Sie, dass Sie die vorangegangenen Eingaben Ihres Antrages geprüft haben.
                Fehlerhafte Eingaben können zu einer Verzögerung bei der Bearbeitung Ihres Antrages durch
                die zuständige und/oder bewirtschaftende Stelle führen.
            </Typography>

            <Box>
                <CheckboxFieldComponent
                    label="Ich habe die Zusammenfassung meines Antrages geprüft."
                    required={true}
                    value={value}
                    onChange={(checked) => {
                        setValue(checked);
                    }}
                    busy={isBusy || isDeriving}
                    error={errors != null ? errors.join(' ') : undefined}
                />
            </Box>
        </>
    );
}
