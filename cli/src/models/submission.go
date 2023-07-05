package models

import "time"

type Submission struct {
	Id            string    `db:"id"`
	Archived      time.Time `db:"archived"`
	ApplicationId int64     `db:"application_id"`
}
