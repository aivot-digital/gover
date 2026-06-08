import {getNodeName} from '../pages/details/components/process-flow-editor/utils/node-utils';
import {AlertComponent} from '../../../components/alert/alert-component';
import React from 'react';
import {ProcessNodeProblems} from '../entities/process-node-problems';
import type {ProcessNodeProvider} from '../services/process-node-provider-api-service';
import {SxProps} from '@mui/material';

interface NodeProblemsAlertProps {
    problems: ProcessNodeProblems[];
    availableNodeProviders: ProcessNodeProvider[];
    sx?: SxProps;
}

export function NodeProblemsAlert(props: NodeProblemsAlertProps) {
    const {
        problems,
        availableNodeProviders,
        sx,
    } = props;

    return (
        <AlertComponent
            color="warning"
            sx={sx}
        >
            Mindestens eins der Prozesselemente hat eine ungültige Konfiguration.
            Sie können den Test starten, es kann jedoch zu Ausführungsproblemen aufgrund der
            ungültigen Konfiguration kommen.

            <ul>
                {
                    problems.map((problem) => {
                        const provider = availableNodeProviders
                            .find((p) => p.key === problem.node.processNodeDefinitionKey && p.majorVersion === problem.node.processNodeDefinitionVersion)!;

                        return (
                            <li key={problem.node.id}>
                                {getNodeName(problem.node, provider)}:
                                <ul>
                                    {
                                        problem.problems.map((problem, index) => (
                                            <li key={index}>{problem}</li>
                                        ))
                                    }
                                </ul>
                            </li>
                        );
                    })
                }
            </ul>
        </AlertComponent>
    );
}