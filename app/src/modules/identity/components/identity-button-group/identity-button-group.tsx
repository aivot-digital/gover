import React, {useEffect, useMemo, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {addError, hydrateCustomerInput, prefillElementsFromIdentityProvider, selectCustomerInputError, selectCustomerInputValue, selectLoadedForm, setHasLoadedSavedCustomerInput} from '../../../../slices/app-slice';
import {useAppSelector} from '../../../../hooks/use-app-selector';
import {IdentityCustomerInputKey} from '../../constants/identity-customer-input-key';
import {IdentityProviderLink} from '../../models/identity-provider-link';
import {IdentityProviderInfo} from '../../models/identity-provider-info';
import {FormsApiService} from '../../../forms/forms-api-service';
import {IdentityButton} from '../identity-button/identity-button';
import {useSearchParams} from 'react-router-dom';
import {IdentityStateQueryParam} from '../../constants/identity-state-query-param';
import {IdentityResultState} from '../../enums/identity-result-state';
import {IdentityValue} from '../../models/identity-value';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {CustomerInputService} from '../../../../services/customer-input-service';
import {IdentityIdQueryParam} from '../../constants/identity-id-query-param';
import {selectIdentityId, setIdentityId} from '../../../../slices/identity-slice';
import {DialogTitleWithClose} from '../../../../components/dialog-title-with-close/dialog-title-with-close';

interface IdentityButtonGroupProps {
    isBusy: boolean;
}

interface CombinedIdentityProviderLink {
    link: IdentityProviderLink;
    provider: IdentityProviderInfo;
}

export function IdentityButtonGroup(props: IdentityButtonGroupProps) {
    const dispatch = useAppDispatch();

    const [searchParams, setSearchParams] = useSearchParams();
    const form = useAppSelector(selectLoadedForm);
    const identityId = useAppSelector(selectIdentityId);

    const value: IdentityValue | undefined | null = useAppSelector(selectCustomerInputValue(IdentityCustomerInputKey));
    const error = useAppSelector(selectCustomerInputError(IdentityCustomerInputKey));

    const [identityLinks, setIdentityLinks] = useState<CombinedIdentityProviderLink[]>();
    const [showSuccessDialog, setShowSuccessDialog] = useState(false);

    useEffect(() => {
        if (form == null) {
            return;
        }

        FormsApiService
            .getIdentityProviders(form.id)
            .then((page) => {
                const identityLinks: CombinedIdentityProviderLink[] = [];

                for (const identityProvider of page.content) {
                    const link = form.identityProviders.find((idpl) => idpl.identityProviderKey === identityProvider.key);

                    if (link == null) {
                        continue;
                    }

                    identityLinks.push({
                        link: link,
                        provider: identityProvider,
                    });
                }

                setIdentityLinks(identityLinks);
            })
            .catch((err) => {
                console.error('Error fetching identity providers:', err);
            });
    }, [form]);

    useEffect(() => {
        const stateStr = searchParams.get(IdentityStateQueryParam);
        const state = stateStr != null ? parseInt(stateStr) : undefined;
        const id = searchParams.get(IdentityIdQueryParam);

        if (state == null) {
            return;
        }

        if (form == null) {
            return;
        }

        if (id == null) {
            return;
        }

        if (state === IdentityResultState.Success) {
            dispatch(setIdentityId(id));
            setShowSuccessDialog(true);
        } else {
            dispatch(addError({
                key: IdentityCustomerInputKey,
                error: 'Bei der Authentifizierung ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.',
            }));
            dispatch(showErrorSnackbar('Bei der Authentifizierung ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.'));
        }
    }, [searchParams, form]);

    useEffect(() => {
        if (identityId == null) {
            return;
        }

        if (form == null) {
            return;
        }

        IdentityProvidersApiService
            .fetchIdentity(identityId)
            .then(({providerKey, metadataIdentifier, attributes}) => {
                const lastSaveData = CustomerInputService
                    .loadCustomerInputState(form);

                if (lastSaveData != null) {
                    dispatch(hydrateCustomerInput(lastSaveData));
                    dispatch(setHasLoadedSavedCustomerInput(true));
                }

                dispatch(prefillElementsFromIdentityProvider({
                    identityProviderKey: providerKey,
                    metadataIdentifier: metadataIdentifier,
                    userInfo: attributes,
                }));

                setSearchParams({});
            })
            .catch((err) => {
                console.error(err);
                dispatch(addError({
                    key: IdentityCustomerInputKey,
                    error: 'Beim Abruf der Authentifizierungsdaten ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.',
                }));
            });
    }, [identityId, form]);

    const successIdp = useMemo(() => {
        return (identityLinks ?? []).find(idp => idp.link.identityProviderKey === value?.identityProviderKey);
    }, [identityLinks, value]);

    if (form == null || identityLinks == null || identityLinks.length === 0) {
        return null;
    }

    return (
        <>
            <Box
                id={IdentityCustomerInputKey}
                sx={{
                    my: 6,
                }}
            >
                {
                    form.identityRequired ?
                        <>
                            <Typography
                                component="h4"
                                variant="h6"
                                color="primary"
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
                                variant="h6"
                                color="primary"
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
                                isBusy={props.isBusy}
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

            <Dialog
                onClose={() => setShowSuccessDialog(false)}
                open={showSuccessDialog}
                maxWidth="xs"
            >
                <DialogTitleWithClose
                    onClose={() => setShowSuccessDialog(false)}
                    closeTooltip="Schließen"
                >
                    Authentifizierung erfolgreich
                </DialogTitleWithClose>
                <DialogContent className="content-without-margin-on-childs">
                    Sie haben sich erfolgreich mit dem Nutzerkonto <strong>„{successIdp?.provider.name}“</strong> angemeldet.
                    Die Daten aus Ihrem Konto wurden automatisch in den Antrag übernommen.
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={() => setShowSuccessDialog(false)}
                        variant="contained"
                    >
                        Mit Antrag fortfahren
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}