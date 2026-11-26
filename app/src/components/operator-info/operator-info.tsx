import {OperatorInfoProps} from './operator-info-props';
import {NoCodeDataTypeLabels} from '../../data/no-code-data-type';
import React, {Fragment} from 'react';
import {Box, Table, TableBody, TableCell, TableContainer, TableRow} from '@mui/material';
import {MarkdownContent} from '../markdown-content/markdown-content';
import {TechnicalInformationSection} from '../technical-information-section/technical-information-section';

export function OperatorInfo(props: OperatorInfoProps) {
    return (
        <Box>
            <MarkdownContent
                markdown={props.operator.description}
                headingBaseLevel={props.headingBaseLevel ?? 3}
            />

            <TechnicalInformationSection headingBaseLevel={props.headingBaseLevel}>
                <TableContainer>
                    <Table sx={{
                        tableLayout: 'fixed',
                        '& .MuiTableCell-root': {overflowWrap: 'anywhere', verticalAlign: 'top'},
                        '& .MuiTableCell-root:first-child': {width: '35%'},
                        '& ul': {m: 0, pl: 2.5},
                    }}>
                        <TableBody>
                            {
                                props.operator.packageName.length > 0 &&
                                <TableRow>
                                    <TableCell component="th" scope="row">
                                        Paket
                                    </TableCell>
                                    <TableCell>
                                        {props.operator.packageName}
                                    </TableCell>
                                </TableRow>
                            }


                            <TableRow>
                                <TableCell component="th" scope="row">
                                    Eindeutige ID
                                </TableCell>
                                <TableCell>
                                    {props.operator.identifier}
                                </TableCell>
                            </TableRow>

                            <TableRow>
                                <TableCell component="th" scope="row">
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
                                                            Keine
                                                            Parameter {' -> '} {NoCodeDataTypeLabels[signature.returnType]}
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
            </TechnicalInformationSection>
        </Box>
    );
}
