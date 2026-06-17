import React, {useEffect, useMemo, useState} from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogTitle from '@mui/material/DialogTitle';
import Typography from '@mui/material/Typography';
import {format} from 'date-fns';
import SettingsBackupRestoreOutlinedIcon from '@mui/icons-material/SettingsBackupRestoreOutlined';
import {useSearchParams} from 'react-router-dom';
import RestorePageIcon from '@mui/icons-material/RestorePage';
import ArrowForwardOutlinedIcon from '@mui/icons-material/ArrowForwardOutlined';
import {type AuthoredElementValues, hasAuthoredElementValuesSomeInput} from '../../models/element-data';
import {prefillQueryParamKey} from '../../data/prefill-query-param-key';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {canPrefillElement} from '../prefill-form-dialog/prefill-form-dialog';
import {flattenElements} from '../../utils/flatten-elements';
import {CustomerInputService} from '../../services/customer-input-service';
import {FormLayoutElement} from '../../models/elements/form-layout-element';

interface CustomerInputLoaderProps {
    processSlug: string;
    formSlug: string;
    version: number;
    rootElement: FormLayoutElement;
    onElementDataLoad: (elementData: AuthoredElementValues) => void;
    onResolved: () => void;
    isBusy: boolean;
}

interface LocalStorageData {
    date: Date;
    data: AuthoredElementValues;
}

export function CustomerInputLoader(props: CustomerInputLoaderProps) {
    const {
        processSlug,
        formSlug,
        version,
        rootElement,
        onElementDataLoad,
        onResolved,
        isBusy,
    } = props;

    const [searchParams, setSearchParams] = useSearchParams();

    const [localStorageData, setLocalStorageData] = useState<LocalStorageData | null | undefined>(undefined);
    const [urlPrefillData, setUrlPrefillData] = useState<AuthoredElementValues | null | undefined>(undefined);

    useEffect(() => {
        initializeLocalStorageData(
            processSlug,
            formSlug,
            version,
            setLocalStorageData,
        );
        initializeUrlPrefillData(
            rootElement,
            setUrlPrefillData,
            searchParams,
        );
    }, [processSlug, formSlug, version, rootElement, searchParams]);

    const dialogState: 'waiting' | 'load' | 'none' = useMemo(() => {
        if (localStorageData === undefined || urlPrefillData === undefined) {
            return 'waiting';
        }
        if (localStorageData != null) {
            return 'load';
        }
        return 'none';
    }, [localStorageData, urlPrefillData]);

    const handleResolved = () => {
        setLocalStorageData(null);
        setUrlPrefillData(null);
        clearConsumedSearchParams(searchParams, setSearchParams);
        onResolved();
    };

    const handleLoadData = () => {
        if (localStorageData != null) {
            onElementDataLoad(localStorageData.data);
        }

        handleResolved();
    };

    const handleRestart = () => {
        if (urlPrefillData != null) {
            onElementDataLoad(urlPrefillData);
        }

        CustomerInputService.cleanCustomerInput(processSlug, formSlug, version);
        handleResolved();
    };

    useEffect(() => {
        if (dialogState !== 'none') {
            return;
        }

        if (urlPrefillData != null) {
            onElementDataLoad(urlPrefillData);
        }

        handleResolved();
    }, [dialogState]);

    return (
        <Dialog
            open={dialogState === 'load'}
            disableEscapeKeyDown={true}
        >
            <DialogTitle>
                <Typography
                    variant="h4"
                    component="div"
                >
                    Möchten Sie den existierenden Entwurf fortführen?
                </Typography>
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
                        alignItems="center"
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
                                color="primary"
                                sx={{fontSize: 54}}
                            />
                        </Box>

                        <Box sx={{ml: 2}}>
                            <Typography
                                component="p"
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
    );
}

function initializeLocalStorageData(processSlug: string,
                                    formSlug: string,
                                    version: number,
                                    setLocalStorageData: (data: LocalStorageData | null) => void) {
    const date = CustomerInputService.loadCustomerInputDate(processSlug, formSlug, version);
    const data = CustomerInputService.loadCustomerInputState(processSlug, formSlug, version);

    if (date != null && data != null && hasAuthoredElementValuesSomeInput(data)) {
        setLocalStorageData({
            date,
            data,
        });
    } else {
        setLocalStorageData(null);
    }
}

function initializeUrlPrefillData(rootElement: FormLayoutElement,
                                  setUrlPrefillData: (data: AuthoredElementValues | null) => void,
                                  searchParams: URLSearchParams): void {
    const prefill = searchParams
        .get(prefillQueryParamKey);

    if (prefill == null || isStringNullOrEmpty(prefill)) {
        setUrlPrefillData(null);
        return;
    }

    let prefillData: unknown;
    try {
        prefillData = JSON.parse(decodeURIComponent(prefill));
    } catch (error) {
        console.error('Error parsing prefill data:', error);
        setUrlPrefillData(null);
        return;
    }

    if (prefillData == null || typeof prefillData !== 'object' || Array.isArray(prefillData)) {
        setUrlPrefillData(null);
        return;
    }

    const allElements = flattenElements(rootElement, true);
    const cleanedPrefillData: AuthoredElementValues = {};

    for (const key of Object.keys(prefillData)) {
        const value = (prefillData as Record<string, unknown>)[key];
        const elem = allElements
            .find(e => e.id === key);

        if (elem != null && canPrefillElement(elem)) {
            cleanedPrefillData[key] = value;
        }
    }

    setUrlPrefillData(
        hasAuthoredElementValuesSomeInput(cleanedPrefillData) ?
            cleanedPrefillData :
            null,
    );
}

function clearConsumedSearchParams(searchParams: URLSearchParams,
                                   setSearchParams: ReturnType<typeof useSearchParams>[1]): void {
    if (!searchParams.has(prefillQueryParamKey)) {
        return;
    }

    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.delete(prefillQueryParamKey);

    setSearchParams(nextSearchParams, {
        replace: true,
    });
}
