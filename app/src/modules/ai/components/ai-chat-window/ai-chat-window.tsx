import {AnyElement} from "../../../../models/elements/any-element";
import {Box, Stack} from "@mui/material";
import {TextFieldComponent} from "../../../../components/text-field/text-field-component";
import {MarkdownContent} from "../../../../components/markdown-content/markdown-content";
import {useEffect, useRef, useState} from "react";
import {AiChatService, type AiChatMessage} from "../../services/ai-chat-service";
import {ElementType} from "../../../../data/element-type/element-type";
import {Chip} from "../../../../components/chip/chip";
import {Actions} from "../../../../components/actions/actions";
import Send from "@aivot/mui-material-symbols-400-n25-outlined/Send";
import Download from "@aivot/mui-material-symbols-400-n25-outlined/Download";
import {downloadBlobFile} from '../../../../utils/download-utils';
import {isApiError} from '../../../../models/api-error';
import {showApiErrorSnackbar} from "../../../../slices/snackbar-slice";
import {useAppDispatch} from "../../../../hooks/use-app-dispatch";

interface AiChatWindowPropsElementEditing {
    rootElement: AnyElement;
    targetRootType: ElementType;
    onElementChange: (element: AnyElement) => void;
}

type AiChatWindowProps = {
    onThinking: (isThinking: boolean) => void;
} & AiChatWindowPropsElementEditing;

export function AiChatWindow(props: AiChatWindowProps) {
    const {
        rootElement,
        targetRootType,
        onElementChange,
        onThinking,
    } = props;

    const dispatch = useAppDispatch();

    const [message, setMessage] = useState<string | null>(null);

    const sendingRef = useRef(false);
    const [isThinking, setIsThinking] = useState<boolean>(false);
    const [isDownloadingTrace, setIsDownloadingTrace] = useState<boolean>(false);
    const [messageBuffer, setMessageBuffer] = useState<(AiChatMessage | {
        role: 'error';
        content: string;
    })[]>([]);

    const initialSessionIdRef = useRef<string | null>(localStorage.getItem('aiChatSessionId'));
    const [sessionId, setSessionId] = useState<string | null>(initialSessionIdRef.current);
    const [isLoadingHistory, setIsLoadingHistory] = useState(initialSessionIdRef.current != null);

    const [shouldScrollToBottomOnMessageReceived, setShouldScrollToBottomOnMessageReceived] = useState<boolean>(true);
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
                    localStorage.removeItem('aiChatSessionId');
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
        if (sendingRef.current || isLoadingHistory || message == null || message.trim() === '') {
            return;
        }

        sendingRef.current = true;
        setIsThinking(true);

        try {
            const service = new AiChatService();
            let activeSessionId = sessionId;
            if (activeSessionId == null) {
                let session;
                try {
                    session = await service
                        .startChatSession();
                } catch (error) {
                    dispatch(showApiErrorSnackbar(error, 'Die KI-Anfrage konnte nicht gestartet werden. Versuchen Sie es später erneut.'));

                    setIsThinking(false);
                    return;
                }
                activeSessionId = session.sessionId;
                setSessionId(activeSessionId);
                localStorage.setItem('aiChatSessionId', activeSessionId);
            }

            setMessageBuffer((prevBuffer) => [
                ...prevBuffer,
                {role: 'user', content: message},
            ]);
            setMessage(null);

            let receivedAssistantContent = false;
            try {
                await service.sendMessage(activeSessionId, message, (chunk) => {
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
                }, undefined, {
                    currentState: rootElement,
                    targetRootType,
                });

                if (!receivedAssistantContent) {
                    setMessageBuffer((prevBuffer) => [
                        ...prevBuffer,
                        {
                            role: 'error',
                            content: 'Die KI hat die Bearbeitung ohne Antwort beendet. Prüfen Sie den aktuellen Formularentwurf und laden Sie bei Bedarf die KI-Diagnose herunter.',
                        },
                    ]);
                }
            } catch (error) {
                console.error('Error sending message:', error);
                setMessageBuffer((prevBuffer) => [
                    ...prevBuffer,
                    {
                        role: 'error',
                        content: 'Die KI-Anfrage wurde mit einem Fehler beendet. Prüfen Sie den aktuellen Formularentwurf, bevor Sie die Anfrage erneut senden.',
                    },
                ]);
            }

            try {
                await service
                    .getCurrentElement(activeSessionId)
                    .then(onElementChange);
            } catch (error) {
                console.error('Error loading the current form draft:', error);
                setMessageBuffer((prevBuffer) => [
                    ...prevBuffer,
                    {role: 'error', content: 'Der aktuelle Formularentwurf konnte nach der KI-Anfrage nicht geladen werden.'},
                ]);
            }
        } catch (error) {
            console.error('Error starting chat session:', error);
            setMessageBuffer((prevBuffer) => [
                ...prevBuffer,
                {role: 'error', content: 'Die KI-Anfrage konnte nicht gestartet werden. Versuchen Sie es später erneut.'},
            ]);
        } finally {
            sendingRef.current = false;
            setIsThinking(false);
        }
    };

    const handleDownloadTrace = async () => {
        if (sessionId == null || isDownloadingTrace) {
            return;
        }

        setIsDownloadingTrace(true);
        try {
            const trace = await new AiChatService().downloadTrace(sessionId);
            downloadBlobFile(`prosuna-ai-chat-trace-${sessionId}.json`, trace);
        } catch (error) {
            console.error('Error downloading AI chat trace:', error);
            setMessageBuffer((prevBuffer) => [
                ...prevBuffer,
                {role: 'error', content: 'Die KI-Diagnose konnte nicht heruntergeladen werden.'},
            ]);
        } finally {
            setIsDownloadingTrace(false);
        }
    };

    return (
        <Stack
            direction="column"
            sx={{
                p: 3,
                height: '100%',
                backgroundColor: 'background.paper',
            }}
        >
            <Box
                ref={chatMessagesBoxRef}
                sx={{
                    overflowY: 'auto',
                    pr: 2,
                }}
            >
                {
                    messageBuffer.map((msg, index) => (
                        <Box key={index} sx={{marginBottom: 2}}>
                            <strong>{msg.role === 'user' ? 'You' : 'AI'}:</strong>
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
                        label="Denke nach..."
                    />
                }
            </Box>

            <Box
                sx={{
                    position: 'relative',
                    mt: 'auto',
                }}
            >
                <TextFieldComponent
                    label="Message"
                    value={message}
                    onChange={setMessage}
                    required
                    multiline
                    rows={4}
                    disabled={isThinking || isLoadingHistory}
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
                            icon: <Send/>,
                            tooltip: "Absenden",
                            onClick: handleSendMessage,
                            disabled: isThinking || isLoadingHistory || message == null || message.trim() === '',
                        }
                    ]}
                />
            </Box>
        </Stack>
    );
}
