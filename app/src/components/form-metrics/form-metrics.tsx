import React, {useMemo} from 'react';
import {type FormMetricsProps} from './form-metrics-props';
import {Box, Table, TableBody, TableCell, TableContainer, TableRow, Typography} from '@mui/material';
import {flattenElements} from '../../utils/flatten-elements';
import {isAnyInputElement} from '../../models/elements/form/input/any-input-element';
import {ElementType} from '../../data/element-type/element-type';
import {type AnyElement} from '../../models/elements/any-element';
import {type RootElement} from '../../models/elements/root-element';
import {AnyElementWithChildren, isAnyElementWithChildren} from '../../models/elements/any-element-with-children';
import HelpIconOutlined from '@mui/icons-material/HelpOutline';
import {HintTooltip} from '../hint-tooltip/hint-tooltip';
import {generateComponentTitle} from '../../utils/generate-component-title';

export function FormMetrics(props: FormMetricsProps): JSX.Element {
    const metrics = useMemo(() => calculateMetrics(props.root), [props.root]);
    const metricRows = useMemo<Array<{ label: string, hint: string, value: string }>>(() => [
        {
            label: 'Eingabefelder Gesamt',
            hint: 'Je mehr Eingabefelder ein Formular hat, desto komplexer ist dieses.',
            value: metrics.inputElements.toString(),
        },
        {
            label: 'Durchschnittliche Eingabefelder pro Abschnitt',
            hint: 'Je mehr Eingabefelder ein Abschnitt hat, desto komplexer ist dieser. Komplexe Abschnitte sollten in mehrere Abschnitte aufgeteilt werden.',
            value: metrics.inputElementsPerStepAvg.toString(),
        },
        {
            label: 'Komplexität Gesamt',
            hint: 'Je Komplexer ein Formular ist, desto schwerer ist dieses zu pflegen, zu bearbeiten und auszufüllen.',
            value: `${metrics.overallComplexity}`,
        },
    ], [metrics]);

    const stepMetrics = useMemo(() => props.root.children.map(step => ({
        step: step,
        metrics: calculateMetrics(step),
    })), [props.root]);
    const stepMetricRows = useMemo(() => stepMetrics.map(({step, metrics}) => ({
        step: step,
        metricRows: [
            {
                label: 'Eingabefelder des Abschnitts',
                hint: 'Je mehr Eingabefelder ein Abschnitt hat, desto komplexer ist dieses.',
                value: metrics.inputElements.toString(),
            },
            {
                label: 'Komplexität des Abschnitts',
                hint: 'Je Komplexer ein Abschnitt ist, desto schwerer ist dieser zu pflegen, zu bearbeiten und auszufüllen.',
                value: `${metrics.overallComplexity}`,
            },
        ],
    })), [stepMetrics]);

    return (
        <Box>
            <Typography
                variant="subtitle1"
            >
                Gesamtes Formular
            </Typography>

            <TableContainer>
                <Table
                    size="small"
                    sx={{
                        tableLayout: 'fixed',
                        mt: 2,
                    }}
                >
                    <TableBody>
                        {
                            metricRows.map((row) => (
                                <TableRow key={row.label}>
                                    <TableCell>
                                        <Box
                                            sx={{
                                                display: 'flex',
                                                alignItems: 'center',
                                            }}
                                        >
                                            <span>{row.label}</span>
                                            <HintTooltip
                                                arrow
                                                placement="right"
                                                title={row.hint}
                                            >
                                                <HelpIconOutlined
                                                    sx={{
                                                        ml: 1,
                                                        color: '#a6a6a6',
                                                        cursor: 'help',
                                                    }}
                                                    fontSize="small"
                                                />
                                            </HintTooltip>
                                        </Box>
                                    </TableCell>
                                    <TableCell>{row.value}</TableCell>
                                </TableRow>
                            ))
                        }
                    </TableBody>
                </Table>
            </TableContainer>

            <Typography
                variant="subtitle1"
                sx={{
                    mt: 4,
                }}
            >
                Pro Abschnitt
            </Typography>

            {
                stepMetricRows.map(({step, metricRows}) => (
                    <Box
                        key={step.id}
                        sx={{
                            pl: 2,
                            mt: 2,
                        }}
                    >
                        <Typography variant="subtitle2">
                            {generateComponentTitle(step)}
                        </Typography>

                        <TableContainer>
                            <Table
                                size="small"
                                sx={{
                                    tableLayout: 'fixed',
                                }}
                            >
                                <TableBody>
                                    {
                                        metricRows.map((row) => (
                                            <TableRow key={row.label}>
                                                <TableCell>
                                                    <Box
                                                        sx={{
                                                            display: 'flex',
                                                            alignItems: 'center',
                                                        }}
                                                    >
                                                        <span>{row.label}</span>
                                                        <HintTooltip
                                                            arrow
                                                            placement="right"
                                                            title={row.hint}
                                                        >
                                                            <HelpIconOutlined
                                                                sx={{
                                                                    ml: 1,
                                                                    color: '#a6a6a6',
                                                                    cursor: 'help',
                                                                }}
                                                                fontSize="small"
                                                            />
                                                        </HintTooltip>
                                                    </Box>
                                                </TableCell>
                                                <TableCell>{row.value}</TableCell>
                                            </TableRow>
                                        ))
                                    }
                                </TableBody>
                            </Table>
                        </TableContainer>
                    </Box>
                ))
            }
        </Box>
    );
}

interface Metrics {
    inputElements: number;
    inputElementsPerStepAvg: number;
    inputElementsPerStepMedian: number;

    maxDepth: number;

    replicatingContainerElements: number;
    replicatingContainerElementsDepth: number;

    overallComplexity: number;
    overallMaintainability: number;
}

function calculateMetrics(root: AnyElementWithChildren): Metrics {
    const metrics: Metrics = {
        inputElements: 0,
        inputElementsPerStepAvg: 0,
        inputElementsPerStepMedian: 0,

        maxDepth: 0,

        replicatingContainerElements: 0,
        replicatingContainerElementsDepth: 0,

        overallComplexity: 0,
        overallMaintainability: 0,
    };

    const flatElements = flattenElements(root);
    const flatElementsPerStep = root.children.map((step) => flattenElements(step));

    metrics.inputElements = flatElements.filter(isAnyInputElement).length;

    const inputElementsPerStep = flatElementsPerStep.map((elements) => elements.filter(isAnyInputElement).length);
    inputElementsPerStep.sort((a, b) => a - b);

    metrics.inputElementsPerStepAvg = inputElementsPerStep.reduce((a, b) => a + b, 0) / inputElementsPerStep.length;
    metrics.inputElementsPerStepMedian = inputElementsPerStep[Math.floor(inputElementsPerStep.length / 2)];

    metrics.maxDepth = getDepthOfElement(root);

    metrics.replicatingContainerElements = flatElements.filter((element) => element.type === ElementType.ReplicatingContainer).length;
    metrics.replicatingContainerElementsDepth = getDepthOfElement(root, (element) => element.type === ElementType.ReplicatingContainer);

    const complexElements = flatElements.filter((element) => baseComplexity[element.type] != null);
    metrics.overallComplexity = Math.floor(complexElements
        .map((element) => baseComplexity[element.type] as number)
        .reduce((a, b) => a + b, 0) / complexElements.length * 100);

    if (isNaN(metrics.overallComplexity)) {
        metrics.overallComplexity = 0;
    }

    return metrics;
}

function getDepthOfElement(element: AnyElement, filter?: (e: AnyElement) => boolean): number {
    if (isAnyElementWithChildren(element)) {
        let maxDepth = 0;
        for (const child of element.children) {
            const depth = getDepthOfElement(child, filter);
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }
        if (filter == null || filter(element)) {
            return maxDepth + 1;
        }
        return maxDepth;
    } else {
        if (filter == null || filter(element)) {
            return 1;
        } else {
            return 0;
        }
    }
}

const baseComplexity: Record<ElementType, number | null> = {
    [ElementType.Alert]: 0.1,
    [ElementType.Checkbox]: 0.1,
    [ElementType.Image]: 0.025,
    [ElementType.Container]: null,
    [ElementType.Date]: 0.25,
    [ElementType.Step]: null,
    [ElementType.Root]: null,
    [ElementType.Headline]: 0.025,
    [ElementType.MultiCheckbox]: 0.15,
    [ElementType.Number]: 0.1,
    [ElementType.ReplicatingContainer]: 0.9,
    [ElementType.Richtext]: 0.1,
    [ElementType.Radio]: 0.05,
    [ElementType.Select]: 0.1,
    [ElementType.Spacer]: null,
    [ElementType.Table]: 1.0,
    [ElementType.Text]: 0.1,
    [ElementType.Time]: 0.25,
    [ElementType.IntroductionStep]: null,
    [ElementType.SummaryStep]: null,
    [ElementType.SubmitStep]: null,
    [ElementType.SubmittedStep]: null,
    [ElementType.FileUpload]: 0.5,
};
