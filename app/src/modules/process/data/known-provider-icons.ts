import AltRoute from "@aivot/mui-material-symbols-400-outlined/dist/alt-route/AltRoute";
import Code from "@aivot/mui-material-symbols-400-outlined/dist/code/Code";
import FactCheck from "@aivot/mui-material-symbols-400-outlined/dist/fact-check/FactCheck";
import Webhook from "@aivot/mui-material-symbols-400-outlined/dist/webhook/Webhook";
import {SvgIconProps} from "@mui/material";
import {FC} from "react";
import RegularExpression from '@aivot/mui-material-symbols-400-outlined/dist/regular-expression/RegularExpression';
import LineEndSquare from '@aivot/mui-material-symbols-400-outlined/dist/line-end-square/LineEndSquare';
import Mail from '@aivot/mui-material-symbols-400-outlined/dist/mail/Mail';
import EditDocument from '@aivot/mui-material-symbols-400-outlined/dist/edit-document/EditDocument';
import Api from '@aivot/mui-material-symbols-400-outlined/dist/api/Api';
import RuleFolder from '@aivot/mui-material-symbols-400-outlined/dist/rule-folder/RuleFolder';
import {CompareArrows} from '@mui/icons-material';
import InputCircle from '@aivot/mui-material-symbols-400-outlined/dist/input-circle/InputCircle';

export const KnownProviderIcons: Record<string, FC<SvgIconProps>> = {
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
};
