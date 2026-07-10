import {Alert, Box, Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type PrivacyDialogProps} from './privacy-dialog-props';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {PublicDepartmentResponseDTO} from '../../modules/departments/entities/v-department-shadowed-entity';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';
import {MarkdownContent} from '../../components/markdown-content/markdown-content';

export const PrivacyDialogId = 'privacy';

export function PrivacyDialog(props: PrivacyDialogProps) {
    const application = props.form;

    const [department, setDepartment] = useState<PublicDepartmentResponseDTO>();
    const privacyDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.privacyDepartmentId));
    const parsedPrivacyDepartmentId = privacyDepartmentId != null && privacyDepartmentId !== '' && !Number.isNaN(parseInt(privacyDepartmentId)) ?
        parseInt(privacyDepartmentId) :
        null;
    const selectedPrivacyDepartmentId = props.isListingPage ?
        parsedPrivacyDepartmentId :
        application.privacyDepartmentId ?? null;

    useEffect(() => {
        if (selectedPrivacyDepartmentId == null) {
            setDepartment(undefined);
            return;
        }

        let isCancelled = false;

        // Clear stale department data immediately when the configured source is removed or changed.
        setDepartment(undefined);
        new DepartmentApiService()
            .retrievePublic(selectedPrivacyDepartmentId)
            .then((department) => {
                if (!isCancelled) {
                    setDepartment(department);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [selectedPrivacyDepartmentId]);

    const commonPrivacy = department?.commonPrivacy;
    const formSpecificPrivacyStatement = props.isListingPage ? undefined : application.formSpecificPrivacyStatement;
    const hasPrivacyText = [commonPrivacy, formSpecificPrivacyStatement]
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
            >
                Datenschutzerklärung
            </DialogTitleWithClose>
            {
                hasPrivacyText ?
                    <DialogContent>
                        {
                            commonPrivacy != null &&
                            commonPrivacy.trim().length > 0 &&
                            <MarkdownContent markdown={commonPrivacy}/>
                        }
                        {
                            formSpecificPrivacyStatement != null &&
                            formSpecificPrivacyStatement.trim().length > 0 &&
                            <Box sx={{mt: commonPrivacy != null && commonPrivacy.trim().length > 0 ? 3 : 0}}>
                                <MarkdownContent markdown={formSpecificPrivacyStatement}/>
                            </Box>
                        }
                    </DialogContent>
                    :
                    <DialogContent tabIndex={0}>
                        <Alert severity="info">
                            Für die Datenschutzerklärung wurden keine Inhalte gefunden. Wählen Sie eine Organisationseinheit
                            mit allgemeinem Datenschutztext aus und pflegen Sie bei Bedarf den formularspezifischen Teil
                            in den rechtlichen Angaben des Formulars.
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
