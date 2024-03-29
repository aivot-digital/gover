import {Box, Button, Dialog, DialogActions, DialogContent, DialogContentText, Grid, Typography} from '@mui/material';
import {BoxLinkComponentView} from '../../components/box-link/box-link.component.view';
import React, {useEffect, useState} from 'react';
import {styled} from '@mui/material/styles';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faChevronDown} from '@fortawesome/pro-light-svg-icons';
import MuiAccordionDetails from '@mui/material/AccordionDetails';
import MuiAccordion, {AccordionProps} from '@mui/material/Accordion';
import MuiAccordionSummary, {AccordionSummaryProps,} from '@mui/material/AccordionSummary';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {Department} from '../../models/department';
import {useSelector} from 'react-redux';
import {DepartmentsService} from '../../services/departments.service';
import {HelpDialogProps} from './help-dialog-props';
import {Localization} from '../../locale/localization';
import strings from './help-dialog-strings.json';
import {selectLoadedApplication} from '../../slices/app-slice';

const _ = Localization(strings);

// TODO: Localize

export function HelpDialog(props: HelpDialogProps) {
    const application = useSelector(selectLoadedApplication);
    const [technicalDepartment, setTechnicalDepartment] = useState<Department>();
    const [specialDepartment, setSpecialDepartment] = useState<Department>();

    useEffect(() => {
        if (application != null) {
            if (technicalDepartment == null && application.root.technicalSupport != null) {
                DepartmentsService.retrieve(application.root.technicalSupport)
                    .then(department => setTechnicalDepartment(department));
            }
            if (specialDepartment == null && application.root.legalSupport != null) {
                DepartmentsService.retrieve(application.root.legalSupport)
                    .then(department => setSpecialDepartment(department));
            }
        }
    }, [application, technicalDepartment, specialDepartment]);

    return (
        <Dialog
            open={props.open}
            maxWidth="md"
            scroll="paper"
            onBackdropClick={props.onHide}
            fullWidth={true}
        >
            <DialogTitleWithClose
                id="help-dialog-title"
                onClose={props.onHide}
                closeTooltip={_.close}
            >
                {_.title}
            </DialogTitleWithClose>
            <DialogContent>

                <Grid
                    container
                    spacing={4}
                    sx={{mt: -3.6, mb: 4}}
                >
                    <Grid
                        item
                        xs={6}
                    >
                        <BoxLinkComponentView
                            link={`mailto:${specialDepartment?.specialSupportAddress}?subject=Fachlicher Support: ${application?.root.title}`}
                        >
                            <span>Fachlicher Support</span><br/>
                            Unterstützung zum Inhalt <br/>und Ausfüllen des Antrages
                        </BoxLinkComponentView>
                    </Grid>
                    <Grid
                        item
                        xs={6}
                    >
                        <BoxLinkComponentView
                            link={`mailto:${technicalDepartment?.technicalSupportAddress}?subject=Technischer Support: ${application?.root.title}`}
                        >
                            <span>Technischer Support</span><br/>
                            Unterstützung bei technischen Problemen und Fehlern
                        </BoxLinkComponentView>
                    </Grid>
                </Grid>

                <DialogContentText component="div">
                    <Box sx={{mb: 4}}>
                        <Typography
                            variant={'h6'}
                            sx={{color: '#16191F'}}
                        >Häufig gestellte Fragen (FAQ)</Typography>
                        Für eine schnelle Hilfe haben wir Ihnen nachfolgend die häufigsten Fragen zu diesem Antrag
                        zusammengestellt. Sollten Sie auf Ihre Frage keine Antwort finden, so nutzen Sie
                        gerne die oben gezeigten Möglichkeiten, um Kontakt mit uns aufzunehmen. Vielen Dank!
                    </Box>
                    <Accordion sx={{mt: 4}}>
                        <AccordionSummary
                            expandIcon={<FontAwesomeIcon icon={faChevronDown}/>}
                            aria-controls="panel1a-content"
                            id="panel1a-header"
                        >
                            <Typography>Wie läuft die Antragstellung ab?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Bitte füllen Sie zunächst alle mit * gekennzeichneten Pflichtfelder aus. Über die
                                Schaltflächen "Weiter" und "Zurück" werden die von Ihnen eingegebenen Daten im
                                jeweiligen Formular-Register geprüft. Zum Abschluss des Formulars erfolgt eine
                                Gesamtprüfung über die Schaltfläche „Antrag abschicken“. Sind alle Eingaben korrekt,
                                wird Ihr Antrag direkt an die zuständige Behörde medienbruchfrei weitergeleitet.
                                Beinhaltet das Antragsformular noch fehlerhafte Eingaben, werden die Felder rot umrahmt.
                                Zusätzlich können die Fehlermeldungen im Bereich „Meldungen“ angezeigt werden. Nach
                                erfolgreicher Korrektur ist erneut die jeweilige Schaltfläche zu betätigen. Das
                                ausgefüllte Antragsformular können Sie sich über die Schaltfläche „PDF erstellen“ lokal
                                abspeichern oder per E-Mail zusenden lassen.
                            </Typography>
                        </AccordionDetails>
                    </Accordion>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<FontAwesomeIcon icon={faChevronDown}/>}
                            aria-controls="panel2a-content"
                            id="panel2a-header"
                        >
                            <Typography>Welche Eingaben sind im Formular zulässig?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Bitte beachten Sie, dass nur die folgenden Zeichen im Formular zulässig sind: A-Z, a-z,
                                Umlaute, ß, 0-9, áàâÁÀÂéèêÉÈÊôÔ sowie die Sonderzeichen , . : ( ) ? ! @ „ ‚ § € / + - _
                                .
                            </Typography>
                        </AccordionDetails>
                    </Accordion>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<FontAwesomeIcon icon={faChevronDown}/>}
                            aria-controls="panel3a-content"
                            id="panel3a-header"
                        >
                            <Typography>Welche Dateiformate können im Formular hochgeladen werden?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Folgende Dateiformate können hochgeladen werden: docx, xlsx, pptx, rtf, pdf, txt, csv,
                                bmp, gif, jfif, jpeg, jpg, png, tiff, prn, msg, ppsx, eps, svg, uxf.
                            </Typography>
                            <Typography sx={{mt: 2}}>
                                Pro Datei ist eine Maximalgröße von 10 MB zulässig. Insgesamt können Dateien bis zu
                                einer Größe von 100 MB hochgeladen werden.
                            </Typography>
                        </AccordionDetails>
                    </Accordion>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<FontAwesomeIcon icon={faChevronDown}/>}
                            aria-controls="panel4a-content"
                            id="panel4a-header"
                        >
                            <Typography>Welche zusätzliche Software benötige ich?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Zum Öffnen von PDF-Dokumenten benötigen Sie eine spezielle Software, die kostenfrei zur
                                Verfügung steht. Die bekannteste ist der PDF Reader der Firma Adobe (<a
                                rel="noreferrer"
                                href={'https://get.adobe.com/de/reader/'}
                                target={'_blank'}
                            >https://get.adobe.com/de/reader/</a>), es gibt aber auch das
                                alternative kostenfreie Produkt (Foxit PDF Reader, <a
                                rel="noreferrer"
                                href={'https://www.foxitsoftware.com/de/pdf-reader/'}
                                target={'_blank'}
                            >https://www.foxitsoftware.com/de/pdf-reader/</a>). Nachdem Sie die
                                Software installiert haben, können Sie das PDF-Dokument öffnen und betrachten.
                            </Typography>
                        </AccordionDetails>
                    </Accordion>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<FontAwesomeIcon icon={faChevronDown}/>}
                            aria-controls="panel5a-content"
                            id="panel5a-header"
                        >
                            <Typography>Wer ist für mich zuständig?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Die Zuständigkeit verbleibt bei der jeweiligen Behörde vor Ort. Beim
                                Online-Antragsmanagement handelt es sich lediglich um ein weiteres Kommunikationsmittel.
                            </Typography>
                        </AccordionDetails>
                    </Accordion>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<FontAwesomeIcon icon={faChevronDown}/>}
                            aria-controls="panel6a-content"
                            id="panel6a-header"
                        >
                            <Typography>Sind meine übermittelten/eingegebenen Daten sicher?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Die verschlüsselte elektronische Verbindung erfolgt über das Internet-Protokoll HTTPS.
                                Eine sichere Übermittlung der Daten ist daher grundsätzlich gewährleistet.
                            </Typography>
                        </AccordionDetails>
                    </Accordion>
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Button
                    size="large"
                    onClick={props.onHide}
                    sx={{
                        mr: 2,
                        mb: 2,
                    }}
                    variant="outlined"
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
        square {...props} />
))(({theme}) => ({
    border: `1px solid ${theme.palette.primary.dark}`,
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
    flexDirection: 'row-reverse',
    transition: '200ms all ease-in-out',
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
