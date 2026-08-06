import {useEffect, useRef, useState} from 'react';
import type {CSSProperties, HTMLAttributes, Ref} from 'react';

// Importing the altcha package introduces the <altcha-widget> element
import 'altcha';
import type {AltchaWidgetElement} from 'altcha';
import {useLocalStorageEffect} from '../../hooks/use-local-storage-effect';
import {StorageKey} from '../../data/storage-key';
import {createApiPath} from '../../utils/url-path-utils';

interface CaptchaSolution {
    payload: string;
    expiresAt?: number;
}

interface AltchaWidgetProps {
    onChallengeSuccess: (solution: CaptchaSolution) => void;
}

interface AltchaStrings {
    ariaLinkLabel: string;
    enterCode: string;
    enterCodeAria: string;
    error: string;
    expired: string;
    footer: string;
    getAudioChallenge: string;
    label: string;
    loading: string;
    reload: string;
    verificationRequired: string;
    verify: string;
    verified: string;
    verifying: string;
    waitAlert: string;
}

type AltchaWidgetElementProps = HTMLAttributes<HTMLElement> & {
    challenge?: string;
    configuration?: string;
    language?: string;
    ref?: Ref<HTMLElement>;
    style?: CSSProperties & Partial<Record<`--${string}`, string | number>>;
};

declare global {
    namespace JSX {
        interface IntrinsicElements {
            'altcha-widget': AltchaWidgetElementProps;
        }
    }

    namespace React {
        namespace JSX {
            interface IntrinsicElements {
                'altcha-widget': AltchaWidgetElementProps;
            }
        }
    }
}

const localization: AltchaStrings & Record<string, string> = {
    'ariaLinkLabel': 'Webseite von Altcha (altcha.org) aufrufen',
    'enterCode': 'Code eingeben',
    'enterCodeAria': 'Code eingeben',
    'error': 'Verifizierung fehlgeschlagen. Versuchen Sie es später erneut.',
    'expired': 'Verifizierung abgelaufen. Versuchen Sie es erneut.',
    'footer': 'Geschützt mit einer quelloffenen <a href="https://altcha.org/captcha/" target="_blank" aria-label="Webseite von Altcha (altcha.org) aufrufen" title="Webseite von Altcha (altcha.org) aufrufen">Captcha-Lösung</a>',
    'getAudioChallenge': 'Audio-Challenge abrufen',
    'label': 'Ich bin ein Mensch – kein Roboter *',
    'loading': 'Wird geladen...',
    'reload': 'Neu laden',
    'verificationRequired': 'Verifizierung erforderlich.',
    'verify': 'Verifizieren',
    'verified': 'Verifizierung erfolgreich.',
    'verifying': 'Wird überprüft…',
    'waitAlert': 'Wird überprüft… Bitte warten.',
};

globalThis.$altcha?.i18n.set('de', localization);

const extractExpiresAt = (payload: string): number | undefined => {
    const decoded = JSON.parse(atob(payload));
    const rawExpiresAt = decoded?.challenge?.parameters?.expiresAt;

    if (typeof rawExpiresAt === 'number') {
        return rawExpiresAt > 9999999999 ? Math.floor(rawExpiresAt / 1000) : rawExpiresAt;
    }

    const salt = decoded?.salt;
    if (typeof salt === 'string') {
        const match = salt.match(/expires=(\d+)/);
        return match ? parseInt(match[1], 10) : undefined;
    }

    return undefined;
};

export const AltchaWidget = ({onChallengeSuccess}: AltchaWidgetProps) => {
    const widgetRef = useRef<AltchaWidgetElement>(null);
    const [debuggingEnabled, setDebuggingEnabled] = useState<boolean | null>(false);

    useLocalStorageEffect<boolean>(setDebuggingEnabled, StorageKey.CaptchaDebuggerActive);

    useEffect(() => {
        const handleStateChange = (ev: Event | CustomEvent) => {
            if (!('detail' in ev)) return;

            const detail = ev.detail;
            const state = detail.state;
            const payload = detail.payload || null;

            if (state === 'verified' && payload) {
                try {
                    onChallengeSuccess?.({
                        payload,
                        expiresAt: extractExpiresAt(payload),
                    });
                } catch (e) {
                    console.warn('[Altcha] Could not decode payload:', e);
                    onChallengeSuccess?.({
                        payload,
                        expiresAt: undefined,
                    });
                }
            }
        };

        const {current} = widgetRef;

        if (current) {
            current.addEventListener('statechange', handleStateChange);
            return () => current.removeEventListener('statechange', handleStateChange);
        }
    }, [onChallengeSuccess]);

    /* docs: https://altcha.org/docs/v2/widget-v3/ */
    return (
        <altcha-widget
            ref={widgetRef}
            style={{
                '--altcha-max-width': '380px',
                '--altcha-border-color': '#E0E0E0',
                '--altcha-border-radius': '4px',
                '--altcha-checkbox-border-radius': '3px',
            }}
            configuration={JSON.stringify({debug: Boolean(debuggingEnabled), hideFooter: true})}
            language="de"
            challenge={createApiPath('/api/public/captcha/challenge/')}
        />
    );
};
