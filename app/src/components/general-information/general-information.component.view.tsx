import React, {useEffect, useMemo, useRef, useState} from 'react';
import Box from '@mui/material/Box';
import ListItem from '@mui/material/ListItem';
import ListItemIcon from '@mui/material/ListItemIcon';
import ListItemText from '@mui/material/ListItemText';
import Typography from '@mui/material/Typography';
import useTheme from '@mui/material/styles/useTheme';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FadingPaper} from '../fading-paper/fading-paper';
import {Preamble} from '../preamble/preamble';
import {type Department} from '../../modules/departments/models/department';
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
import {useApi} from '../../hooks/use-api';
import {DepartmentsApiService} from '../../modules/departments/departments-api-service';
import {AccessibilityDialogId} from '../../dialogs/accessibility-dialog/accessibility-dialog';
import {PrivacyDialogId} from '../../dialogs/privacy-dialog/privacy-dialog';
import {ImprintDialogId} from '../../dialogs/imprint-dialog/imprint-dialog';
import {HelpDialogId} from '../../dialogs/help-dialog/help.dialog';
import {ExpandableList} from '../expandable-list/expandable-list';
import {IdentityButtonGroup} from '../../modules/identity/components/identity-button-group/identity-button-group';

function cleanDocuments(documents: Array<string> | undefined | null) {
    if (documents) {
        return documents.filter(document => document.trim() !== '');
    } else {
        return [];
    }
}

export function GeneralInformationComponentView(props: BaseViewProps<IntroductionStepElement, boolean>): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const theme = useTheme();

    const {
        rootElement,
        element,
        value,
        setValue,
        errors,
        elementData,
        onElementDataChange,
    } = props;

    const {} = element;

    const application = useAppSelector(selectLoadedForm);
    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    const [responsibleDepartment, setResponsibleDepartment] = useState<Department>();
    const [managingDepartment, setManagingDepartment] = useState<Department>();

    const initialDisplayCount = 4;

    const supportingDocuments = cleanDocuments(element.supportingDocuments);
    const documentsToAttach = cleanDocuments(element.documentsToAttach);

    useEffect(() => {
        if (application != null) {
            if (application.responsibleDepartmentId != null) {
                if (responsibleDepartment == null || responsibleDepartment.id !== application.responsibleDepartmentId) {
                    new DepartmentsApiService(api)
                        .retrievePublic(application.responsibleDepartmentId)
                        .then(setResponsibleDepartment)
                        .catch((err) => {
                            console.error(err);
                            dispatch(showErrorSnackbar('Fehler beim Laden der zuständigen Stelle'));
                        });
                }
            } else {
                setResponsibleDepartment(undefined);
            }

            if (application.managingDepartmentId != null) {
                if (managingDepartment == null || managingDepartment.id !== application.managingDepartmentId) {
                    new DepartmentsApiService(api)
                        .retrievePublic(application.managingDepartmentId)
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
    }, [props.element, application?.responsibleDepartmentId, application?.managingDepartmentId]);

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

    const sections: JSX.Element[] = [];

    if (responsibleDepartment != null) {
        sections.push(
            <Box key="responsible">
                <Typography
                    component={'h3'}
                    variant="h5"
                    color="primary"
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
                    color="primary"
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
        !isStringNullOrEmpty(application?.root.expiring)) {
        sections.push(
            <Box key="deadline">
                <Typography
                    component={'h3'}
                    variant="h5"
                    color="primary"
                >
                    Antragsfristen
                </Typography>
                <Typography
                    component="pre"
                    variant="body2"
                    sx={{mt: 1}}
                >
                    {application.root.expiring}
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
                    color="primary"
                >
                    Gebühren dieses Antrages
                </Typography>

                <Typography
                    component={'div'}
                    variant="body2"
                    className={'content-without-margin-on-childs'}
                    sx={{mt: 1}}
                    dangerouslySetInnerHTML={{__html: props.element.expectedCosts ?? ''}}
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
                    !isStringNullOrEmpty(application?.root.expiring) ||
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
                elementData={elementData}
                onElementDataChange={ed => onElementDataChange(ed, [])}
                form={application!}
            />

            <Typography
                component="h4"
                variant="h5"
                color="primary"
                sx={{
                    mt: 4,
                }}
            >
                Hinweise zum Datenschutz
            </Typography>

            {
                application?.root.privacyText != null &&
                <Box
                    sx={{
                        maxWidth: '600px',
                        mt: 1,
                    }}
                >
                    <FormattedTextWithDialogTags
                        text={application.root.privacyText}
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

    const typographyRef = useRef<HTMLSpanElement>(null);

    const dispatch = useAppDispatch();

    const formattedText = useMemo(() => {
        let result: string = text;

        // Iterate over all possible dialog tags and replace them with the corresponding HTML anchor tags
        for (const meta of [AccessibilityDialogId, PrivacyDialogId, ImprintDialogId, HelpDialogId]) {
            const tag = meta.toLowerCase();

            result = result
                .replace(`{${tag}}`, `<a data-dialog="${tag}" style="cursor: pointer;">`)
                .replace(`{/${tag}}`, '</a>');
        }

        return result;
    }, [text]);

    useEffect(() => {
        const typo = typographyRef.current;

        if (typo == null) {
            return;
        }

        typo.querySelectorAll('[data-dialog]').forEach((element) => {
            element.addEventListener('click', (event) => {
                const target = event.target as HTMLElement;
                const dialog = target.getAttribute('data-dialog');

                if (dialog != null) {
                    dispatch(showDialog(dialog));
                }
            });
        });
    }, [typographyRef.current]);

    return (
        <Typography
            ref={typographyRef}
            variant="body2"
            dangerouslySetInnerHTML={{
                __html: formattedText,
            }}
        />
    );
}