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
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        Konfigurieren Sie hier Nutzerkontenanbieter, die in Ihrer Gover-Instanz global verfügbar sein sollen.
                                        Die erforderlichen Konfigurationsdaten erhalten Sie vom Nutzerkontenanbieter oder finden Sie in dessen Dokumentation.
                                    </Typography>
                                    <Typography
                                        variant="body1"
                                        paragraph
                                    >
                                        Es wird empfohlen, für jeden Nutzerkontenanbieter sowohl eine produktive als auch eine vorproduktive Anbindung einzurichten, um Tests zu erleichtern.
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