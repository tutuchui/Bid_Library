DROP TABLE IF EXISTS Content;

CREATE TABLE Content(
	Title VARCHAR(50) NOT NULL,
	IsExternal Boolean NOT NULL,
	Author VARCHAR(50) BINARY NOT NULL,
	ExpiredDate DATE NOT NULL,
	UploadDate DATE NOT NULL,
	Version INT NOT NULL,
	Customer VARCHAR(50) NOT NULL,
	Flag VARCHAR(50) NOT NULL,
	CONSTRAINT so_pk
	PRIMARY KEY (Title,Version),
	CONSTRAINT so_fk
	FOREIGN KEY (Author)
	REFERENCES Users(Username)
	ON UPDATE CASCADE
	ON DELETE CASCADE
)ENGINE = InnoDB;