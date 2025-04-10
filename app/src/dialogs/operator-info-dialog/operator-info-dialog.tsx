import {OperatorInfoDialogProps} from './operator-info-dialog-props';
import {InfoDialog} from '../info-dialog/info-dialog';
import React from 'react';
import {OperatorInfo} from '../../components/operator-info/operator-info';

export function OperatorInfoDialog(props: OperatorInfoDialogProps) {
    return (
        <InfoDialog
            open={props.operator != null}
            title={props.operator?.label ?? ''}
            onClose={props.onClose}
            severity="info"
        >
            {
                props.operator != null &&
                <OperatorInfo operator={props.operator} />
            }
        </InfoDialog>
    );
}