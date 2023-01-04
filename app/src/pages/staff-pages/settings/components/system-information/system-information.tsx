import React from 'react';
import {Table, TableBody, TableCell, TableRow} from '@mui/material';
import ProjectPackage from '../../../../../../package.json';
import gitInfo from '../../../../../git-info.json';
import {Localization} from '../../../../../locale/localization';
import strings from './system-information-strings.json';
import {format} from 'date-fns'

const _ = Localization(strings);

const rows = [
    {label: _.versionLabel, value: ProjectPackage.version},
    {label: _.branchLabel, value: `${gitInfo.gitBranch} ${gitInfo.gitCommitHash}`},
    {label: _.dateLabel, value: format(new Date(gitInfo.date), 'P')},
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
