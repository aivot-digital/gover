import {AnyElement} from "../../../../models/elements/any-element";
import {Alert, Box, Button, IconButton, Paper, Stack, Typography} from "@mui/material";
import {TextFieldComponent} from "../../../../components/text-field/text-field-component";
import {MarkdownContent} from "../../../../components/markdown-content/markdown-content";
import {useEffect, useRef, useState} from "react";
import {AiChatService, type AiChatMessage} from "../../services/ai-chat-service";
import {ElementType} from "../../../../data/element-type/element-type";
import {Chip} from "../../../../components/chip/chip";
import {Actions} from "../../../../components/actions/actions";
import Send from "@aivot/mui-material-symbols-400-n25-outlined/Send";
import StopCircle from "@aivot/mui-material-symbols-400-n25-outlined/StopCircle";
import Download from "@aivot/mui-material-symbols-400-n25-outlined/Download";
import Close from "@aivot/mui-material-symbols-400-n25-outlined/Close";
import {downloadBlobFile} from '../../../../utils/download-utils';
import {isApiError} from '../../../../models/api-error';

interface AiChatWindowPropsElementEditing {
    mode?: 'element';
    rootElement: AnyElement;
    targetRootType: ElementType;
    onElementChange: (element: AnyElement) => void;
}

interface AiChatWindowPropsProcessEditing {
    mode: 'process';
    userId: string;
    processId: number;
    processVersion: number;
    beforeSend: () => Promise<void>;
    afterTurn: () => Promise<void>;
    disabled?: boolean;
    reloadFailed: boolean;
    onRetry: () => void;
    isRetrying: boolean;
    unavailable: boolean;
}

type AiChatWindowProps = {
    onThinking: (isThinking: boolean) => void;
    onClose?: () => void;
    closeDisabled?: boolean;
} & (AiChatWindowPropsElementEditing | AiChatWindowPropsProcessEditing);

export function AiChatWindow(props: AiChatWindowProps) {
    const sessionKey = props.mode === 'process'
        ? `aiChatSessionId:process:${props.userId}:${props.processId}:${props.processVersion}`
        : 'aiChatSessionId';
    return <AiChatSession key={sessionKey} {...props} sessionKey={sessionKey}/>;
}

function AiChatSession(props: AiChatWindowProps & {sessionKey: string}) {
    const {onThinking, sessionKey, onClose, closeDisabled} = props;
    const disabled = props.mode === 'process' && props.disabled;
    const requestRef = useRef<AbortController | null>(null);
    const mountedRef = useRef(false);
    useEffect(() => {
        mountedRef.current = true;
        return () => {
            mountedRef.current = false;
            requestRef.current?.abort();
        };
    }, []);

    const [message, setMessage] = useState<string | null>(null);

    const sendingRef = useRef(false);
    const cancelRequestedRef = useRef(false);
    const [isThinking, setIsThinking] = useState<boolean>(false);
    const [isCancellable, setIsCancellable] = useState<boolean>(false);
    const [isCancelling, setIsCancelling] = useState<boolean>(false);
    const [isDownloadingTrace, setIsDownloadingTrace] = useState<boolean>(false);
    const [messageBuffer, setMessageBuffer] = useState<(AiChatMessage | {
        role: 'error';
        content: string;
    })[]>([]);

    const initialSessionIdRef = useRef<string | null>(localStorage.getItem(sessionKey));
    const [sessionId, setSessionId] = useState<string | null>(initialSessionIdRef.current);
    const [isLoadingHistory, setIsLoadingHistory] = useState(initialSessionIdRef.current != null);

    const chatMessagesBoxRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        onThinking(isThinking);
    }, [isThinking, onThinking]);

    useEffect(() => {
        const initialSessionId = initialSessionIdRef.current;
        if (initialSessionId == null) {
            return;
        }

        const controller = new AbortController();
        new AiChatService().getMessages(initialSessionId, controller.signal)
            .then(messages => {
                if (!controller.signal.aborted) {
                    setMessageBuffer(messages);
                }
            })
            .catch(error => {
                if (controller.signal.aborted) {
                    return;
                }
                console.error('Error loading AI chat history:', error);
                if (isApiError(error) && error.status === 404) {
                    localStorage.removeItem(sessionKey);
                    setSessionId(null);
                    setMessageBuffer([{
                        role: 'error',
                        content: 'Der bisherige Chat ist nicht mehr verfügbar. Mit Ihrer nächsten Nachricht wird ein neuer Chat gestartet.',
                    }]);
                    return;
                }
                setMessageBuffer([{
                    role: 'error',
                    content: 'Der Chatverlauf konnte nicht geladen werden. Öffnen Sie den Chat erneut, um es noch einmal zu versuchen.',
                }]);
            })
            .finally(() => {
                if (!controller.signal.aborted) {
                    setIsLoadingHistory(false);
                }
            });

        return () => controller.abort();
    }, []);

    const handleSendMessage = async () => {
        if (sendingRef.current || disabled || isLoadingHistory || message == null || message.trim() === '') {
            return;
        }

        sendingRef.current = true;
        cancelRequestedRef.current = false;
        setIsThinking(true);
        setIsCancellable(true);
        setIsCancelling(false);
        const controller = new AbortController();
        requestRef.current = controller;
        const service = new AiChatService();
        const outgoingMessage = message;
        let activeSessionId = sessionId;
        let turnStarted = false;
        let editorReloaded = false;

        const reloadEditor = async () => {
            if (activeSessionId == null || !mountedRef.current) {
                return;
            }

            editorReloaded = true;
            requestRef.current = null;
            setIsCancellable(false);
            try {
                if (props.mode === 'process') {
                    await props.afterTurn();
                } else {
                    const element = await service.getCurrentElement(activeSessionId);
                    if (mountedRef.current) props.onElementChange(element);
                }
            } catch (error) {
                if (!mountedRef.current) return;
                console.error('Error reloading the editor after AI chat:', error);
                setMessageBuffer((prevBuffer) => [
                    ...prevBuffer,
                    {role: 'error', content: props.mode === 'process'
                        ? 'Der Prozess konnte nach der KI-Anfrage nicht vollständig geladen werden. Laden Sie ihn erneut, bevor Sie weiterarbeiten.'
                        : 'Der aktuelle Formularentwurf konnte nach der KI-Anfrage nicht geladen werden.'},
                ]);
            }
        };

        try {
            if (props.mode === 'process') {
                try {
                    await props.beforeSend();
                } catch {
                    if (!controller.signal.aborted) {
                        setMessageBuffer(previous => [...previous, {role: 'error', content:
                            'Die offenen Änderungen konnten nicht gespeichert werden. Prüfen Sie die Knotenkonfiguration, bevor Sie die Nachricht erneut senden.'}]);
                    }
                    return;
                }
            }
            if (controller.signal.aborted) return;
            if (activeSessionId == null) {
                let session;
                try {
                    session = await service
                        .startChatSession(controller.signal);
                } catch (error) {
                    if (controller.signal.aborted) return;
                    setMessageBuffer(previous => [...previous, {role: 'error', content:
                        'Die KI-Anfrage konnte nicht gestartet werden. Versuchen Sie es später erneut.'}]);
                    return;
                }
                if (controller.signal.aborted) return;
                activeSessionId = session.sessionId;
                setSessionId(activeSessionId);
                localStorage.setItem(sessionKey, activeSessionId);
            }

            setMessageBuffer((prevBuffer) => [
                ...prevBuffer,
                {role: 'user', content: outgoingMessage},
            ]);
            setMessage(null);

            let receivedAssistantContent = false;
            try {
                turnStarted = true;
                await service.sendMessage(activeSessionId, outgoingMessage, (chunk) => {
                    if (controller.signal.aborted) return;
                    if (chunk.trim().length > 0) {
                        receivedAssistantContent = true;
                    }
                    setMessageBuffer((prevBuffer) => {
                        const lastMessage = prevBuffer[prevBuffer.length - 1];
                        if (lastMessage?.role === 'assistant') {
                            return [
                                ...prevBuffer.slice(0, -1),
                                {role: 'assistant', content: lastMessage.content + chunk},
                            ];
                        }
                        return [...prevBuffer, {role: 'assistant', content: chunk}];
                    });
                }, controller.signal, props.mode === 'process' ? {
                    processId: props.processId,
                    processVersion: props.processVersion,
                } : {
                    currentState: props.rootElement,
                    targetRootType: props.targetRootType,
                });
                if (controller.signal.aborted) return;

                if (!receivedAssistantContent) {
                    setMessageBuffer((prevBuffer) => [
                        ...prevBuffer,
                        {
                            role: 'error',
                            content: props.mode === 'process'
                                ? 'Die KI hat die Bearbeitung ohne Antwort beendet. Bereits ausgeführte Änderungen bleiben gespeichert. Prüfen Sie den Prozess und laden Sie bei Bedarf die KI-Diagnose herunter.'
                                : 'Die KI hat die Bearbeitung ohne Antwort beendet. Prüfen Sie den aktuellen Formularentwurf und laden Sie bei Bedarf die KI-Diagnose herunter.',
                        },
                    ]);
                }
            } catch (error) {
                if (controller.signal.aborted) return;
                console.error('Error sending message:', error);
                setMessageBuffer((prevBuffer) => [
                    ...prevBuffer,
                    {
                        role: 'error',
                        content: props.mode === 'process'
                            ? 'Die KI-Anfrage wurde mit einem Fehler beendet. Bereits ausgeführte Änderungen bleiben gespeichert. Prüfen Sie den Prozess, bevor Sie die Anfrage erneut senden.'
                            : 'Die KI-Anfrage wurde mit einem Fehler beendet. Prüfen Sie den aktuellen Formularentwurf, bevor Sie die Anfrage erneut senden.',
                    },
                ]);
            }

            await reloadEditor();
        } catch (error) {
            if (controller.signal.aborted) return;
            console.error('Error starting chat session:', error);
            setMessageBuffer((prevBuffer) => [
                ...prevBuffer,
                {role: 'error', content: 'Die KI-Anfrage konnte nicht gestartet werden. Versuchen Sie es später erneut.'},
            ]);
        } finally {
            if (cancelRequestedRef.current && mountedRef.current) {
                if (turnStarted && !editorReloaded) {
                    await reloadEditor();
                }
                if (mountedRef.current) {
                    setMessageBuffer(previous => [...previous, {
                        role: 'error',
                        content: 'Die Anfrage wurde abgebrochen. Bereits ausgeführte Änderungen können erhalten bleiben.',
                    }]);
                }
            }
            requestRef.current = null;
            cancelRequestedRef.current = false;
            sendingRef.current = false;
            if (mountedRef.current) {
                setIsCancellable(false);
                setIsCancelling(false);
                setIsThinking(false);
            }
        }
    };

    const handleCancelMessage = () => {
        const request = requestRef.current;
        if (!isThinking || request == null || request.signal.aborted) {
            return;
        }

        cancelRequestedRef.current = true;
        setIsCancelling(true);
        setIsCancellable(false);
        request.abort();
    };

    const handleDownloadTrace = async () => {
        if (sessionId == null || isDownloadingTrace) {
            return;
        }

        setIsDownloadingTrace(true);
        try {
            const trace = await new AiChatService().downloadTrace(sessionId);
            if (!mountedRef.current) return;
            downloadBlobFile(`prosuna-ai-chat-trace-${sessionId}.json`, trace);
        } catch (error) {
            if (!mountedRef.current) return;
            console.error('Error downloading AI chat trace:', error);
            setMessageBuffer((prevBuffer) => [
                ...prevBuffer,
                {role: 'error', content: 'Die KI-Diagnose konnte nicht heruntergeladen werden.'},
            ]);
        } finally {
            if (mountedRef.current) setIsDownloadingTrace(false);
        }
    };

    return (
        <Paper
            sx={{
                boxShadow: '0px 4px 15px rgba(0, 0, 0, 0.1)',
                borderLeft: '1px solid',
                borderLeftColor: 'divider',
                borderRadius: 0,
                position: 'relative',
                height: '100%',
                overflow: 'hidden',
            }}
        >
            <Stack
                direction="column"
                sx={{
                    p: 3,
                    height: '100%',
                    backgroundColor: 'background.paper',
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        mb: 1,
                    }}
                >
                    <Typography variant="h6">KI-Chat</Typography>
                    {onClose != null && (
                        <IconButton
                            aria-label="KI-Chat schließen"
                            disabled={closeDisabled}
                            onClick={onClose}
                        >
                            <Close/>
                        </IconButton>
                    )}
                </Box>

                <Typography variant="body2" sx={{mb: 2}}>
                    {props.mode === 'process'
                        ? 'Änderungen durch die KI werden direkt gespeichert.'
                        : 'Änderungen durch die KI werden in den Formularentwurf übernommen.'}
                </Typography>

                {props.mode === 'process' && props.reloadFailed && (
                    <Alert
                        severity="error"
                        sx={{mb: 2}}
                        action={(
                            <Button
                                disabled={props.isRetrying}
                                onClick={props.onRetry}
                            >
                                Erneut laden
                            </Button>
                        )}
                    >
                        Der Prozess konnte nicht vollständig geladen werden.
                    </Alert>
                )}

                {props.mode === 'process' && props.unavailable && (
                    <Alert severity="info" sx={{mb: 2}}>
                        Der KI-Chat steht nur für bearbeitbare Entwürfe außerhalb des Testmodus zur Verfügung.
                    </Alert>
                )}

                <Box
                    ref={chatMessagesBoxRef}
                    sx={{
                        flex: 1,
                        minHeight: 0,
                        overflowY: 'auto',
                        pr: 2,
                    }}
                >
                {
                    messageBuffer.map((msg, index) => (
                        <Box key={index} sx={{marginBottom: 2}}>
                            <strong>{msg.role === 'user' ? 'Sie' : msg.role === 'error' ? 'Hinweis' : 'KI'}:</strong>
                            <MarkdownContent markdown={msg.content}/>
                        </Box>
                    ))
                }

                {
                    isLoadingHistory &&
                    <Chip
                        label="Chatverlauf wird geladen …"
                    />
                }

                {
                    isThinking &&
                    <Chip
                        label={isCancelling ? 'Abbruch wird abgeschlossen …' : 'Anfrage wird bearbeitet …'}
                    />
                }
                </Box>

                <Box
                    sx={{
                        position: 'relative',
                        mt: 'auto',
                        flexShrink: 0,
                    }}
                >
                    <TextFieldComponent
                        label="Nachricht"
                        value={message}
                        onChange={setMessage}
                        required
                        multiline
                        rows={4}
                        disabled={disabled || isThinking || isLoadingHistory}
                    />

                    <Actions
                        sx={{
                            position: 'absolute',
                            right: 0,
                            bottom: 0,
                            mr: 2,
                            mb: 2,
                            width: 'fit-content',
                        }}
                        direction="column"
                        tooltipPlacement="top"
                        dense={true}
                        size="small"
                        actions={[
                            {
                                icon: <Download/>,
                                tooltip: "KI-Diagnose herunterladen",
                                onClick: handleDownloadTrace,
                                visible: sessionId != null,
                                disabled: isThinking || isLoadingHistory || isDownloadingTrace,
                            },
                            {
                                icon: <StopCircle/>,
                                tooltip: "Anfrage abbrechen",
                                disabledTooltip: "Abbruch wird abgeschlossen …",
                                onClick: handleCancelMessage,
                                visible: isCancellable || isCancelling,
                                disabled: isCancelling,
                            },
                            {
                                icon: <Send/>,
                                tooltip: "Absenden",
                                onClick: handleSendMessage,
                                visible: !isThinking,
                                disabled: disabled || isThinking || isLoadingHistory || message == null || message.trim() === '',
                            }
                        ]}
                    />
                </Box>
            </Stack>
        </Paper>
    );
}
