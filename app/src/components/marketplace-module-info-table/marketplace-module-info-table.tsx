import {type MarketplaceModuleInfoTableProps} from './marketplace-module-info-table-props';
import {Box, IconButton, Table, TableBody, TableCell, TableContainer, TableRow, Tooltip, Typography} from '@mui/material';
import React from 'react';
import OpenInNewIcon from '@aivot/mui-material-symbols-400-n25-outlined/OpenInNew';
import {isStringNotNullOrEmpty} from '../../utils/string-utils';

export function MarketplaceModuleInfoTable(props: MarketplaceModuleInfoTableProps) {
    return (

        <Box>
            <Box
                sx={{
                    display: 'flex',
                    alignItems: 'center',
                }}
            >
                <Typography variant="subtitle1">
                    {props.module.title}
                </Typography>

                {
                    props.module.is_public &&
                    <Tooltip
                        title="Im Marktplatz anzeigen"
                    >
                        <IconButton
                            sx={{
                                ml: 'auto',
                            }}
                            component="a"
                            target="_blank"
                            rel="noopener noreferrer"
                            href={`https://marketplace.prosuna.de/modules/${props.module.id}`}
                        >
                            <OpenInNewIcon/>
                        </IconButton>
                    </Tooltip>
                }
            </Box>

            <TableContainer>
                <Table>
                    <TableBody>
                        <TableRow>
                            <TableCell>
                                Urheber
                            </TableCell>
                            <TableCell>
                                <a
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    href={`https://marketplace.prosuna.de/organizations/${props.module.organization_id}/`}
                                >
                                    @{props.module.organization}
                                </a>
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell>
                                Kurzbeschreibung
                            </TableCell>
                            <TableCell>
                                {props.module.description_short}
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell>
                                Beschreibung
                            </TableCell>
                            <TableCell>
                                <Typography component="pre">
                                    {props.module.description}
                                </Typography>
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell>
                                Datenfeld Id
                            </TableCell>
                            <TableCell>
                                {
                                    props.module.datenfeld_id != null &&
                                    isStringNotNullOrEmpty(props.module.datenfeld_id) ?
                                        props.module.datenfeld_id :
                                        <i>Keine Angaben</i>
                                }
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell>
                                Sichtbarkeit
                            </TableCell>
                            <TableCell>
                                {props.module.is_public ? 'Öffentlich' : 'Privat'}
                            </TableCell>
                        </TableRow>
                        <TableRow>
                            <TableCell>
                                Aktuelle Marktplatz-Version
                            </TableCell>
                            <TableCell>
                                {props.module.current_version}
                            </TableCell>
                        </TableRow>
                        {
                            props.currentVersion != null &&
                        <TableRow>
                            <TableCell>
                                Verbaute Marktplatz-Version
                            </TableCell>
                            <TableCell>
                                {props.currentVersion}
                            </TableCell>
                        </TableRow>
                        }
                        {
                            props.module.versions.length > 1 &&
                            <TableRow>
                                <TableCell>
                                    Alle Marktplatz-Versionen
                                </TableCell>
                                <TableCell>
                                    {props.module.versions.join(', ')}
                                </TableCell>
                            </TableRow>
                        }
                        {
                            props.module.recent_changes != null &&
                            isStringNotNullOrEmpty(props.module.recent_changes) &&
                            <TableRow>
                                <TableCell>
                                    Letzte Änderungen
                                </TableCell>
                                <TableCell>
                                    <Typography component="pre">
                                        {props.module.recent_changes}
                                    </Typography>
                                </TableCell>
                            </TableRow>
                        }
                    </TableBody>
                </Table>
            </TableContainer>
        </Box>
    );
}
