import React, {useEffect, useMemo, useState} from 'react';
import {Alert, AlertTitle, Box, Paper, Skeleton, Typography} from '@mui/material';
import {type BaseEditorProps} from '../../editors/base-editor';
import {type RootElement} from '../../models/elements/root-element';
import {type Destination} from '../../modules/destination/models/destination';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {SelectFieldComponent} from '../select-field/select-field-component';
import {AlertComponent} from '../alert/alert-component';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import {DestinationType} from '../../data/destination-type';
import {useApi} from '../../hooks/use-api';
import {DestinationsApiService} from '../../modules/destination/destinations-api-service';
import {IdentityProviderListDTO} from '../../modules/identity/models/identity-provider-list-dto';
import {IdentityProvidersApiService} from '../../modules/identity/identity-providers-api-service';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectUser} from '../../slices/user-slice';
import {isAdmin} from '../../utils/is-admin';
import {Link} from 'react-router-dom';
import {IdentityProviderType} from '../../modules/identity/enums/identity-provider-type';
import {BayernIdAccessLevel, BayernIdAccessLevelOptions} from '../../modules/identity/enums/bayern-id-access-level';
import {BundIdAccessLevel, BundIdAccessLevelOptions} from '../../modules/identity/enums/bund-id-access-level';
import {ShIdAccessLevel, ShIdAccessLevelOptions} from '../../modules/identity/enums/sh-id-access-level';
import {IdentityProviderLink} from '../../modules/identity/models/identity-provider-link';
import ScienceOutlinedIcon from '@mui/icons-material/ScienceOutlined';
import NotInterestedOutlinedIcon from '@mui/icons-material/NotInterestedOutlined';
import Tooltip from '@mui/material/Tooltip';
import Chip from '@mui/material/Chip';
import {ElementEditorSectionHeader} from '../element-editor-section-header/element-editor-section-header';
import {withDelay} from '../../utils/with-delay';
import {LoadedForm} from '../../slices/app-slice';
import {FormVersionEntity} from '../../modules/forms/entities/form-version-entity';
import {OzgCloudInfo} from '../../modules/destination/components/ozg-cloud-info';

export function RootComponentEditorTabSchnittstellen(props: BaseEditorProps<RootElement, LoadedForm>) {
    const api = useApi();
    const dispatch = useAppDispatch();
    const user = useAppSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);

    const [destinations, setDestinations] = useState<Destination[]>();
    const [identityProviders, setIdentityProviders] = useState<IdentityProviderListDTO[]>();

    useEffect(() => {
        withDelay(new DestinationsApiService(api)
            .listAllOrdered('name', 'ASC'), 600)
            .then(dests => setDestinations(dests.content))
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Liste der Schnittstellen konnte nicht geladen werden.'));
            });

        withDelay(new IdentityProvidersApiService(api)
            .listAllOrdered('name', 'ASC', {
                isEnabled: true,
            }), 600)
            .then(providers => setIdentityProviders(providers.content))
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Liste der Identitätsanbieter konnte nicht geladen werden.'));
            });
    }, [api]);

    if (destinations == null || identityProviders == null) {
        return EditorSkeleton;
    }

    return (
        <>
            <Box>
                <ElementEditorSectionHeader
                    title="Schnittstellen für Nutzerkonten"
                    disableMarginTop
                >
                    Konfigurieren Sie die optionale oder erzwungene Authentifizierung mit Nutzerkonten.
                </ElementEditorSectionHeader>

                {
                    identityProviders.length === 0 &&
                    <Alert severity="info">
                        <AlertTitle>
                            Keine Nutzerkontenanbieter verfügbar
                        </AlertTitle>

                        {
                            userIsAdmin &&
                            <Typography variant="body1">
                                Es sind keine Nutzerkontenanbieter verfügbar.
                                Sie können neue Nutzerkontenanbieter im Bereich <Link to="/identity-providers">Nutzerkontenanbieter</Link> anlegen.
                            </Typography>
                        }

                        {
                            !userIsAdmin &&
                            <Typography variant="body1">
                                Es sind keine Nutzerkontenanbieter verfügbar.
                                Bitte wenden Sie sich an die globale Administrator:in, um einen Nutzerkontenanbieter zu konfigurieren.
                            </Typography>
                        }
                    </Alert>
                }

                {
                    identityProviders.length > 0 &&
                    <Box>
                        <CheckboxFieldComponent
                            variant="switch"
                            label="Authentifizierung erforderlich"
                            hint="Aktivieren Sie diese Option, wenn Sie eine Authentifizierung der antragstellenden Person über einen der bereitgestellten Nutzerkontenanbieter benötigen."
                            value={props.entity.version.identityVerificationRequired}
                            onChange={(checked) => {
                                props.onPatchEntity({
                                    version: {
                                        ...props.entity.version,
                                        identityVerificationRequired: checked,
                                    },
                                });
                            }}
                            disabled={!props.editable}
                        />

                        {
                            props.entity.version.identityVerificationRequired &&
                            props.entity.version.identityProviders.length === 0 &&
                            <AlertComponent
                                color="warning"
                                title="Keine Nutzerkontenanbieter aktiviert"
                                text="Bitte aktivieren Sie mindestens einen Nutzerkontenanbieter, um die Authentifizierung zu ermöglichen. Diese Option wird andernfalls beim Speichern deaktiviert."
                            />
                        }

                        <Box
                            sx={{
                                mt: 2,
                            }}
                        >
                            {
                                identityProviders.map(idp => (
                                    <IdentityProviderItem
                                        key={idp.key}
                                        provider={idp}
                                        version={props.entity.version}
                                        onVersionPatch={(version) => {
                                            props.onPatchEntity({
                                                version: {
                                                    ...props.entity.version,
                                                    ...version,
                                                },
                                            });
                                        }}
                                        disabled={!props.editable}
                                    />
                                ))
                            }
                        </Box>
                    </Box>
                }
            </Box>
        </>
    );
}

interface IdentityProviderItemProps {
    provider: IdentityProviderListDTO;
    version: FormVersionEntity;
    onVersionPatch: (form: Partial<FormVersionEntity>) => void;
    disabled?: boolean;
}

function IdentityProviderItem(props: IdentityProviderItemProps) {
    const {
        provider,
        version,
    } = props;

    const link: IdentityProviderLink | undefined = useMemo(() => {
        return version.identityProviders.find((idp) => idp.identityProviderKey === provider.key);
    }, [version, provider]);

    const isActive = useMemo(() => {
        return link != null;
    }, [link]);

    const handleSwitch = (checked: boolean) => {
        if (checked) {
            const newIdp: IdentityProviderLink = {
                identityProviderKey: provider.key,
                additionalScopes: [],
            };

            switch (provider.type) {
                case IdentityProviderType.BayernID:
                    newIdp.additionalScopes.push(BayernIdAccessLevel.Niedrig);
                    break;
                case IdentityProviderType.BundID:
                    newIdp.additionalScopes.push(BundIdAccessLevel.Niedrig);
                    break;
                case IdentityProviderType.SHID:
                    newIdp.additionalScopes.push(ShIdAccessLevel.Niedrig);
                    break;
            }

            props.onVersionPatch({
                identityProviders: [
                    ...version.identityProviders,
                    newIdp,
                ],
            });
        } else {
            props.onVersionPatch({
                identityProviders: version.identityProviders
                    .filter((idp) => idp.identityProviderKey !== provider.key),
            });
        }
    };

    const handleScopeChange = (val: string | undefined) => {
        props.onVersionPatch({
            identityProviders: version.identityProviders.map((idp) => {
                if (idp.identityProviderKey === provider.key) {
                    return {
                        ...idp,
                        additionalScopes: val != null ? [val] : [],
                    };
                }
                return idp;
            }),
        });
    };

    return (
        <Paper
            variant="outlined"
            sx={{
                p: 2,
                mb: 2,
            }}
        >
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                }}
            >
                <Box>
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        <Typography>
                            {provider.name}
                        </Typography>

                        {
                            provider.isTestProvider &&
                            <Tooltip
                                title="Es handelt sich um einen Test-Nutzerkontenanbieter für z.B. ein Vorproduktionssystem."
                                arrow={true}
                            >
                                <Chip
                                    sx={{ml: 1, mt: -0.25}}
                                    label="Testumgebung"
                                    color="info"
                                    variant="outlined"
                                    size={'small'}
                                    icon={<ScienceOutlinedIcon />}
                                />
                            </Tooltip>
                        }

                        {
                            !provider.isEnabled &&
                            <Tooltip
                                title="Dieser Nutzerkontenanbieter wurde global deaktiviert. Sie können ihn im Formular konfigurieren, Nutzer:innen können sich damit aber nicht authentifizieren."
                                arrow={true}
                            >
                                <Chip
                                    sx={{ml: 1, mt: -0.25}}
                                    label="Global Deaktiviert"
                                    color="warning"
                                    variant="outlined"
                                    size={'small'}
                                    icon={<NotInterestedOutlinedIcon />}
                                />
                            </Tooltip>
                        }
                    </Box>

                    <Typography variant="caption">
                        {provider.description}
                    </Typography>
                </Box>

                <Box
                    sx={{
                        ml: 'auto',
                    }}
                >
                    <CheckboxFieldComponent
                        label="Aktiv"
                        variant="switch"
                        value={isActive}
                        onChange={handleSwitch}
                        sx={{
                            my: 0,
                            mx: 0,
                        }}
                        disabled={props.disabled}
                    />
                </Box>
            </Box>

            {
                isActive &&
                provider.type === IdentityProviderType.BayernID &&
                <SelectFieldComponent
                    label="Vertrauensniveau"
                    value={link?.additionalScopes[0] ?? undefined}
                    onChange={handleScopeChange}
                    options={BayernIdAccessLevelOptions}
                    required={true}
                    disabled={props.disabled}
                />
            }

            {
                isActive &&
                provider.type === IdentityProviderType.BundID &&
                <SelectFieldComponent
                    label="Vertrauensniveau"
                    value={link?.additionalScopes[0] ?? undefined}
                    onChange={handleScopeChange}
                    options={BundIdAccessLevelOptions}
                    required={true}
                />
            }

            {
                isActive &&
                provider.type === IdentityProviderType.SHID &&
                <SelectFieldComponent
                    label="Vertrauensniveau"
                    value={link?.additionalScopes[0] ?? undefined}
                    onChange={handleScopeChange}
                    options={ShIdAccessLevelOptions}
                    required={true}
                />
            }
        </Paper>
    );
}


const EditorSkeleton = (
    <>
        <Skeleton
            width={200}
            height={30}
        />

        <Skeleton
            width={900}
            height={48}
        />

        <Skeleton
            width="100%"
            height={80}
        />

        <Skeleton
            width="100%"
            height={200}
        />

        <Skeleton
            width={200}
            height={30}
        />

        <Skeleton
            width={900}
            height={48}
        />

        <Skeleton
            width="100%"
            height={100}
        />

        <Skeleton
            width="100%"
            height={100}
        />

        <Skeleton
            width="100%"
            height={100}
        />
    </>
);
