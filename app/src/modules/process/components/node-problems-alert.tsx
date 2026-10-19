import {getNodeName} from '../pages/details/components/process-flow-editor/utils/node-utils';
import {AlertComponent} from '../../../components/alert/alert-component';
import React from 'react';
import {ProcessNodeProblems} from '../entities/process-node-problems';
import type {ProcessNodeProvider} from '../services/process-node-provider-api-service';
import {SxProps} from '@mui/material';

interface NodeProblemsAlertProps {
    problems: ProcessNodeProblems[];
    availableNodeProviders: ProcessNodeProvider[];
    mode: 'test' | 'publish';
    versionProblems?: string[];
    sx?: SxProps;
}

export function NodeProblemsAlert(props: NodeProblemsAlertProps) {
    const {
        problems,
        availableNodeProviders,
        mode,
        sx,
        versionProblems = [],
    } = props;

    return (
        <AlertComponent
            color={mode === 'test' ? 'warning' : 'error'}
            title="Bitte prüfen Sie die Prozessversion"
            sx={sx}
        >
            {
                mode === 'test' &&
                <>
                    Die Prozessversion enthält unvollständige oder fehlerhafte Angaben.
                    Sie können den Test starten, es kann jedoch zu Ausführungsproblemen aufgrund der
                    ungültigen Konfiguration kommen.
                </>
            }
            {
                mode === 'publish' &&
                <>
                    Die Prozessversion enthält unvollständige oder fehlerhafte Angaben.
                    Sie können den Prozess erst veröffentlichen, wenn die nachstehenden Fehler behoben wurden.
                </>
            }

            {versionProblems.length > 0 && (
                <>
                    <p>Versionsspezifische Einstellungen:</p>
                    <ul>
                        {versionProblems.map((problem) => <li key={problem}>{problem}</li>)}
                    </ul>
                </>
            )}
            {problems.length > 0 && (
                <>
                    <p>Prozesselemente:</p>
                    <ul>
                        {problems.map((problem) => {
                            const provider = availableNodeProviders
                                .find((p) => p.key === problem.node.processNodeDefinitionKey && p.majorVersion === problem.node.processNodeDefinitionVersion)!;

                            return (
                                <li key={problem.node.id}>
                                    {getNodeName(problem.node, provider)}:
                                    <ul>
                                        {problem.problems.map((problem, index) => (
                                            <li key={index}>{problem}</li>
                                        ))}
                                    </ul>
                                </li>
                            );
                        })}
                    </ul>
                </>
            )}
        </AlertComponent>
    );
}
