import React, {useEffect, useState} from 'react';
import {Alert, Box, Button, Dialog, DialogActions, DialogContent} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {type ImprintDialogProps} from './imprint-dialog-props';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../slices/system-config-slice';
import {SystemConfigKeys} from '../../data/system-config-keys';
import {PublicDepartmentResponseDTO} from '../../modules/departments/entities/v-department-shadowed-entity';
import {DepartmentApiService} from '../../modules/departments/services/department-api-service';
import {MarkdownContent} from '../../components/markdown-content/markdown-content';

export const ImprintDialogId = 'imprint';

export function ImprintDialog(props: ImprintDialogProps) {
    const [department, setDepartment] = useState<PublicDepartmentResponseDTO>();
    const imprintDepartmentId = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.listingPage.imprintDepartmentId));
    const parsedImprintDepartmentId = imprintDepartmentId != null && imprintDepartmentId !== '' && !Number.isNaN(parseInt(imprintDepartmentId)) ?
        parseInt(imprintDepartmentId) :
        null;
    const selectedImprintDepartmentId = props.isListingPage ?
        parsedImprintDepartmentId :
        props.version?.imprintDepartmentId ?? null;

    useEffect(() => {
        if (selectedImprintDepartmentId == null) {
            setDepartment(undefined);
            return;
        }

        let isCancelled = false;

        // Clear stale department data immediately when the configured source is removed or changed.
        setDepartment(undefined);
        new DepartmentApiService()
            .retrievePublic(selectedImprintDepartmentId)
            .then((department) => {
                if (!isCancelled) {
                    setDepartment(department);
                }
            });

        return () => {
            isCancelled = true;
        };
    }, [selectedImprintDepartmentId]);

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
                Impressum
            </DialogTitleWithClose>
            {
                department?.imprint ?
                    <DialogContent>
                        <MarkdownContent markdown={department.imprint}/>
                    </DialogContent>
                    :
                    <DialogContent tabIndex={0}>
                        <Alert severity="info">
                            Bitte wählen Sie in den versionsspezifischen Einstellungen im Bereich „Rechtliches“ eine
                            Organisationseinheit als Quelle für den Rechtstext des Impressums aus.
                        </Alert>
                    </DialogContent>
            }
            <DialogActions>
                <Box/>
                <Button
                    onClick={props.onHide}
                >
                    Impressum schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
