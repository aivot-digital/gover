import {Card, CardContent, Typography} from '@mui/material';

export function NotificationsPanel() {
    return (
        <Card
            sx={{
                height: '100%',
            }}
        >
            <CardContent>
                <Typography
                    variant="h5"
                    component="h3"
                >
                    Benachrichtigungen
                </Typography>

                <Typography
                    variant="body1"
                    sx={{
                        mt: 1,
                    }}
                >
                    Hier finden Sie alle wichtigen Benachrichtigungen und Updates zu Ihrer Gover-Instanz.
                </Typography>
            </CardContent>
        </Card>
    );
}