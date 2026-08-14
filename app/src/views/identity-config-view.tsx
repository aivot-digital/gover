import {Box, Button, FormHelperText, Grid, Stack, Tooltip, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {alpha} from '@mui/material/styles';
import {BaseViewProps} from './base-view';
import {IdentityConfigElement, IdentityConfigElementSlot} from '../models/elements/form/input/identity-config-element';
import {IdentityProvidersApiService} from '../modules/identity/identity-providers-api-service';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {IdentityProviderListDTO} from '../modules/identity/models/identity-provider-list-dto';
import {isApiError} from '../models/api-error';
import Add from '@aivot/mui-material-symbols-400-n25-outlined/Add';
import AccountCircle from '@aivot/mui-material-symbols-400-n25-outlined/AccountCircle';
import {generateId} from '../utils/id-utils';
import {TextFieldComponent} from '../components/text-field/text-field-component';
import {CheckboxFieldComponent} from '../components/checkbox-field/checkbox-field-component';
import {RichTextInputComponent} from '../components/rich-text-input-component/rich-text-input-component';
import {IdentityProviderType} from '../modules/identity/enums/identity-provider-type';
import {DialogList, DialogListPropsDialogContentComponent} from '../components/dialog-list/dialog-list';
import {SelectFieldComponent} from '../components/select-field-2/select-field-component';
import {BundIdAccessLevelOptions} from '../modules/identity/enums/bund-id-access-level';
import {BayernIdAccessLevelOptions} from '../modules/identity/enums/bayern-id-access-level';
import {ShIdAccessLevelOptions} from '../modules/identity/enums/sh-id-access-level';
import {isStringNullOrEmpty} from '../utils/string-utils';
import {ElementEditorSectionHeader} from '../components/element-editor-section-header/element-editor-section-header';

export function IdentityConfigView(props: BaseViewProps<IdentityConfigElement, IdentityConfigElementSlot[]>) {
    const {
        element,
        value,
        setValue,
        errors,
        isBusy: isGloballyDisabled,
        isDeriving,
    } = props;

    const [providers, setProviders] = useState<IdentityProviderListDTO[]>([]);
    const [providersError, setProvidersError] = useState<string>();
    const [isLoadingProviders, setIsLoadingProviders] = useState(false);

    useEffect(() => {
        setIsLoadingProviders(true);

        new IdentityProvidersApiService()
            .listAll()
            .then((res) => {
                setProviders(res.content);
            })
            .catch((err) => {
                if (isApiError(err) && err.displayableToUser) {
                    setProvidersError(err.message);
                } else {
                    setProvidersError('Beim Abruf der Identitätsanbieter ist ein unbekannter Fehler aufgetreten.');
                }
            })
            .finally(() => {
                setIsLoadingProviders(false);
            });
    }, []);

    const isDisabled = useMemo(() => {
        return Boolean(element.disabled) || isGloballyDisabled;
    }, [element.disabled, isGloballyDisabled]);

    const isFieldBusy = useMemo(() => {
        return (isDeriving && hasDerivableAspects(element)) || isLoadingProviders;
    }, [element, isDeriving, isLoadingProviders]);

    const Component = useMemo(() => {
        return wrapIdentityConfigSlot(providers);
    }, [providers]);

    const shouldShowEmptyState = (value?.length ?? 0) === 0;
    const errorText = [
        errors?.join(' '),
        providersError,
    ]
        .filter((part) => part != null && part.length > 0)
        .join(' ');

    const handleAddSlot = () => {
        setValue([
            ...(value ?? []),
            {
                id: generateId(10),
                title: null,
                description: null,
                allowsMail: false,
                isOptional: false,
                options: [],
            },
        ]);
    };

    const handleSlotChanged = (edited: IdentityConfigElementSlot, original: IdentityConfigElementSlot) => {
        const slotIndex = value?.indexOf(original);

        if (slotIndex == null || slotIndex < 0) {
            return;
        }

        const changedValue = [
            ...(value ?? []),
        ];
        changedValue[slotIndex] = edited;
        setValue(changedValue);
    };

    const handleDelete = (item: IdentityConfigElementSlot) => {
        const slotIndex = value?.indexOf(item);

        if (slotIndex == null || slotIndex < 0) {
            return;
        }

        const changedValue = [
            ...(value ?? []),
        ];
        changedValue.splice(slotIndex, 1);
        setValue(changedValue);
    };

    return (
        <Box>
            <Stack
                direction="row"
                spacing={2}
                sx={{
                    alignItems: "center",
                    justifyContent: "space-between"
                }}>
                <Typography variant="subtitle2">
                    {element.label}{element.required ? ' *' : ''}
                </Typography>

                <Button
                    variant="outlined"
                    size="small"
                    startIcon={<Add/>}
                    disabled={element.disabled || isDisabled || isFieldBusy}
                    onClick={handleAddSlot}
                >
                    Hinzufügen
                </Button>
            </Stack>

            {
                shouldShowEmptyState &&
                <Box
                    sx={(theme) => ({
                        px: 1.5,
                        py: 1.25,
                        mt: 0.75,
                        minHeight: 56,
                        display: 'flex',
                        alignItems: 'center',
                        borderRadius: 1,
                        border: errors != null || providersError != null
                            ? '1px solid'
                            : '1px dashed',
                        borderColor: errors != null || providersError != null
                            ? theme.palette.error.main
                            : alpha(theme.palette.text.primary, 0.18),
                        textAlign: 'left',
                    })}
                >
                    <Stack
                        direction="row"
                        spacing={1}
                        sx={{
                            alignItems: "center"
                        }}
                    >
                        <AccountCircle
                            sx={{
                                flexShrink: 0,
                                fontSize: 20,
                                color: 'text.secondary',
                            }}
                        />
                        <Typography
                            variant="body2"
                            sx={{
                                color: "text.secondary",
                                minWidth: 0
                            }}>
                            Keine Identitäten vorhanden.{' '}
                            {
                                element.required &&
                                <>Mindestens eine Identität ist erforderlich.</>
                            }
                        </Typography>
                    </Stack>
                </Box>
            }

            {
                !shouldShowEmptyState &&
                <Stack
                    direction="column"
                    spacing={2}
                    sx={{
                        mt: 0.75,
                    }}
                >
                    <DialogList
                        dialogTitle="Identität bearbeiten"
                        dialogViewTitle="Identität ansehen"
                        getId={(i) => i.id ?? ''}
                        items={value ?? []}
                        title={getIdentityDisplayName}
                        subTitle={(i) => getIdentityConfigSubtitle(i, isDisabled || isFieldBusy)}
                        dialogContentComponent={Component}
                        onDialogSave={handleSlotChanged}
                        onDelete={handleDelete}
                        disabled={element.disabled || isDisabled || isFieldBusy}
                    />
                </Stack>
            }

            {
                errorText.length > 0 &&
                <FormHelperText
                    error
                    sx={{
                        mx: 1.75,
                        mt: shouldShowEmptyState ? 0.75 : -1,
                    }}
                >
                    {errorText}
                </FormHelperText>
            }
        </Box>
    );
}

function wrapIdentityConfigSlot(providers: IdentityProviderListDTO[]): DialogListPropsDialogContentComponent<IdentityConfigElementSlot> {
    return (props: {
        item: IdentityConfigElementSlot,
        onChange: (item: IdentityConfigElementSlot) => void,
        disabled?: boolean
    }) => (
        <IdentityConfigSlot
            item={props.item}
            onChange={props.onChange}
            providers={providers}
            disabled={props.disabled}
        />
    );
}

function getIdentityConfigSubtitle(item: IdentityConfigElementSlot, isReadonly: boolean): string {
    const optionCount = item.options?.length ?? 0;
    const providerText = optionCount === 1 ? '1 Nutzerkontenanbieter' : `${optionCount} Nutzerkontenanbieter`;

    return [
        item.isOptional === true ? 'Optional' : 'Verpflichtend',
        providerText,
        isReadonly ? 'Zum Anzeigen öffnen' : undefined,
    ]
        .filter((part) => part != null)
        .join(' · ');
}

function getIdentityDisplayName(identity: Pick<IdentityConfigElementSlot, 'title'>): string {
    const title = identity.title?.trim();

    return title != null && title.length > 0 ? title : 'Unbenannte Identität';
}

function IdentityConfigSlot(props: {
    item: IdentityConfigElementSlot;
    onChange: (value: IdentityConfigElementSlot) => void;
    disabled?: boolean;
    providers: IdentityProviderListDTO[];
}) {
    const {
        item,
        onChange,
        disabled = false,
        providers,
    } = props;

    return (
        <Stack
            spacing={3.5}
            sx={{
                pt: 1,
            }}
        >
            <Box>
                <Grid
                    container
                    spacing={2}
                    sx={{
                        alignItems: "flex-start"
                    }}
                >
                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <TextFieldComponent
                            label="Eindeutiger Schlüssel"
                            hint="Über diesen eindeutigen Schlüssel werden die Informationen zu dieser Identität im Prozess identifiziert."
                            value={item.id}
                            onChange={(val) => {
                                onChange({
                                    ...item,
                                    id: val ?? '',
                                });
                            }}
                            pattern={{
                                regex: '[a-zA-Z0-9_]+',
                                message: 'Der eindeutige Schlüssel darf nur Buchstaben (außer Umlaute), Zahlen, Unterstriche enthalten.',
                            }}
                            minCharacters={1}
                            maxCharacters={32}
                            muiPassTroughProps={{
                                margin: 'none',
                            }}
                            required={true}
                            disabled={disabled}
                        />
                    </Grid>

                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <TextFieldComponent
                            label="Titel"
                            hint="Dieser Titel wird Nutzer:innen angezeigt."
                            value={item.title}
                            onChange={(val) => {
                                onChange({
                                    ...item,
                                    title: val ?? '',
                                });
                            }}
                            required={true}
                            disabled={disabled}
                            muiPassTroughProps={{
                                margin: 'none',
                            }}
                        />
                    </Grid>

                    <Grid size={{xs: 12}}>
                        <RichTextInputComponent
                            label="Beschreibung"
                            hint="Optionale Beschreibung, die Nutzer:innen angezeigt wird. Sie kann erklären, warum eine Anmeldung mit dieser Identität sinnvoll oder notwendig ist."
                            value={item.description}
                            onChange={(val) => {
                                onChange({
                                    ...item,
                                    description: isStringNullOrEmpty(val) ? null : val,
                                });
                            }}
                            disabled={disabled}
                            sx={{
                                mb: 0,
                            }}
                        />
                    </Grid>
                </Grid>
            </Box>

            <Box>
                <Typography
                    variant="h5"
                    component="div"
                    sx={{
                        mb: 2,
                    }}
                >
                    Einstellungen
                </Typography>

                <Grid
                    container
                    spacing={2}
                >
                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <CheckboxFieldComponent
                            label="Optional"
                            hint="Ist eine Identität optional, muss sie nicht angegeben werden."
                            variant="switch"
                            value={item.isOptional ?? false}
                            onChange={(val) => {
                                onChange({
                                    ...item,
                                    isOptional: val,
                                });
                            }}
                            sx={{
                                my: 0,
                            }}
                            disabled={disabled}
                        />
                    </Grid>

                    <Grid
                        size={{
                            xs: 12,
                            md: 6,
                        }}
                    >
                        <Tooltip title="Eine alternative Nutzung von E-Mail als Kommunikationskanal wird in einer zukünftigen Version ermöglicht.">
                            <span>
                                <CheckboxFieldComponent
                                    label="E-Mail"
                                    hint="Statt einer Anmeldung mittels Identitätsanbieter kann auch lediglich eine E-Mail-Adresse angegeben werden."
                                    variant="switch"
                                    value={item.allowsMail ?? false}
                                    onChange={(val) => {
                                        onChange({
                                            ...item,
                                            allowsMail: val,
                                        });
                                    }}
                                    sx={{
                                        my: 0,
                                    }}
                                    disabled={true}
                                />
                            </span>
                        </Tooltip>
                    </Grid>
                </Grid>
            </Box>

            <Box>
                <ElementEditorSectionHeader
                    title="Aktive Nutzerkontenanbieter"
                    variant={"h5"}
                    disableMarginTop
                >
                    Legen Sie fest, welche Anbieter für diese Identität angeboten werden und welches Vertrauensniveau
                    mindestens erforderlich ist.
                </ElementEditorSectionHeader>

                <Grid
                    container
                    spacing={2}
                    sx={{mt: 4}}
                >
                    {
                        providers
                            .map((providerOption, index) => (
                                <Grid
                                    key={index}
                                    size={{
                                        xs: 12,
                                        md: 6,
                                    }}
                                >
                                    <ProviderOption
                                        provider={providerOption}
                                        item={item}
                                        onChange={onChange}
                                        disabled={disabled}
                                    />
                                </Grid>
                            ))

                    }
                </Grid>
            </Box>
        </Stack>
    );
}

function ProviderOption(props: {
    provider: IdentityProviderListDTO;
    item: IdentityConfigElementSlot;
    onChange: (value: IdentityConfigElementSlot) => void;
    disabled: boolean;
}) {
    const {
        provider: provider,
        item,
        onChange,
        disabled,
    } = props;

    const relatedOption = item.options?.find(opt => opt.identityProviderKey === provider.key);
    const selected = relatedOption != null;

    const handleToggle = (checked: boolean) => {
        if (checked) {
            onChange({
                ...item,
                options: [
                    ...(item.options ?? []),
                    {
                        identityProviderKey: provider.key,
                        additionalScopes: [],
                    },
                ],
            });
        } else {
            onChange({
                ...item,
                options: (item.options ?? [])
                    .filter(i => i.identityProviderKey !== provider.key),
            });
        }
    };

    const handleScopesChange = (scopeValue: string | null) => {
        onChange({
            ...item,
            options: (item.options ?? [])
                .map((opt) =>
                    opt.identityProviderKey === provider.key
                        ? {
                            ...opt,
                            additionalScopes: scopeValue == null ? [] : [scopeValue],
                        }
                        : opt,
                ),
        });
    };

    return (
        <Stack
            direction="column"
            spacing={1.5}
            sx={{
                height: '100%',
                p: 1.5,
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
            }}
        >
            <CheckboxFieldComponent
                label={provider.name}
                hint={provider.isTestProvider ? 'Es handelt sich um einen vor-produktiven Identitätsanbieter.' : undefined}
                value={selected}
                onChange={handleToggle}
                variant="switch"
                disabled={disabled}
            />
            {
                selected &&
                provider.type === IdentityProviderType.BayernID &&
                <SelectFieldComponent
                    label="Mindest-Vertrauensniveau"
                    value={relatedOption?.additionalScopes?.[0]}
                    onChange={handleScopesChange}
                    options={BayernIdAccessLevelOptions}
                    required={true}
                    disabled={disabled}
                />
            }
            {
                selected &&
                provider.type === IdentityProviderType.BundID &&
                <SelectFieldComponent
                    label="Mindest-Vertrauensniveau"
                    value={relatedOption?.additionalScopes?.[0]}
                    onChange={handleScopesChange}
                    options={BundIdAccessLevelOptions}
                    required={true}
                    disabled={disabled}
                />
            }
            {
                selected &&
                provider.type === IdentityProviderType.SHID &&
                <SelectFieldComponent
                    label="Mindest-Vertrauensniveau"
                    value={relatedOption?.additionalScopes?.[0]}
                    onChange={handleScopesChange}
                    options={ShIdAccessLevelOptions}
                    required={true}
                    disabled={disabled}
                />
            }
        </Stack>
    );
}
