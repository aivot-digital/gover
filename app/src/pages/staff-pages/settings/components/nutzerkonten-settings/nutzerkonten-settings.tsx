import React from 'react';
import {AppConfig} from '../../../../../app-config';
import {SystemConfigKeys} from '../../../../../data/system-config-keys';
import {Nutzerkonto} from './nutzerkonto';
import {Typography} from '@mui/material';
import LinkOutlinedIcon from '@mui/icons-material/LinkOutlined';
import CodeOutlinedIcon from '@mui/icons-material/CodeOutlined';

export function NutzerkontenSettings(): JSX.Element {
    return (
        <>
            <Typography
                variant="subtitle1"
                sx={{
                    mt: 4,
                }}
            >
                Anbindung von zentralen Konten
            </Typography>

            <Typography
                sx={{
                    maxWidth: 900,
                    mb: 1.6,
                }}
            >
                Zentrale Konten bieten Bürger:innen und Unternehmen einen sicheren Zugang zu verschiedenen staatlichen Diensten und Informationen über eine einzige Anmeldung. Hier finden Sie Informationen und Metadaten zur Anbindung für durch Gover unterstützte Konten und können die konfigurierten Konten aktivieren bzw. deaktivieren.
            </Typography>

            <Typography
                variant="subtitle1"
                sx={{
                    mt: 4,
                    mb: 1,
                }}
            >
                Zentrale Nutzerkonten
            </Typography>

            <Nutzerkonto
                configKey={SystemConfigKeys.nutzerkonten.bundid}
                label="BundID"
                links={[
                    {
                        icon: <LinkOutlinedIcon />,
                        label: 'Leitfaden zur Anbindung der BundID',
                        url: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/einrichten/bund-id',
                    },
                    {
                        icon: <LinkOutlinedIcon />,
                        label: 'BundID Portal',
                        url: 'https://id.bund.de',
                    },
                    {
                        icon: <LinkOutlinedIcon />,
                        label: 'Offizielle Dokumentation (BSCW-Server)',
                        url: 'https://bscw.bund.de',
                    },
                    {
                        icon: <CodeOutlinedIcon />,
                        label: 'SAML Metadaten',
                        url: `${AppConfig.bundId.host}/realms/${AppConfig.bundId.realm}/bundid/${AppConfig.bundId.broker}/metadata`,
                    },
                ]}
            >
                Bitte beachten Sie, dass es für die Funktionsfähigkeit der BundID einer technischen Anbindung beim ITZBund bedarf.
                Nützliche Informationen sowie die technischen Anbindungsdaten finden Sie nachstehend.
            </Nutzerkonto>

            <Nutzerkonto
                configKey={SystemConfigKeys.nutzerkonten.bayernId}
                label="BayernID"
                links={[
                    {
                        icon: <LinkOutlinedIcon />,
                        label: 'Leitfaden zur Anbindung der BayernID',
                        url: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/einrichten/bayern-id',
                    },
                    {
                        icon: <LinkOutlinedIcon />,
                        label: 'BayernID Portal',
                        url: 'https://id.bayernportal.de',
                    },
                    {
                        icon: <CodeOutlinedIcon />,
                        label: 'SAML Metadaten',
                        url: `${AppConfig.bayernId.host}/realms/${AppConfig.bayernId.realm}/bayernid/${AppConfig.bayernId.broker}/metadata`,
                    },
                ]}
            >
                Bitte beachten Sie, dass es für die Funktionsfähigkeit der BayernID einer technischen Anbindung bei der AKDB bedarf.
                Nützliche Informationen sowie die technischen Anbindungsdaten finden Sie nachstehend.
            </Nutzerkonto>

            <Nutzerkonto
                configKey={SystemConfigKeys.nutzerkonten.schleswigHolsteinId}
                label="Servicekonto Schleswig-Holstein"
                links={[
                    {
                        icon: <LinkOutlinedIcon />,
                        label: 'Leitfaden zur Anbindung des Servicekontos Schleswig-Holstein',
                        url: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/einrichten/servicekonto-sh',
                    },
                    {
                        icon: <LinkOutlinedIcon />,
                        label: 'Serviceportal Schleswig-Holstein',
                        url: 'https://serviceportal.schleswig-holstein.de',
                    },
                    {
                        icon: <CodeOutlinedIcon />,
                        label: 'SAML Metadaten',
                        url: `${AppConfig.schleswigHolsteinId.host}/realms/${AppConfig.schleswigHolsteinId.realm}/broker/${AppConfig.schleswigHolsteinId.broker}/endpoint/descriptor`,
                    },
                ]}
            >
                Bitte beachten Sie, dass es für die Funktionsfähigkeit des Servicekontos Schleswig-Holstein einer technischen Anbindung bei der Dataport bedarf.
                Nützliche Informationen sowie die technischen Anbindungsdaten finden Sie nachstehend.
            </Nutzerkonto>

            <Typography
                variant="subtitle1"
                sx={{
                    mt: 4,
                    mb: 1,
                }}
            >
                Zentrale Unternehmenskonten
            </Typography>

            <Nutzerkonto
                configKey={SystemConfigKeys.nutzerkonten.muk}
                label="Mein Unternehmenskonto (MUK)"
                links={[
                    {
                        icon: <LinkOutlinedIcon />,
                        label: 'Leitfaden zur Anbindung des Mein Unternehmenskontos',
                        url: 'https://wiki.teamaivot.de/de/dokumentation/gover/benutzerhandbuch/einrichten/mein-unternehmenskonto',
                    },
                    {
                        icon: <LinkOutlinedIcon />,
                        label: 'Offizielle Dokumentation',
                        url: 'https://info.mein-unternehmenskonto.de/downloads/',
                    },
                    {
                        icon: <LinkOutlinedIcon />,
                        label: 'Self Service Portal (SSP)',
                        url: 'https://service.mein-unternehmenskonto.de',
                    },
                    {
                        icon: <CodeOutlinedIcon />,
                        label: 'SAML Metadaten',
                        url: `${AppConfig.muk.host}/realms/${AppConfig.muk.realm}/broker/${AppConfig.muk.broker}/endpoint/descriptor`,
                    },
                ]}
            >
                Bitte beachten Sie, dass es für die Funktionsfähigkeit des Mein Unternehmenskontos einer technischen Anbindung im Self Service Portal (SSP) des Bayerischen Staatsministeriums für Digitales bedarf.
                Nützliche Informationen sowie die technischen Anbindungsdaten finden Sie nachstehend.
            </Nutzerkonto>
        </>
    );
}
