import {
    Alert,
    AlertTitle,
    Box,
    Button,
    IconButton,
    ListItemIcon,
    ListItemText,
    Menu,
    MenuItem,
    Typography
} from '@mui/material';
import React from 'react';
import {RichTextEditorComponentView} from '../../../../../richt-text-editor/rich-text-editor.component.view';
import {Function} from "../../../../../../models/functions/function";
import {FunctionNoCode} from "../../../../../../models/functions/function-no-code";
import {FunctionCode, isFunctionCode} from "../../../../../../models/functions/function-code";
import {ConditionSetOperator} from "../../../../../../data/condition-set-operator";
import {AnyElement} from "../../../../../../models/elements/any-element";
import {CodeTabCodeEditor} from "./components/code-tab-code-editor";
import {CodeTabNoCodeEditor} from "./components/code-tab-no-code-editor";
import {ConditionOperator} from "../../../../../../data/condition-operator";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faEllipsisVertical, faTrashCanXmark} from "@fortawesome/pro-light-svg-icons";

export type CodeTabProps = {
    element: AnyElement;
    resultTitle: string;
    resultHint: string;
    shouldReturnString: boolean;
} & ({
    allowNoCode: true;
    func?: FunctionNoCode | FunctionCode;
    onChange: (updatedFunc: FunctionNoCode | FunctionCode | undefined) => void;
} | {
    allowNoCode: false;
    func?: FunctionCode;
    onChange: (updatedFunc: FunctionCode | undefined) => void;
})

function newCodeFunction(func: Function | undefined): FunctionCode {
    return {
        requirements: func?.requirements ?? '',
        code: 'function main(data, element, id) {\n    console.log(data, element, id)\n}',
    };
}

function newNoCodeFunction(func: Function | undefined, element: AnyElement): FunctionNoCode {
    return {
        requirements: func?.requirements ?? '',
        conditionSet: {
            operator: ConditionSetOperator.Any,
            conditions: [
                {
                    operandA: {
                        id: '',
                    },
                    operator: ConditionOperator.Equals,
                    operandB: {
                        value: ''
                    },
                    conditionUnmetMessage: '',
                },
            ],
            conditionsSets: [],
            conditionSetUnmetMessage: '',
        },
    };
}

export function CodeTab({
                            element,
                            resultTitle,
                            resultHint,
                            func,
                            allowNoCode,
                            shouldReturnString,
                            onChange
                        }: CodeTabProps) {
    const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
    const handleClick = (event: React.MouseEvent<HTMLButtonElement>) => {
        setAnchorEl(event.currentTarget);
    };
    const handleClose = () => {
        setAnchorEl(null);
    };


    return (
        <Box sx={{m: 4}}>
            <Typography
                sx={{mb: 2}}
                variant="subtitle1"
            >
                Fachliche Anforderungen
            </Typography>

            <RichTextEditorComponentView
                value={func?.requirements ?? ''}
                onChange={req => {
                    // @ts-ignore TODO: Fix typing issues
                    onChange({
                        ...func,
                        requirements: req,
                    });
                }}
            />

            {
                (
                    func == null ||
                    !(
                        'code' in func ||
                        'conditionSet' in func
                    )
                ) &&
                <Box sx={{display: 'flex', mt: 4, alignItems: 'stretch'}}>
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
                                sx={{mb: 2}}
                            >
                                No-Code Funktion anlegen
                            </Typography>
                            <Typography
                                align="center"
                            >
                                Eine No-Code Funktion kann genutzt werden, um dynamisches Verhalten, ganz ohne
                                Programmierkenntnisse einzupflegen.
                            </Typography>

                            <Box sx={{flex: '1'}}/>

                            <Box sx={{mt: 2}}>
                                <Button
                                    fullWidth
                                    onClick={() => onChange(newNoCodeFunction(func, element))}
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
                            sx={{mb: 2}}
                        >
                            Programmcode anlegen
                        </Typography>
                        <Typography
                            align="center"
                        >
                            Mit Programmcode kann dynamisches Verhalten implementiert werden.
                        </Typography>

                        <Box sx={{flex: '1'}}/>

                        <Box sx={{mt: 2}}>
                            <Button
                                fullWidth
                                onClick={() => onChange(newCodeFunction(func))}
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
                    'code' in func ||
                    'conditionSet' in func
                ) &&
                <>
                    <Alert
                        sx={{mt: 8}}
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
                                isFunctionCode(func) ? 'Programmcode' : 'No-Code Funktion'
                            }
                        </Typography>

                        <Box>
                            {
                                allowNoCode &&
                                !isFunctionCode(func) &&
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
                                allowNoCode &&
                                isFunctionCode(func) &&
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

                            <IconButton
                                onClick={handleClick}
                            >
                                <FontAwesomeIcon
                                    icon={faEllipsisVertical}
                                    fixedWidth
                                />
                            </IconButton>

                            <Menu
                                open={anchorEl != null}
                                anchorEl={anchorEl}
                                onClose={handleClose}
                            >
                                    <MenuItem
                                        onClick={() => {
                                            const res = window.confirm('Soll die Funktion wirklich gelöscht werden? Diese Aktion kann nicht rückgängig gemacht werden.');
                                            if (res) {
                                                onChange(undefined);
                                            }
                                            handleClose();
                                        }}
                                    >
                                        <ListItemIcon>
                                            <FontAwesomeIcon
                                                icon={faTrashCanXmark}
                                                style={{marginTop: '-4px'}}
                                            />
                                        </ListItemIcon>
                                        <ListItemText>
                                            Funktion entfernen
                                        </ListItemText>
                                    </MenuItem>
                            </Menu>
                        </Box>
                    </Box>

                    <Box sx={{mt: 2}}>
                        {
                            isFunctionCode(func) &&
                            <CodeTabCodeEditor
                                func={func}
                                onChange={onChange}
                            />
                        }

                        {
                            allowNoCode &&
                            !isFunctionCode(func) &&
                            <CodeTabNoCodeEditor
                                element={element}
                                func={func}
                                onChange={onChange}
                                shouldReturnString={shouldReturnString}
                            />
                        }
                    </Box>
                </>
            }
        </Box>
    );
}

