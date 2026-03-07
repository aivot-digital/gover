import AltRoute from "@aivot/mui-material-symbols-400-outlined/dist/alt-route/AltRoute";
import Code from "@aivot/mui-material-symbols-400-outlined/dist/code/Code";
import FactCheck from "@aivot/mui-material-symbols-400-outlined/dist/fact-check/FactCheck";
import Webhook from "@aivot/mui-material-symbols-400-outlined/dist/webhook/Webhook";
import {SvgIconProps} from "@mui/material";
import {FC} from "react";
import RegularExpression from '@aivot/mui-material-symbols-400-outlined/dist/regular-expression/RegularExpression';

export const KnownProviderIcons: Record<string, FC<SvgIconProps>> = {
    'if': AltRoute,
    'webhook': Webhook,
    'js': Code,
    'no-code': RegularExpression,
    'check-and-update': FactCheck,
};
