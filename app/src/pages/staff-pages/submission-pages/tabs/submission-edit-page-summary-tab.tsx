import React, { useEffect, useState, type JSX } from 'react';
import {Form} from '../../../../models/entities/form';
import {SummaryDispatcherComponent} from '../../../../components/summary-dispatcher.component';
import {SubmissionDetailsResponseDTO} from '../../../../modules/submissions/dtos/submission-details-response-dto';

interface SubmissionEditPageSummaryTabProps {
    form: Form;
    submission: SubmissionDetailsResponseDTO;
}

export function SubmissionEditPageSummaryTab(props: SubmissionEditPageSummaryTabProps): JSX.Element {
    const {
        form,
        submission,
    } = props;

    return (
        <SummaryDispatcherComponent
            element={form.root}
            allowStepNavigation={false}
            showTechnical={true}
            elementData={submission.customerInput}
        />
    );
}
