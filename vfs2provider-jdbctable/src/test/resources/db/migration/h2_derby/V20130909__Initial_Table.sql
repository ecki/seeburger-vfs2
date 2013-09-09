CREATE TABLE tBlobs (
    cParent VARCHAR(255) NOT NULL,
    cName VARCHAR(127) NOT NULL,
    cSize BIGINT NOT NULL,
    cLastModified BIGINT NOT NULL,
    cMarkGarbage BIGINT,
    cUserMark1 BIGINT,
    cUserMark2 BIGINT,
    cBlob BLOB,
CONSTRAINT PK_name_parent PRIMARY KEY(cParent, cName)
);
