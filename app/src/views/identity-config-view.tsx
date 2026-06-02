import {Box, Button, Divider, Grid, Stack, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {BaseViewProps} from './base-view';
import {IdentityConfigElement, IdentityConfigElementSlot} from '../models/elements/form/input/identity-config-element';
import {IdentityProvidersApiService} from '../modules/identity/identity-providers-api-service';
import {hasDerivableAspects} from '../utils/has-derivable-aspects';
import {IdentityProviderListDTO} from '../modules/identity/models/identity-provider-list-dto';
import {isApiError} from '../models/api-error';
import Add from '@aivot/mui-material-symbols-400-outlined/dist/add/Add';
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

    const handleAddSlot = () => {
        setValue([
            ...value ?? [],
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
            ...value ?? [],
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
            ...value ?? [],
        ];
        changedValue.splice(slotIndex, 1);
        setValue(changedValue);
    };

    return (
        <Box>
            <Stack
                direction="row"
                spacing={2}
                alignItems="center"
                justifyContent="space-between"
                marginBottom={0.75}
            >
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

            <Stack
                direction="column"
                spacing={2}
            >
                <DialogList
                    dialogTitle="Identität bearbeiten"
                    getId={(i) => i.id ?? ''}
                    items={value ?? []}
                    title={(i) => i.title ?? 'Unbenannte Identität'}
                    dialogContentComponent={Component}
                    onDialogSave={handleSlotChanged}
                    onDelete={handleDelete}
                    disabled={element.disabled || isDisabled || isFieldBusy}
                />
            </Stack>

            {
                (errors != null || providersError != null) &&
                <Typography
                    variant="body2"
                    color="error"
                >
                    {
                        errors != null &&
                        errors.join(' ')
                    }
                    {providersError}
                </Typography>
            }
        </Box>
    );
}

function wrapIdentityConfigSlot(providers: IdentityProviderListDTO[]): DialogListPropsDialogContentComponent<IdentityConfigElementSlot> {
    return (props: { item: IdentityConfigElementSlot, onChange: (item: IdentityConfigElementSlot) => void, disabled?: boolean }) => (
        <IdentityConfigSlot
            item={props.item}
            onChange={props.onChange}
            providers={providers}
            disabled={props.disabled}
        />
    );
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
        <>
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
                sx={{
                    mt: 1,
                }}
                required={true}
                disabled={disabled}
            />

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
            />

            <RichTextInputComponent
                label="Beschreibung"
                hint="Diese Beschreibung wird Nutzer:innen angezeigt. Sie sollte erklären, warum eine Anmeldung mit dieser Identität notwendig ist."
                value={item.description}
                onChange={(val) => {
                    onChange({
                        ...item,
                        description: val ?? '',
                    });
                }}
                required={true}
                disabled={disabled}
                sx={{
                    mb: 1,
                }}
            />

            <Grid
                container
                spacing={2}
            >
                <Grid
                    size={{
                        xs: 12,
                        lg: 6,
                    }}
                >
                    <CheckboxFieldComponent
                        label="Optional"
                        hint="Ist eine Identität optional, so muss diese nicht angegeben werden."
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
                        lg: 6,
                    }}
                >
                    <CheckboxFieldComponent
                        label="E-Mail"
                        hint="Statt einer Anmeldung mittles Identitätsanbieter kann auch lediglich eine E-Mail-Adresse angegeben werden."
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
                        disabled={disabled}
                    />
                </Grid>
            </Grid>

            <Divider
                sx={{
                    my: 2,
                }}
            />

            <Typography
                variant="h5"
                component="div"
            >
                Aktive Nutzerkontenanbieter
            </Typography>

            <Grid
                container
                spacing={2}
            >
                {
                    providers
                        .map((providerOption, index) => (
                            <Grid
                                key={index}
                                size={{
                                    xs: 12,
                                    lg: 6,
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
        </>
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

    const handleScopesChange = (scopeValue: string | undefined) => {
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
            justifyContent="space-between"
            sx={{
                height: '100%',
            }}
        >
            <CheckboxFieldComponent
                label={provider.name}
                hint={provider.isTestProvider ? 'Es handelt sich im einen vor-produktiven Identitätsanbieter.' : undefined}
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
                    value={relatedOption?.additionalScopes?.[0] ?? undefined}
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
                    value={relatedOption?.additionalScopes?.[0] ?? undefined}
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
                    value={relatedOption?.additionalScopes?.[0] ?? undefined}
                    onChange={handleScopesChange}
                    options={ShIdAccessLevelOptions}
                    required={true}
                    disabled={disabled}
                />
            }
        </Stack>
    );
}