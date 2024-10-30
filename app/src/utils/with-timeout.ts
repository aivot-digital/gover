export function withTimeout<T>(first: () => void, func: () => Promise<T>, after: (result: T) => void, timeout: number): Promise<void> {
    first();

    const start = new Date().getMilliseconds();

    return new Promise(resolve => {
        func()
            .then(res => {
                const deltaTime = new Date().getMilliseconds() - start;
                const remainingTimeout = deltaTime >= timeout ? 1 : timeout - deltaTime;
                setTimeout(() => {
                    after(res);
                    resolve();
                }, remainingTimeout);
            });
    });
}
