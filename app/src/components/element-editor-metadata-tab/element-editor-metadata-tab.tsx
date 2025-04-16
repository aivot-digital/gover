import {Box, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {type ElementEditorMetadataTabProps} from './element-editor-metadata-tab-props';
import {type AnyElement} from '../../models/elements/any-element';
import {ElementMetadata} from '../../models/elements/element-metadata';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {TextFieldComponent} from '../text-field/text-field-component';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import type {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {ElementType} from '../../data/element-type/element-type';
import {UserInfoIdentifier, UserInfoIdentifierOptions} from '../../data/user-info-identifier';
import {FormsApiService} from '../../modules/forms/forms-api-service';
import {isForm} from '../../models/entities/form';
import {IdentityProviderListDTO} from '../../modules/identity/models/identity-provider-list-dto';
import {IdentityProvidersApiService} from '../../modules/identity/identity-providers-api-service';
import {useApi} from '../../hooks/use-api';
import {getMetadataMapping} from '../../utils/prefill-elements';

export type ElementAttributeMappingOption = SelectFieldComponentOption & {
    limit: ElementType[];
};

export function ElementEditorMetadataTab<T extends AnyElement, E extends ElementTreeEntity>(props: ElementEditorMetadataTabProps<T, E>): JSX.Element {
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

    const handlePatchMetadata = (data: Partial<ElementMetadata>) => {
        props.onChange({
            ...element,
            metadata: {
                ...element,
                ...data,
            },
        });
    };

    const userInfoIdentifiers = useMemo(() => {
        if (isAnyInputElement(element) && element.type !== ElementType.ReplicatingContainer) {
            return UserInfoIdentifierOptions;
        }

        return undefined;
    }, [element]);

    // non-input elements have no metadata
    if (!isAnyInputElement(element)) {
        return <></>;
    }

    return (
        <Box sx={{p: 4}}>
            {
                false && /* Disabled for now */
                userInfoIdentifiers != null &&
                <Box
                    sx={{
                        mb: 4,
                    }}
                >
                    <Typography
                        variant="h6"
                    >
                        Nutzer:innen-Eigenschaften
                    </Typography>

                    <SelectFieldComponent
                        value={element.metadata?.userInfoIdentifier}
                        onChange={(val) => {
                            handlePatchMetadata({
                                userInfoIdentifier: val as UserInfoIdentifier | undefined,
                            });
                        }}
                        options={userInfoIdentifiers ?? []}
                        placeholder="Nicht gesetzt"
                        label="Nutzer:innen-Eingenschaft"
                        hint="Wählen Sie aus, welche Eigenschaft einer Nutzer:in dieses Feld darstellt. "
                        disabled={!props.editable}
                    />
                </Box>
            }

            {
                element.type === ElementType.Text &&
                linkedIdentityProviders != null &&
                linkedIdentityProviders.length > 0 &&
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
                        linkedIdentityProviders
                            .map((identityProvider) => {
                                const existingMetadataMapping = getMetadataMapping(element, identityProvider.metadataIdentifier);

                                return (
                                    <SelectFieldComponent
                                        key={identityProvider.key}
                                        value={existingMetadataMapping}
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
                                        options={identityProvider.attributes.map((attribute) => ({
                                            label: attribute.label,
                                            subLabel: attribute.description,
                                            value: attribute.keyInData,
                                        }))}
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
