import { type SubmissionListDto } from './submission-list-dto';
import { type CustomerInput } from '../customer-input';

export interface SubmissionDetailsDto extends SubmissionListDto {
    customerInput: CustomerInput;
}
