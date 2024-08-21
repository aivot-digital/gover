import {Box, Button, Dialog, DialogActions, DialogContent, DialogContentText, Grid, Typography} from '@mui/material';
import {BoxLink} from '../../components/box-link/box-link';
import React, {useEffect, useState} from 'react';
import {styled} from '@mui/material/styles';
import MuiAccordionDetails from '@mui/material/AccordionDetails';
import MuiAccordion, {type AccordionProps} from '@mui/material/Accordion';
import MuiAccordionSummary, {type AccordionSummaryProps} from '@mui/material/AccordionSummary';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../models/entities/department';
import {useSelector} from 'react-redux';
import {type HelpDialogProps} from './help-dialog-props';
import {selectLoadedForm} from '../../slices/app-slice';
import ExpandMoreOutlinedIcon from '@mui/icons-material/ExpandMoreOutlined';
import {useApi} from "../../hooks/use-api";
import {useDepartmentsApi} from "../../hooks/use-departments-api";

export const HelpDialogId = 'help';

export function HelpDialog(props: HelpDialogProps): JSX.Element {
    const api = useApi();
    const application = useSelector(selectLoadedForm);
    const [technicalDepartment, setTechnicalDepartment] = useState<Department>();
    const [specialDepartment, setSpecialDepartment] = useState<Department>();

    useEffect(() => {
        if (
            application?.technicalSupportDepartmentId != null &&
            (technicalDepartment == null || technicalDepartment.id !== application.technicalSupportDepartmentId)
        ) {
            useDepartmentsApi(api)
                .retrieve(application.technicalSupportDepartmentId)
                .then(setTechnicalDepartment);
        }

        if (
            application?.legalSupportDepartmentId != null &&
            (specialDepartment == null || specialDepartment.id !== application.legalSupportDepartmentId)
        ) {
            useDepartmentsApi(api)
                .retrieve(application.legalSupportDepartmentId)
                .then(setSpecialDepartment);
        }
    }, [application, technicalDepartment, specialDepartment]);

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
                            sx={{color: '#16191F'}}
                        >Häufig gestellte Fragen (FAQ)</Typography>
                        Für eine schnelle Hilfe haben wir Ihnen nachfolgend die häufigsten Fragen zu diesem Formular
                        zusammengestellt. Sollten Sie auf Ihre Frage keine Antwort finden, so nutzen Sie
                        gerne die oben gezeigten Möglichkeiten, um Kontakt mit uns aufzunehmen. Vielen Dank!
                    </Box>
                    <Accordion sx={{mt: 4}}>
                        <AccordionSummary
                            expandIcon={<ExpandMoreOutlinedIcon/>}
                            aria-controls="panel1a-content"
                            id="panel1a-header"
                        >
                            <Typography>Wie läuft die Antragstellung ab?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Bitte füllen Sie zunächst alle mit * gekennzeichneten Pflichtfelder aus. Über die
                                Schaltflächen "Weiter" und "Zum vorherigen Schritt" werden die von Ihnen eingegebenen Daten im
                                jeweiligen Formular-Register geprüft. Zum Abschluss des Formulars erfolgt eine
                                Gesamtprüfung über die Schaltfläche „Antrag verbindlich einreichen“. Sind alle Eingaben korrekt,
                                wird Ihr Antrag direkt an die zuständige Behörde medienbruchfrei weitergeleitet.
                                Beinhaltet das Antragsformular noch fehlerhafte Eingaben, werden die Felder rot umrahmt. Nach
                                erfolgreicher Korrektur ist erneut die jeweilige Schaltfläche zu betätigen. Das
                                ausgefüllte Antragsformular können Sie sich über die Schaltfläche „PDF erstellen“ lokal
                                abspeichern oder per E-Mail zusenden lassen.
                            </Typography>
                        </AccordionDetails>
                    </Accordion>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<ExpandMoreOutlinedIcon/>}
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
                            expandIcon={<ExpandMoreOutlinedIcon/>}
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
                            expandIcon={<ExpandMoreOutlinedIcon/>}
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
                            expandIcon={<ExpandMoreOutlinedIcon/>}
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
                            expandIcon={<ExpandMoreOutlinedIcon/>}
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
