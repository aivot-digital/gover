import {Box, Button, Divider, Grid, Link as MuiLink, Typography} from '@mui/material';
import {useEffect, useMemo, useState} from 'react';
import {Link as RouterLink} from 'react-router-dom';
import OpenInNew from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import ArrowForward from '@aivot/mui-material-symbols-400-n25-outlined/ArrowForward';
import MenuBook from '@aivot/mui-material-symbols-400-n25-outlined/MenuBook';
import SupportAgent from '@aivot/mui-material-symbols-400-n25-outlined/SupportAgent';
import ReadinessScore from '@aivot/mui-material-symbols-400-n25-outlined/ReadinessScore';
import {useApi} from '../../../hooks/use-api';
import {CustomLinksApiService} from '../../custom-links/custom-links-api-service';
import {type CustomLink, CustomLinkType} from '../../custom-links/models/custom-link';
import {getCustomLinkIcon} from '../../custom-links/data/custom-link-icons';
import {type SvgIconComponent} from '../../../types/svg-icon-component';
import {DashboardPanel} from './dashboard-panel';
import {ModuleIcons} from '../../../shells/staff/data/module-icons';
import {Permission} from '../../../data/permissions/permission';
import {useHasSystemPermission} from '../../permissions/hooks/use-permissions';
import Balancer from 'react-wrap-balancer';
import Link2 from '@aivot/mui-material-symbols-400-n25-outlined/Link2';

interface CustomLinkCardProps {
    actionLabel: string;
    category: string;
    description: string;
    external: boolean;
    icon: SvgIconComponent;
    title: string;
    url: string;
}

const actionSx = {
    display: 'flex',
    alignItems: 'center',
    px: 2.25,
    py: 1.4,
    color: 'text.primary',
    fontSize: '0.875rem',
    fontWeight: 600,
    textDecoration: 'none',
    '&:hover': {
        bgcolor: 'action.hover',
        textDecoration: 'none',
    },
};

const fixedCardContentSx = {
    width: '100%',
    maxWidth: 460,
    mx: 'auto',
};

function CustomLinkCard(props: CustomLinkCardProps) {
    const Icon = props.icon;
    const actionContent = (
        <Box sx={{...fixedCardContentSx, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 1}}>
            <span>{props.actionLabel}</span>
            {props.external
                ? <OpenInNew sx={{fontSize: 17, color: 'text.disabled'}}/>
                : <ArrowForward sx={{fontSize: 18, color: 'text.secondary'}}/>}
        </Box>
    );

    return (
        <DashboardPanel sx={{width: '100%', height: '100%', display: 'flex', flexDirection: 'column', overflow: 'hidden'}}>
            <Box sx={{p: 2.25, minHeight: 150}}>
                <Box sx={fixedCardContentSx}>
                    <Box sx={{display: 'flex', alignItems: 'center', gap: 1}}>
                        <Icon sx={{fontSize: 20, color: 'text.secondary'}}/>
                        <Typography
                            variant="caption"
                            sx={{
                                color: "text.secondary",
                                fontWeight: 650
                            }}>
                            {props.category}
                        </Typography>
                    </Box>
                    <Typography component="h3" sx={{mt: 1.75, fontSize: '1rem', lineHeight: 1.35, fontWeight: 650}}>
                        {props.title}
                    </Typography>
                    <Typography
                        variant="body2"
                        sx={{
                            color: "text.secondary",
                            mt: 0.75
                        }}>
                        <Balancer>{props.description}</Balancer>
                    </Typography>
                </Box>
            </Box>
            <Divider sx={{mt: 'auto'}}/>
            {props.external ? (
                <MuiLink href={props.url} target="_blank" rel="noopener noreferrer" sx={actionSx}>
                    {actionContent}
                </MuiLink>
            ) : (
                <MuiLink
                    component={RouterLink}
                    to={props.url}
                    sx={actionSx}
                >
                    {actionContent}
                </MuiLink>
            )}
        </DashboardPanel>
    );
}

function OrganizationLinksPanel({links, canCreate}: {links: CustomLink[]; canCreate: boolean}) {
    if (links.length === 0 && !canCreate) return null;

    return (
        <Box component="section" aria-labelledby="organization-links-title" sx={{mb: 3}}>
            <DashboardPanel sx={{overflow: 'hidden'}}>
                <Box sx={{display: 'flex', alignItems: 'center', gap: 1.5, px: {xs: 2, sm: 2.75}, py: 2.25}}>
                    <Box sx={{width: 40, height: 40, display: 'grid', placeItems: 'center', borderRadius: '50%', bgcolor: 'action.hover', color: 'text.secondary', flexShrink: 0}}>
                        <Link2/>
                    </Box>
                    <Box>
                        <Typography id="organization-links-title" variant="h6" component="h2">
                            Relevante Links
                        </Typography>
                        <Typography variant="body2" sx={{
                            color: "text.secondary"
                        }}>
                            Interne Informationen und häufig genutzte Dienste
                        </Typography>
                    </Box>
                </Box>
                <Divider/>
                {links.length === 0 ? (
                    <Box sx={{px: 3, py: 4, textAlign: 'center'}}>
                        <Typography sx={{
                            fontWeight: 650
                        }}>
                            <Balancer>Für die Übersicht sind noch keine zusätzlichen Links eingerichtet.</Balancer>
                        </Typography>
                        <Typography
                            variant="body2"
                            sx={{
                                color: "text.secondary",
                                mt: 0.5,
                                mx: 'auto',
                                maxWidth: 720
                            }}>
                            <Balancer>
                                Hinterlegen Sie interne Leitfäden, das Intranet, eine Statusseite oder andere häufig genutzte Dienste.
                            </Balancer>
                        </Typography>
                        <Button
                            component={RouterLink}
                            to="/settings/dashboard"
                            startIcon={ModuleIcons.dashboardSettings}
                            sx={{mt: 2}}
                        >
                            Links konfigurieren
                        </Button>
                    </Box>
                ) : (
                    <Box
                        sx={{
                            display: 'grid',
                            gridTemplateColumns: {
                                xs: 'minmax(0, 1fr)',
                                md: 'repeat(2, minmax(0, 1fr))',
                                lg: 'repeat(3, minmax(0, 1fr))',
                            },
                            gap: 0.5,
                            p: 1,
                        }}
                    >
                        {links.map((link) => {
                            const LinkIcon = getCustomLinkIcon(link.icon);
                            return (
                                <MuiLink
                                    key={link.id}
                                    href={link.url}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    sx={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 1.5,
                                        minWidth: 0,
                                        px: {xs: 1.5, sm: 1.75},
                                        py: 1.5,
                                        borderRadius: 1,
                                        color: 'text.primary',
                                        textDecoration: 'none',
                                        '&:hover': {
                                            bgcolor: 'action.hover',
                                            textDecoration: 'none',
                                        },
                                    }}
                                >
                                    <Box sx={{width: 36, height: 36, display: 'grid', placeItems: 'center', borderRadius: '50%', bgcolor: 'action.hover', color: 'text.secondary', flexShrink: 0}}>
                                        <LinkIcon sx={{fontSize: 20}}/>
                                    </Box>
                                    <Box sx={{minWidth: 0, flex: 1}}>
                                        <Box sx={{display: 'flex', alignItems: 'center', gap: 0.5, minWidth: 0}}>
                                            <Typography
                                                variant="body2"
                                                noWrap
                                                sx={{
                                                    fontWeight: 650,
                                                    minWidth: 0
                                                }}>{link.label}</Typography>
                                            <OpenInNew sx={{fontSize: 15, color: 'text.disabled', flexShrink: 0}}/>
                                        </Box>
                                        {link.description && (
                                            <Typography
                                                variant="caption"
                                                sx={{
                                                    color: "text.secondary",
                                                    display: 'block',
                                                    lineHeight: 1.4
                                                }}>
                                                <Balancer>{link.description}</Balancer>
                                            </Typography>
                                        )}
                                    </Box>
                                </MuiLink>
                            );
                        })}
                    </Box>
                )}
            </DashboardPanel>
        </Box>
    );
}

export function DashboardLinksPanel() {
    const api = useApi();
    const customLinksService = useMemo(() => new CustomLinksApiService(api), [api]);
    const [links, setLinks] = useState<CustomLink[] | null>(null);
    const canCreate = useHasSystemPermission(Permission.SYSTEM_CONFIG_CREATE);
    const supportUrl = AppConfig.supportUrl;

    useEffect(() => {
        customLinksService.listAvailable(CustomLinkType.Dashboard)
            .then((page) => setLinks(page.content))
            .catch(() => setLinks(null));
    }, [customLinksService]);

    const cards: CustomLinkCardProps[] = [
        {
            category: 'Dokumentation',
            title: 'Antworten und Anleitungen finden',
            description: 'Lernen Sie Funktionen kennen und finden Sie praxisnahe Anleitungen für Ihre Arbeit mit Prosuna.',
            actionLabel: 'Dokumentation öffnen',
            url: 'https://docs.prosuna.de',
            external: true,
            icon: MenuBook,
        },
        ...(supportUrl ? [{
            category: 'Support',
            title: 'Unterstützung erhalten',
            description: 'Melden Sie technische Fragen oder Probleme über das für diese Instanz hinterlegte Support-Angebot.',
            actionLabel: 'Support-Angebot aufrufen',
            url: supportUrl,
            external: true,
            icon: SupportAgent,
        }] : []),
        {
            category: 'System',
            title: 'Informationen zur Installation',
            description: 'Prüfen Sie Version, Systemzustand und technische Diagnoseinformationen dieser Instanz.',
            actionLabel: 'Systeminformationen ansehen',
            url: '/settings/status',
            external: false,
            icon: ReadinessScore,
        },
    ];

    return (
        <Box component="section" aria-label="Schnellzugriffe">
            {links != null && <OrganizationLinksPanel links={links} canCreate={canCreate}/>}
            <Grid container spacing={2.5} sx={{
                alignItems: "stretch"
            }}>
                {cards.map((card) => (
                    <Grid
                        key={`${card.category}-${card.title}-${card.url}`}
                        size={{xs: 12, md: 6, lg: cards.length === 3 ? 4 : 6}}
                        sx={{display: 'flex'}}
                    >
                        <CustomLinkCard {...card}/>
                    </Grid>
                ))}
            </Grid>
        </Box>
    );
}
