import {User} from "../../models/entities/user";

export interface SelectUserDialogProps {
    userIdsToIgnore?: number[];

    title: string;
    show: boolean;

    onSelect: (user: User) => void;
    onCancel: () => void;
}