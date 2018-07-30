CREATE TABLE tBlobs (
    cParent        VARCHAR(255) NOT NULL,
    cName          VARCHAR(127) NOT NULL,
    cSize          bigint    NOT NULL,
    cLastModified  bigint    NOT NULL,
    cMarkGarbage   bigint,
    cBlob          BYTEA,
  CONSTRAINT "PK_name_parent" PRIMARY KEY(cParent, cName)
);
