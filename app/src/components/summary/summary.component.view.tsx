import React from 'react';
import {useSelector} from 'react-redux';
import {SummaryStepElement} from '../../models/elements/step-elements/summary-step-element';
import {BaseViewProps} from '../_lib/base-view-props';
import {SummaryMap} from '../summary.map';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import {ElementType} from '../../data/element-type/element-type';
import {Box, Typography} from '@mui/material';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {selectLoadedApplication} from '../../slices/app-slice';
import {AnyElement} from '../../models/elements/any-element';
import {isAnyElementWithChildren} from '../../models/elements/any-element-with-children';

export const SummaryUserInputKey = '__summary__';

// TODO: Localization

export function SummaryComponentView(_: BaseViewProps<SummaryStepElement, void>) {
    const application = useSelector(selectLoadedApplication);

    if (application == null) {
        return null;
    }

    const models = flattenElements(application.root);

    return (
        <>
            <Typography
                sx={{mb: 5, maxWidth: '620px'}}
                variant={'body2'}
            >
                Bitte prüfen Sie die von Ihnen eingegebenen Daten sorgfältig, bevor Sie den Antrag einreichen. Durch
                einen Klick auf das jeweilige Datenfeld gelangen Sie zurück zu dem dazugehörigen Abschnitt um die
                Eingabe zu ändern.
            </Typography>

            {
                models.map(model => (
                    <SummaryDispatcherComponent
                        key={model.id}
                        model={model}
                    />
                ))
            }

            <Typography
                variant="h6"
                color="primary"
                sx={{mt: 6}}
            >
                Bestätigung der Datenprüfung
            </Typography>

            <Typography
                sx={{mt: 1, maxWidth: '700px'}}
                variant={'body2'}
            >
                Bitte bestätigen Sie, dass Sie die vorangegangenen Eingaben Ihres Antrages geprüft haben.
                Fehlerhafte Eingaben können zu einer Verzögerung bei der Bearbeitung Ihres Antrages durch
                die zuständige und/oder bewirtschaftende Stelle führen. Vielen Dank!
            </Typography>

            <Box>
                <ViewDispatcherComponent
                    model={{
                        type: ElementType.Checkbox,
                        label: 'Ich habe die Zusammenfassung meines Antrages geprüft.',
                        id: SummaryUserInputKey
                    }}
                />
            </Box>
        </>
    );
}

export function flattenElements(model: AnyElement): AnyElement[] {
    let results: AnyElement[] = [];

    const Summary = SummaryMap[model.type];
    if (Summary != null) {
        results.push(model);
    }

    if (model.type !== ElementType.ReplicatingContainer && isAnyElementWithChildren(model)) {
        for (const child of model.children) {
            results = results.concat(flattenElements(child));
        }
    }

    return results;
}
