import {Box, Button, Card, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography} from '@mui/material';
import React, {useContext} from 'react';
import {
    GenericDetailsPageContext,
    GenericDetailsPageContextType
} from '../../../../components/generic-details-page/generic-details-page-context';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import {AppConfig} from '../../../../app-config';
import CodeOutlinedIcon from "@mui/icons-material/CodeOutlined";
import {SettingsSuggestOutlined} from "@mui/icons-material";
import {SamlMetadataDialog} from "../../../../dialogs/saml-metadata-dialog/saml-metadata-dialog";

interface SetupInfoBlockProps {
    title: string;
    description: string;
    links: {
        label: string;
        url: string;
        icon: React.ReactNode;
    }[];
}

function SetupInfoBlock({ title, description, links }: SetupInfoBlockProps) {
    const [open, setOpen] = React.useState(false);
    const [loading, setLoading] = React.useState(false);
    const [fields, setFields] = React.useState<{ label: string; value: string }[]>([]);
    const [error, setError] = React.useState<string | null>(null);
    const { item: identityProvider } = useContext<GenericDetailsPageContextType<IdentityProviderDetailsDTO, void>>(GenericDetailsPageContext);

    const handleFetchMetadata = async () => {
        if (!identityProvider) return;

        const metadataLink = links.find(l => l.label.includes('SAML Metadaten'))?.url;
        if (!metadataLink) return;

        setLoading(true);
        setError(null);
        try {
            const response = await fetch(metadataLink);
            const text = await response.text();
            const parser = new DOMParser();
            const xml = parser.parseFromString(text, "application/xml");

            const entityID = xml.documentElement.getAttribute('entityID') ?? '';

            const signingCertNode = xml.querySelector('KeyDescriptor[use="signing"] X509Certificate');
            const signingCert = signingCertNode?.textContent?.trim() ?? '';

            const encryptionCertNode = xml.querySelector('KeyDescriptor[use="encryption"] X509Certificate');
            const encryptionCert = encryptionCertNode?.textContent?.trim() ?? '';

            const assertionConsumerServiceNode = xml.querySelector('AssertionConsumerService');
            const assertionConsumerServiceURL = assertionConsumerServiceNode?.getAttribute('Location') ?? '';

            const singleLogoutServiceNode = xml.querySelector('SingleLogoutService');
            const singleLogoutServiceURL = singleLogoutServiceNode?.getAttribute('Location') ?? '';

            if (!entityID || !signingCert || !encryptionCert || !assertionConsumerServiceURL) {
                throw new Error("Nicht alle erforderlichen Metadaten konnten ausgelesen werden.");
            }

            const fieldsToShow: { label: string; value: string }[] = [];

            if (identityProvider.type === IdentityProviderType.BundID) {
                fieldsToShow.push(
                    { label: 'EntityID', value: entityID },
                    { label: 'Primäres X509 Zertifikat (signing)', value: signingCert },
                    { label: 'X509 Zertifikat (encryption)', value: encryptionCert },
                    { label: 'Assertion Consumer Service URL', value: assertionConsumerServiceURL },
                );
            } else if (identityProvider.type === IdentityProviderType.MUK) {
                fieldsToShow.push(
                    { label: 'EntityID', value: entityID },
                    { label: 'Primäres X509 Zertifikat (signing)', value: signingCert },
                    { label: 'X509 Zertifikat (encryption)', value: encryptionCert },
                    { label: 'Assertion Consumer Service URL', value: assertionConsumerServiceURL },
                    { label: 'Single Logout Service URL', value: singleLogoutServiceURL || 'Nicht angegeben' },
                );
            }

            setFields(fieldsToShow);
        } catch (error) {
            console.error('Fehler beim Laden oder Verarbeiten der Metadaten', error);
            setError('Die Metadaten konnten nicht korrekt geladen oder verarbeitet werden.');
        } finally {
            setOpen(true);
            setLoading(false);
        }
    };

    const needsSelectiveMetadata = identityProvider?.type === IdentityProviderType.BundID || identityProvider?.type === IdentityProviderType.MUK;

    return (
        <>
            <Typography variant="h5" sx={{ mt: 1.5, mb: 1 }}>
                {title}
            </Typography>

            <Typography sx={{ mb: 3, maxWidth: 900 }}>
                {description}
            </Typography>

            <Card variant="outlined" sx={{ mb: 4 }}>
                <List>
                    {links.map(link => (
                        <ListItem key={link.label} disablePadding>
                            <ListItemButton component="a" href={link.url} target="_blank">
                                <ListItemIcon>{link.icon}</ListItemIcon>
                                <ListItemText primary={link.label} />
                            </ListItemButton>
                        </ListItem>
                    ))}
                </List>
            </Card>

            {needsSelectiveMetadata && (
                <Box
                    sx={{
                        display: 'flex',
                        marginTop: 2,
                        gap: 2,
                    }}
                >
                    <Button
                        variant="contained"
                        color="primary"
                        onClick={handleFetchMetadata}
                        startIcon={<SettingsSuggestOutlined />}
                    >
                        Metadaten für Einrichtung auslesen
                    </Button>
                </Box>
                )
            }

            <SamlMetadataDialog
                open={open}
                loading={loading}
                fields={fields}
                error={error}
                onClose={() => setOpen(false)}
            />
        </>
    );
}


export function IdentityProviderDetailsPageSetup() {
    const {
        item: identityProvider,
    } = useContext<GenericDetailsPageContextType<IdentityProviderDetailsDTO, void>>(GenericDetailsPageContext);

    return (
        <Box>
            {identityProvider?.type === IdentityProviderType.BundID && (
                <SetupInfoBlock
                    title="Einrichtung der BundID"
                    description="Bitte beachten Sie, dass es für die Funktionsfähigkeit der BundID einer technischen Anbindung im Self Service Portal (SSP)
                    des Bundesministerium des Innern und für Heimat bedarf. Nützliche Informationen sowie die technischen Anbindungsdaten finden Sie nachstehend."
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
                            label: 'Self Service Portal (SSP)',
                            url: 'https://ssp.id.bund.de/',
                        },
                        {
                            icon: <CodeOutlinedIcon />,
                            label: 'SAML Metadaten',
                            url: `${AppConfig.bundId.host}/realms/${AppConfig.bundId.realm}/broker/${AppConfig.bundId.broker}/endpoint/descriptor`,
                        },
                    ]}
                />
            )}

            {identityProvider?.type === IdentityProviderType.BayernID && (
                <SetupInfoBlock
                    title="Einrichtung der BayernID"
                    description="Bitte beachten Sie, dass es für die Funktionsfähigkeit der BayernID einer technischen Anbindung bei der AKDB bedarf.
                    Nützliche Informationen sowie die technischen Anbindungsdaten finden Sie nachstehend."
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
                />
            )}

            {identityProvider?.type === IdentityProviderType.SHID && (
                <SetupInfoBlock
                    title="Einrichtung des Servicekonto Schleswig-Holstein"
                    description="Bitte beachten Sie, dass es für die Funktionsfähigkeit des Servicekontos Schleswig-Holstein einer technischen Anbindung bei der Dataport bedarf.
                    Nützliche Informationen sowie die technischen Anbindungsdaten finden Sie nachstehend."
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
                />
            )}

            {identityProvider?.type === IdentityProviderType.MUK && (
                <SetupInfoBlock
                    title="Einrichtung des Mein Unternehmenskonto (MUK)"
                    description="Bitte beachten Sie, dass es für die Funktionsfähigkeit des Mein Unternehmenskontos einer technischen Anbindung im Self Service Portal (SSP) des Bayerischen Staatsministeriums für Digitales bedarf.
                    Nützliche Informationen sowie die technischen Anbindungsdaten finden Sie nachstehend."
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
                />
            )}
        </Box>
    );
}