import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../models/entities/department';
import {useSelector} from 'react-redux';
import {type AccessibilityDialogProps} from './accessibility-dialog-props';
import {selectLoadedForm} from '../../slices/app-slice';
import {useApi} from "../../hooks/use-api";
import {useDepartmentsApi} from "../../hooks/use-departments-api";

export const AccessibilityDialogId = 'accessibility';

export function AccessibilityDialog(props: AccessibilityDialogProps): JSX.Element {
    const api = useApi();
    const application = useSelector(selectLoadedForm);

    const [department, setDepartment] = useState<Department>();

    useEffect(() => {
        if (
            application?.accessibilityDepartmentId != null &&
            (department == null || department.id !== application.accessibilityDepartmentId)
        ) {
            useDepartmentsApi(api)
                .retrieve(application.accessibilityDepartmentId)
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
                closeTooltip="Schließen"
            >
                Informationen zur Barrierefreiheit
            </DialogTitleWithClose>
            <DialogContent
                dangerouslySetInnerHTML={{
                    __html: department?.accessibility ?? '',
                }}
            />
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
