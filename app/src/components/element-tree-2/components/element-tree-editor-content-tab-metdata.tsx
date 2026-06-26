import {useElementTreeEditorContext} from './element-tree-editor-context';
import React, {useMemo} from 'react';
import {ElementEditorSectionHeader} from '../../element-editor-section-header/element-editor-section-header';
import {useElementTreeContext} from '../element-tree-context';
import {AnyInputElement} from '../../../models/elements/form/input/any-input-element';
import {SelectFieldComponent} from '../../select-field-2/select-field-component';
import {Box, Divider, Grid, Stack, Typography} from '@mui/material';

interface ElementTreeEditorContentTabMetadataProps {
    editable: boolean;
}

function getIdentityDisplayName(identity: { title: string | null }): string {
    const title = identity.title?.trim();

    return title != null && title.length > 0 ? title : 'Unbenannte Identität';
}

export function ElementTreeEditorContentTabMetadata<T extends AnyInputElement>(props: ElementTreeEditorContentTabMetadataProps) {
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
    }, [currentElement.metadata?.identitySourceId, identityMappingInformation]);

    return (
        <>
            <ElementEditorSectionHeader
                title="Identitätsdaten"
                disableMarginTop
            >
                Hier können Sie festlegen, welche Attribute angebundener Identitätsanbieter diesem Element zugeordnet
                werden.
                So kann das Feld bei vorhandenen Identitätsdaten automatisch vorausgefüllt werden.
            </ElementEditorSectionHeader>

            <Stack spacing={4} sx={{mt: -2}}>
                <Box>
                    <ElementEditorSectionHeader
                        title="Zugeordnete Identität"
                        variant={"h5"}
                    >
                        Wählen Sie aus, aus welcher Identität die Daten für dieses Formularelement übernommen werden
                        sollen.
                    </ElementEditorSectionHeader>

                    <Grid
                        container
                        spacing={2}
                        sx={{
                            mt: 1,
                        }}
                    >
                        <Grid
                            size={{
                                xs: 12,
                                md: 6,
                            }}
                        >
                            <SelectFieldComponent
                                label="Zugeordnete Identität"
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
                                            label: getIdentityDisplayName(identity),
                                            subLabel: identity.id ?? undefined,
                                            value: identity.id ?? '',
                                        }))
                                }
                                disabled={!props.editable}
                            />
                        </Grid>
                    </Grid>
                </Box>

                {
                    assignedIdentity != null &&
                    assignedIdentity.options != null &&
                    <Box>
                        <Divider sx={{mb: 3}}/>

                        <ElementEditorSectionHeader
                            title="Attribute zuordnen"
                            variant={"h5"}
                        >
                            Ordnen Sie nun die Attribute der Identitätsanbieter dem ausgewählten Formularelement zu.
                            Pro angebundenem Anbieter kann ein passendes Attribut ausgewählt werden.
                        </ElementEditorSectionHeader>

                        <Grid
                            container
                            spacing={2}
                        >
                            {
                                assignedIdentity.options
                                    .map((idpOption) => (
                                        <Grid
                                            key={idpOption.provider.metadataIdentifier}
                                            size={{
                                                xs: 12,
                                                md: 6,
                                            }}
                                        >
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
                                                disabled={!props.editable}
                                            />
                                        </Grid>
                                    ))
                            }
                        </Grid>
                    </Box>
                }
            </Stack>
        </>
    );
}
