import React, {useEffect, useState} from 'react';
import {Box, Typography} from '@mui/material';
import {IdentityCustomerInputKey} from '../../constants/identity-customer-input-key';
import {IdentityProviderLink} from '../../models/identity-provider-link';
import {IdentityProviderInfo} from '../../models/identity-provider-info';
import {FormsApiService} from '../../../forms/forms-api-service';
import {IdentityButton} from '../identity-button/identity-button';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {IdentityData} from '../../models/identity-data';
import {ElementData} from '../../../../models/element-data';
import {AnyElement} from '../../../../models/elements/any-element';
import {Api, useApi} from '../../../../hooks/use-api';
import {FormDetailsResponseDTO} from '../../../forms/dtos/form-details-response-dto';

interface IdentityButtonGroupProps {
    rootElement: AnyElement;
    isBusy: boolean;
    isDeriving: boolean;
    elementData: ElementData;
    onElementDataChange: (elementData: ElementData) => void;
    form: FormDetailsResponseDTO;
}

export interface CombinedIdentityProviderLink {
    link: IdentityProviderLink;
    provider: IdentityProviderInfo;
}

export function IdentityButtonGroup(props: IdentityButtonGroupProps) {
    const {
        isBusy,
        elementData,
        form,
    } = props;

    const dispatch = useAppDispatch();
    const api = useApi();

    const value: IdentityData | undefined | null = elementData[IdentityCustomerInputKey]?.inputValue ?? undefined;
    const error: string[] | null | undefined = elementData[IdentityCustomerInputKey]?.computedErrors ?? undefined;

    const [identityLinks, setIdentityLinks] = useState<CombinedIdentityProviderLink[]>();

    useEffect(() => {
        getIdentityProviderLinks(api, form)
            .then(setIdentityLinks)
            .catch((err) => {
                console.error('Error fetching identity providers:', err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Nutzerkontenanbieter.'));
            });
    }, [form]);

    if (form == null || identityLinks == null || identityLinks.length === 0) {
        return null;
    }

    return (
        <Box
            id={IdentityCustomerInputKey}
            sx={{
                my: 6,
            }}
        >
            {
                form.identityVerificationRequired ?
                    <>
                        <Typography
                            component="h4"
                            variant="h5"
                            sx={{
                                mt: 4,
                            }}
                        >
                            Verpflichtende Authentifizierung mit einem Nutzerkonto
                        </Typography>
                        <Typography
                            sx={{
                                mt: 1,
                                mb: 2,
                                maxWidth: '620px',
                            }}
                        >
                            Eine Authentifizierung mittels einem der nachfolgenden Konten ist verpflichtend. Ihre Daten werden im Anschluss automatisch in den Antrag übernommen.
                        </Typography>
                    </> :
                    <>
                        <Typography
                            component="h4"
                            variant="h5"
                            sx={{
                                mt: 4,
                            }}
                        >
                            Optionale Authentifizierung mit einem Nutzerkonto
                        </Typography>
                        <Typography
                            sx={{
                                mt: 1,
                                mb: 2,
                                maxWidth: '600px',
                            }}
                        >
                            Eine Authentifizierung mittels der nachfolgenden Konten ist optional möglich. Ihre Daten werden im Anschluss automatisch in den Antrag übernommen.
                        </Typography>
                    </>
            }

            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    alignItems: 'center',
                    width: '100%',
                }}
            >

                {
                    identityLinks.map(({link, provider}) => (
                        <IdentityButton
                            key={link.identityProviderKey}
                            identityProviderLink={link}
                            identityProviderInfo={provider}
                            isBusy={isBusy}
                            value={value}
                        />
                    ))
                }
            </Box>

            {
                error != null &&
                <Typography
                    variant="caption"
                    color="error"
                    sx={{
                        display: 'block',
                        mt: 2,
                    }}
                >
                    {error}
                </Typography>
            }
        </Box>
    );
}

async function getIdentityProviderLinks(api: Api, form: FormDetailsResponseDTO) {
    const idps = await new FormsApiService(api)
        .getIdentityProviders(form.slug, form.version);

    const identityLinks: CombinedIdentityProviderLink[] = [];

    for (const identityProvider of idps.content) {
        const link = form
            .identityProviders
            .find((idpl) => idpl.identityProviderKey === identityProvider.key);

        if (link == null) {
            continue;
        }

        identityLinks.push({
            link: link,
            provider: identityProvider,
        });
    }

    return identityLinks;
}
