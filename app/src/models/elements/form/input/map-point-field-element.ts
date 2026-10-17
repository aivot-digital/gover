import {BaseInputElement} from '../base-input-element';
import {ElementType} from '../../../../data/element-type/element-type';

export interface MapPointFieldElement extends BaseInputElement<ElementType.MapPoint> {
    zoom: number | null | undefined;
    centerLatitude: number | null | undefined;
    centerLongitude: number | null | undefined;
}

export interface MapPointValue {
    latitude: number | null | undefined;
    longitude: number | null | undefined;
    address: string | null | undefined;
}
