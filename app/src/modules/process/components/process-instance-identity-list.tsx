import React, {type ReactNode, useEffect, useMemo, useState} from 'react';
import {
    Box,
    Divider,
    Skeleton,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Typography,
} from '@mui/material';
import {IdentityProvidersApiService} from '../../identity/identity-providers-api-service';
import {type IdentityData, type IdentityDataMap} from '../../identity/models/identity-data';
import {type IdentityProviderListDTO} from '../../identity/models/identity-provider-list-dto';
import {CommunicationProvidersApiService} from '../../communication/communication-providers-api-service';
import {
    type CommunicationProvider,
    type CommunicationProviderBinding,
} from '../../communication/models';

interface ProcessInstanceIdentityListProps {
    identities: IdentityDataMap;
    canReadIdentityProviders: boolean;
    canReadCommunicationProviders: boolean;
    title?: ReactNode;
}

interface SettledLookup<T> {
    value: T | null;
}

interface IdentityListItem {
    mapKey: string;
    identity: IdentityData;
}

async function settleLookup<T>(request: Promise<T>): Promise<SettledLookup<T>> {
    try {
        return {value: await request};
    } catch {
        return {value: null};
    }
}

function compareLabels(left: string, right: string): number {
    return left.localeCompare(right, 'de', {
        numeric: true,
        sensitivity: 'base',
    });
}

export function ProcessInstanceIdentityList(props: ProcessInstanceIdentityListProps): React.JSX.Element | null {
    const identityItems = useMemo<IdentityListItem[]>(() => (
        Object.entries(props.identities)
            .map(([mapKey, identity]) => ({mapKey, identity}))
            .sort((left, right) => compareLabels(
                left.identity.identityId || left.mapKey,
                right.identity.identityId || right.mapKey,
            ))
    ), [props.identities]);

    const identityProviderKeys = useMemo(() => (
        Array.from(new Set(identityItems
            .filter(({identity}) => identity.type !== 'Email' && identity.providerKey != null)
            .map(({identity}) => identity.providerKey!)
        )).sort(compareLabels)
    ), [identityItems]);

    const communicationProviderKeys = useMemo(() => (
        Array.from(new Set(identityItems
            .filter(({identity}) => (
                identity.type !== 'Email' &&
                identity.providerKey != null &&
                identity.communicationProviderBindingId != null
            ))
            .map(({identity}) => identity.providerKey!)
        )).sort(compareLabels)
    ), [identityItems]);

    const [identityProvidersByKey, setIdentityProvidersByKey] = useState<Record<string, IdentityProviderListDTO>>({});
    const [communicationBindingsById, setCommunicationBindingsById] = useState<Record<number, CommunicationProviderBinding>>({});
    const [communicationProvidersById, setCommunicationProvidersById] = useState<Record<number, CommunicationProvider>>({});
    const [isLoadingIdentityProviders, setIsLoadingIdentityProviders] = useState(false);
    const [isLoadingCommunicationProviders, setIsLoadingCommunicationProviders] = useState(false);

    useEffect(() => {
        let cancelled = false;

        setIdentityProvidersByKey({});
        if (!props.canReadIdentityProviders || identityProviderKeys.length === 0) {
            setIsLoadingIdentityProviders(false);
            return () => {
                cancelled = true;
            };
        }

        setIsLoadingIdentityProviders(true);
        void settleLookup(
            new IdentityProvidersApiService().listAll({keys: identityProviderKeys}),
        ).then((result) => {
            if (cancelled) {
                return;
            }

            const providersByKey: Record<string, IdentityProviderListDTO> = {};
            for (const provider of result.value?.content ?? []) {
                providersByKey[provider.key] = provider;
            }
            setIdentityProvidersByKey(providersByKey);
            setIsLoadingIdentityProviders(false);
        });

        return () => {
            cancelled = true;
        };
    }, [identityProviderKeys, props.canReadIdentityProviders]);

    useEffect(() => {
        let cancelled = false;

        setCommunicationBindingsById({});
        setCommunicationProvidersById({});
        if (!props.canReadCommunicationProviders || communicationProviderKeys.length === 0) {
            setIsLoadingCommunicationProviders(false);
            return () => {
                cancelled = true;
            };
        }

        setIsLoadingCommunicationProviders(true);
        const api = new CommunicationProvidersApiService();

        void Promise.all([
            settleLookup(api.listProviders()),
            Promise.all(communicationProviderKeys.map((providerKey) => (
                settleLookup(api.listBindings(providerKey))
            ))),
        ]).then(([providerResult, bindingResults]) => {
            if (cancelled) {
                return;
            }

            const providersById: Record<number, CommunicationProvider> = {};
            for (const provider of providerResult.value ?? []) {
                providersById[provider.id] = provider;
            }

            const bindingsById: Record<number, CommunicationProviderBinding> = {};
            for (const bindingResult of bindingResults) {
                for (const binding of bindingResult.value ?? []) {
                    bindingsById[binding.id] = binding;
                }
            }

            setCommunicationProvidersById(providersById);
            setCommunicationBindingsById(bindingsById);
            setIsLoadingCommunicationProviders(false);
        });

        return () => {
            cancelled = true;
        };
    }, [communicationProviderKeys, props.canReadCommunicationProviders]);

    if (identityItems.length === 0) {
        return null;
    }

    return (
        <Box>
            {
                props.title !== null &&
                <Typography
                    variant="h6"
                    sx={{mb: 1}}
                >
                    {props.title ?? 'Identitäten'}
                </Typography>
            }

            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 1,
                }}
            >
                {
                    identityItems.map(({mapKey, identity}) => (
                        <IdentityItem
                            key={mapKey}
                            identity={identity}
                            identityProvider={identity.providerKey == null
                                ? null
                                : identityProvidersByKey[identity.providerKey] ?? null}
                            communicationBinding={identity.communicationProviderBindingId == null
                                ? null
                                : communicationBindingsById[identity.communicationProviderBindingId] ?? null}
                            communicationProvidersById={communicationProvidersById}
                            canReadIdentityProviders={props.canReadIdentityProviders}
                            canReadCommunicationProviders={props.canReadCommunicationProviders}
                            isLoadingIdentityProviders={isLoadingIdentityProviders}
                            isLoadingCommunicationProviders={isLoadingCommunicationProviders}
                        />
                    ))
                }
            </Box>
        </Box>
    );
}

interface IdentityItemProps {
    identity: IdentityData;
    identityProvider: IdentityProviderListDTO | null;
    communicationBinding: CommunicationProviderBinding | null;
    communicationProvidersById: Record<number, CommunicationProvider>;
    canReadIdentityProviders: boolean;
    canReadCommunicationProviders: boolean;
    isLoadingIdentityProviders: boolean;
    isLoadingCommunicationProviders: boolean;
}

function IdentityItem(props: IdentityItemProps): React.JSX.Element {
    const identity = props.identity;
    const isEmailIdentity = identity.type === 'Email';
    const identityLabel = identity.identityId || 'Unbenannte Identität';
    const attributes = Object.entries(identity.attributes ?? {}).sort(([left], [right]) => compareLabels(left, right));
    const communicationProvider = props.communicationBinding == null
        ? null
        : props.communicationProvidersById[props.communicationBinding.communicationProviderId] ?? null;

    return (
        <Box
            component="article"
            aria-label={`Identität ${identityLabel}`}
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: '6px',
                p: 1.5,
                backgroundColor: 'background.paper',
                overflow: 'hidden',
            }}
        >
            <Typography
                component="h3"
                sx={{
                    fontWeight: 600,
                    overflowWrap: 'anywhere',
                }}
            >
                {identityLabel}
            </Typography>

            <Box component="dl" sx={{m: 0, mt: 0.5}}>
                <DetailRow label="Typ">
                    {isEmailIdentity ? 'E-Mail-Identität' : 'Nutzerkontenanbieter-Identität'}
                </DetailRow>
                {
                    isEmailIdentity &&
                    <DetailRow label="E-Mail-Adresse">
                        {identity.emailAddress ?? 'Nicht verfügbar'}
                    </DetailRow>
                }
            </Box>

            <Divider sx={{my: 1.5}}/>

            <Typography sx={{fontWeight: 600, mb: 0.5}}>
                Nutzerkontenanbieter
            </Typography>
            {
                isEmailIdentity ?
                    <UnavailableText>
                        Nicht zutreffend – direkte E-Mail-Identität
                    </UnavailableText> :
                    <Box component="dl" sx={{m: 0}}>
                        <DetailRow label="Name">
                            <ResolvedName
                                name={props.identityProvider?.name ?? null}
                                isLoading={props.isLoadingIdentityProviders}
                                hasPermission={props.canReadIdentityProviders}
                            />
                        </DetailRow>
                    </Box>
            }

            <Typography sx={{fontWeight: 600, mt: 1.5, mb: 0.5}}>
                Kommunikationsanbieter
            </Typography>
            {
                isEmailIdentity ?
                    <UnavailableText>
                        Nicht zutreffend – direkter E-Mail-Versand
                    </UnavailableText> :
                    <CommunicationProviderDetails
                        bindingId={identity.communicationProviderBindingId}
                        binding={props.communicationBinding}
                        provider={communicationProvider}
                        isLoading={props.isLoadingCommunicationProviders}
                        hasPermission={props.canReadCommunicationProviders}
                    />
            }

            <Typography sx={{fontWeight: 600, mt: 1.5, mb: 0.5}}>
                Attribute
            </Typography>
            {
                attributes.length === 0 ?
                    <UnavailableText>Keine Attribute vorhanden</UnavailableText> :
                    <TableContainer
                        sx={{
                            border: '1px solid',
                            borderColor: 'divider',
                            borderRadius: 1,
                        }}
                    >
                        <Table
                            size="small"
                            aria-label={`Attribute der Identität ${identityLabel}`}
                        >
                            <TableHead>
                                <TableRow>
                                    <TableCell sx={{fontWeight: 600}}>Attribut</TableCell>
                                    <TableCell sx={{fontWeight: 600}}>Wert</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {
                                    attributes.map(([key, value]) => (
                                        <TableRow key={key}>
                                            <TableCell sx={{overflowWrap: 'anywhere'}}>{key}</TableCell>
                                            <TableCell sx={{overflowWrap: 'anywhere'}}>{value}</TableCell>
                                        </TableRow>
                                    ))
                                }
                            </TableBody>
                        </Table>
                    </TableContainer>
            }
        </Box>
    );
}

interface CommunicationProviderDetailsProps {
    bindingId: number | null;
    binding: CommunicationProviderBinding | null;
    provider: CommunicationProvider | null;
    isLoading: boolean;
    hasPermission: boolean;
}

function CommunicationProviderDetails(props: CommunicationProviderDetailsProps): React.JSX.Element {
    if (props.bindingId == null) {
        return <UnavailableText>Kein Kommunikationsanbieter ausgewählt</UnavailableText>;
    }

    return (
        <Box component="dl" sx={{m: 0}}>
            <DetailRow label="Anbindung">
                <ResolvedName
                    name={props.binding?.name ?? null}
                    isLoading={props.isLoading}
                    hasPermission={props.hasPermission}
                />
            </DetailRow>
            <DetailRow label="Anbieter">
                {
                    props.binding == null && !props.isLoading ?
                        'Über die Anbindung nicht auflösbar' :
                        <ResolvedName
                            name={props.provider?.name ?? null}
                            isLoading={props.isLoading}
                            hasPermission={props.hasPermission}
                        />
                }
            </DetailRow>
        </Box>
    );
}

interface DetailRowProps {
    label: string;
    children: ReactNode;
}

function DetailRow(props: DetailRowProps): React.JSX.Element {
    return (
        <Box
            sx={{
                display: 'grid',
                gridTemplateColumns: {xs: '1fr', sm: 'minmax(140px, auto) minmax(0, 1fr)'},
                columnGap: 1,
                py: 0.125,
            }}
        >
            <Typography component="dt" sx={{color: 'text.secondary', fontSize: '0.8125rem'}}>
                {props.label}
            </Typography>
            <Box
                component="dd"
                sx={{
                    m: 0,
                    minWidth: 0,
                    fontSize: '0.875rem',
                    overflowWrap: 'anywhere',
                }}
            >
                {props.children}
            </Box>
        </Box>
    );
}

interface ResolvedNameProps {
    name: string | null;
    isLoading: boolean;
    hasPermission: boolean;
}

function ResolvedName(props: ResolvedNameProps): React.JSX.Element {
    if (props.name != null) {
        return <>{props.name}</>;
    }

    if (props.isLoading) {
        return <Skeleton width={180}/>;
    }

    return (
        <Typography component="span" sx={{color: 'text.secondary', fontSize: '0.8125rem', fontStyle: 'italic'}}>
            {
                props.hasPermission ?
                    'Name nicht verfügbar' :
                    'Name mangels Berechtigung nicht verfügbar'
            }
        </Typography>
    );
}

function UnavailableText(props: {children: ReactNode}): React.JSX.Element {
    return (
        <Typography sx={{color: 'text.secondary', fontSize: '0.8125rem', fontStyle: 'italic'}}>
            {props.children}
        </Typography>
    );
}
