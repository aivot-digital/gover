import {useEffect, useState} from "react";
import {Box, Button, Container, Paper} from "@mui/material";
import {useParams} from "react-router-dom";
import {ModuleIcons} from "../../../../shells/staff/data/module-icons";
import {GenericDetailsSkeleton} from "../../../../components/generic-details-page/generic-details-skeleton";
import {PageWrapper} from "../../../../components/page-wrapper/page-wrapper";
import {GenericPageHeader} from "../../../../components/generic-page-header/generic-page-header";
import {ElementDerivationContext} from "../../../elements/components/element-derivation-context";
import {ProcessInstanceTaskApiService, TaskView, TaskViewEvent} from "../../services/process-instance-task-api-service";
import FactCheck from "@aivot/mui-material-symbols-400-outlined/dist/fact-check/FactCheck";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";
import {setErrorMessage, setLoadingMessage} from "../../../../slices/shell-slice";
import {showApiErrorSnackbar} from "../../../../slices/snackbar-slice";

export function ProcessTaskViewPage() {
    const params = useParams();
    const dispatch = useAppDispatch();

    const [taskView, setTaskView] = useState<TaskView>();

    useEffect(() => {
        new ProcessInstanceTaskApiService()
            .getTaskView(params.instanceAccessKey!, params.taskAccessKey!)
            .then(setTaskView)
            .catch((err) => {
                if (err.status === 403) {
                    dispatch(setErrorMessage({
                        message: 'Sie haben keine Berechtigung, diese Aufgabenansicht zu sehen.',
                        status: 403,
                    }))
                } else {
                    dispatch(showApiErrorSnackbar(err, 'Die Aufgabenansicht konnte nicht geladen werden.'));
                }
            });
    }, [params]);

    const handleEventClick = (evt: TaskViewEvent) => {
        dispatch(setLoadingMessage({
            message: `Verarbeite Aktion: ${evt.label}`,
            blocking: true,
            estimatedTime: 500,
        }));

        new ProcessInstanceTaskApiService()
            .putTaskView(params.instanceAccessKey!, params.taskAccessKey!, taskView?.data!, evt.event)
            .then(setTaskView)
            .finally(() => {
                window.location.reload();
            });
    };

    if (taskView == null) {
        return (
            <GenericDetailsSkeleton/>
        );
    }

    return (
        <PageWrapper
            title="Aufgabenansicht"
            fullWidth={true}
            fullHeight={true}
        >
            <Box
                sx={{
                    display: 'flex',
                    height: '100vh',
                }}
            >
                <Box
                    sx={{
                        flex: 1,
                        px: 2,
                        py: 2,
                    }}
                >
                    <Container>

                        <GenericPageHeader
                            title={'Aufgabenansicht: '}
                            badge={{
                                color: 'default',
                                label: `Version `,
                            }}
                            icon={<FactCheck/>}
                            actions={[
                                /*
                                {
                                    tooltip: 'Änderung rückgängig machen',
                                    icon: <Undo/>,
                                    onClick: handleUndo,
                                    disabled: previousProcessFlowStates.length > 0,
                                    visible: processFlow.version.status === ProcessStatus.Drafted,
                                },
                                {
                                    tooltip: 'Änderung wiederherstellen',
                                    icon: <Redo/>,
                                    onClick: handleRedo,
                                    disabled: futureProcessFlowStates.length > 0,
                                    visible: processFlow.version.status === ProcessStatus.Drafted,
                                },
                                'separator',
                                /*
                                {
                                    tooltip: 'Historie anzeigen',
                                    icon: <AccessTimeIcon/>,
                                    onClick: () => {
                                        setShowRevisions(true);
                                    },
                                    visible: canViewHistory,
                                },

                                {
                                    tooltip: 'Auslöser hinzufügen',
                                    icon: <Add/>,
                                    onClick: () => setShowAddTriggerDialog(true),
                                },
                                {
                                    tooltip: 'Exportieren',
                                    icon: <Download/>,
                                    onClick: handleExport,
                                },
                                {
                                    tooltip: 'Vorgänge',
                                    icon: <Download/>,
                                    to: `/processes/${processFlow.definition.id}/versions/${processFlow.version.processDefinitionVersion}/instances`,
                                },
                                 */
                            ]}
                        />


                        <Paper
                            sx={{
                                mt: 2,
                                p: 2,
                            }}
                        >
                            <ElementDerivationContext
                                element={taskView.layout}
                                elementData={taskView.data}
                                onElementDataChange={(elementData) => {
                                    setTaskView({
                                        ...taskView,
                                        data: elementData,
                                    });
                                }}
                            />

                            <Box
                                sx={{
                                    display: 'flex',
                                    gap: 2,
                                }}
                            >
                                {
                                    taskView
                                        .events
                                        .map((evt) => (
                                            <Button
                                                variant="contained"
                                                onClick={() => {
                                                    handleEventClick(evt);
                                                }}
                                            >
                                                {evt.label}
                                            </Button>
                                        ))
                                }
                            </Box>
                        </Paper>
                    </Container>
                </Box>
            </Box>
        </PageWrapper>
    );
}
