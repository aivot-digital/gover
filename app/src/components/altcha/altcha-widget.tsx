import { useEffect, useRef, useState, forwardRef, useImperativeHandle } from 'react'

// Importing the altcha package introduces the <altcha-widget> element
import 'altcha'

interface CaptchaSolution {
    payload: string
    expiresAt?: number
}

interface AltchaWidgetProps {
    onChallengeSuccess: (solution: CaptchaSolution) => void
}

const localization = {
    "ariaLinkLabel": "Webseite von Altcha (altcha.org) aufrufen",
    "error": "Verifizierung fehlgeschlagen. Versuchen Sie es später erneut.",
    "expired": "Verifizierung abgelaufen. Versuchen Sie es erneut.",
    "footer": "Geschützt mit einer quelloffenen <a href=\"https://altcha.org/captcha/\" target=\"_blank\" aria-label=\"Webseite von Altcha (altcha.org) aufrufen\" title=\"Webseite von Altcha (altcha.org) aufrufen\">Captcha-Lösung</a>",
    "label": "Ich bin ein Mensch – kein Roboter *",
    "verified": "Verifizierung erfolgreich.",
    "verifying": "Wird überprüft…",
    "waitAlert": "Wird überprüft… Bitte warten."
};

const AltchaWidget = forwardRef<{ value: string | null }, AltchaWidgetProps>(({ onChallengeSuccess }, ref) => {
    const widgetRef = useRef<AltchaWidget & AltchaWidgetMethods & HTMLElement>(null)
    const [value, setValue] = useState<string | null>(null)

    useImperativeHandle(ref, () => {
        return {
            get value() {
                return value
            }
        }
    }, [value])

    useEffect(() => {
        const handleStateChange = (ev: Event | CustomEvent) => {
            if (!('detail' in ev)) return;

            const detail = ev.detail;
            const state = detail.state;
            const payload = detail.payload || null;
            setValue(ev.detail.payload || null);

            if (state === 'verified' && payload) {
                try {
                    const decoded = JSON.parse(atob(payload));
                    const salt = decoded.salt as string;
                    const match = salt.match(/expires=(\d+)/);
                    const expiresAt = match ? parseInt(match[1], 10) : undefined;

                    onChallengeSuccess?.({
                        payload,
                        expiresAt,
                    });
                } catch (e) {
                    console.warn('[Altcha] Could not decode payload:', e);
                    onChallengeSuccess?.({
                        payload,
                        expiresAt: undefined,
                    });
                }
            }
        }

        const { current } = widgetRef

        if (current) {
            current.addEventListener('statechange', handleStateChange)
            return () => current.removeEventListener('statechange', handleStateChange)
        }
    }, [onChallengeSuccess])

    /* docs: https://altcha.org/docs/website-integration/#using-altcha-widget */
    return (
        <altcha-widget
            ref={widgetRef}
            style={{
                '--altcha-max-width': '380px',
                '--altcha-color-border': '#E0E0E0',
                '--altcha-color-border-focus': '#E0E0E0',
                '--altcha-border-radius': '4px',
            }}
            debug
            strings={JSON.stringify(localization)}
            challengeurl={"/api/public/captcha/challenge/"}
        />
    )
})

export default AltchaWidget
