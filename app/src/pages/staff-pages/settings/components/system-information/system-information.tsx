import React, {useEffect, useState} from 'react';
import {Box, CircularProgress, Skeleton, Table, TableBody, TableCell, TableContainer, TableRow, Typography} from '@mui/material';
import ProjectPackage from '../../../../../../package.json';
import gitInfo from '../../../../../git-info.json';
import {format} from 'date-fns';
import {DbComponent, DiskSpaceComponent, Health, MailComponent} from '../../../../../models/dtos/health';
import {SystemService} from '../../../../../services/system-service';
import ErrorOutlineOutlinedIcon from '@mui/icons-material/ErrorOutlineOutlined';
import CheckCircleOutlineOutlinedIcon from '@mui/icons-material/CheckCircleOutlineOutlined';
import {AlertComponent} from '../../../../../components/alert/alert-component';
import {humanizeFileSize} from '../../../../../utils/huminization-utils';

const rows = [
    {
        label: 'Version',
        value: ProjectPackage.version,
    },
    {
        label: 'Ursprung',
        value: `${gitInfo.gitBranch} ${gitInfo.gitCommitHash}`,
    },
    {
        label: 'Compiler-Datum',
        value: format(new Date(gitInfo.date), 'P'),
    },
];


function convertGbToBytes(gb: number): number {
    return gb * 1024 * 1024 * 1024;
}

const diskSpaceThresholdBytes = convertGbToBytes(2);

export function SystemInformation(): JSX.Element {
    const [health, setHealth] = useState<Health | 'error'>();

    useEffect(() => {
        SystemService
            .checkHealth()
            .then(setHealth)
            .catch((err) => {
                if (err.details != null) {
                    setHealth(err.details);
                } else {
                    console.error(err);
                    setHealth('error');
                }
            });
    }, []);

    const diskSpace: DiskSpaceComponent | undefined = health != null && health !== 'error' ? health.components.diskSpace : undefined;
    const mail: MailComponent | undefined = health != null && health !== 'error' ? health.components.mail : undefined;
    const database: DbComponent | undefined = health != null && health !== 'error' ? health.components.db : undefined;

    return (
        <>
            <Typography

                variant="h5"
            >
                Softwareinformationen
            </Typography>

            <TableContainer>
                <Table>
                    <TableBody>
                        {
                            rows.map((row) => (
                                <TableRow key={row.label}>
                                    <TableCell>
                                        {row.label}
                                    </TableCell>
                                    <TableCell>
                                        {row.value}
                                    </TableCell>
                                </TableRow>
                            ))
                        }
                    </TableBody>
                </Table>
            </TableContainer>

            <Box
                sx={{
                    mt: 4,
                    mb: 2,
                    display: 'flex',
                }}
            >
                <Typography

                    variant="h5"
                >
                    Systemstatus
                </Typography>

                <Box sx={{ml: 2}}>
                    {
                        health == null ?
                            <CircularProgress size="1em"/> :
                            (
                                health === 'error' ?
                                    <ErrorOutlineOutlinedIcon color="error"/> :
                                    <CheckCircleOutlineOutlinedIcon color="success"/>
                            )
                    }
                </Box>
            </Box>

            {
                health == null &&
                <Skeleton
                    variant="rectangular"
                    height={200}
                />
            }
            {
                health != null &&
                health === 'error' &&
                <AlertComponent color="error">
                    Der Systemstatus konnte nicht abgerufen werden.
                </AlertComponent>
            }
            {
                health != null &&
                health !== 'error' &&
                <TableContainer>
                    <Table>
                        <TableBody>
                            <TableRow>
                                <TableCell>
                                    Datenbank
                                </TableCell>
                                <TableCell>
                                    <Box
                                        sx={{
                                            display: 'flex',
                                        }}
                                    >
                                        {
                                            database != null && database.status === 'UP' ?
                                                <CheckCircleOutlineOutlinedIcon color="success"/> :
                                                <ErrorOutlineOutlinedIcon color="error"/>
                                        }

                                        <Typography
                                            sx={{ml: 2}}
                                        >
                                            {
                                                database != null && database.status === 'UP' ?
                                                    'Verfügbar' :
                                                    'Nicht verfügbar'
                                            }
                                        </Typography>
                                    </Box>
                                </TableCell>
                            </TableRow>

                            <TableRow>
                                <TableCell>
                                    SMTP-Server
                                </TableCell>
                                <TableCell>
                                    <Box
                                        sx={{
                                            display: 'flex',
                                        }}
                                    >
                                        {
                                            mail != null && mail.status === 'UP' ?
                                                <CheckCircleOutlineOutlinedIcon color="success"/> :
                                                <ErrorOutlineOutlinedIcon color="error"/>
                                        }

                                        <Typography
                                            sx={{ml: 2}}
                                        >
                                            {
                                                mail != null && mail.status === 'UP' ?
                                                    'Verfügbar' :
                                                    'Nicht verfügbar'
                                            }
                                        </Typography>
                                    </Box>
                                </TableCell>
                            </TableRow>

                            <TableRow>
                                <TableCell>
                                    Speicher
                                </TableCell>
                                <TableCell>
                                    <Box
                                        sx={{
                                            display: 'flex',
                                        }}
                                    >
                                        {
                                            diskSpace != null && diskSpace.details.free > diskSpaceThresholdBytes ?
                                                <CheckCircleOutlineOutlinedIcon color="success"/> :
                                                <ErrorOutlineOutlinedIcon color="error"/>
                                        }

                                        <Typography
                                            sx={{ml: 2}}
                                        >
                                            {
                                                diskSpace != null && diskSpace.details.free < diskSpaceThresholdBytes &&
                                                <span>
                                                    Nur noch {humanizeFileSize(diskSpace.details.free)} frei
                                                </span>
                                            }

                                            {
                                                diskSpace != null && diskSpace.details.free > diskSpaceThresholdBytes &&
                                                <span>
                                                    Noch {humanizeFileSize(diskSpace.details.free)} von { humanizeFileSize(diskSpace.details.total) } frei
                                                </span>
                                            }
                                        </Typography>
                                    </Box>
                                </TableCell>
                            </TableRow>
                        </TableBody>
                    </Table>
                </TableContainer>
            }
        </>
    );
}
