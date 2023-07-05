import {SubmissionListDto} from "./submission-list-dto";
import {CustomerInput} from "../customer-input";

export interface SubmissionDetailsDto extends SubmissionListDto {
    customerInput: CustomerInput;
}