import {Alert, Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../modules/departments/models/department';
import {useSelector} from 'react-redux';
import {selectLoadedForm} from '../../slices/app-slice';
import {type PrivacyDialogProps} from './privacy-dialog-props';
import {useApi} from "../../hooks/use-api";
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../slices/system-config-slice";
import {SystemConfigKeys} from "../../data/system-config-keys";
import {DepartmentsApiService} from '../../modules/departments/departments-api-service';

export const PrivacyDialogId = 'privacy';

export function PrivacyDialog(props: PrivacyDialogProps): JSX.Element {
    const api = useApi();
    const application = useSelector(selectLoadedForm);

    const [department, setDepartment] = useState<Department>();
    const privacyDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.privacyDepartmentId));

    useEffect(() => {
        if (
            !props.isListingPage &&
            application?.privacyDepartmentId != null &&
            (department == null || department.id !== application.privacyDepartmentId)
        ) {
            new DepartmentsApiService(api)
                .retrievePublic(application.privacyDepartmentId)
                .then(setDepartment);
        } else if (
            props.isListingPage &&
            privacyDepartmentId != null &&
            privacyDepartmentId != '' &&
            (department == null || department.id !== parseInt(privacyDepartmentId))
        ){
            new DepartmentsApiService(api)
                .retrievePublic(parseInt(privacyDepartmentId))
                .then(setDepartment);
        }
    }, [privacyDepartmentId, application, department]);

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
                Datenschutzerklärung
            </DialogTitleWithClose>
            {
                department?.privacy ?
                <DialogContent
                    dangerouslySetInnerHTML={{__html: department?.privacy}}
                />
                :
                <DialogContent tabIndex={0}>
                    <Alert severity="info">
                        Bitte wählen Sie in den Einstellungen des Formulars im Bereich „Rechtliches“ einen Fachbereich als Quelle für den Rechtstext der Datenschutzerklärung aus.
                    </Alert>
                </DialogContent>
            }
            <DialogActions>
                <Button
                    onClick={props.onHide}
                    variant="contained"
                >
                    Datenschutzerklärung schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
