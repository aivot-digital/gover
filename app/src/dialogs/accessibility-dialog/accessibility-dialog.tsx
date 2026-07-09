import {Alert, Box, Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type AccessibilityDialogProps} from './accessibility-dialog-props';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {PublicDepartmentResponseDTO} from '../../modules/departments/entities/v-department-shadowed-entity';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';
import {MarkdownContent} from '../../components/markdown-content/markdown-content';

export const AccessibilityDialogId = 'accessibility';

export function AccessibilityDialog(props: AccessibilityDialogProps) {
    const application = props.form;

    const [department, setDepartment] = useState<PublicDepartmentResponseDTO>();
    const accessibilityDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.accessibilityDepartmentId));
    const parsedAccessibilityDepartmentId = accessibilityDepartmentId != null && accessibilityDepartmentId !== '' && !Number.isNaN(parseInt(accessibilityDepartmentId)) ?
        parseInt(accessibilityDepartmentId) :
        null;
    const selectedAccessibilityDepartmentId = props.isListingPage ?
        parsedAccessibilityDepartmentId :
        application.accessibilityDepartmentId ?? null;

    useEffect(() => {
        if (selectedAccessibilityDepartmentId == null) {
            setDepartment(undefined);
            return;
        }

        if (department?.id === selectedAccessibilityDepartmentId) {
            return;
        }

        let isCancelled = false;

        // Clear stale department data immediately when the configured source is removed or changed.
        setDepartment(undefined);
        new DepartmentApiService()
            .retrievePublic(selectedAccessibilityDepartmentId)
            .then((department) => {
                if (!isCancelled) {
                    setDepartment(department);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [selectedAccessibilityDepartmentId, department?.id]);

    const commonAccessibility = department?.commonAccessibility;
    const formSpecificAccessibilityStatement = props.isListingPage ? undefined : application.formSpecificAccessibilityStatement;
    const hasAccessibilityText = [commonAccessibility, formSpecificAccessibilityStatement]
        .some((text) => text != null && text.trim().length > 0);

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
                hasAccessibilityText ?
                    <DialogContent>
                        {
                            commonAccessibility != null &&
                            commonAccessibility.trim().length > 0 &&
                            <MarkdownContent markdown={commonAccessibility}/>
                        }
                        {
                            formSpecificAccessibilityStatement != null &&
                            formSpecificAccessibilityStatement.trim().length > 0 &&
                            <Box sx={{mt: commonAccessibility != null && commonAccessibility.trim().length > 0 ? 3 : 0}}>
                                <MarkdownContent markdown={formSpecificAccessibilityStatement}/>
                            </Box>
                        }
                    </DialogContent>
                    :
                    <DialogContent tabIndex={0}>
                        <Alert severity="info">
                            Für die Barrierefreiheitserklärung wurden keine Inhalte gefunden. Wählen Sie eine
                            Organisationseinheit mit allgemeiner Barrierefreiheitserklärung aus und pflegen Sie bei
                            Bedarf den formularspezifischen Teil in den rechtlichen Angaben des Formulars.
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
