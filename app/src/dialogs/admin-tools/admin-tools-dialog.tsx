import React from 'react';
import {
    Box,
    Dialog,
    DialogContent,
    FormControlLabel,
    FormGroup,
    FormHelperText,
    Switch,
    Typography
} from '@mui/material';
import {useDispatch, useSelector} from 'react-redux';
import {AppDispatch, RootState} from '../../store';
import {
    AdminSettingsState,
    toggleShowDebugOutput,
    toggleShowUserInput,
    toggleValidation,
    toggleVisibility
} from '../../slices/admin-settings-slice';
import {DialogTitleWithClose} from '../../components/static-components/dialog-title-with-close/dialog-title-with-close';
import {AdminToolsDialogProps} from './admin-tools-dialog-props';
import {Localization} from "../../locale/localization";
import strings from "./admin-tools-dialog-strings.json";

const _ = Localization(strings);

const switches: {
    label: string;
    hint: string;
    onToggle: (dispatch: AppDispatch) => void;
    isActive: (settings: AdminSettingsState) => boolean;
}[] = [
    {
        label: _.validateSwitchLabel,
        hint: _.validateSwitchHint,
        onToggle: dispatch => dispatch(toggleValidation()),
        isActive: settings => !settings.disableValidation,
    },
    {
        label: _.visibilitySwitchLabel,
        hint: _.visibilitySwitchHint,
        onToggle: dispatch => dispatch(toggleVisibility()),
        isActive: settings => !settings.disableVisibility,
    },
    {
        label: _.debugSwitchLabel,
        hint: _.debugSwitchHint,
        onToggle: dispatch => dispatch(toggleShowDebugOutput()),
        isActive: settings => settings.showDebugOutput,
    },
    {
        label: _.userInputSwitchLabel,
        hint: _.userInputSwitchHint,
        onToggle: dispatch => dispatch(toggleShowUserInput()),
        isActive: settings => settings.showUserInput,
    },
];

export function AdminToolsDialog({open, onClose}: AdminToolsDialogProps) {
    const dispatch = useDispatch();

    const adminSettings = useSelector((state: RootState) => state.adminSettings);

    return (
        <>
            <Dialog
                open={open}
                onClose={onClose}
                fullWidth
            >
                <DialogTitleWithClose
                    id="admin-tools-dialog-title"
                    onClose={onClose}
                    closeTooltip={_.close}
                >
                    {_.title}
                </DialogTitleWithClose>

                <DialogContent>
                    <Typography variant="body1">
                        {_.hint}
                    </Typography>

                    <Box sx={{mt: 3}}>
                        {
                            switches.map(swtch => (
                                <FormGroup key={swtch.label}>
                                    <FormControlLabel
                                        control={<Switch
                                            checked={swtch.isActive(adminSettings)}
                                            onChange={() => swtch.onToggle(dispatch)}
                                        />}
                                        label={swtch.label}
                                    />

                                    <FormHelperText>
                                        {swtch.hint}
                                    </FormHelperText>
                                </FormGroup>
                            ))
                        }
                    </Box>
                </DialogContent>
            </Dialog>
        </>
    );
}
