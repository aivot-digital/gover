import {Box, Button, Dialog, DialogActions, DialogContent, DialogContentText, Grid, Typography} from '@mui/material';
import {BoxLink} from '../../components/box-link/box-link';
import React, {useEffect, useState} from 'react';
import {styled} from '@mui/material/styles';
import MuiAccordionDetails from '@mui/material/AccordionDetails';
import MuiAccordion, {type AccordionProps} from '@mui/material/Accordion';
import MuiAccordionSummary, {type AccordionSummaryProps} from '@mui/material/AccordionSummary';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../modules/departments/models/department';
import {useSelector} from 'react-redux';
import {type HelpDialogProps} from './help-dialog-props';
import {selectLoadedForm} from '../../slices/app-slice';
import ExpandMoreOutlinedIcon from '@mui/icons-material/ExpandMoreOutlined';
import {useApi} from "../../hooks/use-api";
import {DepartmentsApiService} from '../../modules/departments/departments-api-service';

export const HelpDialogId = 'help';

export function HelpDialog(props: HelpDialogProps): JSX.Element {
    const api = useApi();
    const application = useSelector(selectLoadedForm);
    const [technicalDepartment, setTechnicalDepartment] = useState<Department>();
    const [specialDepartment, setSpecialDepartment] = useState<Department>();

    useEffect(() => {
        const ac = new AbortController();

        if (
            application?.technicalSupportDepartmentId != null &&
            (technicalDepartment == null || technicalDepartment.id !== application.technicalSupportDepartmentId)
        ) {
            new DepartmentsApiService(api)
                .retrievePublic(application.technicalSupportDepartmentId)
                .then(setTechnicalDepartment);
        }

        if (
            application?.legalSupportDepartmentId != null &&
            (specialDepartment == null || specialDepartment.id !== application.legalSupportDepartmentId)
        ) {
            new DepartmentsApiService(api)
                .retrievePublic(application.legalSupportDepartmentId)
                .then(setSpecialDepartment);
        }

        return () => ac.abort();
    }, [application, technicalDepartment, specialDepartment]);

    const FAQs = [
        {
            question: "Wie funktioniert die Online-Antragstellung?",
            answer: (
                <>
                    <Typography>
                        Um einen Antrag online zu stellen, folgen Sie diesen Schritten:
                    </Typography>
                    <ul>
                        <li>Füllen Sie alle mit Stern (*) gekennzeichneten Pflichtfelder aus.</li>
                        <li>Nutzen Sie die Schaltflächen <b>„Weiter“</b> und <b>„Zum vorherigen Abschnitt“</b>, zum Navigieren durch die Schritte und um Ihre Eingaben zu prüfen.</li>
                        <li>Klicken Sie abschließend auf <b>„Antrag verbindlich einreichen“</b>.</li>
                        <li>Falls Fehler vorliegen, werden diese rot markiert. Bitte korrigieren Sie sie und versuchen Sie erneut, das Formular abzusenden.</li>
                        <li>Nach erfolgreicher Übermittlung wird Ihr Antrag direkt an die zuständige Behörde weitergeleitet. Sollte eine Online-Bezahlung notwendig sein, so führen Sie diese bitte durch, indem Sie den im Antrag angezeigten Anweisungen folgen.</li>
                        <li>Laden Sie den Antrag als PDF herunter oder lassen Sie ihn sich per E-Mail zusenden.</li>
                    </ul>
                </>
            ),
        },
        {
            question: "Welche Zeichen kann ich im Formular verwenden?",
            answer: (
                <>
                    <Typography>
                        Das Formular unterstützt Zeichen aus dem <b>Unicode-Zeichensatz</b>, die in der <b>UTF-8-Kodierung</b> gespeichert und übertragen werden. Sie können folgende Zeichen verwenden:
                    </Typography>
                    <ul>
                        <li><b>Buchstaben:</b> A-Z, a-z, Umlaute (ä, ö, ü), ß, sowie diakritische Zeichen (á, à, â, é, è, ê, ô, etc.).</li>
                        <li><b>Zahlen:</b> 0-9</li>
                        <li><b>Sonderzeichen:</b> , . : ( ) ? ! @ „ ‚ § € / + - _</li>
                    </ul>
                    <Typography>
                        Andere Sonderzeichen, Steuerzeichen oder nicht-druckbare Zeichen sind nicht erlaubt.
                        Darüber hinaus besteht die Möglichkeit, dass diese Optionen je nach Feld und Antrag weiter eingeschränkt sind. Bitte beachten Sie entsprechende Hinweise im Formular.
                    </Typography>
                </>
            ),
        },
        {
            question: "Welche Dateiformate kann ich hochladen?",
            answer: (
                <>
                    <Typography>Grundsätzlich können folgende Dateiformate hochgeladen werden:</Typography>
                    <ul>
                        <li><b>Dokumente:</b> pdf, doc, docx, odt, fodt, odf</li>
                        <li><b>Tabellen:</b> xls, xlsx, ods, fods</li>
                        <li><b>Präsentationen:</b> ppt, pptx, odp, fodp</li>
                        <li><b>Bilder & Grafiken:</b> png, jpg, jpeg, odg, fodg</li>
                        <li><b>Maximale Dateigröße:</b> 10 MB pro Datei, insgesamt max. 100 MB</li>
                    </ul>
                    <Typography>
                        Es besteht die Möglichkeit, dass diese Optionen je nach Feld und Antrag variieren. Bitte beachten Sie die Hinweise im Formular.
                    </Typography>
                </>
            ),
        },
        {
            question: "Benötige ich zusätzliche Software für meinen Antrag?",
            answer: (
                <>
                    <Typography>
                        Sie benötigen zum Ausfüllen eines Formulars grundsätzlich keine zusätzliche Software abseits ihres Web-Browsers.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Wenn Sie optional ein PDF-Dokument herunterladen und ansehen möchten, so benötigen Sie möglicherweise eine spezielle Software. Eine bekannte Lösung ist der PDF Reader der Firma Adobe (<a
                        rel="noreferrer"
                        href={'https://get.adobe.com/de/reader/'}
                        target={'_blank'}
                    >https://get.adobe.com/de/reader/</a>), es gibt aber auch das
                        kostenfreie Alternativen wie z. B. Foxit PDF Reader (<a
                        rel="noreferrer"
                        href={'https://www.foxitsoftware.com/de/pdf-reader/'}
                        target={'_blank'}
                    >https://www.foxitsoftware.com/de/pdf-reader/</a>).
                    </Typography>
                </>
            ),
        },
        {
            question: "Wer ist für meinen Antrag zuständig?",
            answer: (
                <Typography>
                    Die Bearbeitung erfolgt durch die im Formular genannten zuständigen Parteien. Die Online-Plattform dient nur der digitalen Übermittlung der Antragsdaten.
                </Typography>
            ),
        },
        {
            question: "Werden meine Daten sicher übertragen?",
            answer: (
                <Typography>
                    Ihre Daten werden über eine <b>verschlüsselte HTTPS- bzw. TLS-Verbindung</b> übertragen und sind somit auf dem Transportweg vor unbefugtem Zugriff geschützt.
                </Typography>
            ),
        },
        {
            question: "Kann ich meinen Antrag zwischenspeichern und später weiterbearbeiten?",
            answer: (
                <>
                    <Typography>
                        Ihre Eingaben werden automatisch im <b>lokalen Speicher (Local Storage)</b> Ihres Browsers zwischengespeichert. Wenn Sie das Formular erneut aufrufen, können Sie entscheiden, ob Sie Ihre Eingaben fortsetzen oder einen neuen Antrag beginnen möchten.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        <b>Wichtige Hinweise:</b>
                    </Typography>
                    <ul>
                        <li>Die Zwischenspeicherung erfolgt lokal auf Ihrem Endgerät. Eine Kopie auf einem Server existiert nicht.</li>
                        <li>Wenn Sie Ihren Browser-Cache leeren oder den Inkognito-Modus nutzen, gehen die gespeicherten Daten verloren.</li>
                        <li>Wie lange die Daten in Ihrem Browser gespeichert bleiben, hängt von vielen Faktoren wie den spezifischen Benutzer-Einstellungen ab. Wir haben hierauf keinen Einfluss.</li>
                        <li>Falls Sie ein öffentliches oder gemeinsam genutztes Gerät verwenden, löschen Sie Ihre Daten am besten nach der Nutzung, um Missbrauch zu vermeiden.</li>
                    </ul>
                </>
            ),
        },
        {
            question: "Kann ich meinen Antrag nachträglich ändern oder zurückziehen?",
            answer: (
                <>
                    <Typography>
                        Nachträgliche Änderungen sind online nicht mehr möglich, sobald Ihr Antrag übermittelt wurde. Bitte kontaktieren Sie in solchen Fällen schnellstmöglich die im Antrag genannten Ansprechpartner, welche Ihnen möglicherweise weiterhelfen können.
                    </Typography>
                </>
            ),
        },
    ];

    return (
        <Dialog
            open={props.open}
            maxWidth="md"
            scroll="paper"
            onClose={props.onHide}
            fullWidth={true}
        >
            <DialogTitleWithClose
                onClose={props.onHide}
            >
                Hilfe für diesen Antrag
            </DialogTitleWithClose>
            <DialogContent tabIndex={0}>
                {
                    application != null &&
                    specialDepartment != null &&
                    technicalDepartment != null &&
                    <Grid
                        container
                        spacing={4}
                        sx={{
                            mt: -3.6,
                            mb: 4,
                        }}
                    >
                        <Grid
                            item
                            xs={12}
                            md={6}
                        >
                            <BoxLink
                                link={`mailto:${specialDepartment.specialSupportAddress}?subject=Fachlicher Support: ${application.title}`}
                                text={'Fachlicher Support\nUnterstützung zum Inhalt\nund Ausfüllen des Antrages'}
                            />
                        </Grid>
                        <Grid
                            item
                            xs={12}
                            md={6}
                        >
                            <BoxLink
                                link={`mailto:${technicalDepartment.technicalSupportAddress}?subject=Technischer Support: ${application.title}`}
                                text={'Technischer Support\nUnterstützung bei technischen Problemen und Fehlern'}
                            />
                        </Grid>
                    </Grid>
                }

                <DialogContentText component="div">
                    <Box sx={{mb: 4}}>
                        <Typography
                            variant={'h6'}
                            color={'text.primary'}
                        >Häufig gestellte Fragen (FAQ)</Typography>
                        Für eine schnelle Hilfe haben wir Ihnen nachfolgend die häufigsten Fragen zu diesem Formular
                        zusammengestellt. Sollten Sie auf Ihre Frage keine Antwort finden, so nutzen Sie
                        gerne die oben gezeigten Möglichkeiten, um Kontakt mit uns aufzunehmen. Vielen Dank!
                    </Box>
                    {FAQs.map((faq, index) => (
                        <Accordion key={index}>
                            <AccordionSummary expandIcon={<ExpandMoreOutlinedIcon />}>
                                <Typography>{faq.question}</Typography>
                            </AccordionSummary>
                            <AccordionDetails>{faq.answer}</AccordionDetails>
                        </Accordion>
                    ))}
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button
                    onClick={props.onHide}
                    variant="contained"
                >
                    Hilfe schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}

const Accordion = styled((props: AccordionProps) => (
    <MuiAccordion
        disableGutters
        elevation={0}
        square
        {...props}
    />
))(({theme}) => ({
    'border': `1px solid ${theme.palette.primary.dark}`,
    '&:not(:last-child)': {
        borderBottom: 0,
    },
    '&:before': {
        display: 'none',
    },
}));

const AccordionSummary = styled((props: AccordionSummaryProps) => (
    <MuiAccordionSummary
        {...props}
    />
))(({theme}) => ({
    'flexDirection': 'row-reverse',
    'transition': '200ms all ease-in-out',
    '&.Mui-expanded': {
        backgroundColor: theme.palette.primary.main,
    },
    '&.Mui-expanded .MuiTypography-body1': {
        color: '#fff',
    },
    '& .MuiAccordionSummary-expandIconWrapper.Mui-expanded': {
        transform: 'rotate(90deg)',
        color: theme.palette.secondary.main,
    },
    '& .MuiAccordionSummary-content': {
        marginLeft: theme.spacing(1),
    },
}));

const AccordionDetails = styled(MuiAccordionDetails)(({theme}) => ({
    padding: theme.spacing(2),
    paddingBottom: theme.spacing(4),
    borderTop: '1px solid rgba(0, 0, 0, .125)',
}));
