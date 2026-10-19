import {useEffect, useMemo, useState} from 'react';
import {type BaseViewProps} from './base-view';
import {type DepartmentSelectInputElement} from '../models/elements/form/input/department-select-input-element';
import {DepartmentSelectField} from '../modules/departments/components/department-select-field';
import {VDepartmentShadowedApiService} from '../modules/departments/services/v-department-shadowed-api-service';
import {type VDepartmentShadowedEntity} from '../modules/departments/entities/v-department-shadowed-entity';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';

export function DepartmentSelectInputView(props: BaseViewProps<DepartmentSelectInputElement, number>) {
    const {
        element,
        setValue,
        value,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;
    const [department, setDepartment] = useState<VDepartmentShadowedEntity | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [loadError, setLoadError] = useState<string>();

    useEffect(() => {
        let active = true;

        if (value == null) {
            setDepartment(null);
            setLoadError(undefined);
            setIsLoading(false);
            return () => {
                active = false;
            };
        }

        setIsLoading(true);
        setDepartment(null);
        setLoadError(undefined);
        void new VDepartmentShadowedApiService()
            .retrieve(value)
            .then((loadedDepartment) => {
                if (active) {
                    setDepartment(loadedDepartment);
                }
            })
            .catch(() => {
                if (active) {
                    setDepartment({
                        id: value,
                        name: `Organisationseinheit (ID: ${value})`,
                        created: '',
                        updated: '',
                        depth: 0,
                    });
                    setLoadError('Die ausgewählte Organisationseinheit konnte nicht geladen werden.');
                }
            })
            .finally(() => {
                if (active) {
                    setIsLoading(false);
                }
            });

        return () => {
            active = false;
        };
    }, [value]);

    const isDisabled = useMemo(
        () => Boolean(element.disabled || isGloballyDisabled),
        [element.disabled, isGloballyDisabled],
    );
    const isDerivationBusy = useMemo(
        () => isDeriving && hasDerivableAspects(element),
        [isDeriving, element],
    );
    const error = [...(errors ?? []), ...(loadError != null ? [loadError] : [])].join(' ');

    return (
        <DepartmentSelectField
            label={element.label ?? ''}
            value={department}
            onChange={(selectedDepartment) => setValue(selectedDepartment?.id ?? null)}
            hint={element.hint ?? undefined}
            error={error || undefined}
            placeholder={element.placeholder ?? undefined}
            dialogTitle={element.dialogTitle ?? undefined}
            required={element.required ?? undefined}
            disabled={isDisabled}
            readOnly={isDerivationBusy}
            busy={isLoading || isDerivationBusy}
        />
    );
}
