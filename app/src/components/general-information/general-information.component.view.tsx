import React, {useEffect, useState} from 'react';
import Box from '@mui/material/Box';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import {useTheme} from '@mui/material/styles';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FadingPaper} from '../fading-paper/fading-paper';
import {Preamble} from '../preamble/preamble';
import {selectLoadedForm, showDialog} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import {AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {ExpandableList} from '../expandable-list/expandable-list';
import {IdentityButtonGroup} from '../../modules/identity/components/identity-button-group/identity-button-group';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';
import {VDepartmentShadowedEntity} from '../../modules/departments/entities/v-department-shadowed-entity';
import {MarkdownContent} from '../markdown-content/markdown-content';

function cleanDocuments(documents: Array<string> | undefined | null) {
    if (documents) {
        return documents.filter(document => document.trim() !== '');
    } else {
        return [];
    }
}

export function GeneralInformationComponentView(props: BaseViewProps<IntroductionStepElement, boolean>) {
    const dispatch = useAppDispatch();
    const theme = useTheme();

    const {
        rootElement,
        element,
        value,
        setValue,
        errors,
        authoredElementValues,
        derivedData,
        onAuthoredElementValuesChange,
    } = props;

    const {} = element;

    const application = useAppSelector(selectLoadedForm);
    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    const [responsibleDepartment, setResponsibleDepartment] = useState<VDepartmentShadowedEntity>();
    const [managingDepartment, setManagingDepartment] = useState<VDepartmentShadowedEntity>();

    const initialDisplayCount = 4;

    const supportingDocuments = cleanDocuments(element.supportingDocuments);
    const documentsToAttach = cleanDocuments(element.documentsToAttach);

    useEffect(() => {
        if (application != null) {
            if (application.version.responsibleDepartmentId != null) {
                if (responsibleDepartment == null || responsibleDepartment.id !== application.version.responsibleDepartmentId) {
                    new DepartmentApiService()
                        .retrievePublic(application.version.responsibleDepartmentId)
                        .then(setResponsibleDepartment)
                        .catch((err) => {
                            console.error(err);
                            dispatch(showErrorSnackbar('Fehler beim Laden der zuständigen Stelle'));
                        });
                }
            } else {
                setResponsibleDepartment(undefined);
            }

            if (application.version.managingDepartmentId != null) {
                if (managingDepartment == null || managingDepartment.id !== application.version.managingDepartmentId) {
                    new DepartmentApiService()
                        .retrievePublic(application.version.managingDepartmentId)
                        .then(setManagingDepartment)
                        .catch((err) => {
                            console.error(err);
                            dispatch(showErrorSnackbar('Fehler beim Laden der bewirtschaftenden Stelle'));
                        });
                }
            } else {
                setManagingDepartment(undefined);
            }
        }
    }, [props.element, application?.version.responsibleDepartmentId, application?.version.managingDepartmentId]);

    const renderEligiblePerson = (person: string, index: number) => (
        <ListItem
            disableGutters
            key={String(index) + person}
        >
            <ListItemIcon sx={{minWidth: '34px'}}>
                <PersonOutlineOutlinedIcon sx={{color: theme.palette.primary.main}} />
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
                <DescriptionOutlinedIcon sx={{color: theme.palette.primary.main}} />
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
                <UploadFileOutlinedIcon sx={{color: theme.palette.primary.main}} />
            </ListItemIcon>
            <ListItemText>{document}</ListItemText>
        </ListItem>
    );

    const sections: React.ReactNode[] = [];

    if (responsibleDepartment != null) {
        sections.push(
            <Box key="responsible">
                <Typography
                    component={'h3'}
                    variant="h5"
                >
                    Zuständige Stelle
                </Typography>
                <Typography
                    component={'pre'}
                    variant="body2"
                    sx={{mt: 1}}
                >
                    {[
                        isStringNotNullOrEmpty(providerName) ? providerName : null,
                        responsibleDepartment.name,
                        responsibleDepartment.address,
                    ].filter(Boolean).join('\n')}
                </Typography>
            </Box>,
        );
    }

    if (managingDepartment != null) {
        sections.push(
            <Box key="managing">
                <Typography
                    component={'h3'}
                    variant="h5"
                >
                    Bewirtschaftende Stelle
                </Typography>
                <Typography
                    component={'pre'}
                    variant="body2"
                >
                    {[
                        isStringNotNullOrEmpty(providerName) ? providerName : null,
                        managingDepartment.name,
                        managingDepartment.address,
                    ].filter(Boolean).join('\n')}
                </Typography>
            </Box>,
        );
    }

    if (props.element.eligiblePersons != null &&
        props.element.eligiblePersons.length > 0) {
        sections.push(
            <ExpandableList
                key="eligible"
                title="Antragsberechtigte"
                items={props.element.eligiblePersons}
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

    if (application != null &&
        !isStringNullOrEmpty(application?.version.rootElement.expiring)) {
        sections.push(
            <Box key="deadline">
                <Typography
                    component={'h3'}
                    variant="h5"
                >
                    Antragsfristen
                </Typography>
                <Typography
                    component="pre"
                    variant="body2"
                    sx={{mt: 1}}
                >
                    {application.version.rootElement.expiring}
                </Typography>
            </Box>,
        );
    }

    if (props.element.expectedCosts != null &&
        !isStringNullOrEmpty(props.element.expectedCosts)) {
        sections.push(
            <Box key="costs">
                <Typography
                    component={'h3'}
                    variant="h5"
                >
                    Gebühren dieses Antrages
                </Typography>

                <MarkdownContent
                    markdown={props.element.expectedCosts}
                    className={'content-without-margin-on-childs'}
                    sx={{
                        mt: 1,
                        typography: 'body2',
                    }}
                />
            </Box>,
        );
    }

    return (
        <>
            {
                element.teaserText != null &&
                element.initiativeLogoLink != null &&
                element.initiativeName != null &&
                isStringNotNullOrEmpty(element.teaserText) &&
                isStringNotNullOrEmpty(element.initiativeLogoLink) &&
                isStringNotNullOrEmpty(element.initiativeName) &&
                <Preamble
                    text={element.teaserText}
                    logoLink={element.initiativeLogoLink}
                    logoAlt={element.initiativeName}
                />
            }

            {
                (
                    responsibleDepartment != null ||
                    managingDepartment != null ||
                    (props.element.eligiblePersons ?? []).length > 0 ||
                    (props.element.supportingDocuments ?? []).length > 0 ||
                    (props.element.documentsToAttach ?? []).length > 0 ||
                    !isStringNullOrEmpty(application?.version.rootElement.expiring) ||
                    !isStringNullOrEmpty(props.element.expectedCosts)
                ) &&
                <FadingPaper>
                    <Box
                        sx={{
                            columnCount: {xs: 1, md: 2},
                            columnGap: 7,
                        }}
                    >
                        {sections.map((section, index) => (
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
                        ))}
                    </Box>
                </FadingPaper>
            }

            <IdentityButtonGroup
                rootElement={rootElement}
                isBusy={props.isBusy}
                isDeriving={props.isDeriving}
                authoredElementValues={authoredElementValues}
                derivedData={derivedData}
                onAuthoredElementValuesChange={ed => onAuthoredElementValuesChange(ed, [])}
                form={application?.form!}
                version={application?.version!}
            />

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
                application?.version.rootElement.privacyText != null &&
                <Box
                    sx={{
                        maxWidth: '600px',
                        mt: 1,
                    }}
                >
                    <FormattedTextWithDialogTags
                        text={application.version.rootElement.privacyText}
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

        formattedText = formattedText.replace(pattern, '[$1](dialog:' + tag + ')');
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
                a: ({href, children, ...anchorProps}) => {
                    if (href?.startsWith('dialog:')) {
                        const dialog = href.replace('dialog:', '');

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
