/** The refresh name is reserved for the built-in event and has no payload. */
export type GenericDetailsPageEventMap<Events extends object = {}> = Omit<Events, 'refresh'> & {
    refresh: undefined;
};

type CustomEventName<Events extends object> = Exclude<Extract<keyof Events, string>, 'refresh'>;
type EventListener<Payload> = (payload: Payload) => void | Promise<void>;

export type GenericDetailsPageEventName<Events extends object = {}> =
    Extract<keyof GenericDetailsPageEventMap<Events>, string>;

type EmitEventArguments<Events extends object> = [name: 'refresh', payload?: undefined] | {
    [Name in CustomEventName<Events>]: undefined extends Events[Name]
        ? [name: Name, payload?: Events[Name]]
        : [name: Name, payload: Events[Name]];
}[CustomEventName<Events>];

export type GenericDetailsPageEmitEvent<Events extends object = {}> =
    (...args: EmitEventArguments<Events>) => void;

export type GenericDetailsPageSubscribeEvent<Events extends object = {}> =
    <Name extends GenericDetailsPageEventName<Events>>(
        name: Name,
        listener: EventListener<GenericDetailsPageEventMap<Events>[Name]>,
    ) => () => void;

type RegisteredListener = EventListener<unknown>;

export function createGenericDetailsPageEventChannel<Events extends object = {}>(): {
    emitEvent: GenericDetailsPageEmitEvent<Events>;
    subscribeEvent: GenericDetailsPageSubscribeEvent<Events>;
} {
    const listeners = new Map<string, Set<RegisteredListener>>();

    const subscribeEvent: GenericDetailsPageSubscribeEvent<Events> = (name, listener) => {
        // Each subscription owns its registration, even when the same callback is used twice.
        const registeredListener: RegisteredListener = payload => (listener as RegisteredListener)(payload);
        const eventListeners = listeners.get(name) ?? new Set<RegisteredListener>();
        eventListeners.add(registeredListener);
        listeners.set(name, eventListeners);

        return () => {
            eventListeners.delete(registeredListener);
            if (eventListeners.size === 0 && listeners.get(name) === eventListeners) {
                listeners.delete(name);
            }
        };
    };

    const emitEvent: GenericDetailsPageEmitEvent<Events> = (...[name, payload]) => {
        const eventListeners = listeners.get(name);
        if (eventListeners == null) {
            return;
        }

        for (const listener of [...eventListeners]) {
            if (!eventListeners.has(listener)) {
                continue;
            }
            try {
                void Promise.resolve(listener(payload)).catch(reportListenerError);
            } catch (error) {
                reportListenerError(error);
            }
        }
    };

    return {emitEvent, subscribeEvent};
}

function reportListenerError(error: unknown): void {
    console.error('Error handling a GenericDetailsPage event.', error);
}
