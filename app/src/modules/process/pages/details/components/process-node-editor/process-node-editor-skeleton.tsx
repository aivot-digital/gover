import React, {type ReactNode} from 'react';
import {Box, Skeleton} from '@mui/material';

interface ProcessNodeEditorSkeletonProps {
    height?: number | string;
}

const TAB_SKELETON_WIDTHS = [112, 124, 96, 84];

export function ProcessNodeEditorSkeleton(props: ProcessNodeEditorSkeletonProps): ReactNode {
    const {
        height = '100vh',
    } = props;

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                height,
            }}
        >
            <Box
                sx={{
                    flex: 1,
                    display: 'flex',
                    flexDirection: 'column',
                    overflow: 'hidden',
                }}
            >
                <Box
                    sx={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 2,
                        px: 2,
                        pt: 1,
                        minWidth: 0,
                    }}
                >
                    <Skeleton
                        variant="circular"
                        width={32}
                        height={32}
                    />

                    <Box
                        sx={{
                            flex: 1,
                            minWidth: 0,
                        }}
                    >
                        <Skeleton width={88} height={16}/>

                        <Box
                            sx={{
                                mt: 0.25,
                                display: 'flex',
                                alignItems: 'center',
                                gap: 1,
                                minWidth: 0,
                            }}
                        >
                            <Skeleton
                                width="62%"
                                height={28}
                            />
                            <Skeleton
                                variant="rounded"
                                width={86}
                                height={24}
                            />
                        </Box>
                    </Box>

                    <Skeleton
                        variant="circular"
                        width={36}
                        height={36}
                    />
                </Box>

                <Box
                    sx={{
                        mt: 1,
                        px: 2,
                        pb: 0.75,
                        display: 'flex',
                        alignItems: 'center',
                        gap: 1.5,
                        borderBottom: '1px solid #ddd',
                    }}
                >
                    {
                        TAB_SKELETON_WIDTHS.map((width, index) => (
                            <Skeleton
                                key={index}
                                variant="rounded"
                                width={width}
                                height={32}
                            />
                        ))
                    }
                </Box>

                <Box
                    sx={{
                        px: 2,
                        py: 2,
                        flex: 1,
                        overflow: 'hidden',
                    }}
                >
                    <Box
                        sx={{
                            display: 'flex',
                            flexDirection: 'column',
                            gap: 2.5,
                        }}
                    >
                        <Box>
                            <Skeleton width={132} height={18}/>
                            <Skeleton variant="rounded" height={46}/>
                        </Box>

                        <Box>
                            <Skeleton width={116} height={18}/>
                            <Skeleton variant="rounded" height={46}/>
                        </Box>

                        <Box>
                            <Skeleton width={148} height={18}/>
                            <Skeleton variant="rounded" height={112}/>
                        </Box>

                        <Box
                            sx={{
                                display: 'grid',
                                gridTemplateColumns: '1fr 1fr',
                                gap: 2,
                            }}
                        >
                            <Box>
                                <Skeleton width={96} height={18}/>
                                <Skeleton variant="rounded" height={46}/>
                            </Box>
                            <Box>
                                <Skeleton width={104} height={18}/>
                                <Skeleton variant="rounded" height={46}/>
                            </Box>
                        </Box>
                    </Box>
                </Box>
            </Box>

            <Box
                sx={{
                    borderTop: '1px solid #ddd',
                    mt: 'auto',
                    px: 2,
                    pt: 2,
                    pb: 2.5,
                    display: 'flex',
                    justifyContent: 'space-between',
                    gap: 1.5,
                }}
            >
                <Skeleton
                    variant="rounded"
                    width={184}
                    height={36}
                />
                <Skeleton
                    variant="rounded"
                    width={112}
                    height={36}
                />
            </Box>
        </Box>
    );
}
