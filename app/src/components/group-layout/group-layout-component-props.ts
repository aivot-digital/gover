import {type AnyElement} from '../../models/elements/any-element';

export interface GroupLayoutComponentProps {
    allElements: AnyElement[];
    children: AnyElement[];
    idPrefix?: string;
    isBusy: boolean;
    isDeriving: boolean;
    valueOverride?: {
        values: Record<string, any>;
        onChange: (key: string, value: any) => void;
    };
    errorsOverride?: Record<string, string>;
}
