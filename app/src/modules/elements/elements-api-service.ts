import {AnyElement} from '../../models/elements/any-element';
import {BaseApiService} from '../../services/base-api-service';

export interface ElementDerivationOptions {
    skipErrorsForElementIds: string[];
    skipVisibilitiesForElementIds: string[];
    skipOverridesForElementIds: string[];
    skipValuesForElementIds: string[];
}

export interface ElementConfigurationErrors {
    /** Error messages keyed by the name of the erroneous property. */
    properties: Record<string, string[]>;
    /** Errors of child elements keyed by child ID. Children without errors are omitted. */
    children: Record<string, ElementConfigurationErrors>;
}

export class ElementsApiService extends BaseApiService {
    public async recalculateReferencedIds<T extends AnyElement>(element: T): Promise<T> {
        return await this.post<T, T>('/api/elements/recalculate-referenced-ids/', element);
    }

    public async validateConfiguration(element: AnyElement, signal?: AbortSignal): Promise<ElementConfigurationErrors> {
        return await this.post<AnyElement, ElementConfigurationErrors>('/api/elements/validate-configuration/', element, {
            abort: signal,
        });
    }
}
