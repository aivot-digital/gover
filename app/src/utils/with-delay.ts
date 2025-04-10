/**
 * @deprecated Use withAsyncWrapper instead
 * @param promise
 * @param delay
 */
export async function delayPromise<T>(promise: Promise<T>, delay?: number): Promise<T> {
    const _delay = delay ?? 500;
    const start = new Date().getMilliseconds();
    const res: T = await promise;

    const deltaTime = new Date().getMilliseconds() - start;
    if (deltaTime > _delay) {
        return res;
    }
    const remainingDelay = _delay - deltaTime;

    await new Promise<void>((resolve) => {
        setTimeout(() => {
            resolve();
        }, remainingDelay);
    });

    return res;
}
