import {type BaseSummaryProps} from './base-summary';
import {SummaryDispatcherComponent} from '../components/summary-dispatcher.component';
import {SummaryLayoutElement} from '../models/elements/form/layout/summary-layout-element';

export function SummaryLayoutSummary(props: BaseSummaryProps<SummaryLayoutElement, void>) {
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

    return (
        <>
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
