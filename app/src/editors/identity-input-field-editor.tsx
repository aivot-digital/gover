import {Alert, Box, Button, Divider, IconButton, Paper, Typography} from '@mui/material';
import Grid from '@mui/material/Grid';
import AddOutlinedIcon from '@mui/icons-material/AddOutlined';
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined';
import {useEffect, useMemo, useState} from 'react';
import {BaseEditorProps} from './base-editor';
import {
    IdentityInputFieldElement,
    IdentityInputFieldOption,
    IdentityInputFieldOptionAttributeMapping,
} from '../models/elements/form/input/identity-input-field-element';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {useApi} from '../hooks/use-api';
import {IdentityProvidersApiService} from '../modules/identity/identity-providers-api-service';
import {IdentityProviderListDTO} from '../modules/identity/models/identity-provider-list-dto';
import {SelectFieldComponent} from '../components/select-field-2/select-field-component';
import {ChipInputFieldComponent} from '../components/chip-input-field/chip-input-field-component';
import {useElementTreeContext} from '../components/element-tree-2/element-tree-context';
import {generateComponentPath, generateComponentTitle} from '../utils/generate-component-title';
import {SelectElementDialog} from '../dialogs/select-element-dialog/select-element-dialog';
import {isAnyInputElement} from '../models/elements/form/input/any-input-element';
import {ElementType} from '../data/element-type/element-type';
import {isElementNestedInReplicatingContainer} from '../utils/identity-input-field-utils';

function createEmptyIdentityInputOption(): IdentityInputFieldOption {
    return {
        identityProviderKey: undefined,
        additionalScopes: [],
        attributeMappings: [],
    };
}

function createEmptyAttributeMapping(): IdentityInputFieldOptionAttributeMapping {
    return {
        fromIdentityProviderAttribute: undefined,
        toFormElementWithId: undefined,
    };
}

export function IdentityInputFieldEditor(props: BaseEditorProps<IdentityInputFieldElement>) {
    const {
        element,
        editable,
        onPatch,
    } = props;

    const api = useApi();
    const {
        allElements,
    } = useElementTreeContext();

    const [providers, setProviders] = useState<IdentityProviderListDTO[]>([]);
    const [providersError, setProvidersError] = useState<string>();
    const [activeTargetSelection, setActiveTargetSelection] = useState<{
        optionIndex: number;
        mappingIndex: number;
    } | null>(null);

    const options = useMemo(() => element.options ?? [], [element.options]);

    const selectableTargetElements = useMemo(() => {
        return allElements.filter(({element: candidate, parents}) => {
            if (!isAnyInputElement(candidate) || candidate.id === element.id) {
                return false;
            }

            if (parents.some((parent) => parent.type === ElementType.SummaryLayout)) {
                return false;
            }

            return !isElementNestedInReplicatingContainer(parents);
        });
    }, [allElements, element.id]);

    useEffect(() => {
        let cancelled = false;

        new IdentityProvidersApiService(api)
            .listAll({
                isEnabled: true,
            })
            .then((response) => {
                if (!cancelled) {
                    setProviders(response.content);
                }
            })
            .catch((error) => {
                console.error('Error loading identity providers:', error);
                if (!cancelled) {
                    setProviders([]);
                    setProvidersError('Die Identifizierungsanbieter konnten nicht geladen werden.');
                }
            });

        return () => {
            cancelled = true;
        };
    }, [api]);

    const patchOptions = (nextOptions: IdentityInputFieldOption[]) => {
        onPatch({
            options: nextOptions,
        });
    };

    const patchOption = (optionIndex: number, optionPatch: Partial<IdentityInputFieldOption>) => {
        const nextOptions = [...options];
        nextOptions[optionIndex] = {
            ...createEmptyIdentityInputOption(),
            ...nextOptions[optionIndex],
            ...optionPatch,
        };
        patchOptions(nextOptions);
    };

    const patchAttributeMapping = (
        optionIndex: number,
        mappingIndex: number,
        mappingPatch: Partial<IdentityInputFieldOptionAttributeMapping>,
    ) => {
        const nextOptions = [...options];
        const currentOption = {
            ...createEmptyIdentityInputOption(),
            ...nextOptions[optionIndex],
        };
        const nextMappings = [...(currentOption.attributeMappings ?? [])];
        nextMappings[mappingIndex] = {
            ...createEmptyAttributeMapping(),
            ...nextMappings[mappingIndex],
            ...mappingPatch,
        };

        nextOptions[optionIndex] = {
            ...currentOption,
            attributeMappings: nextMappings,
        };

        patchOptions(nextOptions);
    };

    return (
        <>
            <Grid
                container
                columnSpacing={4}
                rowSpacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <CheckboxFieldComponent
                        label="Alternative E-Mail-Eingabe erlauben"
                        hint="Wenn aktiviert, koennen antragstellende Personen statt der Identifizierung eine E-Mail-Adresse angeben."
                        value={element.allowsMail ?? false}
                        onChange={(allowsMail) => {
                            onPatch({
                                allowsMail,
                            });
                        }}
                        variant="switch"
                        disabled={!editable}
                    />
                </Grid>

                <Grid size={12}>
                    <Alert severity="info">
                        Attributzuordnungen in Wiederholungscontainern sind nicht verfuegbar.
                    </Alert>
                </Grid>

                {
                    providersError != null &&
                    <Grid size={12}>
                        <Alert severity="error">
                            {providersError}
                        </Alert>
                    </Grid>
                }

                <Grid size={12}>
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            mb: 1,
                        }}
                    >
                        <Typography variant="subtitle1">
                            Identifizierungsoptionen
                        </Typography>

                        <Button
                            startIcon={<AddOutlinedIcon />}
                            onClick={() => {
                                patchOptions([
                                    ...options,
                                    createEmptyIdentityInputOption(),
                                ]);
                            }}
                            disabled={!editable}
                        >
                            Option hinzufuegen
                        </Button>
                    </Box>

                    {
                        options.length === 0 &&
                        <Alert severity="warning">
                            Es ist noch keine Identifizierungsoption konfiguriert.
                        </Alert>
                    }

                    <Grid
                        container
                        rowSpacing={2}
                    >
                        {
                            options.map((option, optionIndex) => {
                                const selectedProvider = providers.find((provider) => provider.key === option.identityProviderKey);
                                const providerOptions = createProviderOptions(providers, option.identityProviderKey);
                                const mappingTargetMap = new Map(
                                    selectableTargetElements.map((entry) => [
                                        entry.element.id,
                                        {
                                            title: generateComponentTitle(entry.element),
                                            path: generateComponentPath(entry.parents),
                                        },
                                    ]),
                                );

                                return (
                                    <Grid
                                        key={`identity-option-${optionIndex}`}
                                        size={12}
                                    >
                                        <Paper
                                            variant="outlined"
                                            sx={{
                                                p: 2,
                                            }}
                                        >
                                            <Box
                                                sx={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    justifyContent: 'space-between',
                                                    gap: 2,
                                                    mb: 2,
                                                }}
                                            >
                                                <Typography variant="subtitle2">
                                                    Option {optionIndex + 1}
                                                </Typography>

                                                <IconButton
                                                    color="error"
                                                    onClick={() => {
                                                        const nextOptions = [...options];
                                                        nextOptions.splice(optionIndex, 1);
                                                        patchOptions(nextOptions);
                                                    }}
                                                    disabled={!editable}
                                                >
                                                    <DeleteOutlineOutlinedIcon />
                                                </IconButton>
                                            </Box>

                                            <Grid
                                                container
                                                columnSpacing={4}
                                                rowSpacing={2}
                                            >
                                                <Grid
                                                    size={{
                                                        xs: 12,
                                                        lg: 6,
                                                    }}
                                                >
                                                    <SelectFieldComponent
                                                        label="Identifizierungsanbieter"
                                                        value={option.identityProviderKey ?? undefined}
                                                        onChange={(identityProviderKey) => {
                                                            patchOption(optionIndex, {
                                                                identityProviderKey,
                                                            });
                                                        }}
                                                        options={providerOptions}
                                                        hint="Alle aktivierten Identifizierungsanbieter stehen hier zur Auswahl."
                                                        disabled={!editable}
                                                        emptyStatePlaceholder="Keine aktivierten Anbieter verfuegbar"
                                                    />
                                                </Grid>

                                                <Grid
                                                    size={{
                                                        xs: 12,
                                                        lg: 6,
                                                    }}
                                                >
                                                    <ChipInputFieldComponent
                                                        label="Zusaetzliche Scopes"
                                                        value={option.additionalScopes}
                                                        onChange={(additionalScopes) => {
                                                            patchOption(optionIndex, {
                                                                additionalScopes,
                                                            });
                                                        }}
                                                        hint="Optionale zusaetzliche Berechtigungen fuer den gewaehlten Anbieter."
                                                        placeholder="Scope hinzufuegen"
                                                        disabled={!editable}
                                                        allowDuplicates={false}
                                                    />
                                                </Grid>

                                                <Grid size={12}>
                                                    <Divider sx={{my: 0.5}}>
                                                        Attributzuordnungen
                                                    </Divider>
                                                </Grid>

                                                <Grid size={12}>
                                                    <Box
                                                        sx={{
                                                            display: 'flex',
                                                            justifyContent: 'space-between',
                                                            alignItems: 'center',
                                                            gap: 2,
                                                            mb: 1,
                                                        }}
                                                    >
                                                        <Typography variant="body2">
                                                            Attribute des Identifizierungsanbieters werden in Formularfelder ausserhalb von Wiederholungscontainern uebernommen.
                                                        </Typography>

                                                        <Button
                                                            size="small"
                                                            startIcon={<AddOutlinedIcon />}
                                                            onClick={() => {
                                                                patchOption(optionIndex, {
                                                                    attributeMappings: [
                                                                        ...(option.attributeMappings ?? []),
                                                                        createEmptyAttributeMapping(),
                                                                    ],
                                                                });
                                                            }}
                                                            disabled={!editable}
                                                        >
                                                            Zuordnung
                                                        </Button>
                                                    </Box>

                                                    <Grid
                                                        container
                                                        rowSpacing={1.5}
                                                    >
                                                        {
                                                            (option.attributeMappings ?? []).map((mapping, mappingIndex) => {
                                                                const selectedTarget = mapping.toFormElementWithId != null ? mappingTargetMap.get(mapping.toFormElementWithId) : undefined;
                                                                const attributeOptions = createAttributeOptions(
                                                                    selectedProvider,
                                                                    mapping.fromIdentityProviderAttribute,
                                                                );

                                                                return (
                                                                    <Grid
                                                                        key={`identity-option-${optionIndex}-mapping-${mappingIndex}`}
                                                                        size={12}
                                                                    >
                                                                        <Paper
                                                                            variant="outlined"
                                                                            sx={{
                                                                                p: 2,
                                                                            }}
                                                                        >
                                                                            <Grid
                                                                                container
                                                                                columnSpacing={3}
                                                                                rowSpacing={2}
                                                                            >
                                                                                <Grid
                                                                                    size={{
                                                                                        xs: 12,
                                                                                        lg: 4,
                                                                                    }}
                                                                                >
                                                                                    <SelectFieldComponent
                                                                                        label="Anbieterattribut"
                                                                                        value={mapping.fromIdentityProviderAttribute ?? undefined}
                                                                                        onChange={(fromIdentityProviderAttribute) => {
                                                                                            patchAttributeMapping(optionIndex, mappingIndex, {
                                                                                                fromIdentityProviderAttribute,
                                                                                            });
                                                                                        }}
                                                                                        options={attributeOptions}
                                                                                        hint="Nur Attribute des aktuell gewaehlten Anbieters stehen zur Auswahl."
                                                                                        disabled={!editable || selectedProvider == null}
                                                                                        emptyStatePlaceholder="Keine Anbieterattribute verfuegbar"
                                                                                    />
                                                                                </Grid>

                                                                                <Grid
                                                                                    size={{
                                                                                        xs: 12,
                                                                                        lg: 7,
                                                                                    }}
                                                                                >
                                                                                    <Button
                                                                                        variant="outlined"
                                                                                        fullWidth
                                                                                        onClick={() => {
                                                                                            setActiveTargetSelection({
                                                                                                optionIndex,
                                                                                                mappingIndex,
                                                                                            });
                                                                                        }}
                                                                                        disabled={!editable}
                                                                                        sx={{
                                                                                            justifyContent: 'flex-start',
                                                                                            textTransform: 'none',
                                                                                            minHeight: 56,
                                                                                            px: 2,
                                                                                        }}
                                                                                    >
                                                                                        <Box
                                                                                            sx={{
                                                                                                textAlign: 'left',
                                                                                            }}
                                                                                        >
                                                                                            <Typography
                                                                                                component="span"
                                                                                                sx={{
                                                                                                    display: 'block',
                                                                                                    fontWeight: 600,
                                                                                                }}
                                                                                            >
                                                                                                {selectedTarget?.title ?? 'Zielelement auswaehlen'}
                                                                                            </Typography>
                                                                                            <Typography
                                                                                                component="span"
                                                                                                variant="body2"
                                                                                                sx={{
                                                                                                    display: 'block',
                                                                                                    color: 'text.secondary',
                                                                                                }}
                                                                                            >
                                                                                                {selectedTarget?.path ?? 'Wiederholungscontainer werden nicht angeboten.'}
                                                                                            </Typography>
                                                                                        </Box>
                                                                                    </Button>
                                                                                </Grid>

                                                                                <Grid
                                                                                    size={{
                                                                                        xs: 12,
                                                                                        lg: 1,
                                                                                    }}
                                                                                    sx={{
                                                                                        display: 'flex',
                                                                                        justifyContent: {
                                                                                            xs: 'flex-end',
                                                                                            lg: 'center',
                                                                                        },
                                                                                    }}
                                                                                >
                                                                                    <IconButton
                                                                                        color="error"
                                                                                        onClick={() => {
                                                                                            const nextMappings = [...(option.attributeMappings ?? [])];
                                                                                            nextMappings.splice(mappingIndex, 1);
                                                                                            patchOption(optionIndex, {
                                                                                                attributeMappings: nextMappings,
                                                                                            });
                                                                                        }}
                                                                                        disabled={!editable}
                                                                                    >
                                                                                        <DeleteOutlineOutlinedIcon />
                                                                                    </IconButton>
                                                                                </Grid>
                                                                            </Grid>
                                                                        </Paper>
                                                                    </Grid>
                                                                );
                                                            })
                                                        }
                                                    </Grid>
                                                </Grid>
                                            </Grid>
                                        </Paper>
                                    </Grid>
                                );
                            })
                        }
                    </Grid>
                </Grid>
            </Grid>

            <SelectElementDialog
                allElements={selectableTargetElements}
                open={activeTargetSelection != null}
                onSelect={(selectedElement) => {
                    if (activeTargetSelection == null) {
                        return;
                    }

                    patchAttributeMapping(
                        activeTargetSelection.optionIndex,
                        activeTargetSelection.mappingIndex,
                        {
                            toFormElementWithId: selectedElement.id,
                        },
                    );
                    setActiveTargetSelection(null);
                }}
                onClose={() => {
                    setActiveTargetSelection(null);
                }}
            />
        </>
    );
}

function createProviderOptions(
    providers: IdentityProviderListDTO[],
    selectedProviderKey: string | null | undefined,
) {
    const options = providers.map((provider) => ({
        label: provider.name,
        subLabel: provider.metadataIdentifier,
        value: provider.key,
    }));

    if (selectedProviderKey != null && !providers.some((provider) => provider.key === selectedProviderKey)) {
        options.push({
            label: 'Nicht verfuegbarer Anbieter',
            subLabel: selectedProviderKey,
            value: selectedProviderKey,
        });
    }

    return options;
}

function createAttributeOptions(
    provider: IdentityProviderListDTO | undefined,
    selectedAttributeKey: string | null | undefined,
) {
    const options = (provider?.attributes ?? []).map((attribute) => ({
        label: attribute.label,
        subLabel: `${attribute.keyInData}${attribute.description != null && attribute.description.length > 0 ? ` - ${attribute.description}` : ''}`,
        value: attribute.keyInData,
    }));

    if (selectedAttributeKey != null && !options.some((option) => option.value === selectedAttributeKey)) {
        options.push({
            label: 'Nicht verfuegbares Attribut',
            subLabel: selectedAttributeKey,
            value: selectedAttributeKey,
        });
    }

    return options;
}
