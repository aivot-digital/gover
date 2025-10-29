import PersonOutlineOutlinedIcon from '@mui/icons-material/PersonOutlineOutlined';
import RemoveRedEyeOutlinedIcon from '@mui/icons-material/RemoveRedEyeOutlined';
import SwapHorizOutlinedIcon from '@mui/icons-material/SwapHorizOutlined';

import {ReactElement} from 'react';
import DraftOrders from '@aivot/mui-material-symbols-400-outlined/dist/draft-orders/DraftOrders';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import Inventory2 from '@aivot/mui-material-symbols-400-outlined/dist/inventory-2/Inventory2';
import Edit from '@aivot/mui-material-symbols-400-outlined/dist/edit/Edit';
import Unpublished from '@aivot/mui-material-symbols-400-outlined/dist/unpublished/Unpublished';
import PublishedWithChanges from '@aivot/mui-material-symbols-400-outlined/dist/published-with-changes/PublishedWithChanges';
import CheckCircle from '@aivot/mui-material-symbols-400-outlined/dist/check-circle/CheckCircle';

export enum FormStatus {
    Drafted = 0,
    Published = 1,
    Revoked = 2,
}

export const FormStatusLabels: Record<FormStatus, string> = {
    [FormStatus.Drafted]: 'Entwurf',
    [FormStatus.Published]: 'Veröffentlicht',
    [FormStatus.Revoked]: 'Zurückgezogen',
};

export const FormStatusDescriptions: Record<FormStatus, string> = {
    [FormStatus.Drafted]: 'Das Formular befindet sich im Entwurfsmodus und ist für Kunden nicht zugänglich.',
    [FormStatus.Published]: 'Das Formular ist veröffentlicht und für Kunden zugänglich.',
    [FormStatus.Revoked]: 'Das Formular wurde zurückgezogen und ist für Kunden nicht mehr zugänglich.',
};

export const FormStatusColors: Record<FormStatus, 'success' | 'warning' | 'error' | 'info'> = {
    [FormStatus.Drafted]: 'info',
    [FormStatus.Published]: 'success',
    [FormStatus.Revoked]: 'warning',
};

export const FormStatusIcons: Record<FormStatus, ReactElement> = {
    [FormStatus.Drafted]: <DraftOrders />,
    [FormStatus.Published]: <CheckCircle />,
    [FormStatus.Revoked]: <Unpublished />,
};

export const FormVersionStatusIcons: Record<FormStatus, ReactElement> = {
    [FormStatus.Drafted]: <Edit />,
    [FormStatus.Published]: ModuleIcons.forms,
    [FormStatus.Revoked]: <Inventory2 />,
};
