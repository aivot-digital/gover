import {Box, Button, Dialog, DialogActions, DialogContent, Divider, Grid, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import React, {useCallback, useEffect, useMemo, useRef, useState} from 'react';
import {ElementType} from '../../data/element-type/element-type';
import {flattenElements} from '../../utils/flatten-elements';
import {generateComponentTitle} from '../../utils/generate-component-title';
import {CustomerInput} from '../../models/customer-input';
import {showErrorSnackbar, showSuccessSnackbar} from '../../slices/snackbar-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {StepElement} from '../../models/elements/steps/step-element';
import {AnyElement} from '../../models/elements/any-element';
import {Hint} from '../../components/hint/hint';
import {prefillQueryParamKey} from '../../data/prefill-query-param-key';
import MuiCollapse from '@mui/material/Collapse';
import {uploadObjectFile} from '../../utils/download-utils';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {getElementNameForType} from '../../data/element-type/element-names';
import QrCode2OutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/QrCode2';
import ImportExportOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/SwapVert';
import CloudUploadOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/CloudUpload';
import ContentPasteOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ContentPaste';
import ExpandMoreOutlined from '@aivot/mui-material-symbols-400-n25-outlined/ExpandMore';
import {Accordion, AccordionDetails, AccordionGroup, AccordionSummary} from '../../components/accordion/accordion';
import {getStepIcon} from '../../data/step-icons';
import {AlertComponent} from '../../components/alert/alert-component';
import Chip from '@mui/material/Chip';
import InfoOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Info';
import {
    AuthoredElementValues,
    createDerivedRuntimeElementData,
    DerivedRuntimeElementData,
} from '../../models/element-data';
import {copyToClipboardText} from '../../utils/copy-to-clipboard';
import {createCustomerPath} from '../../utils/url-path-utils';
import {FormLayoutElement} from '../../models/elements/form-layout-element';
import {ViewDispatcherComponent} from '../../components/view-dispatcher/view-dispatcher.component';

interface PrefillFormDialogProps {
    form: FormLayoutElement;
    open: boolean;
    onClose: () => void;
}

const prefillableElementTypes = [
    ElementType.Text,
    ElementType.Number,
    ElementType.Date,
    ElementType.Time,
    ElementType.Select,
    ElementType.Radio,
    ElementType.Checkbox,
    ElementType.MultiCheckbox,
    ElementType.ChipInput,
    ElementType.DateTime,
    ElementType.DateRange,
    ElementType.TimeRange,
    ElementType.DateTimeRange,
    ElementType.MapPoint,
];

const MAX_LINK_LENGTH = 2000; // Most sources suggest 2048 maximum for URLs, but some browsers may have lower limits, so playing safe here.

export function canPrefillElement(e: AnyElement): boolean {
    return (
        isAnyInputElement(e) &&
        prefillableElementTypes.includes(e.type) &&
        e.technical != true &&
        e.disabled != true
    );
}

function buildPrefillValues(elementData: AuthoredElementValues): AuthoredElementValues {
    const inputs: AuthoredElementValues = {};

    for (const key of Object.keys(elementData)) {
        const dataObject = elementData[key];
        if (dataObject != null) {
            inputs[key] = dataObject;
        }
    }

    return inputs;
}

function buildPrefillLink(slug: string, version: number, elementData: AuthoredElementValues): string {
    const versionedLink = createCustomerPath(`${slug}/${version}`);
    const queryParams = new URLSearchParams({
        [prefillQueryParamKey]: JSON.stringify(buildPrefillValues(elementData)),
    }).toString();

    return `${versionedLink}?${queryParams}`;
}

export function PrefillFormDialog(props: PrefillFormDialogProps) {
    const dispatch = useAppDispatch();
    const [elementData, setElementDataState] = useState<AuthoredElementValues>({});
    const [derivedData, setDerivedData] = useState<DerivedRuntimeElementData>(createDerivedRuntimeElementData());
    const [hasPrefillableElements, setHasPrefillableElements] = useState<Boolean>(false);
    const elementDataRef = useRef<AuthoredElementValues>({});
    const form = props.form;
    const setElementData = useCallback((nextData: AuthoredElementValues) => {
        elementDataRef.current = nextData;
        setElementDataState(nextData);
    }, []);

    const allElements = useMemo(() => {
        if (form == null) {
            return [];
        }

        return flattenElements(form, true);
    }, [form]);

    const link = useMemo(() => {
        if (form == null) {
            return '';
        }

        return ''; // TODO buildPrefillLink(form.form.slug, form.version.version, elementData);
    }, [elementData, form]);

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

        return (form.children ?? [])
            .map((s) => {
                const stepElements = flattenElements(s, true)
                    .filter(canPrefillElement);
                return {
                    step: s as StepElement,
                    elements: stepElements,
                };
            });
    }, [form]);

    useEffect(() => {
        setHasPrefillableElements(allElementsPerStep.some((x) => x.elements.length > 0));
    }, [allElementsPerStep]);

    const flushPendingElementData = useCallback(async (): Promise<AuthoredElementValues> => {
        const activeElement = document.activeElement;
        if (activeElement instanceof HTMLElement) {
            activeElement.blur();
            await new Promise<void>((resolve) => {
                window.setTimeout(resolve, 0);
            });
        }

        return elementDataRef.current;
    }, []);

    const handleCopyLink = useCallback(async () => {
        if (form == null) {
            return;
        }

        try {
            const latestElementData = await flushPendingElementData();
            const latestLink = ''; // TODO: buildPrefillLink(form.form.slug, form.version.version, latestElementData);
            if (latestLink.length > MAX_LINK_LENGTH) {
                dispatch(showErrorSnackbar('Der erzeugte Link ist zu lang und kann nicht kopiert werden.'));
                return;
            }

            const success = await copyToClipboardText(latestLink);
            if (!success) {
                throw new Error('copy failed');
            }
            dispatch(showSuccessSnackbar('Link wurde in die Zwischenablage kopiert!'));
        } catch {
            dispatch(showErrorSnackbar('Fehler beim Kopieren des Links!'));
        }
    }, [dispatch, flushPendingElementData, form]);

    const handleDownloadQrCode = useCallback(async () => {
        if (form == null) {
            return;
        }

        try {
            const latestElementData = await flushPendingElementData();
            const latestLink = ''; // TODO: buildPrefillLink(form.form.slug, form.version.version, latestElementData);
            if (latestLink.length > MAX_LINK_LENGTH) {
                dispatch(showErrorSnackbar('Der erzeugte Link ist zu lang und kann nicht als QR-Code bereitgestellt werden.'));
                return;
            }

            // TODO: await downloadQrCode(latestLink, `${form.form.slug}-${form.version.version}-prefill.png`);
            dispatch(showSuccessSnackbar('QR-Code wurde als PNG heruntergeladen!'));
        } catch {
            dispatch(showErrorSnackbar('Fehler beim Herunterladen des QR-Codes!'));
        }
    }, [dispatch, flushPendingElementData, form]);

    const handleExport = async () => {
        if (form == null) {
            return;
        }

        const latestElementData = await flushPendingElementData();
        // TODO: downloadObjectFile(`prefill-${form.form.slug}-${form.version.version}.json`, buildPrefillValues(latestElementData));
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
                    setElementData(validValues);
                    dispatch(showSuccessSnackbar('Daten erfolgreich importiert!'));
                }
            })
            .catch((error) => {
                console.error(error);
                dispatch(showErrorSnackbar('Fehler beim Importieren der Daten'));
            });
    };

    const handleClose = () => {
        setElementData({});
        setDerivedData(createDerivedRuntimeElementData());
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
                        Mit diesem Werkzeug können Sie einen Link erzeugen, über den ein Formular mit vorab definierten
                        Werten vorbefüllt wird.
                        Das ist besonders nützlich, wenn Sie ein Formular z. B. an einen Personenkreis mit teilweise
                        bereits bekannten Angaben weitergeben möchten.
                    </Typography>
                </Box>

                <AccordionGroup sx={{mt: 3}}>
                    <Accordion defaultExpanded>
                        <AccordionSummary
                            expandIcon={<ExpandMoreOutlined/>}
                            aria-controls={`panel-moreinfo-content`}
                            id={`panel-moreinfo-header`}
                        >
                            <InfoOutlinedIcon sx={{mr: 1}}/>
                            <Typography>
                                Wichtige Hinweise zur Verwendung
                            </Typography>
                        </AccordionSummary>

                        <AccordionDetails>
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
                                    maxWidth: 920,
                                }}
                            >
                                <li>Es können ausschließlich die folgenden Felder vorbefüllt werden: {
                                    prefillableElementTypes
                                        .map(getElementNameForType)
                                        .join(', ')
                                }. Technische Felder und deaktivierte Felder können nicht vorbefüllt werden.
                                </li>
                                <li>Für die Anzeige der vorbefüllbaren Felder (s.u.) ist die Berechnung der
                                    Sichtbarkeiten deaktiviert. Bitte bedenken Sie, dass demnach auch Felder vorbefüllt
                                    werden können, die ggf. später im durch Nutzer:innen aufgerufenen Formular nicht
                                    sichtbar sein könnten.
                                </li>
                                <li>Der erzeugte Link enthält alle vorbefüllten Werte und kann dadurch sehr lang werden.
                                    Aus technischen Gründen ist die maximale Länge auf {MAX_LINK_LENGTH} Zeichen
                                    begrenzt – längere Links können in manchen
                                    Browsern zu Problemen führen.
                                </li>
                                <li>Die eingegebenen Werte werden nicht gespeichert, sondern nur im Link kodiert. Wenn
                                    Sie den Link später bearbeiten möchten, können Sie die vorbefüllten Werte
                                    exportieren und ggf. wieder importieren.
                                </li>
                            </Box>
                        </AccordionDetails>
                    </Accordion>
                </AccordionGroup>

                <Divider sx={{my: 4}}/>

                {
                    allElementsPerStep.length === 0 ? (
                        <AlertComponent
                            color="info"
                            title={'Dieses Formular enthält keine vorbefüllbaren Felder'}
                        >
                            Um dieses Werkzeug nutzen zu können, muss das Formular Eingabefelder enthalten, die
                            vorbefüllt werden können.
                            Es können ausschließlich die folgenden Felder vorbefüllt werden: {
                            prefillableElementTypes
                                .map(getElementNameForType)
                                .join(', ')
                        }.
                            Technische Felder und deaktivierte Felder können nicht vorbefüllt werden.
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
                                                expandIcon={<ExpandMoreOutlined/>}
                                                aria-controls={`panel-${step.id}-content`}
                                                id={`panel-${step.id}-header`}
                                            >
                                                {(() => {
                                                    const StepIcon = getStepIcon(step);
                                                    return <StepIcon sx={{mr: 1}}/>;
                                                })()}
                                                <Typography>
                                                    {generateComponentTitle(step)} {elements.length === 0 &&
                                                    <Chip sx={{ml: 1}}
                                                          label={'Keine vorbefüllbaren Felder vorhanden'}
                                                          size="small"
                                                          variant={'outlined'}/>}
                                                </Typography>
                                            </AccordionSummary>

                                            <AccordionDetails>
                                                <Grid
                                                    container
                                                    spacing={2}
                                                >
                                                    {elements.length === 0 &&
                                                        <Grid>
                                                            <AlertComponent color={'info'}
                                                                            title={'Dieser Abschnitt enthält keine vorbefüllbaren Felder'}
                                                                            sx={{mt: 1, mb: 0}}>
                                                                Es können ausschließlich die folgenden Felder vorbefüllt
                                                                werden: {
                                                                prefillableElementTypes
                                                                    .map(getElementNameForType)
                                                                    .join(', ')
                                                            }.
                                                                Technische Felder und deaktivierte Felder können nicht
                                                                vorbefüllt werden.
                                                            </AlertComponent>
                                                        </Grid>
                                                    }

                                                    { /* TODO
                                                        elements.map((element) => (
                                                            <ViewDispatcherComponent
                                                                rootElement={form}
                                                                key={element.id}
                                                                allElements={allElements}
                                                                element={element}
                                                                isBusy={false}
                                                                isDeriving={false}
                                                                mode="viewer"
                                                                authoredElementValues={elementData}
                                                                derivedData={derivedData}
                                                                onAuthoredElementValuesChange={setElementData}
                                                                onDerivedDataChange={setDerivedData}
                                                                onElementBlur={undefined}
                                                                disableVisibility={true}
                                                                scrollContainerRef={undefined}
                                                                derivationTriggerIdQueue={[]}
                                                                onDerive={() => {

                                                                }}
                                                            />
                                                        ))
                                                    */}
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
                        disabled={linkTooLong || !hasPrefillableElements}
                        startIcon={<ContentPasteOutlinedIcon/>}
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
                        disabled={linkTooLong || !hasPrefillableElements}
                        startIcon={<QrCode2OutlinedIcon/>}
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
                        disabled={!hasPrefillableElements}
                        startIcon={<ImportExportOutlinedIcon/>}
                        sx={{
                            ml: 'auto',
                        }}
                    >
                        Eingaben exportieren
                    </Button>

                    <Button
                        onClick={handleImport}
                        disabled={!hasPrefillableElements}
                        startIcon={<CloudUploadOutlinedIcon/>}
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
