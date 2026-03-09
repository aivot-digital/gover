import React, {useEffect, useState} from 'react';
import {Box, Button, Chip, Typography} from '@mui/material';
import ExtensionOutlinedIcon from '@mui/icons-material/ExtensionOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import {GoverStoreService} from '../../../services/gover-store.service';
import {LoadingPlaceholder} from '../../../components/loading-placeholder/loading-placeholder';
import {isStringNotNullOrEmpty} from '../../../utils/string-utils';
import {type StoreDetailModule} from '../../../models/entities/store-detail-module';
import {type AnyElement} from '../../../models/elements/any-element';
import {cloneElement} from '../../../utils/clone-element';
import {type ReactNode} from 'react';

export function ModuleInfoTab({
    moduleId,
    onAddElement,
    primaryActionLabel,
    primaryActionIcon,
    onClose,
}: {
    moduleId: string,
    onAddElement: (element: AnyElement) => void,
    primaryActionLabel: string,
    primaryActionIcon: ReactNode,
    onClose: () => void,
}) {
    const [module, setModule] = useState<StoreDetailModule>();
    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));

    useEffect(() => {
        if (moduleId == null) {
            return;
        }

        GoverStoreService.fetchModule(moduleId, storeKey)
            .then(setModule)
            .catch((err) => {
                console.error(err);
            });
    }, [moduleId, storeKey]);

    const handleAddElement = () => {
        if (module == null) {
            return;
        }

        Promise.all([
            GoverStoreService.fetchModuleCode(module.id, module.current_version, storeKey),
            GoverStoreService.fetchModule(module.id, storeKey),
        ])
            .then(([element, detailedModule]) => {
                const elementToAdd = cloneElement(element, true);
                elementToAdd.storeLink = {
                    storeId: detailedModule.id,
                    storeVersion: detailedModule.current_version,
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
        <>
            <Box
                sx={{
                    p: 2,
                    borderBottom: '1px solid',
                    borderColor: 'divider',
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 2,
                    }}
                >
                    <Box
                        sx={{
                            width: 38,
                            height: 38,
                            borderRadius: '50%',
                            bgcolor: 'grey.100',
                            display: 'inline-flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            flexShrink: 0,
                        }}
                    >
                        <ExtensionOutlinedIcon sx={{fontSize: 20, color: 'text.secondary'}}/>
                    </Box>

                    <Box sx={{minWidth: 0, flex: 1}}>
                        <Typography
                            variant="caption"
                            sx={{
                                display: 'block',
                                lineHeight: 1.2,
                                mt: 0.5,
                            }}
                        >
                            Gover Marktplatz
                        </Typography>

                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1,
                                minWidth: 0,
                            }}
                        >
                            <Typography
                                variant="h6"
                                lineHeight={1.2}
                                sx={{
                                    flex: 1,
                                    minWidth: 0,
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    whiteSpace: 'nowrap',
                                }}
                            >
                                {module.title}
                            </Typography>
                            <Chip
                                size="small"
                                label={`Version ${module.current_version}`}
                                sx={{flexShrink: 0}}
                            />
                        </Box>
                    </Box>
                </Box>
            </Box>

            <Box
                sx={{
                    flex: 1,
                    overflowY: 'auto',
                    px: 2.25,
                    pt: 2.25,
                    pb: 3.75,
                    display: 'flex',
                    flexDirection: 'column',
                    gap: 2.5,
                }}
            >
                <Typography variant="body2" color="text.secondary">
                    {module.description_short}
                </Typography>

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
                            href={`https://store.gover.digital/modules/${module.id}/`}
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
                        href={`https://store.gover.digital/organizations/${module.organization_id}/`}
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
            </Box>

            <Box
                sx={{
                    px: 2,
                    pt: 2,
                    pb: 2.5,
                    borderTop: '1px solid',
                    borderColor: 'divider',
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 1,
                }}
            >
                <Button
                    variant="contained"
                    startIcon={primaryActionIcon}
                    onClick={handleAddElement}
                >
                    {primaryActionLabel}
                </Button>
                <Button
                    variant="text"
                    onClick={onClose}
                >
                    Details schließen
                </Button>
            </Box>
        </>
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
