import React, {useEffect, useMemo, useState} from 'react';
import {Box, FormControlLabel, Switch, Typography} from '@mui/material';
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
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectBooleanSystemConfigValue, setSystemConfigs} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {BundIdAccessLevel, BundIdAccessLevelOptions} from '../../data/bund-id-access-level';
import {BayernIdAccessLevel, BayernIdAccessLevelOptions} from '../../data/bayern-id-access-level';
import {ShIdAccessLevel, ShIdAccessLevelOptions} from '../../data/sh-id-access-level';
import {MukAccessLevel, MukAccessLevelOptions} from '../../data/muk-access-level';
import {SelectFieldComponentOption} from '../select-field/select-field-component-option';
import {DestinationsApiService} from '../../modules/destination/destinations-api-service';
import {SystemConfigsApiService} from '../../modules/configs/system-configs-api-service';

export function RootComponentEditorTabSchnittstellen(props: BaseEditorProps<RootElement, Application>): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const [destinations, setDestinations] = useState<Destination[]>();

    const bundIdActive = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.nutzerkonten.bundid));
    const bayernIdActive = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.nutzerkonten.bayernId));
    const shIdActive = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.nutzerkonten.schleswigHolsteinId));
    const mukActive = useAppSelector(selectBooleanSystemConfigValue(SystemConfigKeys.nutzerkonten.muk));

    // Fetch the system configs again to make sure the state of available service accounts is up to date
    useEffect(() => {
        new SystemConfigsApiService(api)
            .listAll()
            .then(configs => {
                dispatch(setSystemConfigs(configs.content));
            });
    }, []);

    useEffect(() => {
        new DestinationsApiService(api)
            .list(0, 999, undefined, undefined, {})
            .then(dests => setDestinations(dests.content))
            .catch((err) => {
                console.error(err);
                dispatch(showErrorSnackbar('Die Liste der Schnittstellen konnte nicht geladen werden.'));
            });
    }, []);

    const accountGroups: {
        label: string;
        accounts: {
            label: string;
            active: boolean;
            levelOptions: SelectFieldComponentOption[];
            enabledField: keyof Form & 'bundIdEnabled' | 'bayernIdEnabled' | 'shIdEnabled' | 'mukEnabled';
            levelField: keyof Form & 'bundIdLevel' | 'bayernIdLevel' | 'shIdLevel' | 'mukLevel';
            defaultLevel: string;
            isRequired: (form: Form) => boolean;
        }[];
    }[] = useMemo(() => [
        {
            label: 'Zentrale Nutzerkonten',
            accounts: [
                {
                    label: 'BundID',
                    active: bundIdActive,
                    levelOptions: BundIdAccessLevelOptions,
                    enabledField: 'bundIdEnabled',
                    levelField: 'bundIdLevel',
                    defaultLevel: BundIdAccessLevel.Niedrig,
                    isRequired: (form) => form.bundIdEnabled && form.bundIdLevel !== BundIdAccessLevel.Optional,
                },
                {
                    label: 'BayernID',
                    active: bayernIdActive,
                    levelOptions: BayernIdAccessLevelOptions,
                    enabledField: 'bayernIdEnabled',
                    levelField: 'bayernIdLevel',
                    defaultLevel: BayernIdAccessLevel.Niedrig,
                    isRequired: (form) => form.bayernIdEnabled && form.bayernIdLevel !== BayernIdAccessLevel.Optional,
                },
                {
                    label: 'Servicekonto Schleswig-Holstein',
                    active: shIdActive,
                    levelOptions: ShIdAccessLevelOptions,
                    enabledField: 'shIdEnabled',
                    levelField: 'shIdLevel',
                    defaultLevel: ShIdAccessLevel.Niedrig,
                    isRequired: (form) => form.shIdEnabled && form.shIdLevel !== ShIdAccessLevel.Optional,
                },
            ],
        },
        {
            label: 'Zentrale Unternehmenskonten',
            accounts: [
                {
                    label: 'Mein Unternehmenskonto (MUK)',
                    active: mukActive,
                    levelOptions: MukAccessLevelOptions,
                    enabledField: 'mukEnabled',
                    levelField: 'mukLevel',
                    defaultLevel: MukAccessLevel.Optional,
                    isRequired: (form) => form.mukEnabled && form.mukLevel !== MukAccessLevel.Optional,
                },
            ],
        },
    ], [bundIdActive, bayernIdActive, shIdActive, mukActive]);

    return (
        <>
            <Typography
                variant="h6"
            >
                Schnittstellen
            </Typography>

            {
                destinations != null &&
                <SelectFieldComponent
                    label="Auswahl der Schnittstelle"
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
                props.entity.destinationId == null &&
                <AlertComponent
                    title="Keine Schnittstelle ausgewählt"
                    color="info"
                >
                    Sie haben aktuell keine Schnittstelle ausgewählt. Bitte beachten Sie, dass in diesem Fall die
                    Anträge ausschließlich in Gover eingehen. Die Mitarbeiter:innen des bewirtschaftenden oder
                    zuständigen Fachbereichs werden per E-Mail über eingegangene Anträge informiert.
                </AlertComponent>
            }

            {
                props.entity.destinationId != null &&
                destinations?.find((dest) => dest.id === props.entity.destinationId)?.type === DestinationType.Mail &&
                <AlertComponent
                    title="Hinweis zur E-Mail Schnittstelle"
                    color="warning"
                >
                    <Typography>
                        Auch wenn der Transportweg der E-Mail verschlüsselt ist, sind die Inhalte der E-Mail mitsamt
                        eventueller Anhänge <b>nicht verschlüsselt</b>. Bitte prüfen Sie, in Abwägung mit den von Ihnen im
                        vorliegenden Fall verarbeiteten Datenkategorien, ob die E-Mail Schnittstelle für die Übertragung der
                        Daten geeignet ist.
                    </Typography>
                    <Typography sx={{ mt: 1 }}>
                        Die Gover-Plattform kann lediglich die <b>erfolgreiche Übergabe</b> der E-Mail an den hinterlegten SMTP-Server bestätigen, nicht die Zustellung selbst.
                        Eine spätere Nichtzustellung (z. B. durch Bounces oder Spam-Filter) kann nicht automatisiert vom System erkannt werden.
                    </Typography>
                    <Typography sx={{ mt: 1 }}>
                        Um Zustellprobleme zu vermeiden, empfehlen wir, die für Gover konfigurierte E-Mail-Absenderadresse in die <b>Whitelist</b> Ihrer
                        E-Mail-Konfiguration aufzunehmen. Falls Sie keine E-Mails erhalten, überprüfen Sie bitte auch Ihren Spam-Ordner oder kontaktieren Sie Ihre
                        E-Mail-Administrator:in.
                    </Typography>
                </AlertComponent>
            }

            {
                accountGroups
                    .filter(group => group.accounts.some(acc => acc.active))
                    .map(group => (
                        <Box
                            key={group.label}
                            sx={{
                                mt: 4,
                            }}
                        >
                            <Typography
                                variant="h6"
                            >
                                {group.label}
                            </Typography>

                            {
                                group
                                    .accounts
                                    .filter(account => account.active)
                                    .map(account => (
                                        <Box
                                            key={account.label}
                                            mt={2}
                                        >
                                            <FormControlLabel
                                                label={`${account.label} aktivieren`}
                                                control={
                                                    <Switch
                                                        checked={props.entity[account.enabledField]}
                                                        onChange={(_, checked) => {
                                                            props.onPatchEntity({
                                                                [account.enabledField]: checked,
                                                                [account.levelField]: checked ? account.defaultLevel : undefined,
                                                            });
                                                        }}
                                                        inputProps={{'aria-label': 'controlled'}}
                                                        disabled={!props.editable}
                                                    />
                                                }
                                            />

                                            {
                                                props.entity[account.enabledField] &&
                                                <SelectFieldComponent
                                                    label={`${account.label} Mindest-Vertrauensniveau`}
                                                    value={props.entity[account.levelField]}
                                                    onChange={(val) => {
                                                        props.onPatchEntity({
                                                            [account.levelField]: (val ?? account.defaultLevel),
                                                        });
                                                    }}
                                                    required={props.entity[account.enabledField]}
                                                    options={account.levelOptions}
                                                    disabled={!props.editable}
                                                />
                                            }
                                        </Box>
                                    ))
                            }
                        </Box>
                    ))
            }

            {
                accountGroups
                    .flatMap(account => account.accounts)
                    .filter(account => account.active && props.entity[account.enabledField]).length >= 2 &&
                accountGroups
                    .flatMap(account => account.accounts)
                    .some(account => account.active && props.entity[account.enabledField] && account.isRequired(props.entity)) &&
                <AlertComponent
                    title="Verpflichtende Nutzung"
                    color="warning"
                >
                    Sie bieten für antragstellende Personen mehrere Konten an, von denen mindestens ein Konto verpflichtend ist.
                    Das Formular wird somit die Nutzung mindestens eines der angebotenen Konten erzwingen,
                    auch wenn nicht alle von Ihnen angebotenen Konten als verpflichtend gefordert sind.
                </AlertComponent>
            }
        </>
    );
}
