import {useCallback, useRef, useState} from 'react';

export type Errors<T> = Partial<Record<keyof T, string>>;

export function useValidationErrors<T>(): {
    validationErrors: Errors<T>;
    hasValidationErrors: () => boolean;
    clearValidationErrors: () => void;
    setValidationError: (key: keyof T, error: string) => void;
} {
    const [errors, setErrors] = useState<Errors<T>>({});
    const errorsRef = useRef<Errors<T>>({});

    const hasErrors = useCallback(() => {
        return Object.keys(errorsRef.current).length > 0;
    }, [errors]);

    const clearErrors = useCallback(() => {
        errorsRef.current = {};
        setErrors({});
    }, [setErrors]);

    const setError = useCallback((key: keyof T, error: string) => {
        const updatedErrors = {
            ...errorsRef.current,
            [key]: error,
        };
        errorsRef.current = updatedErrors;
        setErrors(updatedErrors);
    }, [setErrors]);

    return {
        validationErrors: errors,
        hasValidationErrors: hasErrors,
        clearValidationErrors: clearErrors,
        setValidationError: setError,
    };
}
