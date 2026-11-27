const PREPARATION_TIMEOUT = 2_000;

let preparation: (() => Promise<unknown>) | undefined;

/** Lets the open editor store its pending local backup before a reload offered by the application. */
export function registerPageReloadPreparation(prepare: () => Promise<unknown>): () => void {
    preparation = prepare;
    return () => {
        if (preparation === prepare) {
            preparation = undefined;
        }
    };
}

export async function reloadPage(): Promise<void> {
    let timer: number | undefined;
    try {
        // A blocked IndexedDB must not keep the page from reloading.
        await Promise.race([
            preparation?.(),
            new Promise((resolve) => {
                timer = window.setTimeout(resolve, PREPARATION_TIMEOUT);
            }),
        ]);
    } catch (error) {
        console.error(error);
    } finally {
        window.clearTimeout(timer);
    }
    window.location.reload();
}
