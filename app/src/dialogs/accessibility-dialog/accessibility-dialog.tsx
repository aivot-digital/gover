import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {Department} from '../../models/department';
import {DepartmentsService} from '../../services/departments.service';
import {useSelector} from 'react-redux';
import {AccessibilityDialogProps} from './accessibility-dialog-props';
import {selectLoadedApplication} from '../../slices/app-slice';

// TODO: Localize

export function AccessibilityDialog(props: AccessibilityDialogProps) {
    const application = useSelector(selectLoadedApplication);

    const [department, setDepartment] = useState<Department>();

    useEffect(() => {
        if (application != null && application.root.accessibility != null && department == null) {
            DepartmentsService.retrieve(application.root.accessibility)
                .then(setDepartment);
        }
    }, [application, department]);

    return (
        <Dialog
            open={props.open}
            maxWidth={'md'}
            scroll={'paper'}
            onBackdropClick={props.onHide}
            fullWidth={true}
        >
            <DialogTitleWithClose
                id="help-dialog-title"
                onClose={props.onHide}
                closeTooltip={'Close' /* TODO: Localize */}
            >
                Informationen zur Barrierefreiheit
            </DialogTitleWithClose>
            <DialogContent
                dangerouslySetInnerHTML={{__html: department?.accessibility ?? ''}}
            />
            <DialogActions>
                <Button
                    size={'large'}
                    onClick={props.onHide}
                    sx={{mr: 2, mb: 2}}
                    variant={'outlined'}
                >
                    Barrierefreiheit schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
