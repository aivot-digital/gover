import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {type Department} from '../../models/entities/department';
import {DepartmentsService} from '../../services/departments-service';
import {useSelector} from 'react-redux';
import {type AccessibilityDialogProps} from './accessibility-dialog-props';
import {selectLoadedApplication} from '../../slices/app-slice';

export function AccessibilityDialog(props: AccessibilityDialogProps): JSX.Element {
    const application = useSelector(selectLoadedApplication);

    const [department, setDepartment] = useState<Department>();

    useEffect(() => {
        if (
            application?.accessibilityDepartment != null &&
            (department == null || department.id !== application.accessibilityDepartment)
        ) {
            DepartmentsService
                .retrieve(application.accessibilityDepartment)
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
                    size="large"
                    onClick={props.onHide}
                    sx={{
                        mr: 2,
                        mb: 2,
                    }}
                    variant="outlined"
                >
                    Barrierefreiheit schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
