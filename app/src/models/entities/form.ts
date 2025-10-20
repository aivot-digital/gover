import {FormDetailsResponseDTO} from '../../modules/forms/dtos/form-details-response-dto';

export type Form = FormDetailsResponseDTO;

export function isForm(obj: any): obj is Form {
    return obj != null && 'slug' in obj;
}
