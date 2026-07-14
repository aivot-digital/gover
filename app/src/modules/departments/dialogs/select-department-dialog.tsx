import {Dialog, DialogContent} from '@mui/material';
import React, {useEffect, useState} from 'react';
import CheckOutlined from '@mui/icons-material/CheckOutlined';
import {DialogTitleWithClose} from '../../../components/dialog-title-with-close/dialog-title-with-close';
import {AlertComponent} from '../../../components/alert/alert-component';
import {DepartmentBrowser} from '../components/department-browser';
import {type VDepartmentShadowedEntityWithChildren} from '../entities/v-department-shadowed-entity';
import {VDepartmentShadowedApiService} from '../services/v-department-shadowed-api-service';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectPermissions} from '../../../slices/user-slice';
import {Permission} from '../../../data/permissions/permission';
import {checkDepartmentPermission, formatMissingPermissionTooltip} from '../../permissions/utils/permission-utils';

interface SelectDepartmentDialogProps {
    open: boolean;
    onClose: () => void;
    onSelect: (department: VDepartmentShadowedEntityWithChildren) => void;
    selectedDepartmentId?: number | null;
    title?: string;
    departments?: VDepartmentShadowedEntityWithChildren[];
}

export function SelectDepartmentDialog(props: SelectDepartmentDialogProps): React.ReactElement {
    const permissions = useAppSelector(selectPermissions);
    const {
        open,
        onClose,
        onSelect,
        selectedDepartmentId,
        title = 'Organisationseinheit auswählen',
        departments: providedDepartments,
    } = props;

    const [
        departments,
        setDepartments,
    ] = useState<VDepartmentShadowedEntityWithChildren[]>();
    const [
        loadError,
        setLoadError,
    ] = useState(false);

    useEffect(() => {
        if (!open || providedDepartments != null) {
            return;
        }

        let active = true;

        setDepartments(undefined);
        setLoadError(false);

        void new VDepartmentShadowedApiService()
            .retrieveOrgTree()
            .then((items) => {
                if (!active) {
                    return;
                }

                setDepartments(items);
            })
            .catch((err) => {
                if (!active) {
                    return;
                }

                console.error(err);
                setDepartments([]);
                setLoadError(true);
            });

        return () => {
            active = false;
        };
    }, [open, providedDepartments]);

    useEffect(() => {
        if (providedDepartments != null) {
            setDepartments(providedDepartments);
            setLoadError(false);
        }
    }, [providedDepartments]);

    return (
        <Dialog
            open={open}
            onClose={onClose}
            fullWidth
            maxWidth="lg"
        >
            <DialogTitleWithClose onClose={onClose}>
                {title}
            </DialogTitleWithClose>

            <DialogContent
                sx={{
                    p: 2,
                    height: 'min(74vh, 820px)',
                    overflowY: 'auto',
                }}
            >
                <DepartmentBrowser
                    departments={departments}
                    loadError={loadError}
                    selectedDepartmentId={selectedDepartmentId}
                    emptyState={(
                        <AlertComponent
                            color="info"
                            sx={{my: 1}}
                        >
                            Es sind keine Organisationseinheiten vorhanden.
                        </AlertComponent>
                    )}
                    getActions={(department) => [
                        {
                            label: 'Auswählen',
                            icon: <CheckOutlined />,
                            variant: 'contained',
                            disabled: !checkDepartmentPermission(permissions, department.id, Permission.DEPARTMENT_READ),
                            disabledTooltip: formatMissingPermissionTooltip(Permission.DEPARTMENT_READ),
                            onClick: () => {
                                onSelect(department);
                                onClose();
                            },
                        },
                    ]}
                />
            </DialogContent>
        </Dialog>
    );
}
