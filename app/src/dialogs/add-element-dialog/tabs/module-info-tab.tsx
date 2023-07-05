import React, {useEffect, useState} from "react";
import {Box, DialogContent, Divider, IconButton, Tooltip, Typography} from "@mui/material";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faClose, faExternalLink, faLock} from "@fortawesome/pro-light-svg-icons";
import {useAppSelector} from "../../../hooks/use-app-selector";
import {selectSystemConfigValue} from "../../../slices/system-config-slice";
import {SystemConfigKeys} from "../../../data/system-config-keys";
import {GoverStoreService} from "../../../services/gover-store.service";
import {
    LoadingPlaceholderComponentView
} from "../../../components/static-components/loading-placeholder/loading-placeholder.component.view";
import {isStringNotNullOrEmpty} from "../../../utils/string-utils";
import {StoreDetailModule} from "../../../models/entities/store-detail-module";


export function ModuleInfoTab({moduleId, onClose}: { moduleId: string, onClose: () => void }) {
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
        <DialogContent>

            {
                module == null &&
                <LoadingPlaceholderComponentView/>
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
                                <FontAwesomeIcon
                                    icon={faLock}
                                    style={{marginRight: '0.5em'}}
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
                                    <FontAwesomeIcon icon={faExternalLink}/>
                                </IconButton>
                            </Tooltip>
                        }

                        <Tooltip title="Schließen">
                            <IconButton
                                onClick={handleClose}
                                size="small"
                                sx={{ml: 'auto'}}
                            >
                                <FontAwesomeIcon icon={faClose}/>
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