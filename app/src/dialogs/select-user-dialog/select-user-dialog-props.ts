import {User} from "../../models/entities/user";

export interface SelectUserDialogProps {
    userIdsToIgnore?: string[];

    title: string;
    show: boolean;

    onSelect: (user: User) => void;
    onCancel: () => void;
}
