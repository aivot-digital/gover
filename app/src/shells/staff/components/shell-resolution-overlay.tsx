import {useEffect, useState} from 'react';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import Button from '@mui/material/Button';
import DesktopWindows from '@aivot/mui-material-symbols-400-outlined/dist/desktop-windows/DesktopWindows';
import OpenInNew from '@aivot/mui-material-symbols-400-outlined/dist/open-in-new/OpenInNew';
import {Paper, useMediaQuery} from '@mui/material';

export function ShellResolutionOverlay() {
    const widthOK = useMediaQuery('(min-width:1280px)');
    const heightOK = useMediaQuery('(min-height:720px)');
    const tooSmall = !(widthOK && heightOK);

    const [dims, setDims] = useState({w: 0, h: 0});

    useEffect(() => {
        const update = () => setDims({w: window.innerWidth, h: window.innerHeight});
        update();
        let raf = 0;
        const onResize = () => {
            cancelAnimationFrame(raf);
            raf = requestAnimationFrame(update);
        };
        window.addEventListener('resize', onResize, {passive: true});
        return () => {
            cancelAnimationFrame(raf);
            window.removeEventListener('resize', onResize);
        };
    }, []);

    if (!tooSmall) return null;

    return (
        <Box
            sx={{
                position: 'fixed',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                bgcolor: '#f7f8fa',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                zIndex: 9999,
                textAlign: 'center',
                p: 3,
                backgroundImage: 'url("/assets/images/blurred-application-bg.jpg")',
                backgroundSize: 'cover',
                backgroundPosition: 'center',
            }}
        >
            <Paper
                sx={{
                    p: 5,
                    maxWidth: 600,
                }}
            >
                <DesktopWindows sx={{fontSize: 64, mb: 2, color: 'primary'}} />
                <Typography
                    variant="h2"
                    sx={{mb: 2}}
                    fontWeight={600}
                >
                    Die Auflösung dieses Fensters oder Bildschirms wird leider nicht unterstützt.
                </Typography>
                <Typography sx={{mb: 3}}>
                    Diese Anwendung enthält komplexe Funktionen und Abläufe, bei denen eine korrekte Darstellung und Wahrnehmung gewährleistet werden muss. Dafür ist es zwingend erforderlich, dass genügend Platz auf dem Bildschirm zur
                    Verfügung steht.
                </Typography>
                <Typography sx={{mb: 4}}>
                    Diese Webanwendung erfordert mindestens <b>1280 × 720 Pixel</b>.
                    <br />
                    Aktuell beträgt die Auflösung Ihres Fensters{' '}
                    <b>{dims.w} × {dims.h}</b>
                    Pixel.
                    <br />
                    Bitte vergrößern Sie Ihr Fenster oder verwenden Sie ein Gerät mit höherer Auflösung,
                    z.&nbsp;B. einen Laptop oder Desktop-PC.
                </Typography>
                <Button
                    variant="contained"
                    onClick={() => window.location.reload()} // Todo: link to Gover docs page
                    startIcon={<OpenInNew />}
                    color="inherit"
                >
                    Systemanforderungen prüfen
                </Button>
            </Paper>
        </Box>
    );
}
