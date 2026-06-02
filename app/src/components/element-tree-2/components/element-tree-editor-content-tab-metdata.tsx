import {useElementTreeEditorContext} from './element-tree-editor-context';
import React, {useMemo} from 'react';
import {ElementEditorSectionHeader} from '../../element-editor-section-header/element-editor-section-header';
import {useElementTreeContext} from '../element-tree-context';
import {AnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {SelectFieldComponent} from '../../select-field-2/select-field-component';

export function ElementTreeEditorContentTabMetadata<T extends AnyInputElement>() {
    const {
        identityMappingInformation,
    } = useElementTreeContext();

    const {
        currentElement,
        onChangeCurrentElement,
    } = useElementTreeEditorContext<T>();

    const assignedIdentity = useMemo(() => {
        return identityMappingInformation
            ?.find((idm) => idm.id === currentElement.metadata?.identitySourceId);
    }, [currentElement.metadata?.identitySourceId]);

    return (
        <>
            <ElementEditorSectionHeader
                title="Identitätsdatenzuordnung"
                disableMarginTop
            >
                Hier können Sie festlegen, welche Attribute angebundener Identitätsanbieter diesem Element zugeordnet
                werden.
                So kann das Feld bei vorhandenen Identitätsdaten automatisch vorausgefüllt werden.
            </ElementEditorSectionHeader>

            <SelectFieldComponent
                label="Zugeodnete Identität"
                value={currentElement.metadata?.identitySourceId ?? undefined}
                onChange={(val) => onChangeCurrentElement({
                    ...currentElement,
                    metadata: {
                        ...currentElement.metadata,
                        identitySourceId: val,
                    },
                })}
                options={
                    (identityMappingInformation ?? [])
                        .map((identity) => ({
                            label: identity.title ?? '',
                            value: identity.id ?? '',
                        }))
                }
            />

            {
                assignedIdentity != null &&
                assignedIdentity.options != null &&
                assignedIdentity.options
                    .map((idpOption) => (
                        <SelectFieldComponent
                            label={idpOption.provider.name}
                            value={currentElement.metadata?.identityMappings?.[idpOption.provider.metadataIdentifier] ?? undefined}
                            onChange={(attributeKey) => {
                                onChangeCurrentElement({
                                    ...currentElement,
                                    metadata: {
                                        ...currentElement.metadata,
                                        identityMappings: {
                                            ...currentElement.metadata?.identityMappings,
                                            [idpOption.provider.metadataIdentifier]: attributeKey,
                                        },
                                    },
                                });
                            }}
                            options={
                                idpOption
                                    .provider
                                    .attributes
                                    .map((att) => ({
                                        label: att.label,
                                        value: att.keyInData,
                                    }))
                            }
                        />
                    ))
            }
        </>
    );
}
