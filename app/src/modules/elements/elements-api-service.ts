import {AuthoredElementValues, DerivedRuntimeElementData} from '../../models/element-data';
import {AnyElement} from '../../models/elements/any-element';
import {BaseApiService, RequestOptions} from '../../services/base-api-service';

interface ElementDerivationOptions {
    skipErrorsForElementIds: string[];
    skipVisibilitiesForElementIds: string[];
    skipOverridesForElementIds: string[];
    skipValuesForElementIds: string[];
}

export interface ProcessExecutionData {
    $: Record<string, any>;
    $$: Record<string, any>;
    _: Record<string, any>;
}

export interface ElementDerivationRequest {
    element: AnyElement;
    authoredElementValues: AuthoredElementValues;
    derivationOptions: ElementDerivationOptions;
    processExecutionData: ProcessExecutionData;
}

export class ElementsApiService extends BaseApiService {
    public async derive(request: ElementDerivationRequest, opt?: RequestOptions): Promise<DerivedRuntimeElementData> {
        return await this.post<ElementDerivationRequest, DerivedRuntimeElementData>('/api/elements/derive/', request, opt);
    }

    public async recalculateReferencedIds<T extends AnyElement>(element: T): Promise<T> {
        return await this.post<T, T>('/api/elements/recalculate-referenced-ids/', element);
    }
}
