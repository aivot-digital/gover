import {OperatorInfoProps} from './operator-info-props';
import ReactMarkdown from 'react-markdown';
import {NoCodeDataTypeLabels} from '../../data/no-code-data-type';
import React, {Fragment} from 'react';
import {Table, TableBody, TableCell, TableContainer, TableRow} from '@mui/material';
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
                            {
                                props.operator.packageName.length > 0 &&
                                <TableRow>
                                    <TableCell>
                                        Paket
                                    </TableCell>
                                    <TableCell>
                                        {props.operator.packageName}
                                    </TableCell>
                                </TableRow>
                            }


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
                                    Signaturen
                                </TableCell>
                                <TableCell>
                                    <ul>
                                        {props.operator.signatures.map((signature, signatureIndex) => (
                                            <li key={signatureIndex}>
                                                {
                                                    signature.parameters.length > 0 ? (
                                                        <>
                                                            {signature.parameters.map((parameter, parameterIndex) => (
                                                                <Fragment key={parameterIndex}>
                                                                    {parameterIndex > 0 && ', '}
                                                                    {parameter.label}: {NoCodeDataTypeLabels[parameter.type]}
                                                                </Fragment>
                                                            ))}
                                                            {' -> '}
                                                            {NoCodeDataTypeLabels[signature.returnType]}
                                                        </>
                                                    ) : (
                                                        <>
                                                            Keine Parameter {' -> '} {NoCodeDataTypeLabels[signature.returnType]}
                                                        </>
                                                    )
                                                }
                                            </li>
                                        ))}
                                    </ul>
                                </TableCell>
                            </TableRow>
                        </TableBody>
                    </Table>
                </TableContainer>
            </Collapse>
        </>
    );
}
