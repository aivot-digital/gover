import {Alert, Box, Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {useSelector} from 'react-redux';
import {type AccessibilityDialogProps} from './accessibility-dialog-props';
import {selectLoadedForm} from '../../slices/app-slice';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {VDepartmentShadowedEntity} from '../../modules/departments/entities/v-department-shadowed-entity';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';

export const AccessibilityDialogId = 'accessibility';

export function AccessibilityDialog(props: AccessibilityDialogProps) {
    const application = useSelector(selectLoadedForm);

    const [department, setDepartment] = useState<VDepartmentShadowedEntity>();
    const accessibilityDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.accessibilityDepartmentId));

    useEffect(() => {
        if (
            !props.isListingPage &&
            application?.version.accessibilityDepartmentId != null &&
            (department == null || department.id !== application.version.accessibilityDepartmentId)
        ) {
            new DepartmentApiService()
                .retrievePublic(application.version.accessibilityDepartmentId)
                .then(setDepartment);
        } else if (
            props.isListingPage &&
            accessibilityDepartmentId != null &&
            accessibilityDepartmentId != '' &&
            (department == null || department.id !== parseInt(accessibilityDepartmentId))
        ) {
            new DepartmentApiService()
                .retrievePublic(parseInt(accessibilityDepartmentId))
                .then(setDepartment);
        }
    }, [accessibilityDepartmentId, application, department]);

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
            {
                department?.commonAccessibility ?
                    <DialogContent
                        dangerouslySetInnerHTML={{__html: department?.commonAccessibility}}
                    />
                    :
                    <DialogContent tabIndex={0}>
                        <Alert severity="info">
                            Bitte wählen Sie in den Einstellungen des Formulars im Bereich „Rechtliches“ einen Fachbereich als Quelle für die Informationen zur Barrierefreiheit aus.
                        </Alert>
                    </DialogContent>
            }
            <DialogActions>
                <Box/>
                <Button
                    onClick={props.onHide}
                >
                    Informationen schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
