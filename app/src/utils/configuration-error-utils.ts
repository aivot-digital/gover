import {ElementType} from '../data/element-type/element-type';

/** Configuration summaries are opt-in; other elements retain their existing error semantics. */
export function getConfigurationErrorMessages(type: ElementType, errors: string[] | null | undefined, details: unknown): string[] {
    if (type !== ElementType.PaymentConfigElement && type !== ElementType.IdentityConfigElement) {
        return errors ?? [];
    }
    if (details != null && typeof details === 'object' && 'messages' in details && Array.isArray(details.messages)) {
        const messages = details.messages.filter((message): message is string => typeof message === 'string' && message.trim().length > 0);
        if (messages.length > 0) {
            // The backend includes provider validation in this list before returning the configuration.
            return messages;
        }
    }
    return (errors ?? []).filter((message) => message.trim().length > 0);
}
