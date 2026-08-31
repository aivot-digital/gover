import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {
    useOptionalProcessNodeEditorContext,
} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {
    ProcessInstanceAttachmentSetSelectElement,
} from '../models/elements/form/input/process-instance-attachment-set-select-element';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {ProcessInstanceAttachmentSetSelect} from '../components/process-instance-attachment-set-select/process-instance-attachment-set-select';

export function ProcessInstanceAttachmentSetSelectFieldView(props: BaseViewProps<ProcessInstanceAttachmentSetSelectElement, string[]>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const opec = useOptionalProcessNodeEditorContext();

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    return (
        <ProcessInstanceAttachmentSetSelect
            attachmentSets={opec?.incomingMetadata?.forwardedAttachmentSets}
            value={value}
            onChange={setValue}
            label={element.label ?? ''}
            placeholder={element.placeholder}
            hint={element.hint}
            errors={errors}
            required={element.required}
            disabled={isDisabled}
            busy={isBusy}
            maxItems={element.maxItems}
        />
    );
}
