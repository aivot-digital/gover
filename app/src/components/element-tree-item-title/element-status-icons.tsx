import {AnyElement} from '../../models/elements/any-element';
import {IconButton, Tooltip} from '@mui/material';
import React, {ReactNode, useMemo} from 'react';
import {FunctionTypeLabels, getFunctionStatus} from '../../utils/function-status-utils';
import GradingOutlinedIcon from '@mui/icons-material/GradingOutlined';
import InventoryOutlinedIcon from '@mui/icons-material/InventoryOutlined';
import {isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import {hasUntestedChild} from '../../utils/has-untested-child';
import ChecklistOutlinedIcon from '@mui/icons-material/ChecklistOutlined';
import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import {checkId} from '../../utils/id-utils';
import ReportOutlinedIcon from '@mui/icons-material/ReportOutlined';
import OfflineBoltOutlinedIcon from '@mui/icons-material/OfflineBoltOutlined';
import {generateComponentTitle} from '../../utils/generate-component-title';
import BuildCircleOutlinedIcon from '@mui/icons-material/BuildCircleOutlined';
import HelpOutlineOutlinedIcon from '@mui/icons-material/HelpOutlineOutlined';
import AccountCircleOutlinedIcon from '@mui/icons-material/AccountCircleOutlined';
import {useAppSelector} from '../../hooks/use-app-selector';
import {selectAllElements, selectFunctionReferences, selectLoadedForm} from '../../slices/app-slice';
import {selectUseTestMode, selectWarnDuplicateIds} from '../../slices/admin-settings-slice';

interface ElementStatusIconsProps {
    element: AnyElement;
}

interface StatusIcon {
    icon: ReactNode;
    tooltip: string;
}

export function ElementStatusIcons(props: ElementStatusIconsProps) {
    const form = useAppSelector(selectLoadedForm);
    const references = useAppSelector(selectFunctionReferences);
    const allElements = useAppSelector(selectAllElements);
    const useTestMode = useAppSelector(selectUseTestMode);
    const warnDuplicateIds = useAppSelector(selectWarnDuplicateIds);

    const icons = useMemo(() => {
        if (references == null || allElements == null) {
            return [];
        }

        const icons: StatusIcon[] = [];
        const functionStatus = getFunctionStatus(props.element);

        if (useTestMode) {
            const professionalTestMissing = props.element.testProtocolSet?.professionalTest == null;
            const technicalTestMissing = functionStatus.length > 0 && props.element.testProtocolSet?.technicalTest == null;

            if (professionalTestMissing) {
                icons.push({
                    icon: <GradingOutlinedIcon color="warning" />,
                    tooltip: 'Fachliche Prüfung ausstehend',
                });
            }

            if (technicalTestMissing) {
                icons.push({
                    icon: <InventoryOutlinedIcon color="warning" />,
                    tooltip: 'Technische Prüfung ausstehend',
                });
            }

            if (!professionalTestMissing && !technicalTestMissing && isAnyElementWithChildren(props.element)) {
                if (hasUntestedChild(props.element)) {
                    icons.push({
                        icon: <ChecklistOutlinedIcon color="warning" />,
                        tooltip: 'Prüfung für Kind-Element ausstehend',
                    });
                }
            }
        }

        if ('technical' in props.element && props.element.technical) {
            icons.push({
                icon: <VisibilityOffOutlinedIcon />,
                tooltip: 'Technisches Feld (im Formular nicht sichtbar)',
            });
        }

        if (warnDuplicateIds && form != null) {
            const msg = checkId(form.root, props.element.id);
            if (msg != null) {
                icons.push({
                    icon: <ReportOutlinedIcon color="error" />,
                    tooltip: msg,
                });
            }
        }

        const referencesToThisElement = references.filter(ref => ref.target.id === props.element.id);
        if (referencesToThisElement.length > 0) {
            icons.push({
                icon: <OfflineBoltOutlinedIcon />,
                tooltip: 'Von Funktion referenziert: ' + referencesToThisElement.map(ref => `${generateComponentTitle(ref.source)} (${FunctionTypeLabels[ref.functionType]})`).join(', '),
            });
        }

        if (functionStatus.length > 0) {
            if (functionStatus.every((s) => s.status === 'done')) {
                icons.push({
                    icon: <BuildCircleOutlinedIcon />,
                    tooltip: 'Individuelle Funktionen definiert',
                });
            } else if (functionStatus.some((s) => s.status === 'todo')) {
                icons.push({
                    icon: <BuildCircleOutlinedIcon color="error" />,
                    tooltip: 'Individuelle Funktionen erforderlich',
                });
            } else if (functionStatus.some((s) => s.status === 'unnecessary')) {
                icons.push({
                    icon: <HelpOutlineOutlinedIcon color="error" />,
                    tooltip: 'Individuelle Funktionen definiert aber keine Anforderung vorhanden',
                });
            }
        }

        if (props.element.metadata && Object.values(props.element.metadata).length !== 0) {
            if ((props.element.metadata?.bundIdMapping || props.element.metadata?.bayernIdMapping || props.element.metadata?.shIdMapping || props.element.metadata?.mukMapping)) {
                icons.push({
                    icon: <AccountCircleOutlinedIcon />,
                    tooltip: 'Verknüpfung mit Nutzerkonto vorhanden',
                });
            }
        }

        return icons;
    }, [props.element, allElements, references, useTestMode, warnDuplicateIds, form]);

    return (
        <>
            {
                icons.map((icon) => (
                    <Tooltip
                        title={icon.tooltip}
                        arrow
                        key={icon.tooltip}
                        sx={{
                            cursor: 'help',
                        }}
                    >
                        <IconButton
                            disableRipple
                        >
                            {icon.icon}
                        </IconButton>
                    </Tooltip>
                ))
            }
        </>
    );
}