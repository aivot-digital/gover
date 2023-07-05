import React from 'react';
import {Table, TableBody, TableCell, TableRow} from '@mui/material';
import ProjectPackage from '../../../../../../package.json';
import gitInfo from '../../../../../git-info.json';
import {format} from 'date-fns'

const rows = [
    {label: 'Version', value: ProjectPackage.version},
    {label: 'Ursprung', value: `${gitInfo.gitBranch} ${gitInfo.gitCommitHash}`},
    {label: 'Compiler-Datum', value: format(new Date(gitInfo.date), 'P')},
];

export function SystemInformation() {
    return (
        <>
            <Table>
                <TableBody>
                    {
                        rows.map(row => (
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
        </>
    );
}
