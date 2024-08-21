import React, {useEffect, useState} from 'react';
import {
    Box,
    Button,
    Divider,
    Grid,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    Typography,
    useTheme
} from '@mui/material';
import {type IntroductionStepElement} from '../../models/elements/steps/introduction-step-element';
import {FadingPaper} from '../fading-paper/fading-paper';
import {Preamble} from '../preamble/preamble';
import {type Department} from '../../models/entities/department';
import {
    selectCustomerInputError,
    selectCustomerInputValue,
    selectLoadedForm,
    updateCustomerInput,
} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {isStringNotNullOrEmpty, isStringNullOrEmpty} from '../../utils/string-utils';
import {type BaseViewProps} from '../../views/base-view';
import {useLocation} from 'react-router-dom';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {CheckboxFieldComponent} from '../checkbox-field/checkbox-field-component';
import {useAppDispatch} from '../../hooks/use-app-dispatch';
import {formatMetaDialog} from '../../utils/format-meta-dialog';
import {showErrorSnackbar} from '../../slices/snackbar-slice';
import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined';
import UnfoldMoreOutlinedIcon from '@mui/icons-material/UnfoldMoreOutlined';
import UnfoldLessOutlinedIcon from '@mui/icons-material/UnfoldLessOutlined';
import {useApi} from '../../hooks/use-api';
import {useDepartmentsApi} from '../../hooks/use-departments-api';
import {BundIdInput} from '../bund-id-input/bund-id-input';
import {BayernIdInput} from '../bayern-id-input/bayern-id-input';
import {ShIdInput} from '../sh-id-input/sh-id-input';
import {MukInput} from '../muk-input/muk-input';
import {BayernIdAccessLevel} from '../../data/bayern-id-access-level';
import {BundIdAccessLevel} from '../../data/bund-id-access-level';
import {ShIdAccessLevel} from '../../data/sh-id-access-level';
import {MukAccessLevel} from '../../data/muk-access-level';
import {Form} from '../../models/entities/form';
import {AnyElement} from '../../models/elements/any-element';
import {IdCustomerDataKey} from '../id-input/id-input';
import {RichtextComponentView} from '../richtext/richtext.component.view';

export const PrivacyUserInputKey = '__privacy__';

export function GeneralInformationComponentView(props: BaseViewProps<IntroductionStepElement, void>): JSX.Element {
    const api = useApi();
    const dispatch = useAppDispatch();
    const theme = useTheme();
    const location = useLocation();

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

    function cleanDocuments(documents: Array<String> | undefined) {
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
                    useDepartmentsApi(api)
                        .retrieve(application.responsibleDepartmentId)
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
                    useDepartmentsApi(api)
                        .retrieve(application.managingDepartmentId)
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
    }, [props.element]);

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
                                sx={{ textTransform: 'uppercase' }}
                            >
                                Zuständige Stelle
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {
                                    isStringNotNullOrEmpty(providerName) &&
                                    <>
                                        {providerName}<br/>
                                    </>
                                }
                                {responsibleDepartment.name}<br/>
                                {responsibleDepartment.address}
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
                                sx={{ textTransform: 'uppercase' }}
                            >
                                Bewirtschaftende Stelle
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {
                                    isStringNotNullOrEmpty(providerName) &&
                                    <>
                                        {providerName}<br/>
                                    </>
                                }
                                {managingDepartment.name}<br/>
                                {managingDepartment.address}
                            </Typography>
                        </Box>
                    }

                    {
                        props.element.eligiblePersons != null &&
                        props.element.eligiblePersons.length > 0 &&
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
                                sx={{ textTransform: 'uppercase' }}
                            >
                                Antragsberechtigte
                            </Typography>
                            <List dense
                                  disablePadding>
                                {props.element.eligiblePersons
                                    .slice(0, showAllEligiblePersons ? props.element.eligiblePersons.length : initialDisplayCount)
                                    .map((person, index) => (
                                        <ListItem key={String(index) + person}
                                                  disableGutters>
                                            <ListItemIcon sx={{minWidth: '34px'}}>
                                                <PersonOutlineOutlinedIcon sx={{color: theme.palette.primary.main}}/>
                                            </ListItemIcon>
                                            <ListItemText>{person}</ListItemText>
                                        </ListItem>
                                    ))}
                                {props.element.eligiblePersons.length > initialDisplayCount && (
                                    <div>
                                        <Divider sx={{my: 1, width: "50%"}}
                                                 component="li"/>
                                        <Button size={"small"}
                                                startIcon={showAllEligiblePersons ? <UnfoldLessOutlinedIcon sx={{
                                                    color: theme.palette.primary.main,
                                                    marginLeft: "2px",
                                                    marginRight: "4px"
                                                }}/> : <UnfoldMoreOutlinedIcon sx={{
                                                    color: theme.palette.primary.main,
                                                    marginLeft: "2px",
                                                    marginRight: "4px"
                                                }}/>}
                                                onClick={handleToggleShowAllEligiblePersons}>
                                            {showAllEligiblePersons ? 'Weniger anzeigen' : 'Alle anzeigen (' + (props.element.eligiblePersons.length - initialDisplayCount) + ' weitere)'}
                                        </Button>
                                    </div>
                                )}
                            </List>
                        </Box>
                    }

                    {
                        supportingDocuments.length > 0 &&
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
                                sx={{ textTransform: 'uppercase' }}
                            >
                                Relevante Dokumente
                            </Typography>
                            <List dense
                                  disablePadding>
                                {supportingDocuments
                                    .slice(0, showAllSupportingDocuments ? supportingDocuments.length : initialDisplayCount)
                                    .map((document, index) => (
                                        <ListItem key={String(index) + document}
                                                  disableGutters>
                                            <ListItemIcon sx={{minWidth: '34px'}}>
                                                <DescriptionOutlinedIcon sx={{color: theme.palette.primary.main}}/>
                                            </ListItemIcon>
                                            <ListItemText>{document}</ListItemText>
                                        </ListItem>
                                    ))}
                                {supportingDocuments.length > initialDisplayCount && (
                                    <div>
                                        <Divider sx={{my: 1, width: "50%"}}
                                                 component="li"/>
                                        <Button size={"small"}
                                                startIcon={showAllSupportingDocuments ? <UnfoldLessOutlinedIcon sx={{
                                                    color: theme.palette.primary.main,
                                                    marginLeft: "2px",
                                                    marginRight: "4px"
                                                }}/> : <UnfoldMoreOutlinedIcon sx={{
                                                    color: theme.palette.primary.main,
                                                    marginLeft: "2px",
                                                    marginRight: "4px"
                                                }}/>}
                                                onClick={handleToggleShowAllSupportingDocuments}>
                                            {showAllSupportingDocuments ? 'Weniger anzeigen' : 'Alle anzeigen (' + (supportingDocuments.length - initialDisplayCount) + ' weitere)'}
                                        </Button>
                                    </div>
                                )}
                            </List>
                        </Box>
                    }

                    {
                        documentsToAttach.length > 0 &&
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
                                sx={{ textTransform: 'uppercase' }}
                            >
                                Einzureichende Dokumente
                            </Typography>
                            <List dense
                                  disablePadding>
                                {documentsToAttach
                                    .slice(0, showAllDocumentsToAttach ? documentsToAttach.length : initialDisplayCount)
                                    .map((document, index) => (
                                        <ListItem key={String(index) + document}
                                                  disableGutters>
                                            <ListItemIcon sx={{minWidth: '34px'}}>
                                                <UploadFileOutlinedIcon sx={{color: theme.palette.primary.main}}/>
                                            </ListItemIcon>
                                            <ListItemText>{document}</ListItemText>
                                        </ListItem>
                                    ))}
                                {documentsToAttach.length > initialDisplayCount && (
                                    <div>
                                        <Divider sx={{my: 1, width: "50%"}}
                                                 component="li"/>
                                        <Button size={"small"}
                                                startIcon={showAllDocumentsToAttach ? <UnfoldLessOutlinedIcon sx={{
                                                    color: theme.palette.primary.main,
                                                    marginLeft: "2px",
                                                    marginRight: "4px"
                                                }}/> : <UnfoldMoreOutlinedIcon sx={{
                                                    color: theme.palette.primary.main,
                                                    marginLeft: "2px",
                                                    marginRight: "4px"
                                                }}/>}
                                                onClick={handleToggleShowAllDocumentsToAttach}>
                                            {showAllDocumentsToAttach ? 'Weniger anzeigen' : 'Alle anzeigen (' + (documentsToAttach.length - initialDisplayCount) + ' weitere)'}
                                        </Button>
                                    </div>
                                )}
                            </List>
                        </Box>
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
                                sx={{ textTransform: 'uppercase' }}
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
                                sx={{ textTransform: 'uppercase' }}
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

            {
                application != null &&
                (
                    application.bayernIdEnabled ||
                    application.bundIdEnabled ||
                    application.shIdEnabled ||
                    application.mukEnabled
                ) &&
                <NutzerkontoSelect
                    form={application}
                    allElements={props.allElements}
                />
            }

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
                    <Typography
                        variant="body2"
                        dangerouslySetInnerHTML={{
                            __html: formatMetaDialog(application.root.privacyText, location),
                        }}
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

function getAccountHeadlineSuffix(form: Form){
    if((form.bayernIdLevel != null || form.bundIdLevel != null || form.shIdLevel != null) && form.mukLevel != null){
        return "Nutzer- oder Unternehmens-Konto";
    } else if(!form.mukLevel){
        return "Nutzerkonto";
    } else {
        return "Unternehmenskonto";
    }
}

function NutzerkontoSelect(props: { form: Form, allElements: AnyElement[] }) {
    const error = useAppSelector(selectCustomerInputError(IdCustomerDataKey));
    const accountHeadlineSuffix = getAccountHeadlineSuffix(props.form);
    return (
        <Box
            id={"idp-login"}
            sx={{
                my: 6,
            }}
        >

            {
                (
                    (props.form.bayernIdLevel != null && props.form.bayernIdLevel !== BayernIdAccessLevel.Optional) ||
                    (props.form.bundIdLevel != null && props.form.bundIdLevel !== BundIdAccessLevel.Optional) ||
                    (props.form.shIdLevel != null && props.form.shIdLevel !== ShIdAccessLevel.Optional) ||
                    (props.form.mukLevel != null && props.form.mukLevel !== MukAccessLevel.Optional)
                ) ? <div>
                    <Typography
                        component={'h4'}
                        variant="h6"
                        color="primary"
                        sx={{
                            mt: 4,
                        }}
                    >
                        Verpflichtende Authentifizierung mit einem {accountHeadlineSuffix}
                    </Typography>
                    <Typography sx={{mt: 1, mb: 2, maxWidth: "620px"}}>
                        Eine Authentifizierung mittels einem der nachfolgenden Konten ist verpflichtend. Ihre Daten werden im Anschluss automatisch in den Antrag übernommen.
                    </Typography>
                </div> : <div>
                    <Typography
                        component={'h4'}
                        variant="h6"
                        color="primary"
                        sx={{
                            mt: 4,
                        }}
                    >
                        Optionale Authentifizierung mit einem {accountHeadlineSuffix}
                    </Typography>
                    <Typography sx={{mt: 1, mb: 2, maxWidth: "600px"}}>
                        Eine Authentifizierung mittels der nachfolgenden Konten ist optional möglich. Ihre Daten werden im Anschluss automatisch in den Antrag übernommen.
                    </Typography>
                </div>
            }

            <Grid container>
                <Grid
                    item
                    xs={12}
                    lg={12}
                >
                    {
                        props.form.bundIdEnabled &&
                        <BundIdInput
                            allElements={props.allElements}
                            form={props.form}
                        />
                    }

                    {
                        props.form.bayernIdEnabled &&
                        <BayernIdInput
                            allElements={props.allElements}
                            form={props.form}
                        />
                    }

                    {
                        props.form.shIdEnabled &&
                        <ShIdInput
                            allElements={props.allElements}
                            form={props.form}
                        />
                    }

                    {
                        props.form.mukEnabled &&
                        <MukInput
                            allElements={props.allElements}
                            form={props.form}
                        />
                    }
                </Grid>
            </Grid>

            {
                error != null &&
                <Typography
                    variant="caption"
                    color="error"
                    sx={{
                        display: 'block',
                        mt: 2,
                    }}
                >
                    {error}
                </Typography>
            }
        </Box>
    );
}
