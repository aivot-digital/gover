import React from 'react';
import {useSelector} from 'react-redux';
import {SummaryStepElement} from '../../models/elements/steps/summary-step-element';
import {SummaryDispatcherComponent} from '../summary-dispatcher.component';
import {ElementType} from '../../data/element-type/element-type';
import {Box, Typography} from '@mui/material';
import {ViewDispatcherComponent} from '../view-dispatcher.component';
import {selectLoadedForm} from '../../slices/app-slice';
import {AnyElement} from '../../models/elements/any-element';
import {isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {CustomerInput} from '../../models/customer-input';
import ProjectPackage from '../../../package.json';
import {BaseViewProps} from '../../views/base-view';
import SummaryMap from '../../summaries';
import {useAppSelector} from '../../hooks/use-app-selector';

export const SummaryUserInputKey = '__summary__';
export const SummaryAttachmentsTooLargeKey = '__summary_attachments__';

export function SummaryComponentView({allElements, isBusy, isDeriving}: BaseViewProps<SummaryStepElement, any>) {
    const form = useSelector(selectLoadedForm);
    const customerInput = useAppSelector(state => state.app.inputs);
    const visibilities = useAppSelector(state => state.app.visibilities);

    if (form == null) {
        return null;
    }

    const models = flattenElementsForSummary(allElements, form.root, customerInput, visibilities, undefined);

    return (
        <>
            <Typography
                sx={{
                    mb: 5,
                    maxWidth: '620px',
                }}
                variant={'body2'}
            >
                Bitte prüfen Sie die von Ihnen eingegebenen Daten sorgfältig, bevor Sie den Antrag einreichen. Durch
                einen Klick auf das jeweilige Datenfeld gelangen Sie zurück zu dem dazugehörigen Abschnitt um die
                Eingabe zu ändern.
            </Typography>

            {
                models.map((model, index) => (
                    <SummaryDispatcherComponent
                        allElements={allElements}
                        key={model.id + index.toString()}
                        element={model}
                        showTechnical={false}
                        allowStepNavigation={true}
                        isBusy={isBusy}
                    />
                ))
            }

            <Typography
                component={'h3'}
                variant="h6"
                color="primary"
                sx={{mt: 6}}
            >
                Bestätigung der Datenprüfung
            </Typography>

            <Typography
                sx={{
                    mt: 1,
                    maxWidth: '700px',
                }}
                variant={'body2'}
            >
                Bitte bestätigen Sie, dass Sie die vorangegangenen Eingaben Ihres Antrages geprüft haben.
                Fehlerhafte Eingaben können zu einer Verzögerung bei der Bearbeitung Ihres Antrages durch
                die zuständige und/oder bewirtschaftende Stelle führen. Vielen Dank!
            </Typography>

            <Box>
                <ViewDispatcherComponent
                    allElements={allElements}
                    element={{
                        type: ElementType.Checkbox,
                        label: 'Ich habe die Zusammenfassung meines Antrages geprüft. *',
                        id: SummaryUserInputKey,
                        appVersion: ProjectPackage.version,
                    }}
                    isBusy={isBusy}
                    isDeriving={isDeriving}
                />
            </Box>
        </>
    );
}

export function flattenElementsForSummary(allElements: AnyElement[], model: AnyElement, userInput: CustomerInput, visibilities: Record<string, boolean>, idPrefix?: string): AnyElement[] {
    const isVisible = visibilities[model.id] ?? true;

    if (!isVisible) {
        return [];
    }

    const modelSummary = SummaryMap[model.type];

    let childElements: AnyElement[] = [];
    if (model.type !== ElementType.ReplicatingContainer && isAnyElementWithChildren(model)) {
        for (const child of model.children) {
            childElements = childElements.concat(flattenElementsForSummary(allElements, child, userInput, visibilities, idPrefix));
        }
    }

    if (modelSummary != null) {
        if (model.type === ElementType.Step && childElements.length === 0) {
            return [];
        }

        return [
            model,
            ...childElements,
        ];
    } else {
        return [
            ...childElements,
        ];
    }
}
