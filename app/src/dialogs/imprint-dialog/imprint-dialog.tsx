import React, {useEffect, useState} from 'react';
import {Alert, Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../modules/departments/models/department';
import {useSelector} from 'react-redux';
import {selectLoadedForm} from '../../slices/app-slice';
import {type ImprintDialogProps} from './imprint-dialog-props';
import {useApi} from "../../hooks/use-api";
import {useAppSelector} from "../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../slices/system-config-slice";
import {SystemConfigKeys} from "../../data/system-config-keys";
import {DepartmentsApiService} from '../../modules/departments/departments-api-service';

export const ImprintDialogId = 'imprint';

export function ImprintDialog(props: ImprintDialogProps): JSX.Element {
    const api = useApi();
    const application = useSelector(selectLoadedForm);

    const [department, setDepartment] = useState<Department>();
    const imprintDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.imprintDepartmentId));

    useEffect(() => {
        if (
            !props.isListingPage &&
            application?.imprintDepartmentId != null &&
            (department == null || department.id !== application.imprintDepartmentId)
        ) {
            new DepartmentsApiService(api)
                .retrievePublic(application.imprintDepartmentId)
                .then(setDepartment);
        } else if (
            props.isListingPage &&
            imprintDepartmentId != null &&
            imprintDepartmentId != '' &&
            (department == null || department.id !== parseInt(imprintDepartmentId))
        ){
            new DepartmentsApiService(api)
                .retrievePublic(parseInt(imprintDepartmentId))
                .then(setDepartment);
        }
    }, [imprintDepartmentId, application, department]);

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
                Impressum
            </DialogTitleWithClose>
            {
                department?.imprint ?
                    <DialogContent
                        dangerouslySetInnerHTML={{__html: department?.imprint}}
                    />
                    :
                    <DialogContent tabIndex={0}>
                        <Alert severity="info">
                            Bitte wählen Sie in den Einstellungen des Formulars im Bereich „Rechtliches“ einen Fachbereich als Quelle für den Rechtstext des Impressums aus.
                        </Alert>
                    </DialogContent>
            }
            <DialogActions>
                <Button
                    onClick={props.onHide}
                    variant="contained"
                >
                    Impressum schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
