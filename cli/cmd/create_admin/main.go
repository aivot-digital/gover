package main

import (
	"bufio"
	"cli/src/database"
	"fmt"
	"golang.org/x/crypto/bcrypt"
	"golang.org/x/term"
	"log"
	"net/mail"
	"os"
	"syscall"
)

func main() {
	reader := bufio.NewReader(os.Stdin)

	fmt.Print("Name: ")
	name, _ := reader.ReadString('\n')

	if len(name) < 3 {
		log.Fatal("Der Name muss aus mindestens 3 Zeichen bestehen")
	}
	if len(name) > 96 {
		log.Fatal("Der Name darf maximal aus 96 Zeichen bestehen")
	}

	fmt.Print("E-Mail-Adresse: ")
	email, _ := reader.ReadString('\n')

	_, err := mail.ParseAddress(email)
	if err != nil {
		log.Fatal("Bitte geben Sie eine gültige E-Mail-Adresse ein")
	}

	var exists bool
	err = database.DB.Get(&exists, "SELECT EXISTS(SELECT 1 FROM users WHERE email = $1)", email)
	if err != nil {
		log.Fatal(err)
	}
	if exists {
		log.Fatal("Es existiert bereits ein Nutzer mit dieser E-Mail-Adresse")
	}

	fmt.Print("Passwort: ")
	password, err := term.ReadPassword(int(syscall.Stdin))
	if err != nil {
		log.Fatal(err)
	}

	if len(string(password)) < 6 {
		log.Fatal("Das Passwort muss aus mindestens 6 Zeichen bestehen")
	}

	passwordBytes, err := bcrypt.GenerateFromPassword(password, 10)
	if err != nil {
		log.Fatal(err)
	}

	_, err = database.DB.Exec(`
		INSERT INTO users 
			(name, email, password, active, admin, created, updated) 
		VALUES ($1, $2, $3, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
	`, name, email, string(passwordBytes))

	if err != nil {
		log.Fatal(err)
	}
}
