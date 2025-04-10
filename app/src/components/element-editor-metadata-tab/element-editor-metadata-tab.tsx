import {Box, Typography} from '@mui/material';
import React from 'react';
import {type ElementEditorMetadataTabProps} from './element-editor-metadata-tab-props';
import {type AnyElement} from '../../models/elements/any-element';
import {ElementMetadata} from '../../models/elements/element-metadata';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {BundIdAttribute, BundIdAttributes} from '../../data/bund-id-attributes';
import {TextFieldComponent} from '../text-field/text-field-component';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectBooleanSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {BayernIdAttribute, BayernIdAttributes} from '../../data/bayern-id-attributes';
import {ShIdAttribute, ShIdAttributes} from '../../data/sh-id-attributes';
import {MukAttribute, MukAttributes} from '../../data/muk-attributes';
import type {ElementTreeEntity} from '../element-tree/element-tree-entity';
import {isForm} from '../../models/entities/form';
import {SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {ElementType} from '../../data/element-type/element-type';
import {UserInfoIdentifier, UserInfoIdentifierOptions} from '../../data/user-info-identifier';

export type ElementAttributeMappingOption = SelectFieldComponentOption & {
    limit: ElementType[];
};

export function ElementEditorMetadataTab<T extends AnyElement, E extends ElementTreeEntity>(props: ElementEditorMetadataTabProps<T, E>): JSX.Element {
    const bundIdActive = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.nutzerkonten.bundid)) && isForm(props.entity) && props.entity.bundIdEnabled;
    const bayernIdActive = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.nutzerkonten.bayernId)) && isForm(props.entity) && props.entity.bayernIdEnabled;
    const shIdActive = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.nutzerkonten.schleswigHolsteinId)) && isForm(props.entity) && props.entity.shIdEnabled;
    const mukActive = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.nutzerkonten.muk)) && isForm(props.entity) && props.entity.mukEnabled;

    const handlePatchMetadata = (data: Partial<ElementMetadata>) => {
        props.onChange({
            ...props.elementModel,
            metadata: {
                ...props.elementModel.metadata,
                ...data,
            },
        });
    };

    const filterOptions = (options: ElementAttributeMappingOption[]) => {
        return options.filter(opt => opt.limit.includes(props.elementModel.type));
    };

    const showBundId = bundIdActive && isAnyInputElement(props.elementModel) && filterOptions(BundIdAttributes).length > 0;
    const showBayernId = bayernIdActive && isAnyInputElement(props.elementModel) && filterOptions(BayernIdAttributes).length > 0;
    const showShId = shIdActive && isAnyInputElement(props.elementModel) && filterOptions(ShIdAttributes).length > 0;
    const showMuk = mukActive && isAnyInputElement(props.elementModel) && filterOptions(MukAttributes).length > 0;

    const showAccountMapping = (
        showBundId ||
        showBayernId ||
        showShId ||
        showMuk
    );

    const userInfoIdentifiers = isAnyInputElement(props.elementModel) && props.elementModel.type !== ElementType.ReplicatingContainer ? UserInfoIdentifierOptions : [];

    return (
        <Box sx={{p: 4}}>
            {
                false && /* Disabled for now */
                userInfoIdentifiers.length > 0 &&
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
                        value={props.elementModel.metadata?.userInfoIdentifier}
                        onChange={(val) => {
                            handlePatchMetadata({
                                userInfoIdentifier: val as UserInfoIdentifier | undefined,
                            });
                        }}
                        options={userInfoIdentifiers}
                        placeholder="Nicht gesetzt"
                        label="Nutzer:innen-Eingenschaft"
                        hint="Wählen Sie aus, welche Eigenschaft einer Nutzer:in dieses Feld darstellt. "
                        disabled={!props.editable}
                    />
                </Box>
            }

            {
                showAccountMapping &&
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
                        showBundId &&
                        <SelectFieldComponent
                            value={props.elementModel.metadata?.bundIdMapping}
                            onChange={(val) => {
                                handlePatchMetadata({
                                    bundIdMapping: val as BundIdAttribute | undefined,
                                });
                            }}
                            options={filterOptions(BundIdAttributes)}
                            placeholder="Nicht gesetzt"
                            label="Attribut in der BundID"
                            hint="Verknüpfen Sie dieses Element mit einem Attribut aus der BundID. Das Element wird dann automatisch mit den Daten aus der BundID befüllt."
                            emptyStatePlaceholder="Keine Attribute für diesen Element-Typ in der BundID vorhanden"
                            disabled={!props.editable}
                        />
                    }

                    {
                        showBayernId &&
                        <SelectFieldComponent
                            value={props.elementModel.metadata?.bayernIdMapping}
                            onChange={(val) => {
                                handlePatchMetadata({
                                    bayernIdMapping: val as BayernIdAttribute | undefined,
                                });
                            }}
                            options={filterOptions(BayernIdAttributes)}
                            placeholder="Nicht gesetzt"
                            label="Attribut in der BayernID"
                            hint="Verknüpfen Sie dieses Element mit einem Attribut aus der BayernID. Das Element wird dann automatisch mit den Daten aus der BayernID befüllt."
                            emptyStatePlaceholder="Keine Attribute für diesen Element-Typ in der BayernID vorhanden"
                            disabled={!props.editable}
                        />
                    }

                    {
                        showShId &&
                        <SelectFieldComponent
                            value={props.elementModel.metadata?.shIdMapping}
                            onChange={(val) => {
                                handlePatchMetadata({
                                    shIdMapping: val as ShIdAttribute | undefined,
                                });
                            }}
                            options={filterOptions(ShIdAttributes)}
                            placeholder="Nicht gesetzt"
                            label="Attribut im Servicekonto Schleswig-Holstein"
                            hint="Verknüpfen Sie dieses Element mit einem Attribut aus dem Servicekonto Schleswig-Holstein. Das Element wird dann automatisch mit den Daten aus dem Servicekonto Schleswig-Holstein befüllt."
                            emptyStatePlaceholder="Keine Attribute für diesen Element-Typ im Servicekonto Schleswig-Holstein vorhanden"
                            disabled={!props.editable}
                        />
                    }

                    {
                        showMuk &&
                        <SelectFieldComponent
                            value={props.elementModel.metadata?.mukMapping}
                            onChange={(val) => {
                                handlePatchMetadata({
                                    mukMapping: val as MukAttribute | undefined,
                                });
                            }}
                            options={filterOptions(MukAttributes)}
                            placeholder="Nicht gesetzt"
                            label="Attribut im Mein Unternehmenskonto (MUK)"
                            hint="Verknüpfen Sie dieses Element mit einem Attribut aus dem Mein Unternehmenskonto. Das Element wird dann automatisch mit den Daten aus dem Mein Unternehmenskonto befüllt."
                            emptyStatePlaceholder="Keine Attribute für diesen Element-Typ im Mein Unternehmenskonto (MUK) vorhanden"
                            disabled={!props.editable}
                        />
                    }
                </Box>
            }

            {
                isAnyInputElement(props.elementModel) &&
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
                        value={props.elementModel.destinationKey ?? undefined}
                        label="HTTP-Schnittstellen-Schlüssel"
                        onChange={(val) => {
                            props.onChange({
                                ...props.elementModel,
                                destinationKey: val,
                            });
                        }}
                        hint="Dieser Schlüssel wird statt der Feld-ID verwendet, wenn die Daten an eine HTTP-Schnittstelle gesendet werden."
                        disabled={!props.editable}
                    />
                </Box>
            }
        </Box>
    );
}
