import {type BaseSummaryProps} from './base-summary';
import {SummaryDispatcherComponent} from '../components/summary-dispatcher.component';
import {type RootElement} from '../models/elements/root-element';

export function RootSummary(props: BaseSummaryProps<RootElement, void>) {
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
