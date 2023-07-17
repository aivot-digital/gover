import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../models/entities/department';
import {DepartmentsService} from '../../services/departments-service';
import {useSelector} from 'react-redux';
import {selectLoadedApplication} from '../../slices/app-slice';
import {type PrivacyDialogProps} from './privacy-dialog-props';


export function PrivacyDialog(props: PrivacyDialogProps): JSX.Element {
    const application = useSelector(selectLoadedApplication);

    const [department, setDepartment] = useState<Department>();

    useEffect(() => {
        if (
            application?.privacyDepartment != null &&
            (department == null || department.id !== application.privacyDepartment)) {
            DepartmentsService
                .retrieve(application.privacyDepartment)
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
                    size="large"
                    onClick={props.onHide}
                    sx={{
                        mr: 2,
                        mb: 2,
                    }}
                    variant="outlined"
                >
                    Datenschutzerklärung schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
