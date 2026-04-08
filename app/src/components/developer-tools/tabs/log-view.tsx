import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import {LogLevelIcon} from '../../log-level-icon/log-level-icon';
import {format} from 'date-fns';
import TableContainer from '@mui/material/TableContainer';
import React from 'react';
import {selectLogLevel, selectLogs} from '../../../slices/logging-slice';
import {useAppSelector} from '../../../hooks/use-app-selector';

export function LogView() {
    const logLevel = useAppSelector(selectLogLevel);
    const logs = useAppSelector(selectLogs(logLevel));

    return (
        <TableContainer>
            <Table>
                <TableBody>
                    {
                        logs.length === 0 &&
                        <TableRow>
                            <TableCell>
                                <em>
                                    Keine Log-Einträge vorhanden.
                                </em>
                            </TableCell>
                        </TableRow>
                    }
                    {
                        logs.slice().reverse().map((log, index) => (
                            <TableRow
                                key={log.timestamp.toString() + index}
                            >
                                <TableCell>
                                    <LogLevelIcon
                                        level={log.type}
                                        active={true}
                                    />
                                </TableCell>
                                <TableCell>
                                    {format(log.timestamp, 'HH:mm:ss')}
                                </TableCell>
                                <TableCell>
                                    {log.source}
                                </TableCell>
                                <TableCell>
                                    <pre>{log.message}</pre>
                                </TableCell>
                            </TableRow>
                        ))
                    }
                </TableBody>
            </Table>
        </TableContainer>
    );
}