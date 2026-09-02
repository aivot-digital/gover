import {PageWrapper} from '../../../../components/page-wrapper/page-wrapper';
import {Typography} from '@mui/material';
import {GenericDetailsPage} from '../../../../components/generic-details-page/generic-details-page';
import BadgeOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Badge';
import {IdentityProviderDetailsDTO} from '../../models/identity-provider-details-dto';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {IdentityProviderType} from '../../enums/identity-provider-type';
import {ServerEntityType} from '../../../../shells/staff/data/server-entity-type';
import {Permission} from '../../../../data/permissions/permission';

export function IdentityProviderDetailsPage() {
    return (
        <>
            <PageWrapper
                title="Nutzerkontenanbieter bearbeiten"
                fullWidth
                background
            >
                <GenericDetailsPage<IdentityProviderDetailsDTO, string, void>
                    permissionCheck={{
                        create: Permission.IDENTITY_PROVIDER_CREATE,
                        read: Permission.IDENTITY_PROVIDER_READ,
                        update: Permission.IDENTITY_PROVIDER_UPDATE,
                        scope: {
                            type: 'system',
                        },
                    }}
                    header={{
                        icon: <BadgeOutlinedIcon />,
                        title: 'Nutzerkontenanbieter bearbeiten',
                        helpDialog: {
                            title: 'Hilfe zu Nutzerkontenanbietern',
                            tooltip: 'Hilfe anzeigen',
                            content: (
                                <>
                                    <Typography
                                        variant="body1"
                                        sx={{
                                            marginBottom: "16px"
                                        }}
                                    >
                                        Konfigurieren Sie hier die Nutzerkontenanbieter, die in Ihrer Prosuna-Instanz global verfügbar sein sollen.
                                        Die angebundenen Nutzerkonten können in Formularen als Authentifizierungsoptionen verwendet werden.
                                        Unterstützt werden alle Anbieter, die eine OpenID Connect (OIDC) kompatible Schnittstelle bereitstellen.
                                    </Typography>
                                    <Typography
                                        variant="body1"
                                        sx={{
                                            marginBottom: "16px"
                                        }}
                                    >
                                        <strong>Mögliche Szenarien:</strong>
                                    </Typography>
                                    <ul>
                                        <li>
                                            <Typography
                                                variant="body1"
                                                sx={{
                                                    marginBottom: "16px"
                                                }}
                                            >
                                                <strong>Direkt OpenID Connect kompatible IdPs</strong>
                                                (z.B. BundID, BayernID, Mein Unternehmenskonto, Servicekonto SH, Keycloak, Azure AD):
                                                <br />
                                                → Sie können den Anbieter direkt anbinden, indem Sie die Verbindungsdaten hier hinterlegen.
                                            </Typography>
                                        </li>
                                        <li>
                                            <Typography
                                                variant="body1"
                                                sx={{
                                                    marginBottom: "16px"
                                                }}
                                            >
                                                <strong>Systeme ohne OpenID Connect Unterstützung</strong>
                                                (z.B. LDAP/AD, andere IdPs):
                                                <br />
                                                → Die Anbindung erfolgt über den integrierten Keycloak von Prosuna. Tragen Sie anschließend die OpenID Connect-Daten des Keycloak-Realms hier ein.
                                            </Typography>
                                        </li>
                                        <li>
                                            <Typography
                                                variant="body1"
                                                sx={{
                                                    marginBottom: "16px"
                                                }}
                                            >
                                                <strong>LDAP/AD für Prosuna-Mitarbeitende:</strong>
                                                <br />
                                                → Nutzung der User Federation im Staff Realm des Prosuna-Keycloaks.
                                                <br />
                                                Diese Nutzerkonten werden nicht über die Funktion "Nutzerkontenanbieter" verwaltet.
                                            </Typography>
                                        </li>
                                    </ul>
                                    <Typography
                                        variant="body1"
                                        sx={{
                                            marginBottom: "16px"
                                        }}
                                    >
                                        Es wird empfohlen, für jeden Nutzerkontenanbieter sowohl eine produktive als auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
                                    </Typography>
                                    <Typography
                                        variant="body1"
                                        sx={{
                                            marginBottom: "16px"
                                        }}
                                    >
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
                                requiredPermission: Permission.IDENTITY_PROVIDER_UPDATE,
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
                    initializeItem={(api) => new IdentityProvidersApiService().initialize()}
                    fetchData={(api, id: string) => new IdentityProvidersApiService().retrieve(id)}
                    getTabTitle={(item: IdentityProviderDetailsDTO) => {
                        if (item.key === '') {
                            return 'Neuer Nutzerkontenanbieter';
                        } else {
                            return item.name;
                        }
                    }}
                    getHeaderTitle={(item, isNewItem, notFound) => {
                        if (notFound) {
                            return 'Nutzerkontenanbieter nicht gefunden';
                        }
                        if (isNewItem) {
                            return 'Neuen Nutzerkontenanbieter anlegen';
                        }
                        return `Nutzerkontenanbieter: ${item?.name ?? 'Unbenannt'}`;
                    }}
                    parentLink={{
                        label: 'Liste der Nutzerkontenanbieter',
                        to: '/identity-providers',
                    }}
                    idParam="key"
                    entityType={ServerEntityType.IdentityProviders}
                />
            </PageWrapper>
        </>
    );
}
