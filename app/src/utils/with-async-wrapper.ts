interface AsyncWrapperOptions<B, M> {
    desiredMinRuntime?: number;
    runtimeCallback?: (isRunning: boolean) => void;
    before?: () => Promise<B>;
    main: (beforeResult: B | undefined, signal?: AbortSignal) => Promise<M>;
    after?: (mainResult: M) => Promise<void>;
    signal?: AbortSignal;
}

export async function withAsyncWrapper<B, M>(options: AsyncWrapperOptions<B, M>): Promise<M> {
    const startTime = new Date().getMilliseconds();

    if (options.runtimeCallback) {
        options.runtimeCallback(true);
    }

    let beforeResult: B | undefined = undefined;
    if (options.before) {
        beforeResult = await options.before();
    }

    if (options.signal?.aborted) {
        throw new DOMException("Aborted", "AbortError");
    }

    const mainResult = await options.main(beforeResult);

    if (options.signal?.aborted) {
        throw new DOMException("Aborted", "AbortError");
    }

    if (options.after) {
        await options.after(mainResult);
    }

    const deltaTime = new Date().getMilliseconds() - startTime;
    const remainingTimeout = options.desiredMinRuntime == null || deltaTime >= options.desiredMinRuntime ? 0 : options.desiredMinRuntime - deltaTime;

    if (remainingTimeout == 0) {
        if (options.runtimeCallback && !options.signal?.aborted) {
            options.runtimeCallback(false);
        }

        return mainResult;
    } else {
        return await new Promise((resolve, reject) => {
            setTimeout(() => {
                if (options.signal?.aborted) {
                    reject(new DOMException("Aborted", "AbortError"));
                    return;
                }

                if (options.runtimeCallback) {
                    options.runtimeCallback(false);
                }

                resolve(mainResult);
            }, remainingTimeout);
        });
    }
}