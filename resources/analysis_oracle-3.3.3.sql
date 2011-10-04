-- record of user upload files, generated by [hibernatetool] 
create table user_upload (
        id number(19,0) not null,
        extension varchar2(255 char),
        file_length number(19,0),
        kind varchar2(255 char),
        last_modified timestamp,
        name varchar2(255 char),
        num_parts number(10,0),
        num_parts_recd number(10,0),
        path varchar2(255 char),
        user_id varchar2(255 char),
        primary key (id),
        unique (user_id, path)
    );
create sequence user_upload_SEQ;
create index idx_user_upload_user_id on user_upload (user_id);

-- for GenomeSpace integration, link GP user account to GS user account
create table GS_ACCOUNT (
    -- use the File.canonicalPath as the primary key
    GP_USERID varchar2(255),
    -- owner of the file
    TOKEN varchar2 (255),
constraint gsa_pk primary key (GP_USERID),
constraint gsa_fk foreign key (GP_USERID) references GP_USER(USER_ID)
);

-- improve performance by creating indexes on the ANALYSIS_JOB table
CREATE INDEX IDX_AJ_STATUS ON ANALYSIS_JOB(STATUS_ID);
CREATE INDEX IDX_AJ_PARENT ON ANALYSIS_JOB(PARENT);

-- for SGE integration
CREATE TABLE JOB_SGE
(
    GP_JOB_NO NUMBER(10,0) NOT NULL,
    SGE_JOB_ID VARCHAR2(4000),
    SGE_SUBMIT_TIME TIMESTAMP,
    SGE_START_TIME TIMESTAMP,
    SGE_END_TIME TIMESTAMP,
    SGE_RETURN_CODE NUMBER(10,0),
    SGE_JOB_COMPLETION_STATUS VARCHAR2(4000),
    PRIMARY KEY (GP_JOB_NO)
);

CREATE INDEX IDX_JOB_SGE_GP_JOB_NO ON JOB_SGE (GP_JOB_NO);
CREATE INDEX IDX_SGE_JOB_ID on JOB_SGE (SGE_JOB_ID);

-- update schema version
INSERT INTO PROPS (KEY, VALUE) VALUES ('registeredVersion3.3.3', '3.3.3');

COMMIT;






