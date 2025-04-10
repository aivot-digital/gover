import {Box, LinearProgress, Typography, useTheme} from '@mui/material';
import React, {ReactNode} from 'react';

export function Loader({message}: { message?: string | ReactNode }) {
    const theme = useTheme();

    return (
        <>
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'flex-end',
                    alignItems: 'center',
                }}
            >
                <Typography
                    variant="h6"
                    sx={{
                        mr: 'auto',
                        pr: 1,
                        lineHeight: 1.2,
                    }}
                >
                    {message ? message : 'Daten werden geladen'}…
                </Typography>
                <svg
                    id={"gover-decal-loader"}
                    height={32}
                    style={{flexShrink: 0}}
                    viewBox="0 0 460 110"
                    fill="none"
                    xmlns="http://www.w3.org/2000/svg"
                >
                    <style>
                        {`
                          @keyframes blink {
                            50% {
                              fill-opacity: .5;
                            }
                            100% {
                              fill-opacity: 1;
                            }
                          }
                          #gover-decal-loader g > path:first-child {
                            animation: blink 2s infinite;
                          }
                          #gover-decal-loader g:nth-of-type(1) > path {
                            animation-delay: .5s;
                          }
                          #gover-decal-loader g:nth-of-type(2) > path {
                            animation-delay: 1s;
                          }
                          #gover-decal-loader g:nth-of-type(3) > path {
                            animation-delay: .25s;
                          }
                          #gover-decal-loader g:nth-of-type(4) > path {
                            animation-delay: .75s;
                          }
                        `}
                    </style>
                    <g>
                        <path
                            d="M40.3775 3.56515C46.3319 0.144951 53.6681 0.144949 59.6225 3.56515L90.3775 21.2307C96.332 24.6509 100 30.9717 100 37.8121V73.1431C100 79.9835 96.332 86.3043 90.3775 89.7245L59.6225 107.39C53.6681 110.81 46.3319 110.81 40.3775 107.39L9.62251 89.7245C3.66808 86.3043 0 79.9835 0 73.1431V37.8121C0 30.9717 3.66808 24.6509 9.62251 21.2307L40.3775 3.56515Z"
                            fill={theme.palette.primary.main}
                        />
                        <circle cx={50}
                                cy={55}
                                r={14}
                                fill="#FFFFFF"/>
                    </g>
                    <g>
                        <path
                            d="M160.378 2.56515C166.332 -0.855049 173.668 -0.855051 179.623 2.56515L210.377 20.2307C216.332 23.6509 220 29.9717 220 36.8121V72.1431C220 78.9835 216.332 85.3043 210.377 88.7245L179.623 106.39C173.668 109.81 166.332 109.81 160.378 106.39L129.623 88.7245C123.668 85.3043 120 78.9835 120 72.1431V36.8121C120 29.9717 123.668 23.6509 129.623 20.2307L160.378 2.56515Z"
                            fill={theme.palette.primary.light}
                        />
                    </g>
                    <g>
                        <path
                            d="M280.378 2.56515C286.332 -0.855049 293.668 -0.855051 299.623 2.56515L330.377 20.2307C336.332 23.6509 340 29.9717 340 36.8121V72.1431C340 78.9835 336.332 85.3043 330.377 88.7245L299.623 106.39C293.668 109.81 286.332 109.81 280.378 106.39L249.623 88.7245C243.668 85.3043 240 78.9835 240 72.1431V36.8121C240 29.9717 243.668 23.6509 249.623 20.2307L280.378 2.56515Z"
                            fill={theme.palette.primary.main}
                        />
                        <path
                            d="M312 54C312 45.7157 305.284 39 297 39H240V69H297C305.284 69 312 62.2843 312 54V54Z"
                            fill="#FFFFFF"
                        />
                    </g>
                    <g>
                        <path
                            d="M400.378 3.56515C406.332 0.144951 413.668 0.144949 419.623 3.56515L450.377 21.2307C456.332 24.6509 460 30.9717 460 37.8121V73.1431C460 79.9835 456.332 86.3043 450.377 89.7245L419.623 107.39C413.668 110.81 406.332 110.81 400.378 107.39L369.623 89.7245C363.668 86.3043 360 79.9835 360 73.1431V37.8121C360 30.9717 363.668 24.6509 369.623 21.2307L400.378 3.56515Z"
                            fill={theme.palette.primary.light}
                        />
                        <path
                            d="M405.189 29.2826C408.166 27.5725 411.834 27.5725 414.811 29.2826L430.189 38.1154C433.166 39.8255 435 42.9858 435 46.4061V64.0715C435 67.4917 433.166 70.6521 430.189 72.3622L414.811 81.195C411.834 82.9051 408.166 82.9051 405.189 81.195L389.811 72.3622C386.834 70.6521 385 67.4917 385 64.0715V46.4061C385 42.9858 386.834 39.8255 389.811 38.1154L405.189 29.2826Z"
                            fill="#FFFFFF"
                        />
                    </g>
                </svg>
            </Box>
            <LinearProgress sx={{mt: 2}}/>
        </>
    );
}