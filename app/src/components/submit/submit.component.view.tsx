import React, {useEffect, useState} from 'react';
import {BaseViewProps} from '../_lib/base-view-props';
import {SubmitStepElement} from '../../models/elements/step-elements/submit-step-element';
import {Preamble} from '../static-components/preamble/preamble';
import {
    Box,
    Button,
    CircularProgress,
    FormHelperText,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    Typography,
    useTheme
} from '@mui/material';
import {FadingPaper} from '../static-components/fading-paper/fading-paper';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faFileArrowUp, faUserRobot} from '@fortawesome/pro-light-svg-icons';
import {faShieldCheck} from '@fortawesome/pro-solid-svg-icons';
import {selectCustomerInputValue, updateUserInput} from '../../slices/customer-input-slice';
import {isNullOrEmpty} from '../../utils/is-null-or-empty';
import {Department} from '../../models/department';
import {DepartmentsService} from '../../services/departments.service';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectCustomerInputErrorValue} from '../../slices/customer-input-errors-slice';
import {selectLoadedApplication} from '../../slices/app-slice';
import {useAppDispatch} from '../../hooks/use-app-dispatch';

export const SubmitHumanKey = '__human__';

// TODO: Localization

export function SubmitComponentView({element}: BaseViewProps<SubmitStepElement, void>) {
    const theme = useTheme();
    const dispatch = useAppDispatch();

    const [isCalculating, setIsCalculating] = useState(false);
    const isHuman = useAppSelector(selectCustomerInputValue(SubmitHumanKey));
    const error = useAppSelector(selectCustomerInputErrorValue(SubmitHumanKey));

    const introductionStep = useAppSelector(selectLoadedApplication)?.root.introductionStep;

    const [responsibleDepartment, setResponsibleDepartment] = useState<Department>();
    const [managingDepartment, setManagingDepartment] = useState<Department>();

    useEffect(() => {
        if (introductionStep?.responsibleDepartment) {
            DepartmentsService.retrieve(introductionStep.responsibleDepartment)
                .then(setResponsibleDepartment);
        }
        if (introductionStep?.managingDepartment) {
            DepartmentsService.retrieve(introductionStep.managingDepartment)
                .then(setManagingDepartment);
        }
    }, [introductionStep]);

    if (introductionStep == null) {
        return null;
    }

    return (
        <>
            {
                !isNullOrEmpty(element.textPreSubmit) &&
                <Preamble
                    text={element.textPreSubmit ?? ''}
                />
            }

            {
                (
                    responsibleDepartment ||
                    managingDepartment ||
                    !isNullOrEmpty(element.textProcessingTime) ||
                    (element.documentsToReceive && element.documentsToReceive.length > 0)
                ) &&
                <FadingPaper>
                    {
                        responsibleDepartment &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
                            <Typography
                                variant="subtitle1"
                                color="primary"
                            >
                                ZUSTÄNDIGE STELLE
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {responsibleDepartment.name}<br/>
                                {responsibleDepartment.address}
                            </Typography>
                        </Box>
                    }

                    {
                        managingDepartment &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
                            <Typography
                                variant="subtitle1"
                                color="primary"
                            >
                                BEWIRTSCHAFTENDE STELLE
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {managingDepartment.name}<br/>
                                {managingDepartment.address}
                            </Typography>
                        </Box>
                    }

                    {
                        element.textProcessingTime &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
                            <Typography
                                variant="subtitle1"
                                color="primary"
                            >
                                GESCHÄTZTE BEARBEITUNGSZEIT
                            </Typography>
                            <Typography
                                component="pre"
                                variant="body2"
                            >
                                {element.textProcessingTime}
                            </Typography>
                        </Box>
                    }

                    {
                        element.documentsToReceive && element.documentsToReceive.length > 0 &&
                        <Box sx={{mb: 3, position: 'relative', zIndex: 1,}}>
                            <Typography
                                variant="subtitle1"
                                color="primary"
                            >
                                SIE ERHALTEN FOLGENDE DOKUMENTE
                            </Typography>
                            <List
                                dense
                                disablePadding
                            >
                                {
                                    element.documentsToReceive.map((doc: string) => (
                                        <ListItem
                                            key={doc}
                                            disableGutters
                                        >
                                            <ListItemIcon sx={{minWidth: '34px'}}>
                                                <FontAwesomeIcon
                                                    icon={faFileArrowUp}
                                                    fixedWidth
                                                    size={'lg'}
                                                    color={theme.palette.primary.main}
                                                />
                                            </ListItemIcon>
                                            <ListItemText>
                                                {doc}
                                            </ListItemText>
                                        </ListItem>
                                    ))
                                }
                            </List>
                        </Box>
                    }
                </FadingPaper>
            }

            <Box sx={{mt: 4}}>
                <Typography
                    variant="h6"
                    color="primary"
                >
                    Schutz vor automatisierten Einreichungen
                </Typography>

                <Typography
                    variant={'body2'}
                    sx={{maxWidth: '660px', mt: 1}}
                >
                    Bitte bestätigen Sie mit einem Klick auf das nachfolgende Element, dass Sie ein Mensch sind.
                    Die Verifizierung kann einen kleinen Moment dauern. Vielen Dank!
                </Typography>

                <Box sx={{mt: 3, minHeight: '61px'}}>
                    {
                        !isCalculating && !isHuman &&
                        <>
                            <Box>
                                <Button
                                    startIcon={<FontAwesomeIcon
                                        icon={faUserRobot}
                                        fixedWidth
                                    />}
                                    onClick={() => {
                                        setIsCalculating(true);
                                        setTimeout(() => {
                                            dispatch(updateUserInput({
                                                key: SubmitHumanKey,
                                                value: true,
                                            }));
                                            setIsCalculating(false);
                                        }, 1000);
                                    }}
                                    sx={{border: '1px solid #E0E0E0', px: 4, py: 2}}
                                    size={'large'}
                                >
                                    Verifizierung starten
                                </Button>
                            </Box>
                            {
                                error &&
                                <Box sx={{mt: 2}}>
                                    <FormHelperText error={true}>
                                        {error}
                                    </FormHelperText>
                                </Box>
                            }
                        </>
                    }
                    {
                        isCalculating &&
                        <CircularProgress/>
                    }
                    {
                        isHuman &&
                        <Box sx={{display: 'flex', alignItems: 'center'}}>
                            <FontAwesomeIcon
                                icon={faShieldCheck}
                                size="2x"
                            />
                            <Typography
                                sx={{ml: 2}}
                            >
                                Verifizierung erfolgreich. Sie sind ein Mensch.
                            </Typography>
                        </Box>
                    }
                </Box>
            </Box>

        </>
    );
}
