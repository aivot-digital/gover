import {Box, Button, Dialog, DialogActions, DialogContent, Divider, Grid, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectLoadedForm} from '../../slices/app-slice';
import {useMemo, useState} from 'react';
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
                    .filter(e => relevantElementTypes.includes(e.type));
                return {
                    step: s,
                    elements: stepElements,
                };
            })
            .filter(({elements}) => elements.length > 0);
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


    return (
        <Dialog
            open={props.open}
            onClose={props.onClose}
            maxWidth="lg"
        >
            <DialogTitleWithClose onClose={props.onClose}>
                Vorbefülltes Formular
            </DialogTitleWithClose>

            <DialogContent>
                <Typography variant="body2">
                    Hier können Sie einen Link erzeugen, der ein Formular mit den hier angegebenen Werten vorbefüllt.
                    Dies ist besonders nützlich, wenn Sie ein Formular z.B. an einen Personenkreis mit vorher bekannten Angaben weitergeben möchten.
                    Bitte beachten Sie, dass nicht alle Elemente unterstützt werden.
                </Typography>

                <Typography
                    variant="body2"
                    sx={{
                        mt: 1,
                    }}
                >
                    <strong>Wichtig:&nbsp;</strong>
                    Der Link kann sehr lang werden und in einigen Browsern Probleme verursachen.
                    Die maximale Länge für Links beträgt deshalb {MAX_LINK_LENGTH} Zeichen.
                </Typography>

                <Divider sx={{my: 2}} />

                {
                    allElementsPerStep
                        .map(({step, elements}) => (
                            <Box
                                key={step.id}
                                sx={{
                                    mb: 4,
                                }}
                            >
                                <Typography
                                    variant="h6"
                                    component="div"
                                >
                                    {generateComponentTitle(step)}
                                </Typography>
                                <Grid
                                    container
                                    spacing={2}
                                >
                                    {
                                        elements.map((element) => (
                                            <Grid
                                                key={element.id}
                                                item
                                                xs={12}
                                                md={6}
                                                xl={4}
                                            >
                                                <ViewDispatcherComponent
                                                    allElements={[]}
                                                    element={{
                                                        ...element,
                                                        disabled: false,
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
                                                />
                                            </Grid>
                                        ))
                                    }
                                </Grid>
                            </Box>
                        ))
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
                        onClick={handleCopyLink}
                        disabled={linkTooLong}
                    >
                        Link kopieren
                    </Button>
                    {
                        linkTooLong &&
                        <Hint
                            summary="Link zu lang"
                            detailsTitle="Link zu lang"
                            details="Der generierte Link ist zu lang, um ihn zu kopieren oder als QR-Code herunterzuladen. Bitte reduzieren Sie die Anzahl der vorbefüllten Felder oder die Länge der Werte."
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
                    >
                        QR-Code herunterladen
                    </Button>

                    {
                        linkTooLong &&
                        <Hint
                            summary="Link zu lang"
                            detailsTitle="Link zu lang"
                            details="Der generierte Link ist zu lang, um ihn zu kopieren oder als QR-Code herunterzuladen. Bitte reduzieren Sie die Anzahl der vorbefüllten Felder oder die Länge der Werte."
                            isError={true}
                            sx={{
                                ml: 0,
                                mr: 2,
                            }}
                        />
                    }

                    <Button
                        variant="contained"
                        onClick={props.onClose}
                        sx={{
                            ml: 'auto',
                        }}
                    >
                        Schließen
                    </Button>
                </Box>
            </DialogActions>
        </Dialog>
    );
}