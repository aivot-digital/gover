import React, {useState} from 'react';
import {Box, type SxProps, type Theme} from '@mui/material';
import GroupWork from '@aivot/mui-material-symbols-400-n25-outlined/GroupWork';
import {type VDepartmentShadowedEntity, type VDepartmentShadowedEntityWithChildren} from '../entities/v-department-shadowed-entity';
import {getDepartmentPath, getDepartmentTypeIcons} from '../utils/department-utils';
import {SelectDepartmentDialog} from '../dialogs/select-department-dialog';
import {DialogSelectionField, type FormFieldLayoutProps} from '../../../components/form-field';
import {useNormalizedReactId} from '../../../hooks/use-normalized-react-id';

export interface DepartmentSelectFieldProps extends FormFieldLayoutProps {
    label: string;
    value?: VDepartmentShadowedEntity | null;
    onChange: (department: VDepartmentShadowedEntityWithChildren | null) => void;
    disabled?: boolean;
    readOnly?: boolean;
    busy?: boolean;
    error?: string;
    hint?: string;
    placeholder?: string;
    required?: boolean;
    dialogTitle?: string;
    departments?: VDepartmentShadowedEntityWithChildren[];
    isDepartmentSelectable?: (department: VDepartmentShadowedEntityWithChildren) => boolean;
    getDepartmentDisabledTooltip?: (department: VDepartmentShadowedEntityWithChildren) => string | undefined;
    controlSx?: SxProps<Theme>;
}

export function DepartmentSelectField(props: DepartmentSelectFieldProps): React.ReactElement {
    const {
        label,
        value,
        onChange,
        disabled = false,
        readOnly = false,
        busy = false,
        error,
        hint,
        placeholder = 'Keine Organisationseinheit ausgewählt',
        required = false,
        dialogTitle = 'Organisationseinheit auswählen',
        departments,
        isDepartmentSelectable,
        getDepartmentDisabledTooltip,
    } = props;
    const generatedId = useNormalizedReactId();
    const dialogId = `${props.id ?? `department-select-${generatedId}`}-dialog`;
    const [showSelectDepartmentDialog, setShowSelectDepartmentDialog] = useState(false);
    const departmentPath = value != null && (value.parentNames?.length ?? 0) > 0
        ? getDepartmentPath(value)
        : undefined;
    const departmentIcon = value != null ? getDepartmentTypeIcons(value.depth) : <GroupWork />;
    const isInteractionDisabled = disabled || readOnly || busy;

    return (
        <>
            <DialogSelectionField
                id={props.id}
                ariaLabel={props.ariaLabel}
                ariaDescribedBy={props.ariaDescribedBy}
                label={label}
                labelAction={props.labelAction}
                hint={hint}
                error={error}
                required={required}
                disabled={disabled}
                readOnly={readOnly}
                busy={busy}
                margin={props.margin}
                sx={props.sx}
                showOptionalIndicator={props.showOptionalIndicator}
                controlSx={props.controlSx}
                open={showSelectDepartmentDialog}
                dialogId={dialogId}
                hasValue={value != null}
                primaryText={value?.name ?? placeholder}
                secondaryText={departmentPath}
                leadingVisual={(
                    <Box
                        component="span"
                        sx={{
                            display: 'inline-flex',
                            color: isInteractionDisabled
                                ? 'text.disabled'
                                : value != null ? 'primary.main' : 'action.active',
                            '& .MuiSvgIcon-root': {fontSize: 20},
                        }}
                    >
                        {departmentIcon}
                    </Box>
                )}
                onOpen={() => setShowSelectDepartmentDialog(true)}
                onClear={() => onChange(null)}
            />

            <SelectDepartmentDialog
                id={dialogId}
                open={showSelectDepartmentDialog}
                title={dialogTitle}
                departments={departments}
                isDepartmentSelectable={isDepartmentSelectable}
                getDepartmentDisabledTooltip={getDepartmentDisabledTooltip}
                selectedDepartmentId={value?.id ?? null}
                onClose={() => setShowSelectDepartmentDialog(false)}
                onSelect={(department) => {
                    onChange(department);
                    setShowSelectDepartmentDialog(false);
                }}
            />
        </>
    );
}
