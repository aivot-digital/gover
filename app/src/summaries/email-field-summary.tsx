import {BaseSummaryProps} from './base-summary';
import {EmailFieldElement} from '../models/elements/form/input/email-field-element';
import {TextFieldSummary} from './text-field-summary';

export function EmailFieldSummary(props: BaseSummaryProps<EmailFieldElement, string>) {
    const value = props.value;
    return <TextFieldSummary {...props} value={value} />;
}
