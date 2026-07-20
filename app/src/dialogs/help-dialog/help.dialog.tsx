import {Box, Button, Dialog, DialogActions, DialogContent, DialogContentText, Grid, Link, Stack, Typography} from '@mui/material';
import React, {useEffect, useLayoutEffect, useRef, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type HelpDialogProps} from './help-dialog-props';
import ExpandMoreOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/ExpandMore';
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
                color="text.secondary"
                sx={{mb: 1.5}}
            >
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
    const technicalSupportDepartmentId = application?.technicalSupportDepartmentId ?? null;
    const legalSupportDepartmentId = application?.legalSupportDepartmentId ?? null;

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

    const mailSubjectTitle = application.publicTitle ?? 'Online-Formular';
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
                        <li>Nutzen Sie die Schaltflächen <b>„Weiter“</b> und <b>„Zum vorherigen Abschnitt“</b>, zum
                            Navigieren durch die Schritte und um Ihre Eingaben zu prüfen.
                        </li>
                        <li>Klicken Sie abschließend auf <b>„Verbindlich einreichen“</b>.</li>
                        <li>Falls Fehler vorliegen, werden diese rot markiert. Bitte korrigieren Sie sie und versuchen
                            Sie erneut, das Formular abzusenden.
                        </li>
                        <li>Nach erfolgreicher Übermittlung werden Ihre Angaben direkt an die zuständige Behörde
                            weitergeleitet. Sollte eine Online-Bezahlung notwendig sein, so führen Sie diese bitte
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
                <>
                    <Typography>
                        Das Formular unterstützt Zeichen aus dem <b>Unicode-Zeichensatz</b>, die in
                        der <b>UTF-8-Kodierung</b> gespeichert und übertragen werden. Sie können folgende Zeichen
                        verwenden:
                    </Typography>
                    <ul>
                        <li>
                            <b>Buchstaben:</b>
                            A-Z, a-z, Umlaute (ä, ö, ü), ß, sowie diakritische Zeichen (á, à, â, é, è, ê, ô, etc.).
                        </li>
                        <li>
                            <b>Zahlen:</b>
                            0-9
                        </li>
                        <li>
                            <b>Sonderzeichen:</b>
                            , . : ( ) ? ! @ „ ‚ § € / + - _
                        </li>
                    </ul>
                    <Typography>
                        Andere Sonderzeichen, Steuerzeichen oder nicht-druckbare Zeichen sind nicht erlaubt.
                        Darüber hinaus besteht die Möglichkeit, dass diese Optionen je nach Feld und Formular weiter
                        eingeschränkt sind. Bitte beachten Sie entsprechende Hinweise im Formular.
                    </Typography>
                </>
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
                            10 MB pro Datei, insgesamt max. 100 MB
                        </li>
                    </ul>
                    <Typography>
                        Es besteht die Möglichkeit, dass diese Optionen je nach Feld und Formular variieren. Bitte
                        beachten Sie die Hinweise im Formular.
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
                    Die Bearbeitung erfolgt durch die im Formular genannten zuständigen Parteien. Die Online-Plattform
                    dient nur der digitalen Übermittlung der Formulardaten.
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
                        <li>Wenn Sie Ihren Browser-Cache leeren oder den Inkognito-Modus nutzen, gehen die gespeicherten
                            Daten verloren.
                        </li>
                        <li>Wie lange die Daten in Ihrem Browser gespeichert bleiben, hängt von vielen Faktoren wie den
                            spezifischen Benutzer-Einstellungen ab. Wir haben hierauf keinen Einfluss.
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
                        Nachträgliche Änderungen sind online nicht mehr möglich, sobald das Formular übermittelt wurde.
                        Bitte kontaktieren Sie in solchen Fällen schnellstmöglich die im Formular genannten
                        Ansprechpartner, welche Ihnen
                        möglicherweise weiterhelfen können.
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
                    application != null &&
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
                            color={'text.primary'}
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
