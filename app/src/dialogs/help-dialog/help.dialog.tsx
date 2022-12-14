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
                            Unterst??tzung zum Inhalt <br/>und Ausf??llen des Antrages
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
                            Unterst??tzung bei technischen Problemen und Fehlern
                        </BoxLinkComponentView>
                    </Grid>
                </Grid>

                <DialogContentText component="div">
                    <Box sx={{mb: 4}}>
                        <Typography
                            variant={'h6'}
                            sx={{color: '#16191F'}}
                        >H??ufig gestellte Fragen (FAQ)</Typography>
                        F??r eine schnelle Hilfe haben wir Ihnen nachfolgend die h??ufigsten Fragen zu diesem Antrag
                        zusammengestellt. Sollten Sie auf Ihre Frage keine Antwort finden, so nutzen Sie
                        gerne die oben gezeigten M??glichkeiten, um Kontakt mit uns aufzunehmen. Vielen Dank!
                    </Box>
                    <Accordion sx={{mt: 4}}>
                        <AccordionSummary
                            expandIcon={<FontAwesomeIcon icon={faChevronDown}/>}
                            aria-controls="panel1a-content"
                            id="panel1a-header"
                        >
                            <Typography>Wie l??uft die Antragstellung ab?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Bitte f??llen Sie zun??chst alle mit * gekennzeichneten Pflichtfelder aus. ??ber die
                                Schaltfl??chen "Weiter" und "Zur??ck" werden die von Ihnen eingegebenen Daten im
                                jeweiligen Formular-Register gepr??ft. Zum Abschluss des Formulars erfolgt eine
                                Gesamtpr??fung ??ber die Schaltfl??che ???Antrag abschicken???. Sind alle Eingaben korrekt,
                                wird Ihr Antrag direkt an die zust??ndige Beh??rde medienbruchfrei weitergeleitet.
                                Beinhaltet das Antragsformular noch fehlerhafte Eingaben, werden die Felder rot umrahmt.
                                Zus??tzlich k??nnen die Fehlermeldungen im Bereich ???Meldungen??? angezeigt werden. Nach
                                erfolgreicher Korrektur ist erneut die jeweilige Schaltfl??che zu bet??tigen. Das
                                ausgef??llte Antragsformular k??nnen Sie sich ??ber die Schaltfl??che ???PDF erstellen??? lokal
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
                            <Typography>Welche Eingaben sind im Formular zul??ssig?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Bitte beachten Sie, dass nur die folgenden Zeichen im Formular zul??ssig sind: A-Z, a-z,
                                Umlaute, ??, 0-9, ???????????????????????????? sowie die Sonderzeichen , . : ( ) ? ! @ ??? ??? ?? ??? / + - _
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
                            <Typography>Welche Dateiformate k??nnen im Formular hochgeladen werden?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Folgende Dateiformate k??nnen hochgeladen werden: docx, xlsx, pptx, rtf, pdf, txt, csv,
                                bmp, gif, jfif, jpeg, jpg, png, tiff, prn, msg, ppsx, eps, svg, uxf.
                            </Typography>
                            <Typography sx={{mt: 2}}>
                                Pro Datei ist eine Maximalgr????e von 10 MB zul??ssig. Insgesamt k??nnen Dateien bis zu
                                einer Gr????e von 100 MB hochgeladen werden.
                            </Typography>
                        </AccordionDetails>
                    </Accordion>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<FontAwesomeIcon icon={faChevronDown}/>}
                            aria-controls="panel4a-content"
                            id="panel4a-header"
                        >
                            <Typography>Welche zus??tzliche Software ben??tige ich?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Zum ??ffnen von PDF-Dokumenten ben??tigen Sie eine spezielle Software, die kostenfrei zur
                                Verf??gung steht. Die bekannteste ist der PDF Reader der Firma Adobe (<a
                                rel="noreferrer"
                                href={'https://get.adobe.com/de/reader/'}
                                target={'_blank'}
                            >https://get.adobe.com/de/reader/</a>), es gibt aber auch das
                                alternative kostenfreie Produkt (Foxit PDF Reader, <a
                                rel="noreferrer"
                                href={'https://www.foxitsoftware.com/de/pdf-reader/'}
                                target={'_blank'}
                            >https://www.foxitsoftware.com/de/pdf-reader/</a>). Nachdem Sie die
                                Software installiert haben, k??nnen Sie das PDF-Dokument ??ffnen und betrachten.
                            </Typography>
                        </AccordionDetails>
                    </Accordion>
                    <Accordion>
                        <AccordionSummary
                            expandIcon={<FontAwesomeIcon icon={faChevronDown}/>}
                            aria-controls="panel5a-content"
                            id="panel5a-header"
                        >
                            <Typography>Wer ist f??r mich zust??ndig?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Die Zust??ndigkeit verbleibt bei der jeweiligen Beh??rde vor Ort. Beim
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
                            <Typography>Sind meine ??bermittelten/eingegebenen Daten sicher?</Typography>
                        </AccordionSummary>
                        <AccordionDetails>
                            <Typography>
                                Die verschl??sselte elektronische Verbindung erfolgt ??ber das Internet-Protokoll HTTPS.
                                Eine sichere ??bermittlung der Daten ist daher grunds??tzlich gew??hrleistet.
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
                    Hilfe schlie??en
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
