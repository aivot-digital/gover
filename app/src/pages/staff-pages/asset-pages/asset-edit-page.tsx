import {useAuthGuard} from "../../../hooks/use-auth-guard";
import React, {FormEvent, useState} from "react";
import {useNavigate, useParams} from "react-router-dom";
import {Box, Button, Typography} from "@mui/material";
import {useAppDispatch} from "../../../hooks/use-app-dispatch";
import {useUserGuard} from "../../../hooks/use-user-guard";
import {PageWrapper} from "../../../components/page-wrapper/page-wrapper";
import {FileUpload} from "../../../components/file-upload/file-upload";
import {AssetService} from "../../../services/asset-service";

export function AssetEditPage() {
    useAuthGuard();
    useUserGuard(user => user != null && user.admin);

    const navigate = useNavigate();

    const {name} = useParams();

    const [file, setFile] = useState<File[]>([]);
    const [isUploading, setIsUploading] = useState(false);

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault();

        if (file.length === 0) {
            return;
        }

        const form = new FormData();
        form.set('file', file[0]);
        setIsUploading(true);
        AssetService
            .create(form)
            .then(() => navigate('/assets'));
    };

    const handleDelete = () => {
        if (name != null) {
            AssetService.destroy(name);
            navigate('/assets');
        }
    };

    return (
        <PageWrapper
            title="Anlage bearbeiten"
            isLoading={isUploading}
        >
            {
                name != null &&
                name === 'new' &&
                <form onSubmit={handleSubmit}>
                    <Typography
                        variant="h6"
                        sx={{mb: 4}}
                    >
                        Neue Anlage hochladen
                    </Typography>

                    <FileUpload
                        value={file}
                        onChange={setFile}
                        multiple={false}
                    />

                    <Box sx={{mt: 4}}>
                        <Button
                            type="submit"
                            disabled={file.length === 0}
                        >
                            Hochladen
                        </Button>
                    </Box>
                </form>
            }

            {
                name != null &&
                name !== 'new' &&
                <Box>
                    <a
                        href={AssetService.getLink(name)}
                        target="_blank"
                    >
                        {AssetService.getLink(name)}
                    </a>

                    <Box sx={{mt: 4}}>
                        <Button
                            color="error"
                            onClick={handleDelete}
                        >
                            Anlage Löschen
                        </Button>
                    </Box>
                </Box>
            }
        </PageWrapper>
    );
}