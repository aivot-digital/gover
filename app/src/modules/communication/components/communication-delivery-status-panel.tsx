import {Alert, AlertTitle, Box, Typography} from '@mui/material';
import {useEffect, useState} from 'react';
import {
    CommunicationDeliveriesApiService,
    CommunicationDeliveryStatus,
    type CommunicationDeliveryView,
} from '../communication-deliveries-api-service';

type Props = ({taskId: number} | {providerId: number; deliveryId: string}) & {onStatusChange?: (status: CommunicationDeliveryStatus) => void};

export function CommunicationDeliveryStatusPanel(props: Props) {
    const {onStatusChange} = props;
    const taskId = 'taskId' in props ? props.taskId : undefined;
    const providerId = 'providerId' in props ? props.providerId : undefined;
    const deliveryId = 'deliveryId' in props ? props.deliveryId : undefined;
    const [delivery, setDelivery] = useState<CommunicationDeliveryView | null>(null);
    const [failed, setFailed] = useState(false);

    useEffect(() => {
        let active = true;
        let timer: ReturnType<typeof setTimeout> | undefined;
        const service = new CommunicationDeliveriesApiService();
        setDelivery(null);
        setFailed(false);
        const update = async () => {
            try {
                const result = taskId != null
                    ? await service.forTask(taskId)
                    : await service.forTest(providerId!, deliveryId!);
                if (!active) return;
                setDelivery(result);
                if (result != null) onStatusChange?.(result.status);
                setFailed(false);
                if (result?.checking) timer = setTimeout(update, 10000);
            } catch {
                if (!active) return;
                setFailed(true);
                timer = setTimeout(update, 10000);
            }
        };
        void update();
        return () => {
            active = false;
            clearTimeout(timer);
        };
    }, [taskId, providerId, deliveryId, onStatusChange]);

    if (delivery == null && !failed) return null;
    const severity = delivery?.status === CommunicationDeliveryStatus.Accepted ? 'success'
        : delivery?.status === CommunicationDeliveryStatus.Rejected || delivery?.status === CommunicationDeliveryStatus.Failed ? 'error'
            : delivery?.status === CommunicationDeliveryStatus.Unknown ? 'warning' : 'info';

    return (
        <Box sx={{my: 2}} aria-live="polite">
            {failed && <Alert severity="warning">Der aktuelle Versandstatus konnte nicht geladen werden. Die Abfrage wird wiederholt.</Alert>}
            {delivery != null && (
                <Alert severity={severity}>
                    <AlertTitle>{delivery.label}</AlertTitle>
                    {delivery.message && <Typography>{delivery.message}</Typography>}
                    {delivery.status === CommunicationDeliveryStatus.Submitted && (
                        <Typography>Die Nachricht wurde an FIT-Connect übergeben. Die Bestätigung des Postfachadapters steht noch aus.</Typography>
                    )}
                    {delivery.overdue && delivery.status === CommunicationDeliveryStatus.Submitted && (
                        <Typography>Seit mehr als 24 Stunden liegt keine Zustellbestätigung vor. Die Prüfung wird fortgesetzt.</Typography>
                    )}
                    {delivery.receipt.problems?.map((problem, index) => (
                        <Box key={index} sx={{mt: 1, overflowWrap: 'anywhere'}}>
                            <Typography>{problem.title}{problem.detail ? `: ${problem.detail}` : ''}</Typography>
                            {problem.instance && <Typography variant="body2">Betroffener Bereich: {problem.instance}</Typography>}
                            {problem.type && <Typography variant="body2">Fehlertyp: {problem.type}</Typography>}
                        </Box>
                    ))}
                    {delivery.receipt.submissionId && <Typography variant="body2" sx={{mt: 1, overflowWrap: 'anywhere'}}>Submission-ID: {delivery.receipt.submissionId}</Typography>}
                    {delivery.receipt.caseId && <Typography variant="body2" sx={{overflowWrap: 'anywhere'}}>FIT-Connect-Case-ID: {delivery.receipt.caseId}</Typography>}
                    {delivery.receipt.eventId && <Typography variant="body2" sx={{overflowWrap: 'anywhere'}}>Event-ID: {delivery.receipt.eventId}</Typography>}
                </Alert>
            )}
        </Box>
    );
}
