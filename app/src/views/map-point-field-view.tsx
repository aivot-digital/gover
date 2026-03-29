import {useMemo} from 'react';
import {BaseViewProps} from './base-view';
import {MapPointFieldElement, MapPointValue} from '../models/elements/form/input/map-point-field-element';
import {MapPointFieldComponent} from '../components/map-point-field/map-point-field-component';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function MapPointFieldView(props: BaseViewProps<MapPointFieldElement, MapPointValue>) {
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

    return (
        <MapPointFieldComponent
            label={element.label ?? ''}
            value={value ?? undefined}
            onChange={setValue}
            hint={element.hint ?? undefined}
            required={element.required ?? false}
            disabled={isDisabled}
            busy={isBusy}
            error={errors?.join(', ') ?? undefined}
            zoom={element.zoom ?? 14}
            centerLatitude={element.centerLatitude ?? undefined}
            centerLongitude={element.centerLongitude ?? undefined}
        />
    );
}
