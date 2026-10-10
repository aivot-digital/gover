import {AnyElement} from '../../models/elements/any-element';
import {BaseApiService} from '../../services/base-api-service';

export interface ElementDerivationOptions {
    skipErrorsForElementIds: string[];
    skipVisibilitiesForElementIds: string[];
    skipOverridesForElementIds: string[];
    skipValuesForElementIds: string[];
}

export class ElementsApiService extends BaseApiService {
    public async recalculateReferencedIds<T extends AnyElement>(element: T): Promise<T> {
        return await this.post<T, T>('/api/elements/recalculate-referenced-ids/', element);
    }
}
