CREATE TABLE tBlobs (
    cName VARCHAR(30) NOT NULL,
    cParent VARCHAR(255),
    cSize BIGINT NOT NULL,
    cLastModified BIGINT NOT NULL,
    cMarkGarbage BIGINT,
    cUserMark1 BIGINT,
    cUserMark2 BIGINT,
    cBlob BLOB,
CONSTRAINT PK_name_parent PRIMARY KEY(cName, cParent)
);
