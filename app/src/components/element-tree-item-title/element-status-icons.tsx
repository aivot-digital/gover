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
import {IdentityProviderInfo} from '../../modules/identity/models/identity-provider-info';
import {ElementMetadata} from '../../models/elements/element-metadata';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {getMetadataMapping} from '../../utils/prefill-elements';

interface ElementStatusIconsProps {
    element: AnyElement;
    enabledIdentityProviderInfos: IdentityProviderInfo[];
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

    const {
        element,
        enabledIdentityProviderInfos,
    } = props;

    const icons = useMemo(() => {
        if (references == null || allElements == null) {
            return [];
        }

        const icons: StatusIcon[] = [];
        const functionStatus = getFunctionStatus(element);

        if (useTestMode) {
            const testProtocolSet = element.testProtocolSet ?? {};

            const professionalTestMissing = testProtocolSet.professionalTest == null;
            const technicalTestMissing = functionStatus.length > 0 && testProtocolSet.technicalTest == null;

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

            if (!professionalTestMissing && !technicalTestMissing && isAnyElementWithChildren(element)) {
                if (hasUntestedChild(element)) {
                    icons.push({
                        icon: <ChecklistOutlinedIcon color="warning" />,
                        tooltip: 'Prüfung für Kind-Element ausstehend',
                    });
                }
            }
        }

        if ('technical' in element && element.technical) {
            icons.push({
                icon: <VisibilityOffOutlinedIcon />,
                tooltip: 'Technisches Feld (im Formular nicht sichtbar)',
            });
        }

        if (warnDuplicateIds && form != null) {
            const msg = checkId(form.root, element.id);
            if (msg != null) {
                icons.push({
                    icon: <ReportOutlinedIcon color="error" />,
                    tooltip: msg,
                });
            }
        }

        const referencesToThisElement = references.filter(ref => ref.target.id === element.id);
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

        if (isAnyInputElement(element)) {
            const mappedIdentityProviders: string[] = [];
            for (const identityProviderInfo of enabledIdentityProviderInfos) {
                const isMapped = getMetadataMapping(element, identityProviderInfo.metadataIdentifier) != null;
                if (isMapped) {
                    mappedIdentityProviders.push(identityProviderInfo.name);
                }
            }
            if (mappedIdentityProviders.length > 0) {
                icons.push({
                    icon: <AccountCircleOutlinedIcon />,
                    tooltip: 'Verknüpfung mit Nutzerkontenanbieter vorhanden: ' + mappedIdentityProviders.join(', '),
                });
            }
        }

        return icons;
    }, [element, allElements, references, useTestMode, warnDuplicateIds, form, enabledIdentityProviderInfos]);

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