CREATE TABLE tBlobs (
    cParent       VARCHAR(255) NOT NULL,
    cName         VARCHAR(127) NOT NULL,
    cSize         BIGINT NOT NULL,
    cLastModified BIGINT NOT NULL,
    cMarkGarbage  BIGINT,
    cBlob         BLOB,
  CONSTRAINT PK_Blobs_name_parent PRIMARY KEY(cParent, cName)
);

CREATE TABLE tBlobs2 (
    cParent       VARCHAR(255) NOT NULL,
    cName         VARCHAR(127) NOT NULL,
    cSize         BIGINT NOT NULL,
    cLastModified BIGINT NOT NULL,
    cMarkGarbage  BIGINT,
    cBlob         BLOB,
  CONSTRAINT PK_Blobs2_name_parent PRIMARY KEY(cParent, cName)
);
