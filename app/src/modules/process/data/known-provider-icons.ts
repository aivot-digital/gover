import AltRoute from '@aivot/mui-material-symbols-400-n25-outlined/AltRoute';
import Code from '@aivot/mui-material-symbols-400-n25-outlined/Code';
import FactCheck from '@aivot/mui-material-symbols-400-n25-outlined/FactCheck';
import Webhook from '@aivot/mui-material-symbols-400-n25-outlined/Webhook';
import {type SvgIconComponent} from '../../../types/svg-icon-component';
import RegularExpression from '@aivot/mui-material-symbols-400-n25-outlined/RegularExpression';
import LineEndSquare from '@aivot/mui-material-symbols-400-n25-outlined/LineEndSquare';
import Mail from '@aivot/mui-material-symbols-400-n25-outlined/Mail';
import EditDocument from '@aivot/mui-material-symbols-400-n25-outlined/EditDocument';
import Api from '@aivot/mui-material-symbols-400-n25-outlined/Api';
import RuleFolder from '@aivot/mui-material-symbols-400-n25-outlined/RuleFolder';
import InputCircle from '@aivot/mui-material-symbols-400-n25-outlined/InputCircle';
import ApprovalDelegation from '@aivot/mui-material-symbols-400-n25-outlined/ApprovalDelegation';
import Counter1 from '@aivot/mui-material-symbols-400-n25-outlined/Counter1';
import EditNote from '@aivot/mui-material-symbols-400-n25-outlined/EditNote';
import AdsClick from '@aivot/mui-material-symbols-400-n25-outlined/AdsClick';

export const KnownProviderIcons: Record<string, SvgIconComponent> = {
    'if': AltRoute,
    'webhook': Webhook,
    'js': Code,
    'no-code': RegularExpression,
    'check-and-update': FactCheck,
    'data_mapping': InputCircle,
    'http_request': Api,
    'mail': Mail,
    'pdf': EditDocument,
    'data_type_validation': RuleFolder,
    'default-termination': LineEndSquare,
    'approval': ApprovalDelegation,
    'counter': Counter1,
    'data_change': EditNote,
    'manual_action': AdsClick,
};
