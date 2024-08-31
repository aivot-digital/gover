import {Alert, AlertTitle, Box, Button, IconButton, ListItemIcon, ListItemText, Menu, MenuItem, Typography} from '@mui/material';
import React from 'react';
import {RichTextEditorComponentView} from '../richt-text-editor/rich-text-editor.component.view';
import {type Function} from '../../models/functions/function';
import {ConditionSetOperator} from '../../data/condition-set-operator';
import {type AnyElement} from '../../models/elements/any-element';
import {ConditionOperator} from '../../data/condition-operator';
import {type RootElement} from '../../models/elements/root-element';
import {type StepElement} from '../../models/elements/steps/step-element';
import {type GroupLayout} from '../../models/elements/form/layout/group-layout';
import MoreVertOutlinedIcon from '@mui/icons-material/MoreVertOutlined';
import DeleteForeverOutlinedIcon from '@mui/icons-material/DeleteForeverOutlined';
import {type ReplicatingContainerLayout} from '../../models/elements/form/layout/replicating-container-layout';
import {CodeTabCodeEditor} from '../code-tab-code-editor';
import {CodeTabNoCodeEditor} from '../code-tab-no-code-editor';

export type CodeTabProps = {
    parents: Array<RootElement | StepElement | GroupLayout | ReplicatingContainerLayout>;
    element: AnyElement;
    resultTitle: string;
    resultHint: string;
    shouldReturnString: boolean;
    editable: boolean;
} & ({
    allowNoCode: true;
    func?: Function;
    onChange: (updatedFunc: Function | undefined) => void;
} | {
    allowNoCode: false;
    func?: Function;
    onChange: (updatedFunc: Function | undefined) => void;
});

function newCodeFunction(func: Function | undefined): Function {
    return {
        requirements: func?.requirements ?? '',
        code: '/**\n * @param{Data} data Die Nutzereingaben\n * @param{CurrentElement} element Das aktuelle Element\n * @param{string} id Die ID des aktuellen Elements\n */\nfunction main(data, element, id) {\n    console.log(data, element, id);\n}',
    };
}

function newNoCodeFunction(func: Function | undefined, element: AnyElement): Function {
    return {
        requirements: func?.requirements ?? '',
        conditionSet: {
            operator: ConditionSetOperator.Any,
            conditions: [
                {
                    reference: '',
                    operator: ConditionOperator.Equals,
                    value: '',
                    conditionUnmetMessage: '',
                },
            ],
            conditionsSets: [],
            conditionSetUnmetMessage: '',
        },
    };
}

export function CodeTab({
                            parents,
                            element,
                            resultTitle,
                            resultHint,
                            func,
                            allowNoCode,
                            shouldReturnString,
                            onChange,
                            editable,
                        }: CodeTabProps): JSX.Element {
    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
    const handleClick = (event: React.MouseEvent<HTMLButtonElement>): void => {
        setAnchorEl(event.currentTarget);
    };
    const handleClose = (): void => {
        setAnchorEl(null);
    };

    return (
        <Box
            sx={{
                m: 4,
            }}
        >
            <Typography
                sx={{
                    mb: 2,
                }}
                variant="subtitle1"
            >
                Fachliche Anforderungen
            </Typography>

            <RichTextEditorComponentView
                value={func?.requirements ?? ''}
                onChange={(req) => {
                    onChange({
                        ...func,
                        requirements: req ?? '',
                    });
                }}
                disabled={!editable}
            />

            {
                editable &&
                (
                    func == null ||
                    (func.code == null && func.conditionSet == null)
                ) &&
                <Box
                    sx={{
                        display: 'flex',
                        mt: 4,
                        alignItems: 'stretch',
                    }}
                >
                    {
                        allowNoCode &&
                        <Box
                            sx={{
                                flex: 1,
                                display: 'flex',
                                flexDirection: 'column',
                            }}
                        >
                            <Typography
                                variant="subtitle1"
                                align="center"
                                sx={{
                                    mb: 2,
                                }}
                            >
                                No-Code Funktion anlegen
                            </Typography>
                            <Typography
                                align="center"
                            >
                                Eine No-Code Funktion kann genutzt werden, um dynamisches Verhalten, ganz ohne
                                Programmierkenntnisse einzupflegen.
                            </Typography>

                            <Box
                                sx={{
                                    flex: '1',
                                }}
                            />

                            <Box
                                sx={{
                                    mt: 2,
                                }}
                            >
                                <Button
                                    fullWidth
                                    onClick={() => {
                                        onChange(newNoCodeFunction(func, element));
                                    }}
                                >
                                    Jetzt No-Code Funktion anlegen
                                </Button>
                            </Box>
                        </Box>
                    }

                    {
                        allowNoCode &&
                        <Box
                            sx={{
                                width: '0.125em',
                                backgroundColor: '#e0e0e0',
                                mx: 2,
                            }}
                        />
                    }

                    <Box
                        sx={{
                            flex: 1,
                            display: 'flex',
                            flexDirection: 'column',
                        }}
                    >
                        <Typography
                            variant="subtitle1"
                            align="center"
                            sx={{
                                mb: 2,
                            }}
                        >
                            Programmcode anlegen
                        </Typography>
                        <Typography
                            align="center"
                        >
                            Mit Programmcode kann dynamisches Verhalten implementiert werden.
                        </Typography>

                        <Box
                            sx={{
                                flex: '1',
                            }}
                        />

                        <Box
                            sx={{
                                mt: 2,
                            }}
                        >
                            <Button
                                fullWidth
                                onClick={() => {
                                    onChange(newCodeFunction(func));
                                }}
                            >
                                Jetzt Programmcode anlegen
                            </Button>
                        </Box>
                    </Box>
                </Box>
            }

            {
                func != null &&
                (
                    func.code != null ||
                    func.conditionSet != null
                ) &&
                <>
                    <Alert
                        sx={{
                            mt: 8,
                        }}
                        severity="info"
                    >
                        <AlertTitle>
                            {resultTitle}
                        </AlertTitle>
                        {resultHint}
                    </Alert>

                    <Box
                        sx={{
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            mt: 2,
                        }}
                    >
                        <Typography variant="subtitle1">
                            {
                                func.code != null ? 'Programmcode' : 'No-Code Funktion'
                            }
                        </Typography>

                        <Box>
                            {
                                editable &&
                                allowNoCode &&
                                func.conditionSet != null &&
                                <Button
                                    onClick={() => {
                                        const res = window.confirm('Soll wirklich zu Programmcode gewechselt werden? Alle Inhalte für diese Funktion gehen damit verloren.');
                                        if (res) {
                                            onChange(newCodeFunction(func));
                                        }
                                    }}
                                >
                                    Zu Programmcode wechseln
                                </Button>
                            }

                            {
                                editable &&
                                allowNoCode &&
                                func.code != null &&
                                <Button
                                    onClick={() => {
                                        const res = window.confirm('Soll wirklich zu No-Code gewechselt werden? Alle Inhalte für diese Funktion gehen damit verloren.');
                                        if (res) {
                                            onChange(newNoCodeFunction(func, element));
                                        }
                                    }}
                                >
                                    Zu No-Code wechseln
                                </Button>
                            }

                            {

                                editable &&
                                <IconButton
                                    onClick={handleClick}
                                >
                                    <MoreVertOutlinedIcon/>
                                </IconButton>
                            }

                            <Menu
                                open={anchorEl != null}
                                anchorEl={anchorEl}
                                onClose={handleClose}
                            >
                                <MenuItem
                                    onClick={() => {
                                        const res = window.confirm('Soll die Funktion wirklich gelöscht werden? Diese Aktion kann nicht rückgängig gemacht werden.');
                                        if (res) {
                                            onChange({
                                                requirements: func.requirements,
                                            });
                                        }
                                        handleClose();
                                    }}
                                >
                                    <ListItemIcon>
                                        <DeleteForeverOutlinedIcon/>
                                    </ListItemIcon>
                                    <ListItemText>
                                        Funktion entfernen
                                    </ListItemText>
                                </MenuItem>
                            </Menu>
                        </Box>
                    </Box>

                    <Box
                        sx={{
                            mt: 2,
                        }}
                    >
                        {
                            func.code != null &&
                            <CodeTabCodeEditor
                                element={element}
                                func={func}
                                onChange={onChange}
                                editable={editable}
                            />
                        }

                        {
                            allowNoCode &&
                            func.conditionSet != null &&
                            <CodeTabNoCodeEditor
                                parents={parents}
                                element={element}
                                func={func}
                                onChange={onChange}
                                shouldReturnString={shouldReturnString}
                                editable={editable}
                            />
                        }
                    </Box>
                </>
            }
        </Box>
    );
}

