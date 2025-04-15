import React, {useEffect, useMemo, useRef, useState} from 'react';
import {Box, ListItem, ListItemIcon, ListItemText, Typography, useTheme} from '@mui/material';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FadingPaper} from '../fading-paper/fading-paper';
import {Preamble} from '../preamble/preamble';
import {type Department} from '../../modules/departments/models/department';
import {selectCustomerInputError, selectCustomerInputValue, selectLoadedForm, showDialog, updateCustomerInput} from '../../slices/app-slice';
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
import ExpandableList from '../expandable-list/expandable-list';
import {IdentityButtonGroup} from '../../modules/identity/components/identity-button-group/identity-button-group';

export const PrivacyUserInputKey = '__privacy__';

export function GeneralInformationComponentView(props: BaseViewProps<IntroductionStepElement, void>): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const theme = useTheme();

    const application = useAppSelector(selectLoadedForm);
    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));

    const privacyValue = useAppSelector(selectCustomerInputValue(PrivacyUserInputKey));
    const privacyError = useAppSelector(selectCustomerInputError(PrivacyUserInputKey));

    const [responsibleDepartment, setResponsibleDepartment] = useState<Department>();
    const [managingDepartment, setManagingDepartment] = useState<Department>();

    const initialDisplayCount = 4;
    const [showAllEligiblePersons, setShowAllEligiblePersons] = useState(false); // Zustandsvariable, um zu kontrollieren, ob alle Personen angezeigt werden sollen
    const [showAllDocumentsToAttach, setShowAllDocumentsToAttach] = useState(false); // Zustandsvariable, um zu kontrollieren, ob alle Personen angezeigt werden sollen
    const [showAllSupportingDocuments, setShowAllSupportingDocuments] = useState(false); // Zustandsvariable, um zu kontrollieren, ob alle Personen angezeigt werden sollen

    const handleToggleShowAllEligiblePersons = () => {
        setShowAllEligiblePersons(!showAllEligiblePersons);
    };
    const handleToggleShowAllDocumentsToAttach = () => {
        setShowAllDocumentsToAttach(!showAllDocumentsToAttach);
    };
    const handleToggleShowAllSupportingDocuments = () => {
        setShowAllSupportingDocuments(!showAllSupportingDocuments);
    };

    function cleanDocuments(documents: Array<string> | undefined) {
        if (documents) {
            return documents.filter(document => document.trim() !== '');
        } else {
            return [];
        }
    }

    const supportingDocuments = cleanDocuments(props.element.supportingDocuments);
    const documentsToAttach = cleanDocuments(props.element.documentsToAttach);

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


    return (
        <>
            {
                props.element.teaserText != null &&
                isStringNotNullOrEmpty(props.element.teaserText) &&
                <Preamble
                    text={props.element.teaserText}
                    logoLink={props.element.initiativeLogoLink}
                    logoAlt={props.element.initiativeName}
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
                    {
                        responsibleDepartment != null &&
                        <Box
                            sx={{
                                mb: 3,
                                position: 'relative',
                                zIndex: 1,
                            }}
                        >
                            <Typography
                                component={'h3'}
                                variant="subtitle1"
                                color="primary"
                                sx={{textTransform: 'uppercase'}}
                            >
                                Zuständige Stelle
                            </Typography>
                            <Typography
                                component={'pre'}
                                variant="body2"
                            >
                                {[
                                    isStringNotNullOrEmpty(providerName) ? providerName : null,
                                    responsibleDepartment.name,
                                    responsibleDepartment.address,
                                ].filter(Boolean).join('\n')}
                            </Typography>
                        </Box>
                    }

                    {
                        managingDepartment != null &&
                        <Box
                            sx={{
                                mb: 3,
                                position: 'relative',
                                zIndex: 1,
                            }}
                        >
                            <Typography
                                component={'h3'}
                                variant="subtitle1"
                                color="primary"
                                sx={{textTransform: 'uppercase'}}
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
                        </Box>
                    }

                    {
                        props.element.eligiblePersons != null &&
                        props.element.eligiblePersons.length > 0 &&
                        <ExpandableList
                            title="Antragsberechtigte"
                            items={props.element.eligiblePersons}
                            initialVisible={initialDisplayCount}
                            singularLabel="Person"
                            pluralLabel="Personen"
                            listId="eligible-persons-list"
                            renderItem={renderEligiblePerson}
                        />
                    }

                    {
                        supportingDocuments.length > 0 &&
                        <ExpandableList
                            title="Relevante Dokumente"
                            items={supportingDocuments}
                            initialVisible={initialDisplayCount}
                            singularLabel="Dokument"
                            pluralLabel="Dokumente"
                            listId="supporting-documents-list"
                            renderItem={renderSupportingDocument}
                        />
                    }

                    {
                        documentsToAttach.length > 0 &&
                        <ExpandableList
                            title="Einzureichende Dokumente"
                            items={documentsToAttach}
                            initialVisible={initialDisplayCount}
                            singularLabel="Dokument"
                            pluralLabel="Dokumente"
                            listId="documents-to-attach-list"
                            renderItem={renderDocumentToAttach}
                        />
                    }

                    {
                        application != null &&
                        !isStringNullOrEmpty(application?.root.expiring) &&
                        <Box
                            sx={{
                                mb: 3,
                                position: 'relative',
                                zIndex: 1,
                            }}
                        >
                            <Typography
                                component={'h3'}
                                variant="subtitle1"
                                color="primary"
                                sx={{textTransform: 'uppercase'}}
                            >
                                Antragsfristen
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {application.root.expiring}
                            </Typography>
                        </Box>
                    }

                    {
                        props.element.expectedCosts != null &&
                        !isStringNullOrEmpty(props.element.expectedCosts) &&
                        <Box
                            sx={{
                                mb: 3,
                                position: 'relative',
                                zIndex: 1,
                            }}
                        >
                            <Typography
                                component={'h3'}
                                variant="subtitle1"
                                color="primary"
                                sx={{textTransform: 'uppercase'}}
                            >
                                Gebühren dieses Antrages
                            </Typography>

                            <Typography
                                variant="body2"
                                dangerouslySetInnerHTML={{__html: props.element.expectedCosts ?? ''}}
                            />
                        </Box>
                    }
                </FadingPaper>
            }

            <IdentityButtonGroup
                isBusy={props.isBusy}
            />

            <Typography
                component={'h4'}
                variant="h6"
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

            <Box id={PrivacyUserInputKey}>
                <CheckboxFieldComponent
                    label="Ich habe die Hinweise zum Datenschutz zur Kenntnis genommen."
                    value={privacyValue}
                    onChange={(checked) => {
                        if (application != null) {
                            dispatch(updateCustomerInput({
                                key: PrivacyUserInputKey,
                                value: checked,
                            }));
                        }
                    }}
                    required={true}
                    error={privacyError}
                    disabled={props.isBusy}
                />
            </Box>

            <Typography
                variant={'caption'}
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