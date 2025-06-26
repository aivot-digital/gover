import {Box, Button, Dialog, DialogActions, DialogContent, Divider, Grid, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedForm} from '../../slices/app-slice';
import React, {useMemo, useState} from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {ViewDispatcherComponent} from '../../components/view-dispatcher.component';
import {flattenElements} from '../../utils/flatten-elements';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {CustomerInput} from '../../models/customer-input';
import {downloadQrCode} from '../../utils/download-qrcode';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {StepElement} from '../../models/elements/steps/step-element';
import {AnyElement} from '../../models/elements/any-element';
import {Hint} from '../../components/hint/hint';
import {prefillQueryParamKey} from '../../data/prefill-query-param-key';
import MuiCollapse from '@mui/material/Collapse';
import {downloadObjectFile, uploadObjectFile} from '../../utils/download-utils';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {getElementNameForType} from '../../data/element-type/element-names';
import QrCode2OutlinedIcon from '@mui/icons-material/QrCode2Outlined';
import ImportExportOutlinedIcon from '@mui/icons-material/ImportExportOutlined';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import ContentPasteOutlinedIcon from '@mui/icons-material/ContentPasteOutlined';
import {ExpandMoreOutlined} from '@mui/icons-material';
import {Accordion, AccordionDetails, AccordionGroup, AccordionSummary} from '../../components/accordion/accordion';
import {getStepIcon} from '../../data/step-icons';
import {AlertComponent} from '../../components/alert/alert-component';
import Chip from '@mui/material/Chip';

interface PrefillFormDialogProps {
    open: boolean;
    onClose: () => void;
}

const relevantElementTypes = [
    ElementType.Text,
    ElementType.Number,
    ElementType.Date,
    ElementType.Time,
    ElementType.Select,
    ElementType.Radio,
    ElementType.Checkbox,
    ElementType.MultiCheckbox,
];

const MAX_LINK_LENGTH = 2000; // Most sources suggest 2048 maximum for URLs, but some browsers may have lower limits, so playing safe here.

export function canPrefillElement(e: AnyElement): boolean {
    return (
        isAnyInputElement(e) &&
        relevantElementTypes.includes(e.type) &&
        e.technical != true &&
        e.disabled != true
    );
}

export function PrefillFormDialog(props: PrefillFormDialogProps) {
    const dispatch = useAppDispatch();
    const [inputs, setInputs] = useState<CustomerInput>({});
    const form = useAppSelector(selectLoadedForm);

    const link = useMemo(() => {
        if (form == null) {
            return '';
        }

        const versionedLink = `${window.location.protocol}//${window.location.host}/${form.slug}/${form.version}`;

        const queryParams = new URLSearchParams({
            [prefillQueryParamKey]: JSON.stringify(inputs),
        }).toString();

        return `${versionedLink}?${queryParams}`;
    }, [inputs, form]);

    const linkTooLong = useMemo(() => {
        return link.length > MAX_LINK_LENGTH;
    }, [link.length]);

    const linkOverflow = useMemo(() => {
        return link.length - MAX_LINK_LENGTH;
    }, [link.length]);

    const allElementsPerStep: {
        step: StepElement,
        elements: AnyElement[],
    }[] = useMemo(() => {
        if (form == null) {
            return [];
        }

        return form.root.children
            .map((s) => {
                const stepElements = flattenElements(s, true)
                    .filter(canPrefillElement);
                return {
                    step: s,
                    elements: stepElements,
                };
            })
    }, [form]);

    const handleCopyLink = async () => {
        if (form == null) {
            return;
        }

        try {
            await navigator.clipboard.writeText(link);
            dispatch(showSuccessSnackbar('Link wurde in die Zwischenablage kopiert!'));
        } catch {
            dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
        }
    };

    const handleDownloadQrCode = async () => {
        if (form == null) {
            return;
        }

        try {
            await downloadQrCode(link, `${form.slug}-${form.version}-prefill.png`);
            dispatch(showSuccessSnackbar('QR-Code wurde als PNG heruntergeladen!'));
        } catch {
            dispatch(showErrorSnackbar('Fehler beim Herunterladen des QR-Codes!'));
        }
    };

    const handleExport = () => {
        if (form == null) {
            return;
        }

        downloadObjectFile(`prefill-${form.slug}-${form.version}.json`, inputs);
    };

    const handleImport = () => {
        if (form == null) {
            return;
        }

        uploadObjectFile<CustomerInput>('.json,application/json')
            .then((importedValues) => {
                if (importedValues == null) {
                    return;
                }

                const validValues: CustomerInput = {};
                for (const step of allElementsPerStep) {
                    for (const element of step.elements) {
                        const importedValue = importedValues[element.id];
                        if (importedValue != null) {
                            validValues[element.id] = importedValue;
                        }
                    }
                }

                if (Object.keys(validValues).length === 0) {
                    dispatch(showErrorSnackbar('Keine gültigen Eingaben zum Importieren gefunden.'));
                } else {
                    setInputs(validValues);
                    dispatch(showSuccessSnackbar('Daten erfolgreich importiert!'));
                }
            })
            .catch((error) => {
                console.error(error);
                dispatch(showErrorSnackbar('Fehler beim Importieren der Daten'));
            });
    };

    const handleClose = () => {
        setInputs({});
        props.onClose();
    };

    return (
        <Dialog
            open={props.open}
            onClose={handleClose}
            maxWidth="lg"
            fullWidth
        >
            <DialogTitleWithClose onClose={handleClose}>
                Formular vorbefüllen
            </DialogTitleWithClose>

            <DialogContent>
                <Box sx={{maxWidth: 920}}>
                    <Typography variant="body2">
                        Mit diesem Werkzeug können Sie einen Link erzeugen, über den ein Formular mit vorab definierten Werten vorbefüllt wird.
                        Das ist besonders nützlich, wenn Sie ein Formular z. B. an einen Personenkreis mit teilweise bereits bekannten Angaben weitergeben möchten.
                    </Typography>
                    <Typography
                        variant="h5"
                        sx={{mt: 2}}
                    >
                        Wichtige Hinweise zur Verwendung
                    </Typography>

                    <Box
                        component="ul"
                        sx={{
                            pl: 3,
                            mt: 1,
                            mb: 2,
                            typography: 'body2',
                            '& li': {
                                mb: 1,
                            },
                        }}
                    >
                        <li>Es können ausschließlich die folgenden Felder vorbefüllt werden: {
                            relevantElementTypes
                                .map(getElementNameForType)
                                .join(', ')
                        }. Technische Felder, bedingt sichtbare Felder und deaktivierte Felder können nicht vorbefüllt werden.
                        </li>
                        <li>Der erzeugte Link enthält alle vorbefüllten Werte und kann dadurch sehr lang werden. Aus technischen Gründen ist die maximale Länge auf {MAX_LINK_LENGTH} Zeichen begrenzt – längere Links können in manchen
                            Browsern zu Problemen führen.
                        </li>
                        <li>Die eingegebenen Werte werden nicht gespeichert, sondern nur im Link kodiert. Wenn Sie den Link später bearbeiten möchten, können Sie die vorbefüllten Werte exportieren und ggf. wieder importieren.</li>
                    </Box>
                </Box>

                <Divider sx={{my: 4}} />

                {
                    allElementsPerStep.length === 0 ? (
                        <AlertComponent
                            color="info"
                            title={'Dieses Formular enthält keine vorbefüllbaren Felder'}
                        >
                            Um dieses Werkzeug nutzen zu können, muss das Formular Eingabefelder enthalten, die vorbefüllt werden können.
                            Es können ausschließlich die folgenden Felder vorbefüllt werden: {
                            relevantElementTypes
                                .map(getElementNameForType)
                                .join(', ')
                        }.
                            Technische Felder, bedingt sichtbare Felder und deaktivierte Felder können nicht vorbefüllt werden.
                        </AlertComponent>
                    ) : (
                        <AccordionGroup sx={{mb: 2}}>
                            {
                                allElementsPerStep
                                    .map(({step, elements}) => (
                                        <Accordion
                                            key={step.id}
                                            slots={{transition: MuiCollapse}}
                                            slotProps={{
                                                transition: {
                                                    unmountOnExit: true,
                                                },
                                            }}
                                        >
                                            <AccordionSummary
                                                expandIcon={<ExpandMoreOutlined />}
                                                aria-controls={`panel-${step.id}-content`}
                                                id={`panel-${step.id}-header`}
                                            >
                                                {(() => {
                                                    const StepIcon = getStepIcon(step);
                                                    return <StepIcon sx={{mr: 1}} />;
                                                })()}
                                                <Typography>
                                                    {generateComponentTitle(step)} {elements.length === 0 && <Chip sx={{ml: 1}} label={"Keine vorbefüllbaren Felder vorhanden"} size='small' variant={"outlined"}/>}
                                                </Typography>
                                            </AccordionSummary>

                                            <AccordionDetails>
                                                <Grid
                                                    container
                                                    spacing={2}
                                                >
                                                    {elements.length === 0 &&
                                                        <Grid
                                                            item
                                                        >
                                                            <AlertComponent color={'info'} title={"Dieser Abschnitt enthält keine vorbefüllbaren Felder"} sx={{mt: 1, mb: 0}}>
                                                                Es können ausschließlich die folgenden Felder vorbefüllt werden: {
                                                                    relevantElementTypes
                                                                        .map(getElementNameForType)
                                                                        .join(', ')
                                                                }.
                                                                Technische Felder, bedingt sichtbare Felder und deaktivierte Felder können nicht vorbefüllt werden.
                                                            </AlertComponent>
                                                        </Grid>
                                                    }

                                                    {elements.map((element) => (
                                                        <ViewDispatcherComponent
                                                            key={element.id}
                                                            allElements={[]}
                                                            element={{
                                                                ...element,
                                                                width: 12,
                                                            }}
                                                            isBusy={false}
                                                            isDeriving={false}
                                                            mode="viewer"
                                                            valueOverride={{
                                                                values: inputs,
                                                                onChange: (key, value) => {
                                                                    setInputs({
                                                                        ...inputs,
                                                                        [key]: value,
                                                                    });
                                                                },
                                                            }}
                                                            visibilitiesOverride={{}}
                                                            overridesOverride={{}}
                                                        />
                                                    ))}
                                                </Grid>
                                            </AccordionDetails>
                                        </Accordion>
                                    ))
                            }
                        </AccordionGroup>
                    )
                }
            </DialogContent>

            <DialogActions>
                <Box
                    sx={{
                        display: 'flex',
                        justifyContent: 'flex-start',
                        width: '100%',
                    }}
                >
                    <Button
                        variant={'contained'}
                        onClick={handleCopyLink}
                        disabled={linkTooLong}
                        startIcon={<ContentPasteOutlinedIcon />}
                    >
                        Link in Zwischenablage kopieren
                    </Button>
                    {
                        linkTooLong &&
                        <Hint
                            summary={`Der erzeugte Link ist um ${linkOverflow} Zeichen zu lang und überschreitet damit das technische Limit.`}
                            detailsTitle="Link zu lang"
                            details="Der erzeugte Link ist zu lang, um ihn zu kopieren oder als QR-Code bereitzustellen. Bitte reduzieren Sie die Anzahl der vorbefüllten Felder oder kürzen Sie deren Inhalte."
                            isError={true}
                            sx={{
                                ml: 0,
                                mr: 2,
                            }}
                        />
                    }

                    <Button
                        onClick={handleDownloadQrCode}
                        disabled={linkTooLong}
                        startIcon={<QrCode2OutlinedIcon />}
                        sx={{
                            ml: 2,
                        }}
                    >
                        QR-Code mit Link herunterladen
                    </Button>

                    {
                        linkTooLong &&
                        <Hint
                            summary={`Der erzeugte Link ist um ${linkOverflow} Zeichen zu lang und überschreitet damit das technische Limit.`}
                            detailsTitle="Link zu lang"
                            details="Der erzeugte Link ist zu lang, um ihn zu kopieren oder als QR-Code bereitzustellen. Bitte reduzieren Sie die Anzahl der vorbefüllten Felder oder kürzen Sie deren Inhalte."
                            isError={true}
                            sx={{
                                ml: 0,
                                mr: 2,
                            }}
                        />
                    }

                    <Button
                        onClick={handleExport}
                        startIcon={<ImportExportOutlinedIcon />}
                        sx={{
                            ml: 'auto',
                        }}
                    >
                        Eingaben exportieren
                    </Button>

                    <Button
                        onClick={handleImport}
                        startIcon={<CloudUploadOutlinedIcon />}
                        sx={{
                            ml: 2,
                        }}
                    >
                        Eingaben importieren
                    </Button>
                </Box>
            </DialogActions>
        </Dialog>
    );
}