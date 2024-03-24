import React, {useEffect, useState} from 'react';
import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../models/entities/department';
import {useSelector} from 'react-redux';
import {selectLoadedForm} from '../../slices/app-slice';
import {type ImprintDialogProps} from './imprint-dialog-props';
import {useApi} from "../../hooks/use-api";
import {useDepartmentsApi} from "../../hooks/use-departments-api";

export const ImprintDialogId = 'imprint';

export function ImprintDialog(props: ImprintDialogProps): JSX.Element {
    const api = useApi();
    const application = useSelector(selectLoadedForm);

    const [department, setDepartment] = useState<Department>();

    useEffect(() => {
        if (
            application?.imprintDepartmentId != null &&
            (department == null || department.id !== application.imprintDepartmentId)
        ) {
            useDepartmentsApi(api)
                .retrieve(application.imprintDepartmentId)
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
                Impressum
            </DialogTitleWithClose>
            <DialogContent
                dangerouslySetInnerHTML={{__html: department?.imprint ?? ''}}
            />
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
