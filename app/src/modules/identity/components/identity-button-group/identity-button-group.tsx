import React, {useEffect, useMemo, useState} from 'react';
import {Box, Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {IdentityCustomerInputKey} from '../../constants/identity-customer-input-key';
import {IdentityProviderLink} from '../../models/identity-provider-link';
import {IdentityProviderInfo} from '../../models/identity-provider-info';
import {FormsApiService} from '../../../forms/forms-api-service';
import {IdentityButton} from '../identity-button/identity-button';
import {useSearchParams} from 'react-router-dom';
import {IdentityStateQueryParam} from '../../constants/identity-state-query-param';
import {IdentityResultState} from '../../enums/identity-result-state';
import {IdentityProvidersApiService} from '../../identity-providers-api-service';
import {useAppDispatch} from '../../../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../../../slices/snackbar-slice';
import {IdentityIdQueryParam} from '../../constants/identity-id-query-param';
import {DialogTitleWithClose} from '../../../../components/dialog-title-with-close/dialog-title-with-close';
import {IdentityData} from '../../models/identity-data';
import {ElementData} from '../../../../models/element-data';
import {CustomerInputService} from '../../../../services/customer-input-service';
import {prefillIdentityData} from '../../../../utils/prefill-elements';
import {ElementType} from '../../../../data/element-type/element-type';
import {AnyElement} from '../../../../models/elements/any-element';
import {FormPublicProjection} from '../../../../models/entities/form';

interface IdentityButtonGroupProps {
    rootElement: AnyElement;
    isBusy: boolean;
    elementData: ElementData;
    onElementDataChange: (elementData: ElementData) => void;
    form: FormPublicProjection;
}

interface CombinedIdentityProviderLink {
    link: IdentityProviderLink;
    provider: IdentityProviderInfo;
}

export function IdentityButtonGroup(props: IdentityButtonGroupProps) {
    const {
        rootElement,
        isBusy,
        elementData,
        onElementDataChange,
        form,
    } = props;

    const dispatch = useAppDispatch();

    const [searchParams, setSearchParams] = useSearchParams();

    const value: IdentityData | undefined | null = elementData[IdentityCustomerInputKey]?.inputValue ?? undefined;
    const error: string[] | null | undefined = elementData[IdentityCustomerInputKey]?.computedErrors ?? undefined;

    const [identityLinks, setIdentityLinks] = useState<CombinedIdentityProviderLink[]>();
    const [identityData, setIdentityData] = useState<IdentityData>();
    const [isProcessing, setIsProcessing] = useState(false);

    useEffect(() => {
        getIdentityProviderLinks(form)
            .then(setIdentityLinks)
            .catch((err) => {
                console.error('Error fetching identity providers:', err);
                dispatch(showErrorSnackbar('Fehler beim Laden der Nutzerkontenanbieter.'));
            });
    }, [form]);

    useEffect(() => {
        processIdentityResult(searchParams, setSearchParams)
            .then((res) => {
                if (res == null) {
                    // No identity data or error, do nothing
                } else if (typeof res === 'string') {
                    // Error message
                    console.error(res);
                    dispatch(showErrorSnackbar(res));
                } else {
                    // Valid identity data
                    setIdentityData(res);
                }
            })
            .catch((err) => {
                console.error('Error processing identity result:', err);
                dispatch(showErrorSnackbar(err.message || 'Ein Fehler ist aufgetreten.'));
            });
    }, [searchParams]);

    const successIdp = useMemo(() => {
        return (identityLinks ?? []).find(idp => idp.link.identityProviderKey === value?.providerKey);
    }, [identityLinks, value]);

    const handleIDPSuccess = () => {
        if (identityData == null) {
            return;
        }

        setIsProcessing(true);

        const lastSaveData: ElementData = CustomerInputService
            .loadCustomerInputState(form) ?? {};

        const prefilledData = prefillIdentityData(rootElement, lastSaveData, identityData);

        onElementDataChange({
            ...prefilledData,
            [IdentityCustomerInputKey]: {
                $type: ElementType.IntroductionStep,
                inputValue: identityData,
                isDirty: false,
                isVisible: true,
                isPrefilled: true,
                computedErrors: undefined,
                computedValue: undefined,
                computedOverride: undefined,
            },
        });

        setIsProcessing(false);
        setIdentityData(undefined);
    };

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
                                variant="h5"
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
                                variant="h5"
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

            <Dialog
                onClose={() => setIdentityData(undefined)}
                open={identityData != null}
                maxWidth="xs"
            >
                <DialogTitleWithClose
                    onClose={() => setIdentityData(undefined)}
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
                        onClick={handleIDPSuccess}
                        variant="contained"
                        disabled={isProcessing}
                    >
                        Mit Antrag fortfahren
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}

async function getIdentityProviderLinks(form: FormPublicProjection) {
    const idps = await FormsApiService.getIdentityProviders(form.id);

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

async function processIdentityResult(searchParams: URLSearchParams, setSearchParams: (sp: URLSearchParams) => void): Promise<string | IdentityData | undefined> {
    const identityId = searchParams.get(IdentityIdQueryParam);
    if (identityId == null) {
        return undefined;
    }

    const stateStr = searchParams.get(IdentityStateQueryParam);
    const state = stateStr != null ? parseInt(stateStr) : undefined;
    if (state == null || isNaN(state)) {
        return undefined;
    }

    if (state !== IdentityResultState.Success) {
        return 'Bei der Authentifizierung ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.';
    }

    let result: IdentityData | undefined;
    try {
        result = await IdentityProvidersApiService.fetchIdentity(identityId);
    } catch (err) {
        console.error('Error fetching identity data:', err);
        throw new Error('Beim Abruf der Authentifizierungsdaten ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.');
    }

    searchParams.delete(IdentityIdQueryParam);
    searchParams.delete(IdentityStateQueryParam);
    setSearchParams(searchParams);

    return result;
}