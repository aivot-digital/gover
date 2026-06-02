import {useCallback, useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {
    UiDefinitionInputFieldElement,
    UiDefinitionInputFieldElementItem,
} from '../models/elements/form/input/ui-definition-input-field-element';
import {
    UiDefinitionInputFieldComponent,
} from '../components/ui-definition-input-field/ui-definition-input-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {ElementDisplayContext} from '../data/element-type/element-child-options';
import {
    useProcessNodeEditorContext,
} from '../modules/process/pages/details/components/process-node-editor/process-node-editor-context';
import {useNavigate} from 'react-router-dom';

export function UiDefinitionInputFieldView(
    props: BaseViewProps<UiDefinitionInputFieldElement, UiDefinitionInputFieldElementItem>
) {
    const navigate = useNavigate();

    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const isDisabled = useMemo(() => {
        return element.disabled || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isBusy = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    const {
        node,
    } = useProcessNodeEditorContext();

    const openOverride = useCallback(() => {
        navigate(`/form-triggers/${node.id}`);
    }, [node]);

    return (
        <UiDefinitionInputFieldComponent
            label={element.label ?? ''}
            hint={element.hint}
            error={errors != null ? errors.join(' ') : undefined}
            required={element.required}
            disabled={Boolean(isDisabled) || isBusy}
            value={value ?? undefined}
            expectedRootType={element.elementType}
            onChange={setValue}
            displayContext={element.displayContext ?? ElementDisplayContext.CitizenFacing}
            openOverride={element.openExternalEditor ? openOverride : undefined}
        />
    );
}
