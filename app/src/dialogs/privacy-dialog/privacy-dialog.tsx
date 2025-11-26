import {Alert, Box, Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {useSelector} from 'react-redux';
import {selectLoadedForm} from '../../slices/app-slice';
import {type PrivacyDialogProps} from './privacy-dialog-props';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {VDepartmentShadowedEntity} from '../../modules/departments/entities/v-department-shadowed-entity';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';

export const PrivacyDialogId = 'privacy';

export function PrivacyDialog(props: PrivacyDialogProps) {
    const application = useSelector(selectLoadedForm);

    const [department, setDepartment] = useState<VDepartmentShadowedEntity>();
    const privacyDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.privacyDepartmentId));

    useEffect(() => {
        if (
            !props.isListingPage &&
            application?.privacyDepartmentId != null &&
            (department == null || department.id !== application.privacyDepartmentId)
        ) {
            new DepartmentApiService()
                .retrievePublic(application.privacyDepartmentId)
                .then(setDepartment);
        } else if (
            props.isListingPage &&
            privacyDepartmentId != null &&
            privacyDepartmentId != '' &&
            (department == null || department.id !== parseInt(privacyDepartmentId))
        ){
            new DepartmentApiService()
                .retrievePublic(parseInt(privacyDepartmentId))
                .then(setDepartment);
        }
    }, [privacyDepartmentId, application, department]);

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
            {
                department?.commonPrivacy?
                <DialogContent
                    dangerouslySetInnerHTML={{__html: department?.commonPrivacy}}
                />
                :
                <DialogContent tabIndex={0}>
                    <Alert severity="info">
                        Bitte wählen Sie in den Einstellungen des Formulars im Bereich „Rechtliches“ einen Fachbereich als Quelle für den Rechtstext der Datenschutzerklärung aus.
                    </Alert>
                </DialogContent>
            }
            <DialogActions>
                <Box/>
                <Button
                    onClick={props.onHide}
                >
                    Datenschutzerklärung schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
