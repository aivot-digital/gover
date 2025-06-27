import {BaseSummaryProps} from './base-summary';
import {GroupLayout} from '../models/elements/form/layout/group-layout';
import {SummaryDispatcherComponent} from '../components/summary-dispatcher.component';

export function GroupSummary(props: BaseSummaryProps<GroupLayout, void>) {
    return (
        <>
            {
                props.model.children.map((model, index) => (
                    <SummaryDispatcherComponent
                        key={model.id + index.toString()}
                        allElements={props.allElements}
                        element={model}
                        showTechnical={props.showTechnical}
                        allowStepNavigation={props.allowStepNavigation}
                        isBusy={props.isBusy}
                    />
                ))
            }
        </>
    );
}
