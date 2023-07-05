import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {ChangeEvent, useEffect, useState} from "react";
import {Box, FormControlLabel, Switch} from "@mui/material";
import {useNavigate, useParams} from "react-router-dom";
import {ApplicationService} from "../../../services/application-service";
import {Application} from "../../../models/entities/application";
import {SubmissionListDto} from "../../../models/entities/submission-list-dto";
import {SubmissionService} from "../../../services/submission-service";
import {format, parseISO} from "date-fns";
import Chip from "@mui/material/Chip";
import {IconProp} from "@fortawesome/fontawesome-svg-core";
import {
    faBracketsCurly,
    faCircleCheck,
    faCircleX,
    faFileCirclePlus,
    faFilePen,
    faFileZipper,
    faUserEdit,
} from "@fortawesome/pro-light-svg-icons";
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {LocalStorageService} from "../../../services/local-storage-service";
import {LocalstorageKey} from "../../../data/localstorage-key";
import {UsersService} from "../../../services/users-service";
import {User} from "../../../models/entities/user";
import {useAppSelector} from "../../../hooks/use-app-selector";
import {selectUser} from "../../../slices/user-slice";
import {Destination} from "../../../models/entities/destination";
import {DestinationsService} from "../../../services/destinations-service";
import {GridColDef} from "@mui/x-data-grid";
import {isStringNotNullOrEmpty} from "../../../utils/string-utils";
import {TablePageWrapper} from "../../../components/table-page-wrapper/table-page-wrapper";

type Submission = SubmissionListDto & {
    resolvedAssignee?: User;
    resolvedDestination?: Destination;
}

const columns: GridColDef<Submission>[] = [
    {
        field: 'status',
        headerName: 'Status',
        renderCell: params => (
            <Chip
                label={determineLabel(params.row)}
                icon={<FontAwesomeIcon icon={determineIcon(params.row)}/>}
                color={determineColor(params.row)}
                variant="outlined"
                sx={{
                    px: 4,
                    width: '16em',
                }}
            />
        ),
        valueGetter: params => determineLabel(params.row),
        flex: 1,
    },
    {
        field: 'fileNumber',
        headerName: 'Aktenzeichen',
        valueGetter: params => isStringNotNullOrEmpty(params.row.fileNumber) ? params.row.fileNumber : 'Kein Aktenzeichen',
        flex: 1,
    },
    {
        field: 'created',
        headerName: 'Eingangsdatum',
        type: 'date',
        renderCell: params => {
            const created = parseISO(params.row.created);
            return `${format(created, 'dd.MM.yyyy')} - ${format(created, 'HH:mm')} Uhr`;
        },
        valueGetter: params => parseISO(params.row.created),
        flex: 1,
    },
    {
        field: 'assignee',
        headerName: 'Mitarbeiter:in',
        renderCell: params => (
            <>
                {
                    params.row.destination != null &&
                    <FontAwesomeIcon
                        icon={faBracketsCurly}
                        style={{marginRight: '0.5em'}}
                    />
                }

                {
                    params.row.assignee != null &&
                    <FontAwesomeIcon
                        icon={faUserEdit}
                        style={{marginRight: '0.5em'}}
                    />
                }

                {
                    params.row.resolvedDestination != null &&
                    `Schnittstelle "${params.row.resolvedDestination.name}"`
                }

                {
                    params.row.resolvedAssignee != null &&
                    params.row.resolvedAssignee.name
                }
            </>
        ),
        flex: 1,
    },
];

async function resolveSubmission(sub: SubmissionListDto): Promise<Submission> {
    if (sub.destination != null) {
        const dest = await DestinationsService
            .retrieve(sub.destination);
        return {
            ...sub,
            resolvedDestination: dest,
        };
    } else if (sub.assignee != null) {
        const assignee = await UsersService
            .retrieve(sub.assignee);
        return {
            ...sub,
            resolvedAssignee: assignee,
        };
    } else {
        return sub;
    }
}

export function SubmissionListPage() {
    useAuthGuard();
    const navigate = useNavigate();
    const strId = useParams().id;
    const id = strId != null ? parseInt(strId) : undefined;
    const user = useAppSelector(selectUser);

    const [includeArchived, setIncludeArchived] = useState(LocalStorageService.loadFlag(LocalstorageKey.SubmissionsIncludeArchived));
    const [onlyAssigned, setOnlyAssigned] = useState(LocalStorageService.loadFlag(LocalstorageKey.SubmissionsOnlyAssigned));

    const [application, setApplication] = useState<Application>();
    const [submissions, setSubmissions] = useState<Submission[]>();
    const [search, setSearch] = useState('');

    useEffect(() => {
        if (id != null) {
            ApplicationService
                .retrieve(id)
                .then(setApplication);
        }
    }, [id]);

    useEffect(() => {
        if (user != null && id != null) {
            SubmissionService
                .list(id, includeArchived, onlyAssigned ? user.id : undefined)
                .then(submissions => Promise.all(submissions.map(resolveSubmission)))
                .then(setSubmissions);
        }
    }, [id, includeArchived, onlyAssigned]);

    const handleToggleIncludeArchived = (_: ChangeEvent<HTMLInputElement>, checked: boolean) => {
        setIncludeArchived(checked);
        LocalStorageService.storeFlag(LocalstorageKey.SubmissionsIncludeArchived, checked);
    };

    const handleToggleOnlyAssigned = (_: ChangeEvent<HTMLInputElement>, checked: boolean) => {
        setOnlyAssigned(checked);
        LocalStorageService.storeFlag(LocalstorageKey.SubmissionsOnlyAssigned, checked);
    };

    const filteredSubmissions = submissions != null ? submissions.filter(sub => sub.fileNumber == null || sub.fileNumber.toLowerCase().includes(search.toLowerCase())) : undefined;

    return (
        <TablePageWrapper
            title={`Anträge - ${application?.title}`}
            isLoading={application == null || submissions == null}

            rows={filteredSubmissions ?? []}
            columns={columns}
            onRowClick={sub => navigate(`/submissions/${sub.application}/${sub.id}`)}

            search={search}
            onSearchChange={setSearch}
            searchPlaceholder="Aktenzeichen suchen..."

            actions={[]}
        >
            <Box
                sx={{
                    display: 'flex',
                    my: 2
                }}
            >
                <FormControlLabel
                    control={
                        <Switch
                            checked={includeArchived}
                            onChange={handleToggleIncludeArchived}
                        />
                    }
                    label="Abgeschlosse Vorgänge anzeigen"
                />

                <FormControlLabel
                    control={
                        <Switch
                            checked={onlyAssigned}
                            onChange={handleToggleOnlyAssigned}
                        />
                    }
                    label="Mir zugewiesene Vorgänge anzeigen"
                    sx={{mr: 'auto'}}
                />
            </Box>
        </TablePageWrapper>
    );
}

function determineLabel(sub: SubmissionListDto): string {
    if (sub.destination != null) {
        if (sub.destinationSuccess) {
            return 'Übertragen';
        } else {
            return 'Fehlgeschlagen';
        }
    } else if (sub.archived != null) {
        return 'Abgeschlossen';
    } else if (sub.assignee != null) {
        return 'In Bearbeitung';
    } else {
        return 'Offen';
    }
}

function determineIcon(sub: SubmissionListDto): IconProp {
    if (sub.destination != null) {
        if (sub.destinationSuccess) {
            return faCircleCheck;
        } else {
            return faCircleX;
        }
    } else if (sub.archived != null) {
        return faFileZipper;
    } else if (sub.assignee != null) {
        return faFilePen;
    } else {
        return faFileCirclePlus;
    }
}

function determineColor(sub: SubmissionListDto): 'info' | 'error' | 'default' {
    if (sub.destination != null) {
        if (sub.destinationSuccess) {
            return 'default';
        } else {
            return 'error';
        }
    } else if (sub.archived != null) {
        return 'default';
    } else if (sub.assignee != null) {
        return 'info';
    } else {
        return 'error';
    }
}