import {FC} from "react";
import {ProcessExportData} from "../../entities/process-export";
import {EmptyProcessTemplate} from "./empty-process-template";
import EventRepeat from "@aivot/mui-material-symbols-400-outlined/dist/event-repeat/EventRepeat";
import Automation from "@aivot/mui-material-symbols-400-outlined/dist/automation/Automation";
import {FormProcessTemplate} from "./form-process-template";
import {ApiProcessTemplate} from "./api-process-template";
import {ScheduledProcessTemplate} from "./scheduled-process-template";
import {SvgIconProps} from "@mui/material";
import Description from "@aivot/mui-material-symbols-400-outlined/dist/description/Description";
import Webhook from "@aivot/mui-material-symbols-400-outlined/dist/webhook/Webhook";

interface ProcessTemplate {
    name: string;
    description: string;
    Icon: FC<SvgIconProps>;
    data: ProcessExportData;
}

export const ProcessTemplates: ProcessTemplate[] = [
    {
        name: "Formularverfahren",
        description: "Ein Formularverfahren zur Erfassung von Daten über ein Webformular.",
        Icon: Description,
        data: FormProcessTemplate,
    },
    {
        name: "API-gesteuertes Verfahren",
        description: "Ein Verfahren, das über eine API Schnittstelle gesteuert wird.",
        Icon: Webhook,
        data: ApiProcessTemplate,
    },
    {
        name: "Periodisches Verfahren",
        description: "Ein Verfahren, das in regelmäßigen Abständen automatisch gestartet wird.",
        Icon: EventRepeat,
        data: ScheduledProcessTemplate,
    },
    {
        name: "Leeres Verfahren",
        description: "Ein leeres Verfahren ohne vordefinierte Schritte oder Logik.",
        Icon: Automation,
        data: EmptyProcessTemplate,
    },
];