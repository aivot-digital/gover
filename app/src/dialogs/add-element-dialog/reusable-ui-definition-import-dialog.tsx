import React from 'react';
import {Button, Dialog, DialogActions, DialogContent, Typography} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {DisabledTooltip} from '../../components/disabled-tooltip/disabled-tooltip';
import {type ReusableUiDefinitionOption} from './reusable-ui-definition-utils';
import {useRetainedDialogValue} from '../../hooks/use-retained-dialog-value';

interface ReusableUiDefinitionImportDialogProps {
    option?: ReusableUiDefinitionOption;
    onImportAsGroup: () => void;
    onImportFlat: () => void;
    onClose: () => void;
}

export function ReusableUiDefinitionImportDialog(
    props: ReusableUiDefinitionImportDialogProps,
): React.ReactNode {
    const renderOption = useRetainedDialogValue(props.option != null, props.option);
    const groupAction = (
        <Button
            variant="contained"
            onClick={props.onImportAsGroup}
            disabled={renderOption?.groupDisabledReason != null}
        >
            Als Gruppe einfügen
        </Button>
    );
    const flatAction = (
        <Button
            variant="outlined"
            onClick={props.onImportFlat}
            disabled={renderOption?.flatDisabledReason != null}
        >
            Flach einfügen
        </Button>
    );

    return (
        <Dialog
            open={props.option != null}
            onClose={props.onClose}
            fullWidth
            maxWidth="sm"
        >
            <DialogTitleWithClose onClose={props.onClose}>
                Teilbereich einfügen
            </DialogTitleWithClose>

            <DialogContent>
                <Typography>
                    Möchten Sie „{renderOption?.definition.label}“ als Gruppe oder ohne Gruppen-Wrapper einfügen?
                </Typography>
                <Typography variant="body2" sx={{mt: 1, color: 'text.secondary'}}>
                    Die Überschrift und alle enthaltenen Elemente bleiben in beiden Varianten erhalten.
                </Typography>
            </DialogContent>

            <DialogActions sx={{justifyContent: 'flex-start'}}>
                <DisabledTooltip
                    disabled={renderOption?.groupDisabledReason != null}
                    title={renderOption?.groupDisabledReason}
                >
                    {groupAction}
                </DisabledTooltip>
                <DisabledTooltip
                    disabled={renderOption?.flatDisabledReason != null}
                    title={renderOption?.flatDisabledReason}
                >
                    {flatAction}
                </DisabledTooltip>
                <Button onClick={props.onClose}>
                    Abbrechen
                </Button>
            </DialogActions>
        </Dialog>
    );
}
