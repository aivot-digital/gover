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
import {Form, Form as Application} from '../../models/entities/form';
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
import {BayernIdAccessLevel, BayernIdAccessLevelOptions} from '../../data/bayern-id-access-level';
import {BundIdAccessLevel, BundIdAccessLevelOptions} from '../../data/bund-id-access-level';
import {ShIdAccessLevel, ShIdAccessLevelOptions} from '../../data/sh-id-access-level';
import {IdentityProviderLink} from '../../modules/identity/models/identity-provider-link';

export function RootComponentEditorTabSchnittstellen(props: BaseEditorProps<RootElement, Application>): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const user = useAppSelector(selectUser);
    const userIsAdmin = useMemo(() => isAdmin(user), [user]);

    const [destinations, setDestinations] = useState<Destination[]>();
    const [identityProviders, setIdentityProviders] = useState<IdentityProviderListDTO[]>();

    useEffect(() => {
        new DestinationsApiService(api)
            .listAllOrdered('name', 'ASC')
            .then(dests => setDestinations(dests.content))
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Liste der Schnittstellen konnte nicht geladen werden.'));
            });

        new IdentityProvidersApiService(api)
            .listAllOrdered('name', 'ASC')
            .then(providers => setIdentityProviders(providers.content))
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Liste der Identitätsanbieter konnte nicht geladen werden.'));
            });
    }, [api]);

    return (
        <>
            <Box
                sx={{
                    mb: 4,
                }}
            >
                <Typography
                    variant="h6"
                >
                    Antragsschnittstelle
                </Typography>

                {
                    destinations == null &&
                    <Skeleton />
                }

                {
                    destinations != null &&
                    destinations.length === 0 &&
                    <Alert
                        severity="info"
                        sx={{
                            my: 2,
                        }}
                    >
                        <AlertTitle>
                            Keine Schnittstellen verfügbar
                        </AlertTitle>

                        {
                            userIsAdmin &&
                            <Typography variant="body1">
                                Es sind keine Schnittstellen verfügbar.
                                Sie können neue Schnittstellen im Bereich <Link to="/destinations">Schnittstellen</Link> anlegen.
                            </Typography>
                        }

                        {
                            !userIsAdmin &&
                            <Typography variant="body1">
                                Es sind keine Schnittstellen verfügbar.
                                Bitte wenden Sie sich an die globale Administrator:in, um eine Schnittstelle zu konfigurieren.
                            </Typography>
                        }

                        <Typography variant="body1">
                            Sie haben aktuell keine Schnittstelle ausgewählt. Bitte beachten Sie, dass in diesem Fall die
                            Anträge ausschließlich in Gover eingehen. Die Mitarbeiter:innen des bewirtschaftenden oder
                            zuständigen Fachbereichs werden per E-Mail über eingegangene Anträge informiert.
                        </Typography>
                    </Alert>
                }

                {
                    destinations != null &&
                    destinations.length > 0 &&
                    <SelectFieldComponent
                        label="Auswahl der Schnittstelle"
                        hint="Wählen Sie hier die Schnittstelle aus, an die die Anträge von Gover übermittelt werden sollen."
                        value={props.entity.destinationId?.toString() ?? undefined}
                        onChange={(val) => {
                            props.onPatchEntity({
                                destinationId: val != null ? parseInt(val) : undefined,
                            });
                        }}
                        options={destinations.map((destination) => ({
                            value: destination.id.toString(),
                            label: destination.name,
                        }))}
                        disabled={!props.editable}
                    />
                }

                {
                    destinations != null &&
                    destinations.length > 0 &&
                    props.entity.destinationId == null &&
                    <AlertComponent
                        title="Keine Schnittstelle ausgewählt"
                        color="info"
                        sx={{
                            mt: 1,
                        }}
                    >
                        Sie haben aktuell keine Schnittstelle ausgewählt. Bitte beachten Sie, dass in diesem Fall die
                        Anträge ausschließlich in Gover eingehen. Die Mitarbeiter:innen des bewirtschaftenden oder
                        zuständigen Fachbereichs werden per E-Mail über eingegangene Anträge informiert.
                    </AlertComponent>
                }

                {
                    destinations != null &&
                    destinations.length > 0 &&
                    props.entity.destinationId != null &&
                    destinations.find((dest) => dest.id === props.entity.destinationId)?.type === DestinationType.Mail &&
                    <AlertComponent
                        title="Hinweis zur E-Mail Schnittstelle"
                        color="warning"
                        sx={{
                            mt: 1,
                        }}
                    >
                        <Typography>
                            Auch wenn der Transportweg der E-Mail verschlüsselt ist, sind die Inhalte der E-Mail mitsamt
                            eventueller Anhänge <b>nicht verschlüsselt</b>. Bitte prüfen Sie, in Abwägung mit den von Ihnen im
                            vorliegenden Fall verarbeiteten Datenkategorien, ob die E-Mail Schnittstelle für die Übertragung der
                            Daten geeignet ist.
                        </Typography>
                        <Typography sx={{mt: 1}}>
                            Die Gover-Plattform kann lediglich die <b>erfolgreiche Übergabe</b> der E-Mail an den hinterlegten SMTP-Server bestätigen, nicht die Zustellung selbst.
                            Eine spätere Nichtzustellung (z. B. durch Bounces oder Spam-Filter) kann nicht automatisiert vom System erkannt werden.
                        </Typography>
                        <Typography sx={{mt: 1}}>
                            Um Zustellprobleme zu vermeiden, empfehlen wir, die für Gover konfigurierte E-Mail-Absenderadresse in die <b>Whitelist</b> Ihrer
                            E-Mail-Konfiguration aufzunehmen. Falls Sie keine E-Mails erhalten, überprüfen Sie bitte auch Ihren Spam-Ordner oder kontaktieren Sie Ihre
                            E-Mail-Administrator:in.
                        </Typography>
                    </AlertComponent>
                }
            </Box>

            <Box>
                <Typography
                    variant="h6"
                >
                    Nutzerkontenschnittstellen
                </Typography>

                {
                    identityProviders == null &&
                    <Skeleton />
                }

                {
                    identityProviders != null &&
                    identityProviders.length === 0 &&
                    <Alert>
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
                    identityProviders != null &&
                    identityProviders.length > 0 &&
                    <Box>
                        <CheckboxFieldComponent
                            variant="switch"
                            label="Authentifizierung erforderlich"
                            hint="Aktivieren Sie diese Option, wenn Sie eine Authentifizierung der antragstellenden Person über einen der bereitgestellten Nutzerkontenanbieter benötigen."
                            value={props.entity.identityRequired}
                            onChange={(checked) => {
                                props.onPatchEntity({
                                    identityRequired: checked,
                                });
                            }}
                        />

                        {
                            props.entity.identityRequired &&
                            props.entity.identityProviders.length === 0 &&
                            <AlertComponent
                                color="warning"
                                title="Keine Nutzerkontenanbieter aktiviert"
                                text="Bitte aktivieren Sie mindestens einen Nutzerkontenanbieter, um die Authentifizierung zu ermöglichen."
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
                                        form={props.entity}
                                        onFormPatch={props.onPatchEntity}
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
    form: Form;
    onFormPatch: (form: Partial<Form>) => void;
}

function IdentityProviderItem(props: IdentityProviderItemProps) {
    const {
        provider,
        form,
    } = props;

    const link: IdentityProviderLink | undefined = useMemo(() => {
        return form.identityProviders.find((idp) => idp.identityProviderKey === provider.key);
    }, [form, provider]);

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

            props.onFormPatch({
                identityProviders: [
                    ...form.identityProviders,
                    newIdp,
                ],
            });
        } else {
            props.onFormPatch({
                identityProviders: form.identityProviders
                    .filter((idp) => idp.identityProviderKey !== provider.key),
            });
        }
    };

    const handleScopeChange = (val: string | undefined) => {
        props.onFormPatch({
            identityProviders: form.identityProviders.map((idp) => {
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
                <Typography>
                    {provider.name}
                </Typography>

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
