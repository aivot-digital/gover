import React, {ReactNode, useMemo} from 'react';
import Box from '@mui/material/Box';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FadingPaper} from '../fading-paper/fading-paper';
import {Preamble} from '../preamble/preamble';
import {showDialog} from '../../slices/app-slice';
import {isStringNullOrEmpty, stringOrUndefined} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {ExpandableList} from '../expandable-list/expandable-list';
import {MarkdownContent} from '../markdown-content/markdown-content';
import {isRootElement} from '../../models/elements/form-layout-element';
import {useViewDispatcherContext} from '../view-dispatcher/view-dispatcher.context';
import {ViewDispatcherComponent} from '../view-dispatcher/view-dispatcher.component';
import {Grid} from '@mui/material';
import {hasDerivableAspects} from '../../utils/has-derivable-aspects';
import {useFormDepartmentAddressSections} from '../form-department-addresses/form-department-addresses';

function cleanDocuments(documents: Array<string> | undefined | null) {
    if (documents) {
        return documents.filter(document => document.trim() !== '');
    } else {
        return [];
    }
}

export function GeneralInformationComponentView(props: BaseViewProps<IntroductionStepElement, boolean>) {
    const dispatch = useAppDispatch();

    const {
        element,
        value,
        setValue,
        errors,
        isDeriving,
    } = props;

    const {
        expectedCosts,
        supportingDocuments: supportingDocumentsRaw,
        documentsToAttach: documentsToAttachRaw,
        eligiblePersons,
        expiring: expiringRaw,
    } = element;

    const {
        rootElement,
    } = useViewDispatcherContext();

    const formElement = isRootElement(rootElement) ? rootElement : null;
    const departmentSections = useFormDepartmentAddressSections(formElement);

    const initialDisplayCount = 4;

    const supportingDocuments = useMemo(() => cleanDocuments(supportingDocumentsRaw), [supportingDocumentsRaw]);
    const documentsToAttach = useMemo(() => cleanDocuments(documentsToAttachRaw), [documentsToAttachRaw]);
    const preambleText = stringOrUndefined(element.teaserText);
    const initiativeLogoLink = stringOrUndefined(element.initiativeLogoLink);
    const initiativeName = stringOrUndefined(element.initiativeName);
    const expiring = stringOrUndefined(expiringRaw);

    const pass = useMemo(() => {
        return isDeriving && hasDerivableAspects(element);
    }, [isDeriving, element]);

    const sections: ReactNode[] = useMemo(() => {
        const sections: ReactNode[] = [...departmentSections];

        if (eligiblePersons != null &&
            eligiblePersons.length > 0) {
            sections.push(
                <ExpandableList
                    key="eligible"
                    title="Antragsberechtigte"
                    items={eligiblePersons}
                    initialVisible={initialDisplayCount}
                    singularLabel="Person"
                    pluralLabel="Personen"
                    listId="eligible-persons-list"
                    renderItem={renderEligiblePerson}
                />,
            );
        }

        if (supportingDocuments.length > 0) {
            sections.push(
                <ExpandableList
                    key="supporting"
                    title="Relevante Dokumente"
                    items={supportingDocuments}
                    initialVisible={initialDisplayCount}
                    singularLabel="Dokument"
                    pluralLabel="Dokumente"
                    listId="supporting-documents-list"
                    renderItem={renderSupportingDocument}
                />,
            );
        }

        if (documentsToAttach.length > 0) {
            sections.push(
                <ExpandableList
                    key="attachments"
                    title="Einzureichende Dokumente"
                    items={documentsToAttach}
                    initialVisible={initialDisplayCount}
                    singularLabel="Dokument"
                    pluralLabel="Dokumente"
                    listId="documents-to-attach-list"
                    renderItem={renderDocumentToAttach}
                />,
            );
        }

        if (expiring != null) {
            sections.push(
                <Box key="deadline">
                    <Typography
                        component={'h3'}
                        variant="h5"
                    >
                        Fristen
                    </Typography>
                    <Typography
                        component="pre"
                        variant="body2"
                        sx={{mt: 1}}
                    >
                        {expiring}
                    </Typography>
                </Box>,
            );
        }

        if (expectedCosts != null && !isStringNullOrEmpty(expectedCosts)) {
            sections.push(
                <Box key="costs">
                    <Typography
                        component={'h3'}
                        variant="h5"
                    >
                        Gebühren dieses Antrages
                    </Typography>

                    <MarkdownContent
                        markdown={expectedCosts}
                        className={'content-without-margin-on-childs'}
                        sx={{
                            mt: 1,
                            typography: 'body2',
                        }}
                    />
                </Box>,
            );
        }

        return sections;
    }, [departmentSections, eligiblePersons, supportingDocuments, documentsToAttach, expiring, expectedCosts]);

    return (
        <>
            {
                preambleText &&
                <Preamble
                    text={preambleText}
                    logoLink={initiativeLogoLink}
                    logoAlt={initiativeName}
                />
            }

            {
                sections.length > 0 &&
                <FadingPaper>
                    <Box
                        sx={{
                            columnCount: {xs: 1, md: 2},
                            columnGap: 7,
                        }}
                    >
                        {
                            sections.map((section, index) => (
                                <Box
                                    key={index}
                                    sx={{
                                        breakInside: 'avoid',
                                        mb: 3,
                                        display: 'inline-block',
                                        width: '100%',
                                    }}
                                >
                                    {section}
                                </Box>
                            ))
                        }
                    </Box>
                </FadingPaper>
            }

            {
                element.children != null &&
                <Grid
                    container
                    spacing={2}
                    sx={{
                        mt: 4,
                    }}
                >
                    {
                        element
                            .children
                            .map((child, index) => (
                                <ViewDispatcherComponent
                                    {...props}
                                    key={index}
                                    element={child}
                                    isDeriving={isDeriving || pass}
                                />
                            ))
                    }
                </Grid>
            }

            <Typography
                component="h4"
                variant="h5"
                sx={{
                    mt: 4,
                }}
            >
                Hinweise zum Datenschutz
            </Typography>

            {
                element.privacyText != null &&
                <Box
                    sx={{
                        maxWidth: '600px',
                        mt: 1,
                    }}
                >
                    <FormattedTextWithDialogTags
                        text={element.privacyText}
                    />
                </Box>
            }

            <Box id={element.id}>
                <CheckboxFieldComponent
                    label="Ich habe die Hinweise zum Datenschutz zur Kenntnis genommen."
                    value={value ?? undefined}
                    onChange={(checked) => {
                        setValue(checked);
                    }}
                    required={true}
                    error={errors != null ? errors[0] ?? undefined : undefined}
                    disabled={props.isBusy}
                />
            </Box>

            <Typography
                variant="caption"
                sx={{
                    mt: 4,
                }}
                color={'text.secondary'}
            >
                Alle mit Stern (*) gekennzeichneten Felder sind Pflichtfelder.
            </Typography>
        </>
    );
}

const renderEligiblePerson = (person: string, index: number) => (
    <ListItem
        disableGutters
        key={String(index) + person}
    >
        <ListItemIcon sx={{minWidth: '34px'}}>
            <PersonOutlineOutlinedIcon color="primary"/>
        </ListItemIcon>
        <ListItemText>{person}</ListItemText>
    </ListItem>
);

const renderSupportingDocument = (document: string, index: number) => (
    <ListItem
        disableGutters
        key={String(index) + document}
    >
        <ListItemIcon sx={{minWidth: '34px'}}>
            <DescriptionOutlinedIcon color="primary"/>
        </ListItemIcon>
        <ListItemText>{document}</ListItemText>
    </ListItem>
);

const renderDocumentToAttach = (document: string, index: number) => (
    <ListItem
        disableGutters
        key={String(index) + document}
    >
        <ListItemIcon sx={{minWidth: '34px'}}>
            <UploadFileOutlinedIcon color="primary"/>
        </ListItemIcon>
        <ListItemText>{document}</ListItemText>
    </ListItem>
);

interface FormattedTextWithDialogTagsProps {
    text: string;
}

function FormattedTextWithDialogTags(props: FormattedTextWithDialogTagsProps) {
    const {text} = props;
    const dispatch = useAppDispatch();
    let formattedText = text;

    for (const meta of [AccessibilityDialogId, PrivacyDialogId, ImprintDialogId, HelpDialogId]) {
        const tag = meta.toLowerCase();
        const pattern = new RegExp(`\\{${tag}\\}([\\s\\S]*?)\\{\\/${tag}\\}`, 'gi');

        formattedText = formattedText.replace(pattern, '[$1](#dialog:' + tag + ')');
    }

    return (
        <MarkdownContent
            markdown={formattedText}
            sx={{
                typography: 'body2',
                '& a': {
                    cursor: 'pointer',
                },
            }}
            components={{
                a: ({href, children, node: _node, ...anchorProps}) => {
                    if (href?.startsWith('#dialog:')) {
                        const dialog = href.replace('#dialog:', '');

                        return (
                            <a
                                href={href}
                                {...anchorProps}
                                onClick={(event) => {
                                    event.preventDefault();
                                    dispatch(showDialog(dialog));
                                }}
                            >
                                {children}
                            </a>
                        );
                    }

                    const isExternalLink = href != null && /^(https?:)?\/\//.test(href);

                    return (
                        <a
                            href={href}
                            {...anchorProps}
                            target={isExternalLink ? '_blank' : undefined}
                            rel={isExternalLink ? 'noopener noreferrer' : undefined}
                        >
                            {children}
                        </a>
                    );
                },
            }}
        />
    );
}
