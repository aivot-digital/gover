import React, {useEffect, useState} from "react";
import {Box, DialogContent, Divider, IconButton, Tooltip, Typography} from "@mui/material";
import {useAppSelector} from "../../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../../slices/system-config-slice";
import {SystemConfigKeys} from "../../../data/system-config-keys";
import {GoverStoreService} from "../../../services/gover-store.service";
import {LoadingPlaceholder} from "../../../components/loading-placeholder/loading-placeholder";
import {isStringNotNullOrEmpty} from "../../../utils/string-utils";
import {StoreDetailModule} from "../../../models/entities/store-detail-module";
import CloseOutlinedIcon from '@mui/icons-material/CloseOutlined';
import OpenInNewOutlinedIcon from '@mui/icons-material/OpenInNewOutlined';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';

export function ModuleInfoTab({
                                  moduleId,
                                  onClose,
                              }: {moduleId: string, onClose: () => void}) {
    const [module, setModule] = useState<StoreDetailModule>();
    const storeKey = useAppSelector(selectSystemConfigValue(SystemConfigKeys.gover.storeKey));

    useEffect(() => {
        if (moduleId != null) {
            GoverStoreService.fetchModule(moduleId, storeKey)
                .then(setModule)
                .catch(err => {
                    console.error(err);
                });
        }
    }, [moduleId]);

    const handleClose = () => {
        setModule(undefined);
        onClose();
    };

    return (
        <DialogContent tabIndex={0}>

            {
                module == null &&
                <LoadingPlaceholder/>
            }

            {
                module != null &&
                <>
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        {
                            !module.is_public &&
                            <Tooltip title="Privater Baustein">
                                <LockOutlinedIcon
                                    fontSize="small"
                                    sx={{
                                        marginRight: '0.25em',
                                    }}
                                />
                            </Tooltip>
                        }

                        <Typography
                            variant="h6"
                            component="h3"
                        >
                            {module.title}
                        </Typography>

                        <Typography
                            variant="caption"
                            sx={{ml: 1}}
                        >
                            {module.current_version}
                        </Typography>

                        {
                            module.is_public &&
                            <Tooltip title="Im Store Anzeigen">
                                <IconButton
                                    onClick={handleClose}
                                    size="small"
                                    component="a"
                                    sx={{ml: 2}}
                                    href={`https://store.gover.digital/modules/${module.id}/`}
                                    target="_blank"
                                >
                                    <OpenInNewOutlinedIcon/>
                                </IconButton>
                            </Tooltip>
                        }

                        <Tooltip title="Schließen">
                            <IconButton
                                onClick={handleClose}
                                size="small"
                                sx={{ml: 'auto'}}
                            >
                                <CloseOutlinedIcon/>
                            </IconButton>
                        </Tooltip>

                    </Box>
                    <Box
                        sx={{
                            display: 'flex',
                            alignItems: 'center',
                        }}
                    >
                        <a
                            href={`https://store.gover.digital/organizations/${module.organization_id}/`}
                            target="_blank"
                        >
                            @{module.organization}
                        </a>
                    </Box>

                    {
                        module.datenfeld_id != null &&
                        isStringNotNullOrEmpty(module.datenfeld_id) &&
                        <Box
                            sx={{
                                display: 'flex',
                                alignItems: 'center',
                            }}
                        >
                            <a
                                href={`https://fimportal.de/detail/D/${module.datenfeld_id}`}
                                target="_blank"
                                rel="noreferrer noopener"
                            >
                                Datenfeld {module.datenfeld_id}
                            </a>
                        </Box>
                    }

                    <Divider sx={{my: 4}}>
                        Informationen
                    </Divider>

                    <Box>
                        <Typography variant="body1">
                            {module.description}
                        </Typography>
                    </Box>

                    {
                        isStringNotNullOrEmpty(module.recent_changes) &&
                        <>
                            <Divider sx={{my: 4}}>
                                Letzte Änderungen
                            </Divider>

                            <Box>
                                <Typography variant="body1">
                                    {module.recent_changes}
                                </Typography>
                            </Box>
                        </>
                    }
                </>
            }
        </DialogContent>
    );
}
