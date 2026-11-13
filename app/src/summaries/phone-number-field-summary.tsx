import {BaseSummaryProps} from './base-summary';
import {PhoneNumberFieldElement} from '../models/elements/form/input/phone-number-field-element';
import {TextFieldSummary} from './text-field-summary';
import {parsePhoneNumberFromString} from 'libphonenumber-js/max';

export function PhoneNumberFieldSummary(props: BaseSummaryProps<PhoneNumberFieldElement, string>) {
    const parsed = props.value ? parsePhoneNumberFromString(props.value) : undefined;
    const value = parsed?.isPossible() && !parsed.ext ? parsed.formatInternational() : props.value;
    return <TextFieldSummary {...props} value={value} />;
}
