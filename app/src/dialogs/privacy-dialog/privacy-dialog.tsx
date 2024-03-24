import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../models/entities/department';
import {useSelector} from 'react-redux';
import {selectLoadedForm} from '../../slices/app-slice';
import {type PrivacyDialogProps} from './privacy-dialog-props';
import {useApi} from "../../hooks/use-api";
import {useDepartmentsApi} from "../../hooks/use-departments-api";

export const PrivacyDialogId = 'privacy';

export function PrivacyDialog(props: PrivacyDialogProps): JSX.Element {
    const api = useApi();
    const application = useSelector(selectLoadedForm);

    const [department, setDepartment] = useState<Department>();

    useEffect(() => {
        if (
            application?.privacyDepartmentId != null &&
            (department == null || department.id !== application.privacyDepartmentId)) {
            useDepartmentsApi(api)
                .retrieve(application.privacyDepartmentId)
                .then(setDepartment);
        }
    }, [application, department]);

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
            <DialogContent
                dangerouslySetInnerHTML={{__html: department?.privacy ?? ''}}
            />
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
