import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {Department} from '../../models/entities/department';
import {DepartmentsService} from '../../services/departments.service';
import {useSelector} from 'react-redux';
import {selectLoadedApplication} from '../../slices/app-slice';
import strings from './privacy-dialog-strings.json';
import {Localization} from '../../locale/localization';
import {PrivacyDialogProps} from './privacy-dialog-props';

const _ = Localization(strings);

export function PrivacyDialog(props: PrivacyDialogProps) {
    const application = useSelector(selectLoadedApplication);

    const [department, setDepartment] = useState<Department>();

    useEffect(() => {
        if (application != null && application.root.privacy != null && department == null) {
            DepartmentsService.retrieve(application.root.privacy)
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
                id="privacy-dialog-title"
                onClose={props.onHide}
                closeTooltip={_.close}
            >
                {_.title}
            </DialogTitleWithClose>
            <DialogContent
                dangerouslySetInnerHTML={{__html: department?.privacy ?? ''}}
            />
            <DialogActions>
                <Button
                    size="large"
                    onClick={props.onHide}
                    sx={{mr: 2, mb: 2}}
                    variant="outlined"
                >
                    {_.close}
                </Button>
            </DialogActions>
        </Dialog>
    );
}
