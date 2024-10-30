export interface StoreListForm {
    id: string;
    organization: string;
    organization_id: string;
    title: string;
    description_short: string;
    is_public: boolean;
    current_version: string;
    leika_ids: string[];
}