import {useMemo, useState} from 'react';
import {ObjectSchema, ValidationError} from 'yup';
import {shallowEquals} from '../utils/equality-utils';

interface FormManager<T> {
    currentItem: T | undefined | null;
    errors: Partial<Record<keyof T, string>>;
    hasNotChanged: boolean;

    handleInputChange: <K extends keyof T>(field: K) => (value: T[K] | undefined) => void;
    handleInputBlur: (field: keyof T) => () => void;

    validate: () => boolean;
    reset: () => void;
}

// TODO: Fix extending type
export function useFormManager<T extends { [key: string]: any }>(originalItem: T | undefined | null, schema: ObjectSchema<T>): FormManager<T> {
    const [editedItem, setEditedItem] = useState<T>();
    const [touchedFields, setTouchedFields] = useState<Partial<Record<keyof T, boolean>>>({});
    const [errors, setErrors] = useState<Partial<Record<keyof T, string>>>({});

    const currentItem = useMemo(() => {
        return editedItem ?? originalItem;
    }, [editedItem, originalItem]);

    const hasNotChanged = useMemo(() => {
        return editedItem == null || shallowEquals(originalItem, editedItem);
    }, [originalItem, editedItem]);

    const handleInputChange = <K extends keyof T>(field: K) => (value: T[K] | undefined) => {
        if (currentItem == null) {
            return;
        }

        setEditedItem({
            ...currentItem,
            [field]: value,
        });

        validateField(field, value);
    };

    const handleInputBlur = (field: keyof T) => () => {
        if (currentItem == null) {
            return;
        }

        setTouchedFields({
            ...touchedFields,
            [field]: true,
        });

        validateField(field, currentItem[field], true);
    };

    const validateField = (field: keyof T, value: T[keyof T] | undefined, validateUntouchedField: boolean = false) => {
        if (!validateUntouchedField && !touchedFields[field]) {
            // Do nothing if the field hasn't been touched or is not forced to validate
            return;
        }

        schema
            .validateAt(field as string, { [field]: value })
            .then(() => {
                setErrors(prevErrors => ({
                    ...prevErrors,
                    [field]: undefined,
                }));
            })
            .catch(err => {
                setErrors(prevErrors => ({
                    ...prevErrors,
                    [field]: err.message || 'Ungültige Eingabe',
                }));
            });
    };

    const validate = () => {
        setErrors({});

        const isYupError = (error: unknown): error is ValidationError =>
            error instanceof ValidationError;

        try {
            schema.validateSync(currentItem, { abortEarly: false });
            return true;
        } catch (error) {
            if (isYupError(error)) {
                const validationErrors: Partial<Record<keyof T, string>> = {};
                error.inner.forEach(err => {
                    if (err.path) {
                        validationErrors[err.path as keyof T] = err.message;
                    }
                });
                setErrors(validationErrors);
            }
            return false;
        }

    };

    const reset = () => {
        setEditedItem(undefined);
        setErrors({});
    };

    return {
        currentItem,
        errors,
        hasNotChanged,

        handleInputChange,
        handleInputBlur,

        validate,
        reset,
    };
}