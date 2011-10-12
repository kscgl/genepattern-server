# create tables for mapping GP jobs to SGE job jobs

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

CREATE INDEX IDX_SGE_JOB_ID on JOB_SGE (SGE_JOB_ID);

COMMIT;
