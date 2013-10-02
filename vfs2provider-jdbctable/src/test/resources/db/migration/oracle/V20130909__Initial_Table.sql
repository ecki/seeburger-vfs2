CREATE TABLE "tBlobs" (
    cParent        VARCHAR2(255) NOT NULL,
    cName          VARCHAR2(127) NOT NULL,
    cSize          NUMBER(19)    NOT NULL,
    cLastModified  NUMBER(19)    NOT NULL,
    cMarkGarbage   NUMBER(19),
    cBlob          BLOB,
  CONSTRAINT "PK_name_parent" PRIMARY KEY(cParent, cName)
);
