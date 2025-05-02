import {Box, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {type ElementEditorMetadataTabProps} from './element-editor-metadata-tab-props';
import {type AnyElement} from '../../models/elements/any-element';
import {ElementMetadata} from '../../models/elements/element-metadata';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {TextFieldComponent} from '../text-field/text-field-component';
import {AnyInputElement, isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import type {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {ElementType} from '../../data/element-type/element-type';
import {FormsApiService} from '../../modules/forms/forms-api-service';
import {isForm} from '../../models/entities/form';
import {IdentityProviderListDTO} from '../../modules/identity/models/identity-provider-list-dto';
import {IdentityProvidersApiService} from '../../modules/identity/identity-providers-api-service';
import {useApi} from '../../hooks/use-api';
import {getMetadataMapping} from '../../utils/prefill-elements';
import {SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {IdentityProviderType} from '../../modules/identity/enums/identity-provider-type';
import {BayernIdAttributes, BundIdAttributes, MukAttributes, ShIdAttributes} from '../../modules/identity/constants/system-identity-provider-attribute-maps';
import {BayernIdAttribute, BundIdAttribute, MukAttribute, ShIdAttribute} from '../../modules/identity/constants/system-identity-provider-attributes';
import {Page} from '../../models/dtos/page';

export function ElementEditorMetadataTab<T extends AnyElement, E extends ElementTreeEntity>(props: ElementEditorMetadataTabProps<T, E>) {
    const api = useApi();

    const {
        entity: entity,
        elementModel: element,
    } = props;

    const [linkedIdentityProviders, setLinkedIdentityProviders] = useState<IdentityProviderListDTO[]>();

    useEffect(() => {
        if (isForm(entity)) {
            FormsApiService
                .getIdentityProviders(entity.id)
                .then((linkedIdentityProvidersPage) => {
                    return linkedIdentityProvidersPage.content;
                })
                .then((linkedIdentityProviders) => {
                    const linkedIdentityProvidersKeys = linkedIdentityProviders
                        .map((provider) => provider.key);

                    if (linkedIdentityProvidersKeys.length === 0) {
                        return new Promise<Page<IdentityProviderListDTO>>(resolve => {
                            resolve({
                                empty: true,
                                first: true,
                                last: true,
                                number: 0,
                                numberOfElements: 0,
                                size: 0,
                                totalElements: 0,
                                totalPages: 0,
                                content: [],
                            });
                        });
                    }

                    return new IdentityProvidersApiService(api)
                        .listAllOrdered('name', 'ASC', {
                            keys: linkedIdentityProvidersKeys,
                        });
                })
                .then((identityProvidersPage) => {
                    setLinkedIdentityProviders(identityProvidersPage.content);
                })
                .catch(console.error); // TODO: Handle error
        }
    }, [entity]);

    const identityProviders: Array<IdentityProviderListDTO & {
        existingMetadataMapping: string | undefined;
        options: SelectFieldComponentOption[];
    }> = useMemo(() => {
        if (element == null || linkedIdentityProviders == null) {
            return [];
        }

        if (!isAnyInputElement(element)) {
            return [];
        }

        return linkedIdentityProviders
            .map((identityProvider) => {
                const existingMetadataMapping = getMetadataMapping(element, identityProvider.metadataIdentifier);

                const attributeOptions = filterOptions(element, identityProvider);

                return {
                    ...identityProvider,
                    existingMetadataMapping: existingMetadataMapping,
                    options: attributeOptions,
                };
            })
            .filter((idp) => idp.options.length > 0);
    }, [element, linkedIdentityProviders]);

    const handlePatchMetadata = (data: Partial<ElementMetadata>) => {
        props.onChange({
            ...element,
            metadata: {
                ...element,
                ...data,
            },
        });
    };

    // non-input elements have no metadata
    if (!isAnyInputElement(element)) {
        return <></>;
    }

    return (
        <Box sx={{p: 4}}>
            {
                identityProviders.length > 0 &&
                <Box
                    sx={{
                        mb: 4,
                    }}
                >
                    <Typography
                        variant="h6"
                    >
                        Verknüpfung mit Nutzerkonten
                    </Typography>

                    {
                        identityProviders
                            .map((identityProvider) => {
                                return (
                                    <SelectFieldComponent
                                        key={identityProvider.key}
                                        value={identityProvider.existingMetadataMapping}
                                        onChange={(val) => {
                                            const updatedIdentityMappings: Record<string, string> = {
                                                ...element.metadata?.identityMappings,
                                            };

                                            if (val == null) {
                                                delete updatedIdentityMappings[identityProvider.metadataIdentifier];
                                            } else {
                                                updatedIdentityMappings[identityProvider.metadataIdentifier] = val;
                                            }

                                            handlePatchMetadata({
                                                identityMappings: updatedIdentityMappings,
                                            });
                                        }}
                                        options={identityProvider.options}
                                        placeholder="Nicht gesetzt"
                                        label={`Attribut im Nutzerkonto ${identityProvider.name}`}
                                        hint={`Verknüpfen Sie dieses Element mit einem Attribut aus dem Nutzerkonto ${identityProvider.name}. Das Element wird dann automatisch mit den Daten aus dem Nutzerkonto ${identityProvider.name} befüllt.`}
                                        emptyStatePlaceholder={`Es sind keine Attribute im Nutzerkonto ${identityProvider.name} vorhanden`}
                                        disabled={!props.editable}
                                    />
                                );
                            })
                    }
                </Box>
            }

            <Box
                sx={{
                    mb: 4,
                }}
            >
                <Typography
                    variant="h6"
                >
                    Schnittstellendaten
                </Typography>

                <TextFieldComponent
                    value={element.destinationKey ?? undefined}
                    label="HTTP-Schnittstellen-Schlüssel"
                    onChange={(val) => {
                        props.onChange({
                            ...element,
                            destinationKey: val,
                        });
                    }}
                    hint="Dieser Schlüssel wird statt der Feld-ID verwendet, wenn die Daten an eine HTTP-Schnittstelle gesendet werden."
                    disabled={!props.editable}
                />
            </Box>
        </Box>
    );
}

function filterOptions(element: AnyInputElement, identityProvider: IdentityProviderListDTO): SelectFieldComponentOption[] {
    return identityProvider
        .attributes
        .filter((attribute) => {
            let mappings: ElementType[] | null | undefined = null;

            switch (identityProvider.type) {
                case IdentityProviderType.BayernID:
                    mappings = BayernIdAttributes[attribute.keyInData as BayernIdAttribute];
                    break;
                case IdentityProviderType.BundID:
                    mappings = BundIdAttributes[attribute.keyInData as BundIdAttribute];
                    break;
                case IdentityProviderType.MUK:
                    mappings = MukAttributes[attribute.keyInData as MukAttribute];
                    break;
                case IdentityProviderType.SHID:
                    mappings = ShIdAttributes[attribute.keyInData as ShIdAttribute];
                    break;
            }

            return mappings == null && element.type === ElementType.Text || mappings != null && mappings.includes(element.type);
        })
        .map((attribute) => ({
            label: attribute.label,
            subLabel: attribute.description,
            value: attribute.keyInData,
        }));
}