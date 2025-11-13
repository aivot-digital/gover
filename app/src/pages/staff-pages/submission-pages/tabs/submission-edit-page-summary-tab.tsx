import React from 'react';
import {Form} from '../../../../models/entities/form';
import {SummaryDispatcherComponent} from '../../../../components/summary-dispatcher.component';
import {SubmissionDetailsResponseDTO} from '../../../../modules/submissions/dtos/submission-details-response-dto';

interface SubmissionEditPageSummaryTabProps {
    form: Form;
    submission: SubmissionDetailsResponseDTO;
}

export function SubmissionEditPageSummaryTab(props: SubmissionEditPageSummaryTabProps) {
    const {
        form,
        submission,
    } = props;

    return (
        <SummaryDispatcherComponent
            element={form.rootElement}
            allowStepNavigation={false}
            showTechnical={true}
            elementData={submission.customerInput}
        />
    );
}
