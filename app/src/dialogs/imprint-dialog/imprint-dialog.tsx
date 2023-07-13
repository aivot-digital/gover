import React, { useEffect, useState } from 'react';
import { Button, Dialog, DialogActions, DialogContent } from '@mui/material';
import {
    DialogTitleWithClose,
} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import { type Department } from '../../models/entities/department';
import { DepartmentsService } from '../../services/departments-service';
import { useSelector } from 'react-redux';
import { selectLoadedApplication } from '../../slices/app-slice';
import { type ImprintDialogProps } from './imprint-dialog-props';


export function ImprintDialog(props: ImprintDialogProps): JSX.Element {
    const application = useSelector(selectLoadedApplication);

    const [department, setDepartment] = useState<Department>();

    useEffect(() => {
        if (
            application?.imprintDepartment != null &&
            (department == null || department.id !== application.imprintDepartment)
        ) {
            DepartmentsService
                .retrieve(application.imprintDepartment)
                .then(setDepartment);
        }
    }, [application, department]);

    return (
        <Dialog
            open={ props.open }
            maxWidth="md"
            scroll="paper"
            onClose={ props.onHide }
            fullWidth={ true }
        >
            <DialogTitleWithClose
                onClose={ props.onHide }
            >
                Impressum
            </DialogTitleWithClose>
            <DialogContent
                dangerouslySetInnerHTML={ { __html: department?.imprint ?? '' } }
            />
            <DialogActions>
                <Button
                    size="large"
                    onClick={ props.onHide }
                    sx={ {
                        mr: 2,
                        mb: 2,
                    } }
                    variant="outlined"
                >
                    Impressum schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
