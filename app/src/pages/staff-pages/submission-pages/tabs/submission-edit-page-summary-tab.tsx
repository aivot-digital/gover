import React from 'react';
import {Form} from '../../../../models/entities/form';
import {Submission} from '../../../../models/entities/submission';
import {SummaryDispatcherComponent} from '../../../../components/summary-dispatcher.component';
import {flattenElementsForSummary} from '../../../../components/summary/summary.component.view';
import {flattenElements} from '../../../../utils/flatten-elements';

interface SubmissionEditPageSummaryTabProps {
    form: Form;
    submission: Submission;
}

export function SubmissionEditPageSummaryTab(props: SubmissionEditPageSummaryTabProps): JSX.Element {
    const allElements = flattenElements(props.form.root);
    const models = flattenElementsForSummary(allElements, props.form.root, props.submission.customerInput, undefined);

    return (
        <>
            {
                models.map((model, index) => (
                    <SummaryDispatcherComponent
                        allElements={allElements}
                        key={model.id + index.toString()}
                        element={model}
                        allowStepNavigation={false}
                        customerInput={props.submission.customerInput}
                        showTechnical={true}
                    />
                ))
            }
        </>
    );
}
