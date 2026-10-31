import {Box, Button, Dialog, DialogActions, DialogContent, DialogContentText, Grid, Link, Stack, Typography} from '@mui/material';
import React, {useEffect, useLayoutEffect, useRef, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type HelpDialogProps} from './help-dialog-props';
import ExpandMoreOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/KeyboardArrowDown';
import {Accordion, AccordionDetails, AccordionGroup, AccordionSummary} from '../../components/accordion/accordion';
import {PublicDepartmentResponseDTO} from '../../modules/departments/entities/v-department-shadowed-entity';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';
import {MarkdownContent} from '../../components/markdown-content/markdown-content';
import {formatPhoneNumberForDisplay, normalizePhoneNumberForTelLink} from '../../utils/phone-number-utils';
import {alpha} from '@mui/material/styles';

export const HelpDialogId = 'help';

const CollapsedSupportInfoMaxHeight = 144;

function ExpandableSupportInfo(props: {
    markdown: string;
}) {
    const {
        markdown,
    } = props;
    const contentRef = useRef<HTMLDivElement | null>(null);
    const [expanded, setExpanded] = useState(false);
    const [canToggle, setCanToggle] = useState(false);

    useEffect(() => {
        setExpanded(false);
    }, [markdown]);

    useLayoutEffect(() => {
        const contentElement = contentRef.current;
        if (contentElement == null) {
            return undefined;
        }

        const updateCanToggle = () => {
            setCanToggle(contentElement.scrollHeight > CollapsedSupportInfoMaxHeight + 1);
        };

        updateCanToggle();

        const resizeObserver = new ResizeObserver(updateCanToggle);
        resizeObserver.observe(contentElement);

        return () => resizeObserver.disconnect();
    }, [markdown]);

    return (
        <Box>
            <Box
                ref={contentRef}
                sx={{
                    position: 'relative',
                    maxHeight: canToggle && !expanded ? CollapsedSupportInfoMaxHeight : 'none',
                    overflow: 'hidden',
                }}
            >
                <MarkdownContent markdown={markdown}/>
                {
                    canToggle &&
                    !expanded &&
                    <Box
                        aria-hidden="true"
                        sx={(theme) => ({
                            position: 'absolute',
                            right: 0,
                            bottom: 0,
                            left: 0,
                            height: 40,
                            background: `linear-gradient(to bottom, ${alpha(theme.palette.background.paper, 0)}, ${theme.palette.background.paper})`,
                        })}
                    />
                }
            </Box>
            {
                canToggle &&
                <Button
                    size="small"
                    sx={{
                        mt: 1,
                        ml: -1,
                    }}
                    aria-expanded={expanded}
                    onClick={() => setExpanded((prev) => !prev)}
                >
                    {expanded ? 'Weniger anzeigen' : 'Mehr anzeigen'}
                </Button>
            }
        </Box>
    );
}

function SupportContactBlock(props: {
    title: string;
    description: string;
    email?: string | null;
    phone?: string | null;
    info?: string | null;
    mailSubject: string;
}) {
    const {
        title,
        description,
        email,
        phone,
        info,
        mailSubject,
    } = props;
    const normalizedPhoneNumber = normalizePhoneNumberForTelLink(phone);
    const phoneNumberLabel = formatPhoneNumberForDisplay(phone);

    return (
        <Box
            sx={{
                border: '1px solid',
                borderColor: 'divider',
                borderRadius: 1,
                p: 2,
                height: '100%',
            }}
        >
            <Typography
                variant="h6"
                sx={{mb: 0.5}}
            >
                {title}
            </Typography>
            <Typography
                sx={{
                    color: "text.secondary",
                    mb: 1.5
                }}>
                {description}
            </Typography>
            <Stack spacing={1}>
                {
                    email != null &&
                    <Typography>
                        <b>E-Mail:</b>{' '}
                        <Link href={`mailto:${email}?subject=${encodeURIComponent(mailSubject)}`}>
                            {email}
                        </Link>
                    </Typography>
                }
                {
                    phone != null &&
                    phone.trim().length > 0 &&
                    <Typography>
                        <b>Telefon:</b>{' '}
                        {
                            normalizedPhoneNumber != null ?
                                <Link href={`tel:${normalizedPhoneNumber}`}>
                                    {phoneNumberLabel}
                                </Link> :
                                phoneNumberLabel
                        }
                    </Typography>
                }
                {
                    info != null &&
                    info.trim().length > 0 &&
                    <Box
                        sx={{
                            '& > *:first-child': {
                                mt: 0,
                            },
                            '& > *:last-child': {
                                mb: 0,
                            },
                        }}
                    >
                        <ExpandableSupportInfo markdown={info}/>
                    </Box>
                }
            </Stack>
        </Box>
    );
}

export function HelpDialog(props: HelpDialogProps) {
    const application = props.form;
    const [technicalDepartment, setTechnicalDepartment] = useState<PublicDepartmentResponseDTO>();
    const [specialDepartment, setSpecialDepartment] = useState<PublicDepartmentResponseDTO>();
    const technicalSupportDepartmentId = props.technicalSupportDepartmentId ??
        props.version?.technicalSupportDepartmentId ??
        null;
    const legalSupportDepartmentId = props.legalSupportDepartmentId ??
        props.version?.legalSupportDepartmentId ??
        null;

    useEffect(() => {
        if (technicalSupportDepartmentId == null) {
            setTechnicalDepartment(undefined);
            return;
        }

        let isCancelled = false;

        // Clear stale department data immediately when the configured source is removed or changed.
        setTechnicalDepartment(undefined);
        new DepartmentApiService()
            .retrievePublic(technicalSupportDepartmentId)
            .then((department) => {
                if (!isCancelled) {
                    setTechnicalDepartment(department);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [technicalSupportDepartmentId]);

    useEffect(() => {
        if (legalSupportDepartmentId == null) {
            setSpecialDepartment(undefined);
            return;
        }

        let isCancelled = false;

        // Clear stale department data immediately when the configured source is removed or changed.
        setSpecialDepartment(undefined);
        new DepartmentApiService()
            .retrievePublic(legalSupportDepartmentId)
            .then((department) => {
                if (!isCancelled) {
                    setSpecialDepartment(department);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [legalSupportDepartmentId]);

    const mailSubjectTitle = props.version?.publicTitle ?? application?.publicTitle ?? 'Online-Formular';
    const hasSpecialContact = specialDepartment != null;
    const hasTechnicalContact = technicalDepartment != null;

    const FAQs = [
        {
            question: 'Wie funktioniert das Online-Formular?',
            answer: (
                <>
                    <Typography>
                        Um ein Formular online auszufüllen und zu übermitteln, folgen Sie diesen Schritten:
                    </Typography>
                    <ul>
                        <li>Füllen Sie alle mit Stern (*) gekennzeichneten Pflichtfelder aus.</li>
                        <li>Nutzen Sie die Schaltflächen <b>„Weiter“</b> und <b>„Zum vorherigen Abschnitt“</b>, um
                            durch die Abschnitte zu navigieren und Ihre Angaben zu prüfen.
                        </li>
                        <li>Klicken Sie abschließend auf <b>„Angaben übermitteln“</b>.</li>
                        <li>Falls Fehler vorliegen, werden diese rot markiert. Bitte korrigieren Sie sie und versuchen
                            Sie erneut, das Formular abzusenden.
                        </li>
                        <li>Nach erfolgreicher Übermittlung werden Ihre Angaben weiterverarbeitet.
                            Informationen zur Verarbeitung Ihrer Daten finden Sie in der Datenschutzerklärung.
                            Sollte eine Online-Bezahlung notwendig sein, so führen Sie diese bitte
                            durch, indem Sie den im Formular angezeigten
                            Anweisungen folgen.
                        </li>
                        <li>Auf der Bestätigungsseite erhalten Sie ggf. weitere Hinweise zum weiteren Ablauf.</li>
                    </ul>
                </>
            ),
        },
        {
            question: 'Welche Zeichen kann ich im Formular verwenden?',
            answer: (
                <Typography>
                    Welche Zeichen zulässig sind, richtet sich nach dem jeweiligen Feld. Beachten Sie dessen
                    Hinweise und die angezeigten Fehlermeldungen.
                </Typography>
            ),
        },
        {
            question: 'Welche Dateiformate kann ich hochladen?',
            answer: (
                <>
                    <Typography>Grundsätzlich können folgende Dateiformate hochgeladen werden:</Typography>
                    <ul>
                        <li>
                            <b>Dokumente:</b>
                            pdf, doc, docx, odt, fodt, odf
                        </li>
                        <li>
                            <b>Tabellen:</b>
                            xls, xlsx, ods, fods
                        </li>
                        <li>
                            <b>Präsentationen:</b>
                            ppt, pptx, odp, fodp
                        </li>
                        <li>
                            <b>Bilder & Grafiken:</b>
                            png, jpg, jpeg, odg, fodg
                        </li>
                        <li>
                            <b>Maximale Dateigröße:</b>
                            In der Regel 100 MB pro Datei und insgesamt 500 MB.
                        </li>
                    </ul>
                    <Typography>
                        Je nach Formular und Anlagenfeld können andere Dateiformate und Größenbegrenzungen gelten.
                        Beachten Sie die Hinweise am jeweiligen Anlagenfeld.
                    </Typography>
                </>
            ),
        },
        {
            question: 'Benötige ich zusätzliche Software für dieses Formular?',
            answer: (
                <>
                    <Typography>
                        Sie benötigen zum Ausfüllen eines Formulars grundsätzlich keine zusätzliche Software abseits
                        ihres Web-Browsers.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        Wenn Sie optional ein PDF-Dokument herunterladen und ansehen möchten, so benötigen Sie
                        möglicherweise eine spezielle Software. Eine bekannte Lösung ist der PDF Reader der Firma Adobe
                        (
                        <a
                            rel="noreferrer"
                            href={'https://get.adobe.com/de/reader/'}
                            target={'_blank'}
                        >https://get.adobe.com/de/reader/
                        </a>
                        ), es gibt aber auch das
                        kostenfreie Alternativen wie z. B. Foxit PDF Reader (
                        <a
                            rel="noreferrer"
                            href={'https://www.foxitsoftware.com/de/pdf-reader/'}
                            target={'_blank'}
                        >https://www.foxitsoftware.com/de/pdf-reader/
                        </a>
                        ).
                    </Typography>
                </>
            ),
        },
        {
            question: 'Wer ist für dieses Formular zuständig?',
            answer: (
                <Typography>
                    Die verantwortliche Stelle finden Sie im Formular. Wie Ihre Angaben nach der Übermittlung
                    weiterverarbeitet werden, erfahren Sie in der Datenschutzerklärung.
                </Typography>
            ),
        },
        {
            question: 'Werden meine Daten sicher übertragen?',
            answer: (
                <Typography>
                    Ihre Daten werden über eine <b>verschlüsselte HTTPS- bzw. TLS-Verbindung</b> übertragen und sind
                    somit auf dem Transportweg vor unbefugtem Zugriff geschützt.
                </Typography>
            ),
        },
        {
            question: 'Kann ich dieses Formular zwischenspeichern und später weiterbearbeiten?',
            answer: (
                <>
                    <Typography>
                        Ihre Eingaben werden automatisch im <b>lokalen Speicher (Local Storage)</b> Ihres Browsers
                        zwischengespeichert. Wenn Sie das Formular erneut aufrufen, können Sie entscheiden, ob Sie Ihre
                        Eingaben fortsetzen oder
                        das Formular neu beginnen möchten.
                    </Typography>
                    <Typography sx={{mt: 2}}>
                        <b>Wichtige Hinweise:</b>
                    </Typography>
                    <ul>
                        <li>Die Zwischenspeicherung erfolgt lokal auf Ihrem Endgerät. Eine Kopie auf einem Server
                            existiert nicht.
                        </li>
                        <li>Wenn Sie die Website-Daten für Prosuna löschen, wird auch Ihr lokal gespeicherter Entwurf
                            entfernt. In privaten Browserfenstern bleibt er in der Regel nur bis zum Ende der privaten
                            Sitzung erhalten.
                        </li>
                        <li>Wie lange die Daten in Ihrem Browser gespeichert bleiben, hängt von vielen Faktoren wie den
                            persönlichen Einstellungen ab. Wir haben hierauf keinen Einfluss.
                        </li>
                        <li>Falls Sie ein öffentliches oder gemeinsam genutztes Gerät verwenden, löschen Sie Ihre Daten
                            am besten nach der Nutzung, um Missbrauch zu vermeiden.
                        </li>
                    </ul>
                </>
            ),
        },
        {
            question: 'Kann ich meine Angaben nachträglich ändern oder zurückziehen?',
            answer: (
                <>
                    <Typography>
                        Die bereits übermittelten Angaben können Sie in diesem Formular nicht nachträglich ändern.
                        Wenden Sie sich für Berichtigungen an die verantwortliche Stelle. Falls weitere Angaben
                        benötigt werden, können Sie dazu eine gesonderte Aufforderung erhalten.
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
                Hilfe zu diesem Formular
            </DialogTitleWithClose>
            <DialogContent tabIndex={0}>
                {
                    (hasSpecialContact || hasTechnicalContact) &&
                    <Grid
                        container
                        spacing={2}
                        sx={{
                            mb: 3,
                        }}
                    >
                        <Grid
                            size={{
                                xs: 12,
                                md: 6,
                            }}
                        >
                            {
                                hasSpecialContact &&
                                <SupportContactBlock
                                    title="Fachliche Unterstützung"
                                    description="Unterstützung zum Inhalt und Ausfüllen des Formulars."
                                    email={specialDepartment?.specialSupportEmail}
                                    phone={specialDepartment?.specialSupportPhone}
                                    info={specialDepartment?.specialSupportInfo}
                                    mailSubject={`Fachliche Hilfe: ${mailSubjectTitle}`}
                                />
                            }
                        </Grid>
                        <Grid
                            size={{
                                xs: 12,
                                md: 6,
                            }}
                        >
                            {
                                hasTechnicalContact &&
                                <SupportContactBlock
                                    title="Technische Unterstützung"
                                    description="Unterstützung bei technischen Problemen und Fehlern."
                                    email={technicalDepartment?.technicalSupportEmail}
                                    phone={technicalDepartment?.technicalSupportPhone}
                                    info={technicalDepartment?.technicalSupportInfo}
                                    mailSubject={`Technische Hilfe: ${mailSubjectTitle}`}
                                />
                            }
                        </Grid>
                    </Grid>
                }

                <DialogContentText component="div">
                    <Box sx={{mb: 4}}>
                        <Typography
                            variant={'h5'}
                            sx={{
                                color: 'text.primary'
                            }}
                        >
                            Häufig gestellte Fragen (FAQ)
                        </Typography>
                        <Typography sx={{mt: 1}}>
                            Für eine schnelle Hilfe haben wir Ihnen nachfolgend die häufigsten Fragen zu diesem Formular
                            zusammengestellt. Sollten Sie auf Ihre Frage keine Antwort finden, so nutzen Sie
                            gerne die oben gezeigten Möglichkeiten, um Kontakt mit uns aufzunehmen. Vielen Dank!
                        </Typography>
                    </Box>
                    <AccordionGroup>
                        {FAQs.map((faq, index) => (
                            <Accordion key={index}>
                                <AccordionSummary expandIcon={<ExpandMoreOutlinedIcon/>}>
                                    <Typography>{faq.question}</Typography>
                                </AccordionSummary>
                                <AccordionDetails>{faq.answer}</AccordionDetails>
                            </Accordion>
                        ))}
                    </AccordionGroup>
                </DialogContentText>
            </DialogContent>
            <DialogActions>
                <Box/>
                <Button
                    onClick={props.onHide}
                >
                    Hilfe schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
