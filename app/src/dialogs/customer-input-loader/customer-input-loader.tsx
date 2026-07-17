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
import SettingsBackupRestoreOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/SettingsBackupRestore';
import {useSearchParams} from 'react-router-dom';
import RestorePageIcon from '@aivot/mui-material-symbols-400-n25-outlined/RestorePage';
import ArrowForwardOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import {type AuthoredElementValues, hasAuthoredElementValuesSomeInput} from '../../models/element-data';
import {prefillQueryParamKey} from '../../data/prefill-query-param-key';
import {isStringNullOrEmpty} from '../../utils/string-utils';
import {canPrefillElement} from '../prefill-form-dialog/prefill-form-dialog';
import {flattenElements} from '../../utils/flatten-elements';
import {type CustomerInputDraft, CustomerInputService} from '../../services/customer-input-service';
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

    const [customerInputDraft, setCustomerInputDraft] = useState<CustomerInputDraft | null | undefined>(undefined);
    const [urlPrefillData, setUrlPrefillData] = useState<AuthoredElementValues | null | undefined>(undefined);

    useEffect(() => {
        initializeLocalStorageData(
            processSlug,
            formSlug,
            version,
            setCustomerInputDraft,
        );
        initializeUrlPrefillData(
            rootElement,
            setUrlPrefillData,
            searchParams,
        );
    }, [processSlug, formSlug, version, rootElement, searchParams]);

    const dialogState: 'waiting' | 'load' | 'none' = useMemo(() => {
        if (customerInputDraft === undefined || urlPrefillData === undefined) {
            return 'waiting';
        }
        if (customerInputDraft != null) {
            return 'load';
        }
        return 'none';
    }, [customerInputDraft, urlPrefillData]);

    const handleResolved = () => {
        setCustomerInputDraft(null);
        setUrlPrefillData(null);
        clearConsumedSearchParams(searchParams, setSearchParams);
        onResolved();
    };

    const handleLoadData = () => {
        if (customerInputDraft != null) {
            onElementDataLoad(customerInputDraft.data);
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
                        Auf Ihrem Gerät existiert ein zwischengespeicherter Entwurf für dieses Formular. Möchten Sie
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
                                Formular-Entwurf aus Ihrem lokalen Speicher
                            </Typography>
                            <Typography
                                variant="body2"
                                sx={{
                                    mt: -0.5,
                                }}
                            >
                                {
                                    customerInputDraft?.date != null && (
                                        <span>
                                            Zuletzt bearbeitet am {format(customerInputDraft.date, 'dd.MM.yyyy')} um {format(customerInputDraft.date, 'HH:mm')} Uhr
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
                        Bitte beachten Sie, dass hinzugefügte Anlagen/Dateien aus Datenschutzgründen nicht
                        gespeichert wurden und ggf. <b>erneut hinzugefügt</b> werden müssen.
                    </Typography>
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
                                    setCustomerInputDraft: (data: CustomerInputDraft | null) => void) {
    setCustomerInputDraft(CustomerInputService.loadCustomerInputDraft(processSlug, formSlug, version));
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
