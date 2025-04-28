import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import BadgeOutlinedIcon from '@mui/icons-material/BadgeOutlined';
import {useAdminGuard} from '../../../../hooks/use-admin-guard';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {IdentityProviderType} from "../../enums/identity-provider-type";

export function IdentityProviderDetailsPage() {
    useAdminGuard();

    return (
        <>
            <PageWrapper
                title="Nutzerkontenanbieter bearbeiten"
                fullWidth
                background
            >
                <GenericDetailsPage<IdentityProviderDetailsDTO, string, void>
                    header={{
                        icon: <BadgeOutlinedIcon />,
                        title: 'Nutzerkontenanbieter bearbeiten',
                        helpDialog: {
                            title: 'Hilfe zu Nutzerkontenanbietern',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography variant="body1" paragraph>
                                        Konfigurieren Sie hier die Nutzerkontenanbieter, die in Ihrer Gover-Instanz global verfügbar sein sollen.
                                        Die angebundenen Nuterkonten können in Formularen als Authentifizierungsoptionen verwendet werden.
                                        Unterstützt werden alle Anbieter, die eine OpenID Connect (OIDC) kompatible Schnittstelle bereitstellen.
                                    </Typography>
                                    <Typography variant="body1" paragraph>
                                        <strong>Mögliche Szenarien:</strong>
                                    </Typography>
                                    <ul>
                                        <li>
                                            <Typography variant="body1" paragraph>
                                                <strong>Direkt OpenID Connect kompatible IDPs</strong> (z.B. BundID, BayernID, Mein Unternehmenskonto, Servicekonto SH, Keycloak, Azure AD):<br />
                                                → Sie können den Anbieter direkt anbinden, indem Sie die Verbindungsdaten hier hinterlegen.
                                            </Typography>
                                        </li>
                                        <li>
                                            <Typography variant="body1" paragraph>
                                                <strong>Systeme ohne OpenID Connect Unterstützung</strong> (z.B. LDAP/AD, andere IDPs):<br />
                                                → Die Anbindung erfolgt über den integrierten Keycloak von Gover. Tragen Sie anschließend die OpenID Connect-Daten des Keycloak-Realms hier ein.
                                            </Typography>
                                        </li>
                                        <li>
                                            <Typography variant="body1" paragraph>
                                                <strong>LDAP/AD für Gover-Mitarbeitende:</strong><br />
                                                → Nutzung der User Federation im Staff Realm des Gover-Keycloaks.<br />
                                                Diese Nutzerkonten werden nicht über die Funktion "Nutzerkontenanbieter" verwaltet.
                                            </Typography>
                                        </li>
                                    </ul>
                                    <Typography variant="body1" paragraph>
                                        Es wird empfohlen, für jeden Nutzerkontenanbieter sowohl eine produktive als auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
                                    </Typography>
                                    <Typography variant="body1" paragraph>
                                        Die notwendigen Konfigurationsdaten erhalten Sie in der Dokumentation des Anbieters oder direkt vom Anbieter selbst.
                                    </Typography>
                                </>
                            ),
                        },
                    }}
                    tabs={(item: IdentityProviderDetailsDTO | undefined) => {
                        const tabs = [
                            {
                                path: '/identity-providers/:key',
                                label: 'Konfiguration',
                            },
                            {
                                path: '/identity-providers/:key/test',
                                label: 'Testen',
                                isDisabled: (item: IdentityProviderDetailsDTO | undefined) => item?.key === '',
                            },
                            {
                                path: '/identity-providers/:key/forms',
                                label: 'Formulare',
                                isDisabled: (item: IdentityProviderDetailsDTO | undefined) => item?.key === '',
                            },
                        ];

                        if (!item || item.key === '') {
                            return tabs;
                        }

                        if (item.type !== IdentityProviderType.Custom) {
                            tabs.push({
                                path: '/identity-providers/:key/setup',
                                label: 'Einrichtung',
                            });
                        }

                        return tabs;
                    }}
                    initializeItem={(api) => new IdentityProvidersApiService(api).initialize()}
                    fetchData={(api, id: string) => new IdentityProvidersApiService(api).retrieve(id)}
                    getTabTitle={(item: IdentityProviderDetailsDTO) => {
                        if (item.key === '') {
                            return 'Neuer Nutzerkontenanbieter';
                        } else {
                            return item.name;
                        }
                    }}
                    getHeaderTitle={(item, isNewItem, notFound) => {
                        if (notFound) return 'Nutzerkontenanbieter nicht gefunden';
                        if (isNewItem) return 'Neuen Nutzerkontenanbieter anlegen';
                        return `Nutzerkontenanbieter: ${item?.name ?? 'Unbenannt'}`;
                    }}
                    parentLink={{
                        label: 'Liste der Nutzerkontenanbieter',
                        to: '/identity-providers',
                    }}
                    idParam="key"
                />
            </PageWrapper>
        </>
    );
}