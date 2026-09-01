import React, {type ReactNode} from 'react';
import {
    Alert,
    Box,
    Button,
    Chip,
    Dialog,
    DialogActions,
    DialogContent,
    Link,
    Stack,
    Typography,
} from '@mui/material';
import {DialogTitleWithClose} from '../../components/dialog-title-with-close/dialog-title-with-close';
import {DocumentationLink} from '../../components/documentation-link/documentation-link';
import {MarkdownContent} from '../../components/markdown-content/markdown-content';
import {Permission} from '../../data/permissions/permission';
import {useRetainedDialogValue} from '../../hooks/use-retained-dialog-value';
import {useHasSystemPermission} from '../../modules/permissions/hooks/use-permissions';
import {type PluginDTO} from '../../services/plugins-api-service';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';

export interface PluginInfoDialogProps {
    open: boolean;
    plugin: PluginDTO | null;
    onClose: () => void;
}

interface PluginInfoRowProps {
    label: string;
    children: ReactNode;
}

export function PluginInfoDialog(props: PluginInfoDialogProps): ReactNode {
    const canReadPlugins = useHasSystemPermission(Permission.PLUGIN_READ);
    const renderPlugin = useRetainedDialogValue(props.open && canReadPlugins, props.plugin);
    const isDeprecated = isStringNotNullOrEmpty(renderPlugin?.deprecationNotice);

    return (
        <Dialog
            open={props.open && canReadPlugins && renderPlugin != null}
            onClose={props.onClose}
            fullWidth
            maxWidth="sm"
            scroll="paper"
        >
            <DialogTitleWithClose onClose={props.onClose}>
                Plugin-Informationen
            </DialogTitleWithClose>

            {
                canReadPlugins && renderPlugin != null &&
                <DialogContent
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 2.5,
                    }}
                >
                    <Box>
                        <Stack
                            direction="row"
                            spacing={1}
                            useFlexGap
                            sx={{
                                alignItems: 'center',
                                flexWrap: 'wrap',
                            }}
                        >
                            <Typography variant="h6">
                                {renderPlugin.name}
                            </Typography>
                            <Chip
                                size="small"
                                label={`Version ${renderPlugin.version}`}
                            />
                            <Chip
                                size="small"
                                label={isDeprecated ? 'Veraltet' : 'Aktiv'}
                                color={isDeprecated ? 'warning' : 'success'}
                                variant="outlined"
                            />
                        </Stack>

                        <Typography
                            variant="body2"
                            sx={{
                                color: 'text.secondary',
                                mt: 0.75,
                            }}
                        >
                            von {renderPlugin.vendorName}
                        </Typography>
                    </Box>

                    {
                        isDeprecated &&
                        <Alert severity="warning">
                            <MarkdownContent
                                markdown={renderPlugin.deprecationNotice}
                                sx={{typography: 'body2'}}
                            />
                        </Alert>
                    }

                    <MarkdownContent
                        markdown={renderPlugin.description}
                        sx={{
                            typography: 'body2',
                            color: 'text.secondary',
                        }}
                    />

                    <Box
                        component="dl"
                        sx={{
                            m: 0,
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 1.5,
                        }}
                    >
                        <PluginInfoRow label="Eindeutiger Schlüssel">
                            <Box component="span" sx={{fontFamily: 'monospace'}}>
                                {renderPlugin.key}
                            </Box>
                        </PluginInfoRow>

                        <PluginInfoRow label="Hersteller">
                            {renderPlugin.vendorName}
                        </PluginInfoRow>

                        <PluginInfoRow label="Webseite des Herstellers">
                            {
                                isStringNotNullOrEmpty(renderPlugin.vendorWebsite) ?
                                    <Link
                                        href={renderPlugin.vendorWebsite}
                                        target="_blank"
                                        rel="noopener noreferrer"
                                        underline="hover"
                                        sx={{overflowWrap: 'anywhere'}}
                                    >
                                        {renderPlugin.vendorWebsite}
                                    </Link> :
                                    'Nicht hinterlegt'
                            }
                        </PluginInfoRow>
                    </Box>

                    <DocumentationLink
                        url={renderPlugin.documentationUrl}
                        sx={{alignSelf: 'flex-start'}}
                    />
                </DialogContent>
            }

            <DialogActions sx={{justifyContent: 'flex-end'}}>
                <Button onClick={props.onClose}>
                    Schließen
                </Button>
            </DialogActions>
        </Dialog>
    );
}

function PluginInfoRow(props: PluginInfoRowProps): ReactNode {
    return (
        <Box sx={{minWidth: 0}}>
            <Typography
                component="dt"
                variant="caption"
                sx={{
                    color: 'text.secondary',
                    fontWeight: 600,
                }}
            >
                {props.label}
            </Typography>
            <Typography
                component="dd"
                variant="body2"
                sx={{
                    m: 0,
                    mt: 0.25,
                    overflowWrap: 'anywhere',
                }}
            >
                {props.children}
            </Typography>
        </Box>
    );
}
