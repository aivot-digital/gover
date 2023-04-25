import {Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {Department} from '../../models/entities/department';
import {DepartmentsService} from '../../services/departments.service';
import {useSelector} from 'react-redux';
import {selectLoadedApplication} from '../../slices/app-slice';
import {Localization} from '../../locale/localization';
import strings from './imprint-dialog-strings.json';
import {ImprintDialogProps} from './imprint-dialog-props';


const _ = Localization(strings);

export function ImprintDialog(props: ImprintDialogProps) {
    const application = useSelector(selectLoadedApplication);

    const [department, setDepartment] = useState<Department>();

    useEffect(() => {
        if (application != null && application.root.imprint != null && department == null) {
            DepartmentsService.retrieve(application.root.imprint)
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
                id="imprint-dialog-title"
                onClose={props.onHide}
                closeTooltip={_.close}
            >
                {_.title}
            </DialogTitleWithClose>
            <DialogContent
                dangerouslySetInnerHTML={{__html: department?.imprint ?? ''}}
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
