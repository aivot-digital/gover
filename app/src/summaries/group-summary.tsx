import {type BaseSummaryProps} from './base-summary';
import {type GroupLayout} from '../models/elements/form/layout/group-layout';
import {SummaryDispatcherComponent} from '../components/summary-dispatcher.component';

export function GroupSummary(props: BaseSummaryProps<GroupLayout, void>) {
    const {
        model,
        showTechnical,
        allowStepNavigation,
        elementData,
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
                            elementData={elementData}
                        />
                    ))
            }
        </>
    );
}
