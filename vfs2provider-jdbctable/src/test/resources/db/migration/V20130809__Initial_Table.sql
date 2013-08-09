CREATE TABLE tBlobs (
	cID varchar(30) PRIMARY KEY,
	cSize BIGINT,
	cLastModifies BIGINT,
	cGeneration BIGINT,
	cBlob BLOB);