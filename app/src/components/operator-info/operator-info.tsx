import {OperatorInfoProps} from './operator-info-props';
import ReactMarkdown from 'react-markdown';
import {StatusTable} from '../status-table/status-table';
import {NoCodeDataTypeLabels} from '../../data/no-code-data-type';
import React from 'react';
import {Table, TableBody, TableCell, TableContainer, TableRow, Typography} from '@mui/material';
import {Collapse} from '../collapse/collapse';

export function OperatorInfo(props: OperatorInfoProps) {
    return (
        <>
            <ReactMarkdown>
                {props.operator.description}
            </ReactMarkdown>

            <Collapse
                label="Technische Informationen"
                closeTooltip="Technische Informationen ausblenden"
                openTooltip="Technische Informationen einblenden"
            >
                <TableContainer>
                    <Table>
                        <TableBody>
                            <TableRow>
                                <TableCell>
                                    Paket
                                </TableCell>
                                <TableCell>
                                    {props.operator.packageName}
                                </TableCell>
                            </TableRow>


                            <TableRow>
                                <TableCell>
                                    Eindeutige ID
                                </TableCell>
                                <TableCell>
                                    {props.operator.identifier}
                                </TableCell>
                            </TableRow>

                            <TableRow>
                                <TableCell>
                                    Parameter
                                </TableCell>
                                <TableCell>
                                    <ul>
                                        {props.operator.parameters.map((parameter, i) => (
                                            <li key={i}>
                                                {parameter.label}: {NoCodeDataTypeLabels[parameter.type]}
                                            </li>
                                        ))}
                                    </ul>
                                </TableCell>
                            </TableRow>

                            <TableRow>
                                <TableCell>
                                    Rückgabewert
                                </TableCell>
                                <TableCell>
                                    {NoCodeDataTypeLabels[props.operator.returnType]}
                                </TableCell>
                            </TableRow>
                        </TableBody>
                    </Table>
                </TableContainer>
            </Collapse>
        </>
    );
}