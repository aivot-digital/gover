import React, {useEffect, useState} from 'react';
import {Box, Chip, Typography} from '@mui/material';
import ExtensionOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/Extension';
import OpenInNewOutlinedIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {ProsunaMarketplaceService} from '../../../services/prosuna-marketplace.service';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {isStringNotNullOrEmpty} from '../../../utils/string-utils';
import {type MarketplaceDetailModule} from '../../../models/entities/marketplace-detail-module';
import {type AnyElement} from '../../../models/elements/any-element';
import {cloneElement} from '../../../utils/clone-element';
import {type ReactNode} from 'react';
import {SelectionDetailsPanel} from '../../../components/selection-dialog/selection-details-panel';

export function MarketplaceModuleInfoTab({
    marketplaceModuleId,
    onAddElement,
    primaryActionLabel,
    primaryActionIcon,
    onClose,
}: {
    marketplaceModuleId: string,
    onAddElement: (element: AnyElement) => void,
    primaryActionLabel: string,
    primaryActionIcon: ReactNode,
    onClose: () => void,
}) {
    const [module, setModule] = useState<MarketplaceDetailModule>();
    const marketplaceKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.prosuna.marketplaceKey));

    useEffect(() => {
        if (marketplaceModuleId == null) {
            return;
        }

        ProsunaMarketplaceService.fetchModule(marketplaceModuleId, marketplaceKey)
            .then(setModule)
            .catch((err) => {
                console.error(err);
            });
    }, [marketplaceModuleId, marketplaceKey]);

    const handleAddElement = () => {
        if (module == null) {
            return;
        }

        Promise.all([
            ProsunaMarketplaceService.fetchModuleCode(module.id, module.current_version, marketplaceKey),
            ProsunaMarketplaceService.fetchModule(module.id, marketplaceKey),
        ])
            .then(([element, detailedModule]) => {
                const elementToAdd = cloneElement(element, true);
                elementToAdd.marketplaceLink = {
                    marketplaceId: detailedModule.id,
                    marketplaceVersion: detailedModule.current_version,
                };
                elementToAdd.name = detailedModule.title.substring(0, 30);
                onAddElement(elementToAdd);
            })
            .catch((err) => {
                console.error(err);
            });
    };

    if (module == null) {
        return (
            <Box sx={{p: 2.5}}>
                <LoadingPlaceholder/>
            </Box>
        );
    }

    return (
        <SelectionDetailsPanel
            icon={<ExtensionOutlinedIcon sx={{fontSize: 20, color: 'text.secondary'}}/>}
            label="Prosuna Marktplatz"
            title={module.title}
            titleAdornment={(
                <Chip
                    size="small"
                    label={`Version ${module.current_version}`}
                    sx={{flexShrink: 0}}
                />
            )}
            description={module.description_short}
            primaryActionLabel={primaryActionLabel}
            primaryActionIcon={primaryActionIcon}
            onPrimaryAction={handleAddElement}
            onClose={onClose}
        >
            <ModuleInfoSection title="Allgemein">
                <ModuleInfoRow label="Organisation" value={`@${module.organization}`}/>
                <ModuleInfoRow label="Sichtbarkeit" value={module.is_public ? 'Öffentlich' : 'Privat'}/>
                {
                    isStringNotNullOrEmpty(module.datenfeld_id) &&
                    <ModuleInfoRow label="Datenfeld" value={`Datenfeld ${module.datenfeld_id}`}/>
                }
            </ModuleInfoSection>

            <ModuleInfoSection title="Beschreibung">
                <Typography variant="body2">
                    {module.description}
                </Typography>
            </ModuleInfoSection>

            {
                isStringNotNullOrEmpty(module.recent_changes) &&
                <ModuleInfoSection title="Letzte Änderungen">
                    <Typography variant="body2">
                        {module.recent_changes}
                    </Typography>
                </ModuleInfoSection>
            }

            <ModuleInfoSection title="Weiterführende Links">
                {
                    module.is_public &&
                    <Typography
                        component="a"
                        variant="body2"
                        href={`https://marketplace.prosuna.de/modules/${module.id}/`}
                        target="_blank"
                        rel="noreferrer noopener"
                        sx={{
                            color: 'primary.main',
                            textDecoration: 'none',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 0.75,
                        }}
                    >
                        <OpenInNewOutlinedIcon sx={{fontSize: 16}}/>
                        Im Marktplatz anzeigen
                    </Typography>
                }

                <Typography
                    component="a"
                    variant="body2"
                    href={`https://marketplace.prosuna.de/organizations/${module.organization_id}/`}
                    target="_blank"
                    rel="noreferrer noopener"
                    sx={{
                        color: 'primary.main',
                        textDecoration: 'none',
                        display: 'inline-flex',
                        alignItems: 'center',
                        gap: 0.75,
                    }}
                >
                    <OpenInNewOutlinedIcon sx={{fontSize: 16}}/>
                    Organisation öffnen
                </Typography>

                {
                    isStringNotNullOrEmpty(module.datenfeld_id) &&
                    <Typography
                        component="a"
                        variant="body2"
                        href={`https://fimportal.de/detail/D/${module.datenfeld_id}`}
                        target="_blank"
                        rel="noreferrer noopener"
                        sx={{
                            color: 'primary.main',
                            textDecoration: 'none',
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: 0.75,
                        }}
                    >
                        <OpenInNewOutlinedIcon sx={{fontSize: 16}}/>
                        Datenfeld öffnen
                    </Typography>
                }
            </ModuleInfoSection>
        </SelectionDetailsPanel>
    );
}

function ModuleInfoSection({
    title,
    children,
}: {
    title: string;
    children: React.ReactNode;
}) {
    return (
        <Box>
            <Typography
                variant="subtitle2"
                sx={{
                    mb: 1.25,
                    fontWeight: 700,
                }}
            >
                {title}
            </Typography>
            <Box
                sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 1.25,
                }}
            >
                {children}
            </Box>
        </Box>
    );
}

function ModuleInfoRow({
    label,
    value,
}: {
    label: string;
    value: string;
}) {
    return (
        <Box sx={{py: 0.25}}>
            <Typography variant="caption" color="text.secondary">
                {label}
            </Typography>
            <Typography variant="body2" sx={{mt: 0.25}}>
                {value}
            </Typography>
        </Box>
    );
}
