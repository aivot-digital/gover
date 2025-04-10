import QRCode from 'qrcode';

export async function downloadQrCode(url: string, filename: string = 'qr-code.png') {
    try {
        // QR-Code als Data URL (PNG-Format, 1024x1024 px) generieren
        const qrCodeDataUrl = await QRCode.toDataURL(url, { width: 1024 });

        // Erstelle einen temporären Download-Link
        const link = document.createElement('a');
        link.href = qrCodeDataUrl;
        link.download = filename;

        // Füge den Link zum DOM hinzu und simuliere einen Klick
        document.body.appendChild(link);
        link.click();

        // Entferne den Link nach dem Download
        document.body.removeChild(link);
    } catch (err) {
        console.error('Fehler beim Erstellen des QR-Codes:', err);
    }
}
