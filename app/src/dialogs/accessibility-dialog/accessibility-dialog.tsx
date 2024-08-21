import {Alert, Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../models/entities/department';
import {useSelector} from 'react-redux';
import {type AccessibilityDialogProps} from './accessibility-dialog-props';
import {selectLoadedForm} from '../../slices/app-slice';
import {useApi} from "../../hooks/use-api";
import {useDepartmentsApi} from "../../hooks/use-departments-api";
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../slices/system-config-slice";
import {SystemConfigKeys} from "../../data/system-config-keys";

export const AccessibilityDialogId = 'accessibility';

export function AccessibilityDialog(props: AccessibilityDialogProps): JSX.Element {
    const api = useApi();
    const application = useSelector(selectLoadedForm);

    const [department, setDepartment] = useState<Department>();
    const accessibilityDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.accessibilityDepartmentId));

    useEffect(() => {
        if (
            !props.isListingPage &&
            application?.accessibilityDepartmentId != null &&
            (department == null || department.id !== application.accessibilityDepartmentId)
        ) {
            useDepartmentsApi(api)
                .retrieve(application.accessibilityDepartmentId)
                .then(setDepartment);
        } else if (
            props.isListingPage &&
            accessibilityDepartmentId != null &&
            accessibilityDepartmentId != '' &&
            (department == null || department.id !== parseInt(accessibilityDepartmentId))
        ){
            useDepartmentsApi(api)
                .retrieve(parseInt(accessibilityDepartmentId))
                .then(setDepartment);
        }
    }, [accessibilityDepartmentId, application, department]);

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
                closeTooltip="Schließen"
            >
                Informationen zur Barrierefreiheit
            </DialogTitleWithClose>
            {
                department?.accessibility ?
                    <DialogContent
                        dangerouslySetInnerHTML={{__html: department?.accessibility}}
                    />
                    :
                    <DialogContent tabIndex={0}>
                        <Alert severity="info">
                            Bitte wählen Sie in den Einstellungen des Formulars im Bereich „Rechtliches“ einen Fachbereich als Quelle für die Informationen zur Barrierefreiheit aus.
                        </Alert>
                    </DialogContent>
            }
            <DialogActions>
                <Button
                    onClick={props.onHide}
                    variant="contained"
                >
                    Informationen schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
