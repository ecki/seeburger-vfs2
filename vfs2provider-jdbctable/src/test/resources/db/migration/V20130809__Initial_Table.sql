CREATE TABLE tBlobs (
	cID varchar(30) PRIMARY KEY,
	cSize BIGINT NOT NULL,
	cLastModified BIGINT NOT NULL,
	cMarkGarbage BIGINT,
	cUserMark1 BIGINT,
	cUserMark2 BIGINT,
	cBlob BLOB);
