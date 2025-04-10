import {Box, Card, CardActionArea, CardContent, Container, Grid, Typography,} from '@mui/material';
import React, {useEffect, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {AppFooter} from '../../../components/app-footer/app-footer';
import {MetaElement} from '../../../components/meta-element/meta-element';
import {AppHeader} from '../../../components/app-header/app-header';
import {AppMode} from '../../../data/app-mode';
import {useAppSelector} from '../../../hooks/use-app-selector';
import {selectSystemConfigValue} from '../../../slices/system-config-slice';
import {SystemConfigKeys} from '../../../data/system-config-keys';
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined';
import TaskOutlinedIcon from '@mui/icons-material/TaskOutlined';
import {CheckboxFieldComponent} from '../../../components/checkbox-field/checkbox-field-component';
import {ProviderLinksGrid} from '../../../modules/provider-links/components/provider-links-grid';
import {StorageScope, StorageService} from '../../../services/storage-service';
import {StorageKey} from '../../../data/storage-key';
import {Introductory} from '../../../components/introductory/introductory';

const moduleLinks = [
    {
        label: 'Formularverwaltung',
        description: 'Verwalten und Erstellen Sie moderne Online-Formulare. Einfach, intuitiv und mittels Low-/No-Code anpassbar.',
        to: '/forms',
        icon: <DescriptionOutlinedIcon
            sx={{color: 'primary'}}
            fontSize={"large"}
        />,
        adminOnly: false,
    },
    {
        label: 'Antragsverarbeitung',
        description: 'Verwalten und Bearbeiten Sie Anträge direkt in Gover. Alle Informationen inkl. Zuständigkeiten auf einen Blick.',
        to: '/submissions',
        icon: <TaskOutlinedIcon
            sx={{color: 'primary'}}
            fontSize={"large"}
        />,
        adminOnly: false,
    },
];

export function ModuleSelectPage(): JSX.Element {
    const navigate = useNavigate();
    const providerName = useAppSelector(selectSystemConfigValue(SystemConfigKeys.provider.name));
    const [saveSelection, setSaveSelection] = useState(false);

    useEffect(() => {
        const savedModule = StorageService
            .loadString(StorageKey.SavedModule);
        if (savedModule != null) {
            navigate(savedModule);
        }
    }, []);

    return (
        <>
            <MetaElement
                title={providerName != null && providerName.length > 0 ? providerName : 'powered by Aivot'}
            />

            <AppHeader
                mode={AppMode.Staff}
            />

            <Introductory
                mode={AppMode.Staff}
            />

            <Box
                sx={{
                    backgroundColor: '#F3F3F3',
                    minHeight: '60vh',
                }}
            >
                <Container
                    sx={{
                        mb: 5,
                        py: 7,
                    }}
                >
                    <Typography
                        variant="h2"
                        component="h2"
                    >
                        Modul auswählen
                    </Typography>
                    <Typography variant={"body2"}
                                sx={{mt: 1}}>
                        Bitte wählen Sie das Gover-Modul aus, das Sie verwenden möchten.
                    </Typography>


                    <Grid container
                          spacing={4}
                          sx={{mt: 2, mb: 3}}>
                        {
                            moduleLinks.map(linkItem => (
                                <Grid item
                                      md={6}
                                      key={linkItem.to}>
                                    <Card sx={{maxWidth: "100%"}}>
                                        <CardActionArea onClick={() => {
                                            if (saveSelection) {
                                                StorageService
                                                    .storeString(
                                                        StorageKey.SavedModule,
                                                        linkItem.to,
                                                        StorageScope.Local,
                                                    );
                                            }
                                            navigate(linkItem.to);
                                        }}>
                                            <CardContent sx={{display: "flex"}}>

                                                <Box
                                                    sx={{
                                                        borderRight: "1px solid #F3F3F3",
                                                        pr: 2, mr: 2.5
                                                    }}
                                                >
                                                    {linkItem.icon}
                                                </Box>
                                                <Box
                                                    sx={{pb: 1.5}}
                                                >
                                                    <Typography gutterBottom
                                                                variant="h5"
                                                                component="div"
                                                                sx={{mt: .7}}>
                                                        {linkItem.label}
                                                    </Typography>
                                                    <Typography variant="body2"
                                                                color="text.secondary">
                                                        {linkItem.description}
                                                    </Typography>
                                                </Box>

                                            </CardContent>
                                        </CardActionArea>
                                    </Card>
                                </Grid>
                            ))}

                    </Grid>

                    <CheckboxFieldComponent
                        label="Auswahl für die Zukunft merken"
                        value={saveSelection}
                        onChange={setSaveSelection}
                        hint="Sie können Ihre Auswahl jederzeit ändern oder diese Option deaktivieren."
                    />
                </Container>
            </Box>

            <ProviderLinksGrid/>

            <AppFooter mode={AppMode.Staff}/>
        </>
    );
}

