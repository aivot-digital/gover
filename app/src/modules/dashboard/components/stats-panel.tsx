import {Card, CardContent, Typography} from '@mui/material';

export function StatsPanel() {
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
                    Zahlen, Daten & Fakten
                </Typography>

                <Typography
                    variant="body1"
                    sx={{
                        mt: 1,
                    }}
                >
                    Behalten Sie Ihre Gover-Instanz im Blick und entdecken Sie spannende Metriken.
                </Typography>
            </CardContent>
        </Card>
    );
}