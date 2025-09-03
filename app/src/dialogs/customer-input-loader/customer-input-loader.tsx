import React, {useEffect, useMemo, useState} from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Typography from '@mui/material/Typography';
import {CustomerInputService} from '../../services/customer-input-service';
import {format} from 'date-fns';
import {type Form} from '../../models/entities/form';
import SettingsBackupRestoreOutlinedIcon from '@mui/icons-material/SettingsBackupRestoreOutlined';
import {useSearchParams} from 'react-router-dom';
import RestorePageIcon from '@mui/icons-material/RestorePage';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';
import {IdentityStateQueryParam} from '../../modules/identity/constants/identity-state-query-param';
import {type ElementData, hasElementDataSomeInput, newElementDataObject} from '../../models/element-data';
import {IdentityIdQueryParam} from '../../modules/identity/constants/identity-id-query-param';
import {prefillQueryParamKey} from '../../data/prefill-query-param-key';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {canPrefillElement} from '../prefill-form-dialog/prefill-form-dialog';
import {flattenElements} from '../../utils/flatten-elements';
import {IdentityResultState} from '../../modules/identity/enums/identity-result-state';
import {IdentityData} from '../../modules/identity/models/identity-data';
import {IdentityProvidersApiService} from '../../modules/identity/identity-providers-api-service';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {IdentityProviderInfo} from '../../modules/identity/models/identity-provider-info';
import {FormsApiService} from '../../modules/forms/forms-api-service';
import {Page} from '../../models/dtos/page';
import {prefillIdentityData} from '../../utils/prefill-elements';
import {IdentityCustomerInputKey} from '../../modules/identity/constants/identity-customer-input-key';
import {ElementType} from '../../data/element-type/element-type';
import {Api, useApi} from '../../hooks/use-api';

interface LoadUserInputDialogProps {
    form: Form;
    onElementDataLoad: (elementData: ElementData) => void;
    isBusy: boolean;
}

interface LocalStorageData {
    date: Date;
    data: ElementData;
}

interface IdentityPreloadedData {
    identity: IdentityData;
    provider: IdentityProviderInfo;
}

export function CustomerInputLoader(props: LoadUserInputDialogProps) {
    const {
        form,
        onElementDataLoad,
        isBusy,
    } = props;

    const api = useApi();

    const [searchParams, setSearchParams] = useSearchParams();

    const [localStorageData, setLocalStorageData] = useState<LocalStorageData | null | undefined>(undefined);
    const [urlPrefillData, setUrlPrefillData] = useState<Record<string, any> | null | undefined>(undefined);
    const [identityData, setIdentityData] = useState<IdentityPreloadedData | string | null | undefined>(null);

    useEffect(() => {
        initializeLocalStorageData(form, setLocalStorageData);
        initializeUrlPrefillData(form, setUrlPrefillData, searchParams);
        initializeIdentityData(api, form, setIdentityData, searchParams)
            .catch((err) => {
                console.error('Error initializing identity data:', err);
                setIdentityData('Fehler beim Laden der Authentifizierungsdaten. Bitte versuchen Sie es erneut.');
            });
    }, [form]);

    const dialogState: 'waiting' | 'load' | 'identity' | 'none' = useMemo(() => {
        if (localStorageData === undefined || urlPrefillData === undefined || identityData === undefined) {
            return 'waiting';
        }
        if (identityData != null) {
            return 'identity';
        }
        if (localStorageData != null) {
            return 'load';
        }
        return 'none';
    }, [identityData, localStorageData]);

    const handleLoadData = () => {
        console.log('Loading local storage data:', localStorageData);

        if (localStorageData != null) {
            onElementDataLoad(localStorageData.data);
        }

        handleCleanup();
    };

    const handleRestart = () => {
        console.log('Handling restart of form');
        handleInsertUrlPrefillData();
        CustomerInputService.cleanCustomerInput(form);
        handleCleanup();
    };

    const handleInsertUrlPrefillData = () => {
        console.log('Handling insert of URL prefill data:', urlPrefillData);

        if (urlPrefillData != null) {
            const allElements = flattenElements(form.rootElement, true);

            const cleanedPrefillData: ElementData = {};

            for (const key of Object.keys(urlPrefillData)) {
                const value = urlPrefillData[key];

                const elem = (allElements ?? [])
                    .find(e => e.id === key);

                if (elem != null && canPrefillElement(elem)) {
                    const dataObject = newElementDataObject(elem.type);
                    dataObject.inputValue = value;
                    cleanedPrefillData[key] = dataObject;
                }
            }

            onElementDataLoad(cleanedPrefillData);
        }

        handleCleanup();
    };

    const handleContinueAfterIdentitySuccess = () => {
        console.log('Continuing after identity success:', identityData);

        if (identityData != null && typeof identityData !== 'string') {
            let prefilledData: ElementData;
            if (localStorageData != null) {
                console.log('Merging local storage data with identity data');
                prefilledData = prefillIdentityData(form.rootElement, localStorageData.data, identityData.identity);
            } else {
                console.log('Prefilling identity data into form');
                prefilledData = prefillIdentityData(form.rootElement, {}, identityData.identity);
            }

            prefilledData[IdentityCustomerInputKey] = {
                ...newElementDataObject(ElementType.IntroductionStep),
                inputValue: identityData.identity,
            };

            onElementDataLoad(prefilledData);
        }

        handleCleanup();
    };

    const handleCleanup = () => {
        console.log('Cleaning up customer input loader state');

        setLocalStorageData(null);
        setUrlPrefillData(null);
        setIdentityData(null);
        setSearchParams({});
    };

    useEffect(() => {
        if (dialogState === 'none') {
            handleInsertUrlPrefillData();
        }
    }, [dialogState]);

    return (
        <>
            <Dialog
                open={dialogState === 'load'}
                disableEscapeKeyDown={true}
            >
                <DialogTitle>
                    Möchten Sie den existierenden Entwurf fortführen?
                </DialogTitle>
                <DialogContent tabIndex={0}>
                    <DialogContentText component="div">
                        <Typography variant="body2">
                            Auf Ihrem Gerät existiert ein zwischengespeicherter Entwurf für diesen Antrag. Möchten Sie
                            diesen Entwurf verwenden und weiter bearbeiten?
                        </Typography>

                        <Box
                            display="flex"
                            justifyContent="center"
                            alignItems={'center'}
                            sx={{
                                border: '1px solid #DFDFDF',
                                px: 4,
                                py: 2,
                                mt: 3,
                                mb: 3,
                            }}
                        >
                            <Box>
                                <RestorePageIcon
                                    color={'primary'}
                                    sx={{fontSize: 54}}
                                />
                            </Box>

                            <Box sx={{ml: 2}}>
                                <Typography
                                    component={'p'}
                                    variant="h6"
                                    sx={{
                                        color: '#16191F',
                                        mt: -0.5,
                                    }}
                                >
                                    Antrags-Entwurf aus Ihrem lokalen Speicher
                                </Typography>
                                <Typography
                                    variant="body2"
                                    sx={{
                                        mt: -0.5,
                                    }}
                                >
                                    {
                                        localStorageData?.date != null && (
                                            <span>
                                                Zuletzt bearbeitet am {format(localStorageData.date, 'dd.MM.yyyy')} um {format(localStorageData.date, 'HH:mm')} Uhr
                                            </span>
                                        )
                                    }
                                </Typography>
                            </Box>
                        </Box>

                        <Typography
                            variant="body2"
                            gutterBottom
                        >
                            Bitte beachten Sie, dass Sie aus Datenschutzgründen ggf. folgende Aktionen <b>erneut
                            ausführen</b> müssen, da diese nicht gespeichert wurden:
                        </Typography>
                        <ul style={{margin: 0}}>
                            <li>Anmeldung mit einem Nutzer- oder Unternehmenskonto</li>
                            <li>Hinzufügen von Anlagen/Dateien</li>
                        </ul>
                    </DialogContentText>
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={handleLoadData}
                        variant="contained"
                        startIcon={
                            <ArrowForwardOutlinedIcon />
                        }
                        disabled={isBusy}
                    >
                        Entwurf fortführen
                    </Button>
                    <Button
                        onClick={handleRestart}
                        startIcon={
                            <SettingsBackupRestoreOutlinedIcon />
                        }
                        disabled={isBusy}
                    >
                        Neu beginnen
                    </Button>
                </DialogActions>
            </Dialog>

            <Dialog
                onClose={() => setIdentityData(undefined)}
                open={dialogState === 'identity'}
                maxWidth="xs"
            >
                <DialogTitleWithClose
                    onClose={() => setIdentityData(undefined)}
                    closeTooltip="Schließen"
                >
                    {
                        typeof identityData === 'string' ?
                            'Authentifizierungsfehler' :
                            'Authentifizierung erfolgreich'
                    }
                </DialogTitleWithClose>
                <DialogContent className="content-without-margin-on-childs">
                    {
                        typeof identityData === 'string' ?
                            <Box>
                                <Typography>
                                    Fehler bei der Authentifizierung.
                                </Typography>
                                <Typography>
                                    {identityData}
                                </Typography>
                            </Box> :
                            <Box>
                                <Typography>
                                    Sie haben sich erfolgreich mit dem Nutzerkonto <strong>„{identityData?.provider.name}“</strong> angemeldet.
                                    Die Daten aus Ihrem Konto wurden automatisch in den Antrag übernommen.
                                </Typography>
                            </Box>
                    }
                </DialogContent>
                <DialogActions>
                    <Button
                        onClick={handleContinueAfterIdentitySuccess}
                        variant="contained"
                        startIcon={
                            <ArrowForwardOutlinedIcon />
                        }
                        disabled={isBusy}
                    >
                        {
                            typeof identityData === 'string' ?
                                'Fortfahren und erneut versuchen' :
                                'Mit Antrag fortfahren'
                        }
                    </Button>
                </DialogActions>
            </Dialog>
        </>
    );
}

function initializeLocalStorageData(form: Form, setLocalStorageData: (data: LocalStorageData | null) => void) {
    const date = CustomerInputService.loadCustomerInputDate(form);
    const data = CustomerInputService.loadCustomerInputState(form);

    if (date != null && data != null && hasElementDataSomeInput(data)) {
        setLocalStorageData({
            date,
            data,
        });
    } else {
        setLocalStorageData(null);
    }
}

function initializeUrlPrefillData(
    form: Form,
    setUrlPrefillData: (data: Record<string, any> | null) => void,
    searchParams: URLSearchParams,
): void {
    const prefill = searchParams
        .get(prefillQueryParamKey);

    if (prefill == null || isStringNullOrEmpty(prefill)) {
        setUrlPrefillData(null);
        return;
    }

    const prefillData = JSON.parse(decodeURIComponent(prefill));

    if (prefillData == null || typeof prefillData !== 'object') {
        setUrlPrefillData(null);
        return;
    }

    const allElements = flattenElements(form.rootElement, true);

    const cleanedPrefillData: Record<string, any> = {};

    for (const key of Object.keys(prefillData)) {
        const value = prefillData[key];

        const elem = (allElements ?? [])
            .find(e => e.id === key);

        if (elem != null && canPrefillElement(elem)) {
            cleanedPrefillData[key] = value;
        }
    }

    setUrlPrefillData(cleanedPrefillData);
}

async function initializeIdentityData(
    api: Api,
    form: Form,
    setIdentityData: (data: IdentityPreloadedData | string | null) => void,
    searchParams: URLSearchParams,
): Promise<void> {
    const identityId = searchParams.get(IdentityIdQueryParam);
    if (identityId == null) {
        setIdentityData(null);
        return;
    }

    const stateStr = searchParams.get(IdentityStateQueryParam);
    const state = stateStr != null ? parseInt(stateStr) : undefined;
    if (state == null || isNaN(state)) {
        setIdentityData('Ungültiger Authentifizierungsstatus. Bitte versuchen Sie es erneut.');
        return;
    }

    if (state !== IdentityResultState.Success) {
        setIdentityData('Bei der Authentifizierung ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.');
        return;
    }

    let identityResult: IdentityData;
    try {
        identityResult = await IdentityProvidersApiService
            .fetchIdentity(identityId);
    } catch (err) {
        console.error('Error fetching identity data:', err);
        setIdentityData('Beim Abruf der Authentifizierungsdaten ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.');
        return;
    }

    let providerResults: Page<IdentityProviderInfo>;
    try {
        providerResults = await new FormsApiService(api)
            .getIdentityProviders(form.slug, form.version);
    } catch (err) {
        console.error('Error fetching identity providers:', err);
        setIdentityData('Beim Abruf der Authentifizierungsanbieter ist ein Fehler aufgetreten. Bitte versuchen Sie es erneut.');
        return;
    }

    const provider = providerResults.content
        .find(p => p.key === identityResult.providerKey);

    if (provider == null) {
        setIdentityData('Der Authentifizierungsanbieter ist nicht verfügbar. Bitte versuchen Sie es erneut.');
        return;
    }

    setIdentityData({
        identity: identityResult,
        provider: provider,
    });
}