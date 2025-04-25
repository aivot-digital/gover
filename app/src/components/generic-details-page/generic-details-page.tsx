import {GenericDetailsPageProps} from './generic-details-page-props';
import {Box, Button, Container, Paper, Stack, Tab, Tabs, Typography} from '@mui/material';
import React, {useEffect, useMemo, useState} from 'react';
import {Api, useApi} from '../../hooks/use-api';
import {GenericPageHeader} from '../generic-page-header/generic-page-header';
import {generatePath, Link, matchPath, Outlet, useLocation, useNavigate, useParams} from 'react-router-dom';
import {GenericDetailsPageContext} from './generic-details-page-context';
import {ApiError} from '../../models/api-error';
import {ReactComponent as NotFoundIllustration} from './resource-not-found-illustration.svg';
import ArrowBackOutlinedIcon from "@mui/icons-material/ArrowBackOutlined";
import FormatListBulletedOutlinedIcon from "@mui/icons-material/FormatListBulletedOutlined";

export const DEFAULT_ID_PARAM = 'id';
export const NEW_ID_INDICATOR = 'new';

interface DataFetchResult<ItemType, AdditionalData> {
    item: ItemType;
    additionalData: AdditionalData;
}

async function fetchData<ItemType, ID, AdditionalData>(api: Api, id: ID, props: GenericDetailsPageProps<ItemType, ID, AdditionalData>): Promise<DataFetchResult<ItemType, AdditionalData>> {
    let item: ItemType;
    if (id === NEW_ID_INDICATOR) {
        item = props.initializeItem(api);
    } else {
        item = await props.fetchData(api, id);
    }

    let additionalData: Partial<AdditionalData> = {};
    if (props.fetchAdditionalData != null) {
        for (const key in props.fetchAdditionalData) {
            additionalData[key] = await props.fetchAdditionalData[key](api, id);
        }
    }

    return Promise.resolve({
        item: item,
        additionalData: additionalData as AdditionalData,
    });
}

export function GenericDetailsPage<ItemType, ID, AdditionalData>(props: GenericDetailsPageProps<ItemType, ID, AdditionalData>) {
    const api = useApi();
    const params = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const [notFound, setNotFound] = useState(false);

    const ID_PARAM = props.idParam ?? DEFAULT_ID_PARAM;
    const id = useMemo(() => {
        return params[ID_PARAM] as ID;
    }, [params]);

    const [isBusy, setIsBusy] = useState(false);
    const [item, setItem] = useState<ItemType>();
    const [additionalData, setAdditionalData] = useState<AdditionalData>();

    const resolvedTabs = useMemo(() => {
        if (typeof props.tabs === 'function') {
            return props.tabs(item);
        }
        return props.tabs;
    }, [props.tabs, item]);

    const currentTab: number = useMemo(() => {
        return resolvedTabs
            .map(tab => generatePath(tab.path, {[ID_PARAM]: id}))
            .findIndex(path => matchPath(location.pathname, path) != null);
    }, [id, location, resolvedTabs]);

    useEffect(() => {
        console.log(resolvedTabs);
    }, [resolvedTabs]);

    useEffect(() => {
        if (id == null) {
            setItem(undefined);
            setAdditionalData(undefined);
            return;
        }

        setIsBusy(true);
        fetchData<ItemType, ID, AdditionalData>(api, id, props)
            .then(({item, additionalData}) => {
                setItem(item);
                setAdditionalData(additionalData);
                setNotFound(false);
            })
            .catch((error: ApiError) => {
                console.error(error);
                setNotFound(true);
            })
            .finally(() => setIsBusy(false));
    }, [api, id]);

    const headerTitle = useMemo(() => {
        if (props.getHeaderTitle) {
            return props.getHeaderTitle(item, id === NEW_ID_INDICATOR, notFound);
        }
        return props.header.title ?? 'Resource bearbeiten'; // use static title as fallback, if defined
    }, [item, id, notFound]);

    return (
        <>
            <Container>
                <GenericPageHeader {...props.header} title={isBusy ? "Wird geladen…" : headerTitle} isBusy={isBusy} />

                <Paper
                    sx={{
                        marginTop: 3.5,
                    }}
                >
                    {
                        resolvedTabs.length > 1 && !notFound &&
                            <Box
                                sx={{
                                    borderBottom: 1,
                                    borderBottomColor: 'divider',
                                }}
                            >

                                        <Tabs
                                            sx={{
                                                flex: 1,
                                            }}
                                            value={currentTab}
                                            onChange={(_, index: number) => {
                                                const tab = resolvedTabs[index];
                                                navigate(generatePath(tab.path, {[ID_PARAM]: id}));
                                            }}
                                        >
                                            {
                                                resolvedTabs.length > 1 &&
                                                resolvedTabs.map(({path, label, isDisabled}, index) => (
                                                    <Tab
                                                        key={path}
                                                        value={index}
                                                        label={label}
                                                        disabled={isDisabled?.(item)}
                                                    />
                                                ))
                                            }
                                        </Tabs>

                            </Box>
                    }
                    <Box
                        sx={{
                            padding: 2,
                        }}
                    >
                        {
                            notFound ?
                                <Stack
                                    direction="column"
                                    gap={2}
                                    sx={{
                                        maxWidth: 440,
                                        margin: "40px auto 60px auto",
                                        textAlign: "center",
                                        alignItems: "center",
                                    }}
                                >
                                    <NotFoundIllustration/>
                                    <Typography variant={"h2"} sx={{marginTop: 1}}>Diese Ressource konnte leider nicht (mehr) gefunden werden</Typography>
                                    <Typography>
                                        Die angeforderte Ressource existiert nicht oder wurde möglicherweise entfernt.
                                        Bitte überprüfen Sie die URL oder nutzen Sie eine der folgenden Möglichkeiten.
                                    </Typography>
                                    <Stack direction={"row"} gap={2} sx={{marginTop: 1.5}}>
                                        {
                                            props.parentLink &&
                                            <Button
                                                variant={"contained"}
                                                size={"small"}
                                                startIcon={<FormatListBulletedOutlinedIcon />}
                                                component={Link}
                                                to={props.parentLink.to}
                                            >
                                                {props.parentLink.label}
                                            </Button>
                                        }
                                        <Button
                                            variant={"outlined"}
                                            size={"small"}
                                            startIcon={<ArrowBackOutlinedIcon />}
                                            onClick={() => {
                                                if (window.history.length > 2) {
                                                    navigate(-1);
                                                } else {
                                                    navigate('/');
                                                }
                                            }}
                                        >
                                            Zurück
                                        </Button>
                                    </Stack>
                                </Stack> :
                                <GenericDetailsPageContext.Provider
                                    value={{
                                        item: item,
                                        setItem: setItem,
                                        isNewItem: id === NEW_ID_INDICATOR,
                                        isExistingItem: id !== NEW_ID_INDICATOR,
                                        additionalData: additionalData,
                                        setAdditionalData: setAdditionalData,
                                        isBusy: isBusy,
                                        setIsBusy: setIsBusy,
                                    }}
                                >
                                    <Outlet />
                                </GenericDetailsPageContext.Provider>
                        }
                    </Box>
                </Paper>
            </Container>
        </>
    );
}