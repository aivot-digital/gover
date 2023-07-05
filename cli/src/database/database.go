package database

import (
	"fmt"
	"github.com/jmoiron/sqlx"
	_ "github.com/lib/pq"
	"log"
	"os"
)

var DB *sqlx.DB

func init() {
	connStr := fmt.Sprintf(
		"host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
		env("GOVER_DB_HOST"),
		env("GOVER_DB_PORT"),
		env("GOVER_DB_USERNAME"),
		env("GOVER_DB_PASSWORD"),
		env("GOVER_DB_DATABASE"),
	)

	var err error
	DB, err = sqlx.Connect("postgres", connStr)
	if err != nil {
		log.Fatalln(err)
	}
}

func env(env string) string {
	v, _ := os.LookupEnv(env)
	return v
}
