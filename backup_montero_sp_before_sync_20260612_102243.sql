/* [dbo].[dt_addtosourcecontrol] */
create proc dbo.dt_addtosourcecontrol
    @vchSourceSafeINI varchar(255) = '',
    @vchProjectName   varchar(255) ='',
    @vchComment       varchar(255) ='',
    @vchLoginName     varchar(255) ='',
    @vchPassword      varchar(255) =''

as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId = 0

declare @iStreamObjectId int
select @iStreamObjectId = 0

declare @VSSGUID varchar(100)
select @VSSGUID = 'SQLVersionControl.VCS_SQL'

declare @vchDatabaseName varchar(255)
select @vchDatabaseName = db_name()

declare @iReturnValue int
select @iReturnValue = 0

declare @iPropertyObjectId int
declare @vchParentId varchar(255)

declare @iObjectCount int
select @iObjectCount = 0

    exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT
    if @iReturn <> 0 GOTO E_OAError


    /* Create Project in SS */
    exec @iReturn = sp_OAMethod @iObjectId,
                                'AddProjectToSourceSafe',
                                NULL,
                                @vchSourceSafeINI,
                                @vchProjectName output,
                                @@SERVERNAME,
                                @vchDatabaseName,
                                @vchLoginName,
                                @vchPassword,
                                @vchComment


    if @iReturn <> 0 GOTO E_OAError

    exec @iReturn = sp_OAGetProperty @iObjectId, 'GetStreamObject', @iStreamObjectId OUT

    if @iReturn <> 0 GOTO E_OAError

    /* Set Database Properties */

    begin tran SetProperties

    /* add high level object */

    exec @iPropertyObjectId = dbo.dt_adduserobject_vcs 'VCSProjectID'

    select @vchParentId = CONVERT(varchar(255),@iPropertyObjectId)

    exec dbo.dt_setpropertybyid @iPropertyObjectId, 'VCSProjectID', @vchParentId , NULL
    exec dbo.dt_setpropertybyid @iPropertyObjectId, 'VCSProject' , @vchProjectName , NULL
    exec dbo.dt_setpropertybyid @iPropertyObjectId, 'VCSSourceSafeINI' , @vchSourceSafeINI , NULL
    exec dbo.dt_setpropertybyid @iPropertyObjectId, 'VCSSQLServer', @@SERVERNAME, NULL
    exec dbo.dt_setpropertybyid @iPropertyObjectId, 'VCSSQLDatabase', @vchDatabaseName, NULL

    if @@error <> 0 GOTO E_General_Error

    commit tran SetProperties

    declare cursorProcNames cursor for
        select convert(varchar(255), name) from sysobjects where type = 'P' and name not like 'dt_%'
    open cursorProcNames

    while 1 = 1
    begin
        declare @vchProcName varchar(255)
        fetch next from cursorProcNames into @vchProcName
        if @@fetch_status <> 0
            break

        select colid, text into #ProcLines
        from syscomments
        where id = object_id(@vchProcName)
        order by colid

        declare @iCurProcLine int
        declare @iProcLines int
        select @iCurProcLine = 1
        select @iProcLines = (select count(*) from #ProcLines)
        while @iCurProcLine <= @iProcLines
        begin
            declare @pos int
            select @pos = 1
            declare @iCurLineSize int
            select @iCurLineSize = len((select text from #ProcLines where colid = @iCurProcLine))
            while @pos <= @iCurLineSize
            begin
                declare @vchProcLinePiece varchar(255)
                select @vchProcLinePiece = convert(varchar(255),
                    substring((select text from #ProcLines where colid = @iCurProcLine),
                              @pos, 255 ))
                exec @iReturn = sp_OAMethod @iStreamObjectId, 'AddStream', @iReturnValue OUT, @vchProcLinePiece
                if @iReturn <> 0 GOTO E_OAError
                select @pos = @pos + 255
            end
            select @iCurProcLine = @iCurProcLine + 1
        end
        drop table #ProcLines

        exec @iReturn = sp_OAMethod @iObjectId,
                                    'CheckIn_StoredProcedure',
                                    NULL,
                                    @sProjectName = @vchProjectName,
                                    @sSourceSafeINI = @vchSourceSafeINI,
                                    @sServerName = @@SERVERNAME,
                                    @sDatabaseName = @vchDatabaseName,
                                    @sObjectName = @vchProcName,
                                    @sComment = @vchComment,
                                    @sLoginName = @vchLoginName,
                                    @sPassword = @vchPassword,
                                    @iVCSFlags = 0,
                                    @iActionFlag = 0,
                                    @sStream = ''

        if @iReturn = 0 select @iObjectCount = @iObjectCount + 1

    end

CleanUp:
	close cursorProcNames
	deallocate cursorProcNames
    select @vchProjectName
    select @iObjectCount
    return

E_General_Error:
    /* this is an all or nothing.  No specific error messages */
    goto CleanUp

E_OAError:
    exec dbo.dt_displayoaerror @iObjectId, @iReturn
    goto CleanUp



GO

/* [dbo].[dt_addtosourcecontrol_u] */
create proc dbo.dt_addtosourcecontrol_u
    @vchSourceSafeINI nvarchar(255) = '',
    @vchProjectName   nvarchar(255) ='',
    @vchComment       nvarchar(255) ='',
    @vchLoginName     nvarchar(255) ='',
    @vchPassword      nvarchar(255) =''

as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId = 0

declare @iStreamObjectId int
select @iStreamObjectId = 0

declare @VSSGUID nvarchar(100)
select @VSSGUID = N'SQLVersionControl.VCS_SQL'

declare @vchDatabaseName varchar(255)
select @vchDatabaseName = db_name()

declare @iReturnValue int
select @iReturnValue = 0

declare @iPropertyObjectId int
declare @vchParentId nvarchar(255)

declare @iObjectCount int
select @iObjectCount = 0

    exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT
    if @iReturn <> 0 GOTO E_OAError


    /* Create Project in SS */
    exec @iReturn = sp_OAMethod @iObjectId,
                                'AddProjectToSourceSafe',
                                NULL,
                                @vchSourceSafeINI,
                                @vchProjectName output,
                                @@SERVERNAME,
                                @vchDatabaseName,
                                @vchLoginName,
                                @vchPassword,
                                @vchComment


    if @iReturn <> 0 GOTO E_OAError

    exec @iReturn = sp_OAGetProperty @iObjectId, N'GetStreamObject', @iStreamObjectId OUT

    if @iReturn <> 0 GOTO E_OAError

    /* Set Database Properties */

    begin tran SetProperties

    /* add high level object */

    exec @iPropertyObjectId = dbo.dt_adduserobject_vcs 'VCSProjectID'

    select @vchParentId = CONVERT(nvarchar(255),@iPropertyObjectId)

    exec dbo.dt_setpropertybyid_u @iPropertyObjectId, 'VCSProjectID', @vchParentId , NULL
    exec dbo.dt_setpropertybyid_u @iPropertyObjectId, 'VCSProject' , @vchProjectName , NULL
    exec dbo.dt_setpropertybyid_u @iPropertyObjectId, 'VCSSourceSafeINI' , @vchSourceSafeINI , NULL
    exec dbo.dt_setpropertybyid_u @iPropertyObjectId, 'VCSSQLServer', @@SERVERNAME, NULL
    exec dbo.dt_setpropertybyid_u @iPropertyObjectId, 'VCSSQLDatabase', @vchDatabaseName, NULL

    if @@error <> 0 GOTO E_General_Error

    commit tran SetProperties

    declare cursorProcNames cursor for
        select convert(nvarchar(255), name) from sysobjects where type = N'P' and name not like N'dt_%'
    open cursorProcNames

    while 1 = 1
    begin
        declare @vchProcName nvarchar(255)
        fetch next from cursorProcNames into @vchProcName
        if @@fetch_status <> 0
            break

        select colid, text into #ProcLines
        from syscomments
        where id = object_id(@vchProcName)
        order by colid

        declare @iCurProcLine int
        declare @iProcLines int
        select @iCurProcLine = 1
        select @iProcLines = (select count(*) from #ProcLines)
        while @iCurProcLine <= @iProcLines
        begin
            declare @pos int
            select @pos = 1
            declare @iCurLineSize int
            select @iCurLineSize = len((select text from #ProcLines where colid = @iCurProcLine))
            while @pos <= @iCurLineSize
            begin
                declare @vchProcLinePiece nvarchar(255)
                select @vchProcLinePiece = convert(nvarchar(255),
                    substring((select text from #ProcLines where colid = @iCurProcLine),
                              @pos, 255 ))
                exec @iReturn = sp_OAMethod @iStreamObjectId, N'AddStream', @iReturnValue OUT, @vchProcLinePiece
                if @iReturn <> 0 GOTO E_OAError
                select @pos = @pos + 255
            end
            select @iCurProcLine = @iCurProcLine + 1
        end
        drop table #ProcLines

        exec @iReturn = sp_OAMethod @iObjectId,
                                    'CheckIn_StoredProcedure',
                                    NULL,
                                    @sProjectName = @vchProjectName,
                                    @sSourceSafeINI = @vchSourceSafeINI,
                                    @sServerName = @@SERVERNAME,
                                    @sDatabaseName = @vchDatabaseName,
                                    @sObjectName = @vchProcName,
                                    @sComment = @vchComment,
                                    @sLoginName = @vchLoginName,
                                    @sPassword = @vchPassword,
                                    @iVCSFlags = 0,
                                    @iActionFlag = 0,
                                    @sStream = ''

        if @iReturn = 0 select @iObjectCount = @iObjectCount + 1

    end

CleanUp:
	close cursorProcNames
	deallocate cursorProcNames
    select @vchProjectName
    select @iObjectCount
    return

E_General_Error:
    /* this is an all or nothing.  No specific error messages */
    goto CleanUp

E_OAError:
    exec dbo.dt_displayoaerror_u @iObjectId, @iReturn
    goto CleanUp



GO

/* [dbo].[dt_adduserobject] */
/*
**	Add an object to the dtproperties table
*/
create procedure dbo.dt_adduserobject
as
	set nocount on
	/*
	** Create the user object if it does not exist already
	*/
	begin transaction
		insert dbo.dtproperties (property) VALUES ('DtgSchemaOBJECT')
		update dbo.dtproperties set objectid=@@identity 
			where id=@@identity and property='DtgSchemaOBJECT'
	commit
	return @@identity

GO

/* [dbo].[dt_adduserobject_vcs] */
create procedure dbo.dt_adduserobject_vcs
    @vchProperty varchar(64)

as

set nocount on

declare @iReturn int
    /*
    ** Create the user object if it does not exist already
    */
    begin transaction
        select @iReturn = objectid from dbo.dtproperties where property = @vchProperty
        if @iReturn IS NULL
        begin
            insert dbo.dtproperties (property) VALUES (@vchProperty)
            update dbo.dtproperties set objectid=@@identity
                    where id=@@identity and property=@vchProperty
            select @iReturn = @@identity
        end
    commit
    return @iReturn



GO

/* [dbo].[dt_checkinobject] */
create proc dbo.dt_checkinobject
    @chObjectType  char(4),
    @vchObjectName varchar(255),
    @vchComment    varchar(255)='',
    @vchLoginName  varchar(255),
    @vchPassword   varchar(255)='',
    @iVCSFlags     int = 0,
    @iActionFlag   int = 0,   /* 0 => AddFile, 1 => CheckIn */
    @txStream1     Text = '', /* There is a bug that if items are NULL they do not pass to OLE servers */
    @txStream2     Text = '',
    @txStream3     Text = ''


as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId = 0

declare @VSSGUID varchar(100)
select @VSSGUID = 'SQLVersionControl.VCS_SQL'


declare @iPropertyObjectId int
select @iPropertyObjectId  = 0

    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    declare @vchProjectName   varchar(255)
    declare @vchSourceSafeINI varchar(255)
    declare @vchServerName    varchar(255)
    declare @vchDatabaseName  varchar(255)
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSProject',       @vchProjectName   OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSourceSafeINI', @vchSourceSafeINI OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSQLServer',     @vchServerName    OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSQLDatabase',   @vchDatabaseName  OUT

    if @chObjectType = 'PROC'
    begin
        if @iActionFlag = 1
        begin
            /* Procedure Can have up to three streams
            Drop Stream, Create Stream, GRANT stream */

            begin tran compile_all

            /* try to compile the streams */
            exec (@txStream1)
            if @@error <> 0 GOTO E_Compile_Fail

            exec (@txStream2)
            if @@error <> 0 GOTO E_Compile_Fail

            exec (@txStream3)
            if @@error <> 0 GOTO E_Compile_Fail
        end

        exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT
        if @iReturn <> 0 GOTO E_OAError

        if @iActionFlag = 1
        begin
            exec @iReturn = sp_OAMethod @iObjectId,
                                        'CheckIn_StoredProcedure',
                                        NULL,
                                        @sProjectName = @vchProjectName,
                                        @sSourceSafeINI = @vchSourceSafeINI,
                                        @sServerName = @vchServerName,
                                        @sDatabaseName = @vchDatabaseName,
                                        @sObjectName = @vchObjectName,
                                        @sComment = @vchComment,
                                        @sLoginName = @vchLoginName,
                                        @sPassword = @vchPassword,
                                        @iVCSFlags = @iVCSFlags,
                                        @iActionFlag = @iActionFlag,
                                        @sStream = @txStream2
        end
        else
        begin
            declare @iStreamObjectId int
            declare @iReturnValue int

            exec @iReturn = sp_OAGetProperty @iObjectId, 'GetStreamObject', @iStreamObjectId OUT
            if @iReturn <> 0 GOTO E_OAError

            select colid, text into #ProcLines
            from syscomments
            where id = object_id(@vchObjectName)
            order by colid

            declare @iCurProcLine int
            declare @iProcLines int
            select @iCurProcLine = 1
            select @iProcLines = (select count(*) from #ProcLines)
            while @iCurProcLine <= @iProcLines
            begin
                declare @pos int
                select @pos = 1
                declare @iCurLineSize int
                select @iCurLineSize = len((select text from #ProcLines where colid = @iCurProcLine))
                while @pos <= @iCurLineSize
                begin
                    declare @vchProcLinePiece varchar(255)
                    select @vchProcLinePiece = convert(varchar(255),
                        substring((select text from #ProcLines where colid = @iCurProcLine),
                                  @pos, 255 ))
                    exec @iReturn = sp_OAMethod @iStreamObjectId, 'AddStream', @iReturnValue OUT, @vchProcLinePiece
                    if @iReturn <> 0 GOTO E_OAError
                    select @pos = @pos + 255
                end
                select @iCurProcLine = @iCurProcLine + 1
            end
            drop table #ProcLines

            exec @iReturn = sp_OAMethod @iObjectId,
                                        'CheckIn_StoredProcedure',
                                        NULL,
                                        @sProjectName = @vchProjectName,
                                        @sSourceSafeINI = @vchSourceSafeINI,
                                        @sServerName = @vchServerName,
                                        @sDatabaseName = @vchDatabaseName,
                                        @sObjectName = @vchObjectName,
                                        @sComment = @vchComment,
                                        @sLoginName = @vchLoginName,
                                        @sPassword = @vchPassword,
                                        @iVCSFlags = @iVCSFlags,
                                        @iActionFlag = @iActionFlag,
                                        @sStream = ''
        end

        if @iReturn <> 0 GOTO E_OAError

        if @iActionFlag = 1
        begin
            commit tran compile_all
            if @@error <> 0 GOTO E_Compile_Fail
        end

    end

CleanUp:
    return

E_Compile_Fail:
    declare @lerror int
    select @lerror = @@error
    rollback tran compile_all
    RAISERROR (@lerror,16,-1)
    goto CleanUp

E_OAError:
    if @iActionFlag = 1 rollback tran compile_all
    exec dbo.dt_displayoaerror @iObjectId, @iReturn
    goto CleanUp



GO

/* [dbo].[dt_checkinobject_u] */
create proc dbo.dt_checkinobject_u
    @chObjectType  char(4),
    @vchObjectName nvarchar(255),
    @vchComment    nvarchar(255)='',
    @vchLoginName  nvarchar(255),
    @vchPassword   nvarchar(255)='',
    @iVCSFlags     int = 0,
    @iActionFlag   int = 0,   /* 0 => AddFile, 1 => CheckIn */
    @txStream1     Text = '', /* There is a bug that if items are NULL they do not pass to OLE servers */
    @txStream2     Text = '',
    @txStream3     Text = ''


as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId = 0

declare @VSSGUID nvarchar(100)
select @VSSGUID = N'SQLVersionControl.VCS_SQL'


declare @iPropertyObjectId int
select @iPropertyObjectId  = 0

    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    declare @vchProjectName   nvarchar(255)
    declare @vchSourceSafeINI nvarchar(255)
    declare @vchServerName    nvarchar(255)
    declare @vchDatabaseName  nvarchar(255)
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSProject',       @vchProjectName   OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSourceSafeINI', @vchSourceSafeINI OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSQLServer',     @vchServerName    OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSQLDatabase',   @vchDatabaseName  OUT

    if @chObjectType = 'PROC'
    begin
        if @iActionFlag = 1
        begin
            /* Procedure Can have up to three streams
            Drop Stream, Create Stream, GRANT stream */

            begin tran compile_all

            /* try to compile the streams */
            exec (@txStream1)
            if @@error <> 0 GOTO E_Compile_Fail

            exec (@txStream2)
            if @@error <> 0 GOTO E_Compile_Fail

            exec (@txStream3)
            if @@error <> 0 GOTO E_Compile_Fail
        end

        exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT
        if @iReturn <> 0 GOTO E_OAError

        if @iActionFlag = 1
        begin
            exec @iReturn = sp_OAMethod @iObjectId,
                                        N'CheckIn_StoredProcedure',
                                        NULL,
                                        @sProjectName = @vchProjectName,
                                        @sSourceSafeINI = @vchSourceSafeINI,
                                        @sServerName = @vchServerName,
                                        @sDatabaseName = @vchDatabaseName,
                                        @sObjectName = @vchObjectName,
                                        @sComment = @vchComment,
                                        @sLoginName = @vchLoginName,
                                        @sPassword = @vchPassword,
                                        @iVCSFlags = @iVCSFlags,
                                        @iActionFlag = @iActionFlag,
                                        @sStream = @txStream2
        end
        else
        begin
            declare @iStreamObjectId int
            declare @iReturnValue int

            exec @iReturn = sp_OAGetProperty @iObjectId, N'GetStreamObject', @iStreamObjectId OUT
            if @iReturn <> 0 GOTO E_OAError

            select colid, text into #ProcLines
            from syscomments
            where id = object_id(@vchObjectName)
            order by colid

            declare @iCurProcLine int
            declare @iProcLines int
            select @iCurProcLine = 1
            select @iProcLines = (select count(*) from #ProcLines)
            while @iCurProcLine <= @iProcLines
            begin
                declare @pos int
                select @pos = 1
                declare @iCurLineSize int
                select @iCurLineSize = len((select text from #ProcLines where colid = @iCurProcLine))
                while @pos <= @iCurLineSize
                begin
                    declare @vchProcLinePiece nvarchar(255)
                    select @vchProcLinePiece = convert(nvarchar(255),
                        substring((select text from #ProcLines where colid = @iCurProcLine),
                                  @pos, 255 ))
                    exec @iReturn = sp_OAMethod @iStreamObjectId, N'AddStream', @iReturnValue OUT, @vchProcLinePiece
                    if @iReturn <> 0 GOTO E_OAError
                    select @pos = @pos + 255
                end
                select @iCurProcLine = @iCurProcLine + 1
            end
            drop table #ProcLines

            exec @iReturn = sp_OAMethod @iObjectId,
                                        N'CheckIn_StoredProcedure',
                                        NULL,
                                        @sProjectName = @vchProjectName,
                                        @sSourceSafeINI = @vchSourceSafeINI,
                                        @sServerName = @vchServerName,
                                        @sDatabaseName = @vchDatabaseName,
                                        @sObjectName = @vchObjectName,
                                        @sComment = @vchComment,
                                        @sLoginName = @vchLoginName,
                                        @sPassword = @vchPassword,
                                        @iVCSFlags = @iVCSFlags,
                                        @iActionFlag = @iActionFlag,
                                        @sStream = ''
        end

        if @iReturn <> 0 GOTO E_OAError

        if @iActionFlag = 1
        begin
            commit tran compile_all
            if @@error <> 0 GOTO E_Compile_Fail
        end

    end

CleanUp:
    return

E_Compile_Fail:
    declare @lerror int
    select @lerror = @@error
    rollback tran compile_all
    RAISERROR (@lerror,16,-1)
    goto CleanUp

E_OAError:
    if @iActionFlag = 1 rollback tran compile_all
    exec dbo.dt_displayoaerror_u @iObjectId, @iReturn
    goto CleanUp



GO

/* [dbo].[dt_checkoutobject] */
create proc dbo.dt_checkoutobject
    @chObjectType  char(4),
    @vchObjectName varchar(255),
    @vchComment    varchar(255),
    @vchLoginName  varchar(255),
    @vchPassword   varchar(255),
    @iVCSFlags     int = 0,
    @iActionFlag   int = 0/* 0 => Checkout, 1 => GetLatest, 2 => UndoCheckOut */

as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId =0

declare @VSSGUID varchar(100)
select @VSSGUID = 'SQLVersionControl.VCS_SQL'

declare @iReturnValue int
select @iReturnValue = 0

declare @vchTempText varchar(255)

/* this is for our strings */
declare @iStreamObjectId int
select @iStreamObjectId = 0

    declare @iPropertyObjectId int
    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    declare @vchProjectName   varchar(255)
    declare @vchSourceSafeINI varchar(255)
    declare @vchServerName    varchar(255)
    declare @vchDatabaseName  varchar(255)
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSProject',       @vchProjectName   OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSourceSafeINI', @vchSourceSafeINI OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSQLServer',     @vchServerName    OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSQLDatabase',   @vchDatabaseName  OUT

    if @chObjectType = 'PROC'
    begin
        /* Procedure Can have up to three streams
           Drop Stream, Create Stream, GRANT stream */

        exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT

        if @iReturn <> 0 GOTO E_OAError

        exec @iReturn = sp_OAMethod @iObjectId,
                                    'CheckOut_StoredProcedure',
                                    NULL,
                                    @sProjectName = @vchProjectName,
                                    @sSourceSafeINI = @vchSourceSafeINI,
                                    @sObjectName = @vchObjectName,
                                    @sServerName = @vchServerName,
                                    @sDatabaseName = @vchDatabaseName,
                                    @sComment = @vchComment,
                                    @sLoginName = @vchLoginName,
                                    @sPassword = @vchPassword,
                                    @iVCSFlags = @iVCSFlags,
                                    @iActionFlag = @iActionFlag

        if @iReturn <> 0 GOTO E_OAError


        exec @iReturn = sp_OAGetProperty @iObjectId, 'GetStreamObject', @iStreamObjectId OUT

        if @iReturn <> 0 GOTO E_OAError

        create table #commenttext (id int identity, sourcecode varchar(255))


        select @vchTempText = 'STUB'
        while @vchTempText IS NOT NULL
        begin
            exec @iReturn = sp_OAMethod @iStreamObjectId, 'GetStream', @iReturnValue OUT, @vchTempText OUT
            if @iReturn <> 0 GOTO E_OAError

            if (@vchTempText IS NOT NULL) insert into #commenttext (sourcecode) select @vchTempText
        end

        select 'VCS'=sourcecode from #commenttext order by id
        select 'SQL'=text from syscomments where id = object_id(@vchObjectName) order by colid

    end

CleanUp:
    return

E_OAError:
    exec dbo.dt_displayoaerror @iObjectId, @iReturn
    GOTO CleanUp



GO

/* [dbo].[dt_checkoutobject_u] */
create proc dbo.dt_checkoutobject_u
    @chObjectType  char(4),
    @vchObjectName nvarchar(255),
    @vchComment    nvarchar(255),
    @vchLoginName  nvarchar(255),
    @vchPassword   nvarchar(255),
    @iVCSFlags     int = 0,
    @iActionFlag   int = 0/* 0 => Checkout, 1 => GetLatest, 2 => UndoCheckOut */

as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId =0

declare @VSSGUID nvarchar(100)
select @VSSGUID = N'SQLVersionControl.VCS_SQL'

declare @iReturnValue int
select @iReturnValue = 0

declare @vchTempText nvarchar(255)

/* this is for our strings */
declare @iStreamObjectId int
select @iStreamObjectId = 0

    declare @iPropertyObjectId int
    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    declare @vchProjectName   nvarchar(255)
    declare @vchSourceSafeINI nvarchar(255)
    declare @vchServerName    nvarchar(255)
    declare @vchDatabaseName  nvarchar(255)
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSProject',       @vchProjectName   OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSourceSafeINI', @vchSourceSafeINI OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSQLServer',     @vchServerName    OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSQLDatabase',   @vchDatabaseName  OUT

    if @chObjectType = 'PROC'
    begin
        /* Procedure Can have up to three streams
           Drop Stream, Create Stream, GRANT stream */

        exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT

        if @iReturn <> 0 GOTO E_OAError

        exec @iReturn = sp_OAMethod @iObjectId,
                                    N'CheckOut_StoredProcedure',
                                    NULL,
                                    @sProjectName = @vchProjectName,
                                    @sSourceSafeINI = @vchSourceSafeINI,
                                    @sObjectName = @vchObjectName,
                                    @sServerName = @vchServerName,
                                    @sDatabaseName = @vchDatabaseName,
                                    @sComment = @vchComment,
                                    @sLoginName = @vchLoginName,
                                    @sPassword = @vchPassword,
                                    @iVCSFlags = @iVCSFlags,
                                    @iActionFlag = @iActionFlag

        if @iReturn <> 0 GOTO E_OAError


        exec @iReturn = sp_OAGetProperty @iObjectId, N'GetStreamObject', @iStreamObjectId OUT

        if @iReturn <> 0 GOTO E_OAError

        create table #commenttext (id int identity, sourcecode nvarchar(255))


        select @vchTempText = N'STUB'
        while @vchTempText IS NOT NULL
        begin
            exec @iReturn = sp_OAMethod @iStreamObjectId, N'GetStream', @iReturnValue OUT, @vchTempText OUT
            if @iReturn <> 0 GOTO E_OAError

            if (@vchTempText IS NOT NULL) insert into #commenttext (sourcecode) select @vchTempText
        end

        select N'VCS'=sourcecode from #commenttext order by id
        select N'SQL'=text from syscomments where id = object_id(@vchObjectName) order by colid

    end

CleanUp:
    return

E_OAError:
    exec dbo.dt_displayoaerror_u @iObjectId, @iReturn
    GOTO CleanUp



GO

/* [dbo].[dt_displayoaerror] */
CREATE PROCEDURE dbo.dt_displayoaerror
    @iObject int,
    @iresult int
as

set nocount on

declare @vchOutput      varchar(255)
declare @hr             int
declare @vchSource      varchar(255)
declare @vchDescription varchar(255)

    exec @hr = sp_OAGetErrorInfo @iObject, @vchSource OUT, @vchDescription OUT

    select @vchOutput = @vchSource + ': ' + @vchDescription
    raiserror (@vchOutput,16,-1)

    return


GO

/* [dbo].[dt_displayoaerror_u] */
CREATE PROCEDURE dbo.dt_displayoaerror_u
    @iObject int,
    @iresult int
as

set nocount on

declare @vchOutput      nvarchar(255)
declare @hr             int
declare @vchSource      nvarchar(255)
declare @vchDescription nvarchar(255)

    exec @hr = sp_OAGetErrorInfo @iObject, @vchSource OUT, @vchDescription OUT

    select @vchOutput = @vchSource + ': ' + @vchDescription
    raiserror (@vchOutput,16,-1)

    return


GO

/* [dbo].[dt_droppropertiesbyid] */
/*
**	Drop one or all the associated properties of an object or an attribute 
**
**	dt_dropproperties objid, null or '' -- drop all properties of the object itself
**	dt_dropproperties objid, property -- drop the property
*/
create procedure dbo.dt_droppropertiesbyid
	@id int,
	@property varchar(64)
as
	set nocount on

	if (@property is null) or (@property = '')
		delete from dbo.dtproperties where objectid=@id
	else
		delete from dbo.dtproperties 
			where objectid=@id and property=@property


GO

/* [dbo].[dt_dropuserobjectbyid] */
/*
**	Drop an object from the dbo.dtproperties table
*/
create procedure dbo.dt_dropuserobjectbyid
	@id int
as
	set nocount on
	delete from dbo.dtproperties where objectid=@id

GO

/* [dbo].[dt_generateansiname] */
/* 
**	Generate an ansi name that is unique in the dtproperties.value column 
*/ 
create procedure dbo.dt_generateansiname(@name varchar(255) output) 
as 
	declare @prologue varchar(20) 
	declare @indexstring varchar(20) 
	declare @index integer 
 
	set @prologue = 'MSDT-A-' 
	set @index = 1 
 
	while 1 = 1 
	begin 
		set @indexstring = cast(@index as varchar(20)) 
		set @name = @prologue + @indexstring 
		if not exists (select value from dtproperties where value = @name) 
			break 
		 
		set @index = @index + 1 
 
		if (@index = 10000) 
			goto TooMany 
	end 
 
Leave: 
 
	return 
 
TooMany: 
 
	set @name = 'DIAGRAM' 
	goto Leave 

GO

/* [dbo].[dt_getobjwithprop] */
/*
**	Retrieve the owner object(s) of a given property
*/
create procedure dbo.dt_getobjwithprop
	@property varchar(30),
	@value varchar(255)
as
	set nocount on

	if (@property is null) or (@property = '')
	begin
		raiserror('Must specify a property name.',-1,-1)
		return (1)
	end

	if (@value is null)
		select objectid id from dbo.dtproperties
			where property=@property

	else
		select objectid id from dbo.dtproperties
			where property=@property and value=@value

GO

/* [dbo].[dt_getobjwithprop_u] */
/*
**	Retrieve the owner object(s) of a given property
*/
create procedure dbo.dt_getobjwithprop_u
	@property varchar(30),
	@uvalue nvarchar(255)
as
	set nocount on

	if (@property is null) or (@property = '')
	begin
		raiserror('Must specify a property name.',-1,-1)
		return (1)
	end

	if (@uvalue is null)
		select objectid id from dbo.dtproperties
			where property=@property

	else
		select objectid id from dbo.dtproperties
			where property=@property and uvalue=@uvalue

GO

/* [dbo].[dt_getpropertiesbyid] */
/*
**	Retrieve properties by id's
**
**	dt_getproperties objid, null or '' -- retrieve all properties of the object itself
**	dt_getproperties objid, property -- retrieve the property specified
*/
create procedure dbo.dt_getpropertiesbyid
	@id int,
	@property varchar(64)
as
	set nocount on

	if (@property is null) or (@property = '')
		select property, version, value, lvalue
			from dbo.dtproperties
			where  @id=objectid
	else
		select property, version, value, lvalue
			from dbo.dtproperties
			where  @id=objectid and @property=property

GO

/* [dbo].[dt_getpropertiesbyid_u] */
/*
**	Retrieve properties by id's
**
**	dt_getproperties objid, null or '' -- retrieve all properties of the object itself
**	dt_getproperties objid, property -- retrieve the property specified
*/
create procedure dbo.dt_getpropertiesbyid_u
	@id int,
	@property varchar(64)
as
	set nocount on

	if (@property is null) or (@property = '')
		select property, version, uvalue, lvalue
			from dbo.dtproperties
			where  @id=objectid
	else
		select property, version, uvalue, lvalue
			from dbo.dtproperties
			where  @id=objectid and @property=property

GO

/* [dbo].[dt_getpropertiesbyid_vcs] */
create procedure dbo.dt_getpropertiesbyid_vcs
    @id       int,
    @property varchar(64),
    @value    varchar(255) = NULL OUT

as

    set nocount on

    select @value = (
        select value
                from dbo.dtproperties
                where @id=objectid and @property=property
                )


GO

/* [dbo].[dt_getpropertiesbyid_vcs_u] */
create procedure dbo.dt_getpropertiesbyid_vcs_u
    @id       int,
    @property varchar(64),
    @value    nvarchar(255) = NULL OUT

as

    set nocount on

    select @value = (
        select uvalue
                from dbo.dtproperties
                where @id=objectid and @property=property
                )


GO

/* [dbo].[dt_isundersourcecontrol] */
create proc dbo.dt_isundersourcecontrol
    @vchLoginName varchar(255) = '',
    @vchPassword  varchar(255) = '',
    @iWhoToo      int = 0 /* 0 => Just check project; 1 => get list of objs */

as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId = 0

declare @VSSGUID varchar(100)
select @VSSGUID = 'SQLVersionControl.VCS_SQL'

declare @iReturnValue int
select @iReturnValue = 0

declare @iStreamObjectId int
select @iStreamObjectId   = 0

declare @vchTempText varchar(255)

    declare @iPropertyObjectId int
    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    declare @vchProjectName   varchar(255)
    declare @vchSourceSafeINI varchar(255)
    declare @vchServerName    varchar(255)
    declare @vchDatabaseName  varchar(255)
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSProject',       @vchProjectName   OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSourceSafeINI', @vchSourceSafeINI OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSQLServer',     @vchServerName    OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSQLDatabase',   @vchDatabaseName  OUT

    if (@vchProjectName IS NULL) or (@vchSourceSafeINI  IS NULL) or (@vchServerName IS NULL) or (@vchDatabaseName IS NULL)
    begin
        RAISERROR('Not Under Source Control',16,-1)
        return
    end

    if @iWhoToo = 1
    begin

        /* Get List of Procs in the project */
        exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT
        if @iReturn <> 0 GOTO E_OAError

        exec @iReturn = sp_OAMethod @iObjectId,
                                    'GetListOfObjects',
                                    NULL,
                                    @vchProjectName,
                                    @vchSourceSafeINI,
                                    @vchServerName,
                                    @vchDatabaseName,
                                    @vchLoginName,
                                    @vchPassword

        if @iReturn <> 0 GOTO E_OAError

        exec @iReturn = sp_OAGetProperty @iObjectId, 'GetStreamObject', @iStreamObjectId OUT

        if @iReturn <> 0 GOTO E_OAError

        create table #ObjectList (id int identity, vchObjectlist varchar(255))

        select @vchTempText = 'STUB'
        while @vchTempText IS NOT NULL
        begin
            exec @iReturn = sp_OAMethod @iStreamObjectId, 'GetStream', @iReturnValue OUT, @vchTempText OUT
            if @iReturn <> 0 GOTO E_OAError

            if (@vchTempText IS NOT NULL) insert into #ObjectList (vchObjectlist ) select @vchTempText
        end

        select vchObjectlist from #ObjectList order by id
    end

CleanUp:
    return

E_OAError:
    exec dbo.dt_displayoaerror @iObjectId, @iReturn
    goto CleanUp



GO

/* [dbo].[dt_isundersourcecontrol_u] */
create proc dbo.dt_isundersourcecontrol_u
    @vchLoginName nvarchar(255) = '',
    @vchPassword  nvarchar(255) = '',
    @iWhoToo      int = 0 /* 0 => Just check project; 1 => get list of objs */

as

	set nocount on

	declare @iReturn int
	declare @iObjectId int
	select @iObjectId = 0

	declare @VSSGUID nvarchar(100)
	select @VSSGUID = N'SQLVersionControl.VCS_SQL'

	declare @iReturnValue int
	select @iReturnValue = 0

	declare @iStreamObjectId int
	select @iStreamObjectId   = 0

	declare @vchTempText nvarchar(255)

    declare @iPropertyObjectId int
    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    declare @vchProjectName   nvarchar(255)
    declare @vchSourceSafeINI nvarchar(255)
    declare @vchServerName    nvarchar(255)
    declare @vchDatabaseName  nvarchar(255)
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSProject',       @vchProjectName   OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSourceSafeINI', @vchSourceSafeINI OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSQLServer',     @vchServerName    OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSQLDatabase',   @vchDatabaseName  OUT

    if (@vchProjectName IS NULL) or (@vchSourceSafeINI  IS NULL) or (@vchServerName IS NULL) or (@vchDatabaseName IS NULL)
    begin
        RAISERROR(N'Not Under Source Control',16,-1)
        return
    end

    if @iWhoToo = 1
    begin

        /* Get List of Procs in the project */
        exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT
        if @iReturn <> 0 GOTO E_OAError

        exec @iReturn = sp_OAMethod @iObjectId,
                                    N'GetListOfObjects',
                                    NULL,
                                    @vchProjectName,
                                    @vchSourceSafeINI,
                                    @vchServerName,
                                    @vchDatabaseName,
                                    @vchLoginName,
                                    @vchPassword

        if @iReturn <> 0 GOTO E_OAError

        exec @iReturn = sp_OAGetProperty @iObjectId, N'GetStreamObject', @iStreamObjectId OUT

        if @iReturn <> 0 GOTO E_OAError

        create table #ObjectList (id int identity, vchObjectlist nvarchar(255))

        select @vchTempText = N'STUB'
        while @vchTempText IS NOT NULL
        begin
            exec @iReturn = sp_OAMethod @iStreamObjectId, N'GetStream', @iReturnValue OUT, @vchTempText OUT
            if @iReturn <> 0 GOTO E_OAError

            if (@vchTempText IS NOT NULL) insert into #ObjectList (vchObjectlist ) select @vchTempText
        end

        select vchObjectlist from #ObjectList order by id
    end

CleanUp:
    return

E_OAError:
    exec dbo.dt_displayoaerror_u @iObjectId, @iReturn
    goto CleanUp



GO

/* [dbo].[dt_removefromsourcecontrol] */
create procedure dbo.dt_removefromsourcecontrol

as

    set nocount on

    declare @iPropertyObjectId int
    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    exec dbo.dt_droppropertiesbyid @iPropertyObjectId, null

    /* -1 is returned by dt_droppopertiesbyid */
    if @@error <> 0 and @@error <> -1 return 1

    return 0



GO

/* [dbo].[dt_setpropertybyid] */
/*
**	If the property already exists, reset the value; otherwise add property
**		id -- the id in sysobjects of the object
**		property -- the name of the property
**		value -- the text value of the property
**		lvalue -- the binary value of the property (image)
*/
create procedure dbo.dt_setpropertybyid
	@id int,
	@property varchar(64),
	@value varchar(255),
	@lvalue image
as
	set nocount on
	declare @uvalue nvarchar(255) 
	set @uvalue = convert(nvarchar(255), @value) 
	if exists (select * from dbo.dtproperties 
			where objectid=@id and property=@property)
	begin
		--
		-- bump the version count for this row as we update it
		--
		update dbo.dtproperties set value=@value, uvalue=@uvalue, lvalue=@lvalue, version=version+1
			where objectid=@id and property=@property
	end
	else
	begin
		--
		-- version count is auto-set to 0 on initial insert
		--
		insert dbo.dtproperties (property, objectid, value, uvalue, lvalue)
			values (@property, @id, @value, @uvalue, @lvalue)
	end


GO

/* [dbo].[dt_setpropertybyid_u] */
/*
**	If the property already exists, reset the value; otherwise add property
**		id -- the id in sysobjects of the object
**		property -- the name of the property
**		uvalue -- the text value of the property
**		lvalue -- the binary value of the property (image)
*/
create procedure dbo.dt_setpropertybyid_u
	@id int,
	@property varchar(64),
	@uvalue nvarchar(255),
	@lvalue image
as
	set nocount on
	-- 
	-- If we are writing the name property, find the ansi equivalent. 
	-- If there is no lossless translation, generate an ansi name. 
	-- 
	declare @avalue varchar(255) 
	set @avalue = null 
	if (@uvalue is not null) 
	begin 
		if (convert(nvarchar(255), convert(varchar(255), @uvalue)) = @uvalue) 
		begin 
			set @avalue = convert(varchar(255), @uvalue) 
		end 
		else 
		begin 
			if 'DtgSchemaNAME' = @property 
			begin 
				exec dbo.dt_generateansiname @avalue output 
			end 
		end 
	end 
	if exists (select * from dbo.dtproperties 
			where objectid=@id and property=@property)
	begin
		--
		-- bump the version count for this row as we update it
		--
		update dbo.dtproperties set value=@avalue, uvalue=@uvalue, lvalue=@lvalue, version=version+1
			where objectid=@id and property=@property
	end
	else
	begin
		--
		-- version count is auto-set to 0 on initial insert
		--
		insert dbo.dtproperties (property, objectid, value, uvalue, lvalue)
			values (@property, @id, @avalue, @uvalue, @lvalue)
	end

GO

/* [dbo].[dt_validateloginparams] */
create proc dbo.dt_validateloginparams
    @vchLoginName  varchar(255),
    @vchPassword   varchar(255)
as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId =0

declare @VSSGUID varchar(100)
select @VSSGUID = 'SQLVersionControl.VCS_SQL'

    declare @iPropertyObjectId int
    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    declare @vchSourceSafeINI varchar(255)
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSourceSafeINI', @vchSourceSafeINI OUT

    exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT
    if @iReturn <> 0 GOTO E_OAError

    exec @iReturn = sp_OAMethod @iObjectId,
                                'ValidateLoginParams',
                                NULL,
                                @sSourceSafeINI = @vchSourceSafeINI,
                                @sLoginName = @vchLoginName,
                                @sPassword = @vchPassword
    if @iReturn <> 0 GOTO E_OAError

CleanUp:
    return

E_OAError:
    exec dbo.dt_displayoaerror @iObjectId, @iReturn
    GOTO CleanUp



GO

/* [dbo].[dt_validateloginparams_u] */
create proc dbo.dt_validateloginparams_u
    @vchLoginName  nvarchar(255),
    @vchPassword   nvarchar(255)
as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId =0

declare @VSSGUID nvarchar(100)
select @VSSGUID = N'SQLVersionControl.VCS_SQL'

    declare @iPropertyObjectId int
    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    declare @vchSourceSafeINI nvarchar(255)
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSourceSafeINI', @vchSourceSafeINI OUT

    exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT
    if @iReturn <> 0 GOTO E_OAError

    exec @iReturn = sp_OAMethod @iObjectId,
                                N'ValidateLoginParams',
                                NULL,
                                @sSourceSafeINI = @vchSourceSafeINI,
                                @sLoginName = @vchLoginName,
                                @sPassword = @vchPassword
    if @iReturn <> 0 GOTO E_OAError

CleanUp:
    return

E_OAError:
    exec dbo.dt_displayoaerror_u @iObjectId, @iReturn
    GOTO CleanUp



GO

/* [dbo].[dt_vcsenabled] */
create proc dbo.dt_vcsenabled

as

set nocount on

declare @iObjectId int
select @iObjectId = 0

declare @VSSGUID varchar(100)
select @VSSGUID = 'SQLVersionControl.VCS_SQL'

    declare @iReturn int
    exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT
    if @iReturn <> 0 raiserror('', 16, -1) /* Can't Load Helper DLLC */



GO

/* [dbo].[dt_verstamp006] */
/*
**	This procedure returns the version number of the stored
**    procedures used by the Microsoft Visual Database Tools.
**    Current version is 7.0.00.
*/
create procedure dbo.dt_verstamp006
as
	select 7000

GO

/* [dbo].[dt_whocheckedout] */
create proc dbo.dt_whocheckedout
        @chObjectType  char(4),
        @vchObjectName varchar(255),
        @vchLoginName  varchar(255),
        @vchPassword   varchar(255)

as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId =0

declare @VSSGUID varchar(100)
select @VSSGUID = 'SQLVersionControl.VCS_SQL'

    declare @iPropertyObjectId int

    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    declare @vchProjectName   varchar(255)
    declare @vchSourceSafeINI varchar(255)
    declare @vchServerName    varchar(255)
    declare @vchDatabaseName  varchar(255)
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSProject',       @vchProjectName   OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSourceSafeINI', @vchSourceSafeINI OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSQLServer',     @vchServerName    OUT
    exec dbo.dt_getpropertiesbyid_vcs @iPropertyObjectId, 'VCSSQLDatabase',   @vchDatabaseName  OUT

    if @chObjectType = 'PROC'
    begin
        exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT

        if @iReturn <> 0 GOTO E_OAError

        declare @vchReturnValue varchar(255)
        select @vchReturnValue = ''

        exec @iReturn = sp_OAMethod @iObjectId,
                                    'WhoCheckedOut',
                                    @vchReturnValue OUT,
                                    @sProjectName = @vchProjectName,
                                    @sSourceSafeINI = @vchSourceSafeINI,
                                    @sObjectName = @vchObjectName,
                                    @sServerName = @vchServerName,
                                    @sDatabaseName = @vchDatabaseName,
                                    @sLoginName = @vchLoginName,
                                    @sPassword = @vchPassword

        if @iReturn <> 0 GOTO E_OAError

        select @vchReturnValue

    end

CleanUp:
    return

E_OAError:
    exec dbo.dt_displayoaerror @iObjectId, @iReturn
    GOTO CleanUp



GO

/* [dbo].[dt_whocheckedout_u] */
create proc dbo.dt_whocheckedout_u
        @chObjectType  char(4),
        @vchObjectName nvarchar(255),
        @vchLoginName  nvarchar(255),
        @vchPassword   nvarchar(255)

as

set nocount on

declare @iReturn int
declare @iObjectId int
select @iObjectId =0

declare @VSSGUID nvarchar(100)
select @VSSGUID = N'SQLVersionControl.VCS_SQL'

    declare @iPropertyObjectId int

    select @iPropertyObjectId = (select objectid from dbo.dtproperties where property = 'VCSProjectID')

    declare @vchProjectName   nvarchar(255)
    declare @vchSourceSafeINI nvarchar(255)
    declare @vchServerName    nvarchar(255)
    declare @vchDatabaseName  nvarchar(255)
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSProject',       @vchProjectName   OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSourceSafeINI', @vchSourceSafeINI OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSQLServer',     @vchServerName    OUT
    exec dbo.dt_getpropertiesbyid_vcs_u @iPropertyObjectId, 'VCSSQLDatabase',   @vchDatabaseName  OUT

    if @chObjectType = 'PROC'
    begin
        exec @iReturn = sp_OACreate @VSSGUID, @iObjectId OUT

        if @iReturn <> 0 GOTO E_OAError

        declare @vchReturnValue nvarchar(255)
        select @vchReturnValue = ''

        exec @iReturn = sp_OAMethod @iObjectId,
                                    N'WhoCheckedOut',
                                    @vchReturnValue OUT,
                                    @sProjectName = @vchProjectName,
                                    @sSourceSafeINI = @vchSourceSafeINI,
                                    @sObjectName = @vchObjectName,
                                    @sServerName = @vchServerName,
                                    @sDatabaseName = @vchDatabaseName,
                                    @sLoginName = @vchLoginName,
                                    @sPassword = @vchPassword

        if @iReturn <> 0 GOTO E_OAError

        select @vchReturnValue

    end

CleanUp:
    return

E_OAError:
    exec dbo.dt_displayoaerror_u @iObjectId, @iReturn
    GOTO CleanUp



GO

/* [dbo].[GenerarBackupDia] */


CREATE proc [dbo].[GenerarBackupDia]
as

--parametros
DECLARE @strBaseDatos nvarchar(50)
DECLARE @strCarpeta nvarchar(500)
DECLARE @strCarpetaOtraCopia nvarchar(500)

--nombre de la base de datos
set @strBaseDatos = N'BDSistemaAntenaPMMontero'
--directorio
set @strCarpeta = N'D:\Backup\Montero\BDSistemaAntenaPM\Dia'
set @strCarpetaOtraCopia = N'F:\Backup\Montero\BDSistemaAntenaPM\Dia'

declare @tToday datetime
set @tToday = GETDATE()

DECLARE @strNombreBackup nvarchar(100)
set @strNombreBackup = @strBaseDatos + N' '
+ str(datepart(year, @tToday),4,0)
+ replace (str(datepart(month, @tToday), 2 ,0), N' ',N'0')
+ replace (str(datepart(day, @tToday), 2 ,0), N' ',N'0')
+ replace (str(datepart(hour, @tToday), 2 ,0), N' ',N'0')
+ replace (str(datepart(minute, @tToday), 2 ,0), N' ',N'0')
--print @strNombreBackup

--creando la carpeta de la fecha
DECLARE @strNombreCarpeta nvarchar(100)
set @strNombreCarpeta = str(datepart(year, @tToday),4,0)
+ replace (str(datepart(month, @tToday), 2 ,0), N' ',N'0')
+ replace (str(datepart(day, @tToday), 2 ,0), N' ',N'0')

--crea directorio
declare @aux nvarchar(500)
declare @aux2 nvarchar(500)
set @aux = @strCarpeta + '\'+ @strNombreCarpeta
set @aux2 = @strCarpetaOtraCopia + '\'+ @strNombreCarpeta


EXEC master.dbo.xp_create_subdir @aux
EXEC master.dbo.xp_create_subdir @aux2


declare @strBackupFile nvarchar(600)
declare @strBackupFileOtraCopia nvarchar(600)
set @strBackupFile = @aux+ N'\'+ @strNombreBackup + N'.bak'
set @strBackupFileOtraCopia = @aux2+ N'\'+ @strNombreBackup + N'.bak'

--Hace Backup
BACKUP DATABASE @strBaseDatos
to disk =@strBackupFile
with
NOFORMAT,
INIT,
SKIP,
NAME = @strBackupFile

--Hace Backup en Otra Unidad
BACKUP DATABASE @strBaseDatos
to disk =@strBackupFileOtraCopia
with
NOFORMAT,
INIT,
SKIP,
NAME = @strBackupFileOtraCopia





GO

/* [dbo].[GenerarBackupTarde] */


create proc [dbo].[GenerarBackupTarde]
as

--parametros
DECLARE @strBaseDatos nvarchar(50)
DECLARE @strCarpeta nvarchar(500)
DECLARE @strCarpetaOtraCopia nvarchar(500)

--nombre de la base de datos
set @strBaseDatos = N'BDSistemaAntenaPMMontero'
--directorio
set @strCarpeta = N'D:\Backup\Montero\BDSistemaAntenaPM\Tarde'
set @strCarpetaOtraCopia = N'F:\Backup\Montero\BDSistemaAntenaPM\Tarde'

declare @tToday datetime
set @tToday = GETDATE()

DECLARE @strNombreBackup nvarchar(100)
set @strNombreBackup = @strBaseDatos + N' '
+ str(datepart(year, @tToday),4,0)
+ replace (str(datepart(month, @tToday), 2 ,0), N' ',N'0')
+ replace (str(datepart(day, @tToday), 2 ,0), N' ',N'0')
+ replace (str(datepart(hour, @tToday), 2 ,0), N' ',N'0')
+ replace (str(datepart(minute, @tToday), 2 ,0), N' ',N'0')
--print @strNombreBackup

--creando la carpeta de la fecha
DECLARE @strNombreCarpeta nvarchar(100)
set @strNombreCarpeta = str(datepart(year, @tToday),4,0)
+ replace (str(datepart(month, @tToday), 2 ,0), N' ',N'0')
+ replace (str(datepart(day, @tToday), 2 ,0), N' ',N'0')

--crea directorio
declare @aux nvarchar(500)
declare @aux2 nvarchar(500)
set @aux = @strCarpeta + '\'+ @strNombreCarpeta
set @aux2 = @strCarpetaOtraCopia + '\'+ @strNombreCarpeta


EXEC master.dbo.xp_create_subdir @aux
EXEC master.dbo.xp_create_subdir @aux2


declare @strBackupFile nvarchar(600)
declare @strBackupFileOtraCopia nvarchar(600)
set @strBackupFile = @aux+ N'\'+ @strNombreBackup + N'.bak'
set @strBackupFileOtraCopia = @aux2+ N'\'+ @strNombreBackup + N'.bak'

--Hace Backup
BACKUP DATABASE @strBaseDatos
to disk =@strBackupFile
with
NOFORMAT,
INIT,
SKIP,
NAME = @strBackupFile

--Hace Backup en Otra Unidad
BACKUP DATABASE @strBaseDatos
to disk =@strBackupFileOtraCopia
with
NOFORMAT,
INIT,
SKIP,
NAME = @strBackupFileOtraCopia





GO

/* [dbo].[ObtenerSalidaAlmacenBol] */
create proc [dbo].[ObtenerSalidaAlmacenBol] (@bol nvarchar(30), @id_ruta int)
as
SELECT     dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor , dbo.tbl_Usuario.Nombre AS Usuario, 
dbo.tbl_Ruta.Id_Ruta , dbo.tbl_Ruta.Nombre AS NombreRuta,
dbo.tbl_Vendedor.Nombre AS NombreVendedor, dbo.tbl_AlmacenVendedor.Fecha,  
dbo.tbl_Producto.Id_Producto ,dbo.tbl_Producto.Nombre AS Producto, dbo.tbl_CodigoAlmacenVendedor.Cod_Inicio,
dbo.tbl_CodigoAlmacenVendedor.Cantidad 
FROM         dbo.tbl_AlmacenVendedor INNER JOIN 
dbo.tbl_CodigoAlmacenVendedor ON  
dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor = dbo.tbl_CodigoAlmacenVendedor.Id_AlmacenVendedor INNER JOIN 
dbo.tbl_Producto ON dbo.tbl_CodigoAlmacenVendedor.Id_Producto = dbo.tbl_Producto.Id_Producto INNER JOIN 
dbo.tbl_Usuario ON dbo.tbl_AlmacenVendedor.Id_Usuario = dbo.tbl_Usuario.Id_Usuario INNER JOIN 
dbo.tbl_Ruta ON dbo.tbl_AlmacenVendedor.Id_Ruta = dbo.tbl_Ruta.Id_Ruta INNER JOIN 
dbo.tbl_Vendedor ON dbo.tbl_AlmacenVendedor.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor 
WHERE  @bol = dbo.tbl_CodigoAlmacenVendedor.Cod_Inicio 
and dbo.tbl_Ruta.Id_Ruta = @id_ruta and tbl_AlmacenVendedor.E_Eliminado=2 and tbl_CodigoAlmacenVendedor.E_Eliminado=2 
GO

/* [dbo].[prod_PuedeCerrarAlmacen] */
CREATE proc prod_PuedeCerrarAlmacen(@fecha datetime)
as
	-- validar si la ruta hizo el cuadre	
	select * from tbl_Cuadre 
	where dbo.DateOnly( Fecha ) = dbo.DateOnly( @fecha ) 
		and E_Eliminado = 0
		and Id_Ruta not in 
					( select id_ruta from tbl_PedidoVendedor 
					where dbo.DateOnly( Fecha ) = dbo.DateOnly( @fecha ) 
					and E_Eliminado = 0 )
					
	-- validar si la ruta tiene una OT registrada.
	select * from tbl_Cuadre 
	where dbo.DateOnly( Fecha ) = dbo.DateOnly( @fecha ) and E_Eliminado = 0
		and Id_Ruta not in 
					(select Id_Ruta from tbl_Venta 
					where dbo.DateOnly( Fecha_Ejecucion ) = dbo.DateOnly( @fecha ) 
					and E_Eliminado = 0 )

GO

/* [dbo].[PRU] */

--PRU 1
CREATE proc PRU (@Id_OrdenVentaRealizada int)
as
BEGIN
	IF OBJECT_ID('dbo.Temporal') IS not NULL
		drop table Temporal;

	declare @contador2 int, @cuantos2 int, @contador int,@cuantos int,@DigitosImei int;
		set @contador = 1;
		set @contador2 = 1;

	declare @strproducto nvarchar(50), @consulta nvarchar(MAX), @consultaInsercion nvarchar(MAX),@NombreProducto nvarchar(50), @NombreProductoAux nvarchar(50);
	set @consulta = 'create table Temporal (Id int identity,Obs nvarchar'
	set @consultaInsercion = 'insert into Temporal values(1'
	declare @Productos table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)
	declare @ProductosAux table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)

	insert into @ProductosAux 
		select Id_Producto, Alias , DigitosImei from tbl_Producto where E_Eliminado=0 order by Id_Producto

	-------------------creando 5 decodificadores
	set @cuantos = (select COUNT(*) from @ProductosAux);
	
	while(@contador <= @cuantos)
	BEGIN
		
		SET @NombreProducto = (Select nombre from @ProductosAux where Id = @contador);
		set @DigitosImei = (Select DigitosImei from @ProductosAux where Id = @contador);		
		if((@NombreProducto = 'DECODIFICADOR') or (@NombreProducto = 'TARJETA'))	
		BEGIN
			WHILE (@contador2<5)
			BEGIN
				SET @NombreProductoAux 	= @NombreProducto;				
				set @NombreProductoAux 	= @NombreProductoAux + CONVERT(nvarchar,@contador2);
				
				insert into @Productos 
					select Id_Producto, @NombreProductoAux, DigitosImei  from @ProductosAux where Id = @contador
					
					set @consulta = @consulta +','+ @NombreProductoAux + ' nvarchar(50)'	
					set @consultaInsercion =@consultaInsercion  + ',0'
				set @contador2 = @contador2+1;
			END
			set @contador2 = 1
		END
		ELSE
		BEGIN
				insert into @Productos 
				select Id_Producto, nombre, DigitosImei  from @ProductosAux where Id = @contador
					set @consulta = @consulta +','+ @NombreProducto + ' nvarchar(50)'
					set @consultaInsercion =@consultaInsercion  + ',0'
		END
		SET @contador = @contador + 1;	
		
	--	set @consultaInsercion =@consultaInsercion  + ','
	END
		set @consulta = @consulta +')'
		set @consultaInsercion = @consultaInsercion +')'
		exec (@consulta)
		exec (@consultaInsercion)

		--------------------------------------------------------------------------------------------------------------------
		DECLARE @ContadorDecodificador int 
		DECLARE @ContadorAntena int 
		set @ContadorDecodificador =0
		set @ContadorAntena =0
		declare @Materiales table (id int identity ,Id_Producto int, Nombre nvarchar(100),Dato nvarchar(50))
		insert into @Materiales
			select p.Id_Producto,p.Alias,Cod_Inicio from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio<>'' and Id_TipoMaterial=1 and cv.E_Eliminado=0  order by Nombre
		insert into @Materiales
			select p.Id_Producto,p.Alias, Convert(nvarchar, Cantidad) from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio=''and Id_TipoMaterial=1  and cv.E_Eliminado=0 order by Nombre
		--select * from @Materiales
		
				set @cuantos = (select COUNT(*) from @Materiales)
				set @contador = 1;
				set @contador2 = 1;
					while (@contador <=@cuantos)
					BEGIN		
						set @NombreProducto = (select nombre from @Materiales where id=@contador)
						print 'nombre producto ' +@NombreProducto	
						IF(@NombreProducto = 'DECODIFICADOR'  OR @NombreProducto = 'TARJETA' )					
						BEGIN
							IF(@NombreProducto = 'DECODIFICADOR' )
							BEGIN
								--	set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'DECODIFICADOR')																		
								--	set @contador2 = 1
											--WHILE (@contador2 <= @cuantos2)
											--BEGIN
												set @ContadorDecodificador = @ContadorDecodificador + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorDecodificador);							
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux + ' = ' 
												set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where Id_Producto = 1 and id = @contador ) + CHAR(39)
												set @consulta = @consulta + ' where id = 1' 															
												--set @contador2 = @contador2 + 1;																				
												exec (@consulta)	
												PRINT 	' 1'	+@consulta
												PRINT @contador
										
											set @contador = @contador + 1
							END
							IF(@NombreProducto = 'TARJETA')
							BEGIN
									--set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'TARJETA')
									--set @contador2 = 1
											--WHILE (@contador2 <= @cuantos2)
											--BEGIN
												set @ContadorAntena = @ContadorAntena + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorAntena);
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux +  ' = ' 
												set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where Id_Producto = 2 and id = @contador) + CHAR(39)
												set @consulta = @consulta + ' where id = 1'											
												exec (@consulta)												
												PRINT 	' 2'	+@consulta
												PRINT @contador
												set @contador = @contador + 1		
														
							END	
						END
						ELSE
						BEGIN
								set @consulta = 'update Temporal set ' 
								set @consulta = @consulta + @NombreProducto + ' = ' 
								set @consulta = @consulta + (select Dato from @Materiales where id = @contador)  
								set @consulta = @consulta + ' where id = 1';
								exec (@consulta);
								print '3' +@consulta
								SET @contador = @contador+1
						END
					END
				
					SELECT * FROM TEMPORAL
				END
							
						
							--[sp_VentasHorizontal] 2
							--IF(@NombreProducto = 'TARJETA' )
							--BEGIN
							--		set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'TARJETA')
							--		set @contador2 = 1
							--				WHILE (@contador2 <= @cuantos2)
							--				BEGIN
							--					set @NombreProductoAux = @NombreProducto;				
							--					set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@contador2);							
							--					set @consulta = 'update Temporal set ' 
							--					set @consulta = @consulta + @NombreProductoAux + ' = ' 
							--					set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where id = @contador) + CHAR(39)
							--					set @consulta = @consulta + ' where id = 2' 															
							--					set @contador2 = @contador2 + 1;								
							--					exec (@consulta)												
							--				END
							--				WHILE (@contador2 <= 4)
							--				BEGIN												
							--					set @consulta = 'update Temporal set ' 
							--					set @consulta = @consulta + @NombreProductoAux + ' = 0 ' 												
							--					set @consulta = @consulta + ' where id = 2' 															
							--					set @contador2 = @contador2 + 1;								
							--					exec (@consulta)												
							--				END
											
							--				SET @contador = @contador+1
							--END
						--END	
						--ELSE
						--BEGIN	
						--		set @consulta = 'update Temporal set ' 
						--		set @consulta = @consulta + @NombreProducto + ' = ' 
						--		set @consulta = @consulta + (select Dato from @Materiales where id = @contador)  
						--		set @consulta = @consulta + 'where id = 2' ;																					
						--		exec (@consulta);
						--		print @consulta
						--		SET @contador = @contador+1
						--END
						
						
--					END
--					SELECT * FROM TEMPORAL
--END



GO

/* [dbo].[Rtp_TraerDetalleRetiro] */

create procedure [dbo].[Rtp_TraerDetalleRetiro] (@Id_Cuadre int)
as
begin
	select Id_Venta, Id_Devolucion, NroOrdenTrabajo, Fecha, Nombre, Cod_Inicio, ChipID, Cantidad
	from tbl_SaldoRetiro where id_cuadre = @Id_Cuadre
end
GO

/* [dbo].[sp_actualizarEquipoTecnico] */
create proc [dbo].[sp_actualizarEquipoTecnico]( @id_vendedor int ,@ruta  int  )
as
	

	update tbl_ruta set id_Vendedor = @id_vendedor
	where id_ruta = @ruta 
GO

/* [dbo].[sp_alterdiagram] */

	CREATE PROCEDURE dbo.sp_alterdiagram
	(
		@diagramname 	sysname,
		@owner_id	int	= null,
		@version 	int,
		@definition 	varbinary(max)
	)
	WITH EXECUTE AS 'dbo'
	AS
	BEGIN
		set nocount on
	
		declare @theId 			int
		declare @retval 		int
		declare @IsDbo 			int
		
		declare @UIDFound 		int
		declare @DiagId			int
		declare @ShouldChangeUID	int
	
		if(@diagramname is null)
		begin
			RAISERROR ('Invalid ARG', 16, 1)
			return -1
		end
	
		execute as caller;
		select @theId = DATABASE_PRINCIPAL_ID();	 
		select @IsDbo = IS_MEMBER(N'db_owner'); 
		if(@owner_id is null)
			select @owner_id = @theId;
		revert;
	
		select @ShouldChangeUID = 0
		select @DiagId = diagram_id, @UIDFound = principal_id from dbo.sysdiagrams where principal_id = @owner_id and name = @diagramname 
		
		if(@DiagId IS NULL or (@IsDbo = 0 and @theId <> @UIDFound))
		begin
			RAISERROR ('Diagram does not exist or you do not have permission.', 16, 1);
			return -3
		end
	
		if(@IsDbo <> 0)
		begin
			if(@UIDFound is null or USER_NAME(@UIDFound) is null) -- invalid principal_id
			begin
				select @ShouldChangeUID = 1 ;
			end
		end

		-- update dds data			
		update dbo.sysdiagrams set definition = @definition where diagram_id = @DiagId ;

		-- change owner
		if(@ShouldChangeUID = 1)
			update dbo.sysdiagrams set principal_id = @theId where diagram_id = @DiagId ;

		-- update dds version
		if(@version is not null)
			update dbo.sysdiagrams set version = @version where diagram_id = @DiagId ;

		return 0
	END
	
GO

/* [dbo].[sp_BorrarLog] */
create proc sp_BorrarLog
as
begin

-- cambiamos el recovery a nodo simple
ALTER DATABASE BDSistemaAntenaPM
SET RECOVERY SIMPLE;

-- reducirmos el archivo log a 1 MB.
DBCC SHRINKFILE (BDAlmacen_Log, 1);

-- devolvemos el nivel de recovery a full
ALTER DATABASE BDSistemaAntenaPM
SET RECOVERY FULL;


end
GO

/* [dbo].[sp_BuscarSerieIngresoAlmacen] */

CREATE proc [dbo].[sp_BuscarSerieIngresoAlmacen] (@Serie nvarchar(20))
as 
begin
	select i.Id_IngresoAlmacen as ID, i.Proveedor as Proveedor_Ruta,i.Observacion,i.NroRecibo,dbo.DateOnly(i.Fecha)Fecha
	,u.Id_Usuario, u.Nombre Usuario,p.Nombre Producto, ci.Cod_Inicio, 'IngresoAlmacen' as Tipo
	from tbl_IngresoAlmacen i, tbl_CodigoIngresoAlmacen ci, tbl_Usuario u, tbl_Producto p
	where i.Id_IngresoAlmacen = ci.Id_IngresoAlmacen
	and u.Id_Usuario = i.Id_Usuario
	and p.Id_Producto = ci.Id_Producto
	and i.E_Eliminado = 0 and ci.E_Eliminado=0
	and ci.Cod_Inicio like '%' + @Serie +'%' 
	
	union all
	
	select i.Id_AlmacenVendedor,(select nombre from tbl_ruta where id_ruta = i.Id_Ruta) as Proveedor_Ruta, i.Observacion,'' as NroRecibo, dbo.DateOnly(i.Fecha)Fecha
	,u.Id_Usuario, u.Nombre Usuario,p.Nombre Producto, ci.Cod_Inicio, 'AlmacenVendedor' as Tipo
	from tbl_almacenvendedor i, tbl_codigoalmacenvendedor ci, tbl_Usuario u, tbl_Producto p
	where i.Id_AlmacenVendedor = ci.Id_AlmacenVendedor
	and u.Id_Usuario = i.Id_Usuario
	and p.Id_Producto = ci.Id_Producto
	and i.E_Eliminado = 0 and ci.E_Eliminado=0
	and ci.Cod_Inicio like '%' + @Serie +'%' 
	
	union all
	
	select i.Id_Venta,(select nombre from tbl_ruta where id_ruta = i.Id_Ruta) as Proveedor_Ruta, i.Observacion,i.OrdenTrabajo as NroRecibo_OrdenTrabajo, dbo.DateOnly(i.Fecha_Registro)Fecha
	,u.Id_Usuario, u.Nombre Usuario,p.Nombre Producto, ci.Cod_Inicio, 'OrdenTrabajo' as Tipo
	from tbl_venta i, tbl_codigoventa ci, tbl_Usuario u, tbl_Producto p
	where i.Id_venta = ci.Id_venta
	and u.Id_Usuario = i.Id_Usuario
	and p.Id_Producto = ci.Id_Producto
	and i.E_Eliminado = 0 and ci.E_Eliminado=0
	and ci.Cod_Inicio like '%' + @Serie +'%' 
end
GO

/* [dbo].[sp_CrearProductoEnSaldoTarjetaVendedor] */
CREATE procedure [dbo].[sp_CrearProductoEnSaldoTarjetaVendedor]
as
declare @contador int
declare @cuantos int
declare @contador2 int
declare @cuantos2 int
declare @Id_Producto  int
 
declare @Id_Ruta int
set @cuantos = 1
set @contador = 1
 
declare @Indice int
set @Indice = 0
declare @TRuta table(Id int identity, Id_Ruta int )
                insert into @TRuta
                select Id_Ruta from tbl_Ruta where E_Eliminado = 0
               
                set @cuantos = (select COUNT(*) from @TRuta)           
               
                while (@contador<=@cuantos)
                begin
                               print '--------------------'
                               set @Id_Ruta = (select id_ruta from @TRuta where Id = @contador )                 
                               print 'ruta ' + convert(varchar, @Id_Ruta )
                              
                               declare @ProductosNuevos table(Id int identity, Id_Producto int)                                                       
                              
                               insert into @ProductosNuevos
                               select Id_Producto from tbl_Producto where Id_Producto not in (select Id_Producto from tbl_SaldoTarjetas where Id_Ruta =@Id_Ruta )
                              
                               select * from @ProductosNuevos
                              
                               set @cuantos2=(select COUNT (*) from @ProductosNuevos)
                              
                               print 'cuantos ' + convert(varchar, @cuantos2)
                               set @contador2 =1
                               while(@contador2 <= @cuantos2)
                               begin
                               print @contador2
                               set @Indice = @Indice + 1
                                               set @Id_Producto = (select id_producto from @ProductosNuevos where Id = @Indice )
                                               insert into tbl_SaldoTarjetas values (@Id_Producto,@Id_Ruta,0,0)      
                                              
                                               set @contador2 = @contador2 +1;
                                              
                               end
                               set @contador = @contador +1;
                              
                               delete from @ProductosNuevos;
                end
GO

/* [dbo].[sp_creatediagram] */

	CREATE PROCEDURE dbo.sp_creatediagram
	(
		@diagramname 	sysname,
		@owner_id		int	= null, 	
		@version 		int,
		@definition 	varbinary(max)
	)
	WITH EXECUTE AS 'dbo'
	AS
	BEGIN
		set nocount on
	
		declare @theId int
		declare @retval int
		declare @IsDbo	int
		declare @userName sysname
		if(@version is null or @diagramname is null)
		begin
			RAISERROR (N'E_INVALIDARG', 16, 1);
			return -1
		end
	
		execute as caller;
		select @theId = DATABASE_PRINCIPAL_ID(); 
		select @IsDbo = IS_MEMBER(N'db_owner');
		revert; 
		
		if @owner_id is null
		begin
			select @owner_id = @theId;
		end
		else
		begin
			if @theId <> @owner_id
			begin
				if @IsDbo = 0
				begin
					RAISERROR (N'E_INVALIDARG', 16, 1);
					return -1
				end
				select @theId = @owner_id
			end
		end
		-- next 2 line only for test, will be removed after define name unique
		if EXISTS(select diagram_id from dbo.sysdiagrams where principal_id = @theId and name = @diagramname)
		begin
			RAISERROR ('The name is already used.', 16, 1);
			return -2
		end
	
		insert into dbo.sysdiagrams(name, principal_id , version, definition)
				VALUES(@diagramname, @theId, @version, @definition) ;
		
		select @retval = @@IDENTITY 
		return @retval
	END
	
GO

/* [dbo].[sp_dropdiagram] */

	CREATE PROCEDURE dbo.sp_dropdiagram
	(
		@diagramname 	sysname,
		@owner_id	int	= null
	)
	WITH EXECUTE AS 'dbo'
	AS
	BEGIN
		set nocount on
		declare @theId 			int
		declare @IsDbo 			int
		
		declare @UIDFound 		int
		declare @DiagId			int
	
		if(@diagramname is null)
		begin
			RAISERROR ('Invalid value', 16, 1);
			return -1
		end
	
		EXECUTE AS CALLER;
		select @theId = DATABASE_PRINCIPAL_ID();
		select @IsDbo = IS_MEMBER(N'db_owner'); 
		if(@owner_id is null)
			select @owner_id = @theId;
		REVERT; 
		
		select @DiagId = diagram_id, @UIDFound = principal_id from dbo.sysdiagrams where principal_id = @owner_id and name = @diagramname 
		if(@DiagId IS NULL or (@IsDbo = 0 and @UIDFound <> @theId))
		begin
			RAISERROR ('Diagram does not exist or you do not have permission.', 16, 1)
			return -3
		end
	
		delete from dbo.sysdiagrams where diagram_id = @DiagId;
	
		return 0;
	END
	
GO

/* [dbo].[sp_Eliminar_AlmacenVendedor_PedidoVendedor_ModificarSaldoTarjeta_X_Id_PedidoVendedor] */
create procedure [dbo].[sp_Eliminar_AlmacenVendedor_PedidoVendedor_ModificarSaldoTarjeta_X_Id_PedidoVendedor](@Id_PedidoVendedor int,@Observacion nvarchar(150))
as
update tbl_almacenvendedor set e_eliminado = 1, Observacion=Observacion + ' - ' +@Observacion where Id_PedidoVendedor = @Id_PedidoVendedor
update tbl_codigoalmacenvendedor set e_eliminado = 1 where Id_AlmacenVendedor in (select Id_AlmacenVendedor from tbl_almacenvendedor where Id_PedidoVendedor = @Id_PedidoVendedor)

update tbl_pedidovendedor set e_eliminado = 1, Observacion=Observacion + ' - ' +@Observacion where id_pedidovendedor = @Id_PedidoVendedor
update tbl_codigopedidovendedor set e_eliminado = 1 where id_pedidovendedor = @Id_PedidoVendedor

update tbl_saldotarjetas
set cantidad = C.Saldo
from tbl_codigopedidovendedor C inner join tbl_saldotarjetas S on C.id_Producto = S.id_Producto
where C.id_PedidoVendedor = @Id_PedidoVendedor and S.id_ruta in (select id_ruta from tbl_pedidovendedor where Id_PedidoVendedor = @Id_PedidoVendedor)
GO

/* [dbo].[sp_EliminarCuadre_ModificarSaldoTarjeta_X_Id_Cuadre] */

CREATE procedure [dbo].[sp_EliminarCuadre_ModificarSaldoTarjeta_X_Id_Cuadre](@Id_Cuadre int,@Observacion nvarchar(150))
as

declare @id_ruta int
set @id_ruta = (select id_ruta from tbl_Cuadre where Id_Cuadre=@Id_Cuadre )
declare @sobrantes table (id int primary key identity, Id_CodigoCuadre int,Id_Cuadre int,Id_Producto int,ItemsSobrantes decimal(18,2),estado bit)
insert into @sobrantes
select Id_CodigoCuadre,Id_Cuadre,Id_Producto,ItemsSobrantes,0 from tbl_CodigoCuadre where id_cuadre=@Id_Cuadre and E_Eliminado=0 order by Id_Producto 


declare @SaldoTarjetas table(id int primary key identity,Id_SaldoTarjetas int, Id_Producto int,Id_Ruta int,Cantidad decimal(18,2), estado bit)
insert into @SaldoTarjetas
select Id_SaldoTarjetas,Id_Producto,Id_Ruta,Cantidad,0 from tbl_SaldoTarjetas where Id_Ruta =@id_ruta and E_Eliminado=0

update @sobrantes
set estado =1
from @SaldoTarjetas s inner join @sobrantes sobr on sobr.Id_Producto = s.Id_Producto
where s.Cantidad = sobr.ItemsSobrantes

update @SaldoTarjetas
set estado =1
from @SaldoTarjetas s inner join @sobrantes sobr on sobr.Id_Producto = s.Id_Producto
where s.Cantidad = sobr.ItemsSobrantes

declare @hayErrores int
set @hayErrores=(select COUNT(*) from @sobrantes where estado =0 and ItemsSobrantes<>0)
	
declare @hay2Errores int
set @hay2Errores=(select COUNT(*) from @SaldoTarjetas where estado =0 and Cantidad<>0)

if(@hayErrores=0)	
begin
	if(@hay2Errores=0)		
	begin
		select 'Se borra'
		update tbl_cuadre set e_eliminado = 1, Observacion=Observacion+' - '+@Observacion where id_cuadre = @Id_Cuadre
		update tbl_codigocuadre set e_eliminado = 1 where id_cuadre = @Id_Cuadre

		update tbl_saldotarjetas
		set cantidad = C.itemssobrantes + C.itemsvendidos
		from tbl_codigocuadre C inner join tbl_saldotarjetas S on C.id_Producto = S.id_Producto
		where C.id_cuadre = @Id_Cuadre and S.id_ruta in (select id_ruta from tbl_cuadre where id_cuadre = @Id_Cuadre)
	
	end
	else select 'No se borro'
end
else select 'No se borro'

GO

/* [dbo].[sp_ExisteCierreAlmacen] */

CREATE proc [dbo].[sp_ExisteCierreAlmacen](@fecha datetime)
as 
Begin	
	declare @FechaInicio datetime 
	set @FechaInicio = (select Max (Fecha) as Fecha from tbl_cierrealmacen where e_eliminado = 0)
	declare @cantidadcierres int
	set @cantidadcierres = (select COUNT(*) from tbl_CierreAlmacen where E_Eliminado=0)
	if (@cantidadcierres = 0)
	begin
		select * from 
			(
			select 'PEDIDOVENDEDOR' as MOVIMIENTO, count(*) as Cantidad from tbl_pedidovendedor where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'VENTA' as MOVIMIENTO, count(*) as Cantidad from tbl_venta where 
				dbo.dateonly(fecha_ejecucion) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha_ejecucion) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'DEVOLUCION' as MOVIMIENTO, count(*) as Cantidad from tbl_devolucion where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'BAJA_PRODUCTOS' as MOVIMIENTO, count(*) as Cantidad from tbl_BajaProductos where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'SALIDA_TRASPASOS' as MOVIMIENTO, count(*) as Cantidad from tbl_SalidaTraspaso where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			) Temporal where Cantidad > 0
	end
	else
	begin
		if(dbo.dateonly(@fecha) <> dbo.dateonly(@FechaInicio))
		begin
			select * from 
			(
			select 'PEDIDOVENDEDOR' as MOVIMIENTO, count(*) as Cantidad from tbl_pedidovendedor where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'VENTA' as MOVIMIENTO, count(*) as Cantidad from tbl_venta where 
				dbo.dateonly(fecha_ejecucion) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha_ejecucion) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'DEVOLUCION' as MOVIMIENTO, count(*) as Cantidad from tbl_devolucion where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'BAJA_PRODUCTOS' as MOVIMIENTO, count(*) as Cantidad from tbl_BajaProductos where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'SALIDA_TRASPASOS' as MOVIMIENTO, count(*) as Cantidad from tbl_SalidaTraspaso where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			) Temporal where Cantidad > 0
		end
		else
		begin
			select 'CIERREALMACEN'as MOVIMIENTO, 1 as Cantidad 
		end
	end	
End

GO

/* [dbo].[sp_ExisteCierreAlmacenPRPD] */

create proc [dbo].[sp_ExisteCierreAlmacenPRPD](@fecha datetime)
as 
Begin	
	declare @FechaInicio datetime 
	set @FechaInicio = (select Max (Fecha) as Fecha from tbl_CierreAlmacenPR_PD where e_eliminado = 0)
	declare @cantidadcierres int
	set @cantidadcierres = (select COUNT(*) from tbl_CierreAlmacenPR_PD where E_Eliminado=0)
	if (@cantidadcierres = 0)
	begin
		select * from 
			(
			select 'PEDIDOVENDEDOR' as MOVIMIENTO, count(*) as Cantidad from tbl_pedidovendedor where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'VENTA' as MOVIMIENTO, count(*) as Cantidad from tbl_venta where 
				dbo.dateonly(fecha_ejecucion) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha_ejecucion) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'DEVOLUCION' as MOVIMIENTO, count(*) as Cantidad from tbl_devolucion where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'BAJA_PRODUCTOS' as MOVIMIENTO, count(*) as Cantidad from tbl_BajaProductos where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'SALIDA_TRASPASOS' as MOVIMIENTO, count(*) as Cantidad from tbl_SalidaTraspaso where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			) Temporal where Cantidad > 0
	end
	else
	begin
		if(dbo.dateonly(@fecha) <> dbo.dateonly(@FechaInicio))
		begin
			select * from 
			(
			select 'PEDIDOVENDEDOR' as MOVIMIENTO, count(*) as Cantidad from tbl_pedidovendedor where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'VENTA' as MOVIMIENTO, count(*) as Cantidad from tbl_venta where 
				dbo.dateonly(fecha_ejecucion) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha_ejecucion) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'DEVOLUCION' as MOVIMIENTO, count(*) as Cantidad from tbl_devolucion where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'BAJA_PRODUCTOS' as MOVIMIENTO, count(*) as Cantidad from tbl_BajaProductos where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			Union ALL
			select 'SALIDA_TRASPASOS' as MOVIMIENTO, count(*) as Cantidad from tbl_SalidaTraspaso where 
				dbo.dateonly(fecha) > dbo.dateonly(@FechaInicio) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and e_eliminado = 0
			) Temporal where Cantidad > 0
		end
		else
		begin
			select 'CIERREALMACEN_PRPD'as MOVIMIENTO, 1 as Cantidad 
		end
	end	
End

GO

/* [dbo].[sp_helpdiagramdefinition] */

	CREATE PROCEDURE dbo.sp_helpdiagramdefinition
	(
		@diagramname 	sysname,
		@owner_id	int	= null 		
	)
	WITH EXECUTE AS N'dbo'
	AS
	BEGIN
		set nocount on

		declare @theId 		int
		declare @IsDbo 		int
		declare @DiagId		int
		declare @UIDFound	int
	
		if(@diagramname is null)
		begin
			RAISERROR (N'E_INVALIDARG', 16, 1);
			return -1
		end
	
		execute as caller;
		select @theId = DATABASE_PRINCIPAL_ID();
		select @IsDbo = IS_MEMBER(N'db_owner');
		if(@owner_id is null)
			select @owner_id = @theId;
		revert; 
	
		select @DiagId = diagram_id, @UIDFound = principal_id from dbo.sysdiagrams where principal_id = @owner_id and name = @diagramname;
		if(@DiagId IS NULL or (@IsDbo = 0 and @UIDFound <> @theId ))
		begin
			RAISERROR ('Diagram does not exist or you do not have permission.', 16, 1);
			return -3
		end

		select version, definition FROM dbo.sysdiagrams where diagram_id = @DiagId ; 
		return 0
	END
	
GO

/* [dbo].[sp_helpdiagrams] */

	CREATE PROCEDURE dbo.sp_helpdiagrams
	(
		@diagramname sysname = NULL,
		@owner_id int = NULL
	)
	WITH EXECUTE AS N'dbo'
	AS
	BEGIN
		DECLARE @user sysname
		DECLARE @dboLogin bit
		EXECUTE AS CALLER;
			SET @user = USER_NAME();
			SET @dboLogin = CONVERT(bit,IS_MEMBER('db_owner'));
		REVERT;
		SELECT
			[Database] = DB_NAME(),
			[Name] = name,
			[ID] = diagram_id,
			[Owner] = USER_NAME(principal_id),
			[OwnerID] = principal_id
		FROM
			sysdiagrams
		WHERE
			(@dboLogin = 1 OR USER_NAME(principal_id) = @user) AND
			(@diagramname IS NULL OR name = @diagramname) AND
			(@owner_id IS NULL OR principal_id = @owner_id)
		ORDER BY
			4, 5, 1
	END
	
GO

/* [dbo].[sp_ModificarOT_OTRealizada] */
create proc [dbo].[sp_ModificarOT_OTRealizada](@Observacion nvarchar(max), @Id_Estado int, @NroOrden int)
as
	update tbl_OrdenTrabajo
	set Observacion=@Observacion,
	Id_Estado=@Id_Estado
	where NroOrden=@NroOrden
GO

/* [dbo].[sp_nps_listar_supervisores_sucursal] */
CREATE PROCEDURE dbo.SP_NPS_LISTAR_SUPERVISORES_SUCURSAL
  @IdSucursal INT
AS
BEGIN
  SET NOCOUNT ON;
  SELECT DISTINCT
    cc.idUsuarioSupervisor AS idSupervisor,
    ISNULL(u.Nombre, cc.supervisorACargo) AS supervisor
  FROM dbo.tbl_ConformacionCuadrillaDiario cc
  LEFT JOIN dbo.tbl_Usuario u ON u.Id_Usuario = cc.idUsuarioSupervisor
  WHERE ISNULL(cc.e_eliminado, 0) = 0
  ORDER BY supervisor;
END

GO

/* [dbo].[sp_nps_listar_tecnicos_por_supervisor] */
CREATE PROCEDURE dbo.SP_NPS_LISTAR_TECNICOS_POR_SUPERVISOR
  @IdSucursal INT,
  @IdSupervisor INT
AS
BEGIN
  SET NOCOUNT ON;
  SELECT DISTINCT
    cc.id_tecnico AS idTecnico,
    ISNULL(ut.Nombre, cc.tecnico) AS tecnico
  FROM dbo.tbl_ConformacionCuadrillaDiario cc
  LEFT JOIN dbo.tbl_UsuarioTecnico map ON map.id_Vendedor = cc.id_tecnico AND ISNULL(map.e_eliminado,0)=0
  LEFT JOIN dbo.tbl_Usuario ut ON ut.Id_Usuario = map.id_Usuario
  WHERE ISNULL(cc.e_eliminado, 0) = 0
    AND (@IdSupervisor = 0 OR cc.idUsuarioSupervisor = @IdSupervisor)
  ORDER BY tecnico;
END

GO

/* [dbo].[sp_ObtenerAlmacenVendedor_X_ID] */


CREATE proc [dbo].[sp_ObtenerAlmacenVendedor_X_ID] (@Id int)
as 
SELECT     dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor AS Id_AlmacenCliente, UPPER(dbo.tbl_Usuario.Nombre) AS Usuario, 
UPPER(dbo.tbl_Vendedor.Nombre +' - '+ dbo.tbl_Ruta.Nombre)  AS Cliente, dbo.tbl_AlmacenVendedor.Fecha,dbo.tbl_AlmacenVendedor.Fecha_Registro, dbo.tbl_AlmacenVendedor.Observacion,
dbo.tbl_AlmacenVendedor.Id_PedidoVendedor
FROM         dbo.tbl_AlmacenVendedor INNER JOIN
                      dbo.tbl_Usuario ON dbo.tbl_AlmacenVendedor.Id_Usuario = dbo.tbl_Usuario.Id_Usuario INNER JOIN
                      dbo.tbl_Vendedor ON dbo.tbl_AlmacenVendedor.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor INNER JOIN
                      dbo.tbl_Ruta ON dbo.tbl_AlmacenVendedor.Id_Ruta = dbo.tbl_Ruta.Id_Ruta
WHERE     (dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor = @Id)

GO

/* [dbo].[sp_ObtenerAlmacenVendedorLocal] */
create  proc [dbo].[sp_ObtenerAlmacenVendedorLocal]--(@id_tipoTransaccion int)
as

	select c.Id_AlmacenVendedorLocal,c.Id_AlmacenVendedor,c.Id_codigoAlmacenVendedor,c.Id_Ruta,r.nombre Ruta,
	c.Id_Vendedor,v.nombre Vendedor, c.id_pedidoVendedor,c.Id_usuario,u.Nombre Usuario,c.Fecha,c.Fecha_Registro,c.observacion
    ,c.Id_Producto,p.nombre Producto,
	c.cod_inicio,c.ChipId,c.cantidad,
	18 id_sucursal,(select upper( sucursal) from tbl_Sucursal where id_sucursal = 18 )Sucursal,  ---Cambiar Sucursal		
	c.e_eliminado, 
	c.Id_TipoTransaccion
	from tbl_AlmacenVendedorLocal c
	inner join tbl_Usuario u on u.id_usuario =c.Id_Usuario
	inner join  tbl_producto p on p.id_producto = c.id_producto 
	inner join tbl_ruta r on r.id_ruta =c.Id_Ruta
	inner join tbl_vendedor v on v.id_vendedor = c.Id_Vendedor	
	where  E_enviado = 0 
	

GO

/* [dbo].[sp_ObtenerBajaProductosLocal] */
create proc [dbo].[sp_ObtenerBajaProductosLocal]--(@id_tipoTransaccion int)
as
	select c.Id_BajaProductoLocal,c.Id_BajaProductos,c.Id_CodigoBajaProductos 
	,c.Id_Usuario,u.Nombre Usuario,
	c.Observacion,c.Fecha,c.Fecha_Registro,c.Id_Ruta, r.nombre Ruta,
	c.Id_EstadoProductos,sp.nombre 	EstadoProducto,
	c.Id_Tecnico,v.nombre Vendedor, 		
	c.Id_TipoBajaProductosPendiente   , tb.TipoBaja TipoBajaProductosPendiente ,
	c.Total, c.Cobrado,c.Id_TipoCobroBaja,		
	c.Id_Producto,p.nombre Producto,	
	c.Serie,c.ChipID ,c.Cantidad ,c.IdRuta ,c.PrecioU ,c.TotalP,
    18 id_sucursal,(select upper( sucursal) from tbl_Sucursal where id_sucursal = 18 )Sucursal,	
	c.e_eliminado, 
	c.Id_TipoTransaccion
	from tbl_BajaProductosLocal c
	inner join tbl_Usuario u on u.id_usuario =c.Id_Usuario --ok
	inner join tbl_producto p on p.id_producto = c.id_producto 	--ok
	inner join tbl_estadoproducto sp on sp.Id_EstadoProducto = c.Id_EstadoProductos	 -- ok
	inner join  tbl_TipoBajaProductosPendiente tb on tb.Id_TipoBajaProductosPendiente =c.Id_TipoBajaProductosPendiente  -- ok
	left join  Tbl_Vendedor v on c.Id_Tecnico = v.id_vendedor -- ok
    left join  Tbl_Ruta r on r.id_ruta = c.id_ruta	 	--ok        	
	where  E_enviado = 0 	

GO

/* [dbo].[sp_ObtenerBitacoraLocal] */
create proc sp_ObtenerBitacoraLocal
as
	select  v.Id_BitacoraLocal,v.Id_Bitacora
	,v.id_producto,(select nombre from tbl_producto  where id_producto = v.id_producto )Nombre
	,v.serial,v.chipID, v.codigo, v.tabla, v.Fechatransaccion, v.Fecharegistro
	,v.id_ruta,(select nombre from tbl_ruta  where id_ruta =  v.id_ruta )Ruta
	,v.id_usuario,(select nombre from tbl_usuario  where id_usuario =  v.id_usuario)Usuario 
	,v.e_eliminado,v.observacion
	,v.idestadoproducto, (select nombre from tbl_estadoproducto where id_estadoproducto =  v.idestadoproducto )Estadoproducto 
	 ,18 id_sucursal,(select upper( sucursal) from tbl_sucursal where id_sucursal = 18 )Sucursal  ---Cambiar Sucursal
	from  tbl_BitacoraLocal v
	where E_enviado = 0 

GO

/* [dbo].[sp_ObtenerCierreAlmacen] */
CREATE proc [dbo].[sp_ObtenerCierreAlmacen](@Fecha datetime)
as 
Begin	
	declare @tbl_Cierre 
	table (
	Id_Producto int, 
	Nombre nvarchar(max),
	SaldoDiaAnterior decimal(18,2),
	SaldoDiaAnteriorDevolucion decimal(18,2),
	IngresoDia decimal(18,2),
	DevolucionIngreso decimal(18,2),
	DevolucionSalida decimal(18,2),
	SalidaDia decimal(18,2),
	SalidaBaja decimal(18,2),
	SaldoDiaHoy decimal(18,2),
	SaldoDiaHoyDevolucion decimal(18,2))
	------------------------saldo dia anterior
	------------------------saldo dia anterior
	--es el saldo del dia anterior
	insert into @tbl_Cierre
			SELECT 
			p.Id_Producto, p.Nombre,isnull(cc.SaldoDiaHoy,0) SaldoDiaHoy, 
			0,--isnull(cc.SaldoDiaHoyDevolucion,0) SaldoDiaHoyDevolucion,
			0,0,0,0,0,isnull(cc.SaldoDiaHoy,0) SaldoDiaHoy, 
			0--isnull(cc.SaldoDiaHoyDevolucion,0) SaldoDiaHoyDevolucion
			FROM   dbo.tbl_Producto p left JOIN dbo.tbl_CodigoCierreAlmacen cc ON p.Id_Producto = cc.Id_Producto 
			AND cc.Id_CierreAlmacen = (select MAX(Id_CierreAlmacen)  from dbo.tbl_CierreAlmacen cd
			WHERE DBO.DATEONLY(cd.Fecha)<DBO.DATEONLY(@Fecha) and cd.E_Eliminado=0 )
			WHERE  p.e_eliminado=0 
			ORDER BY p.Observacion 
	declare @auxiliar table (Id_Producto int, Cantidad decimal(18,2))

	------------------------Ingreso Dia
	------------------------Ingreso Dia
	--compra del dia
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto,sum(cia.Cantidad)AS CompraDia
			FROM    dbo.tbl_IngresoAlmacen ia INNER JOIN 
			dbo.tbl_CodigoIngresoAlmacen cia ON ia.Id_IngresoAlmacen = cia.Id_IngresoAlmacen INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
			group by dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre,dbo.tbl_Producto.PrecioVenta

	update @tbl_Cierre
	set IngresoDia = a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto
		
		
	delete from @auxiliar 
	
	------------------------Salida Dia
	------------------------Salida Dia
	--salida dia = salida vendedor + entregado a tigo de almacen (excedente de almacen) + traspaso 
	--salida vendedor	
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, isnull(SUM(dbo.tbl_CodigoAlmacenVendedor.Cantidad),0)Cantidad				
			FROM    dbo.tbl_CodigoAlmacenVendedor INNER JOIN  
			dbo.tbl_AlmacenVendedor ON dbo.tbl_CodigoAlmacenVendedor.Id_AlmacenVendedor = dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor INNER JOIN  
			dbo.tbl_Producto ON dbo.tbl_CodigoAlmacenVendedor.Id_Producto = dbo.tbl_Producto.Id_Producto  
			WHERE   (dbo.tbl_CodigoAlmacenVendedor.E_Eliminado = 0) AND (dbo.tbl_AlmacenVendedor.E_Eliminado = 0) AND   
			dbo.DateOnly(dbo.tbl_AlmacenVendedor.Fecha) = dbo.DateOnly(@Fecha)
			and tbl_Producto.e_eliminado=0
			GROUP BY dbo.tbl_Producto.Id_Producto 
			order by dbo.tbl_Producto.Id_Producto

			
	update @tbl_Cierre
	set SalidaDia = a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto
	delete from @auxiliar 
	
	--entrega Tigo Devolucion excedente almacen
		insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and Id_TipoDevolucion=5
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set SalidaDia = SalidaDia + a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	--salida traspaso 
		insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_SalidaTraspaso ia INNER JOIN 
			dbo.tbl_CodigoSalidaTraspaso cia ON ia.Id_SalidaTraspaso = cia.Id_SalidaTraspaso  INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set SalidaDia = SalidaDia + a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	
	------------------------Salida Baja
	------------------------Salida Baja
	--select * from tbl_bajaproductos bp
	--select * from tbl_EstadoProducto
	--Baja Productos 
	insert into @auxiliar 
			SELECT  p.Id_Producto, isnull(SUM(cbp.Cantidad),0)Cantidad				
			FROM    dbo.tbl_CodigoBajaProductos cbp INNER JOIN  
			dbo.tbl_BajaProductos bp ON cbp.id_BajaProductos= bp.id_BajaProductos INNER JOIN  
			dbo.tbl_Producto p ON cbp.Id_Producto = p.Id_Producto  
			WHERE   cbp.E_Eliminado = 0 AND bp.E_Eliminado = 0 AND   
			dbo.DateOnly(bp.Fecha) = dbo.DateOnly(@Fecha)
			and p.e_eliminado=0 and bp.id_estadoproductos in(1) and id_ruta in (0)
			GROUP BY p.Id_Producto 
			order by p.Id_Producto

			
	update @tbl_Cierre
	set SalidaBaja = a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto
	delete from @auxiliar 	
	------------------------Devolucion Ingreso
	------------------------Devolucion Ingreso
	--select * from tbl_TipoDevolucion--1 material dañado  2 material retirado   3 excedente ruta  4 devuelto tigo
	--1 material dañado no importa si esta entregado
	--insert into @auxiliar 
	--		SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
	--		FROM    dbo.tbl_Devolucion ia INNER JOIN 
	--		dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
	--		dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
	--		where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion in (1)
	--		group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set DevolucionIngreso = 0--//a.Cantidad
	from @tbl_Cierre c --inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	--------
	--1 material dañado retirado
	--insert into @auxiliar 
	--		SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
	--		FROM    dbo.tbl_Devolucion ia INNER JOIN 
	--		dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
	--		dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
	--		where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion in (2)
	--		and cia.Entregado=1
	--		group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set DevolucionIngreso =0--DevolucionIngreso+ a.Cantidad
	from @tbl_Cierre c --inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 


	------------------------Devolucion Salida
	------------------------Devolucion Salida
    --si es excedente tiene que sacar de almacen
    --devuelvo a tigo
--    select * from tbl_TipoDevolucion devuelto a tigo
	--insert into @auxiliar 
	--		SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
	--		FROM    dbo.tbl_Devolucion ia INNER JOIN 
	--		dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
	--		dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
	--		where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and Id_TipoDevolucion=4
	--		group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set DevolucionSalida = 0--a.Cantidad	
	from @tbl_Cierre c --inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
--	select * from tbl_BajaProductos select * from tbl_EstadoProducto  --4 retirado ot 5 dañado
	--baja de material retirado
		--insert into @auxiliar 
		--	SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
		--	FROM    dbo.tbl_BajaProductos ia INNER JOIN 
		--	dbo.tbl_Codigobajaproductos cia ON ia.Id_bajaproductos= cia.Id_bajaproductos INNER JOIN 
		--	dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
		--	where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
		--	and ia.Id_EstadoProductos in (4,5)
		--	group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set DevolucionSalida = 0--DevolucionSalida+a.Cantidad	
	from @tbl_Cierre c --inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	--[sp_ObtenerCierreAlmacen] '19/12/2017'
	
	------------------------SaldoDiaHoyDevolucion
	------------------------SaldoDiaHoyDevolucion
	update @tbl_Cierre
	set SaldoDiaHoyDevolucion = 0--SaldoDiaAnteriorDevolucion+DevolucionIngreso-DevolucionSalida 
	from @tbl_Cierre c

	------------------------Ingreso x Excende Ruta
--select * from tbl_TipoDevolucion --excedente en ruta
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion = 3
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set IngresoDia = IngresoDia + a.Cantidad
	from @tbl_Cierre c INNER JOIN @auxiliar a on c.id_Producto = a.id_producto


	delete from @auxiliar 

	------------------------Devolucion Salida
	------------------------Devolucion Salida
	--select * from tbl_Devolucion
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 
			group by dbo.tbl_Producto.Id_Producto

	update @tbl_Cierre
	set SaldoDiaHoy = SaldoDiaHoy - a.Cantidad	
	from @tbl_Cierre c INNER JOIN @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar
	------------------------Saldo Dia
	------------------------Saldo Dia

	update @tbl_Cierre
	set SaldoDiaHoy = SaldoDiaAnterior + IngresoDia- SalidaDia - SalidaBaja 

	update @tbl_Cierre
	set SaldoDiaHoyDevolucion = SaldoDiaAnteriorDevolucion+DevolucionIngreso-DevolucionSalida 

	select * from @tbl_Cierre order by Nombre 
end

GO

/* [dbo].[sp_ObtenerCierreAlmacen_Prueba] */
CREATE proc [dbo].[sp_ObtenerCierreAlmacen_Prueba](@Fecha datetime)
as 
--[sp_ObtenerCierreAlmacen_Prueba] '28/12/2016'
--go
--[sp_ObtenerCierreAlmacen] '28/12/2016'

--select * from tbl_Producto
--select * from tbl_Tipodevolucion
--declare @Fecha datetime
--set @Fecha ='25/10/2016'
declare @tbl_Cierre 
table (
Id_Producto int, 
Nombre nvarchar(max),
SaldoDiaAnterior decimal(18,2),
SaldoDiaAnteriorDevolucion decimal(18,2),
IngresoDia decimal(18,2),
DevolucionIngreso decimal(18,2),
DevolucionSalida decimal(18,2),
SalidaDia decimal(18,2),
SaldoDiaHoy decimal(18,2),
SaldoDiaHoyDevolucion decimal(18,2))
------------------------saldo dia anterior
insert into @tbl_Cierre
		SELECT 
		p.Id_Producto, p.Nombre,cc.SaldoDiaHoy, 0 as SaldoDiaHoyDevolucion,
		0,0,0,0,cc.SaldoDiaHoy,0 as SaldoDiaHoyDevolucion
		FROM   dbo.tbl_Producto p left JOIN dbo.tbl_CodigoCierreAlmacen cc ON p.Id_Producto = cc.Id_Producto 
		INNER JOIN dbo.tbl_CierreAlmacen c ON c.Id_CierreAlmacen= cc.Id_CierreAlmacen
		WHERE  (c.E_Eliminado = 0) 
		and p.e_eliminado=0 
		and dbo.dateonly(c.Fecha) =  
		dbo.dateonly((  select max(DBO.DATEONLY(cd.Fecha))  from dbo.tbl_CierreAlmacen cd
		WHERE DBO.DATEONLY(cd.Fecha)<>DBO.DATEONLY(@Fecha) and 
		DBO.DATEONLY(cd.Fecha)<DBO.DATEONLY(@Fecha) and cd.E_Eliminado=0  )) ORDER BY p.Observacion 
--select * from @tbl_Cierre
declare @auxiliar table (Id_Producto int, Cantidad int)

------------------------Compra Dia
------------------------Compra Dia
insert into @auxiliar 
		SELECT  dbo.tbl_Producto.Id_Producto,sum(cia.Cantidad)AS CompraDia
		FROM    dbo.tbl_IngresoAlmacen ia INNER JOIN 
		dbo.tbl_CodigoIngresoAlmacen cia ON ia.Id_IngresoAlmacen = cia.Id_IngresoAlmacen INNER JOIN 
		dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
		where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
		group by dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre,dbo.tbl_Producto.PrecioVenta

update @tbl_Cierre
set IngresoDia = a.Cantidad	
from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto
	
	
delete from @auxiliar 

------------------------Devolucion Salida

insert into @auxiliar 
		SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
		FROM    dbo.tbl_Devolucion ia INNER JOIN 
		dbo.tbl_CodigoDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
		dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
		where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1
		group by dbo.tbl_Producto.Id_Producto
		
update @tbl_Cierre
set DevolucionSalida = a.Cantidad	
from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

delete from @auxiliar 

------------------------Devolucion Ingreso

insert into @auxiliar 
		SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
		FROM    dbo.tbl_Devolucion ia INNER JOIN 
		dbo.tbl_CodigoDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
		dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
		where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion in (2,3)
		group by dbo.tbl_Producto.Id_Producto
		
update @tbl_Cierre
set DevolucionIngreso = a.Cantidad
from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

delete from @auxiliar 


------------------------Venta Dia Vendedores
------------------------Venta Dia Vendedores

insert into @auxiliar 
		SELECT  dbo.tbl_Producto.Id_Producto, isnull(SUM(dbo.tbl_CodigoAlmacenVendedor.Cantidad),0)Cantidad				
		FROM    dbo.tbl_CodigoAlmacenVendedor INNER JOIN  
		dbo.tbl_AlmacenVendedor ON dbo.tbl_CodigoAlmacenVendedor.Id_AlmacenVendedor = dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor INNER JOIN  
		dbo.tbl_Producto ON dbo.tbl_CodigoAlmacenVendedor.Id_Producto = dbo.tbl_Producto.Id_Producto  
		WHERE   (dbo.tbl_CodigoAlmacenVendedor.E_Eliminado = 0) AND (dbo.tbl_AlmacenVendedor.E_Eliminado = 0) AND   
		dbo.DateOnly(dbo.tbl_AlmacenVendedor.Fecha) = dbo.DateOnly(@Fecha)
		and tbl_Producto.e_eliminado=0
		GROUP BY dbo.tbl_Producto.Id_Producto 
		order by dbo.tbl_Producto.Id_Producto

		
update @tbl_Cierre
set SalidaDia = a.Cantidad	
from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto
delete from @auxiliar 
------------------------Devolucion Ingreso x OT

insert into @auxiliar 
		SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
		FROM    dbo.tbl_Devolucion ia INNER JOIN 
		dbo.tbl_CodigoDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
		dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
		where dbo.dateonly(ia.Fecha)=dbo.dateonly('28/12/2016') and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and ia.Estado = 0 and ia.id_TipoDevolucion = 2
		group by dbo.tbl_Producto.Id_Producto
		
update @tbl_Cierre
set SaldoDiaHoyDevolucion = SaldoDiaHoyDevolucion + a.Cantidad
from @tbl_Cierre c INNER JOIN @auxiliar a on c.id_Producto = a.id_producto

delete from @auxiliar 
------------------------Devolucion Ingreso x Excende Ruta

insert into @auxiliar 
		SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
		FROM    dbo.tbl_Devolucion ia INNER JOIN 
		dbo.tbl_CodigoDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
		dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
		where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion = 3
		group by dbo.tbl_Producto.Id_Producto
		
update @tbl_Cierre
set SaldoDiaHoy = SaldoDiaHoy + a.Cantidad
from @tbl_Cierre c INNER JOIN @auxiliar a on c.id_Producto = a.id_producto

delete from @auxiliar 

------------------------Devolucion Salida x OT

insert into @auxiliar 
		SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
		FROM    dbo.tbl_Devolucion ia INNER JOIN 
		dbo.tbl_CodigoDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
		dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
		where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and ia.Id_TipoDevolucion = 2
		group by dbo.tbl_Producto.Id_Producto
		
update @tbl_Cierre
set SaldoDiaHoyDevolucion = SaldoDiaHoyDevolucion - a.Cantidad
from @tbl_Cierre c INNER JOIN @auxiliar a on c.id_Producto = a.id_producto

delete from @auxiliar
------------------------Devolucion Salida x Danado

insert into @auxiliar 
		SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
		FROM    dbo.tbl_Devolucion ia INNER JOIN 
		dbo.tbl_CodigoDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
		dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
		where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and ia.Id_TipoDevolucion = 1
		group by dbo.tbl_Producto.Id_Producto

update @tbl_Cierre
set SaldoDiaHoy = SaldoDiaHoy - a.Cantidad	
from @tbl_Cierre c INNER JOIN @auxiliar a on a.id_Producto = c.id_producto

delete from @auxiliar
------------------------Saldo Dia
------------------------Saldo Dia

update @tbl_Cierre
set SaldoDiaHoy = SaldoDiaHoy - SalidaDia + IngresoDia

select * from @tbl_Cierre
	--select * from tbl_tipodevolucion
GO

/* [dbo].[sp_ObtenerCierreAlmacen_X_Fecha] */

create proc [dbo].[sp_ObtenerCierreAlmacen_X_Fecha](@Fecha datetime)
as
begin
	select (select nombre from tbl_producto where id_producto = cc.id_producto) producto, cc.*, 'SC' Sucursal from tbl_cierrealmacen c inner join tbl_codigocierrealmacen cc on c.id_cierrealmacen = cc.id_cierrealmacen where dbo.dateonly(fecha) = @Fecha and c.e_eliminado = 0
end

GO

/* [dbo].[sp_ObtenerCierreAlmacenDetalladasLocal] */
create proc [dbo].[sp_ObtenerCierreAlmacenDetalladasLocal]--(@id_tipoTransaccion int)
as

	select c.Id_CierreAlmacenlocal,c.Id_CierreAlmacen,c.Id_codigoCierreAlmacen,u.Id_usuario,u.Nombre Usuario,c.Fecha,c.observacion,
	c.Fecha_Registro,c.Id_Producto,p.nombre Producto,
	c.SaldoDiaAnterior,c.SaldoDiaAnteriorDevolucion,c.IngresoDia,c.DevolucionIngreso,c.DevolucionSalida,c.SalidaDia,c.SalidaBaja,
	c.SaldoDiaHoy,c.SaldoDiaHoyDevolucion,c.e_eliminado, 
    18 id_sucursal,(select upper( sucursal) from tbl_Sucursal where id_sucursal = 18 )Sucursal,  ---Cambiar Sucursal	
	c.Id_TipoTransaccion
	from tbl_CierreAlmacenLocal c
	inner join tbl_Usuario u on u.id_usuario =c.Id_usuario
	inner join  tbl_producto p on p.id_producto = c.id_producto 
	where  E_enviado = 0 
	and (SaldoDiaAnterior >0 or	SaldoDiaAnteriorDevolucion >0 or IngresoDia	>0 or DevolucionIngreso >0 or DevolucionSalida >0 or
		SalidaDia >0 or	SalidaBaja >0 or SaldoDiaHoy >0 or	SaldoDiaHoyDevolucion >0 )
	

GO

/* [dbo].[sp_ObtenerCierreAlmacenPR_PD] */
CREATE proc [dbo].[sp_ObtenerCierreAlmacenPR_PD](@Fecha datetime)
as 
Begin	
	declare @tbl_Cierre 
	table (
	Id_Producto int, 
	Nombre nvarchar(max),
	---PR productos retirados(OT) -- PD Productos dañados
	SaldoDiaAnteriorPR decimal(18,2),	
	--IngresoDiaPR decimal(18,2),--OK +
	IngresoDevolucionPR decimal(18,2),	--OK+
	SalidaBajaPR decimal(18,2),--OK-
	SalidaDevolucionTPR decimal(18,2),---
	SaldoDiaHoyPR decimal(18,2),
	
	SaldoDiaAnteriorPD decimal(18,2),	
	IngresoDevolucionPD decimal(18,2),--OK
	--DevolucionIngresoPD decimal(18,2),	--OK
	SalidaBajaPD decimal(18,2),--OK
	SalidaDevolucionTPD decimal(18,2),
	SaldoDiaHoyPD decimal(18,2))
	
	------------------------saldo dia anterior
	------------------------saldo dia anterior
	--es el saldo del dia anterior
	
	insert into @tbl_Cierre
			SELECT 
			p.Id_Producto, p.Nombre,isnull(cc.SaldoDiaHoyPR,0) SaldoDiaAnteriorPR, 
			0,0,0,0,
			isnull(cc.SaldoDiaHoyPD,0) SaldoDiaHoyPD, 
			0,0,0,0
			FROM   dbo.tbl_Producto p left JOIN dbo.tbl_CodigoCierreAlmacenPR_PD cc ON p.Id_Producto = cc.Id_Producto 
			AND cc.Id_CierreAlmacenPR_PD = (select ISNULL(MAX(Id_CierreAlmacenPR_PD),0)  from dbo.tbl_CierreAlmacenPR_PD cd
			WHERE DBO.DATEONLY(cd.Fecha)<DBO.DATEONLY(@Fecha) and cd.E_Eliminado=0 )
			WHERE  p.e_eliminado=0 
			ORDER BY p.Observacion 
	declare @auxiliar table (Id_Producto int, Cantidad int)
		
	-------------------------------------DevolucionIngresoPR
--	SELECT * FROM tbl_TipoDevolucion --2 Material Retirado OT
--SELECT * FROM tbl_Devolucion ORDER BY Id_Devolucion DESC
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion in (2)
			and cia.Entregado=1
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set IngresoDevolucionPR = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 

		
	------------------------DevolucionIngresoPD	
	--select * from tbl_TipoDevolucion--1 material dañado  2 material retirado   3 excedente ruta  4 devuelto tigo
	-- material dañado no importa si esta entregado PD
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion in (1)
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set IngresoDevolucionPD = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	--------SalidaBajaPR

	--select * from tbl_BajaProductos select * from tbl_EstadoProducto  --4 retirado ot 5 dañado
	--baja de material retirado
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_BajaProductos ia INNER JOIN 
			dbo.tbl_Codigobajaproductos cia ON ia.Id_bajaproductos= cia.Id_bajaproductos INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
			and ia.Id_EstadoProductos in (4)
			group by dbo.tbl_Producto.Id_Producto
	
	update @tbl_Cierre
	set SalidaBajaPR = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	--------SalidaBajaPD

	--select * from tbl_BajaProductos select * from tbl_EstadoProducto  --4 retirado ot 5 dañado
	--baja de material retirado
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_BajaProductos ia INNER JOIN 
			dbo.tbl_Codigobajaproductos cia ON ia.Id_bajaproductos= cia.Id_bajaproductos INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
			and ia.Id_EstadoProductos in (5)
			group by dbo.tbl_Producto.Id_Producto
	
	update @tbl_Cierre
	set SalidaBajaPD = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	---------------------SalidaDevolucionPR
	--SELECT * FROM tbl_Devolucion ORDER BY Id_Devolucion DESC--10237
	--SELECT * FROM tbl_Devolucion WHERE Id_Devolucion = 10237
	--SELECT * FROM tbl_DETALLEDevolucion WHERE Id_Devolucion = 10237
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and Id_TipoDevolucion=6
			group by dbo.tbl_Producto.Id_Producto

	update @tbl_Cierre
	set SalidaDevolucionTPR = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	--SELECT * FROM tbl_TipoDevolucion 
	---------------------SalidaDevolucionPD
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and Id_TipoDevolucion=7
			group by dbo.tbl_Producto.Id_Producto

	update @tbl_Cierre
	set SalidaDevolucionTPD = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 	
	
	--------------------SaldoDiaHoyPR		
	update @tbl_Cierre
	set SaldoDiaHoyPR = SaldoDiaAnteriorPR+IngresoDevolucionPR-SalidaBajaPR-SalidaDevolucionTPR 
	from @tbl_Cierre c

--------------------SaldoDiaHoyPD
	update @tbl_Cierre
	set SaldoDiaHoyPD = SaldoDiaAnteriorPD+IngresoDevolucionPD-SalidaBajaPD-SalidaDevolucionTPD 
	from @tbl_Cierre c

select * from @tbl_Cierre	
end

GO

/* [dbo].[sp_ObtenerCierreAlmacenPR_PD_PSaldos] */
CREATE proc [dbo].[sp_ObtenerCierreAlmacenPR_PD_PSaldos](@Fecha datetime)
as 
Begin	
	declare @tbl_Cierre 
	table (
	Id_Producto int, 
	Nombre nvarchar(max),
	---PR productos retirados(OT) -- PD Productos dañados
	SaldoDiaAnteriorPR decimal(18,2),	
	IngresoDiaPR decimal(18,2),--OK +
	DevolucionIngresoPR decimal(18,2),	--OK+
	SalidaBajaPR decimal(18,2),--OK-
	SalidaDevolucionTPR decimal(18,2),---
	SaldoDiaHoyPR decimal(18,2),
	
	SaldoDiaAnteriorPD decimal(18,2),	
	IngresoDiaPD decimal(18,2),--OK
	DevolucionIngresoPD decimal(18,2),	--OK
	SalidaBajaPD decimal(18,2),--OK
	SalidaDevolucionTPD decimal(18,2),
	SaldoDiaHoyPD decimal(18,2))
	
	------------------------saldo dia anterior
	------------------------saldo dia anterior
	--es el saldo del dia anterior
	
	insert into @tbl_Cierre
			SELECT 
			p.Id_Producto, p.Nombre,isnull(cc.SaldoDiaHoyPR,0) SaldoDiaAnteriorPR, 
			0,0,0,0,0,
			isnull(cc.SaldoDiaHoyPD,0) SaldoDiaHoyPD, 
			0,0,0,0,0
			FROM   dbo.tbl_Producto p left JOIN dbo.tbl_CodigoCierreAlmacenPR_PD cc ON p.Id_Producto = cc.Id_Producto 
			AND cc.Id_CierreAlmacenPR_PD = (select ISNULL(MAX(Id_CierreAlmacenPR_PD),0)  from dbo.tbl_CierreAlmacenPR_PD cd
			WHERE DBO.DATEONLY(cd.Fecha)<DBO.DATEONLY(@Fecha) and cd.E_Eliminado=0 )
			WHERE  p.e_eliminado=0 
			ORDER BY p.Observacion 
	declare @auxiliar table (Id_Producto int, Cantidad int)
		
	-------------------------------------DevolucionIngresoPR
--	SELECT * FROM tbl_TipoDevolucion --2 Material Retirado OT
--SELECT * FROM tbl_Devolucion ORDER BY Id_Devolucion DESC
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion in (2)
			and cia.Entregado=1
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set DevolucionIngresoPR = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 

		
	------------------------DevolucionIngresoPD	
	--select * from tbl_TipoDevolucion--1 material dañado  2 material retirado   3 excedente ruta  4 devuelto tigo
	-- material dañado no importa si esta entregado PD
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion in (1)
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set DevolucionIngresoPD = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	--------SalidaBajaPR

	--select * from tbl_BajaProductos select * from tbl_EstadoProducto  --4 retirado ot 5 dañado
	--baja de material retirado
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_BajaProductos ia INNER JOIN 
			dbo.tbl_Codigobajaproductos cia ON ia.Id_bajaproductos= cia.Id_bajaproductos INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
			and ia.Id_EstadoProductos in (4)
			group by dbo.tbl_Producto.Id_Producto
	
	update @tbl_Cierre
	set SalidaBajaPR = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	--------SalidaBajaPD

	--select * from tbl_BajaProductos select * from tbl_EstadoProducto  --4 retirado ot 5 dañado
	--baja de material retirado
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_BajaProductos ia INNER JOIN 
			dbo.tbl_Codigobajaproductos cia ON ia.Id_bajaproductos= cia.Id_bajaproductos INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
			and ia.Id_EstadoProductos in (5)
			group by dbo.tbl_Producto.Id_Producto
	
	update @tbl_Cierre
	set SalidaBajaPD = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	---------------------SalidaDevolucionPR
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and Id_TipoDevolucion=6
			group by dbo.tbl_Producto.Id_Producto
			
			/*	SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly('11/12/2019') and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and Id_TipoDevolucion=6
			group by dbo.tbl_Producto.Id_Producto*/

	update @tbl_Cierre
	set SalidaDevolucionTPR = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	---------------------SalidaDevolucionPD
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and Id_TipoDevolucion=7
			group by dbo.tbl_Producto.Id_Producto

	update @tbl_Cierre
	set SalidaDevolucionTPD = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 	
	
	--------------------SaldoDiaHoyPR		
	update @tbl_Cierre
	set SaldoDiaHoyPR = SaldoDiaAnteriorPR+IngresoDiaPR+DevolucionIngresoPR-SalidaBajaPR-SalidaDevolucionTPR 
	from @tbl_Cierre c

--------------------SaldoDiaHoyPD
	update @tbl_Cierre
	set SaldoDiaHoyPD = SaldoDiaAnteriorPD+IngresoDiaPD+DevolucionIngresoPD-SalidaBajaPD-SalidaDevolucionTPD 
	from @tbl_Cierre c

select * from @tbl_Cierre	
end


GO

/* [dbo].[sp_ObtenerCierreAlmacenPR_PDLocal] */
create  proc [dbo].[sp_ObtenerCierreAlmacenPR_PDLocal]--(@id_tipoTransaccion int)
as

	select c.Id_CierreAlmacenPR_PDlocal ,c.Id_CierreAlmacenPR_PD , c.Id_CodigoCierreAlmacenPR_PD,c.Id_usuario,u.Nombre Usuario,
	c.Fecha,c.Fecha_Registro,c.observacion
    ,c.Id_Producto,p.nombre Producto,
	c.SaldoDiaAnteriorPR,c.IngresoDevolucionPR,c.SalidaBajaPR,c.SalidaDevolucionTPR,
    c.SaldoDiaHoyPR,c.SaldoDiaAnteriorPD,c.IngresoDevolucionPD,c.SalidaBajaPD,c.SalidaDevolucionTPD,
    c.SaldoDiaHoyPD,	
    18 id_sucursal,(select upper( sucursal) from tbl_Sucursal where id_sucursal = 18 )Sucursal,
	c.e_eliminado, 
	c.Id_TipoTransaccion
	from tbl_CierreAlmacenPR_PDLocal c
	inner join tbl_Usuario u on u.id_usuario =c.Id_Usuario
	inner join  tbl_producto p on p.id_producto = c.id_producto 		
	where  E_enviado = 0 
	and  (c.SaldoDiaAnteriorPR>0 or c.IngresoDevolucionPR>0 or c.SalidaBajaPR>0 or c.SalidaDevolucionTPR>0 
	or c.SaldoDiaHoyPR>0 or c.SaldoDiaAnteriorPD>0 or c.IngresoDevolucionPD >0 or c.SalidaBajaPD > 0 or c.SalidaDevolucionTPD>0 or c.SaldoDiaHoyPD >0)

GO

/* [dbo].[sp_ObtenerCodigoDevolucion] */

CREATE proc [dbo].[sp_ObtenerCodigoDevolucion] (@Id_Devolucion int)
as
select cd.Id_Devolucion, Id_detalleDevolucion Id_CodigoDevolucion,cd.Id_Producto,p.Nombre,cd.Cod_Inicio,cd.ChipID,
cd.Cantidad,
case when Entregado = 1 then 'Entregado' else 'NoEntregado' end Estado,
case when PendienteRecojo = 1 then 'Si' else 'No' end PendienteRecojo
from  tbl_detalleDevolucion  cd, tbl_Producto p
where cd.Id_Producto = p.Id_Producto
and cd.E_Eliminado=0 and cd.Id_Devolucion =@Id_Devolucion
order by p.Nombre, DigitosImei--, p.Nombre

select cd.*,pr.Nombre Producto
from tbl_CodigoDevolucion cd inner join tbl_producto pr on pr.Id_Producto = cd.Id_Producto
where Id_Devolucion =@Id_Devolucion 
and cd.E_Eliminado=0
order by pr.Nombre, DigitosImei



GO

/* [dbo].[sp_ObtenerCuadre_X_Fecha] */
create proc [dbo].[sp_ObtenerCuadre_X_Fecha](@Fecha datetime)
as
begin
	select (select nombre from tbl_ruta where id_ruta = c.id_ruta)Ruta, c.*, cc.*, p.nombre, 'SC' Sucursal from tbl_cuadre c inner join tbl_codigocuadre cc on c.id_cuadre = cc.id_cuadre inner join tbl_producto p on cc.id_producto = p.id_producto
	where dbo.dateonly(fecha) = @Fecha and c.e_eliminado = 0
end
GO

/* [dbo].[sp_ObtenerCuadreLocal] */
create  proc [dbo].[sp_ObtenerCuadreLocal]--(@id_tipoTransaccion int)
as

	select c.Id_cuadreLocal,c.Id_Cuadre,c.id_Codigocuadre,c.Id_Ruta,r.nombre Ruta,
	c.Id_Vendedor,v.nombre, c.Id_usuario,u.Nombre Usuario,c.Fecha,c.Fecha_Registro,c.observacion,c.Total
	,c.Id_Producto,p.nombre Producto,
	c.ItemsSobrantes,c.ItemsVendidos,c.precio,c.TotalVendidos,c.ItemsRetirados	,
	18 id_sucursal,(select upper( sucursal) from tbl_Sucursal where id_sucursal = 18 )Sucursal,  ---Cambiar Sucursal	
	c.e_eliminado, 
	c.Id_TipoTransaccion
	from tbl_CuadresLocal c
	inner join tbl_Usuario u on u.id_usuario =c.Id_Usuario
	inner join  tbl_producto p on p.id_producto = c.id_producto 
	inner join tbl_ruta r on r.id_ruta =c.Id_Ruta
	inner join tbl_vendedor v on v.id_vendedor = c.Id_Vendedor	
	where  E_enviado = 0 
	and  (c.ItemsSobrantes>0 or ItemsVendidos>0 or TotalVendidos>0 or ItemsRetirados>0)

GO

/* [dbo].[sp_ObtenerDatosCliente] */
CREATE PROC sp_ObtenerDatosCliente(@CodigoCliente int)
as
SELECT  c.Id_Cliente, c.CodigoCliente,c.Nombre Cliente, ca.Direccion,ca.Referencia_Adicional,ca.Zona,ca.Tipo_Casa,ca.Color ,ca.Servicio,ca.PosicionGeografica,
ca.CodigoCRE,ca.Repetidora,ca.Recepcion
FROM tbl_Cliente c, tbl_Casa ca
where c.Id_Cliente=ca.Id_Cliente and c.E_Eliminado=0
and c.CodigoCliente = @CodigoCliente



GO

/* [dbo].[sp_ObtenerDecodificadores] */

create proc [dbo].[sp_ObtenerDecodificadores]
as
select * from tbl_Producto where (Prefijo like '%DEC%' )
and E_Eliminado = 0


GO

/* [dbo].[sp_ObtenerDecodificadoresPVSeriales] */

CREATE proc [dbo].[sp_ObtenerDecodificadoresPVSeriales]
as

select * from tbl_Producto where id_tipoproducto=1
and E_Eliminado = 0

GO

/* [dbo].[sp_ObtenerDetalleAlmacenVendedor_X_ID] */
CREATE proc [dbo].[sp_ObtenerDetalleAlmacenVendedor_X_ID] (@id int)
as
SELECT     dbo.tbl_CodigoAlmacenVendedor.Id_CodigoAlmacenVendedor Id_CodigoAlmacenCliente, dbo.tbl_Producto.Id_Producto, 
dbo.tbl_Producto.Nombre, dbo.tbl_Producto.Medida,  dbo.tbl_CodigoAlmacenVendedor.Cod_Inicio, dbo.tbl_CodigoAlmacenVendedor.ChipID,
 dbo.tbl_CodigoAlmacenVendedor.Cantidad
FROM         dbo.tbl_CodigoAlmacenVendedor INNER JOIN
dbo.tbl_AlmacenVendedor ON dbo.tbl_CodigoAlmacenVendedor.Id_AlmacenVendedor = dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor INNER JOIN
dbo.tbl_Producto ON dbo.tbl_CodigoAlmacenVendedor.Id_Producto = dbo.tbl_Producto.Id_Producto
where dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor = @id 

GO

/* [dbo].[sp_ObtenerDetalleAlmacenVendedor_X_IDPedidoVendedor] */
create proc [dbo].[sp_ObtenerDetalleAlmacenVendedor_X_IDPedidoVendedor] (@id int)
as
declare @Id_Almacenvendedor int
set @Id_Almacenvendedor  = (select id_almacenvendedor from tbl_AlmacenVendedor where Id_PedidoVendedor =@id)

SELECT     dbo.tbl_CodigoAlmacenVendedor.Id_CodigoAlmacenVendedor Id_CodigoAlmacenCliente, dbo.tbl_Producto.Id_Producto, 
dbo.tbl_Producto.Nombre, dbo.tbl_Producto.Medida,  dbo.tbl_CodigoAlmacenVendedor.Cod_Inicio, dbo.tbl_CodigoAlmacenVendedor.ChipID,
 dbo.tbl_CodigoAlmacenVendedor.Cantidad
FROM         dbo.tbl_CodigoAlmacenVendedor INNER JOIN
dbo.tbl_AlmacenVendedor ON dbo.tbl_CodigoAlmacenVendedor.Id_AlmacenVendedor = dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor INNER JOIN
dbo.tbl_Producto ON dbo.tbl_CodigoAlmacenVendedor.Id_Producto = dbo.tbl_Producto.Id_Producto
where dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor = @Id_Almacenvendedor 
order by dbo.tbl_Producto.Nombre

GO

/* [dbo].[sp_ObtenerDetalleCuadre] */

CREATE proc [dbo].[sp_ObtenerDetalleCuadre] (@id_Cuadre int)
as
begin
	SELECT     dbo.tbl_Cuadre.Id_Cuadre, dbo.tbl_CodigoCuadre.Id_CodigoCuadre, dbo.tbl_Producto.Id_Producto, dbo.tbl_Producto.Nombre,  
	dbo.tbl_CodigoCuadre.Precio,dbo.tbl_CodigoCuadre.ItemsSobrantes, dbo.tbl_CodigoCuadre.ItemsVendidos, dbo.tbl_CodigoCuadre.ItemsRetirados,
	dbo.tbl_CodigoCuadre.TotalVendidos 
	FROM         dbo.tbl_Cuadre INNER JOIN  
	dbo.tbl_CodigoCuadre ON dbo.tbl_Cuadre.Id_Cuadre = dbo.tbl_CodigoCuadre.Id_Cuadre AND   
	dbo.tbl_Cuadre.Id_Cuadre = dbo.tbl_CodigoCuadre.Id_Cuadre INNER JOIN  
	dbo.tbl_Producto ON dbo.tbl_CodigoCuadre.Id_Producto = dbo.tbl_Producto.Id_Producto  
	WHERE  dbo.tbl_Cuadre.Id_Cuadre = @id_Cuadre
	and  (itemssobrantes + itemsvendidos + itemsRetirados <> 0 )
	order by dbo.tbl_Producto.nombre
end

GO

/* [dbo].[sp_ObtenerDetalleEntregaAlmacenOtros_X_ID] */
CREATE proc sp_ObtenerDetalleEntregaAlmacenOtros_X_ID (@Id int)
as 
	select c.Id_CodigoEntregaAlmacenOtros,c.Id_EntregaAlmacenOtros, c.Id_Producto,pr.Nombre,pr.Medida,
	c.Imei,c.Cantidad
	from tbl_CodigoEntregaAlmacenOtros c, tbl_Producto pr
	where c.E_Eliminado=0
	and pr.Id_Producto = c.Id_Producto
	and Id_EntregaAlmacenOtros = @Id
	


GO

/* [dbo].[sp_ObtenerDetalleIngresoAlmacen] */

CREATE proc [dbo].[sp_ObtenerDetalleIngresoAlmacen](@Id_IngresoAlmacen int)
as
SELECT     dbo.tbl_CodigoIngresoAlmacen.Id_CodigoIngresoAlmacen, dbo.tbl_Producto.Id_Producto, dbo.tbl_Producto.Nombre, dbo.tbl_Producto.Medida,
dbo.tbl_CodigoIngresoAlmacen.Cod_Inicio,Chip_Id, dbo.tbl_CodigoIngresoAlmacen.Cantidad,  
 dbo.tbl_IngresoAlmacen.Id_IngresoAlmacen 
FROM         dbo.tbl_CodigoIngresoAlmacen INNER JOIN 
dbo.tbl_Producto ON dbo.tbl_CodigoIngresoAlmacen.Id_Producto = dbo.tbl_Producto.Id_Producto INNER JOIN 
dbo.tbl_IngresoAlmacen ON dbo.tbl_IngresoAlmacen.Id_IngresoAlmacen = dbo.tbl_CodigoIngresoAlmacen.Id_IngresoAlmacen
WHERE dbo.tbl_IngresoAlmacen.Id_IngresoAlmacen = @Id_IngresoAlmacen 
and dbo.tbl_CodigoIngresoAlmacen.E_Eliminado=0 and dbo.tbl_IngresoAlmacen.E_Eliminado=0
order by dbo.tbl_Producto.observacion
GO

/* [dbo].[sp_ObtenerDetallePedidoVendedor_X_ID] */




CREATE proc [dbo].[sp_ObtenerDetallePedidoVendedor_X_ID] (@id int)
as
SELECT     dbo.tbl_CodigoPedidoVendedor.Id_CodigoPedidoVendedor, dbo.tbl_PedidoVendedor.Id_PedidoVendedor, dbo.tbl_Producto.Id_Producto, dbo.tbl_Producto.Nombre, 
dbo.tbl_CodigoPedidoVendedor.Saldo, dbo.tbl_CodigoPedidoVendedor.NvoPedido, dbo.tbl_CodigoPedidoVendedor.ItemsHoy,
dbo.tbl_CodigoPedidoVendedor.E_Eliminado
FROM         dbo.tbl_CodigoPedidoVendedor INNER JOIN
dbo.tbl_PedidoVendedor ON dbo.tbl_CodigoPedidoVendedor.Id_PedidoVendedor = dbo.tbl_PedidoVendedor.Id_PedidoVendedor INNER JOIN
dbo.tbl_Producto ON dbo.tbl_CodigoPedidoVendedor.Id_Producto = dbo.tbl_Producto.Id_Producto
where dbo.tbl_PedidoVendedor.Id_PedidoVendedor=@id and  
dbo.tbl_CodigoPedidoVendedor.ItemsHoy <> 0  and  dbo.tbl_CodigoPedidoVendedor.E_Eliminado=0
order by dbo.tbl_Producto.nombre 


GO

/* [dbo].[sp_ObtenerDetalleVenta_CargoUsuario_X_ID] */

CREATE proc [dbo].[sp_ObtenerDetalleVenta_CargoUsuario_X_ID] (@id int)
as
SELECT   cvu.Id,p.Id_Producto,p.Nombre, p.Medida,cvu.Serial Cod_Inicio,cvu.ChipId,cvu.Cantidad
FROM         dbo.tbl_Venta v INNER JOIN
dbo.tbl_codigoventacargousuario cvu ON v.Id_Venta = cvu.Id_Venta
 INNER JOIN dbo.tbl_Producto p ON cvu.Id_Producto = p.Id_Producto
where v.Id_Venta = @id  
and  cvu.E_Eliminado=0
order by p.nombre


GO

/* [dbo].[sp_ObtenerDetalleVenta_Excedente_X_ID] */

CREATE proc [dbo].[sp_ObtenerDetalleVenta_Excedente_X_ID] (@id int)
as
SELECT     dbo.tbl_CodigoVenta.Id_Venta Id_CodigoAlmacenCliente, dbo.tbl_Producto.Id_Producto, 
dbo.tbl_Producto.Nombre, dbo.tbl_Producto.Medida,  dbo.tbl_CodigoVenta.Cod_Inicio, dbo.tbl_CodigoVenta.ChipID,
 dbo.tbl_CodigoVenta.Cantidad
FROM         dbo.tbl_Venta INNER JOIN
dbo.tbl_CodigoVenta ON dbo.tbl_Venta.Id_Venta = dbo.tbl_CodigoVenta.Id_Venta
 INNER JOIN dbo.tbl_Producto ON dbo.tbl_CodigoVenta.Id_Producto = dbo.tbl_Producto.Id_Producto
where dbo.tbl_Venta.Id_Venta = @id  and  tbl_CodigoVenta.Id_TipoMaterial = 3


GO

/* [dbo].[sp_ObtenerDetalleVenta_Instalado_X_ID] */

CREATE proc [dbo].[sp_ObtenerDetalleVenta_Instalado_X_ID] (@id int)
as
SELECT     dbo.tbl_CodigoVenta.Id_Venta Id_CodigoAlmacenCliente, dbo.tbl_Producto.Id_Producto, 
dbo.tbl_Producto.Nombre, dbo.tbl_Producto.Medida,  dbo.tbl_CodigoVenta.Cod_Inicio, dbo.tbl_CodigoVenta.ChipID,
 dbo.tbl_CodigoVenta.Cantidad
FROM         dbo.tbl_Venta INNER JOIN
dbo.tbl_CodigoVenta ON dbo.tbl_Venta.Id_Venta = dbo.tbl_CodigoVenta.Id_Venta
 INNER JOIN dbo.tbl_Producto ON dbo.tbl_CodigoVenta.Id_Producto = dbo.tbl_Producto.Id_Producto
where dbo.tbl_Venta.Id_Venta = @id  and  tbl_CodigoVenta.Id_TipoMaterial = 1
and  dbo.tbl_CodigoVenta.E_Eliminado=0
order by dbo.tbl_Producto.nombre


GO

/* [dbo].[sp_ObtenerDetalleVenta_Retirado_X_ID] */
CREATE proc [dbo].[sp_ObtenerDetalleVenta_Retirado_X_ID] (@id int)
as
SELECT     dbo.tbl_CodigoVenta.Id_Venta Id_CodigoAlmacenCliente, dbo.tbl_Producto.Id_Producto, 
dbo.tbl_Producto.Nombre, dbo.tbl_Producto.Medida,  dbo.tbl_CodigoVenta.Cod_Inicio, dbo.tbl_CodigoVenta.ChipID,
 dbo.tbl_CodigoVenta.Cantidad, tm.Nombre TipoMaterial
FROM         dbo.tbl_Venta INNER JOIN dbo.tbl_CodigoVenta ON dbo.tbl_Venta.Id_Venta = dbo.tbl_CodigoVenta.Id_Venta
INNER JOIN dbo.tbl_Producto ON dbo.tbl_CodigoVenta.Id_Producto = dbo.tbl_Producto.Id_Producto
inner join tbl_tipomaterial tm on tm.id_tipomaterial = dbo.tbl_CodigoVenta.id_tipomaterial 
where dbo.tbl_Venta.Id_Venta = @id  and  tbl_CodigoVenta.Id_TipoMaterial in (2,5)
and  dbo.tbl_CodigoVenta.E_Eliminado=0
order by  tbl_CodigoVenta.Id_TipoMaterial desc, dbo.tbl_Producto.nombre
GO

/* [dbo].[sp_ObtenerDevolucion] */
CREATE proc [dbo].[sp_ObtenerDevolucion] (@Codigo int)
as
select i.Id_Devolucion,(isnull((select Nombre from tbl_Ruta where Id_Ruta= i.id_ruta),0))Proveedor,
(td.Nombre +' - '+  i.Observacion) Observacion, 
NroOrdenTrabajo,
case when i.Estado = 0 then 'Recepcionado en Almacen' else 'Entregado a Tigo' end Estado,
i.Fecha ,i.Id_Usuario,u.Nombre,'Devolucion'Detalle
from tbl_Devolucion i, tbl_Usuario u, tbl_TipoDevolucion td 
where i.Id_Usuario=u.Id_Usuario 
and i.Id_TipoDevolucion = td.Id_TipoDevolucion
and i.E_Eliminado=0 
and i.Id_Devolucion = @codigo
GO

/* [dbo].[sp_ObtenerDevolucionLocal] */
create  proc [dbo].[sp_ObtenerDevolucionLocal]--(@id_tipoTransaccion int)
as

	select c.Id_DevolucionLocal  ,c.Id_Devolucion  , c.Id_DetalleDevolucion ,c.Id_usuario,u.Nombre Usuario,	
	c.Id_Ruta , r.Nombre Ruta,
	c.Id_Vendedor , v.Nombre Vendedor,
	c.Id_TipoDevolucion  , tp.Nombre TipoDevolucion ,
	c.Id_Venta,
	c.Fecha,c.observacion,Estado,c.Fecha_Registro, c.NroOrdenTrabajo,
	c.Id_Producto,p.nombre Producto,	
	c.Cod_Inicio ,c.ChipID, c.Cantidad,c.Entregado, c.PendienteRecojo, 	
    18 id_sucursal,(select upper( sucursal) from tbl_Sucursal where id_sucursal = 18 )Sucursal,	
	c.e_eliminado, 
	c.Id_TipoTransaccion
	from tbl_DevolucionLocal c
	inner join tbl_Usuario u on u.id_usuario =c.Id_Usuario
	inner join  tbl_producto p on p.id_producto = c.id_producto 
	inner join tbl_tipodevolucion tp  on  tp.id_TipoDevolucion = c.Id_TipoDevolucion 		
	left join  Tbl_Vendedor v on c.id_vendedor = v.id_vendedor
    left join  Tbl_Ruta r on r.id_ruta = c.id_ruta	 		
	where  E_enviado = 0 	
	

GO

/* [dbo].[sp_ObtenerEntregaAlmacenOtros_X_ID] */
CREATE proc sp_ObtenerEntregaAlmacenOtros_X_ID (@Id int)
as 
	select e.*,u.Nombre Usuario from tbl_EntregaAlmacenOtros e, tbl_Usuario u
	where e.E_Eliminado=0 and Id_EntregaAlmacenOtros = @Id
	and e.Id_Usuario = u.Id_Usuario
GO

/* [dbo].[sp_ObtenerEstado] */
create proc sp_ObtenerEstado
as 
	select * from tbl_estado where e_eliminado=0
GO

/* [dbo].[sp_ObtenerIdTipoServicioVerificar] */
create proc sp_ObtenerIdTipoServicioVerificar(@Id_TipoServicio int)
as
	if(@Id_TipoServicio = 1 )
		select * from tbl_TipoServicio where Id_TipoServicio in (1,2,3)
GO

/* [dbo].[sp_ObtenerIngresoAlmacen] */

CREATE proc [dbo].[sp_ObtenerIngresoAlmacen] (@Id_IngresoAlmacen int)
as
SELECT     i.Id_IngresoAlmacen, dbo.tbl_Usuario.Nombre, i.Proveedor, i.Fecha, i.Fecha_Registro, i.Observacion, i.NroRecibo 
FROM         dbo.tbl_IngresoAlmacen i INNER JOIN 
dbo.tbl_Usuario ON i.Id_Usuario = dbo.tbl_Usuario.Id_Usuario
WHERE i.Id_IngresoAlmacen = @Id_IngresoAlmacen
and i.E_Eliminado=0

GO

/* [dbo].[sp_ObtenerIngresoAlmacenBol] */
CREATE proc [dbo].[sp_ObtenerIngresoAlmacenBol](@bol nvarchar(25))
as
select i.Id_IngresoAlmacen,i.Proveedor, dbo.DateOnly(i.Fecha) as Fecha,i.Observacion,i.NroRecibo,p.Nombre as Producto,
ci.Id_Producto
from tbl_IngresoAlmacen i, tbl_CodigoIngresoAlmacen ci, tbl_Usuario u, tbl_Producto p
where i.Id_IngresoAlmacen=ci.Id_IngresoAlmacen
and u.Id_Usuario=i.Id_Usuario
and p.Id_Producto=ci.Id_Producto
and i.E_Eliminado=0 and ci.E_Eliminado=0
and ci.Cod_Inicio=@bol
GO

/* [dbo].[sp_ObtenerIngresoAlmacenLocal] */
create proc [dbo].[sp_ObtenerIngresoAlmacenLocal]--(@id_tipoTransaccion int)
as

	select c.Id_IngresoAlmacenLocal,c.Id_IngresoAlmacen, c.Id_CodigoIngresoAlmacen,c.Id_usuario,u.Nombre Usuario,c.Proveedor,c.Total,c.NroRecibo
	,c.Fecha,c.Fecha_Registro,c.observacion
    ,c.Id_Producto,p.nombre Producto,
	c.cod_inicio,c.ChipId,c.cantidad,
	18 id_sucursal,(select upper( sucursal) from tbl_Sucursal where id_sucursal = 18 )Sucursal,	
	c.e_eliminado, 
	c.Id_TipoTransaccion
	from tbl_IngresoalmacenLocal c
	inner join tbl_Usuario u on u.id_usuario =c.Id_Usuario
	inner join  tbl_producto p on p.id_producto = c.id_producto 		
	where  E_enviado = 0 
	--and  (c.ItemsSobrantes>0 or ItemsVendidos>0 or TotalVendidos>0 or ItemsRetirados>0)

GO

/* [dbo].[sp_ObtenerKitDecodificadores] */
CREATE proc [dbo].[sp_ObtenerKitDecodificadores]
as
select * from tbl_Producto where (Prefijo like '%DEC%' )
and E_Eliminado = 0

GO

/* [dbo].[sp_ObtenerListaDevolucion] */
CREATE proc [dbo].[sp_ObtenerListaDevolucion](@Fecha datetime)
as
begin 

	select * from (
		select i.Id_Devolucion,(isnull((select Nombre from tbl_Ruta where Id_Ruta= i.id_ruta),0))Proveedor,
		i.Observacion, NroOrdenTrabajo,
		case when i.Estado = 0 then 'Recepcionado en Almacen' else 'Entregado a Tigo' end Estado,
		i.Fecha ,i.Id_Usuario,u.Nombre,'Devolucion'Detalle,i.Id_TipoDevolucion,td.Nombre TipoDevolucion
		from tbl_Devolucion i, tbl_Usuario u, tbl_TipoDevolucion td
		where i.Id_Usuario=u.Id_Usuario 
		and td.Id_TipoDevolucion = i.Id_TipoDevolucion
		and i.E_Eliminado=0 
		and dbo.DateOnly(i.Fecha) = dbo.DateOnly(@Fecha) 
		
		
	union all
		
		select i.Id_DevolucionTigoPendiente Id_Devolucion,(isnull((select Nombre from tbl_Ruta where Id_Ruta= 0),0))Proveedor,
		i.Observacion, '' NroOrdenTrabajo,
		Estado,	
		i.FechaPendiente Fecha ,i.Id_UsuarioPendiente Id_Usuario,u.Nombre,'Devolucion SCS'Detalle,i.Id_TipoDevolucion,td.Nombre TipoDevolucion
		from tbl_DevolucionTigoPendiente i, tbl_Usuario u, tbl_TipoDevolucion td
		where i.Id_UsuarioPendiente=u.Id_Usuario 
		and td.Id_TipoDevolucion = i.Id_TipoDevolucion
		and i.E_Eliminado=0 	
		and i.Id_Devolucion =0
		
		) a order by estado,Id_Devolucion DESC  
end
GO

/* [dbo].[sp_ObtenerListaDevolucion_RangoFecha] */
CREATE proc [dbo].[sp_ObtenerListaDevolucion_RangoFecha](@FechaInicio datetime,@FechaFin datetime)
as
begin
	select * from (
		select i.Id_Devolucion,(isnull((select Nombre from tbl_Ruta where Id_Ruta= i.id_ruta),0))Proveedor,
		i.Observacion, NroOrdenTrabajo,
		case when i.Estado = 0 then 'Recepcionado en Almacen - '+(isnull((select Nombre from tbl_Ruta where Id_Ruta= i.id_ruta),0)) 
		else 'Entregado a Tigo' end Estado,
		i.Fecha ,i.Id_Usuario,u.Nombre,'Devolucion'Detalle,i.Id_TipoDevolucion,td.Nombre TipoDevolucion,
		i.nombrearchivo
		from tbl_Devolucion i, tbl_Usuario u, tbl_TipoDevolucion td
		where i.Id_Usuario=u.Id_Usuario 
		and td.Id_TipoDevolucion = i.Id_TipoDevolucion
		and i.E_Eliminado=0 
		and dbo.DateOnly(i.Fecha) between dbo.DateOnly(@FechaInicio)  and dbo.DateOnly(@FechaFin)
		
		union all
		
		
		select i.Id_DevolucionTigoPendiente Id_Devolucion,(isnull((select Nombre from tbl_Ruta where Id_Ruta=0),0))Proveedor,
		i.Observacion, '' NroOrdenTrabajo,
		Estado,	
		i.FechaPendiente Fecha  ,i.Id_UsuarioPendiente Id_Usuario,u.Nombre,'Devolucion SCS'Detalle,i.Id_TipoDevolucion,td.Nombre TipoDevolucion,
		''nombrearchivo
		from tbl_DevolucionTigoPendiente i, tbl_Usuario u, tbl_TipoDevolucion td
		where i.Id_UsuarioPendiente=u.Id_Usuario 
		and td.Id_TipoDevolucion = i.Id_TipoDevolucion
		and i.E_Eliminado=0 
		and dbo.DateOnly(i.FechaPendiente) between dbo.DateOnly(@FechaInicio)  and dbo.DateOnly(@FechaFin)
	) a order by Fecha desc
end
GO

/* [dbo].[sp_ObtenerListadoSucursal] */
CREATE proc [dbo].[sp_ObtenerListadoSucursal]
as
select * from tbl_sucursal where e_eliminado=0 order by Sucursal
GO

/* [dbo].[sp_ObtenerListaIngresoAlmacen] */
CREATE proc [dbo].[sp_ObtenerListaIngresoAlmacen](@Fecha datetime)
as
select i.Id_IngresoAlmacen,i.Proveedor, i.Fecha ,i.Observacion,i.NroRecibo,i.Id_Usuario,u.Nombre
from tbl_IngresoAlmacen i, tbl_Usuario u
where i.Id_Usuario=u.Id_Usuario 
and dbo.DateOnly(i.Fecha)=dbo.DateOnly(@Fecha)

GO

/* [dbo].[sp_ObtenerListaIngresoAlmacen_Devolucion] */
create proc [dbo].[sp_ObtenerListaIngresoAlmacen_Devolucion](@Fecha datetime)
as
select i.Id_IngresoAlmacen,i.Proveedor, i.Fecha ,i.Observacion,i.NroRecibo,i.Id_Usuario,u.Nombre
from tbl_IngresoAlmacen i, tbl_Usuario u
where i.Id_Usuario=u.Id_Usuario 
and dbo.DateOnly(i.Fecha)=dbo.DateOnly(@Fecha)
GO

/* [dbo].[sp_ObtenerListaIngresoAlmacen_RangoFecha] */

CREATE proc [dbo].[sp_ObtenerListaIngresoAlmacen_RangoFecha](@FechaInicio datetime, @FechaFin datetime)
as
select i.Id_IngresoAlmacen,i.Proveedor,i.Observacion ,i.NroRecibo,'' EntregadoTigo, i.Fecha,i.Id_Usuario,u.Nombre,1 Tipo,'Ingreso Almacen'Detalle
from tbl_IngresoAlmacen i, tbl_Usuario u
where i.Id_Usuario=u.Id_Usuario 
and dbo.DateOnly(i.Fecha) between dbo.DateOnly(@FechaInicio) and dbo.DateOnly(@FechaFin)
and i.E_Eliminado=0
order by i.Id_IngresoAlmacen desc
GO

/* [dbo].[sp_ObtenerListaIngresoAlmacenMET_RangoFecha] */
create proc [dbo].[sp_ObtenerListaIngresoAlmacenMET_RangoFecha](@FechaInicio datetime, @FechaFin datetime)
as
select i.*
from tbl_IngresoMaterialTigo i, tbl_Usuario u
where i.Id_Usuario=u.Id_Usuario 
and dbo.DateOnly(i.FechaEntregaTigo) between dbo.DateOnly(@FechaInicio) and dbo.DateOnly(@FechaFin)
and i.E_Eliminado=0
order by i.Id_IngresoMaterialTigo desc
GO

/* [dbo].[sp_ObtenerListaOrdenesTrabajo] */
CREATE PROCEDURE dbo.sp_ObtenerListaOrdenesTrabajo(@Fecha_Registro datetime)
as
Begin
	Select
	R.Nombre as Ruta, V.Id_Venta, dbo.dateonly(V.Fecha_Ejecucion) Fecha_Ejecucion, dbo.dateonly(V.Fecha_Registro) Fecha_Registro, 
	V.OrdenTrabajo, T.Nombre as TipoServicio, V.CodigoCliente, V.Nombre as Cliente, V.Observacion , r.Id_Ruta 
	,	case when  v.TieneObservacion = 0 then 'No' else 'Si' end TieneObservacion,
	 case when (SELECT COUNT(*) FROM tbl_codigoventacargousuario cu where id_venta = v.id_venta and cu.e_eliminado=0)  >0 then 'Si' else 'No' end TieneCargo
	From 
	tbl_venta V 
	inner join tbl_TipoServicio T on V.Id_TipoServicio = T.Id_TipoServicio
	inner join tbl_Ruta R on R.Id_Ruta = V.Id_Ruta 
	Where
	dbo.dateonly(V.Fecha_Ejecucion) = dbo.dateonly(@Fecha_Registro) and V.E_eliminado = 0
	Order by V.Id_Ruta, V.Id_Venta
End

GO

/* [dbo].[sp_obtenerlistaordenestrabajo_otweb] */
CREATE proc [dbo].[sp_ObtenerListaOrdenesTrabajo_OTWEB](@Fecha_Registro datetime)
as
Begin
	Select
	R.Nombre as Ruta, V.Id_Venta, dbo.dateonly(V.Fecha_Ejecucion) Fecha_Ejecucion, dbo.dateonly(V.Fecha_Registro) Fecha_Registro, 
	V.OrdenTrabajo, T.Nombre as TipoServicio, V.CodigoCliente, V.Nombre as Cliente, V.Observacion , r.Id_Ruta 
	,	case when  v.TieneObservacion = 0 then 'No' else 'Si' end TieneObservacion,
	 case when (SELECT COUNT(*) FROM tbl_codigoventacargousuario cu where id_venta = v.id_venta and cu.e_eliminado=0)  >0 then 'Si' else 'No' end TieneCargo
	From 
	tbl_venta V 
	inner join tbl_TipoServicio T on V.Id_TipoServicio = T.Id_TipoServicio
	inner join tbl_Ruta R on R.Id_Ruta = V.Id_Ruta 
	Where
	dbo.dateonly(V.Fecha_Ejecucion) = dbo.dateonly(@Fecha_Registro) and V.E_eliminado = 0
	 AND UPPER(LTRIM(RTRIM(ISNULL(v.Origen, '')))) = 'OT_WEB'
	Order by V.Id_Ruta, V.Id_Venta
End


GO

/* [dbo].[sp_ObtenerListaOrdenesTrabajoRFechas] */
CREATE PROCEDURE dbo.sp_ObtenerListaOrdenesTrabajoRFechas(@Fecha_RegistroInicio datetime,@Fecha_RegistroFin datetime)
as
Begin
	Select
	R.Nombre as Ruta, V.Id_Venta, dbo.dateonly(V.Fecha_Ejecucion) Fecha_Ejecucion, 
	dbo.dateonly(V.Fecha_Registro) Fecha_Registro, 
	V.OrdenTrabajo, T.Nombre as TipoServicio, V.CodigoCliente, V.Nombre as Cliente, 
	V.Observacion
	, case when v.tieneObservacion =0 then 'No' else 'Si' end TieneObservacion,
	case when (SELECT COUNT(*) FROM tbl_codigoventacargousuario cu where id_venta = v.id_venta and cu.e_eliminado=0)  >0 then 'Si' else 'No' end TieneCargo
	From 
	tbl_venta V 
	inner join tbl_TipoServicio T on V.Id_TipoServicio = T.Id_TipoServicio
	inner join tbl_Ruta R on R.Id_Ruta = V.Id_Ruta 
	Where
	dbo.dateonly(V.Fecha_Ejecucion) between dbo.dateonly(@Fecha_RegistroInicio)  and dbo.dateonly(@Fecha_RegistroFin)
	and V.E_eliminado = 0
	Order by V.Id_Ruta,V.Fecha_Ejecucion desc, V.Id_Venta
End

GO

/* [dbo].[sp_ObtenerListaTipoDevolucion] */
create proc [dbo].[sp_ObtenerListaTipoDevolucion]
as 
select * from tbl_TipoDevolucion where E_Eliminado=0

GO

/* [dbo].[sp_ObtenerNotasDeVenta] */


create proc [dbo].[sp_ObtenerNotasDeVenta] (@Id_Ruta int, @fecha datetime)
as
IF OBJECT_ID('dbo.NotasDeVenta') IS not NULL
BEGIN
	drop table NotasDeVenta;
END
--VERIFICA SI ES PDA
--declare @EsPDA bit
--	set @EsPDA = (SELECT E_EsPDA FROM TBL_RUTA  WHERE dbo.tbl_Ruta.Id_Ruta = @Id_Ruta)

declare @contador2 int;
declare @cuantos2 int;
declare @contador int;
	set @contador = 1;

declare @cuantos int;
	set @cuantos = (select COUNT (*) from tbl_Producto )

declare @strproducto nvarchar(50)

declare @consulta nvarchar(1000);
declare @consultaInsercion nvarchar(1000);

declare @ProductosOrdenados table (id int identity ,Id_Producto int, Nombre nvarchar(100))

insert into @ProductosOrdenados 
	select Id_Producto , nombre from tbl_Producto order by Observacion 

	set @consulta = 'create table NotasDeVenta (Id int identity, PDV_EH nvarchar(50), NroNotaVenta nvarchar(50) '
	set @consultaInsercion = 'insert into NotasDeVenta values(0,0'
		while (@contador<= @cuantos )
		begin
			set @strproducto = (select nombre from @ProductosOrdenados where Id =	@contador)
			set @consulta = @consulta +','+ @strproducto + ' int' 
			set @consultaInsercion = @consultaInsercion +',0'
			
			set @contador = @contador + 1;
		end
	
	set @consulta = @consulta +', TOTAL decimal(18,2)'
	set @consulta = @consulta + ')'
	set @consultaInsercion = @consultaInsercion + ',0)'
	
	exec (@consulta)

		declare @NroNotas table(Id int identity, NroNotaVenta nvarchar(20))

		--if ( @EsPDA = 0)
		--	BEGIN		
				insert into @NroNotas  
					SELECT DISTINCT dbo.tbl_Venta.OrdenTrabajo
					FROM         dbo.tbl_Venta 
					WHERE     (dbo.DateOnly(dbo.tbl_Venta.Fecha_Ejecucion) = dbo.DateOnly(@fecha)) AND (dbo.tbl_Venta.E_Eliminado = 0) and dbo.tbl_Venta.Id_Ruta=@Id_Ruta
					ORDER BY dbo.tbl_Venta.OrdenTrabajo
		--	END
		--ELSE
		--	BEGIN
		--		insert into @NroNotas  
		--			SELECT DISTINCT dbo.tbl_VentaPDA.NroNotaVenta
		--			FROM         dbo.tbl_VentaPDA 
		--			WHERE     (dbo.DateOnly(dbo.tbl_VentaPDA.Fecha_Venta) = dbo.DateOnly(@fecha)) AND (dbo.tbl_VentaPDA.Id_Ruta = @Id_Ruta) AND (dbo.tbl_VentaPDA.E_Eliminado = 0)
		--			ORDER BY dbo.tbl_VentaPDA.NroNotaVenta
		--	END


		declare @UniversoNotas table(Id int identity, Id_Ruta int, Ruta nvarchar(150),Vendedor nvarchar(300),Id_Producto int, Producto nvarchar(50),Cantidad int,  Fecha datetime, NroNotaVenta nvarchar(50), Total decimal(18,2))

		--if ( @EsPDA = 0)
		--	BEGIN
				insert into @UniversoNotas
				
					SELECT     dbo.tbl_Venta.Id_Ruta, dbo.tbl_Ruta.Nombre as Ruta,dbo.tbl_Vendedor.Nombre AS Vendedor,
					dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre as Producto,sum(dbo.tbl_CodigoVenta.Cantidad) as Cantidad, 
					 dbo.tbl_Venta.Fecha_Ejecucion, dbo.tbl_Venta.OrdenTrabajo,
					case when dbo.tbl_Producto.Id_Producto = 32  
					then ((sum(dbo.tbl_CodigoVenta.Cantidad * dbo.tbl_CodigoVenta.Precio ))*(-1)) 
					else sum(dbo.tbl_CodigoVenta.Cantidad * dbo.tbl_CodigoVenta.Precio)
					end Total					
					FROM         dbo.tbl_Venta INNER JOIN    
					dbo.tbl_CodigoVenta ON dbo.tbl_Venta.Id_Venta = dbo.tbl_CodigoVenta.Id_Venta INNER JOIN  
					dbo.tbl_Producto ON dbo.tbl_CodigoVenta.Id_Producto = dbo.tbl_Producto.Id_Producto INNER JOIN 
					dbo.tbl_Ruta ON dbo.tbl_Venta.id_Ruta = dbo.tbl_Ruta.Id_Ruta INNER JOIN 
					dbo.tbl_Vendedor ON dbo.tbl_Venta.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor 
					WHERE     (dbo.DateOnly(dbo.tbl_Venta.Fecha_Ejecucion) = dbo.DateOnly(@fecha))
					and dbo.tbl_Ruta.Id_Ruta= @Id_Ruta and dbo.tbl_Venta.E_Eliminado=0 and dbo.tbl_CodigoVenta.E_Eliminado=0 
					group by   dbo.tbl_Venta.id_Ruta, dbo.tbl_Ruta.Nombre ,dbo.tbl_Vendedor.Nombre , 
					dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre , dbo.tbl_Venta.Fecha_Ejecucion , dbo.tbl_Venta.OrdenTrabajo
					order by dbo.tbl_Venta.OrdenTrabajo ,dbo.tbl_Producto.Id_Producto


					
		--	END
		--ELSE
		--	BEGIN
		--		insert into @UniversoNotas		
		--			SELECT     dbo.tbl_VentaPDA.Id_Ruta, dbo.tbl_Ruta.Nombre as Ruta,dbo.tbl_Vendedor.Nombre AS Vendedor,
		--			dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre as Producto,sum(dbo.tbl_CodigoVentaPDA.Cantidad) as Cantidad,
		--			ISNULL((select ehumano from tbl_pda where id_pda = dbo.tbl_VentaPDA.Id_PDA),0) as IDPDV_EH, dbo.tbl_VentaPDA.Fecha_Venta as Fecha, dbo.tbl_VentaPDA.NroNotaVenta,
		--			sum(dbo.tbl_CodigoVentaPDA.Cantidad * dbo.tbl_CodigoVentaPDA.Precio) Total
		--			FROM         dbo.tbl_VentaPDA INNER JOIN
		--			dbo.tbl_CodigoVentaPDA ON dbo.tbl_VentaPDA.Id_VentaPDA = dbo.tbl_CodigoVentaPDA.Id_VentaPDA INNER JOIN
		--			dbo.tbl_Producto ON dbo.tbl_CodigoVentaPDA.Id_Producto = dbo.tbl_Producto.Id_Producto INNER JOIN
		--			dbo.tbl_Ruta ON dbo.tbl_VentaPDA.id_Ruta = dbo.tbl_Ruta.Id_Ruta INNER JOIN
		--			dbo.tbl_Vendedor ON dbo.tbl_VentaPDA.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor
		--			WHERE     (dbo.DateOnly(dbo.tbl_VentaPDA.Fecha_Venta) = dbo.DateOnly(@fecha))
		--			and dbo.tbl_VentaPDA.e_eliminado = 0 and dbo.tbl_CodigoVentaPDA.e_eliminado=0 and dbo.tbl_Ruta.Id_Ruta = @Id_Ruta
		--			group by   dbo.tbl_VentaPDA.id_Ruta, dbo.tbl_Ruta.Nombre ,dbo.tbl_Vendedor.Nombre ,
		--			dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre ,
		--			dbo.tbl_VentaPDA.Id_PDA,dbo.tbl_VentaPDA.Fecha_Venta, dbo.tbl_VentaPDA.NroNotaVenta
		--			order by dbo.tbl_VentaPDA.NroNotaVenta ,dbo.tbl_Producto.Id_Producto
		--	END


			declare @Total decimal(18,2) ;
			declare @indice int
			declare @Cantidad int

				set @contador = 1;
				set @contador2 = 1;
				set @cuantos = (select COUNT(*) from  @NroNotas);
				set @indice = 1
			declare @DetalleNotas table(Id int identity, NroNotaVenta nvarchar(50),Producto nvarchar(50),Cantidad int, Total decimal(18,2))
				while (@contador <= @cuantos)
				begin	
					set @strproducto = (select NroNotaVenta from @NroNotas where Id=@contador);				
					
					insert into @DetalleNotas 
						select NroNotaVenta,Producto,Cantidad,Total from @UniversoNotas where NroNotaVenta = @strproducto			
				--insercion
					exec (@consultaInsercion)
					
						set @Total = 0;
						set @cuantos2 = (select COUNT(*) from @DetalleNotas)
						set @contador2 = 1;
						set @Cantidad = 0;
							while (@contador2 <=@cuantos2)
							begin					
									
									set @Total = @Total + ( select total from @DetalleNotas where id = @indice )							
									set @consulta = 'update NotasDeVenta set '
									set @consulta = @consulta + (select producto from @DetalleNotas where id = @indice) 
									set @consulta = @consulta + ' = '
									set @consulta = @consulta + (select producto from @DetalleNotas where id = @indice) +' + ' + (select cast(cantidad as nvarchar(15)) from @DetalleNotas where id = @indice) 
								--	set @consulta = @consulta + ', PDV_EH = ' +CHAR(39)+(select PDV_EH from @DetalleNotas where id = @indice ) +CHAR(39)
									set @consulta = @consulta + ', NroNotaVenta = ' +(select NroNotaVenta from @DetalleNotas where id = @indice ) 						
									set @consulta = @consulta + ' where id= ' + cast(@contador as nvarchar(15))
									print @consulta
									update NotasDeVenta set Total= @Total where Id=@contador 												
									exec (@consulta)
								set @indice = @indice+1;
								set @contador2 = @contador2 + 1;					
							end		
						delete from @DetalleNotas
						set @contador = @contador + 1;
				end	
				select * from NotasDeVenta
	


GO

/* [dbo].[sp_ObtenerOrdenesDetalladasLocal] */
create proc [dbo].[sp_ObtenerOrdenesDetalladasLocal]--(@id_tipoTransaccion int)
as
   select Id_OrdenesTrabajoDetalladasLocal, Id_venta,  Id_Usuario ,
     (select nombre from tbl_usuario where Id_Usuario = v.id_usuario  )Usuario ,
	 id_codigoVenta , id_vendedor, (select nombre from tbl_vendedor where id_vendedor = v.id_vendedor  ) Vendedor ,
	 Id_Ruta , (select nombre from tbl_ruta where id_ruta = v.Id_Ruta  ) Ruta ,
	 Id_TipoServicio,(select nombre from tbl_tiposervicio where Id_TipoServicio = v.Id_TipoServicio  ) TipoServicio  ,  
	 Fecha_Ejecucion, Fecha_Registro,Ordentrabajo,Observacion ,Id_producto ,
	 (select nombre from tbl_producto where Id_producto = v.Id_producto  ) Producto, Id_tipoMaterial ,
	 (select nombre from tbl_tipomaterial where Id_tipoMaterial = v.Id_tipoMaterial  ) TipoMaterial,
	 Codigo_Inicio, ChipID,Cantidad,Id_UsuarioE,(select nombre from tbl_usuario where Id_Usuario = v.Id_UsuarioE  )UsuarioE ,
	 E_eliminado, id_estado,(select nombre from tbl_estado where id_estado = v.id_estado  ) Estado , 
	 18 Id_Sucursal, (select upper( sucursal)Sucursal from tbl_sucursal where Id_Sucursal = 18  )Sucursal ,---- CAMBIAR SUCURSAL
	 Codigocliente, TieneObservacion, E_enviado, Id_TipoTransaccion       
from tbl_OrdenesTrabajoDetalladasLocal v --inner join tbl_codigoventa cv on v.id_venta = cv.id_venta
where E_enviado = 0 and  v.cantidad>0

GO

/* [dbo].[sp_ObtenerOrdenTrabajo_X_FechaRegistro] */
CREATE proc [dbo].[sp_ObtenerOrdenTrabajo_X_FechaRegistro](@FechaRInicio datetime,@FechaRFin datetime)
as
begin
		select v.Id_Venta, v.Id_Vendedor, ven.Nombre,v.Fecha_Ejecucion,v.Observacion,v.OrdenTrabajo,r.Nombre Ruta,
		v.CodigoCliente,
		r.Id_Ruta,v.TieneObservacion
		from tbl_Venta v, tbl_Vendedor  ven, tbl_Ruta r
		where dbo.DateOnly(Fecha_Ejecucion) between dbo.DateOnly(@FechaRInicio) and dbo.DateOnly(@FechaRFin)
		and v.Id_Ruta = r.Id_Ruta
		and v.E_Eliminado=0
		AND v.id_vendedor= ven.Id_Vendedor
end

GO

/* [dbo].[sp_ObtenerOrdenTrabajo_X_Id_Venta] */
create proc [dbo].[sp_ObtenerOrdenTrabajo_X_Id_Venta](@Id_Venta int)
as
declare @NumeroOrden int;
set @NumeroOrden = isnull((select ordentrabajo from tbl_Venta where Id_Venta =@Id_Venta and E_Eliminado=0),0);

	exec [sp_ObtenerOrdenTrabajo_X_Numero] @NumeroOrden
		
GO

/* [dbo].[sp_ObtenerOrdenTrabajo_X_Numero] */
CREATE proc [dbo].[sp_ObtenerOrdenTrabajo_X_Numero](@NumeroOrden int)
as


	--SELECT     o.Id_OrdenTrabajo, o.NumeroOrden, o.Nombre, o.Direccion, o.ReferenciaAdicional, o.Id_TipoServicio, 
	--ts.Prefijo , ts.nombre TipoServicio, 
	--o.Zona, o.TipoCasa, o.Color, o.PosicionGeografica, o.CRE, o.Fecha_Programacion, o.Fecha_Registro, o.Id_Usuario,U.Nombre Usuario
	--FROM         dbo.tbl_OrdenTrabajo AS o INNER JOIN
	--dbo.tbl_TipoServicio AS ts ON o.Id_TipoServicio = ts.Id_TipoServicio INNER JOIN
	--dbo.tbl_Usuario as U ON o.Id_Usuario = U.Id_Usuario
	--and o.NumeroOrden = @NumeroOrden

	if (@NumeroOrden = 1)
	BEGIN
		select v.*
		from  tbl_Venta v --tbl_Cliente cl, tbl_Casa c, tbl_OrdenTrabajo ot, tbl_TipoServicio ts
		where 
		v.E_Eliminado=0
		and 1 = 2
	END
	ELSE
	BEGIN
		select v.*
		from  tbl_Venta v --tbl_Cliente cl, tbl_Casa c, tbl_OrdenTrabajo ot, tbl_TipoServicio ts
		where 
		v.E_Eliminado=0
		and v.OrdenTrabajo = @NumeroOrden
	END
GO

/* [dbo].[sp_ObtenerPasswordEncriptado] */
CREATE proc [dbo].[sp_ObtenerPasswordEncriptado](@Loggin nvarchar(15))
as 
select Password from tbl_Usuario where loggin=@Loggin and e_eliminado=0

GO

/* [dbo].[sp_ObtenerPedidoVendedor] */

CREATE  proc [dbo].[sp_ObtenerPedidoVendedor] (@fecha datetime)
as
SELECT     dbo.tbl_PedidoVendedor.Id_PedidoVendedor, dbo.tbl_PedidoVendedor.Id_Ruta, dbo.tbl_Ruta.Nombre, dbo.tbl_PedidoVendedor.Fecha, 
dbo.tbl_PedidoVendedor.Observacion
FROM         dbo.tbl_PedidoVendedor INNER JOIN
dbo.tbl_Ruta ON dbo.tbl_PedidoVendedor.Id_Ruta = dbo.tbl_Ruta.Id_Ruta
WHERE dbo.tbl_PedidoVendedor.E_Eliminado = 0 and dbo.dateonly(tbl_PedidoVendedor.Fecha)=dbo.dateonly(@fecha)
order by dbo.tbl_Ruta.id_ruta

GO

/* [dbo].[sp_ObtenerPedidoVendedor_X_ID] */

CREATE proc [dbo].[sp_ObtenerPedidoVendedor_X_ID] (@id int)
as 
SELECT     dbo.tbl_PedidoVendedor.Id_PedidoVendedor, dbo.tbl_Ruta.Nombre RutaNombre, dbo.tbl_Usuario.Nombre Usuario,
dbo.tbl_PedidoVendedor.Fecha, dbo.tbl_PedidoVendedor.Fecha_Registro, dbo.tbl_PedidoVendedor.Total, 
dbo.tbl_Vendedor.Nombre AS NombreVendedor, dbo.tbl_Vendedor.CI, dbo.tbl_Vendedor.Telefono, 
dbo.tbl_PedidoVendedor.Observacion
FROM         dbo.tbl_PedidoVendedor INNER JOIN
dbo.tbl_Usuario ON dbo.tbl_PedidoVendedor.Id_Usuario = dbo.tbl_Usuario.Id_Usuario INNER JOIN
dbo.tbl_Ruta ON dbo.tbl_PedidoVendedor.Id_Ruta = dbo.tbl_Ruta.Id_Ruta INNER JOIN
dbo.tbl_Vendedor ON dbo.tbl_PedidoVendedor.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor
WHERE  dbo.tbl_PedidoVendedor.Id_PedidoVendedor=@id


GO

/* [dbo].[sp_ObtenerProductoAgrupado] */



create proc [dbo].[sp_ObtenerProductoAgrupado]
as
declare @tabla table (id int not null identity(1,1),nombre varchar (100),id_Producto int ,padre int)
declare @cont int
declare @contPadre int
declare @TipoProducto int
set  @cont = 1
insert into @tabla
select distinct  Nombre,Id_TipoProducto,0 from tbl_TipoProducto --order by nombre desc

set @contPadre =   (select COUNT(*) from  @tabla)
while ( @cont<= (@contPadre ))
	begin
        set @TipoProducto=( select id_Producto from @tabla where  id= @cont )
        insert into @tabla
        select Nombre,Id_Producto,Id_TipoProducto from tbl_Producto where Id_TipoProducto = @TipoProducto order by Observacion 
        set @cont =@cont+1
	end
select * from @tabla


GO

/* [dbo].[sp_ObtenerProductoLocal] */
create  proc sp_ObtenerProductoLocal
as
	select  v.Id_productoLocal,v.Id_producto,
	 (select nombre from tbl_producto  where id_producto = v.id_producto )Nombre,
	 (select nombre from tbl_producto  where id_producto = v.id_producto )NombreAnterior,
	 v.FechaActualizacion, v.e_eliminado ,
	 18 id_sucursal,(select upper( sucursal) from tbl_sucursal where id_sucursal = 18 )Sucursal,
	 case when Id_TipoTransaccion in (1,3) then 1 else 0 end E_actualizado
	from  tbl_ProductoLocal v
	where E_enviado = 0 

GO

/* [dbo].[sp_ObtenerPromedioUsoMateriales] */

CREATE proc [dbo].[sp_ObtenerPromedioUsoMateriales]--38
as
	select pr.Id_PromedioUsoMateriales,ts.Id_TipoServicio,ts.Nombre TipoServicio,
	pr.Id_Producto,pr.Cantidad,pr.CantidadMaxima,p.Nombre ,
	pr.CantidadMaximaNM,pr.AdicionaEnForm,pr.Id_TipoProducto
	from tbl_PromedioUsoMateriales pr inner join tbl_TipoServicio ts on ts.Id_TipoServicio = pr.Id_TipoServicio	and pr.E_Eliminado=0
	left join tbl_producto p on p.Id_Producto = pr.Id_Producto
	
GO

/* [dbo].[sp_ObtenerSaldoAlmacen_y_SaldoRetiro_X_Fecha] */
--sp_ObtenerSaldoAlmacen_y_SaldoRetiro_X_Fecha '17/01/2020'
CREATE PROCEDURE sp_ObtenerSaldoAlmacen_y_SaldoRetiro_X_Fecha (@fecha datetime)
AS
BEGIN
--Saldo sumar (tbl_codigocierrealmacen.SaldoDiaHoy + tbl_codigocierrealmacenPR_PD.SaldoDiaHoyPD + tbl_codigocuadre.ItemsSobrantes) Campo1
--Saldo Retiro sumar (tbl_codigocierrealmacenPR_PD.SaldoDiaHoyPR + tbl_codigocuadre.ItemsRetirados) Campo2
--Campo1, Campo2

	--declare @fecha datetime
	--set @fecha = '17/01/2020'
	declare @Cuadre table (Id_Producto int, ItemsSobrantes decimal(18,2), ItemsRetirados decimal(18,2))
	INSERT INTO @Cuadre select p.id_producto, sum(cc.ItemsSobrantes)ItemsSobrantes, sum(cc.ItemsRetirados) ItemsRetirados
	from tbl_cuadre c 
	inner join tbl_codigocuadre cc on c.id_cuadre = cc.id_cuadre
	inner join tbl_producto p on cc.id_producto = p.id_producto
	where c.id_cuadre in 
	(
		select id_cuadre from 
		(
			select Id_Ruta, max(id_cuadre) id_cuadre
			from tbl_cuadre 
			where id_ruta in (select id_ruta from tbl_ruta where e_eliminado = 0 and visible = 1 and id_ruta > 0) and
			dbo.dateonly(fecha) <= @fecha and e_eliminado = 0
			group by Id_Ruta 
		) T
	)
	group by p.id_producto, p.nombre

	declare @Cierre1 table (Id_Producto int, SaldoDiaHoy decimal(18,2))
	INSERT INTO @Cierre1
	select Id_Producto, SaldoDiaHoy 
	from tbl_CierreAlmacen c inner join tbl_CodigoCierreAlmacen cc on c.id_cierrealmacen = cc.id_cierrealmacen 
	where dbo.dateonly(c.fecha) = @fecha and c.e_eliminado = 0

	declare @Cierre2 table (Id_Producto int, SaldoDiaHoyPD decimal(18,2), SaldoDiaHoyPR decimal(18,2))
	INSERT INTO @Cierre2
	select Id_Producto, SaldoDiaHoyPD, SaldoDiaHoyPR  
	from tbl_CierreAlmacenPR_PD c inner join tbl_CodigoCierreAlmacenPR_PD cc on c.id_cierrealmacenPR_PD = cc.id_cierrealmacenPR_PD 
	where dbo.dateonly(c.fecha) = @fecha and c.e_eliminado = 0

	declare @Resultado table (Id_Producto int, ItemsSobrantes decimal(18,2), ItemsRetirados decimal(18,2), SaldoDiaHoy decimal(18,2), SaldoDiaHoyPD decimal(18,2), SaldoDiaHoyPR decimal(18,2))
	INSERT INTO @Resultado
	select Id_producto, ItemsSobrantes, ItemsRetirados, 0 as SaldoDiaHoy, 0 as SaldoDiaHoyPD, 0 as SaldoDiaHoyPR from @Cuadre union all
	select Id_Producto, 0 as ItemsSobrantes, 0 as ItemsRetirados, SaldoDiaHoy, 0 as SaldoDiaHoyPD, 0 as SaldoDiaHoyPR from @Cierre1 union all
	select Id_Producto, 0 as ItemsSobrantes, 0 as ItemsRetirados, 0 as SaldoDiaHoy, SaldoDiaHoyPD, SaldoDiaHoyPR from @Cierre2 

	select T.Id_producto, p.nombre, sum(Campo1) as Saldo, sum(Campo2) as Retiro from 
	(select Id_producto, SaldoDiaHoy + SaldoDiaHoyPD + ItemsSobrantes as Campo1, ItemsRetirados + SaldoDiaHoyPR as Campo2 from @Resultado) T inner join tbl_producto p
	on T.id_producto = p.id_producto
	group by T.Id_producto, p.nombre
END
GO

/* [dbo].[sp_ObtenerSaldoCodigoCuadre] */


CREATE  proc [dbo].[sp_ObtenerSaldoCodigoCuadre] (@Id_Ruta int, @fecha datetime)
as
IF OBJECT_ID('dbo.SaldoPedido') IS not NULL
BEGIN
	drop table SaldoPedido;
END
--VERIFICA SI ES PDA
--declare @EsPDA bit
--	set @EsPDA = (SELECT E_EsPDA FROM TBL_RUTA  WHERE dbo.tbl_Ruta.Id_Ruta = @Id_Ruta)

declare @contador2 int;
declare @cuantos2 int;
declare @contador int;
declare @indice int;
	set @contador = 1;

declare @cuantos int;
	set @cuantos = (select COUNT (*) from tbl_Producto )

declare @strproducto nvarchar(50)

declare @consulta nvarchar(1000);
declare @consultaInsercion nvarchar(1000);
declare @ProductosOrdenados table (id int identity ,Id_Producto int, Nombre nvarchar(100))

insert into @ProductosOrdenados 
	select Id_Producto , nombre from tbl_Producto order by Observacion 


	set @consulta = 'create table SaldoPedido (Id int identity, PDV_EH nvarchar(50), NroNotaVenta nvarchar(50) '
	set @consultaInsercion = 'insert into SaldoPedido values(0,0'
		while (@contador<= @cuantos )
		begin
			set @strproducto = (select nombre from @ProductosOrdenados where Id =	@contador)
			set @consulta = @consulta +','+ @strproducto + ' int' 
			set @consultaInsercion = @consultaInsercion +',0'
			
			set @contador = @contador + 1;
		end
	
	set @consulta = @consulta +', TOTAL decimal(18,2)'
	set @consulta = @consulta + ')'
	set @consultaInsercion = @consultaInsercion + ',0)'
	print (@consultaInsercion)
	print (@consulta)
	
	exec (@consulta)
	exec (@consultaInsercion)
	
		
		declare @Id_CuadreSaldo table (Id int identity, Codigo int) 
		declare @Ventas table(Id int identity, Producto nvarchar(50), ItemsVendidos int,ItemsSobrantes int,TotalVenta decimal(18,2))
		
		----------Ventas
		insert into @Id_CuadreSaldo 
			select top(1) Id_Cuadre from tbl_Cuadre where dbo.DateOnly(Fecha)=dbo.DateOnly(@fecha) and Id_Ruta =@Id_Ruta and E_Eliminado = 0 order by Id_Cuadre desc
		
		insert into @Ventas
			select p.Nombre , cc.ItemsVendidos ,cc.ItemsSobrantes, cc.TotalVendidos  from tbl_CodigoCuadre cc, tbl_Producto p 
			where cc.Id_Producto = p.Id_Producto  and (cc.ItemsVendidos<>0 or cc.ItemsSobrantes>0)--and cc.ItemsVendidos<>0
			and Id_Cuadre in (select codigo from @Id_CuadreSaldo)
			
			
		set @cuantos = (select COUNT(*) from @Ventas)
		 
		set @contador = 1;
		set @indice = 1;
		declare @totalventa decimal(18,2)
		set @totalventa = 0;
		exec (@consultaInsercion)
			while (@contador <=@cuantos)
			begin
			 
						set @consulta = 'update SaldoPedido set '
						set @consulta = @consulta + (select producto from @Ventas where id = @contador) 
						set @consulta = @consulta + ' = '				
						set @consulta = @consulta + (select cast(ItemsVendidos as nvarchar(15))from @Ventas where id = @contador)
						set @consulta = @consulta + ' where id= ' + cast(@indice as nvarchar(15))
						set @totalventa = @totalventa +  cast((select TotalVenta from @Ventas where id = @contador) as decimal(18,2))
						exec (@consulta)
						print @consulta	
						set @contador = @contador+1;
			 end
			 
			 update SaldoPedido set NroNotaVenta ='VentasCuadre' where id = @indice
			 update SaldoPedido set Total =@totalventa where id = @indice
			 set @contador = 1;
			 set @indice = 2;
			while (@contador <=@cuantos)
			     begin
				   set @consulta = 'update SaldoPedido set '
						set @consulta = @consulta + (select producto from @Ventas where id = @contador) 
						set @consulta = @consulta + ' = '				
						set @consulta = @consulta + (select cast(ItemsSobrantes as nvarchar(15))from @Ventas where id = @contador)
						set @consulta = @consulta + ' where id= ' + cast(@indice as nvarchar(15))
						exec (@consulta)
						print @consulta	
				set @contador = @contador+1;
			end
			update SaldoPedido set NroNotaVenta ='SaldoCuadre' where id = @indice
		 select * from SaldoPedido 
		




GO

/* [dbo].[sp_ObtenerSaldoCuadre] */


CREATE PROC [dbo].[sp_ObtenerSaldoCuadre](@Id_Cuadre int)
as
	SELECT     dbo.tbl_Cuadre.Id_Cuadre, dbo.tbl_Producto.Nombre, 
	dbo.tbl_SaldoCuadre.Cod_Inicio, dbo.tbl_SaldoCuadre.Cod_Final, dbo.tbl_SaldoCuadre.Cantidad, dbo.tbl_CodigoCuadre.ItemsSobrantes, 
	dbo.tbl_CodigoCuadre.ItemsVendidos,(sum(dbo.tbl_SaldoCuadre.Cantidad) * dbo.tbl_CodigoCuadre.Precio) as TotalVendidos
	FROM         dbo.tbl_Cuadre INNER JOIN
	dbo.tbl_CodigoCuadre ON dbo.tbl_Cuadre.Id_Cuadre = dbo.tbl_CodigoCuadre.Id_Cuadre AND 
	dbo.tbl_CodigoCuadre.E_Eliminado = 0 INNER JOIN
	dbo.tbl_SaldoCuadre ON dbo.tbl_Cuadre.Id_Cuadre = dbo.tbl_SaldoCuadre.Id_Cuadre and dbo.tbl_SaldoCuadre.E_Eliminado = 0 INNER JOIN
	dbo.tbl_Producto ON dbo.tbl_CodigoCuadre.Id_Producto = dbo.tbl_Producto.Id_Producto and 
	dbo.tbl_SaldoCuadre.Id_Producto = dbo.tbl_Producto.Id_Producto
	where  dbo.tbl_Cuadre.Id_Cuadre=@Id_Cuadre
	group by   dbo.tbl_Cuadre.Id_Cuadre, dbo.tbl_Producto.Nombre, 
	dbo.tbl_SaldoCuadre.Cod_Inicio, dbo.tbl_SaldoCuadre.Cod_Final, dbo.tbl_SaldoCuadre.Cantidad, dbo.tbl_CodigoCuadre.ItemsSobrantes, 
	dbo.tbl_CodigoCuadre.ItemsVendidos, dbo.tbl_CodigoCuadre.Precio,dbo.tbl_Producto.Observacion 
	order by dbo.tbl_Producto.Observacion ,dbo.tbl_Cuadre.Id_Cuadre, dbo.tbl_Producto.Nombre, 
	dbo.tbl_SaldoCuadre.Cantidad, dbo.tbl_CodigoCuadre.ItemsSobrantes, 
	dbo.tbl_CodigoCuadre.ItemsVendidos, 
	dbo.tbl_SaldoCuadre.Cod_Inicio, dbo.tbl_SaldoCuadre.Cod_Final
	
	--[sp_ObtenerSaldoCuadre]1155
	
--	select * from tbl_CodigoCuadre


GO

/* [dbo].[sp_ObtenerSaldoPedido] */


CREATE proc [dbo].[sp_ObtenerSaldoPedido] (@Id_Ruta int, @fecha datetime)
as
IF OBJECT_ID('dbo.SaldoPedido') IS not NULL
BEGIN
	drop table SaldoPedido;
END
--VERIFICA SI ES PDA
--declare @EsPDA bit
--	set @EsPDA = (SELECT E_EsPDA FROM TBL_RUTA  WHERE dbo.tbl_Ruta.Id_Ruta = @Id_Ruta)

declare @contador2 int;
declare @cuantos2 int;
declare @contador int;
declare @indice int;
	set @contador = 1;

declare @cuantos int;
	set @cuantos = (select COUNT (*) from tbl_Producto )

declare @strproducto nvarchar(50)

declare @consulta nvarchar(1000);
declare @consultaInsercion nvarchar(1000);
declare @ProductosOrdenados table (id int identity ,Id_Producto int, Nombre nvarchar(100))

insert into @ProductosOrdenados 
	select Id_Producto , nombre from tbl_Producto order by Observacion 


	set @consulta = 'create table SaldoPedido (Id int identity, PDV_EH nvarchar(50), NroNotaVenta nvarchar(50) '
	set @consultaInsercion = 'insert into SaldoPedido values(0,0'
		while (@contador<= @cuantos )
		begin
			set @strproducto = (select nombre from @ProductosOrdenados where Id =	@contador)
			set @consulta = @consulta +','+ @strproducto + ' int' 
			set @consultaInsercion = @consultaInsercion +',0'
			
			set @contador = @contador + 1;
		end
	
	set @consulta = @consulta +', TOTAL decimal(18,2)'
	set @consulta = @consulta + ')'
	set @consultaInsercion = @consultaInsercion + ',0)'
	
	exec (@consulta)
		
		declare @Id_CuadrePedido table (Id int identity, Codigo int) 
		declare @Sobrantes table(Id int identity, Producto nvarchar(50), ItemsSobrantes int)
		
		----------sobrante
		insert into @Id_CuadrePedido 
			select top(1) Id_Cuadre from tbl_Cuadre where dbo.DateOnly(Fecha)<dbo.DateOnly(@fecha) and Id_Ruta =@Id_Ruta and E_Eliminado = 0 order by Id_Cuadre desc
		
		insert into @Sobrantes
			select p.Nombre , cc.ItemsSobrantes from tbl_CodigoCuadre cc, tbl_Producto p 
			where cc.Id_Producto = p.Id_Producto  and cc.ItemsSobrantes<>0
			and Id_Cuadre in (select codigo from @Id_CuadrePedido)
			
		set @cuantos = (select COUNT(*) from @Sobrantes)
		set @contador = 1;
		set @indice = 1;
		exec (@consultaInsercion)
			while (@contador <=@cuantos)
			begin
				set @consulta = 'update SaldoPedido set '
				set @consulta = @consulta + (select producto from @Sobrantes where id = @contador) 
				set @consulta = @consulta + ' = '				
				set @consulta = @consulta + (select cast(ItemsSobrantes as nvarchar(15))from @Sobrantes where id = @contador)
				set @consulta = @consulta + ' where id= ' + cast(@indice as nvarchar(15))
				exec (@consulta)
				--print @consulta				
				set @contador = @contador+1;
			end
			
		update SaldoPedido set NroNotaVenta ='SaldoCuadre' where id = @indice
			--select * from SaldoPedido 
			
			
		---------------pedido
		delete from @Id_CuadrePedido
		
		insert into @Id_CuadrePedido
			select Id_AlmacenVendedor from tbl_AlmacenVendedor where dbo.DateOnly(Fecha)=dbo.DateOnly(@fecha) and Id_Ruta = @Id_Ruta 
			and E_Eliminado =0	
		
		declare @Pedido table(Id int identity, Producto nvarchar(50), Cantidad int);
		insert into @Pedido
			select p.Nombre , sum(cav.Cantidad)Cantidad  from tbl_CodigoAlmacenVendedor cav, tbl_Producto p
			where cav.Id_Producto = p.Id_Producto  and cav.Id_AlmacenVendedor in (select codigo from @Id_CuadrePedido) 
			group by p.Nombre 
		
	
		set @cuantos = (select COUNT(*) from @Pedido)
		set @contador = 1;
		set @indice = 2;
		exec (@consultaInsercion)
			while (@contador <=@cuantos)
			begin
				set @consulta = 'update SaldoPedido set '
				set @consulta = @consulta + (select producto from @Pedido where id = @contador) 
				set @consulta = @consulta + ' = '				
				set @consulta = @consulta + (select cast(Cantidad as nvarchar(15))from @Pedido where id = @contador)
				set @consulta = @consulta + ' where id= ' + cast(@indice as nvarchar(15))
				exec (@consulta)
				--print @consulta				
				set @contador = @contador+1;
			end
		
		update SaldoPedido set NroNotaVenta ='Pedido' where id = @indice
		
		select * from SaldoPedido 



GO

/* [dbo].[sp_ObtenerSalidaAlmacenBol] */
CREATE proc [dbo].[sp_ObtenerSalidaAlmacenBol] (@bol nvarchar(30), @id_ruta int)
as
SELECT     dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor , dbo.tbl_Usuario.Nombre AS Usuario, 
dbo.tbl_Ruta.Id_Ruta , dbo.tbl_Ruta.Nombre AS NombreRuta,
dbo.tbl_Vendedor.Nombre AS NombreVendedor, dbo.tbl_AlmacenVendedor.Fecha,  
dbo.tbl_Producto.Id_Producto ,dbo.tbl_Producto.Nombre AS Producto, dbo.tbl_CodigoAlmacenVendedor.Cod_Inicio,
dbo.tbl_CodigoAlmacenVendedor.Cod_Inicio as Codigo,  dbo.tbl_CodigoAlmacenVendedor.Cantidad 
FROM         dbo.tbl_AlmacenVendedor INNER JOIN 
dbo.tbl_CodigoAlmacenVendedor ON  
dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor = dbo.tbl_CodigoAlmacenVendedor.Id_AlmacenVendedor INNER JOIN 
dbo.tbl_Producto ON dbo.tbl_CodigoAlmacenVendedor.Id_Producto = dbo.tbl_Producto.Id_Producto INNER JOIN 
dbo.tbl_Usuario ON dbo.tbl_AlmacenVendedor.Id_Usuario = dbo.tbl_Usuario.Id_Usuario INNER JOIN 
dbo.tbl_Ruta ON dbo.tbl_AlmacenVendedor.Id_Ruta = dbo.tbl_Ruta.Id_Ruta INNER JOIN 
dbo.tbl_Vendedor ON dbo.tbl_AlmacenVendedor.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor 
WHERE      @bol = dbo.tbl_CodigoAlmacenVendedor.Cod_Inicio 
and dbo.tbl_Ruta.Id_Ruta = @id_ruta
and tbl_AlmacenVendedor.e_eliminado = 0 and tbl_CodigoAlmacenVendedor.e_eliminado = 0


select * from tbl_CodigoAlmacenVendedor
GO

/* [dbo].[sp_ObtenerSobrante] */


CREATE proc [dbo].[sp_ObtenerSobrante] (@Id_Ruta int, @fecha datetime) 
as
IF OBJECT_ID('dbo.Auxiliar') IS not NULL
BEGIN
	drop table Auxiliar;
END

--VERIFICA SI ES PDA
--declare @EsPDA bit
--	set @EsPDA = (SELECT E_EsPDA FROM TBL_RUTA  WHERE dbo.tbl_Ruta.Id_Ruta = @Id_Ruta)

declare @contador int;
	set @contador = 1;

declare @cuantos int;
	set @cuantos = (select COUNT (*) from tbl_Producto )

declare @strproducto nvarchar(50)

declare @consulta nvarchar(1000);
declare @consultaInsercion nvarchar(1000);

declare @ProductosOrdenados table (id int identity ,Id_Producto int, Nombre nvarchar(100))

insert into @ProductosOrdenados 
	select Id_Producto , nombre from tbl_Producto order by Observacion 

	set @consulta = 'create table Auxiliar (Id int , PDV_EH nvarchar(50), NroNotaVenta nvarchar(50) '
	set @consultaInsercion = 'insert into Auxiliar values(0,0'
		while (@contador<= @cuantos )
		begin
			set @strproducto = (select nombre from @ProductosOrdenados where Id =	@contador)
			set @consulta = @consulta +','+ @strproducto + ' int' 
			set @consultaInsercion = @consultaInsercion +',0'
			
			set @contador = @contador + 1;
		end
	
	set @consulta = @consulta +', TOTAL decimal(18,2)'
	set @consulta = @consulta + ')'
	set @consultaInsercion = @consultaInsercion + ',0)'
	
	exec (@consulta)


	declare @UniversoNotas table(Id int identity, Id_Ruta int, Ruta nvarchar(150),Vendedor nvarchar(300),Id_Producto int, Producto nvarchar(50),Cantidad int, Fecha datetime, NroNotaVenta nvarchar(50), Total decimal(18,2))

		--if ( @EsPDA = 0)
		--	BEGIN
				insert into @UniversoNotas
					SELECT     dbo.tbl_Venta.Id_Ruta, dbo.tbl_Ruta.Nombre as Ruta,dbo.tbl_Vendedor.Nombre AS Vendedor,
					dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre as Producto,sum(dbo.tbl_CodigoVenta.Cantidad) as Cantidad, 
					 dbo.tbl_Venta.Fecha_Ejecucion, dbo.tbl_Venta.OrdenTrabajo,
					sum(dbo.tbl_CodigoVenta.Cantidad * dbo.tbl_Producto.PrecioVenta ) Total
					FROM         dbo.tbl_Venta INNER JOIN    
					dbo.tbl_CodigoVenta ON dbo.tbl_Venta.Id_Venta = dbo.tbl_CodigoVenta.Id_Venta INNER JOIN  
					dbo.tbl_Producto ON dbo.tbl_CodigoVenta.Id_Producto = dbo.tbl_Producto.Id_Producto INNER JOIN 
					dbo.tbl_Ruta ON dbo.tbl_Venta.id_Ruta = dbo.tbl_Ruta.Id_Ruta INNER JOIN 
					dbo.tbl_Vendedor ON dbo.tbl_Venta.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor 
					WHERE     (dbo.DateOnly(dbo.tbl_Venta.Fecha_Ejecucion) = dbo.DateOnly(@fecha))
					and dbo.tbl_Ruta.Id_Ruta= @Id_Ruta and dbo.tbl_Venta.E_Eliminado=0 and dbo.tbl_CodigoVenta.E_Eliminado=0 
					group by   dbo.tbl_Venta.id_Ruta, dbo.tbl_Ruta.Nombre ,dbo.tbl_Vendedor.Nombre , 
					dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre , 
					dbo.tbl_Venta.Fecha_Ejecucion, dbo.tbl_Venta.OrdenTrabajo 
					order by dbo.tbl_Venta.OrdenTrabajo ,dbo.tbl_Producto.Id_Producto
		--	END
		--ELSE
		--	BEGIN
		--		insert into @UniversoNotas		
		--			SELECT     dbo.tbl_VentaPDA.Id_Ruta, dbo.tbl_Ruta.Nombre as Ruta,dbo.tbl_Vendedor.Nombre AS Vendedor,
		--			dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre as Producto,sum(dbo.tbl_CodigoVentaPDA.Cantidad) as Cantidad,
		--			ISNULL((select ehumano from tbl_pda where id_pda = dbo.tbl_VentaPDA.Id_PDA),0) as IDPDV_EH, dbo.tbl_VentaPDA.Fecha_Venta as Fecha, dbo.tbl_VentaPDA.NroNotaVenta,
		--			sum(dbo.tbl_CodigoVentaPDA.Cantidad * dbo.tbl_Producto.PrecioVenta ) Total
		--			FROM         dbo.tbl_VentaPDA INNER JOIN
		--			dbo.tbl_CodigoVentaPDA ON dbo.tbl_VentaPDA.Id_VentaPDA = dbo.tbl_CodigoVentaPDA.Id_VentaPDA INNER JOIN
		--			dbo.tbl_Producto ON dbo.tbl_CodigoVentaPDA.Id_Producto = dbo.tbl_Producto.Id_Producto INNER JOIN
		--			dbo.tbl_Ruta ON dbo.tbl_VentaPDA.id_Ruta = dbo.tbl_Ruta.Id_Ruta INNER JOIN
		--			dbo.tbl_Vendedor ON dbo.tbl_VentaPDA.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor
		--			WHERE     (dbo.DateOnly(dbo.tbl_VentaPDA.Fecha_Venta) = dbo.DateOnly(@fecha))
		--			and dbo.tbl_VentaPDA.e_eliminado = 0 and dbo.tbl_CodigoVentaPDA.e_eliminado=0 and dbo.tbl_Ruta.Id_Ruta = @Id_Ruta
		--			group by   dbo.tbl_VentaPDA.id_Ruta, dbo.tbl_Ruta.Nombre ,dbo.tbl_Vendedor.Nombre ,
		--			dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre ,
		--			dbo.tbl_VentaPDA.Id_PDA,dbo.tbl_VentaPDA.Fecha_Venta, dbo.tbl_VentaPDA.NroNotaVenta
		--			order by dbo.tbl_VentaPDA.NroNotaVenta ,dbo.tbl_Producto.Id_Producto
		--	END
			
declare @Notas table(Id int identity, Producto nvarchar(50), cantidad int)
insert into @Notas
	select Producto, SUM(Cantidad)Cantidad from @UniversoNotas
	group by Producto


declare @CantSaldoPedido int ;
declare @CantNotas int ;
declare @Total int ;
declare @cons nvarchar(150);

insert into Auxiliar exec sp_ObtenerSaldoPedido @Id_Ruta,@fecha 
 
 
 declare @valor table(cant int)
 --select * from Auxiliar

	set @cuantos = (select COUNT(*) from tbl_Producto )
	set @contador =1
	
	delete from SaldoPedido where id=2
	
	
	while (@contador <= @cuantos)
	begin
		set @strproducto = (select Nombre from tbl_Producto where Id_Producto = @contador)		
		
		set @cons = 'select sum('+@strproducto+') from auxiliar';		
		insert into @valor exec (@cons)
		
		set @CantSaldoPedido = (select cant from @valor )
		set @CantNotas = isnull((select cantidad from @Notas where Producto = 	@strproducto),0)
		set @Total = isnull((@CantSaldoPedido - @CantNotas),0);
		
		

				set @consulta = 'update SaldoPedido set '
				set @consulta = @consulta + @strproducto
				set @consulta = @consulta + ' = '
				set @consulta = @consulta + CAST(@Total as nvarchar(50))				
				set @consulta = @consulta + ' where id = 1'
				--print @consulta				
				exec (@consulta)
		delete from @valor
		set @contador = @contador + 1;
		
	end	
	
	update SaldoPedido set NroNotaVenta ='Sobrantes' where id = 1
	select * from SaldoPedido
	


GO

/* [dbo].[sp_ObtenerTipoGrupo] */
CREATE proc sp_ObtenerTipoGrupo
as
declare @tabla table (nombre nvarchar(100))
insert into @tabla values ('ANTENERO')
insert into @tabla values ('MAKIRO')
insert into @tabla values ('SUCURSAL')
SELECT * FROM @tabla

GO

/* [dbo].[sp_ObtenerTipoMaterial] */
CREATE proc [dbo].[sp_ObtenerTipoMaterial]( @TipoServicio int)
as 
	declare @TipoInstalacion int
	set @TipoInstalacion = (select TipoMaterial from tbl_TipoServicio where Id_TipoServicio =@TipoServicio)
		if(@TipoInstalacion = 1)	
			select * from tbl_TipoMaterial where E_Eliminado=0 and Id_TipoMaterial in (1)--)3, 4)
		
		if(@TipoInstalacion = 2)
			select * from tbl_TipoMaterial where E_Eliminado=0 and Id_TipoMaterial in (1,2)--,3,4)
		
		if(@TipoInstalacion = 3)
			select * from tbl_TipoMaterial where E_Eliminado=0 and Id_TipoMaterial in (2)
	 

GO

/* [dbo].[sp_ObtenerVenta] */
CREATE PROC [dbo].[sp_ObtenerVenta](@Id_Venta int)
as
	select v.Id_Venta, v.Id_Vendedor, ven.Nombre,v.Fecha_Ejecucion,v.Observacion,v.OrdenTrabajo
	from tbl_Venta v, tbl_Vendedor  ven
	where Id_Venta=@Id_Venta
	AND v.id_vendedor= ven.Id_Vendedor
	
GO

/* [dbo].[sp_ObtenerVentaXID] */
create proc sp_ObtenerVentaXID(@Id_Venta int)
as
				
	select 
	v.Id_Venta,v.Id_Usuario,u.Nombre Usuario,ven.Id_Vendedor,ven.Nombre Vendedor,r.Id_Ruta,r.Nombre Ruta,
	ts.Id_TipoServicio,ts.Nombre TipoServicio,v.Fecha_Ejecucion,v.Fecha_Registro,v.OrdenTrabajo,v.Observacion
	from tbl_Venta v inner join tbl_Usuario u on u.Id_Usuario = v.Id_Usuario 
	inner join tbl_Vendedor ven on ven.Id_Vendedor = v.Id_Vendedor
	inner join tbl_Ruta r on r.Id_Ruta = v.Id_Ruta 
	inner join tbl_TipoServicio ts on ts.Id_TipoServicio = v.Id_TipoServicio
	where v.Id_Venta=9185 and v.E_Eliminado=0
	
	select cv.Id_CodigoVenta,cv.Id_Venta,pr.Id_Producto,pr.Nombre Producto,tm.Id_TipoMaterial,tm.Nombre TipoMaterial,
	cv.Cod_Inicio,cv.Cod_Final,cv.Cantidad
	from tbl_CodigoVenta cv inner join tbl_Producto pr on pr.Id_Producto = cv.Id_Producto
	inner join tbl_TipoMaterial tm on tm.Id_TipoMaterial = cv.Id_TipoMaterial
	where Id_Venta=9185 and cv.E_Eliminado=0


GO

/* [dbo].[sp_Producto_Treeview] */



CREATE  proc [dbo].[sp_Producto_Treeview]

as

      declare @productos as varchar(300)

      set @productos = 'Tarjeta,T10,T15,T20,T30,T50,T100,MiniCarga,Chip,Chip_Blanco,Chip_Rescate,TigoGiro,TM_Venta,TM_Compra'

      select @productos as Productos

GO

/* [dbo].[sp_renamediagram] */

	CREATE PROCEDURE dbo.sp_renamediagram
	(
		@diagramname 		sysname,
		@owner_id		int	= null,
		@new_diagramname	sysname
	
	)
	WITH EXECUTE AS 'dbo'
	AS
	BEGIN
		set nocount on
		declare @theId 			int
		declare @IsDbo 			int
		
		declare @UIDFound 		int
		declare @DiagId			int
		declare @DiagIdTarg		int
		declare @u_name			sysname
		if((@diagramname is null) or (@new_diagramname is null))
		begin
			RAISERROR ('Invalid value', 16, 1);
			return -1
		end
	
		EXECUTE AS CALLER;
		select @theId = DATABASE_PRINCIPAL_ID();
		select @IsDbo = IS_MEMBER(N'db_owner'); 
		if(@owner_id is null)
			select @owner_id = @theId;
		REVERT;
	
		select @u_name = USER_NAME(@owner_id)
	
		select @DiagId = diagram_id, @UIDFound = principal_id from dbo.sysdiagrams where principal_id = @owner_id and name = @diagramname 
		if(@DiagId IS NULL or (@IsDbo = 0 and @UIDFound <> @theId))
		begin
			RAISERROR ('Diagram does not exist or you do not have permission.', 16, 1)
			return -3
		end
	
		-- if((@u_name is not null) and (@new_diagramname = @diagramname))	-- nothing will change
		--	return 0;
	
		if(@u_name is null)
			select @DiagIdTarg = diagram_id from dbo.sysdiagrams where principal_id = @theId and name = @new_diagramname
		else
			select @DiagIdTarg = diagram_id from dbo.sysdiagrams where principal_id = @owner_id and name = @new_diagramname
	
		if((@DiagIdTarg is not null) and  @DiagId <> @DiagIdTarg)
		begin
			RAISERROR ('The name is already used.', 16, 1);
			return -2
		end		
	
		if(@u_name is null)
			update dbo.sysdiagrams set [name] = @new_diagramname, principal_id = @theId where diagram_id = @DiagId
		else
			update dbo.sysdiagrams set [name] = @new_diagramname where diagram_id = @DiagId
		return 0
	END
	
GO

/* [dbo].[sp_tecnico_obtenernombreporid] */
CREATE PROCEDURE dbo.SP_Tecnico_ObtenerNombrePorId
    @IdTecnico INT
AS
BEGIN
    SET NOCOUNT ON;
    SELECT TOP 1 Nombre
    FROM (
        SELECT v.Nombre, 1 AS orden
        FROM dbo.tbl_Vendedor v
        WHERE v.Id_Vendedor = @IdTecnico
          AND ISNULL(v.E_Eliminado,0)=0
        UNION ALL
        SELECT u.Nombre, 2 AS orden
        FROM dbo.tbl_UsuarioTecnico ut
        LEFT JOIN dbo.tbl_Usuario u ON u.Id_Usuario = ut.id_Usuario
        WHERE ut.id_Usuario = @IdTecnico
          AND ISNULL(ut.e_eliminado,0)=0
        UNION ALL
        SELECT u2.Nombre, 3 AS orden
        FROM dbo.tbl_Usuario u2
        WHERE u2.Id_Usuario = @IdTecnico
          AND ISNULL(u2.E_Eliminado,0)=0
    ) q
    WHERE q.Nombre IS NOT NULL AND LTRIM(RTRIM(q.Nombre)) <> ''
    ORDER BY q.orden;
END

GO

/* [dbo].[sp_TraerCierreAlmacen] */

create proc [dbo].[sp_TraerCierreAlmacen] (@fecha datetime)
as 
SELECT     ca.Id_CierreAlmacen, ca.Id_Usuario, dbo.tbl_Usuario.Nombre, 
ca.Fecha,ca.Observacion, ca.E_Eliminado 
FROM         dbo.tbl_cierrealmacen ca INNER JOIN 
dbo.tbl_Usuario ON ca.Id_Usuario = dbo.tbl_Usuario.Id_Usuario
WHERE     (dbo.DateOnly(CA.Fecha) = dbo.DateOnly(@fecha)) AND (ca.E_Eliminado = 0)


GO

/* [dbo].[sp_TraerCierreAlmacen_xID] */

CREATE proc [dbo].[sp_TraerCierreAlmacen_xID] (@id int)
as 
begin 
	SELECT     ca.Id_CierreAlmacen, ca.Id_Usuario, dbo.tbl_Usuario.Nombre, 
	ca.Fecha,ca.Observacion, ca.E_Eliminado 
	FROM         dbo.tbl_cierrealmacen ca INNER JOIN 
	dbo.tbl_Usuario ON ca.Id_Usuario = dbo.tbl_Usuario.Id_Usuario
	WHERE     (dbo.DateOnly(CA.Id_CierreAlmacen) = @id) AND (ca.E_Eliminado = 0)


	SELECT dbo.tbl_Producto.Id_Producto, dbo.tbl_Producto.Nombre, 
	cca.SaldoDiaAnterior, ISNULL( cca.SaldoDiaAnteriorDevolucion,0)SaldoDiaAnteriorDevolucion, 
	cca.IngresoDia,
	ISNULL( cca.DevolucionIngreso,0)DevolucionIngreso,ISNULL(cca.DevolucionSalida,0)DevolucionSalida, 
	cca.SalidaDia,  cca.SalidaBaja,
	cca.SaldoDiaHoy, ISNULL(cca.SaldoDiaHoyDevolucion,0)SaldoDiaHoyDevolucion
	FROM         dbo.tbl_CodigoCierreAlmacen cca INNER JOIN 
	dbo.tbl_CierreAlmacen ca ON cca.Id_CierreAlmacen = ca.Id_CierreAlmacen INNER JOIN 
	dbo.tbl_Producto ON cca.Id_Producto = dbo.tbl_Producto.Id_Producto 
	WHERE cca.Id_CierreAlmacen =@id and ca.e_eliminado=0 
	ORDER BY dbo.tbl_Producto.nombre
end


GO

/* [dbo].[sp_TraerCierreAlmacen_xRangoFecha] */
create proc [dbo].[sp_TraerCierreAlmacen_xRangoFecha] (@fechaInicio datetime,@fechaFin datetime)
as 
select * from(
SELECT     ca.Id_CierreAlmacen, 'Cierre' Tipo,
CONVERT(varchar,ca.Fecha,103)Fecha,  dbo.tbl_Usuario.Nombre
FROM         dbo.tbl_cierrealmacen ca INNER JOIN 
dbo.tbl_Usuario ON ca.Id_Usuario = dbo.tbl_Usuario.Id_Usuario
WHERE     (dbo.DateOnly(CA.Fecha) between dbo.DateOnly(@fechaInicio) and dbo.DateOnly(@fechaFin)) AND (ca.E_Eliminado = 0)
union all
SELECT     ca.Id_CierreAlmacenPR_PD Id_CierreAlmacen, 'PR_PDCierre' Tipo,
CONVERT(varchar,ca.Fecha,103)Fecha,  dbo.tbl_Usuario.Nombre
FROM         dbo.tbl_CierreAlmacenPR_PD ca INNER JOIN 
dbo.tbl_Usuario ON ca.Id_Usuario = dbo.tbl_Usuario.Id_Usuario
WHERE     (dbo.DateOnly(CA.Fecha) between dbo.DateOnly(@fechaInicio) and dbo.DateOnly(@fechaFin)) AND (ca.E_Eliminado = 0)
)a order by fecha desc

GO

/* [dbo].[sp_TraerCierreAlmacenPRPD] */

CREATE proc [dbo].[sp_TraerCierreAlmacenPRPD] (@fecha datetime)
as 
begin
	declare @codigo int
	set @codigo = (select top 1 Id_CierreAlmacenPR_PD from tbl_CierreAlmacenPR_PD where  dbo.DateOnly(Fecha) = dbo.DateOnly(@fecha) AND (E_Eliminado = 0))

	SELECT     ca.Id_CierreAlmacenPR_PD, ca.Id_Usuario, dbo.tbl_Usuario.Nombre, 
	ca.Fecha,ca.Observacion, ca.E_Eliminado 
	FROM         dbo.tbl_CierreAlmacenPR_PD ca INNER JOIN 
	dbo.tbl_Usuario ON ca.Id_Usuario = dbo.tbl_Usuario.Id_Usuario
	WHERE     (dbo.DateOnly(CA.Fecha) = dbo.DateOnly(@fecha)) AND (ca.E_Eliminado = 0)

	SELECT ca.Id_CierreAlmacenPR_PD,cca.Id_CodigoCierreAlmacenPR_PD, dbo.tbl_Producto.Id_Producto, dbo.tbl_Producto.Nombre,
	cca.SaldoDiaAnteriorPR,cca.IngresoDevolucionPR,cca.SalidaBajaPR,cca.SalidaDevolucionTPR,SaldoDiaHoyPR,
	cca.SaldoDiaAnteriorPD,cca.IngresoDevolucionPD,cca.SalidaBajaPD,cca.SalidaDevolucionTPD,SaldoDiaHoyPD
	FROM         dbo.tbl_CodigoCierreAlmacenPR_PD cca INNER JOIN 
	dbo.tbl_CierreAlmacenPR_PD ca ON cca.Id_CierreAlmacenPR_PD = ca.Id_CierreAlmacenPR_PD INNER JOIN 
	dbo.tbl_Producto ON cca.Id_Producto = dbo.tbl_Producto.Id_Producto 
	WHERE dbo.dateonly(ca.Fecha) = dbo.dateonly(@fecha) and ca.e_eliminado=0  and ca.Id_CierreAlmacenPR_PD = @codigo
	ORDER BY dbo.tbl_Producto.nombre
end


GO

/* [dbo].[sp_TraerCierreAlmacenPRPD_xId] */

CREATE proc [dbo].[sp_TraerCierreAlmacenPRPD_xId] (@codigo int)
as 
begin

	SELECT     ca.Id_CierreAlmacenPR_PD, ca.Id_Usuario, dbo.tbl_Usuario.Nombre, 
	ca.Fecha,ca.Observacion, ca.E_Eliminado 
	FROM         dbo.tbl_CierreAlmacenPR_PD ca INNER JOIN 
	dbo.tbl_Usuario ON ca.Id_Usuario = dbo.tbl_Usuario.Id_Usuario
	WHERE     (ca.Id_CierreAlmacenPR_PD =@codigo) AND (ca.E_Eliminado = 0)

	SELECT ca.Id_CierreAlmacenPR_PD,cca.Id_CodigoCierreAlmacenPR_PD, dbo.tbl_Producto.Id_Producto, dbo.tbl_Producto.Nombre,
	cca.SaldoDiaAnteriorPR,cca.IngresoDevolucionPR,cca.SalidaBajaPR,cca.SalidaDevolucionTPR,SaldoDiaHoyPR,
	cca.SaldoDiaAnteriorPD,cca.IngresoDevolucionPD,cca.SalidaBajaPD,cca.SalidaDevolucionTPD,SaldoDiaHoyPD
	FROM         dbo.tbl_CodigoCierreAlmacenPR_PD cca INNER JOIN 
	dbo.tbl_CierreAlmacenPR_PD ca ON cca.Id_CierreAlmacenPR_PD = ca.Id_CierreAlmacenPR_PD INNER JOIN 
	dbo.tbl_Producto ON cca.Id_Producto = dbo.tbl_Producto.Id_Producto 
	WHERE cca.Id_CierreAlmacenPR_PD =@codigo and ca.e_eliminado=0  and ca.Id_CierreAlmacenPR_PD = @codigo
	ORDER BY dbo.tbl_Producto.nombre
end


GO

/* [dbo].[sp_TraerCodigoCierreAlmacen] */
--[sp_TraerCodigoCierreAlmacen] '08/11/2017'
CREATE proc [dbo].[sp_TraerCodigoCierreAlmacen] (@fecha datetime)
as 
SELECT dbo.tbl_Producto.Id_Producto, dbo.tbl_Producto.Nombre, 
cca.SaldoDiaAnterior, ISNULL( cca.SaldoDiaAnteriorDevolucion,0)SaldoDiaAnteriorDevolucion, 
cca.IngresoDia,
ISNULL( cca.DevolucionIngreso,0)DevolucionIngreso,ISNULL(cca.DevolucionSalida,0)DevolucionSalida, 
cca.SalidaDia,  cca.SalidaBaja,
cca.SaldoDiaHoy, ISNULL(cca.SaldoDiaHoyDevolucion,0)SaldoDiaHoyDevolucion
FROM         dbo.tbl_CodigoCierreAlmacen cca INNER JOIN 
dbo.tbl_CierreAlmacen ca ON cca.Id_CierreAlmacen = ca.Id_CierreAlmacen INNER JOIN 
dbo.tbl_Producto ON cca.Id_Producto = dbo.tbl_Producto.Id_Producto 
WHERE dbo.dateonly(ca.Fecha) = dbo.dateonly(@fecha) and ca.e_eliminado=0 
ORDER BY dbo.tbl_Producto.Observacion
GO

/* [dbo].[sp_TraerCuadre] */

CREATE proc [dbo].[sp_TraerCuadre](@Id_Cuadre int)
as
SELECT     dbo.tbl_Cuadre.Id_Cuadre, dbo.tbl_Ruta.Nombre AS NombreRuta, dbo.tbl_Vendedor.Nombre AS Vendedor, 
dbo.tbl_Cuadre.Fecha, dbo.tbl_Cuadre.Fecha_Registro FechaRegistro, dbo.tbl_Usuario.Nombre as Usuario,dbo.tbl_Cuadre.Total, dbo.tbl_Vendedor.CI,
 dbo.tbl_Vendedor.Telefono
FROM         dbo.tbl_Cuadre INNER JOIN 
dbo.tbl_Ruta ON dbo.tbl_Cuadre.Id_Ruta = dbo.tbl_Ruta.Id_Ruta INNER JOIN 
dbo.tbl_Vendedor ON dbo.tbl_Ruta.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor INNER JOIN 
dbo.tbl_Usuario ON dbo.tbl_Cuadre.Id_Usuario = dbo.tbl_Usuario.Id_Usuario
WHERE     dbo.tbl_Cuadre.Id_Cuadre = @Id_Cuadre




GO

/* [dbo].[sp_TraerEntregasVendedorCierreAlmacen] */
	CREATE proc [dbo].[sp_TraerEntregasVendedorCierreAlmacen] (@fecha datetime)
as 
	select Id_Producto, Nombre,SUM(Cantidad)Cantidad
	from
		(SELECT     p.Id_Producto,p.Nombre, isnull(SUM(cav.Cantidad),0) AS Cantidad
		FROM         dbo.tbl_CodigoAlmacenVendedor cav INNER JOIN  
		dbo.tbl_AlmacenVendedor av ON cav.Id_AlmacenVendedor = av.Id_AlmacenVendedor INNER JOIN  
		dbo.tbl_Producto  p ON cav.Id_Producto = p.Id_Producto  
		WHERE     (cav.E_Eliminado = 0) AND (av.E_Eliminado = 0) AND   
		(dbo.DateOnly(av.Fecha) = dbo.DateOnly(@fecha))  
		GROUP BY p.Id_Producto, p.Nombre		
		union all
		SELECT     p.Id_Producto,p.Nombre, isnull(SUM(cav.Cantidad),0) AS Cantidad
		FROM         dbo.tbl_CodigoDevolucion cav INNER JOIN  
		dbo.tbl_Devolucion av ON cav.Id_Devolucion = av.Id_Devolucion INNER JOIN  
		dbo.tbl_Producto  p ON cav.Id_Producto = p.Id_Producto  
		WHERE     (cav.E_Eliminado = 0) AND (av.E_Eliminado = 0) AND   
		(dbo.DateOnly(av.Fecha) = dbo.DateOnly(@fecha))  
		and av.Estado = 1
		GROUP BY p.Id_Producto, p.Nombre
		union all
		SELECT     p.Id_Producto,p.Nombre, isnull(SUM(cav.Cantidad),0) AS Cantidad
		FROM         dbo.tbl_CodigoEntregaAlmacenOtros cav INNER JOIN  
		dbo.tbl_EntregaAlmacenOtros av ON cav.Id_EntregaAlmacenOtros = av.Id_EntregaAlmacenOtros INNER JOIN  
		dbo.tbl_Producto  p ON cav.Id_Producto = p.Id_Producto  
		WHERE     (cav.E_Eliminado = 0) AND (av.E_Eliminado = 0) AND   
		(dbo.DateOnly(av.Fecha) = dbo.DateOnly(@fecha))  
		GROUP BY p.Id_Producto, p.Nombre) a 		
	group by Id_Producto, Nombre
GO

/* [dbo].[sp_TraerIngresosAlmacenCierreAlmacen] */
CREATE  proc [dbo].[sp_TraerIngresosAlmacenCierreAlmacen] (@fecha datetime)
as
	select Id_Producto, Nombre,SUM(Cantidad)Cantidad
	from
	(SELECT  dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre, sum(cia.Cantidad)AS Cantidad	
	FROM    dbo.tbl_IngresoAlmacen ia INNER JOIN 
	dbo.tbl_CodigoIngresoAlmacen cia ON ia.Id_IngresoAlmacen = cia.Id_IngresoAlmacen INNER JOIN 
	dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
	where dbo.dateonly(ia.Fecha)=dbo.dateonly(@fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
	group by dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre,dbo.tbl_Producto.PrecioVenta
	union all
	SELECT  dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre, sum(cia.Cantidad)AS Cantidad	
	FROM    dbo.tbl_Devolucion ia INNER JOIN 
	dbo.tbl_CodigoDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
	dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
	where dbo.dateonly(ia.Fecha)=dbo.dateonly(@fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
	and Estado = 0
	group by dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre,dbo.tbl_Producto.PrecioVenta) a
	group by Id_Producto, Nombre
GO

/* [dbo].[sp_TraerIngresosDiaHoy] */

create proc [dbo].[sp_TraerIngresosDiaHoy](@Fecha datetime)
as
select * from tbl_IngresoAlmacen where dbo.dateonly(Fecha)=dbo.dateonly(@Fecha)
and E_Eliminado=0


GO

/* [dbo].[sp_TraerRutasNoCuadradas] */
CREATE proc [dbo].[sp_TraerRutasNoCuadradas] (@fecha datetime)
as
	select r.Id_Ruta , r.Nombre as NombreRuta, v.Id_Vendedor, v.Nombre as NombreVendedor, GETDATE() Fecha, SUM(st.Cantidad*p.PrecioVenta) as Total
	from tbl_ruta r, tbl_Vendedor v, tbl_SaldoTarjetas st, tbl_Producto p
	where r.Id_Vendedor = v.Id_Vendedor
	and r.E_Eliminado = 0  and v.E_Eliminado=0
	and st.Id_Ruta = r.Id_Ruta 
	and st.Id_Producto = p.Id_Producto
	AND r.Id_Ruta not in (select Id_Ruta from tbl_cuadre where e_Eliminado = 0 and  dbo.dateonly(fecha)= dbo.dateonly(@fecha))
	and r.Id_Ruta not in(0)
	and visible=1
	group by r.Id_Ruta , r.Nombre, v.Id_Vendedor, v.Nombre
	order by r.Nombre
	
GO

/* [dbo].[sp_TraerSaldoCierreAlmacen] */

CREATE proc [dbo].[sp_TraerSaldoCierreAlmacen] (@fecha datetime)
as
SELECT dbo.tbl_Producto.Id_Producto, dbo.tbl_Producto.Nombre,SUM(cc.SaldoDiaHoy) Cantidad, dbo.tbl_Producto.PrecioVenta Precio, sum(cc.SaldoDiaHoy*dbo.tbl_Producto.PrecioVenta) as TotalParcial
FROM   dbo.tbl_Producto INNER JOIN 
dbo.tbl_CodigoCierreAlmacen cc ON dbo.tbl_Producto.Id_Producto = cc.Id_Producto INNER JOIN 
dbo.tbl_CierreAlmacen ca ON cc.Id_CierreAlmacen = ca.Id_CierreAlmacen
WHERE  cc.E_Eliminado = 0  and ca.E_Eliminado = 0 and
dbo.dateonly(ca.Fecha) =  dbo.dateonly(

(  select max(DBO.DATEONLY(cca.Fecha))  from dbo.tbl_CierreAlmacen cca
WHERE DBO.DATEONLY(cca.Fecha)<>DBO.DATEONLY(@fecha) and 
DBO.DATEONLY(cca.Fecha)<DBO.DATEONLY(@fecha))) --ORDER BY dbo.tbl_Producto.Observacion 

group by dbo.tbl_Producto.Id_Producto, dbo.tbl_Producto.Nombre,dbo.tbl_Producto.PrecioVenta,dbo.tbl_Producto.Observacion
ORDER BY  dbo.tbl_Producto.Observacion
GO

/* [dbo].[sp_TraerSaldoTarjetasRuta] */
CREATE proc [dbo].[sp_TraerSaldoTarjetasRuta] (@Id_Ruta int)
as 
SELECT     dbo.tbl_SaldoTarjetas.Id_SaldoTarjetas, dbo.tbl_SaldoTarjetas.Id_Producto, dbo.tbl_SaldoTarjetas.Id_Ruta, dbo.tbl_SaldoTarjetas.Cantidad, 
dbo.tbl_SaldoTarjetas.E_Eliminado, dbo.tbl_Producto.Observacion, dbo.tbl_Producto.Nombre 
FROM         dbo.tbl_SaldoTarjetas INNER JOIN 
dbo.tbl_Producto ON dbo.tbl_SaldoTarjetas.Id_Producto = dbo.tbl_Producto.Id_Producto 
WHERE     (dbo.tbl_SaldoTarjetas.Id_Ruta = @Id_Ruta) AND (dbo.tbl_SaldoTarjetas.E_Eliminado = 0) 
and dbo.tbl_Producto.E_Eliminado = 0
order by dbo.tbl_Producto.nombre

GO

/* [dbo].[sp_TraerSucursal] */
create proc [dbo].[sp_TraerSucursal]
as
	select * from tbl_Sucursal where E_Eliminado=0 order by Observacion 

GO

/* [dbo].[sp_TraerTodasLasRuta] */

CREATE proc [dbo].[sp_TraerTodasLasRuta]
as
select r.Id_Ruta,r.Nombre,r.Id_Vendedor ,v.Nombre NombreVendedor,BodegaTigo,r.E_Eliminado
from tbl_Ruta r,tbl_Vendedor v
where r.Id_Vendedor = v.Id_Vendedor
and r.E_Eliminado=0 and Id_Ruta>0 --and visible = 1
order by  Nombre



GO

/* [dbo].[sp_TraerTodasLasRutaPDevolucion] */

CREATE proc [dbo].[sp_TraerTodasLasRutaPDevolucion]
as
select r.Id_Ruta,r.Nombre,r.Id_Vendedor,v.Id_Vendedor ,v.Nombre NombreVendedor,r.E_Eliminado 
from tbl_Ruta r,tbl_Vendedor v
where r.Id_Vendedor = v.Id_Vendedor
and r.E_Eliminado=0  and visible=1
order by nombre

GO

/* [dbo].[sp_TraerTodasVenta] */
CREATE PROC [dbo].[sp_TraerTodasVenta](@fecha datetime )
as
	select v.Id_Venta, v.Id_Vendedor, ven.Nombre,v.Fecha_Ejecucion,v.Observacion,v.OrdenTrabajo,r.Nombre Ruta
	from tbl_Venta v, tbl_Vendedor  ven, tbl_Ruta r
	where dbo.DateOnly(Fecha_Ejecucion)=dbo.DateOnly(@fecha)
	and v.Id_Ruta = r.Id_Ruta
	and v.E_Eliminado=0
	AND v.id_vendedor= ven.Id_Vendedor
	
GO

/* [dbo].[sp_TraerTodosAlmacenEntregadoOtrosHoy] */
create proc sp_TraerTodosAlmacenEntregadoOtrosHoy(@Fecha datetime)
as
select *
from tbl_EntregaAlmacenOtros 
where dbo.DateOnly(Fecha)=dbo.DateOnly(@Fecha)
and E_Eliminado=0
GO

/* [dbo].[sp_TraerTodosAlmacenEntregadosHoy] */

CREATE proc [dbo].[sp_TraerTodosAlmacenEntregadosHoy](@Fecha datetime)
as
SELECT     dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor, dbo.tbl_AlmacenVendedor.Id_Ruta, dbo.tbl_Ruta.Nombre, dbo.tbl_Vendedor.Id_Vendedor, 
dbo.tbl_Vendedor.Nombre AS NombreVendedor 
FROM         dbo.tbl_AlmacenVendedor INNER JOIN 
dbo.tbl_Ruta ON dbo.tbl_AlmacenVendedor.Id_Ruta = dbo.tbl_Ruta.Id_Ruta INNER JOIN 
dbo.tbl_Vendedor ON dbo.tbl_Ruta.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor 
where dbo.tbl_AlmacenVendedor.E_Eliminado=0 and dbo.dateonly(dbo.tbl_AlmacenVendedor.Fecha)=dbo.dateonly(@Fecha) 
order by dbo.tbl_Ruta.id_ruta 


GO

/* [dbo].[sp_TraerTodosAlmacenEntregadosRangoFechas] */

create proc [dbo].[sp_TraerTodosAlmacenEntregadosRangoFechas](@Fecha datetime,@FechaFin datetime)
as
SELECT     dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor, dbo.tbl_AlmacenVendedor.Id_Ruta, dbo.tbl_Ruta.Nombre, dbo.tbl_Vendedor.Id_Vendedor, 
dbo.tbl_Vendedor.Nombre AS NombreVendedor 
FROM         dbo.tbl_AlmacenVendedor INNER JOIN 
dbo.tbl_Ruta ON dbo.tbl_AlmacenVendedor.Id_Ruta = dbo.tbl_Ruta.Id_Ruta INNER JOIN 
dbo.tbl_Vendedor ON dbo.tbl_Ruta.Id_Vendedor = dbo.tbl_Vendedor.Id_Vendedor 
where dbo.tbl_AlmacenVendedor.E_Eliminado=0 
and dbo.dateonly(dbo.tbl_AlmacenVendedor.Fecha) between dbo.dateonly(@Fecha) and dbo.dateonly(@FechaFin)
order by dbo.tbl_Ruta.id_ruta 


GO

/* [dbo].[sp_TraerTodosIngresos_RangoFecha] */
CREATE proc [dbo].[sp_TraerTodosIngresos_RangoFecha](@FechaInicio datetime, @FechaFin datetime)
as 
select i.Id_IngresoAlmacen,i.Id_Usuario, u.Nombre,i.Proveedor,i.Fecha,i.Fecha_Registro, i.Observacion,i.Total
from tbl_IngresoAlmacen i, tbl_Usuario u
where u.Id_Usuario=i.Id_Usuario and i.E_Eliminado=0 and 
dbo.DateOnly(i.fecha) between dbo.DateOnly(@FechaInicio) and dbo.DateOnly(@FechaFin)
order by i.Id_IngresoAlmacen desc
GO

/* [dbo].[sp_TraerTodosIngresosMET_RangoFecha] */
create proc [dbo].[sp_TraerTodosIngresosMET_RangoFecha](@FechaInicio datetime, @FechaFin datetime)
as 
select i.Id_IngresoMaterialTigo,u.Nombre Usuario,i.Proveedor,i.FechaEntregaTigo,i.FechaIngreso,i.NroComprobante, i.Observacion,
case when estadoingresocompleto = 0 then 'Incompleto' else 'Completo' end Estado
from tbl_ingresomaterialtigo i, tbl_Usuario u
where u.Id_Usuario=i.Id_Usuario and i.E_Eliminado=0 and 
dbo.DateOnly(i.FechaEntregaTigo) between dbo.DateOnly(@FechaInicio) and dbo.DateOnly(@FechaFin)
order by i.Id_IngresoMaterialTigo desc
GO

/* [dbo].[sp_TraerTodosLosCuadres] */


CREATE proc [dbo].[sp_TraerTodosLosCuadres] (@fecha datetime)
as 
	SELECT c.Id_Cuadre, c.Id_Ruta, r.Nombre, c.Fecha,c.Observacion, c.Total,  
	c.E_Eliminado ,v.Id_Vendedor, v.Nombre as NombreVendedor
	FROM   dbo.tbl_Cuadre c, dbo.tbl_Ruta r, tbl_Vendedor v	
	WHERE (c.E_Eliminado = 0) AND dbo.dateonly(Fecha)=dbo.dateonly(@fecha) 
	and c.Id_Ruta = r.Id_Ruta and c.Id_Vendedor = v.Id_Vendedor


GO

/* [dbo].[sp_TraerTodosLosCuadres_RangoFecha] */


CREATE proc [dbo].[sp_TraerTodosLosCuadres_RangoFecha] (@FechaInicio datetime,@FechaFin datetime,@Id_Ruta int)
as 
	SELECT c.Id_Cuadre, c.Id_Ruta, r.Nombre, c.Fecha,c.Observacion, c.Total,  
	c.E_Eliminado ,v.Id_Vendedor, v.Nombre as NombreVendedor
	FROM   dbo.tbl_Cuadre c, dbo.tbl_Ruta r, tbl_Vendedor v	
	WHERE (c.E_Eliminado = 0) 
	AND dbo.dateonly(Fecha) between dbo.dateonly(@FechaInicio) and dbo.dateonly(@FechaFin)
	and c.Id_Ruta = r.Id_Ruta and c.Id_Vendedor = v.Id_Vendedor
	--and v.Id_Vendedor = @Id_Vendedor
	and r.Id_Ruta = @Id_Ruta
	


GO

/* [dbo].[sp_TraerTodosLosProductosMascara] */
CREATE proc  [dbo].[sp_TraerTodosLosProductosMascara]--46
 as
  select m.Id_producto,m.Mascara,p.Nombre,p.DigitosImei,MascaraChipID,p.DigitosChipId
   from tbl_ProductosMascaras m 
        inner join tbl_Producto p on  m.Id_producto = p.Id_Producto and
		m.E_eliminado = 0 and p.E_Eliminado = 0
		and (p.digitosimei>0 or p.digitoschipid>0)
 
 
 --select * from tbl_Producto
GO

/* [dbo].[sp_TraerVentaDiaRuta] */
CREATE proc [dbo].[sp_TraerVentaDiaRuta](@Id_Ruta int, @Fecha datetime)
as
SELECT p.Id_Producto,v.Id_ruta,SUM(cv.Cantidad)Venta,p.Nombre
FROM         dbo.tbl_Venta v INNER JOIN
dbo.tbl_CodigoVenta cv ON v.Id_Venta = cv.Id_Venta INNER JOIN
dbo.tbl_Producto p ON cv.Id_Producto = p.Id_Producto
and v.E_Eliminado = 0 and cv.E_Eliminado = 0 and cv.Id_TipoMaterial in (1,3)
and dbo.DateOnly(v.Fecha_Ejecucion)=dbo.DateOnly(@Fecha)
AND V.Id_ruta= @Id_Ruta
group by p.Id_Producto,v.Id_ruta,p.Nombre

GO

/* [dbo].[sp_TraerVentaDiaRuta_CantOt] */

create proc [dbo].[sp_TraerVentaDiaRuta_CantOt](@Id_Ruta int, @Fecha datetime)
as
select count(id_venta) CantOrden
from tbl_venta 
where id_ruta=@Id_Ruta and dbo.dateonly(fecha_ejecucion)=dbo.dateonly(@Fecha) and e_eliminado=0


GO

/* [dbo].[sp_upgraddiagrams] */

	CREATE PROCEDURE dbo.sp_upgraddiagrams
	AS
	BEGIN
		IF OBJECT_ID(N'dbo.sysdiagrams') IS NOT NULL
			return 0;
	
		CREATE TABLE dbo.sysdiagrams
		(
			name sysname NOT NULL,
			principal_id int NOT NULL,	-- we may change it to varbinary(85)
			diagram_id int PRIMARY KEY IDENTITY,
			version int,
	
			definition varbinary(max)
			CONSTRAINT UK_principal_name UNIQUE
			(
				principal_id,
				name
			)
		);


		/* Add this if we need to have some form of extended properties for diagrams */
		/*
		IF OBJECT_ID(N'dbo.sysdiagram_properties') IS NULL
		BEGIN
			CREATE TABLE dbo.sysdiagram_properties
			(
				diagram_id int,
				name sysname,
				value varbinary(max) NOT NULL
			)
		END
		*/

		IF OBJECT_ID(N'dbo.dtproperties') IS NOT NULL
		begin
			insert into dbo.sysdiagrams
			(
				[name],
				[principal_id],
				[version],
				[definition]
			)
			select	 
				convert(sysname, dgnm.[uvalue]),
				DATABASE_PRINCIPAL_ID(N'dbo'),			-- will change to the sid of sa
				0,							-- zero for old format, dgdef.[version],
				dgdef.[lvalue]
			from dbo.[dtproperties] dgnm
				inner join dbo.[dtproperties] dggd on dggd.[property] = 'DtgSchemaGUID' and dggd.[objectid] = dgnm.[objectid]	
				inner join dbo.[dtproperties] dgdef on dgdef.[property] = 'DtgSchemaDATA' and dgdef.[objectid] = dgnm.[objectid]
				
			where dgnm.[property] = 'DtgSchemaNAME' and dggd.[uvalue] like N'_EA3E6268-D998-11CE-9454-00AA00A3F36E_' 
			return 2;
		end
		return 1;
	END
	
GO

/* [dbo].[sp_usuario_listaractivosbasico] */
CREATE PROCEDURE dbo.SP_Usuario_ListarActivosBasico
AS
BEGIN
    SET NOCOUNT ON;
    SELECT Id_Usuario AS idUsuario, Nombre AS nombre
    FROM dbo.tbl_Usuario
    WHERE ISNULL(E_Eliminado,0)=0;
END

GO

/* [dbo].[sp_VentasExcedente] */


--select * from tbl_Producto where excedente=1

CREATE proc [dbo].[sp_VentasExcedente] (@Id_OrdenVentaRealizada int)
as
BEGIN
	IF OBJECT_ID('dbo.Temporal') IS not NULL
		drop table Temporal;

	declare @contador2 int, @cuantos2 int, @contador int,@cuantos int,@DigitosImei int;
		set @contador = 1;
		set @contador2 = 1;

	declare @strproducto nvarchar(50), @consulta nvarchar(MAX), @consultaInsercion nvarchar(MAX),@NombreProducto nvarchar(50), @NombreProductoAux nvarchar(50);
	set @consulta = 'create table Temporal (Id int identity,Obs nvarchar'
	set @consultaInsercion = 'insert into Temporal values(1'
	declare @Productos table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)
	declare @ProductosAux table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)

--	insert into @ProductosAux 
		--select Id_Producto, Alias, DigitosImei from tbl_Producto where E_Eliminado=0 AND excedente=1 order by Id_Producto


	insert into @ProductosAux 
		select Id_Producto, Alias , DigitosImei from tbl_Producto where E_Eliminado=0 
		and Id_Producto in (select cv.Id_Producto from tbl_CodigoVenta cv where cv.Id_Venta =@Id_OrdenVentaRealizada  and Id_TipoMaterial=3 and cv.E_Eliminado=0)
		order by Id_Producto
		
		
	-------------------creando 5 decodificadores
	set @cuantos = (select COUNT(*) from @ProductosAux);
	
	while(@contador <= @cuantos)
	BEGIN
		
		SET @NombreProducto = (Select nombre from @ProductosAux where Id = @contador);
		set @DigitosImei = (Select DigitosImei from @ProductosAux where Id = @contador);		
				insert into @Productos 
				select Id_Producto, nombre, DigitosImei  from @ProductosAux where Id = @contador
					set @consulta = @consulta +','+ @NombreProducto + ' nvarchar(50)'
					set @consultaInsercion =@consultaInsercion  + ',0'		
		SET @contador = @contador + 1;	
		
	END
		set @consulta = @consulta +')'
		set @consultaInsercion = @consultaInsercion +')'
		exec (@consulta)
		exec (@consultaInsercion)

		--------------------------------------------------------------------------------------------------------------------
		DECLARE @ContadorDecodificador int 
		DECLARE @ContadorAntena int 
		set @ContadorDecodificador =0
		set @ContadorAntena =0
		declare @Materiales table (id int identity ,Id_Producto int, Nombre nvarchar(100),Dato nvarchar(50))
		insert into @Materiales
		
			select p.Id_Producto,p.Alias,Cod_Inicio from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio<>'' and Id_TipoMaterial in (3 ,4) and cv.E_Eliminado=0  order by Nombre
		insert into @Materiales
			select p.Id_Producto,p.Alias, Convert(nvarchar, Cantidad) from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio=''and Id_TipoMaterial in (3 ,4)  and cv.E_Eliminado=0 order by Nombre
		--select * from @Materiales
		
				set @cuantos = (select COUNT(*) from @Materiales)
				set @contador = 1;
				set @contador2 = 1;
					while (@contador <=@cuantos)
					BEGIN		
						set @NombreProducto = (select nombre from @Materiales where id=@contador)
						print 'nombre producto ' +@NombreProducto	
								set @consulta = 'update Temporal set ' 
								set @consulta = @consulta + @NombreProducto + ' = ' 
								set @consulta = @consulta + (select Dato from @Materiales where id = @contador)  
								set @consulta = @consulta + ' where id = 1';
								exec (@consulta);
								print '3' +@consulta
								SET @contador = @contador+1
					END
				
					SELECT * FROM TEMPORAL
				END
							
						
						

GO

/* [dbo].[sp_VentasExcedenteCopia] */



--select * from tbl_Producto where excedente=1

create proc [dbo].[sp_VentasExcedenteCopia] (@Id_OrdenVentaRealizada int)
as
BEGIN
	IF OBJECT_ID('dbo.Temporal') IS not NULL
		drop table Temporal;

	declare @contador2 int, @cuantos2 int, @contador int,@cuantos int,@DigitosImei int;
		set @contador = 1;
		set @contador2 = 1;

	declare @strproducto nvarchar(50), @consulta nvarchar(MAX), @consultaInsercion nvarchar(MAX),@NombreProducto nvarchar(50), @NombreProductoAux nvarchar(50);
	set @consulta = 'create table Temporal (Id int identity,Obs nvarchar'
	set @consultaInsercion = 'insert into Temporal values(1'
	declare @Productos table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)
	declare @ProductosAux table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)

--	insert into @ProductosAux 
		--select Id_Producto, Alias, DigitosImei from tbl_Producto where E_Eliminado=0 AND excedente=1 order by Id_Producto


	insert into @ProductosAux 
		select Id_Producto, Alias , DigitosImei from tbl_Producto where E_Eliminado=0 
		and Id_Producto in (select cv.Id_Producto from tbl_CodigoVenta cv where cv.Id_Venta =@Id_OrdenVentaRealizada  and Id_TipoMaterial=3 and cv.E_Eliminado=0)
		order by Id_Producto
		
		
	-------------------creando 5 decodificadores
	set @cuantos = (select COUNT(*) from @ProductosAux);
	
	while(@contador <= @cuantos)
	BEGIN
		
		SET @NombreProducto = (Select nombre from @ProductosAux where Id = @contador);
		set @DigitosImei = (Select DigitosImei from @ProductosAux where Id = @contador);		
				insert into @Productos 
				select Id_Producto, nombre, DigitosImei  from @ProductosAux where Id = @contador
					set @consulta = @consulta +','+ @NombreProducto + ' nvarchar(50)'
					set @consultaInsercion =@consultaInsercion  + ',0'		
		SET @contador = @contador + 1;	
		
	END
		set @consulta = @consulta +')'
		set @consultaInsercion = @consultaInsercion +')'
		exec (@consulta)
		exec (@consultaInsercion)

		--------------------------------------------------------------------------------------------------------------------
		DECLARE @ContadorDecodificador int 
		DECLARE @ContadorAntena int 
		set @ContadorDecodificador =0
		set @ContadorAntena =0
		declare @Materiales table (id int identity ,Id_Producto int, Nombre nvarchar(100),Dato nvarchar(50))
		insert into @Materiales
		
			select p.Id_Producto,p.Alias,Cod_Inicio from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio<>'' and Id_TipoMaterial in (3 ,4) and cv.E_Eliminado=0  order by Nombre
		insert into @Materiales
			select p.Id_Producto,p.Alias, Convert(nvarchar, Cantidad) from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio=''and Id_TipoMaterial in (3 ,4)  and cv.E_Eliminado=0 order by Nombre
		--select * from @Materiales
		
				set @cuantos = (select COUNT(*) from @Materiales)
				set @contador = 1;
				set @contador2 = 1;
					while (@contador <=@cuantos)
					BEGIN		
						set @NombreProducto = (select nombre from @Materiales where id=@contador)
						print 'nombre producto ' +@NombreProducto	
								set @consulta = 'update Temporal set ' 
								set @consulta = @consulta + @NombreProducto + ' = ' 
								set @consulta = @consulta + (select Dato from @Materiales where id = @contador)  
								set @consulta = @consulta + ' where id = 1';
								exec (@consulta);
								print '3' +@consulta
								SET @contador = @contador+1
					END
				
					SELECT * FROM TEMPORAL
				END
							
						
						


GO

/* [dbo].[sp_VentasInstalado] */
CREATE proc [dbo].[sp_VentasInstalado] (@Id_OrdenVentaRealizada int)
as
BEGIN
	IF OBJECT_ID('dbo.Temporal') IS not NULL
		drop table Temporal;

	declare @contador2 int, @cuantos2 int, @contador int,@cuantos int,@DigitosImei int,@CantDeco int ,@CantDeco_Crear int;
		set @contador = 1;
		set @contador2 = 1;

	declare @strproducto nvarchar(50), @consulta nvarchar(MAX), @consultaInsercion nvarchar(MAX),@NombreProducto nvarchar(50), @NombreProductoAux nvarchar(50);
	set @consulta = 'create table Temporal (Id int identity,Obs nvarchar'
	set @consultaInsercion = 'insert into Temporal values(1'
	declare @Productos table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)
	declare @ProductosAux table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)

	insert into @ProductosAux 
		select Id_Producto, Alias , DigitosImei from tbl_Producto where E_Eliminado=0 
		and Id_Producto in (select cv.Id_Producto from tbl_CodigoVenta cv where cv.Id_Venta = @Id_OrdenVentaRealizada and Id_TipoMaterial=1 and cv.E_Eliminado=0)
		order by Alias
		
	-------------------creando 5 decodificadores
	set @cuantos = (select COUNT(*) from @ProductosAux);
	 set @CantDeco=(  select count(*) from (select distinct Nombre from @ProductosAux where Nombre like '%deco%') a);
	 --select distinct Nombre from @ProductosAux where Nombre like '%deco%'
	 print 'Cant Tipo Deco' +cast ( @CantDeco as nvarchar(10) )
	--select * from tbl_Producto
	if(@CantDeco =1)
	   set @CantDeco_Crear = 5;
	else
	   set @CantDeco_Crear = 3;
	 
	while(@contador <= @cuantos)
	BEGIN		
		SET @NombreProducto = (Select nombre from @ProductosAux where Id = @contador);
		set @DigitosImei = (Select DigitosImei from @ProductosAux where Id = @contador);		
		if((@NombreProducto like '%DECODIFICADOR%') )	
		BEGIN
			WHILE (@contador2<@CantDeco_Crear)
			BEGIN
				SET @NombreProductoAux 	= @NombreProducto;				
				set @NombreProductoAux 	= @NombreProductoAux + CONVERT(nvarchar,@contador2);
				
				insert into @Productos 
					select Id_Producto, @NombreProductoAux, DigitosImei  from @ProductosAux where Id = @contador
					
					set @consulta = @consulta +','+ @NombreProductoAux + ' nvarchar(50)'	
					set @consultaInsercion =@consultaInsercion  + ',0'
				set @contador2 = @contador2+1;
			END			
			--set @contador2 = 1
		END
		ELSE
		 BEGIN
		 set @contador2 = 1
		   if(@NombreProducto = 'TARJETA')	
		  BEGIN
			WHILE (@contador2<5)
			BEGIN
				SET @NombreProductoAux 	= @NombreProducto;				
				set @NombreProductoAux 	= @NombreProductoAux + CONVERT(nvarchar,@contador2);
				
				insert into @Productos 
					select Id_Producto, @NombreProductoAux, DigitosImei  from @ProductosAux where Id = @contador
					
					set @consulta = @consulta +','+ @NombreProductoAux + ' nvarchar(50)'	
					set @consultaInsercion =@consultaInsercion  + ',0'
				set @contador2 = @contador2+1;
			END			
			set @contador2 = 1

		END
		
           ELSE
            BEGIN
				insert into @Productos 
				select Id_Producto, nombre, DigitosImei  from @ProductosAux where Id = @contador
					set @consulta = @consulta +','+ @NombreProducto + ' nvarchar(50)'
					set @consultaInsercion =@consultaInsercion  + ',0'
			END		
		END
		SET @contador = @contador + 1;	
		
	--	set @consultaInsercion =@consultaInsercion  + ','
	END
		set @consulta = @consulta +')'
		set @consultaInsercion = @consultaInsercion +')'
		exec (@consulta)
		exec (@consultaInsercion)

		--------------------------------------------------------------------------------------------------------------------
		DECLARE @ContadorDecodificador int 
		DECLARE @ContadorAntena int 
		set @ContadorDecodificador =0
		set @ContadorAntena =0
		declare @Materiales table (id int identity ,Id_Producto int, Nombre nvarchar(100),Dato nvarchar(50))
		insert into @Materiales
			select p.Id_Producto,p.Alias,Cod_Inicio from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio<>'' and Id_TipoMaterial=1 and cv.E_Eliminado=0  order by Nombre
		insert into @Materiales
			select p.Id_Producto,p.Alias, Convert(nvarchar, Cantidad) from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio=''and Id_TipoMaterial=1  and cv.E_Eliminado=0 order by Nombre
		
		--select * from @Materiales
		
				set @cuantos = (select COUNT(*) from @Materiales)
				set @contador = 1;
				set @contador2 = 1;
					while (@contador <=@cuantos)
					BEGIN		
						set @NombreProducto = (select nombre from @Materiales where id=@contador)
						print 'nombre producto ' +@NombreProducto	
						IF(@NombreProducto like '%deco%'  OR @NombreProducto = 'TARJETA' )					
						BEGIN
							IF(@NombreProducto like '%deco%' )
							BEGIN
								   print 'Entre por deco'+@NombreProductoAux
												set @ContadorDecodificador = @ContadorDecodificador + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorDecodificador);							
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux + ' = ' 
												set @consulta = @consulta + CHAR(39)+(select Dato from @Materiales where Id_Producto in( 1,28) and id = @contador ) + CHAR(39)
												set @consulta = @consulta + ' where id = 1' 															
												--set @contador2 = @contador2 + 1;																				
												PRINT 	' 1'	+@consulta
												exec (@consulta)													
												PRINT @contador										
											set @contador = @contador + 1
							END
							ELSE
							  BEGIN
							  IF(@NombreProducto = 'TARJETA')
							   BEGIN
									--set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'TARJETA')
									--set @contador2 = 1
											--WHILE (@contador2 <= @cuantos2)
											--BEGIN
												set @ContadorAntena = @ContadorAntena + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorAntena);
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux +  ' = ' 
												set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where Id_Producto = 2 and id = @contador) + CHAR(39)
												set @consulta = @consulta + ' where id = 1'											
												exec (@consulta)												
												PRINT 	' 2'	+@consulta
												PRINT @contador
												set @contador = @contador + 1		
														
							  END	
							end
						END
						ELSE
						BEGIN
								set @consulta = 'update Temporal set ' 
								set @consulta = @consulta + @NombreProducto + ' = ' 
								set @consulta = @consulta + CHAR(39) +(select Dato from @Materiales where id = @contador)+ CHAR(39)  
								set @consulta = @consulta + ' where id = 1';								
								print '3--' +@consulta
								exec (@consulta);
								SET @contador = @contador+1
						END
					END
				
				IF EXISTS( SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
                           WHERE TABLE_NAME = 'TEMPORAL' 
                           AND  COLUMN_NAME = 'DECODIFICADOR_60001')            
				   EXEC sp_rename 'Temporal.DECODIFICADOR_60001', 'DECODIFICADOR1';
				
				IF EXISTS( SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
                           WHERE TABLE_NAME = 'TEMPORAL' 
                           AND  COLUMN_NAME = 'DECODIFICADOR_60002')                            					
                    EXEC sp_rename 'Temporal.DECODIFICADOR_60002', 'DECODIFICADOR2';
                    
                IF EXISTS( SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
                           WHERE TABLE_NAME = 'TEMPORAL' 
                           AND  COLUMN_NAME = 'DECODIFICADOR_60003')                             
                    EXEC sp_rename 'Temporal.DECODIFICADOR_60003', 'DECODIFICADOR3';
                    
                IF EXISTS( SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
                           WHERE TABLE_NAME = 'TEMPORAL' 
                           AND  COLUMN_NAME = 'DECODIFICADOR_60004')
                    EXEC sp_rename 'Temporal.DECODIFICADOR_60004', 'DECODIFICADOR4';
    
					SELECT * FROM TEMPORAL
				END
													



GO

/* [dbo].[sp_VentasInstalado_cOPIA] */


--[sp_VentasInstalado] 1
CREATE proc [dbo].[sp_VentasInstalado_cOPIA] (@Id_OrdenVentaRealizada int)
as
BEGIN
	IF OBJECT_ID('dbo.Temporal') IS not NULL
		drop table Temporal;

	declare @contador2 int, @cuantos2 int, @contador int,@cuantos int,@DigitosImei int;
		set @contador = 1;
		set @contador2 = 1;

	declare @strproducto nvarchar(50), @consulta nvarchar(MAX), @consultaInsercion nvarchar(MAX),@NombreProducto nvarchar(50), @NombreProductoAux nvarchar(50);
	set @consulta = 'create table Temporal (Id int identity,Obs nvarchar'
	set @consultaInsercion = 'insert into Temporal values(1'
	declare @Productos table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)
	declare @ProductosAux table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)

	insert into @ProductosAux 
		select Id_Producto, Alias , DigitosImei from tbl_Producto where E_Eliminado=0 
		and Id_Producto in (select cv.Id_Producto from tbl_CodigoVenta cv where cv.Id_Venta = @Id_OrdenVentaRealizada and Id_TipoMaterial=1 and cv.E_Eliminado=0)
		order by Id_Producto


			
			
			
			
	-------------------creando 5 decodificadores
	set @cuantos = (select COUNT(*) from @ProductosAux);
	
	while(@contador <= @cuantos)
	BEGIN
		
		SET @NombreProducto = (Select nombre from @ProductosAux where Id = @contador);
		set @DigitosImei = (Select DigitosImei from @ProductosAux where Id = @contador);		
		if((@NombreProducto = 'DECODIFICADOR') or (@NombreProducto = 'TARJETA'))	
		BEGIN
			WHILE (@contador2<5)
			BEGIN
				SET @NombreProductoAux 	= @NombreProducto;				
				set @NombreProductoAux 	= @NombreProductoAux + CONVERT(nvarchar,@contador2);
				
				insert into @Productos 
					select Id_Producto, @NombreProductoAux, DigitosImei  from @ProductosAux where Id = @contador
					
					set @consulta = @consulta +','+ @NombreProductoAux + ' nvarchar(50)'	
					set @consultaInsercion =@consultaInsercion  + ',0'
				set @contador2 = @contador2+1;
			END
			set @contador2 = 1
		END
		ELSE
		BEGIN
				insert into @Productos 
				select Id_Producto, nombre, DigitosImei  from @ProductosAux where Id = @contador
					set @consulta = @consulta +','+ @NombreProducto + ' nvarchar(50)'
					set @consultaInsercion =@consultaInsercion  + ',0'
		END
		SET @contador = @contador + 1;	
		
	--	set @consultaInsercion =@consultaInsercion  + ','
	END
		set @consulta = @consulta +')'
		set @consultaInsercion = @consultaInsercion +')'
		exec (@consulta)
		exec (@consultaInsercion)

		--------------------------------------------------------------------------------------------------------------------
		DECLARE @ContadorDecodificador int 
		DECLARE @ContadorAntena int 
		set @ContadorDecodificador =0
		set @ContadorAntena =0
		declare @Materiales table (id int identity ,Id_Producto int, Nombre nvarchar(100),Dato nvarchar(50))
		insert into @Materiales
			select p.Id_Producto,p.Alias,Cod_Inicio from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio<>'' and Id_TipoMaterial=1 and cv.E_Eliminado=0  order by Nombre
		insert into @Materiales
			select p.Id_Producto,p.Alias, Convert(nvarchar, Cantidad) from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio=''and Id_TipoMaterial=1  and cv.E_Eliminado=0 order by Nombre
		--select * from @Materiales
		
				set @cuantos = (select COUNT(*) from @Materiales)
				set @contador = 1;
				set @contador2 = 1;
					while (@contador <=@cuantos)
					BEGIN		
						set @NombreProducto = (select nombre from @Materiales where id=@contador)
						print 'nombre producto ' +@NombreProducto	
						IF(@NombreProducto = 'DECODIFICADOR'  OR @NombreProducto = 'TARJETA' )					
						BEGIN
							IF(@NombreProducto = 'DECODIFICADOR' )
							BEGIN
								--	set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'DECODIFICADOR')																		
								--	set @contador2 = 1
											--WHILE (@contador2 <= @cuantos2)
											--BEGIN
												set @ContadorDecodificador = @ContadorDecodificador + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorDecodificador);							
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux + ' = ' 
												set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where Id_Producto = 1 and id = @contador ) + CHAR(39)
												set @consulta = @consulta + ' where id = 1' 															
												--set @contador2 = @contador2 + 1;																				
												exec (@consulta)	
												PRINT 	' 1'	+@consulta
												PRINT @contador
										
											set @contador = @contador + 1
							END
							IF(@NombreProducto = 'TARJETA')
							BEGIN
									--set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'TARJETA')
									--set @contador2 = 1
											--WHILE (@contador2 <= @cuantos2)
											--BEGIN
												set @ContadorAntena = @ContadorAntena + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorAntena);
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux +  ' = ' 
												set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where Id_Producto = 2 and id = @contador) + CHAR(39)
												set @consulta = @consulta + ' where id = 1'											
												exec (@consulta)												
												PRINT 	' 2'	+@consulta
												PRINT @contador
												set @contador = @contador + 1		
														
							END	
						END
						ELSE
						BEGIN
								set @consulta = 'update Temporal set ' 
								set @consulta = @consulta + @NombreProducto + ' = ' 
								set @consulta = @consulta + (select Dato from @Materiales where id = @contador)  
								set @consulta = @consulta + ' where id = 1';
								exec (@consulta);
								print '3' +@consulta
								SET @contador = @contador+1
						END
					END
				
					SELECT * FROM TEMPORAL
				END
							
						
							--[sp_VentasHorizontal] 2
							--IF(@NombreProducto = 'TARJETA' )
							--BEGIN
							--		set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'TARJETA')
							--		set @contador2 = 1
							--				WHILE (@contador2 <= @cuantos2)
							--				BEGIN
							--					set @NombreProductoAux = @NombreProducto;				
							--					set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@contador2);							
							--					set @consulta = 'update Temporal set ' 
							--					set @consulta = @consulta + @NombreProductoAux + ' = ' 
							--					set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where id = @contador) + CHAR(39)
							--					set @consulta = @consulta + ' where id = 2' 															
							--					set @contador2 = @contador2 + 1;								
							--					exec (@consulta)												
							--				END
							--				WHILE (@contador2 <= 4)
							--				BEGIN												
							--					set @consulta = 'update Temporal set ' 
							--					set @consulta = @consulta + @NombreProductoAux + ' = 0 ' 												
							--					set @consulta = @consulta + ' where id = 2' 															
							--					set @contador2 = @contador2 + 1;								
							--					exec (@consulta)												
							--				END
											
							--				SET @contador = @contador+1
							--END
						--END	
						--ELSE
						--BEGIN	
						--		set @consulta = 'update Temporal set ' 
						--		set @consulta = @consulta + @NombreProducto + ' = ' 
						--		set @consulta = @consulta + (select Dato from @Materiales where id = @contador)  
						--		set @consulta = @consulta + 'where id = 2' ;																					
						--		exec (@consulta);
						--		print @consulta
						--		SET @contador = @contador+1
						--END
						
						
--					END
--					SELECT * FROM TEMPORAL
--END




GO

/* [dbo].[sp_VentasInstalado1] */


CREATE proc [dbo].[sp_VentasInstalado1] (@Id_OrdenVentaRealizada int)
as
BEGIN
	IF OBJECT_ID('dbo.Temporal') IS not NULL
		drop table Temporal;

	declare @contador2 int, @cuantos2 int, @contador int,@cuantos int,@DigitosImei int,@CantDeco int ,@CantDeco_Crear int;
		set @contador = 1;
		set @contador2 = 1;

	declare @strproducto nvarchar(50), @consulta nvarchar(MAX), @consultaInsercion nvarchar(MAX),@NombreProducto nvarchar(50), @NombreProductoAux nvarchar(50);
	set @consulta = 'create table Temporal (Id int identity,Obs nvarchar'
	set @consultaInsercion = 'insert into Temporal values(1'
	declare @Productos table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)
	declare @ProductosAux table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)

	insert into @ProductosAux 
		select Id_Producto, Alias , DigitosImei from tbl_Producto where E_Eliminado=0 
		and Id_Producto in (select cv.Id_Producto from tbl_CodigoVenta cv where cv.Id_Venta = @Id_OrdenVentaRealizada and Id_TipoMaterial=1 and cv.E_Eliminado=0)
		order by Alias
		
	-------------------creando 5 decodificadores
	set @cuantos = (select COUNT(*) from @ProductosAux);
	 set @CantDeco=(  select count(*) from (select distinct Nombre from @ProductosAux where Nombre like '%deco%') a);
	 --select distinct Nombre from @ProductosAux where Nombre like '%deco%'
	 print 'Cant Tipo Deco' +cast ( @CantDeco as nvarchar(10) )
	--select * from tbl_Producto
	if(@CantDeco =1)
	   set @CantDeco_Crear = 5;
	else
	   set @CantDeco_Crear = 3;
	 
	while(@contador <= @cuantos)
	BEGIN		
		SET @NombreProducto = (Select nombre from @ProductosAux where Id = @contador);
		set @DigitosImei = (Select DigitosImei from @ProductosAux where Id = @contador);		
		if((@NombreProducto like '%DECODIFICADOR%') )	
		BEGIN
			WHILE (@contador2<@CantDeco_Crear)
			BEGIN
				SET @NombreProductoAux 	= @NombreProducto;				
				set @NombreProductoAux 	= @NombreProductoAux + CONVERT(nvarchar,@contador2);
				
				insert into @Productos 
					select Id_Producto, @NombreProductoAux, DigitosImei  from @ProductosAux where Id = @contador
					
					set @consulta = @consulta +','+ @NombreProductoAux + ' nvarchar(50)'	
					set @consultaInsercion =@consultaInsercion  + ',0'
				set @contador2 = @contador2+1;
			END			
			--set @contador2 = 1
		END
		ELSE
		 BEGIN
		 set @contador2 = 1
		   if(@NombreProducto = 'TARJETA')	
		  BEGIN
			WHILE (@contador2<5)
			BEGIN
				SET @NombreProductoAux 	= @NombreProducto;				
				set @NombreProductoAux 	= @NombreProductoAux + CONVERT(nvarchar,@contador2);
				
				insert into @Productos 
					select Id_Producto, @NombreProductoAux, DigitosImei  from @ProductosAux where Id = @contador
					
					set @consulta = @consulta +','+ @NombreProductoAux + ' nvarchar(50)'	
					set @consultaInsercion =@consultaInsercion  + ',0'
				set @contador2 = @contador2+1;
			END			
			set @contador2 = 1

		END
		
           ELSE
            BEGIN
				insert into @Productos 
				select Id_Producto, nombre, DigitosImei  from @ProductosAux where Id = @contador
					set @consulta = @consulta +','+ @NombreProducto + ' nvarchar(50)'
					set @consultaInsercion =@consultaInsercion  + ',0'
			END		
		END
		SET @contador = @contador + 1;	
		
	--	set @consultaInsercion =@consultaInsercion  + ','
	END
		set @consulta = @consulta +')'
		set @consultaInsercion = @consultaInsercion +')'
		exec (@consulta)
		exec (@consultaInsercion)

		--------------------------------------------------------------------------------------------------------------------
		DECLARE @ContadorDecodificador int 
		DECLARE @ContadorAntena int 
		set @ContadorDecodificador =0
		set @ContadorAntena =0
		declare @Materiales table (id int identity ,Id_Producto int, Nombre nvarchar(100),Dato nvarchar(50))
		insert into @Materiales
			select p.Id_Producto,p.Alias,Cod_Inicio from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio<>'' and Id_TipoMaterial=1 and cv.E_Eliminado=0  order by Nombre
		insert into @Materiales
			select p.Id_Producto,p.Alias, Convert(nvarchar, Cantidad) from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio=''and Id_TipoMaterial=1  and cv.E_Eliminado=0 order by Nombre
		
		--select * from @Materiales
		
				set @cuantos = (select COUNT(*) from @Materiales)
				set @contador = 1;
				set @contador2 = 1;
					while (@contador <=@cuantos)
					BEGIN		
						set @NombreProducto = (select nombre from @Materiales where id=@contador)
						print 'nombre producto ' +@NombreProducto	
						IF(@NombreProducto like '%deco%'  OR @NombreProducto = 'TARJETA' )					
						BEGIN
							IF(@NombreProducto like '%deco%' )
							BEGIN
								   print 'Entre por deco'+@NombreProductoAux
												set @ContadorDecodificador = @ContadorDecodificador + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorDecodificador);							
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux + ' = ' 
												set @consulta = @consulta + CHAR(39)+(select Dato from @Materiales where Id_Producto in( 1,28) and id = @contador ) + CHAR(39)
												set @consulta = @consulta + ' where id = 1' 															
												--set @contador2 = @contador2 + 1;																				
												PRINT 	' 1'	+@consulta
												exec (@consulta)													
												PRINT @contador										
											set @contador = @contador + 1
							END
							IF(@NombreProducto = 'TARJETA')
							BEGIN
									--set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'TARJETA')
									--set @contador2 = 1
											--WHILE (@contador2 <= @cuantos2)
											--BEGIN
												set @ContadorAntena = @ContadorAntena + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorAntena);
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux +  ' = ' 
												set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where Id_Producto = 2 and id = @contador) + CHAR(39)
												set @consulta = @consulta + ' where id = 1'											
												exec (@consulta)												
												PRINT 	' 2'	+@consulta
												PRINT @contador
												set @contador = @contador + 1		
														
							END	
						END
						ELSE
						BEGIN
								set @consulta = 'update Temporal set ' 
								set @consulta = @consulta + @NombreProducto + ' = ' 
								set @consulta = @consulta + (select Dato from @Materiales where id = @contador)  
								set @consulta = @consulta + ' where id = 1';
								exec (@consulta);
								print '3' +@consulta
								SET @contador = @contador+1
						END
					END
				
					SELECT * FROM TEMPORAL
				END
													

GO

/* [dbo].[sp_VentasRetirado] */
CREATE proc [dbo].[sp_VentasRetirado] (@Id_OrdenVentaRealizada int)
as
BEGIN
	IF OBJECT_ID('dbo.Temporal') IS not NULL
		drop table Temporal;

	declare @contador2 int, @cuantos2 int, @contador int,@cuantos int,@DigitosImei int,@CantDeco int ,@CantDeco_Crear int;;
		set @contador = 1;
		set @contador2 = 1;

	declare @strproducto nvarchar(50), @consulta nvarchar(MAX), @consultaInsercion nvarchar(MAX),@NombreProducto nvarchar(50), @NombreProductoAux nvarchar(50);
	set @consulta = 'create table Temporal (Id int identity,Obs nvarchar'
	set @consultaInsercion = 'insert into Temporal values(1'
	declare @Productos table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)
	declare @ProductosAux table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)


	insert into @ProductosAux 
		select Id_Producto, Alias , DigitosImei from tbl_Producto where E_Eliminado=0 
		and Id_Producto in (select cv.Id_Producto from tbl_CodigoVenta cv where cv.Id_Venta =@Id_OrdenVentaRealizada  and Id_TipoMaterial=2 and cv.E_Eliminado=0)
		order by Id_Producto
		

	-------------------creando 5 decodificadores
	set @cuantos = (select COUNT(*) from @ProductosAux);
	 set @CantDeco=(  select count(*) from (select distinct Nombre from @ProductosAux where Nombre like '%deco%') a);	 
	 print 'Cant Tipo Deco' +cast ( @CantDeco as nvarchar(10) )

	if(@CantDeco =1)
	   set @CantDeco_Crear = 5;
	else
	   set @CantDeco_Crear = 3;
	
	while(@contador <= @cuantos)
	BEGIN
		
		SET @NombreProducto = (Select nombre from @ProductosAux where Id = @contador);
		set @DigitosImei = (Select DigitosImei from @ProductosAux where Id = @contador);		
			if((@NombreProducto like '%DECODIFICADOR%') )	
		BEGIN
			WHILE (@contador2<@CantDeco_Crear)
			BEGIN
				SET @NombreProductoAux 	= @NombreProducto;				
				set @NombreProductoAux 	= @NombreProductoAux + CONVERT(nvarchar,@contador2);
				
				insert into @Productos 
					select Id_Producto, @NombreProductoAux, DigitosImei  from @ProductosAux where Id = @contador
					
					set @consulta = @consulta +','+ @NombreProductoAux + ' nvarchar(50)'	
					set @consultaInsercion =@consultaInsercion  + ',0'
				set @contador2 = @contador2+1;
			END
			--set @contador2 = 1
	END
		ELSE
		 BEGIN
		 set @contador2 = 1
		   if(@NombreProducto = 'TARJETA')	
		  BEGIN
			WHILE (@contador2<5)
			BEGIN
				SET @NombreProductoAux 	= @NombreProducto;				
				set @NombreProductoAux 	= @NombreProductoAux + CONVERT(nvarchar,@contador2);
				
				insert into @Productos 
					select Id_Producto, @NombreProductoAux, DigitosImei  from @ProductosAux where Id = @contador
					
					set @consulta = @consulta +','+ @NombreProductoAux + ' nvarchar(50)'	
					set @consultaInsercion =@consultaInsercion  + ',0'
				set @contador2 = @contador2+1;
			END			
			set @contador2 = 1

		END
		
           ELSE
            BEGIN
				insert into @Productos 
				select Id_Producto, nombre, DigitosImei  from @ProductosAux where Id = @contador
					set @consulta = @consulta +','+ @NombreProducto + ' nvarchar(50)'
					set @consultaInsercion =@consultaInsercion  + ',0'
			END		
		END
		SET @contador = @contador + 1;	
	END
		set @consulta = @consulta +')'
		set @consultaInsercion = @consultaInsercion +')'
		exec (@consulta)
		exec (@consultaInsercion)

		--------------------------------------------------------------------------------------------------------------------
		DECLARE @ContadorDecodificador int 
		DECLARE @ContadorAntena int 
		set @ContadorDecodificador =0
		set @ContadorAntena =0
		declare @Materiales table (id int identity ,Id_Producto int, Nombre nvarchar(100),Dato nvarchar(50))
		insert into @Materiales
			select p.Id_Producto,p.Alias,Cod_Inicio from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio<>'' and Id_TipoMaterial=2 and cv.E_Eliminado=0  order by Nombre
		insert into @Materiales
			select p.Id_Producto,p.Alias, Convert(nvarchar, Cantidad) from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio=''and Id_TipoMaterial=2  and cv.E_Eliminado=0 order by Nombre
		--select * from @Materiales
		
				set @cuantos = (select COUNT(*) from @Materiales)
				set @contador = 1;
				set @contador2 = 1;
					while (@contador <=@cuantos)
					BEGIN		
						set @NombreProducto = (select nombre from @Materiales where id=@contador)
						print 'nombre producto ' +@NombreProducto	
						IF(@NombreProducto like '%deco%'  OR @NombreProducto = 'TARJETA' )					
						BEGIN
							IF(@NombreProducto like '%deco%' )
							BEGIN
												set @ContadorDecodificador = @ContadorDecodificador + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorDecodificador);							
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux + ' = ' 
												set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where Id_Producto = 1 and id = @contador ) + CHAR(39)
												set @consulta = @consulta + ' where id = 1' 															
												--set @contador2 = @contador2 + 1;																				
												exec (@consulta)	
												PRINT 	' 1'	+@consulta
												PRINT @contador
										
											set @contador = @contador + 1
							  END
							ELSE
							BEGIN
							 IF(@NombreProducto = 'TARJETA')
							  BEGIN
									--set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'TARJETA')
									--set @contador2 = 1
											--WHILE (@contador2 <= @cuantos2)
											--BEGIN
												set @ContadorAntena = @ContadorAntena + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorAntena);
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux +  ' = ' 
												set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where Id_Producto = 2 and id = @contador) + CHAR(39)
												set @consulta = @consulta + ' where id = 1'											
												exec (@consulta)												
												PRINT 	' 2'	+@consulta
												PRINT @contador
												set @contador = @contador + 1																
							  END	
							END
						END
						ELSE
						BEGIN
								set @consulta = 'update Temporal set ' 
								set @consulta = @consulta + @NombreProducto + ' = ' 
								set @consulta = @consulta + CHAR(39) +(select Dato from @Materiales where id = @contador) + CHAR(39) 
								set @consulta = @consulta + ' where id = 1';
								exec (@consulta);
								print '3' +@consulta
								SET @contador = @contador+1
						END
					END
				IF EXISTS( SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
                           WHERE TABLE_NAME = 'TEMPORAL' 
                           AND  COLUMN_NAME = 'DECODIFICADOR_60001')            
				   EXEC sp_rename 'Temporal.DECODIFICADOR_60001', 'DECODIFICADOR1';
				
				IF EXISTS( SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
                           WHERE TABLE_NAME = 'TEMPORAL' 
                           AND  COLUMN_NAME = 'DECODIFICADOR_60002')                            					
                    EXEC sp_rename 'Temporal.DECODIFICADOR_60002', 'DECODIFICADOR2';
                    
                IF EXISTS( SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
                           WHERE TABLE_NAME = 'TEMPORAL' 
                           AND  COLUMN_NAME = 'DECODIFICADOR_60003')                             
                    EXEC sp_rename 'Temporal.DECODIFICADOR_60003', 'DECODIFICADOR3';
                    
                IF EXISTS( SELECT * FROM INFORMATION_SCHEMA.COLUMNS 
                           WHERE TABLE_NAME = 'TEMPORAL' 
                           AND  COLUMN_NAME = 'DECODIFICADOR_60004')
                    EXEC sp_rename 'Temporal.DECODIFICADOR_60004', 'DECODIFICADOR4';
				
					SELECT * FROM TEMPORAL
				END
					

GO

/* [dbo].[sp_VentasRetiradoCopia] */



CREATE proc [dbo].[sp_VentasRetiradoCopia] (@Id_OrdenVentaRealizada int)
as
BEGIN
	IF OBJECT_ID('dbo.Temporal') IS not NULL
		drop table Temporal;

	declare @contador2 int, @cuantos2 int, @contador int,@cuantos int,@DigitosImei int;
		set @contador = 1;
		set @contador2 = 1;

	declare @strproducto nvarchar(50), @consulta nvarchar(MAX), @consultaInsercion nvarchar(MAX),@NombreProducto nvarchar(50), @NombreProductoAux nvarchar(50);
	set @consulta = 'create table Temporal (Id int identity,Obs nvarchar'
	set @consultaInsercion = 'insert into Temporal values(1'
	declare @Productos table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)
	declare @ProductosAux table (id int identity ,Id_Producto int, Nombre nvarchar(100), DigitosImei int)


	insert into @ProductosAux 
		select Id_Producto, Alias , DigitosImei from tbl_Producto where E_Eliminado=0 
		and Id_Producto in (select cv.Id_Producto from tbl_CodigoVenta cv where cv.Id_Venta =@Id_OrdenVentaRealizada  and Id_TipoMaterial=2 and cv.E_Eliminado=0)
		order by Id_Producto
		

	-------------------creando 5 decodificadores
	set @cuantos = (select COUNT(*) from @ProductosAux);
	
	while(@contador <= @cuantos)
	BEGIN
		
		SET @NombreProducto = (Select nombre from @ProductosAux where Id = @contador);
		set @DigitosImei = (Select DigitosImei from @ProductosAux where Id = @contador);		
		if((@NombreProducto = 'DECODIFICADOR') or (@NombreProducto = 'TARJETA'))	
		BEGIN
			WHILE (@contador2<5)
			BEGIN
				SET @NombreProductoAux 	= @NombreProducto;				
				set @NombreProductoAux 	= @NombreProductoAux + CONVERT(nvarchar,@contador2);
				
				insert into @Productos 
					select Id_Producto, @NombreProductoAux, DigitosImei  from @ProductosAux where Id = @contador
					
					set @consulta = @consulta +','+ @NombreProductoAux + ' nvarchar(50)'	
					set @consultaInsercion =@consultaInsercion  + ',0'
				set @contador2 = @contador2+1;
			END
			set @contador2 = 1
		END
		ELSE
		BEGIN
				insert into @Productos 
				select Id_Producto, nombre, DigitosImei  from @ProductosAux where Id = @contador
					set @consulta = @consulta +','+ @NombreProducto + ' nvarchar(50)'
					set @consultaInsercion =@consultaInsercion  + ',0'
		END
		SET @contador = @contador + 1;	
		
	--	set @consultaInsercion =@consultaInsercion  + ','
	END
		set @consulta = @consulta +')'
		set @consultaInsercion = @consultaInsercion +')'
		exec (@consulta)
		exec (@consultaInsercion)

		--------------------------------------------------------------------------------------------------------------------
		DECLARE @ContadorDecodificador int 
		DECLARE @ContadorAntena int 
		set @ContadorDecodificador =0
		set @ContadorAntena =0
		declare @Materiales table (id int identity ,Id_Producto int, Nombre nvarchar(100),Dato nvarchar(50))
		insert into @Materiales
			select p.Id_Producto,p.Alias,Cod_Inicio from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio<>'' and Id_TipoMaterial=2 and cv.E_Eliminado=0  order by Nombre
		insert into @Materiales
			select p.Id_Producto,p.Alias, Convert(nvarchar, Cantidad) from tbl_CodigoVenta cv, tbl_Producto p 
			where cv.Id_Producto = p.Id_Producto and cv.Id_Venta = @Id_OrdenVentaRealizada and cv.Cod_Inicio=''and Id_TipoMaterial=2  and cv.E_Eliminado=0 order by Nombre
		--select * from @Materiales
		
				set @cuantos = (select COUNT(*) from @Materiales)
				set @contador = 1;
				set @contador2 = 1;
					while (@contador <=@cuantos)
					BEGIN		
						set @NombreProducto = (select nombre from @Materiales where id=@contador)
						print 'nombre producto ' +@NombreProducto	
						IF(@NombreProducto = 'DECODIFICADOR'  OR @NombreProducto = 'TARJETA' )					
						BEGIN
							IF(@NombreProducto = 'DECODIFICADOR' )
							BEGIN
								--	set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'DECODIFICADOR')																		
								--	set @contador2 = 1
											--WHILE (@contador2 <= @cuantos2)
											--BEGIN
												set @ContadorDecodificador = @ContadorDecodificador + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorDecodificador);							
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux + ' = ' 
												set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where Id_Producto = 1 and id = @contador ) + CHAR(39)
												set @consulta = @consulta + ' where id = 1' 															
												--set @contador2 = @contador2 + 1;																				
												exec (@consulta)	
												PRINT 	' 1'	+@consulta
												PRINT @contador
										
											set @contador = @contador + 1
							END
							IF(@NombreProducto = 'TARJETA')
							BEGIN
									--set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'TARJETA')
									--set @contador2 = 1
											--WHILE (@contador2 <= @cuantos2)
											--BEGIN
												set @ContadorAntena = @ContadorAntena + 1
												set @NombreProductoAux = @NombreProducto;				
												set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@ContadorAntena);
												set @consulta = 'update Temporal set ' 
												set @consulta = @consulta + @NombreProductoAux +  ' = ' 
												set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where Id_Producto = 2 and id = @contador) + CHAR(39)
												set @consulta = @consulta + ' where id = 1'											
												exec (@consulta)												
												PRINT 	' 2'	+@consulta
												PRINT @contador
												set @contador = @contador + 1		
														
							END	
						END
						ELSE
						BEGIN
								set @consulta = 'update Temporal set ' 
								set @consulta = @consulta + @NombreProducto + ' = ' 
								set @consulta = @consulta + (select Dato from @Materiales where id = @contador)  
								set @consulta = @consulta + ' where id = 1';
								exec (@consulta);
								print '3' +@consulta
								SET @contador = @contador+1
						END
					END
				
					SELECT * FROM TEMPORAL
				END
							
						
							--[sp_VentasHorizontal] 2
							--IF(@NombreProducto = 'TARJETA' )
							--BEGIN
							--		set @cuantos2 = (SELECT COUNT (*) FROM @Materiales where Nombre = 'TARJETA')
							--		set @contador2 = 1
							--				WHILE (@contador2 <= @cuantos2)
							--				BEGIN
							--					set @NombreProductoAux = @NombreProducto;				
							--					set @NombreProductoAux = @NombreProductoAux + CONVERT(nvarchar,@contador2);							
							--					set @consulta = 'update Temporal set ' 
							--					set @consulta = @consulta + @NombreProductoAux + ' = ' 
							--					set @consulta = @consulta + CHAR(39) + (select Dato from @Materiales where id = @contador) + CHAR(39)
							--					set @consulta = @consulta + ' where id = 2' 															
							--					set @contador2 = @contador2 + 1;								
							--					exec (@consulta)												
							--				END
							--				WHILE (@contador2 <= 4)
							--				BEGIN												
							--					set @consulta = 'update Temporal set ' 
							--					set @consulta = @consulta + @NombreProductoAux + ' = 0 ' 												
							--					set @consulta = @consulta + ' where id = 2' 															
							--					set @contador2 = @contador2 + 1;								
							--					exec (@consulta)												
							--				END
											
							--				SET @contador = @contador+1
							--END
						--END	
						--ELSE
						--BEGIN	
						--		set @consulta = 'update Temporal set ' 
						--		set @consulta = @consulta + @NombreProducto + ' = ' 
						--		set @consulta = @consulta + (select Dato from @Materiales where id = @contador)  
						--		set @consulta = @consulta + 'where id = 2' ;																					
						--		exec (@consulta);
						--		print @consulta
						--		SET @contador = @contador+1
						--END
						
						
--					END
--					SELECT * FROM TEMPORAL
--END




GO

/* [dbo].[spb_ObtenerProductos] */
CREATE proc spb_ObtenerProductos(@nombre nvarchar(15))
as
select * from tbl_producto  where Nombre like '%' + @nombre+'%'
GO

/* [dbo].[spb_ObtenerProductos2] */
create proc spb_ObtenerProductos2(@nombre nvarchar(15),@numero int)
as
select * from tbl_producto  where Nombre like '%' + @nombre+'%' and DigitosChipId>@numero
GO

/* [dbo].[spb_saldorutascantidad_x_ruta] */
CREATE PROCEDURE [dbo].[spb_SaldoRutasCantidad_X_Ruta](@Id_Ruta int)
as
select r.nombre Ruta,pr.Nombre Producto,s.Cantidad
from tbl_saldotarjetas s inner join tbl_ruta r on r.id_ruta=s.id_ruta
inner join tbl_producto pr on pr.id_producto=s.id_producto
and  pr.e_eliminado=0 
where r.e_eliminado=0 and r.id_ruta >0 and r.id_ruta =@Id_Ruta
order by r.nombre,pr.Nombre

GO

/* [dbo].[spr_BuscarImei] */

CREATE proc spr_BuscarImei (@cod_inicio nvarchar(50))
--declare @cod_inicio nvarchar(50)
--set @cod_inicio = 'TIC-1313-005332'     
as
select i.Fecha,ci.Cod_Inicio,p.Nombre , 'Ingreso'  Origen,'' TipoMaterial,'0' NRO_ORDEN ,'0'Tipo_servicio
,(select top 1 Sucursal from BDAlmacen..tbl_Ruta ) Sucursal
from tbl_IngresoAlmacen i 
       inner join tbl_CodigoIngresoAlmacen ci on i.Id_IngresoAlmacen = ci.Id_IngresoAlmacen
       inner join tbl_Producto p on ci.Id_Producto = p. Id_Producto 
where ci.Cod_Inicio =@cod_inicio       
union
select i.Fecha,ci.Cod_Inicio,p.Nombre , 'AlmacenVendedor'  Origen , '' TipoMaterial,'0' NRO_ORDEN,'0'Tipo_servicio
,(select top 1 Sucursal from BDAlmacen..tbl_Ruta ) Sucursal
from tbl_AlmacenVendedor i 
       inner join tbl_CodigoAlmacenVendedor ci on i.Id_AlmacenVendedor = ci.Id_AlmacenVendedor
       inner join tbl_Producto p on ci.Id_Producto = p. Id_Producto 
where ci.Cod_Inicio =@cod_inicio       
union
select i.Fecha_Ejecucion,ci.Cod_Inicio,p.Nombre , 'NotaDeVenta'  Origen , tp.Nombre TipoMaterial,i.OrdenTrabajo NRO_ORDEN
, ts.Nombre Tipo_servicio
,(select top 1 Sucursal from BDAlmacen..tbl_Ruta ) Sucursal
from tbl_Venta i 
       inner join tbl_CodigoVenta ci on i.Id_Venta = ci.Id_Venta
       inner join tbl_Producto p on ci.Id_Producto = p. Id_Producto 
       inner join tbl_TipoMaterial tp on tp.Id_TipoMaterial = ci.Id_TipoMaterial
       inner join tbl_TipoServicio ts on ts.Id_TipoServicio = i.Id_TipoServicio
where ci.Cod_Inicio =@cod_inicio       

GO

/* [dbo].[spr_CrearSadoTarjetasRutas] */
create proc spr_CrearSadoTarjetasRutas
as
declare @contador int
declare @cuantos int
declare @contador2 int
declare @cuantos2 int
declare @Id_Producto  int

declare @Id_Ruta int
set @cuantos = 1
set @contador = 1

declare @Indice int
set @Indice = 0
declare @TRuta table(Id int identity, Id_Ruta int )
	insert into @TRuta 
	select Id_Ruta from tbl_Ruta where E_Eliminado = 0
	
	set @cuantos = (select COUNT(*) from @TRuta)	
	
	while (@contador<=@cuantos)
	begin
		print '--------------------'
		set @Id_Ruta = (select id_ruta from @TRuta where Id = @contador )		
		print 'ruta ' + convert(varchar, @Id_Ruta )
		
		declare @ProductosNuevos table(Id int identity, Id_Producto int)				
		
		insert into @ProductosNuevos
		select Id_Producto from tbl_Producto where Id_Producto not in (select Id_Producto from tbl_SaldoTarjetas where Id_Ruta =@Id_Ruta )
		
		select * from @ProductosNuevos
		
		set @cuantos2=(select COUNT (*) from @ProductosNuevos)
		
		print 'cuantos ' + convert(varchar, @cuantos2)
		set @contador2 =1
		while(@contador2 <= @cuantos2)
		begin
		print @contador2 
		set @Indice = @Indice + 1 
			set @Id_Producto = (select id_producto from @ProductosNuevos where Id = @Indice )
			insert into tbl_SaldoTarjetas values (@Id_Producto,@Id_Ruta,0,0)	
			
			set @contador2 = @contador2 +1;
			
		end
		set @contador = @contador +1;
		
		delete from @ProductosNuevos;
	end

GO

/* [dbo].[spr_ObtenerOrdenTrabajo] */

CREATE proc [dbo].[spr_ObtenerOrdenTrabajo] (@id_venta int )
as 
begin
	select v.Id_Venta id_PedidoVendedor,r.Nombre RutaNombre ,	 u.Nombre Usuario,
	 v.Fecha_Ejecucion Fecha,v.Fecha_Registro Fecha_Registro,
     0 Total,	 
	isnull(v.Nombre,(select top 1 Nombre from tbl_Cliente where Id_Cliente =(select Id_Cliente from tbl_OrdenTrabajo where NroOrden = (select OrdenTrabajo from tbl_Venta where Id_Venta = @id_venta) and E_Eliminado= 0))) NombreVendedor,
	ISNULL( v.CodigoCliente,o.CodigoCliente) CI ,
	 v.OrdenTrabajo Telefono ,v.Observacion, 
	 vv.Nombre Observacion1,
	 ts.nombre Observacion2,
	 ISNULL( e.Nombre,(select e.Nombre from tbl_OrdenTrabajo orden inner join tbl_Estado e on o.Id_Estado = e.Id_Estado where orden.Id_OrdenTrabajo =  o.Id_OrdenTrabajo)) Observacion3,
	 ISNULL( s.Sucursal,o.Regional)  Observacion4
	 , case when v.tieneObservacion =0 then 'No' else 'Si' end TieneObservacion
	 
	from tbl_Venta v
	inner join tbl_Ruta  r on r.id_ruta = v.id_ruta
	inner join tbl_tiposervicio ts on v.id_tiposervicio = ts.id_tiposervicio
	inner join tbl_Usuario  u on u.Id_usuario = v.Id_Usuario
	inner join tbl_Vendedor vv on vv.Id_Vendedor = v.Id_Vendedor
	left join tbl_Sucursal  s on  s.Id_Sucursal = v.Id_Sucursal
    left join tbl_Estado e on e.Id_Estado = v.Id_Estado 
    left join tbl_OrdenTrabajo o on o.NroOrden = v.OrdenTrabajo   
	where  v.Id_Venta = @id_venta
end


GO

/* [dbo].[spr_TraerVendedores] */
CREATE proc [dbo].[spr_TraerVendedores]
as
begin
	select v.*, ts.id_Tipo_Solicitante, ts.Nombre TipoSolicitante
	from tbl_Vendedor v inner join tbl_TipoSolicitante ts on ts.id_Tipo_Solicitante = v.id_tiposolicitante
	where v.E_Eliminado = 0  order by Nombre
end
GO

/* [dbo].[spr_TraerVendedores_x_FormTecnico] */
CREATE proc [dbo].[spr_TraerVendedores_x_FormTecnico]
as
begin
	select v.*, ts.id_Tipo_Solicitante, ts.Nombre TipoSolicitante
	from tbl_Vendedor v inner join tbl_TipoSolicitante ts on ts.id_Tipo_Solicitante = v.id_tiposolicitante
	where v.E_Eliminado = 0 and v.id_vendedor>0
	order by Nombre
end

GO

/* [dbo].[spr_TraerVendedoresAlmaceneros] */
create proc [dbo].[spr_TraerVendedoresAlmaceneros]
as
begin
	select v.*, ts.id_Tipo_Solicitante, ts.Nombre TipoSolicitante
	from tbl_Vendedor v inner join tbl_TipoSolicitante ts on ts.id_Tipo_Solicitante = v.id_tiposolicitante
	where v.E_Eliminado = 0 and v.id_tiposolicitante in(4) order by Nombre
end


GO

/* [dbo].[spr_TraerVendedoresSocioEmbajador] */
CREATE proc [dbo].[spr_TraerVendedoresSocioEmbajador]
as
begin
	select v.*, ts.id_Tipo_Solicitante, ts.Nombre TipoSolicitante
	from tbl_Vendedor v inner join tbl_TipoSolicitante ts on ts.id_Tipo_Solicitante = v.id_tiposolicitante
	where v.E_Eliminado = 0 and v.id_tiposolicitante in(3,5) order by Nombre

end


GO

/* [dbo].[spx_Actualizacion_tbl_BajaProductoControlEnvio] */
create proc [dbo].[spx_Actualizacion_tbl_BajaProductoControlEnvio](@Id_Sucursal int,@cuantos int,@Sucursal nvarchar(250))
as
update tbl_BajaProductoControlEnvio set e_eliminado=1 where e_eliminado=0 and id_sucursal=@Id_Sucursal

insert into tbl_BajaProductoControlEnvio values (getdate(),@cuantos,@Id_Sucursal,@Sucursal,0)

select * from tbl_BajaProductoControlEnvio where e_eliminado=0
GO

/* [dbo].[spx_actualizarconformacioncuadrillabackoffice] */
CREATE PROCEDURE dbo.spx_ActualizarConformacionCuadrillaBackOffice
    @Id BIGINT,
    @Fecha DATE = NULL,
    @Estado NVARCHAR(50),
    @Actividad NVARCHAR(50),
    @Id_Tecnico INT,
    @Cuenta_SF NVARCHAR(100) = NULL,
    @Salesforce NVARCHAR(100) = NULL,
    @Habilidad NVARCHAR(100) = NULL,
    @Vehiculo NVARCHAR(100) = NULL,
    @Grupo NVARCHAR(100) = NULL,
    @Almacen NVARCHAR(100) = NULL,
    @GrupoDigitacion NVARCHAR(100) = NULL,
    @IdUsuarioDigitador INT = NULL,
    @Digitador NVARCHAR(150) = NULL,
    @Tecnico NVARCHAR(150) = NULL,
    @Id_TecnicoAuxiliar INT = NULL,
    @Auxiliar NVARCHAR(150) = NULL,
    @IdUsuarioSupervisor INT,
    @SupervisorACargo NVARCHAR(150) = NULL,
    @Sucursal NVARCHAR(100),
    @Observacion NVARCHAR(500) = NULL,
    @IdUsuarioRegistra INT
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.tbl_ConformacionCuadrillaDiario
    SET fecha = ISNULL(@Fecha, fecha),
        estado = @Estado,
        actividad = @Actividad,
        id_tecnico = @Id_Tecnico,
        cuenta_sf = @Cuenta_SF,
        salesforce = @Salesforce,
        habilidad = @Habilidad,
        vehiculo = @Vehiculo,
        [grupo] = @Grupo,
        almacen = @Almacen,
        grupoDigitacion = @GrupoDigitacion,
        idUsuarioDigitador = @IdUsuarioDigitador,
        digitador = @Digitador,
        tecnico = @Tecnico,
        id_tecnicoAuxiliar = @Id_TecnicoAuxiliar,
        auxiliar = @Auxiliar,
        idUsuarioSupervisor = @IdUsuarioSupervisor,
        supervisorACargo = @SupervisorACargo,
        sucursal = @Sucursal,
        observacion = @Observacion,
        idUsuarioRegistra = @IdUsuarioRegistra
    WHERE id = @Id
      AND e_eliminado = 0;
END

GO

/* [dbo].[spx_actualizarconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ActualizarConformacionCuadrillaWeb]
    @Id BIGINT,
    @Fecha DATE = NULL,
    @Estado NVARCHAR(50),
    @Actividad NVARCHAR(50),
    @Id_Tecnico INT,
    @Cuenta_SF NVARCHAR(100) = NULL,
    @Salesforce NVARCHAR(100) = NULL,
    @Habilidad NVARCHAR(100) = NULL,
    @Vehiculo NVARCHAR(100) = NULL,
    @Grupo NVARCHAR(100) = NULL,
    @Almacen NVARCHAR(100) = NULL,
    @GrupoDigitacion NVARCHAR(100) = NULL,
    @IdUsuarioDigitador INT = NULL,
    @Digitador NVARCHAR(150) = NULL,
    @Tecnico NVARCHAR(150) = NULL,
    @Id_TecnicoAuxiliar INT = NULL,
    @Auxiliar NVARCHAR(150) = NULL,
    @IdUsuarioSupervisor INT,
    @SupervisorACargo NVARCHAR(150) = NULL,
    @Sucursal NVARCHAR(100),
    @Observacion NVARCHAR(500) = NULL,
    @IdUsuarioRegistra INT
AS
BEGIN
    UPDATE dbo.tbl_ConformacionCuadrillaDiarioWeb
    SET fecha = ISNULL(@Fecha, fecha),
        estado = @Estado,
        actividad = @Actividad,
        id_tecnico = @Id_Tecnico,
        cuenta_sf = @Cuenta_SF,
        salesforce = @Salesforce,
        habilidad = @Habilidad,
        vehiculo = @Vehiculo,
        [grupo] = @Grupo,
        almacen = @Almacen,
        grupoDigitacion = @GrupoDigitacion,
        idUsuarioDigitador = @IdUsuarioDigitador,
        digitador = @Digitador,
        tecnico = @Tecnico,
        id_tecnicoAuxiliar = @Id_TecnicoAuxiliar,
        auxiliar = @Auxiliar,
        idUsuarioSupervisor = @IdUsuarioSupervisor,
        supervisorACargo = @SupervisorACargo,
        sucursal = @Sucursal,
        observacion = @Observacion,
        idUsuarioRegistra = @IdUsuarioRegistra
    WHERE id = @Id
      AND e_eliminado = 0;
END



GO

/* [dbo].[spx_BorrarRegistroPromedioUsoMaterial] */
create proc spx_BorrarRegistroPromedioUsoMaterial(@Id_TipoInstalacion int)
as
	update tbl_PromedioUsoMateriales set E_Eliminado =1 where Id_TipoServicio =@Id_TipoInstalacion

GO

/* [dbo].[spx_BuscarSerialCargoUsuario] */
create proc [dbo].[spx_BuscarSerialCargoUsuario](@serial nvarchar(50),@chipid nvarchar(50), @tipocodigo int)
as
	if (@tipocodigo =0)--serial
	begin
		Select	
		 --distinct(v.Id_Venta)
		 v.Id_Venta,r.Id_Ruta,r.Nombre Ruta,ve.Id_Vendedor, ve.Nombre Vendedor,t.Id_TipoServicio,t.Nombre TipoServicio,
		 v.Fecha_Ejecucion,v.Fecha_Registro,v.OrdenTrabajo,v.CodigoCliente,v.Observacion	
		From 
		tbl_venta V inner join tbl_codigoventacargousuario  cu on cu.id_venta=v.id_venta
		inner join tbl_TipoServicio t on V.Id_TipoServicio = T.Id_TipoServicio--3413
		inner join tbl_Ruta R on R.Id_Ruta = V.Id_Ruta 
		inner join tbl_vendedor ve on ve.id_vendedor=v.id_vendedor	
		Where
		v.E_eliminado = 0 and cu.e_eliminado=0 and cu.serial=@serial
		order by v.id_venta desc
	end
	else 
	begin
		Select	
		 --distinct(v.Id_Venta)
		 v.Id_Venta,r.Id_Ruta,r.Nombre Ruta,ve.Id_Vendedor, ve.Nombre Vendedor,t.Id_TipoServicio,t.Nombre TipoServicio,
		 v.Fecha_Ejecucion,v.Fecha_Registro,v.OrdenTrabajo,v.CodigoCliente,v.Observacion	
		From 
		tbl_venta V inner join tbl_codigoventacargousuario  cu on cu.id_venta=v.id_venta
		inner join tbl_TipoServicio t on V.Id_TipoServicio = T.Id_TipoServicio--3413
		inner join tbl_Ruta R on R.Id_Ruta = V.Id_Ruta 
		inner join tbl_vendedor ve on ve.id_vendedor=v.id_vendedor	
		Where
		v.E_eliminado = 0 and cu.e_eliminado=0 and cu.chipid=@chipid
		order by v.id_venta desc
	end
	
	

GO

/* [dbo].[spx_BusquedaSerieChipID] */

CREATE proc [dbo].[spx_BusquedaSerieChipID](@serie nvarchar(150),@tiposerie bit)
as
if(@tiposerie=0)
begin
	select  * from tbl_CorreccionErrores where serialErroneo =@serie and e_eliminado=0
	union all
	select  * from tbl_CorreccionErrores where serialCorrecto =@serie
end
else 
begin
	select  * from tbl_CorreccionErrores where ChipIdErroneo =@serie and e_eliminado=0
	union all
	select  * from tbl_CorreccionErrores where ChipIdCorrecto =@serie and e_eliminado=0
end





GO

/* [dbo].[spx_CambiarPasswordUsuario] */

CREATE PROC dbo.spx_CambiarPasswordUsuario
    @Login nvarchar(50),
    @PasswordHashActual varchar(100),
    @PasswordHashNueva varchar(100)
AS
BEGIN
    SET NOCOUNT ON;

    IF ISNULL(LTRIM(RTRIM(@Login)), '') = ''
    BEGIN
        RAISERROR('Login requerido.', 16, 1);
        RETURN;
    END

    IF ISNULL(LTRIM(RTRIM(@PasswordHashNueva)), '') = ''
    BEGIN
        RAISERROR('Password nueva requerida.', 16, 1);
        RETURN;
    END

    IF @PasswordHashActual = @PasswordHashNueva
    BEGIN
        RAISERROR('La nueva password no puede ser igual a la actual.', 16, 1);
        RETURN;
    END

    DECLARE @IdUsuario int;

    SELECT TOP 1
        @IdUsuario = u.Id_Usuario
    FROM dbo.tbl_usuario u
    WHERE u.E_Eliminado = 0
      AND u.Loggin = @Login
      AND u.Password = @PasswordHashActual;

    IF @IdUsuario IS NULL
    BEGIN
        RAISERROR('Usuario no existe o password actual incorrecta.', 16, 1);
        RETURN;
    END

    UPDATE dbo.tbl_usuario
    SET Password = @PasswordHashNueva,
        NecesitaCambio = 0,
        UltimaModificacion = GETDATE()
    WHERE Id_Usuario = @IdUsuario;

    SELECT
        CAST(1 AS bit) AS Exito,
        'Password actualizada correctamente.' AS Mensaje,
        @IdUsuario AS Id_Usuario;
END

GO

/* [dbo].[spx_cambiarpasswordusuarioporid] */

CREATE PROC dbo.spx_CambiarPasswordUsuarioPorId
    @Id_Usuario int,
    @PasswordHashActual varchar(100),
    @PasswordHashNueva varchar(100)
AS
BEGIN
    SET NOCOUNT ON;

    IF @Id_Usuario IS NULL OR @Id_Usuario <= 0
    BEGIN
        RAISERROR('Id_Usuario requerido.', 16, 1);
        RETURN;
    END

    IF ISNULL(LTRIM(RTRIM(@PasswordHashNueva)), '') = ''
    BEGIN
        RAISERROR('Password nueva requerida.', 16, 1);
        RETURN;
    END

    IF @PasswordHashActual = @PasswordHashNueva
    BEGIN
        RAISERROR('La nueva password no puede ser igual a la actual.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_usuario u
        WHERE u.E_Eliminado = 0
          AND u.Id_Usuario = @Id_Usuario
          AND u.Password = @PasswordHashActual
    )
    BEGIN
        RAISERROR('Usuario no existe o password actual incorrecta.', 16, 1);
        RETURN;
    END

    UPDATE dbo.tbl_usuario
    SET Password = @PasswordHashNueva,
        NecesitaCambio = 0,
        UltimaModificacion = GETDATE()
    WHERE Id_Usuario = @Id_Usuario;

    SELECT
        CAST(1 AS bit) AS Exito,
        'Password actualizada correctamente.' AS Mensaje,
        @Id_Usuario AS Id_Usuario;
END

GO

/* [dbo].[spx_CargarDatosFaltanteE18] */


CREATE proc [dbo].[spx_CargarDatosFaltanteE18]
as
begin
	insert into tbl_faltanteE18
	select  AÑO,Sucursal,BODEGA,EQUIPO,SERIE,CHIPID,ESTADO_INICIAL,REVTIGO,CAT,SERIEMAGIC,REGIONAL,DICTAMENFINAL,MONTO
	from [tigo.makiro.com.bo].bdSistemaantenaPM.dbo.tbl_faltanteE18
	where serie not in (select serie from tbl_faltanteE18)

	declare @cuantosSC int
	set @cuantosSC = (select count(serie) from [tigo.makiro.com.bo].bdSistemaantenaPM.dbo.tbl_faltanteE18)

	declare @cuantosSucursal int
	set @cuantosSucursal =(select count(serie) from tbl_faltanteE18)

	declare @sucursal nvarchar(150)
	set @sucursal =(select sucursal from tbl_version)


	update [tigo.makiro.com.bo].bdSistemaantenaPM.dbo.tbl_sucursal
	set cantidadFaltantee18 =@cuantosSucursal
	where sucursal=@sucursal

	if(@cuantosSC = @cuantosSucursal )
	BEGIN
		select 'ok'
	END
	else 
	select 'error'
end


GO

/* [dbo].[spx_CargarRutaSegunEstado] */
	--------------------------------------------------------	
CREATE proc [dbo].[spx_CargarRutaSegunEstado](@Id_Estado int)
as
begin
	if(@Id_Estado=1 or @Id_Estado=5 or @Id_Estado=4)
	 select * from tbl_Ruta where Id_Ruta=0 and E_Eliminado=0 order by Nombre
	if(@Id_Estado=2 or @Id_Estado=11)
	 select * from tbl_Ruta where Id_Ruta <>0  and E_Eliminado=0 order by Nombre
	 
end
GO

/* [dbo].[spx_ConfigurarPoliticaPassword] */

CREATE PROC dbo.spx_ConfigurarPoliticaPassword
    @RequierePorFlag bit,
    @DiasVigenciaPassword int
AS
BEGIN
    SET NOCOUNT ON;

    IF @DiasVigenciaPassword < 0
    BEGIN
        RAISERROR('DiasVigenciaPassword no puede ser negativo.', 16, 1);
        RETURN;
    END

    UPDATE dbo.tbl_configuracion_seguridad
    SET Activo = 0
    WHERE Activo = 1;

    INSERT INTO dbo.tbl_configuracion_seguridad
    (
        Activo,
        RequierePorFlag,
        DiasVigenciaPassword,
        FechaCreacion,
        FechaModificacion
    )
    VALUES
    (
        1,
        @RequierePorFlag,
        @DiasVigenciaPassword,
        GETDATE(),
        GETDATE()
    );

    SELECT
        RequierePorFlag,
        DiasVigenciaPassword
    FROM dbo.tbl_configuracion_seguridad
    WHERE Activo = 1;
END

GO

/* [dbo].[spx_CrearProducto] */

CREATE proc [dbo].[spx_CrearProducto](@Nombre nvarchar(100),@IdTipoProducto int,@PermiteDecimal bit,@Habilitado bit,@CantDigitosSerial int,
    @TieneEspacioSerial bit,@Mascara nvarchar(50),@SerialImportacion nvarchar(150),@CantDigitosChipId int,@TieneEspacioChipId bit,@MascaraChipId nvarchar(50),
    @ChipIdImportacion nvarchar(150),@PrecioVenta decimal(18,2),@PrecioUsado decimal(18,2),@SufijoNomenclador nvarchar(5),@Id_Usuario int)
AS
BEGIN
    DECLARE @IdProducto int

    INSERT INTO tbl_producto 
    VALUES (
        SUBSTRING(@Nombre,1,3), '1', @Nombre, 'UNI', '15', @PrecioVenta,
        @CantDigitosSerial, @IdTipoProducto, @Habilitado, @Nombre,
        0, 0, @CantDigitosChipId, @TieneEspacioSerial,
        @TieneEspacioChipId, @PermiteDecimal, @PrecioUsado,
        @SufijoNomenclador, @Id_Usuario
    )

    SET @IdProducto = SCOPE_IDENTITY()

    IF (@CantDigitosSerial > 0)
    BEGIN
        INSERT INTO tbl_productosmascaras 
        VALUES (
            @IdProducto, @Mascara, 0, @MascaraChipId,
            @ChipIdImportacion, @SerialImportacion, @MascaraChipId, @Mascara
        )
    END

    EXEC sp_CrearProductoEnSaldoTarjetaVendedor
END
GO

/* [dbo].[spx_CrearProductoEnSaldoAlmacenProductos] */
CREATE proc spx_CrearProductoEnSaldoAlmacenProductos
as
declare @contador int
declare @cuantos int

declare @Id_Producto  int 
set @cuantos = 1
set @contador = 1
 
     
       declare @ProductosNuevos table(Id int identity, Id_Producto int)                                                       
      
       insert into @ProductosNuevos
       select Id_Producto from tbl_Producto where Id_Producto not in (select Id_Producto from tbl_SaldoAlmacenProductos where E_Eliminado =0) and E_Eliminado=0
      
             
       set @cuantos=(select COUNT (*) from @ProductosNuevos)
             
       set @contador =1
       while(@contador <= @cuantos)
       begin
       
           set @Id_Producto = (select id_producto from @ProductosNuevos where Id = @contador )
           insert into tbl_SaldoAlmacenProductos values (@Id_Producto,0,0)      
          
           set @contador = @contador +1;                      
       end
       
      

GO

/* [dbo].[spx_DarBajaUsuario] */
CREATE proc [dbo].[spx_DarBajaUsuario](@Id_Usuario int)
as
	update tbl_Usuario set E_Eliminado=1 where Id_Usuario = @Id_Usuario 

GO

/* [dbo].[spx_DarDeBajaCorreccionErrores] */

CREATE proc [dbo].[spx_DarDeBajaCorreccionErrores](@id int)
as
update tbl_CorreccionErrores set e_eliminado=1 where id=@id

GO

/* [dbo].[spx_EliminarBajaProductos] */

create proc spx_EliminarBajaProductos(@codigo int)
as
update tbl_bajaproductos set e_eliminado=1 where id_bajaproductos=@codigo
update tbl_codigobajaproductos set e_eliminado=1 where id_bajaproductos=@codigo
update tbl_detallebajaproductos set e_eliminado=1 where id_bajaproductos=@codigo
GO

/* [dbo].[spx_EliminarBajaProductosPendiente] */
create proc spx_EliminarBajaProductosPendiente(@codigo int)
as 
update tbl_bajaproductospendiente set e_eliminado=1 where id_bajaproductospendiente =@codigo
update tbl_codigobajaproductospendiente set e_eliminado=1 where id_bajaproductospendiente =@codigo
update tbl_detallebajaproductospendiente set e_eliminado=1 where id_bajaproductospendiente =@codigo
---------------------------------------------------------------
---
GO

/* [dbo].[spx_EliminarCierreAlmacen] */
create proc [dbo].[spx_EliminarCierreAlmacen](@Id_Cierrealmacen int)
as
update tbl_cierrealmacen set e_eliminado=1 where id_cierrealmacen=@Id_Cierrealmacen
update tbl_codigocierrealmacen set e_eliminado=1 where id_cierrealmacen=@Id_Cierrealmacen
GO

/* [dbo].[spx_EliminarCierreAlmacenPR_PD] */

create proc spx_EliminarCierreAlmacenPR_PD(@Id_Cierrealmacen int)
as
update tbl_cierrealmacenpr_pd set e_eliminado=1 where id_cierrealmacenpr_pd=@Id_Cierrealmacen
update tbl_codigocierrealmacenpr_pd set e_eliminado=1 where id_cierrealmacenpr_pd=@Id_Cierrealmacen


GO

/* [dbo].[spx_EliminarCodigoUsuario_Venta] */
create proc [dbo].[spx_EliminarCodigoUsuario_Venta](@Id_Venta int , @IdUsuario int)
as
declare @count int 
set @count = (select count(*) from tbl_codigoventa where id_venta=@Id_Venta and e_eliminado=0)
if(@count>0)
update tbl_venta set observacion=observacion+'/*se elimino el cargo usuario', Id_UsuarioE=@IdUsuario where id_venta=@Id_Venta
else 
update tbl_venta set e_eliminado=1, Id_UsuarioE=@IdUsuario  where id_venta=@Id_venta
GO

/* [dbo].[spx_eliminarconformacioncuadrillaweb] */

CREATE PROC [dbo].[spx_EliminarConformacionCuadrillaWeb]
    @Id BIGINT
AS
BEGIN
    UPDATE dbo.tbl_ConformacionCuadrillaDiarioWeb
    SET e_eliminado = 1
    WHERE id = @Id
      AND e_eliminado = 0;
END



GO

/* [dbo].[spx_EliminarDetalleDevolucionTigoPendiente] */
create proc spx_EliminarDetalleDevolucionTigoPendiente(@Id_DevolucionTigoPendiente int)
as
update tbl_detalledevoluciontigopendiente 
set e_eliminado=1 
where id_devoluciontigopendiente=@Id_DevolucionTigoPendiente
GO

/* [dbo].[spx_eliminardevoluciontigopendiente] */

CREATE proc [dbo].[spx_eliminarDevolucionTigoPendiente](@codigo int)
as 
update tbl_DevolucionTigoPendiente set e_eliminado=1 where id_devoluciontigopendiente=@codigo
update tbl_detalleDevolucionTigoPendiente set e_eliminado=1 where id_devoluciontigopendiente=@codigo
update tbl_codigoDevolucionTigoPendiente set e_eliminado=1 where id_devoluciontigopendiente=@codigo

GO

/* [dbo].[spx_EliminarIngresoMaterialTigo] */
 
create proc spx_EliminarIngresoMaterialTigo(@codigo int,@FechaRegistro datetime,@Id_Usuario int,@Observacion nvarchar(max) )
as
begin
	update tbl_IngresoMaterialTigo 
	set e_eliminado=1 
	where id_IngresoMaterialTigo =@codigo

	update tbl_codigoIngresoMaterialTigo 
	set e_eliminado=1 
	where id_IngresoMaterialTigo =@codigo

end
GO

/* [dbo].[spx_EliminarMenu_usuario] */
create proc spx_EliminarMenu_usuario(@Id_Usuario int)
as
update tbl_tabla_menu_usuario 
set e_eliminado=1 
where id_usuario=@Id_Usuario
GO

/* [dbo].[spx_EliminarRol] */
create proc spx_EliminarRol(@Id_Rol int)
as
update tbl_rol set e_eliminado=1 where id_rol=@Id_Rol
GO

/* [dbo].[spx_eliminarSalidaTraspasoPendiente] */
create proc spx_eliminarSalidaTraspasoPendiente(@codigo int)
as
update tbl_SalidaTraspasopendiente set e_eliminado=1 where id_salidatraspasopendiente=@codigo
update tbl_codigoSalidaTraspasopendiente set e_eliminado=1 where id_salidatraspasopendiente=@codigo
update tbl_detalleSalidaTraspasopendiente set e_eliminado=1 where id_salidatraspasopendiente=@codigo
GO

/* [dbo].[spx_EliminarTecnico] */
create proc spx_EliminarTecnico(@Id_Tecnico int)
as
begin
	update tbl_vendedor set e_eliminado=1 where id_vendedor=@id_tecnico
end

GO

/* [dbo].[spx_EstadosNoPermitidosCambioNombre] */
CREATE proc [dbo].[spx_EstadosNoPermitidosCambioNombre]
as
select * from tbl_estadoproducto where id_estadoproducto in (1,2,4,5,6,9,10,11,13,14,17) and e_eliminado=0

GO

/* [dbo].[spx_EstadosPermitidosCambioNombreCU] */
create proc [dbo].[spx_EstadosPermitidosCambioNombreCU]
as
select * from tbl_estadoproducto where id_estadoproducto in (17) and e_eliminado=0
GO

/* [dbo].[spx_ExisteCierreAlmacenHoy] */
--SELECT 'una linea' + CHAR(13) + CHAR(10) + 'otra linea'
--spx_ExisteCierreAlmacenHoy '18/05/2021'
CREATE proc [dbo].[spx_ExisteCierreAlmacenHoy](@FechaRegistro datetime)
as
begin
	declare @cuantostablamov_pendientes int
	declare @tablamov_pendientes table(movimiento nvarchar(150), cantidad int)
	insert into @tablamov_pendientes exec [spx_ValidaMovimientos] @fechaRegistro

	declare @textopendientes nvarchar(max)
	set @textopendientes =  (SELECT STUFF(
								(SELECT +', ' +CHAR(10)+ movimiento
								FROM @tablamov_pendientes        
								FOR XML PATH ('')),
								1,2, ''))
	declare @diferenciaDias int 
	set @diferenciaDias=(select DATEDIFF(DAY,@FechaRegistro,GETDATE()))
		if(@diferenciaDias>=0)	--es de dias pasados verificar que no hayan trabajos pendientes
		begin		
			set @cuantostablamov_pendientes = (select COUNT(*) from @tablamov_pendientes)
			if(@cuantostablamov_pendientes>0)
				select -1 cantidad,@FechaRegistro as FechaVerificacion ,('Hay transacciones pendientes: ' +@textopendientes )Observacion		
			else 
				select 0 cantidad,@FechaRegistro as FechaVerificacion,''Observacion		
		end	
		
		if(@diferenciaDias<0)
		begin			
			select -1 cantidad,@FechaRegistro as FechaVerificacion ,'> a la fecha del Servidor' Observacion		
		end
end		
	
	
	
	
	
	
GO

/* [dbo].[spx_ExisteCierreAlmacenHoyPR_PD] */
CREATE PROCEDURE [dbo].[spx_ExisteCierreAlmacenHoyPR_PD](@FechaRegistro datetime)
as
begin
	declare @cuantostablamov_pendientes int
	declare @tablamov_pendientes table(movimiento nvarchar(150), cantidad int)
	insert into @tablamov_pendientes exec [spx_ValidaMovimientos] @fechaRegistro

		if(CONVERT(varchar(10), @FechaRegistro, 103) >= CONVERT(varchar(10), GETDATE(), 103))	
		begin		
			select COUNT(Id_CierreAlmacenPR_PD) cantidad,@FechaRegistro as FechaVerificacion ,'>= a la fecha del servidor'Observacion
			from tbl_CierreAlmacenPR_PD
			where E_Eliminado = 0 and dbo.dateonly(Fecha) = dbo.dateonly(@FechaRegistro) 	
		end
		else 
		begin		
			set @cuantostablamov_pendientes = (select COUNT(*) from @tablamov_pendientes)
			if(@cuantostablamov_pendientes>0)
				select -1 cantidad,@FechaRegistro as FechaVerificacion ,'< a la fecha del Servidor y hay transacciones pendientes. 'Observacion		
			else 
				select -1 cantidad,@FechaRegistro as FechaVerificacion ,'< a la fecha del Servidor' Observacion		
		end
end

GO

/* [dbo].[spx_ExisteDatoCorreccionErrores] */

CREATE proc [dbo].[spx_ExisteDatoCorreccionErrores](@serieErronea nvarchar(150),@chipidErroneo nvarchar(150))
as
select count(*) from tbl_CorreccionErrores
where serialErroneo=@serieErronea and chipidErroneo=@chipidErroneo and estadoModificacion ='Pendiente'
and e_eliminado=0


GO

/* [dbo].[spx_ExisteDatoDescuentoDTHPrepagoAntesBaja] */
create proc spx_ExisteDatoDescuentoDTHPrepagoAntesBaja
(@eh nvarchar(150),@mescomision int,@pago decimal(18,2))
as
select * from tbl_DescuentosDTHPrepagoAntesBaja where eh=@eh and mescomision=@mescomision
and pago=@pago and e_eliminado=0
GO

/* [dbo].[spx_ExisteNomenclador] */
create proc [dbo].[spx_ExisteNomenclador](@SufijoNomenclador nvarchar(5))
as
select * from tbl_producto where sufijonomenclador=@SufijoNomenclador and e_eliminado=0

GO

/* [dbo].[spx_ExisteNomencladorModificar] */
create proc [dbo].[spx_ExisteNomencladorModificar](@SufijoNomenclador nvarchar(5),@IdProducto int)
as
select * from tbl_producto where sufijonomenclador=@SufijoNomenclador and id_producto<>@IdProducto and e_eliminado=0

GO

/* [dbo].[spx_ExisteOrdenRegistrada] */
CREATE proc [dbo].[spx_ExisteOrdenRegistrada](@OrdenTrabajo nvarchar(15),@Tabla nvarchar(50))
as
	if(@Tabla='DTH_PREPAGO')
	BEGIN	
		select * from tbl_DatosDTHPrepago where ORDENNRO=@OrdenTrabajo
	END 
		if(@Tabla='HFC')
	BEGIN	
		select * from tbl_DatosHFC where OT=@OrdenTrabajo
	END 
		if(@Tabla='POST_PAGO')
	BEGIN	
		select * from tbl_DatosPostPago where OT=@OrdenTrabajo
	END 
GO

/* [dbo].[spx_ExistePlacaVehiculo] */

create proc [dbo].[spx_ExistePlacaVehiculo](@placa nvarchar(50))
as
select * from tbl_placavehiculo where e_eliminado=0 and placa=@placa

GO

/* [dbo].[spx_ExisteProducto] */
-------------------------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------------------------
CREATE proc [dbo].[spx_ExisteProducto](@nombre nvarchar(150))
as
select * from tbl_producto where nombre =@nombre and e_eliminado=0
GO

/* [dbo].[spx_existeregistroconformacioncuadrilladiario] */
CREATE proc spx_ExisteRegistroConformacionCuadrillaDiario(@idTecnico int,@fecha datetime)
as
select * from tbl_ConformacionCuadrillaDiario 
where id_tecnico=@idTecnico and dbo.dateonly(fecha)=dbo.dateonly(@fecha)
and e_eliminado=0


GO

/* [dbo].[spx_ExisteTecnico] */
CREATE proc [dbo].[spx_ExisteTecnico](@nombre nvarchar(150),@CodEmpleado nvarchar(15))
as
begin
	select * from tbl_Vendedor 
	where (Nombre = @nombre or codempleado=@codempleado) and E_Eliminado=0
end	 
 
GO

/* [dbo].[spx_ExportarDatosConfirmados] */

create proc spx_ExportarDatosConfirmados(@FechaGestion datetime,@Id_Sucursal int)
as
		select CLIENTENRO NumeroCliente,ORDENNRO OrdenTrabajo, TipoServicioTS TipoSolicitud,FechaGestion,EstadoGestionDealer,FechaProgramacion ,Observacion 
		from tbl_DatosDTHPrepago where IdSucursal= @Id_Sucursal AND 
		(EstadoGestionDealer LIKE 'CONFIRMADA' OR EstadoGestionDealer LIKE 'REPROGRAMADA')
	UNION ALL
		select CODIGO NumeroCliente,OT OrdenTrabajo, TipoServicioTS TipoSolicitud,FechaGestion,EstadoGestionDealer,FechaProgramacion ,Observacion 
		from tbl_DatosHFC where IdSucursal= @Id_Sucursal AND 
		(EstadoGestionDealer LIKE 'CONFIRMADA' OR EstadoGestionDealer LIKE 'REPROGRAMADA')
	UNION ALL
		select CODIGO NumeroCliente,OT OrdenTrabajo, TipoServicioTS TipoSolicitud,FechaGestion,EstadoGestionDealer,FechaProgramacion ,Observacion 
		from tbl_DatosPostPago where IdSucursal= @Id_Sucursal AND 
		(EstadoGestionDealer LIKE 'CONFIRMADA' OR EstadoGestionDealer LIKE 'REPROGRAMADA')


GO

/* [dbo].[spx_ExportarDatosDevueltosATigo] */
CREATE proc spx_ExportarDatosDevueltosATigo(@FechaGestion datetime,@Id_Sucursal int)
as
		select CLIENTENRO NumeroCliente,ORDENNRO OrdenTrabajo, TipoServicioTS TipoSolicitud,FechaGestion,EstadoGestionDealer,FechaProgramacion ,Observacion 
		from tbl_DatosDTHPrepago where IdSucursal= @Id_Sucursal AND 
		(EstadoGestionDealer LIKE 'DEVUELTA_A_TIGO' OR EstadoGestionDealer LIKE 'ORDEN MAL GENERADA' OR EstadoGestionDealer LIKE 'SIN COMUNICACION')
	UNION ALL
		select CODIGO NumeroCliente,OT OrdenTrabajo, TipoServicioTS TipoSolicitud,FechaGestion,EstadoGestionDealer,FechaProgramacion ,Observacion 
		from tbl_DatosHFC where IdSucursal= @Id_Sucursal AND 
		(EstadoGestionDealer LIKE 'DEVUELTA_A_TIGO' OR EstadoGestionDealer LIKE 'ORDEN MAL GENERADA' OR EstadoGestionDealer LIKE 'SIN COMUNICACION')
	UNION ALL
		select CODIGO NumeroCliente,OT OrdenTrabajo, TipoServicioTS TipoSolicitud,FechaGestion,EstadoGestionDealer,FechaProgramacion ,Observacion 
		from tbl_DatosPostPago where IdSucursal= @Id_Sucursal AND 
		(EstadoGestionDealer LIKE 'DEVUELTA_A_TIGO' OR EstadoGestionDealer LIKE 'ORDEN MAL GENERADA' OR EstadoGestionDealer LIKE 'SIN COMUNICACION')


GO

/* [dbo].[spx_ForzarCambioPasswordUsuario] */

CREATE PROC dbo.spx_ForzarCambioPasswordUsuario
    @Login nvarchar(50),
    @NecesitaCambio bit
AS
BEGIN
    SET NOCOUNT ON;

    UPDATE dbo.tbl_usuario
    SET NecesitaCambio = @NecesitaCambio,
        UltimaModificacion = CASE WHEN @NecesitaCambio = 1 THEN ISNULL(UltimaModificacion, GETDATE()) ELSE GETDATE() END
    WHERE E_Eliminado = 0
      AND Loggin = @Login;

    IF @@ROWCOUNT = 0
    BEGIN
        RAISERROR('Usuario no encontrado.', 16, 1);
        RETURN;
    END

    SELECT
        CAST(1 AS bit) AS Exito,
        'Actualizacion realizada.' AS Mensaje,
        @Login AS Loggin,
        @NecesitaCambio AS NecesitaCambio;
END

GO

/* [dbo].[spx_getArchivo] */
CREATE proc [dbo].[spx_getArchivo]( @ID int, @Tabla int )
as 
begin
	if ( @Tabla = 0 ) -- Ingreso Material Tigo.
		select nombrearchivo, archivo from tbl_IngresoMaterialTigo where id_IngresoMaterialTigo = @ID;
	
	if ( @Tabla = 1 ) -- devolución.
		select nombrearchivo, archivo from tbl_Devolucion where Id_Devolucion = @ID;
		
	if ( @Tabla = 2 ) -- baja productos
		select nombrearchivo, archivo from tbl_BajaProductos where id_BajaProductos= @ID;		
		
	if ( @Tabla = 3 ) -- salida traspaso
		select nombrearchivo, archivo from tbl_SalidaTraspaso where id_SalidaTraspaso= @ID;		
end	
GO

/* [dbo].[spx_getMaterialDevueltoTigo] */
CREATE proc [dbo].[spx_getMaterialDevueltoTigo]( @idDevolucionTigo int )
as		
	select td.Observacion, td.nombrearchivo, tp.Id_Producto, tp.Nombre, sum(tdd.Cantidad) as Cantidad
	from tbl_producto tp inner join tbl_DetalleDevolucion tdd on tdd.Id_Producto = tp.Id_Producto and tdd.e_eliminado = 0
			inner join tbl_Devolucion td on td.Id_Devolucion = tdd.Id_Devolucion
		and tdd.Id_Devolucion = @idDevolucionTigo
		and tp.E_Eliminado = 0
	group by td.Observacion, td.nombrearchivo, tp.Id_Producto, tp.Nombre
	
GO

/* [dbo].[spx_getMaterialDevueltoTigoPendiente] */

create proc [dbo].[spx_getMaterialDevueltoTigoPendiente]( @idDevolucionTigo int )
as		
	select td.Observacion,td.Estado, tp.Id_Producto, tp.Nombre, sum(tdd.Cantidad) as Cantidad
	from tbl_producto tp inner join tbl_CodigoDevolucionTigoPendiente tdd on tdd.Id_Producto = tp.Id_Producto and tdd.e_eliminado = 0
			inner join tbl_DevolucionTigoPendiente td on td.Id_DevolucionTigoPendiente = tdd.Id_DevolucionTigoPendiente
		and tdd.Id_DevolucionTigoPendiente = @idDevolucionTigo
		and tp.E_Eliminado = 0
	group by td.Observacion, tp.Id_Producto, tp.Nombre,td.Estado
	
GO

/* [dbo].[spx_getMaterialDevueltoTigoPendienteSAgrupar] */
CREATE proc [dbo].[spx_getMaterialDevueltoTigoPendienteSAgrupar]( @idDevolucionTigo int )
as		
	select td.Id_DevolucionTigoPendiente Id_Devolucion, tdd.Id_CodigoDevolucionTipoPendiente Id_DetalleDevolucion, 
	tp.Id_Producto, tp.Nombre, tdd.Serial Cod_Inicio,tdd.ChipID, tdd.Cantidad,td.Observacion
	from tbl_producto tp inner join tbl_CodigoDevolucionTigoPendiente tdd on tdd.Id_Producto = tp.Id_Producto 
			inner join tbl_DevolucionTigoPendiente td on td.Id_DevolucionTigoPendiente = tdd.Id_DevolucionTigoPendiente
		and tdd.Id_DevolucionTigoPendiente = @idDevolucionTigo
		and tp.E_Eliminado = 0 and td.E_Eliminado = 0 and tdd.E_Eliminado = 0	
GO

/* [dbo].[spx_getMaterialDevueltoTigoSAgrupar] */
CREATE proc [dbo].[spx_getMaterialDevueltoTigoSAgrupar]( @idDevolucionTigo int )
as		
	select td.Id_Devolucion, tdd.Id_DetalleDevolucion, tp.Id_Producto, tp.Nombre, tdd.Cod_Inicio,tdd.ChipID, tdd.Cantidad
	from tbl_producto tp inner join tbl_DetalleDevolucion tdd on tdd.Id_Producto = tp.Id_Producto 
			inner join tbl_Devolucion td on td.Id_Devolucion = tdd.Id_Devolucion
		and tdd.Id_Devolucion = @idDevolucionTigo
		and tp.E_Eliminado = 0 and td.E_Eliminado = 0 and tdd.E_Eliminado = 0	

GO

/* [dbo].[spx_getMaterialPreIngresado] */
CREATE proc [dbo].[spx_getMaterialPreIngresado]( @idIngresoMaterialTigo int )
as
	select tp.Id_Producto, tp.Nombre, SUM( tcia.Cantidad )
	from tbl_producto tp inner join tbl_CodigoIngresoAlmacen tcia on tcia.Id_Producto = tp.Id_Producto and tp.e_eliminado = 0
	inner join tbl_IngresoMaterialTigo_Almacen tima on tima.id_IngresoAlmacen = tcia.Id_IngresoAlmacen and 
	tima.id_IngresoMaterialTigo = @idIngresoMaterialTigo
	where tcia.E_Eliminado=0
	group by tp.Id_Producto, tp.Nombre

GO

/* [dbo].[spx_GetProductoExcedenteRuta] */
CREATE proc spx_GetProductoExcedenteRuta
as
	select tr.Id_Ruta, tr.Nombre, tp.Id_Producto, tp.Nombre, tp.limitePromedio, tst.Cantidad
		from tbl_Ruta tr 
		inner join tbl_SaldoTarjetas tst on tr.Id_Ruta = tst.Id_Ruta and tst.E_Eliminado = 0 and tr.E_Eliminado = 0
		inner join tbl_producto tp on tp.Id_Producto = tst.Id_Producto and tp.E_Eliminado = 0
	where tst.Cantidad > tp.limitePromedio
	order by tr.Id_Ruta asc

GO

/* [dbo].[spx_grupo_asignarsupervisorcentral] */
CREATE PROCEDURE dbo.spx_Grupo_AsignarSupervisorCentral
    @IdUsuarioEjecutor INT,
    @IdGrupo INT,
    @IdUsuarioSupervisor INT
AS
BEGIN
    SET NOCOUNT ON;

    IF @IdGrupo IS NULL OR @IdGrupo <= 0
    BEGIN
        RAISERROR('IdGrupo es requerido.', 16, 1);
        RETURN;
    END

    IF @IdUsuarioSupervisor IS NULL OR @IdUsuarioSupervisor <= 0
    BEGIN
        RAISERROR('IdUsuarioSupervisor es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Grupo g
        WHERE g.id_grupo = @IdGrupo
          AND ISNULL(g.e_eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('Grupo no encontrado o eliminado.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Usuario u
        INNER JOIN dbo.tbl_Rol r ON r.Id_Rol = u.Id_Rol
        WHERE u.Id_Usuario = @IdUsuarioSupervisor
          AND ISNULL(u.E_Eliminado, 0) = 0
          AND LOWER(LTRIM(RTRIM(r.Nombre))) LIKE '%supervisor%'
    )
    BEGIN
        RAISERROR('El usuario indicado no es supervisor activo.', 16, 1);
        RETURN;
    END

    IF EXISTS (
        SELECT 1
        FROM dbo.tbl_GrupoSup gs
        WHERE gs.id_grupo = @IdGrupo
          AND gs.id_usuario = @IdUsuarioSupervisor
    )
    BEGIN
        SELECT gs.id_grupo_sup, gs.id_usuario, gs.id_grupo, gs.fecha_registro
        FROM dbo.tbl_GrupoSup gs
        WHERE gs.id_grupo = @IdGrupo
          AND gs.id_usuario = @IdUsuarioSupervisor;
        RETURN;
    END

    INSERT INTO dbo.tbl_GrupoSup (id_usuario, id_grupo, fecha_registro)
    VALUES (@IdUsuarioSupervisor, @IdGrupo, GETDATE());

    SELECT TOP 1 gs.id_grupo_sup, gs.id_usuario, gs.id_grupo, gs.fecha_registro
    FROM dbo.tbl_GrupoSup gs
    WHERE gs.id_grupo_sup = SCOPE_IDENTITY();
END

GO

/* [dbo].[spx_grupo_asignartecnicocentral] */
CREATE PROCEDURE dbo.spx_Grupo_AsignarTecnicoCentral
    @IdUsuarioEjecutor INT,
    @IdGrupo INT,
    @IdUsuarioTecnico INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @TablaTecnico SYSNAME = NULL;
    DECLARE @ColumnaTecnico SYSNAME = NULL;
    DECLARE @IdTecnicoResuelto INT = NULL;
    DECLARE @Sql NVARCHAR(MAX);
    DECLARE @IdGrupoExistente INT = NULL;
    DECLARE @NombreGrupoExistente NVARCHAR(120) = NULL;

    IF @IdGrupo IS NULL OR @IdGrupo <= 0
    BEGIN
        RAISERROR('IdGrupo es requerido.', 16, 1);
        RETURN;
    END

    IF @IdUsuarioTecnico IS NULL OR @IdUsuarioTecnico <= 0
    BEGIN
        RAISERROR('IdUsuarioTecnico es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Grupo g
        WHERE g.id_grupo = @IdGrupo
          AND ISNULL(g.e_eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('Grupo no encontrado o eliminado.', 16, 1);
        RETURN;
    END

    IF OBJECT_ID('dbo.tbl_usuario_tecnico', 'U') IS NOT NULL SET @TablaTecnico = 'dbo.tbl_usuario_tecnico';
    ELSE IF OBJECT_ID('dbo.tbl_UsuarioTecnico', 'U') IS NOT NULL SET @TablaTecnico = 'dbo.tbl_UsuarioTecnico';
    ELSE IF OBJECT_ID('dbo.tbl_Usuario_Tecnico', 'U') IS NOT NULL SET @TablaTecnico = 'dbo.tbl_Usuario_Tecnico';

    IF @TablaTecnico IS NULL
    BEGIN
        RAISERROR('No existe tabla de usuario tecnico en esta BD.', 16, 1);
        RETURN;
    END

    IF COL_LENGTH(@TablaTecnico, 'id_usuario_tecnico') IS NOT NULL SET @ColumnaTecnico = 'id_usuario_tecnico';
    ELSE IF COL_LENGTH(@TablaTecnico, 'idUsuarioTecnico') IS NOT NULL SET @ColumnaTecnico = 'idUsuarioTecnico';
    ELSE IF COL_LENGTH(@TablaTecnico, 'Id_Usuario_Tecnico') IS NOT NULL SET @ColumnaTecnico = 'Id_Usuario_Tecnico';
    ELSE IF COL_LENGTH(@TablaTecnico, 'id') IS NOT NULL SET @ColumnaTecnico = 'id';

    IF @ColumnaTecnico IS NULL
    BEGIN
        RAISERROR('No se encontro la columna PK de usuario tecnico.', 16, 1);
        RETURN;
    END

    SET @Sql = N'
        SELECT TOP 1 @IdResOut = t.' + QUOTENAME(@ColumnaTecnico) + N'
        FROM ' + @TablaTecnico + N' t
        WHERE t.' + QUOTENAME(@ColumnaTecnico) + N' = @IdInput
          AND (COL_LENGTH(''' + @TablaTecnico + ''', ''e_eliminado'') IS NULL OR ISNULL(t.e_eliminado,0)=0);';
    EXEC sp_executesql @Sql, N'@IdInput INT, @IdResOut INT OUTPUT', @IdInput = @IdUsuarioTecnico, @IdResOut = @IdTecnicoResuelto OUTPUT;

    IF @IdTecnicoResuelto IS NULL
       AND (COL_LENGTH(@TablaTecnico, 'id_vendedor') IS NOT NULL OR COL_LENGTH(@TablaTecnico, 'id_Vendedor') IS NOT NULL)
    BEGIN
        SET @Sql = N'
            SELECT TOP 1 @IdResOut = t.' + QUOTENAME(@ColumnaTecnico) + N'
            FROM ' + @TablaTecnico + N' t
            WHERE (
                    (COL_LENGTH(''' + @TablaTecnico + ''', ''id_vendedor'') IS NOT NULL AND t.id_vendedor = @IdInput)
                 OR (COL_LENGTH(''' + @TablaTecnico + ''', ''id_Vendedor'') IS NOT NULL AND t.id_Vendedor = @IdInput)
                  )
              AND (COL_LENGTH(''' + @TablaTecnico + ''', ''e_eliminado'') IS NULL OR ISNULL(t.e_eliminado,0)=0);';
        EXEC sp_executesql @Sql, N'@IdInput INT, @IdResOut INT OUTPUT', @IdInput = @IdUsuarioTecnico, @IdResOut = @IdTecnicoResuelto OUTPUT;
    END

    IF @IdTecnicoResuelto IS NULL
       AND (COL_LENGTH(@TablaTecnico, 'id_vendedor') IS NOT NULL OR COL_LENGTH(@TablaTecnico, 'id_Vendedor') IS NOT NULL)
    BEGIN
        DECLARE @TablaVendedor SYSNAME = NULL;
        DECLARE @ColVendedorPk SYSNAME = NULL;
        DECLARE @ColTecnicoVendedor SYSNAME = NULL;
        DECLARE @ExisteVendedor INT = 0;
        DECLARE @InsertCols NVARCHAR(MAX);
        DECLARE @InsertVals NVARCHAR(MAX);

        IF OBJECT_ID('dbo.tbl_vendedor', 'U') IS NOT NULL SET @TablaVendedor = 'dbo.tbl_vendedor';
        ELSE IF OBJECT_ID('dbo.tbl_Vendedor', 'U') IS NOT NULL SET @TablaVendedor = 'dbo.tbl_Vendedor';

        IF @TablaVendedor IS NOT NULL
        BEGIN
            IF COL_LENGTH(@TablaVendedor, 'id_vendedor') IS NOT NULL SET @ColVendedorPk = 'id_vendedor';
            ELSE IF COL_LENGTH(@TablaVendedor, 'Id_Vendedor') IS NOT NULL SET @ColVendedorPk = 'Id_Vendedor';
        END

        IF COL_LENGTH(@TablaTecnico, 'id_vendedor') IS NOT NULL SET @ColTecnicoVendedor = 'id_vendedor';
        ELSE IF COL_LENGTH(@TablaTecnico, 'id_Vendedor') IS NOT NULL SET @ColTecnicoVendedor = 'id_Vendedor';

        IF @TablaVendedor IS NOT NULL AND @ColVendedorPk IS NOT NULL AND @ColTecnicoVendedor IS NOT NULL
        BEGIN
            SET @Sql = N'
                SELECT TOP 1 @ExisteOut = 1
                FROM ' + @TablaVendedor + N' v
                WHERE v.' + QUOTENAME(@ColVendedorPk) + N' = @IdInput
                  AND (COL_LENGTH(''' + @TablaVendedor + ''', ''e_eliminado'') IS NULL OR ISNULL(v.e_eliminado,0)=0);';
            EXEC sp_executesql @Sql, N'@IdInput INT, @ExisteOut INT OUTPUT', @IdInput = @IdUsuarioTecnico, @ExisteOut = @ExisteVendedor OUTPUT;
        END

        IF @ExisteVendedor = 1
        BEGIN
            SET @InsertCols = QUOTENAME(@ColTecnicoVendedor);
            SET @InsertVals = N'@IdVendedor';

            IF COL_LENGTH(@TablaTecnico, 'id_Usuario') IS NOT NULL
            BEGIN
                SET @InsertCols = @InsertCols + N', [id_Usuario]';
                SET @InsertVals = @InsertVals + N', NULL';
            END
            IF COL_LENGTH(@TablaTecnico, 'id_UsuarioRegistra') IS NOT NULL
            BEGIN
                SET @InsertCols = @InsertCols + N', [id_UsuarioRegistra]';
                SET @InsertVals = @InsertVals + N', ISNULL(@IdUsuarioRegistra,0)';
            END
            IF COL_LENGTH(@TablaTecnico, 'e_eliminado') IS NOT NULL
            BEGIN
                SET @InsertCols = @InsertCols + N', [e_eliminado]';
                SET @InsertVals = @InsertVals + N', 0';
            END
            IF COL_LENGTH(@TablaTecnico, 'fecharegistro') IS NOT NULL
            BEGIN
                SET @InsertCols = @InsertCols + N', [fecharegistro]';
                SET @InsertVals = @InsertVals + N', GETDATE()';
            END
            IF COL_LENGTH(@TablaTecnico, 'fecha_registro') IS NOT NULL
            BEGIN
                SET @InsertCols = @InsertCols + N', [fecha_registro]';
                SET @InsertVals = @InsertVals + N', GETDATE()';
            END

            SET @Sql = N'
                INSERT INTO ' + @TablaTecnico + N' (' + @InsertCols + N')
                VALUES (' + @InsertVals + N');

                SELECT TOP 1 @IdResOut = t.' + QUOTENAME(@ColumnaTecnico) + N'
                FROM ' + @TablaTecnico + N' t
                WHERE t.' + QUOTENAME(@ColTecnicoVendedor) + N' = @IdVendedor
                  AND (COL_LENGTH(''' + @TablaTecnico + ''', ''e_eliminado'') IS NULL OR ISNULL(t.e_eliminado,0)=0)
                ORDER BY t.' + QUOTENAME(@ColumnaTecnico) + N' DESC;';
            EXEC sp_executesql
                @Sql,
                N'@IdVendedor INT, @IdUsuarioRegistra INT, @IdResOut INT OUTPUT',
                @IdVendedor = @IdUsuarioTecnico,
                @IdUsuarioRegistra = @IdUsuarioEjecutor,
                @IdResOut = @IdTecnicoResuelto OUTPUT;
        END
    END

    IF @IdTecnicoResuelto IS NULL
    BEGIN
        RAISERROR('El tecnico indicado no existe en tabla usuario_tecnico (ni por id interno ni por id_vendedor).', 16, 1);
        RETURN;
    END

    SELECT TOP 1
        @IdGrupoExistente = g.id_grupo,
        @NombreGrupoExistente = g.nombre
    FROM dbo.tbl_DetalleGrupo dg
    INNER JOIN dbo.tbl_Grupo g ON g.id_grupo = dg.id_grupo
    WHERE dg.id_usuario_tecnico = @IdTecnicoResuelto
      AND ISNULL(g.e_eliminado, 0) = 0
      AND g.id_grupo <> @IdGrupo;

    IF @IdGrupoExistente IS NOT NULL
    BEGIN
        RAISERROR('No se puede, este tecnico esta en otro grupo.', 16, 1);
        RETURN;
    END

    IF EXISTS (
        SELECT 1
        FROM dbo.tbl_DetalleGrupo dg
        WHERE dg.id_grupo = @IdGrupo
          AND dg.id_usuario_tecnico = @IdTecnicoResuelto
    )
    BEGIN
        SELECT dg.id_detalle_grupo, dg.id_grupo, dg.id_usuario_tecnico, dg.fecha_registro
        FROM dbo.tbl_DetalleGrupo dg
        WHERE dg.id_grupo = @IdGrupo
          AND dg.id_usuario_tecnico = @IdTecnicoResuelto;
        RETURN;
    END

    INSERT INTO dbo.tbl_DetalleGrupo (id_grupo, id_usuario_tecnico, fecha_registro)
    VALUES (@IdGrupo, @IdTecnicoResuelto, GETDATE());

    SELECT TOP 1 dg.id_detalle_grupo, dg.id_grupo, dg.id_usuario_tecnico, dg.fecha_registro
    FROM dbo.tbl_DetalleGrupo dg
    WHERE dg.id_detalle_grupo = SCOPE_IDENTITY();
END

GO

/* [dbo].[spx_grupo_cambiarcolaboradorbackupcentral] */
CREATE PROCEDURE dbo.spx_Grupo_CambiarColaboradorBackupCentral
    @IdUsuarioEjecutor INT,
    @IdGrupo INT,
    @IdUsuarioTecnico INT
AS
BEGIN
    SET NOCOUNT ON;

    IF @IdGrupo IS NULL OR @IdGrupo <= 0
    BEGIN
        RAISERROR('IdGrupo es requerido.', 16, 1);
        RETURN;
    END
    IF @IdUsuarioTecnico IS NULL OR @IdUsuarioTecnico <= 0
    BEGIN
        RAISERROR('IdUsuarioTecnico es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Grupo g
        WHERE g.id_grupo = @IdGrupo
          AND ISNULL(g.e_eliminado, 0) = 0
          AND ISNULL(g.supervisor_ausente, 0) = 1
    )
    BEGIN
        RAISERROR('El grupo no esta en estado supervisor ausente.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_DetalleGrupo dg
        WHERE dg.id_grupo = @IdGrupo
          AND dg.id_usuario_tecnico = @IdUsuarioTecnico
    )
    BEGIN
        RAISERROR('El tecnico temporal debe pertenecer al grupo.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_GrupoBackup gb
        WHERE gb.id_grupo = @IdGrupo
          AND gb.e_activo = 1
    )
    BEGIN
        RAISERROR('No existe backup activo para el grupo.', 16, 1);
        RETURN;
    END

    UPDATE dbo.tbl_GrupoBackup
    SET id_usuario_tecnico_temporal = @IdUsuarioTecnico,
        id_usuario_actualiza = @IdUsuarioEjecutor,
        fecha_actualizacion = GETDATE()
    WHERE id_grupo = @IdGrupo
      AND e_activo = 1;

    SELECT TOP 1
        gb.id_grupo_backup,
        gb.id_grupo,
        gb.id_usuario_tecnico_temporal AS id_tecnico_temporal_backup,
        gb.e_activo
    FROM dbo.tbl_GrupoBackup gb
    WHERE gb.id_grupo = @IdGrupo
      AND gb.e_activo = 1
    ORDER BY gb.id_grupo_backup DESC;
END

GO

/* [dbo].[spx_grupo_crearcentral] */
CREATE PROCEDURE dbo.spx_Grupo_CrearCentral
    @IdUsuarioEjecutor INT,
    @Nombre NVARCHAR(120)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @NombreNorm NVARCHAR(120) = NULLIF(LTRIM(RTRIM(@Nombre)), '');
    IF @NombreNorm IS NULL
    BEGIN
        RAISERROR('Nombre de grupo es requerido.', 16, 1);
        RETURN;
    END

    IF EXISTS (
        SELECT 1
        FROM dbo.tbl_Grupo g
        WHERE ISNULL(g.e_eliminado, 0) = 0
          AND LOWER(LTRIM(RTRIM(g.nombre))) = LOWER(@NombreNorm)
    )
    BEGIN
        RAISERROR('Ya existe un grupo activo con ese nombre.', 16, 1);
        RETURN;
    END

    INSERT INTO dbo.tbl_Grupo (nombre, e_eliminado, fecha_registro)
    VALUES (@NombreNorm, 0, GETDATE());

    SELECT TOP 1 id_grupo, nombre, e_eliminado, fecha_registro
    FROM dbo.tbl_Grupo
    WHERE id_grupo = SCOPE_IDENTITY();
END

GO

/* [dbo].[spx_grupo_eliminarcentral] */
CREATE PROCEDURE dbo.spx_Grupo_EliminarCentral
    @IdUsuarioEjecutor INT,
    @IdGrupo INT
AS
BEGIN
    SET NOCOUNT ON;

    IF @IdGrupo IS NULL OR @IdGrupo <= 0
    BEGIN
        RAISERROR('IdGrupo es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Grupo g
        WHERE g.id_grupo = @IdGrupo
          AND ISNULL(g.e_eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('Grupo no encontrado o ya eliminado.', 16, 1);
        RETURN;
    END

    UPDATE dbo.tbl_Grupo
    SET e_eliminado = 1,
        supervisor_ausente = 0
    WHERE id_grupo = @IdGrupo
      AND ISNULL(e_eliminado, 0) = 0;

    UPDATE dbo.tbl_GrupoBackup
    SET e_activo = 0,
        fecha_fin = ISNULL(fecha_fin, GETDATE()),
        id_usuario_actualiza = @IdUsuarioEjecutor,
        fecha_actualizacion = GETDATE()
    WHERE id_grupo = @IdGrupo
      AND e_activo = 1;

    SELECT TOP 1 g.id_grupo, g.nombre, g.e_eliminado, g.supervisor_ausente, g.fecha_registro
    FROM dbo.tbl_Grupo g
    WHERE g.id_grupo = @IdGrupo;
END

GO

/* [dbo].[spx_grupo_filtrosupervisorescentral] */
CREATE PROCEDURE dbo.spx_Grupo_FiltroSupervisoresCentral
AS
BEGIN
    SET NOCOUNT ON;
    EXEC dbo.spx_ObtenerSupervisoresConformacionCuadrillaWeb;
END

GO

/* [dbo].[spx_grupo_filtrotecnicoscentral] */
CREATE PROCEDURE dbo.spx_Grupo_FiltroTecnicosCentral
AS
BEGIN
    SET NOCOUNT ON;
    EXEC dbo.spx_ObtenerTecnicosConformacionCuadrillaWeb;
END

GO

/* [dbo].[spx_grupo_listarcentral] */
CREATE PROCEDURE dbo.spx_Grupo_ListarCentral
    @IdUsuarioEjecutor INT
AS
BEGIN
    SET NOCOUNT ON;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Usuario u
        INNER JOIN dbo.tbl_Rol r ON r.Id_Rol = u.Id_Rol
        WHERE u.Id_Usuario = @IdUsuarioEjecutor
          AND ISNULL(u.E_Eliminado, 0) = 0
          AND LOWER(LTRIM(RTRIM(r.Nombre))) = 'central'
    )
    BEGIN
        RAISERROR('Solo el rol Central puede listar grupos.', 16, 1);
        RETURN;
    END

    SELECT
        d.id_grupo,
        d.grupo AS nombre,
        MAX(CAST(c.fechaRegistro AS DATETIME)) AS fecha_registro,
        COUNT(DISTINCT d.id_usuario_supervisor) AS cantidad_supervisores,
        COUNT(DISTINCT d.id_tecnico) AS cantidad_tecnicos,
        d.supervisor,
        CAST(NULL AS INT) AS id_tecnico_temporal_backup,
        CAST(NULL AS NVARCHAR(200)) AS tecnico_temporal_backup,
        d.id_tecnico AS id_usuario_tecnico,
        d.tecnico
    FROM dbo.vw_GruposTecnicosDetalle d
    LEFT JOIN dbo.tbl_ConformacionCuadrillaDiario c
        ON c.grupo = d.grupo
       AND c.id_tecnico = d.id_tecnico
       AND ISNULL(c.e_eliminado, 0) = 0
    GROUP BY d.id_grupo, d.grupo, d.supervisor, d.id_tecnico, d.tecnico
    ORDER BY d.grupo, d.tecnico;
END

GO

/* [dbo].[spx_grupo_marcarsupervisorausentecentral] */
CREATE PROCEDURE dbo.spx_Grupo_MarcarSupervisorAusenteCentral
    @IdUsuarioEjecutor INT,
    @IdGrupo INT,
    @IdUsuarioTecnico INT
AS
BEGIN
    SET NOCOUNT ON;

    IF @IdGrupo IS NULL OR @IdGrupo <= 0
    BEGIN
        RAISERROR('IdGrupo es requerido.', 16, 1);
        RETURN;
    END
    IF @IdUsuarioTecnico IS NULL OR @IdUsuarioTecnico <= 0
    BEGIN
        RAISERROR('IdUsuarioTecnico es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Grupo g
        WHERE g.id_grupo = @IdGrupo
          AND ISNULL(g.e_eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('Grupo no encontrado o eliminado.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_DetalleGrupo dg
        WHERE dg.id_grupo = @IdGrupo
          AND dg.id_usuario_tecnico = @IdUsuarioTecnico
    )
    BEGIN
        RAISERROR('El tecnico temporal debe pertenecer al grupo.', 16, 1);
        RETURN;
    END

    UPDATE dbo.tbl_GrupoBackup
    SET e_activo = 0,
        fecha_fin = ISNULL(fecha_fin, GETDATE()),
        id_usuario_actualiza = @IdUsuarioEjecutor,
        fecha_actualizacion = GETDATE()
    WHERE id_grupo = @IdGrupo
      AND e_activo = 1;

    INSERT INTO dbo.tbl_GrupoBackup (
        id_grupo,
        id_usuario_tecnico_temporal,
        e_activo,
        fecha_inicio,
        id_usuario_registra
    )
    VALUES (
        @IdGrupo,
        @IdUsuarioTecnico,
        1,
        GETDATE(),
        @IdUsuarioEjecutor
    );

    UPDATE dbo.tbl_Grupo
    SET supervisor_ausente = 1
    WHERE id_grupo = @IdGrupo;

    SELECT TOP 1
        g.id_grupo,
        g.nombre,
        g.supervisor_ausente,
        gb.id_usuario_tecnico_temporal AS id_tecnico_temporal_backup
    FROM dbo.tbl_Grupo g
    INNER JOIN dbo.tbl_GrupoBackup gb
            ON gb.id_grupo = g.id_grupo
           AND gb.e_activo = 1
    WHERE g.id_grupo = @IdGrupo
    ORDER BY gb.id_grupo_backup DESC;
END

GO

/* [dbo].[spx_grupo_quitartecnicocentral] */
CREATE PROCEDURE dbo.spx_Grupo_QuitarTecnicoCentral
    @IdUsuarioEjecutor INT,
    @IdGrupo INT,
    @IdUsuarioTecnico INT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @IdTecnicoResuelto INT = NULL;

    IF @IdGrupo IS NULL OR @IdGrupo <= 0
    BEGIN
        RAISERROR('IdGrupo es requerido.', 16, 1);
        RETURN;
    END

    IF @IdUsuarioTecnico IS NULL OR @IdUsuarioTecnico <= 0
    BEGIN
        RAISERROR('IdUsuarioTecnico es requerido.', 16, 1);
        RETURN;
    END

    IF EXISTS (SELECT 1 FROM dbo.tbl_UsuarioTecnico ut WHERE ut.id = @IdUsuarioTecnico)
    BEGIN
        SET @IdTecnicoResuelto = @IdUsuarioTecnico;
    END
    ELSE
    BEGIN
        SELECT TOP 1 @IdTecnicoResuelto = ut.id
        FROM dbo.tbl_UsuarioTecnico ut
        WHERE ut.id_Vendedor = @IdUsuarioTecnico
          AND ISNULL(ut.e_eliminado, 0) = 0;
    END

    IF @IdTecnicoResuelto IS NULL
    BEGIN
        SET @IdTecnicoResuelto = @IdUsuarioTecnico;
    END

    IF EXISTS (
        SELECT 1
        FROM dbo.tbl_GrupoBackup gb
        WHERE gb.id_grupo = @IdGrupo
          AND gb.e_activo = 1
          AND gb.id_usuario_tecnico_temporal = @IdTecnicoResuelto
    )
    BEGIN
        RAISERROR('No se puede quitar el tecnico temporal activo. Primero cambie colaborador o restaure supervisor.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_DetalleGrupo dg
        WHERE dg.id_grupo = @IdGrupo
          AND dg.id_usuario_tecnico = @IdTecnicoResuelto
    )
    BEGIN
        RAISERROR('El tecnico no pertenece al grupo seleccionado.', 16, 1);
        RETURN;
    END

    DELETE FROM dbo.tbl_DetalleGrupo
    WHERE id_grupo = @IdGrupo
      AND id_usuario_tecnico = @IdTecnicoResuelto;

    SELECT
        @IdGrupo AS id_grupo,
        @IdTecnicoResuelto AS id_usuario_tecnico,
        CAST(1 AS BIT) AS quitado;
END

GO

/* [dbo].[spx_grupo_restaurarsupervisorcentral] */
CREATE PROCEDURE dbo.spx_Grupo_RestaurarSupervisorCentral
    @IdUsuarioEjecutor INT,
    @IdGrupo INT
AS
BEGIN
    SET NOCOUNT ON;

    IF @IdGrupo IS NULL OR @IdGrupo <= 0
    BEGIN
        RAISERROR('IdGrupo es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Grupo g
        WHERE g.id_grupo = @IdGrupo
          AND ISNULL(g.e_eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('Grupo no encontrado o eliminado.', 16, 1);
        RETURN;
    END

    UPDATE dbo.tbl_GrupoBackup
    SET e_activo = 0,
        fecha_fin = ISNULL(fecha_fin, GETDATE()),
        id_usuario_actualiza = @IdUsuarioEjecutor,
        fecha_actualizacion = GETDATE()
    WHERE id_grupo = @IdGrupo
      AND e_activo = 1;

    UPDATE dbo.tbl_Grupo
    SET supervisor_ausente = 0
    WHERE id_grupo = @IdGrupo;

    SELECT TOP 1 g.id_grupo, g.nombre, g.supervisor_ausente
    FROM dbo.tbl_Grupo g
    WHERE g.id_grupo = @IdGrupo;
END

GO

/* [dbo].[spx_guardarprivilegiosrol] */
CREATE PROCEDURE dbo.spx_GuardarPrivilegiosRol
    @IdRol INT,
    @MenuIdsCsv NVARCHAR(MAX) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @IdRol IS NULL
    BEGIN
        RAISERROR('IdRol es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Rol r
        WHERE r.Id_Rol = @IdRol
          AND ISNULL(r.E_Eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('Rol no encontrado o inactivo.', 16, 1);
        RETURN;
    END

    DECLARE @MenuIds TABLE (
        Id_Menu INT NOT NULL PRIMARY KEY
    );

    SET @MenuIdsCsv = ISNULL(@MenuIdsCsv, '');
    SET @MenuIdsCsv = REPLACE(@MenuIdsCsv, ' ', '');

    IF LEN(LTRIM(RTRIM(@MenuIdsCsv))) > 0
    BEGIN
        DECLARE @xml XML;
        DECLARE @sanitized NVARCHAR(MAX);
        SET @sanitized = @MenuIdsCsv;
        WHILE CHARINDEX(',,', @sanitized) > 0
        BEGIN
            SET @sanitized = REPLACE(@sanitized, ',,', ',');
        END
        IF LEFT(@sanitized, 1) = ','
        BEGIN
            SET @sanitized = SUBSTRING(@sanitized, 2, LEN(@sanitized) - 1);
        END
        IF RIGHT(@sanitized, 1) = ','
        BEGIN
            SET @sanitized = LEFT(@sanitized, LEN(@sanitized) - 1);
        END

        IF LEN(@sanitized) > 0
        BEGIN
            SET @xml = CAST('<x>' + REPLACE(@sanitized, ',', '</x><x>') + '</x>' AS XML);

            INSERT INTO @MenuIds (Id_Menu)
            SELECT DISTINCT CAST(T.c.value('.', 'nvarchar(30)') AS INT)
            FROM @xml.nodes('/x') AS T(c)
            WHERE ISNUMERIC(T.c.value('.', 'nvarchar(30)')) = 1
              AND CAST(T.c.value('.', 'nvarchar(30)') AS INT) > 0;
        END
    END

    IF EXISTS (
        SELECT 1
        FROM @MenuIds i
        LEFT JOIN dbo.tbl_Tabla_Menu m
               ON m.ID_MENU = i.Id_Menu
              AND ISNULL(m.E_Eliminado, 0) = 0
        WHERE m.ID_MENU IS NULL
    )
    BEGIN
        RAISERROR('MenuIds contiene elementos inexistentes o inactivos.', 16, 1);
        RETURN;
    END

    BEGIN TRY
        BEGIN TRANSACTION;

        UPDATE dbo.tbl_RolMenu
        SET E_Eliminado = 1
        WHERE Id_Rol = @IdRol
          AND ISNULL(E_Eliminado, 0) = 0;

        UPDATE rm
        SET rm.E_Eliminado = 0
        FROM dbo.tbl_RolMenu rm
        INNER JOIN @MenuIds i ON i.Id_Menu = rm.Id_Menu
        WHERE rm.Id_Rol = @IdRol;

        INSERT INTO dbo.tbl_RolMenu (Id_Menu, Id_Rol, E_Eliminado)
        SELECT i.Id_Menu, @IdRol, 0
        FROM @MenuIds i
        WHERE NOT EXISTS (
            SELECT 1
            FROM dbo.tbl_RolMenu rm
            WHERE rm.Id_Rol = @IdRol
              AND rm.Id_Menu = i.Id_Menu
        );

        COMMIT TRANSACTION;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
        BEGIN
            ROLLBACK TRANSACTION;
        END
        DECLARE @ErrMsg NVARCHAR(4000);
        SET @ErrMsg = ERROR_MESSAGE();
        RAISERROR(@ErrMsg, 16, 1);
        RETURN;
    END CATCH

    EXEC dbo.spx_ObtenerPrivilegiosRolDetalle @IdRol;
END

GO

/* [dbo].[spx_ImportFromExcel03] */
 CREATE PROCEDURE spx_ImportFromExcel03
    @SheetName varchar(20),
    @FilePath varchar(100),
    @HDR varchar(3),
    @TableName varchar(50)
AS
BEGIN
    DECLARE @SQL nvarchar(1000)
    IF OBJECT_ID (@TableName,'U') IS NOT NULL
      SET @SQL = 'INSERT INTO ' + @TableName + ' SELECT * FROM OPENDATASOURCE'
    ELSE
      SET @SQL = 'SELECT * INTO ' + @TableName + ' FROM OPENDATASOURCE'
 
    SET @SQL = @SQL + '(''Microsoft.Jet.OLEDB.4.0'',''Data Source='
    SET @SQL = @SQL + @FilePath + ';Extended Properties=''''Excel 8.0;HDR='
    SET @SQL = @SQL + @HDR + ''''''')...['
    SET @SQL = @SQL + @SheetName + ']'
    EXEC sp_executesql @SQL
END
GO

/* [dbo].[spx_ImportFromExcel07] */
CREATE PROCEDURE spx_ImportFromExcel07
   @SheetName varchar(20),
   @FilePath varchar(100),
   @HDR varchar(3),
   @TableName varchar(50)
AS
BEGIN
    DECLARE @SQL nvarchar(1000)
    IF OBJECT_ID (@TableName,'U') IS NOT NULL
      SET @SQL = 'INSERT INTO ' + @TableName + ' SELECT * FROM OPENDATASOURCE'
    ELSE
      SET @SQL = 'SELECT * INTO ' + @TableName + ' FROM OPENDATASOURCE'
    SET @SQL = @SQL + '(''Microsoft.ACE.OLEDB.12.0'',''Data Source='
    SET @SQL = @SQL + @FilePath + ';Extended Properties=''''Excel 12.0;HDR='
    SET @SQL = @SQL + @HDR + ''''''')...['
    SET @SQL = @SQL + @SheetName + ']'
    EXEC sp_executesql @SQL
END

GO

/* [dbo].[spx_la_listartecnicossucursal] */
CREATE PROCEDURE [dbo].[spx_LA_ListarTecnicosSucursal]
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        CAST(v.Id_Vendedor AS INT) AS idTecnico,
        CAST(v.Id_Vendedor AS INT) AS id_tecnico,
        ISNULL(NULLIF(LTRIM(RTRIM(v.Nombre)), ''), 'Tecnico ' + CAST(v.Id_Vendedor AS VARCHAR(20))) AS tecnico,
        ISNULL(NULLIF(LTRIM(RTRIM(v.CodEmpleado)), ''), NULLIF(LTRIM(RTRIM(u.CodEmpleado)), '')) AS codEmpleado,
        ISNULL(NULLIF(LTRIM(RTRIM(v.CuentaSF)), ''), '-') AS cuentaSf,
        ISNULL(NULLIF(LTRIM(RTRIM(v.habilidad)), ''), '-') AS habilidad,
        ut.id_Usuario AS idUsuario,
        ISNULL(NULLIF(LTRIM(RTRIM(u.Loggin)), ''), '') AS loggin
    FROM dbo.tbl_Vendedor v
    LEFT JOIN dbo.tbl_UsuarioTecnico ut ON ut.Id_Vendedor = v.Id_Vendedor AND ISNULL(ut.e_eliminado, 0) = 0
    LEFT JOIN dbo.tbl_Usuario u ON u.Id_Usuario = ut.id_Usuario AND ISNULL(u.E_Eliminado, 0) = 0
    WHERE ISNULL(v.E_Eliminado, 0) = 0
    AND v.Id_Vendedor > 0
    ORDER BY v.Nombre;
END

GO

/* [dbo].[spx_ListaCorreccionErrores] */


CREATE proc [dbo].[spx_ListaCorreccionErrores]
as
select  * from tbl_CorreccionErrores where e_eliminado=0
order by estadomodificacion desc

GO

/* [dbo].[spx_ListadoBajasProductos] */
CREATE proc spx_ListadoBajasProductos
as
	select b.* , u.Nombre Usuario
	from tbl_BajaProductos b inner join tbl_Usuario u
	on b.id_Usuario = u.Id_Usuario
	order by b.id_BajaProductos desc

GO

/* [dbo].[spx_ListadoDescuentosDTHPrepagoAntesBaja] */
create proc [dbo].[spx_ListadoDescuentosDTHPrepagoAntesBaja] 
as
select * from tbl_DescuentosDTHPrepagoAntesBaja where e_eliminado=0
order by id desc
GO

/* [dbo].[spx_ListadoProducto] */
CREATE proc [dbo].[spx_ListadoProducto]
as
begin
	select pr.Id_Producto,-- pr.Prefijo,
	 pr.Nombre NombreProducto,--pr.Medida,
	tp.Id_TipoProducto, tp.Nombre NombreTipoProducto,
	case when pr.PermiteDecimales=1 then 'Si' else 'No' end PermiteDecimales,  
	case when pm.Mascara is null or pm.Mascara ='' then 'No' else 'Si' end TieneSerial,
	pr.DigitosImei CantidadDigitosSerial,
	pm.Mascara MascaraSerial, pm.ImportarSerial FormatoImportarSerial,
	case when pr.SerieTieneEspacio=1 then 'Si' else 'No' end SerieTieneEspacio,
	case when pm.MascaraChipId is null or pm.MascaraChipId='' then 'No' else 'Si' end TieneChipId,
	pr.DigitosChipId CantidadDigitosChipId, 
	pm.MascaraChipId ,pm.ImportarChipId FormatoImportarChipId,
	case when pr.ChipIdTieneEspacio=1 then 'Si' else 'No' end ChipIdTieneEspacio,
	case when pr.E_Eliminado=0 then 'No' else 'Si' end Eliminado,
	pr.PrecioVenta,
	pr.PrecioUsado,
	pr.SufijoNomenclador
	from tbl_producto pr inner join tbl_tipoproducto tp on pr.id_tipoproducto=tp.id_tipoproducto
	left join tbl_productosmascaras pm on pm.id_producto = pr.id_producto AND pm.e_eliminado=0
	where pr.e_eliminado=0 --and pm.e_eliminado=0
	order by pr.id_producto desc
end

GO

/* [dbo].[spx_ListadoPromedioUsoMateriales] */
create proc spx_ListadoPromedioUsoMateriales
as
select pr.Id_TipoServicio,ts.Nombre TipoServicio,tp.Id_TipoProducto ,tp.Nombre TipoProducto,pro.Id_Producto,pro.Nombre Producto,Cantidad,
case when pr.AdicionaEnForm=0 then  'No' else 'Si' end AdicionaEnForm,
case when pr.CantidadMaximaNM=0 then  'No' else 'Si' end CantidadMaximaNM,
pr.Id_PromedioUsoMateriales
from tbl_PromedioUsoMateriales pr inner join tbl_TipoServicio ts on ts.Id_TipoServicio = pr.Id_TipoServicio
inner join tbl_TipoProducto tp on tp.Id_TipoProducto = pr.Id_TipoProducto
left join tbl_producto pro on pro.Id_Producto = pr.Id_Producto
where pr.E_Eliminado =0
order by pr.Id_TipoServicio

GO

/* [dbo].[spx_listarotfinalizadas] */
CREATE PROCEDURE [dbo].[spx_listarotfinalizadas] AS BEGIN SET NOCOUNT ON; END
GO

/* [dbo].[spx_ListaSalesForce] */
create proc spx_ListaSalesForce
	as
	select * from tbl_SalesForce where e_eliminado=0
GO

/* [dbo].[spx_ModicarMontoComision_CobroTH] */
create proc spx_ModicarMontoComision_CobroTH(@idcontrol int, @agente nvarchar(15),@EH int,@mescomision int, @TotalComision decimal(18,2),
@Prepago decimal(18,2),@Postpago decimal(18,2),@HfcHome decimal(18,2),@observacion nvarchar(max),@idUsuario int  )
as
if(@agente ='MakiroB')
begin
	update tbl_controlserteldthprepagop 
	set TotalComision = @TotalComision, TotalAbonado=@TotalComision, prepago=@prepago, postpago=@postpago,hfchome=@hfcHome,
	observacion=+@observacion,
	IdUsuarioImportacion=@idUsuario
	where eh = @eh and mescomision=@mescomision and id_control=@idcontrol
end

GO

/* [dbo].[spx_ModIdIngresoMaterialTigo] */
CREATE proc spx_ModIdIngresoMaterialTigo( @Id_Ingreso int, @Id_IngresoMaterialTigo int, @completo bit )
as 
	insert into tbl_IngresoMaterialTigo_Almacen values( @Id_IngresoMaterialTigo, @Id_Ingreso );
	if ( @completo = 1)
	begin
		update tbl_IngresoMaterialTigo 
		set  estadoIngresoCompleto = 1
		where id_IngresoMaterialTigo = @Id_IngresoMaterialTigo
	end

GO

/* [dbo].[spx_ModificarAnticipo_BajaProductos] */
create proc [dbo].[spx_ModificarAnticipo_BajaProductos](@codigoTransaccion int, @id int , @codigoBaja int , @tabla nvarchar(50))
as 
begin
	SET XACT_ABORT ON
	
		update [tigo.makiro.com.bo].bdSistemaboletasmakiro.dbo.tbl_anticipo  
		set idUsuarioRegPlanillaQ = @codigoTransaccion ,ObservacionRegPlanillaQ=ObservacionRegPlanillaQ+convert(nvarchar(10),@codigoBaja)+'-'+ @tabla 
		where id_anticipo=@id
 	SET XACT_ABORT OFF
 end 

GO

/* [dbo].[spx_ModificarAnticipoPago_BajaCUNR] */
CREATE proc spx_ModificarAnticipoPago_BajaCUNR(@codigo int, @idAnticipo int,@tabla nvarchar(25))
as 
SET XACT_ABORT ON
 update [tigo.makiro.com.bo].bdSistemaBoletasMakiro.dbo.tbl_anticipo 
 set idUsuarioRegPlanillaQ = @codigo,
 ObservacionRegPlanillaQ=ObservacionRegPlanillaQ+(convert(nvarchar(10),@codigo))+'-'+@tabla
 where id_anticipo=@idAnticipo
 
 SET XACT_ABORT OFF
 
GO

/* [dbo].[spx_ModificarBajaProductosPendienteRegistrado] */
CREATE proc [dbo].[spx_ModificarBajaProductosPendienteRegistrado](@Id_BajaProductosPendiente int, @FechaRegistrado datetime,@Id_UsuarioRegistrado int,
@Observacion nvarchar(max),@id_BajaProductos int)
as
begin
	update tbl_BajaProductosPendiente
	set FechaRegistrado=@FechaRegistrado,
	FechaRegistroRegistrado=GETDATE(),
	Id_UsuarioRegistrado=@Id_UsuarioRegistrado,
	Observacion =Observacion +' // '+@Observacion,
	Id_BajaProductos=@id_BajaProductos,
	Estado='Completo'
	where Id_BajaProductosPendiente=@Id_BajaProductosPendiente	
end

GO

/* [dbo].[spx_ModificarContraseña] */

CREATE proc [dbo].[spx_ModificarContraseña](@Id_Usuario int,@Loggin nvarchar(50),@Contraseña nvarchar(50),@accion int,@TipoUsuario int,@Id_TipoUsuario int,@correo nvarchar(150))--,@Id_Rol int)
as
if(@accion = 1) --modificar datos formulario usuario
begin
	update tbl_Usuario 
	set  ultimaModificacion=GETDATE(),	TipoUsuario = @TipoUsuario, Id_Rol=@Id_TipoUsuario
	where Id_Usuario =@Id_Usuario 	
end
if(@accion = 2)
begin--modifica contraseña desde formulario interno
	update tbl_Usuario 
	set Password =@Contraseña , ultimaModificacion=GETDATE(),necesitaCambio =0
	where Id_Usuario =@Id_Usuario 
	
	insert into tbl_UsuarioContraseñas values (@Id_Usuario,@Loggin,@Contraseña,GETDATE())
end
if(@accion = 3)
begin--modifica contraseña desde formulario usuario 
	update tbl_Usuario 
	set Password =@Contraseña , ultimaModificacion=GETDATE(),necesitaCambio =1, correo=@correo
	where Id_Usuario =@Id_Usuario 
end



GO

/* [dbo].[spx_ModificarDatosTecnicoSf] */
create proc [dbo].[spx_ModificarDatosTecnicoSf](@Id_Vendedor int,@salesforce nvarchar(150),@cuentaSF nvarchar(150),@habilidad nvarchar(150),@idVehiculo int,
@vehiculo nvarchar(50),@GrupoDigitacion nvarchar(50),@idUsuarioRegistro int )
as
update tbl_vendedor
set salesForce=@salesforce,cuentaSF=@cuentaSF,habilidad=@habilidad,idVehiculo=@idVehiculo,
Vehiculo=@Vehiculo,grupodigitacion=@grupodigitacion,idUsuarioRegistro = @idUsuarioRegistro
where id_vendedor=@Id_Vendedor



GO

/* [dbo].[spx_ModificarDatosVendedor] */
CREATE proc [dbo].[spx_ModificarDatosVendedor](@Direccion nvarchar(250),@Ci nvarchar(100), @Telefono nvarchar(50), 
	@Observacion nvarchar(max), @CodEmpleado nvarchar(15),@Id_Vendedor int,@Id_TipoSolicitante int ,
	@salesforce nvarchar(250),@cuentaSF nvarchar(150),@habilidad nvarchar(150),@idvehiculo int,@vehiculo nvarchar(150),
	@grupoDigitacion  nvarchar(25)
	)
as
update tbl_vendedor
set direccion=@direccion,
	ci=@ci,telefono=@telefono,observacion=@observacion,codEmpleado=@codEmpleado,
	Id_TipoSolicitante =@Id_TipoSolicitante,
	salesforce=@salesforce, cuentaSF=@cuentaSF,habilidad=@habilidad,
	idvehiculo=@idvehiculo,
	vehiculo=@vehiculo, grupoDigitacion=@grupoDigitacion	
where id_vendedor=@Id_Vendedor


GO

/* [dbo].[spx_ModificarDescuentoDTHPrepago] */
 create proc spx_ModificarDescuentoDTHPrepago(@id int, @codigobaja int , @tabla nvarchar(15))
 as 
 begin
	SET XACT_ABORT ON
	
		update [tigo.makiro.com.bo].bdcomisiones.dbo.tbl_controldthprepagoDescuentos_Cobros  
		set tomadoEnCuentaBaja = 1, CodigoBaja=codigobaja, tabla=@tabla
		where id=@id
		
		SET XACT_ABORT OFF
 end
GO

/* [dbo].[spx_ModificarDescuentoPdaInstaladores] */
 create proc spx_ModificarDescuentoPdaInstaladores(@id int, @codigobaja int , @tabla nvarchar(15))
 as 
 begin
	SET XACT_ABORT ON
	
		update [tigo.makiro.com.bo].bdcomisiones.dbo.tbl_controlPdaInstaladoresDescuentos_Cobros  
		set tomadoEnCuentaBaja = 1,CodigoBaja=@codigobaja,tabla=@tabla
		where id=@id
		
	SET XACT_ABORT OFF
end
 
GO

/* [dbo].[spx_ModificarDevolucionTigoPendienteEntregado] */

CREATE proc [dbo].[spx_ModificarDevolucionTigoPendienteEntregado](@Id_DevolucionTP int, @FechaEntregado datetime,@Id_UsuarioEntregado int,
@Observacion nvarchar(max),@accion int,@iddevolucion int)
as
if(@accion =1 )
begin
	update tbl_devoluciontigopendiente
	set FechaEntregado=@FechaEntregado,
	Id_UsuarioEntregado=@Id_UsuarioEntregado,
	Observacion =Observacion +' // '+@Observacion,
	Estado='EntregaTigoSCS-Entregado'
	where Id_DevolucionTigoPendiente=@Id_DevolucionTP
end
else
begin
	update tbl_devoluciontigopendiente
	set FechaRegistrado=@FechaEntregado,
	Id_UsuarioRegistrado=@Id_UsuarioEntregado,
	Observacion =Observacion +' // '+@Observacion,
	Estado='EntregaTigoSCS-Registrado',
	id_devolucion=@iddevolucion
	where Id_DevolucionTigoPendiente=@Id_DevolucionTP
end
GO

/* [dbo].[spx_ModificarEstadoCierrePRPD_Cierre] */
create proc [dbo].[spx_ModificarEstadoCierrePRPD_Cierre](@fecha datetime)
as
begin
update tbl_CierreAlmacen set CierreAlmacenPR_PD=1 where dbo.DateOnly(fecha)=dbo.DateOnly(@fecha)
end

GO

/* [dbo].[spx_ModificarIdEstado] */

create proc [dbo].[spx_ModificarIdEstado](@idNuevoEstado int,@serial nvarchar(150),@chipid nvarchar(150),@id_producto int) 
as
update tbl_productos 
set id_estadoproducto=@idNuevoEstado
where serial=@serial and chipid=@chipid and id_producto=@id_producto


GO

/* [dbo].[spx_ModificarNombreProductoC4] */

create proc [dbo].[spx_ModificarNombreProductoC4](@idProducto int,@serial nvarchar(25),@chipid nvarchar(25))
as
begin 
	begin transaction
		begin try							
			--aqui tiene q ir el codigo
			update tbl_productos set id_producto=@idProducto where serial=@serial and chipid=@chipid			
		commit transaction
		end try
		begin catch
			rollback transaction
			select error_message() as Error
		end catch
end

GO

/* [dbo].[spx_ModificarObservacionEjecutados] */

CREATE proc [dbo].[spx_ModificarObservacionEjecutados]
(@Id int,@TipoServicio nvarchar(50),@Tor nvarchar(50),@Poblacion nvarchar(50),
@FechaCierre datetime,@Georeferencia nvarchar(50),@Altura nvarchar(50),
@Cierre nvarchar(50),@ObservacionCierre nvarchar(max),@IdGrupo int,@Grupo nvarchar(250),
@Cuadrilla nvarchar(50),@Id_UsuarioModifica int)
as
	if(@TipoServicio = 'DTH_PREPAGO')
	begin
		update tbl_DatosDTHPrepago 
		set TOR=@TOR,Poblacion_=@Poblacion,
		FechaCierre = @FechaCierre,Georeferencia = @Georeferencia,
		Altura = @Altura,Cierre=@Cierre,
		ObservacionCierre= @ObservacionCierre,
		IdGrupo=@IdGrupo,Grupo=@Grupo,Cuadrilla=@Cuadrilla,
		Id_UsuarioModEjecucion=@Id_UsuarioModifica 
		where ID=@Id
	end
	if(@TipoServicio = 'HFC')
	begin
		update tbl_DatosHFC 
		set TOR=@TOR,Poblacion_=@Poblacion,
		FechaCierre = @FechaCierre,Georeferencia = @Georeferencia,
		Altura = @Altura,Cierre=@Cierre,
		ObservacionCierre= @ObservacionCierre,
		IdGrupo=@IdGrupo,Grupo=@Grupo,Cuadrilla=@Cuadrilla,
		Id_UsuarioModEjecucion=@Id_UsuarioModifica 
		where ID=@Id
	end
	if(@TipoServicio = 'POST_PAGO')
	begin
		update tbl_DatosPostPago 
		set TOR=@TOR,Poblacion_=@Poblacion,
		FechaCierre = @FechaCierre,Georeferencia = @Georeferencia,
		Altura = @Altura,Cierre=@Cierre,
		ObservacionCierre= @ObservacionCierre,
		IdGrupo=@IdGrupo,Grupo=@Grupo,Cuadrilla=@Cuadrilla,
		Id_UsuarioModEjecucion=@Id_UsuarioModifica 
		where ID=@Id
	end
GO

/* [dbo].[spx_ModificarObservacionProgramados] */
CREATE proc [dbo].[spx_ModificarObservacionProgramados]
(@Id int,@TipoServicio nvarchar(50),@FechaGestion datetime,@EstadoGestionDealer nvarchar(50),@FechaProgramacion datetime,
@Observacion nvarchar(max),@Id_UsuarioModifica int)
as
	if(@TipoServicio = 'DTH_PREPAGO')
	begin
		update tbl_DatosDTHPrepago 
		set FechaGestion=@FechaGestion,EstadoGestionDealer=@EstadoGestionDealer,
		FechaProgramacion = @FechaProgramacion,observacion= @Observacion,
		Id_UsuarioModifica=@Id_UsuarioModifica 
		where ID=@Id
	end
	if(@TipoServicio = 'HFC')
	begin
		update tbl_DatosHFC 
		set FechaGestion=@FechaGestion,EstadoGestionDealer=@EstadoGestionDealer,
		FechaProgramacion = @FechaProgramacion,observacion= @Observacion,
		Id_UsuarioModifica=@Id_UsuarioModifica 
		where ID=@Id
	end
	if(@TipoServicio = 'POST_PAGO')
	begin
		update tbl_DatosPostPago 
		set FechaGestion=@FechaGestion,EstadoGestionDealer=@EstadoGestionDealer,
		FechaProgramacion = @FechaProgramacion,observacion= @Observacion,
		Id_UsuarioModifica=@Id_UsuarioModifica 
		where ID=@Id
	end
GO

/* [dbo].[spx_ModificarOrdenTrabajoFecha] */
create proc [dbo].[spx_ModificarOrdenTrabajoFecha](@Id_UsuarioModifico int, @fecha datetime,@Id_Venta int)
as
declare @Id_Devolucion int
set @Id_Devolucion = 0
set @Id_Devolucion= (select Id_devolucion from tbl_Devolucion WHERE Id_Venta=@Id_Venta)
select @Id_Devolucion
	if(@Id_Devolucion>0)	
		update tbl_Devolucion set Fecha=@fecha where Id_Venta=@Id_Venta
	
	update tbl_Venta set Fecha_Ejecucion=@fecha,Id_UsuarioE=@Id_UsuarioModifico where Id_Venta =@Id_Venta 


GO

/* [dbo].[spx_ModificarProducto] */

CREATE proc [dbo].[spx_ModificarProducto](@IdProducto int,@PermiteDecimal bit,@Habilitado bit
,@CantDigitosSerial int,@TieneEspacioSerial bit,@Mascara nvarchar(50),@SerialImportacion nvarchar(150)
,@CantDigitosChipId int,@TieneEspacioChipId bit,@MascaraChipId nvarchar(50),@ChipIdImportacion nvarchar(150),
@PrecioVenta decimal(18,2),@PrecioUsado decimal(18,2),@SufijoNomenclador nvarchar(5),@Id_Usuario int )
as
begin	
	update tbl_producto 
	set digitosimei=@CantDigitosSerial,digitoschipid=@CantDigitosChipId,
	serietieneespacio=@TieneEspacioSerial, chipidtieneespacio=@TieneEspacioChipId, permitedecimales=@PermiteDecimal,
	precioventa = @PrecioVenta,preciousado = @PrecioUsado,SufijoNomenclador = @SufijoNomenclador,
	e_eliminado=@habilitado,
	id_Usuario=	@Id_Usuario
	where id_producto=@IdProducto

	declare @existeregistro int
	set @existeregistro = (select count(*) from tbl_productosmascaras  where id_producto=@IdProducto)
	if(@existeregistro >0)
	begin
		update tbl_productosmascaras  
		set mascara=@Mascara,mascarachipid=@MascaraChipId,importarChipId=@ChipIdImportacion,importarserial=@SerialImportacion,
		formatochipid=@MascaraChipId,formatoserial=@Mascara,
		e_eliminado=@habilitado	
		where id_producto=@IdProducto
	end
	else 
	begin
		
		if (@CantDigitosSerial > 0)
		begin
			insert into tbl_productosmascaras 
			values (
				@IdProducto, @Mascara, 0, @MascaraChipId,
				@ChipIdImportacion, @SerialImportacion, @MascaraChipId, @Mascara
			)
		end
	end
	exec sp_CrearProductoEnSaldoTarjetaVendedor

end
GO

/* [dbo].[spx_ModificarSalidaTraspasoPendienteRegistrado] */

CREATE proc [dbo].[spx_ModificarSalidaTraspasoPendienteRegistrado](@Id_SalidaTraspasoPendiente int, @FechaRegistrado datetime,@Id_UsuarioRegistrado int,
@Observacion nvarchar(max),@id_SalidaTraspaso int)
as
begin
	update tbl_SalidaTraspasoPendiente
	set FechaRegistrado=@FechaRegistrado,
	FechaRegistroRegistrado=GETDATE(),
	Id_UsuarioRegistrado=@Id_UsuarioRegistrado,
	Observacion =Observacion +' // '+@Observacion,
	Id_SalidaTraspaso=@Id_SalidaTraspaso,
	Estado='Completo'
	where Id_SalidaTraspasoPendiente=@Id_SalidaTraspasoPendiente
end
GO

/* [dbo].[spx_ModificarSerialChipId] */
CREATE proc [dbo].[spx_ModificarSerialChipId](@serialAntiguo nvarchar(50),@serialNuevo nvarchar(50),@serieChipId bit,@Id_Usuario int)
as
--0 serial 1 chipid
begin
	begin transaction
		begin try
			declare @table  table(id int identity, tabla nvarchar(max))
			declare @idsTabla nvarchar(max)	
			declare @SerialChipId nvarchar(150)
			declare @cuantos int,@contador int,@tabla nvarchar(max)
			if(@serieChipId=0)
			begin
				set @SerialChipId ='Serial'
				insert into @table
				select distinct(tabla) from tbl_bitacora where serial=@serialAntiguo and e_eliminado=0
				set @cuantos = (select count(*) from @table)
				set @contador = 1
				
					select @idsTabla =  COALESCE(@idsTabla + ' ** ', '-') + (convert(nvarchar,id_bitacora) + ' ' + tabla) 
						FROM  tbl_bitacora where serial=@serialAntiguo
						--select @valores
						insert into tbl_BitacoraProductoCambioSerialChipID
						select @idsTabla,@serialNuevo,@serialAntiguo,@Id_Usuario,GETDATE(),@SerialChipId
						
						
					while(@contador<=@cuantos)
					begin	
						
						set @tabla =(select tabla from @table where id=@contador)
						if(@tabla='tbl_IngresoAlmacen')				
							update tbl_codigoingresoalmacen set cod_inicio=@serialNuevo where cod_inicio=@serialAntiguo				
						if(@tabla='tbl_AlmacenVendedor')
							update tbl_codigoalmacenvendedor set cod_inicio=@serialNuevo where cod_inicio=@serialAntiguo									
						if(@tabla='tbl_Venta')				
							update tbl_codigoventa set cod_inicio=@serialNuevo where cod_inicio=@serialAntiguo									
						if(@tabla='tbl_Devolucion')							
							update tbl_detalledevolucion set cod_inicio=@serialNuevo where cod_inicio=@serialAntiguo									
						if(@tabla='tbl_BajaProducto')				
							update tbl_codigobajaproductos set serie=@serialNuevo where serie=@serialAntiguo									
						if(@tabla='tbl_SalidaTraspaso')
							update tbl_codigosalidatraspaso set serie=@serialNuevo where serie=@serialAntiguo									
						if(@tabla='tbl_CodigoVentaCargoUsuario')
							update tbl_CodigoVentaCargoUsuario set serial=@serialNuevo where serial=@serialAntiguo									
						set @contador = @contador+1;
						
						
					end
						update tbl_productos set serial=@serialNuevo where serial=@serialAntiguo
						update tbl_bitacora set serial=@serialNuevo where serial=@serialAntiguo
	 
			end
			else 
			begin
				set @SerialChipId ='Chip'
				insert into @table
				select distinct(tabla) from tbl_bitacora where chipId=@serialAntiguo and e_eliminado=0
				set @cuantos = (select count(*) from @table)
				set @contador = 1
				
					select @idsTabla =  COALESCE(@idsTabla + ' ** ', '-') + (convert(nvarchar,id_bitacora) + ' ' + tabla) 
					FROM  tbl_bitacora where ChipId=@serialAntiguo						
					insert into tbl_BitacoraProductoCambioSerialChipID
					select @idsTabla,@serialNuevo,@serialAntiguo,@Id_Usuario,GETDATE(),@SerialChipId
					
						
					while(@contador<=@cuantos)
					begin	
						set @tabla =(select tabla from @table where id=@contador)
						if(@tabla='tbl_IngresoAlmacen')				
							update tbl_codigoingresoalmacen set Chip_Id=@serialNuevo where Chip_Id=@serialAntiguo				
						if(@tabla='tbl_AlmacenVendedor')				
							update tbl_codigoalmacenvendedor set ChipId=@serialNuevo where ChipId=@serialAntiguo									
						if(@tabla='tbl_Venta')
							update tbl_codigoventa set chipId=@serialNuevo where chipId=@serialAntiguo									
						if(@tabla='tbl_Devolucion')
							update tbl_detalledevolucion set chipId=@serialNuevo where chipId=@serialAntiguo									
						if(@tabla='tbl_BajaProducto')				
							update tbl_codigobajaproductos set ChipId=@serialNuevo where ChipId=@serialAntiguo									
						if(@tabla='tbl_SalidaTraspaso')
							update tbl_codigosalidatraspaso set ChipId=@serialNuevo where ChipId=@serialAntiguo									
						if(@tabla='tbl_CodigoVentaCargoUsuario')
							update tbl_CodigoVentaCargoUsuario set ChipId=@serialNuevo where ChipId=@serialAntiguo									
						set @contador = @contador+1;
						
					end
						update tbl_productos set ChipId=@serialNuevo where ChipId=@serialAntiguo
						update tbl_bitacora set chipId=@serialNuevo where chipId=@serialAntiguo
																								
			end
			
		commit transaction
		end try
		begin catch
			rollback transaction
			select error_message() as Error
		end catch

end
GO

/* [dbo].[spx_ModificarSerialChipId_3_2] */
create proc [dbo].[spx_ModificarSerialChipId_3_2](@serie1 nvarchar(25),@serie2 nvarchar(25),@chipid2 nvarchar(25))
as
--declare @serie1 nvarchar(20),@serie2 nvarchar(20),@chipid2 nvarchar(20)
declare @id_bitacora int, @idventa int
set @id_bitacora = (select max(id_bitacora) from tbl_bitacora where serial=@serie1 and e_eliminado=0)---145713
set @idventa=(select codigo from tbl_bitacora where id_bitacora=@id_bitacora )---26446
select @id_bitacora 
select @idventa 

update tbl_productos set id_estadoproducto=2 where serial=@serie1
update tbl_productos set id_estadoproducto=3 where serial=@serie2
update tbl_bitacora set serial=@serie2, chipid=@chipid2 where id_Bitacora =@id_bitacora
update tbl_codigoventa set cod_inicio=@serie2, chipid=@chipid2 where id_venta=@idventa and cod_inicio=@serie1

GO

/* [dbo].[spx_ModificarSerialChipId_CE_CNE_c1] */

create proc [dbo].[spx_ModificarSerialChipId_CE_CNE_c1](@serialErrado nvarchar(50),@chipIdErrado nvarchar(50),
														@serialCorrecto nvarchar(50),@ChipIdCorrecto nvarchar(50),@serieChipId int,@Id_Usuario int)
as
--0 serial 1 chipid
begin
	begin transaction
		begin try
			declare @table  table(id int identity, tabla nvarchar(max))
			declare @idsTabla nvarchar(max)	
			declare @SerialChipId nvarchar(150)
			declare @cuantos int,@contador int,@tabla nvarchar(max)
			if(@serieChipId=0)
			begin
				set @SerialChipId ='Serial'
				insert into @table
				select distinct(tabla) from tbl_bitacora where serial=@serialErrado and chipid=@chipIdErrado and e_eliminado=0
				set @cuantos = (select count(*) from @table)
				set @contador = 1
				
					select @idsTabla =  COALESCE(@idsTabla + ' ** ', '-') + (convert(nvarchar,id_bitacora) + ' ' + tabla) 
						FROM  tbl_bitacora where serial=@serialErrado	and chipid=@chipIdErrado 					
						insert into tbl_BitacoraProductoCambioSerialChipID					
						select @idsTabla,@serialCorrecto,@serialErrado,@Id_Usuario,GETDATE(),@SerialChipId
						
						
					while(@contador<=@cuantos)
					begin	
						
						set @tabla =(select tabla from @table where id=@contador)
						if(@tabla='tbl_IngresoAlmacen')				
							update tbl_codigoingresoalmacen set cod_inicio=@serialCorrecto where cod_inicio=@serialErrado and chip_id=@chipIdErrado 										
						if(@tabla='tbl_AlmacenVendedor')
							update tbl_codigoalmacenvendedor set cod_inicio=@serialCorrecto where cod_inicio=@serialErrado	and chipid=@chipIdErrado 															
						if(@tabla='tbl_Venta')				
							update tbl_codigoventa set cod_inicio=@serialCorrecto where cod_inicio=@serialErrado and chipid=@chipIdErrado 															
						if(@tabla='tbl_Devolucion')							
							update tbl_detalledevolucion set cod_inicio=@serialCorrecto where cod_inicio=@serialErrado	and chipid=@chipIdErrado 														
						if(@tabla='tbl_BajaProducto')				
							update tbl_codigobajaproductos set serie=@serialCorrecto where serie=@serialErrado	and chipid=@chipIdErrado 															
						if(@tabla='tbl_SalidaTraspaso')
							update tbl_codigosalidatraspaso set serie=@serialCorrecto where serie=@serialErrado and chipid=@chipIdErrado 															
						if(@tabla='tbl_CodigoVentaCargoUsuario')
							update tbl_CodigoVentaCargoUsuario set serial=@serialCorrecto where serial=@serialErrado and chipid=@chipIdErrado

						set @contador = @contador+1;					
					
					end					
						update tbl_productos set serial=@serialCorrecto where serial=@serialErrado and chipid=@chipIdErrado 
						update tbl_bitacora set serial=@serialCorrecto where serial=@serialErrado and chipid=@chipIdErrado 
						
						update tbl_CodigoDevolucionTigoPendiente set serial=@serialCorrecto where serial=@serialErrado and chipid=@chipIdErrado 
						update tbl_CodigoBajaProductosPendiente set serial=@serialCorrecto where serial=@serialErrado and chipid=@chipIdErrado 
						update tbl_CodigoSalidaTraspasoPendiente set serial=@serialCorrecto where serial=@serialErrado and chipid=@chipIdErrado 
						

	 
			end
			else 
			begin
				set @SerialChipId ='Chip'
				insert into @table
				select distinct(tabla) from tbl_bitacora where chipId=@ChipIDErrado and  serial=@serialErrado and e_eliminado=0
				set @cuantos = (select count(*) from @table)
				set @contador = 1
				
					select @idsTabla =  COALESCE(@idsTabla + ' ** ', '-') + (convert(nvarchar,id_bitacora) + ' ' + tabla) 
					FROM  tbl_bitacora where ChipId=@ChipIdErrado	and  serial=@serialErrado					
					insert into tbl_BitacoraProductoCambioSerialChipID
					select @idsTabla,@chipidCorrecto,@ChipidErrado,@Id_Usuario,GETDATE(),@SerialChipId
					

					while(@contador<=@cuantos)
					begin	
						set @tabla =(select tabla from @table where id=@contador)
						if(@tabla='tbl_IngresoAlmacen')				
							update tbl_codigoingresoalmacen set Chip_Id=@ChipIdCorrecto where Chip_Id=@chipidErrado	and  cod_inicio=@serialErrado							
						if(@tabla='tbl_AlmacenVendedor')				
							update tbl_codigoalmacenvendedor set ChipId=@ChipIdCorrecto where ChipId=@chipidErrado and  cod_inicio=@serialErrado							
						if(@tabla='tbl_Venta')
							update tbl_codigoventa set chipId=@ChipIdCorrecto where chipId=@chipidErrado and  cod_inicio=@serialErrado							
						if(@tabla='tbl_Devolucion')
							update tbl_detalledevolucion set chipId=@ChipIdCorrecto where chipId=@chipidErrado and  cod_inicio=@serialErrado
						if(@tabla='tbl_BajaProducto')				
							update tbl_codigobajaproductos set ChipId=@ChipIdCorrecto where ChipId=@chipidErrado and serie=@serialErrado							
						if(@tabla='tbl_SalidaTraspaso')
							update tbl_codigosalidatraspaso set ChipId=@ChipIdCorrecto where ChipId=@chipidErrado and  serie=@serialErrado																
						if(@tabla='tbl_CodigoVentaCargoUsuario')
							update tbl_CodigoVentaCargoUsuario set ChipId=@ChipIdCorrecto where ChipId=@chipidErrado and  serial=@serialErrado																								
							
						set @contador = @contador+1;
						
					end					
						update tbl_productos set ChipId=@ChipIdCorrecto where ChipId=@chipidErrado and  serial=@serialErrado
						update tbl_bitacora set chipId=@ChipIdCorrecto where chipId=@chipidErrado and  serial=@serialErrado
						
						update tbl_CodigoDevolucionTigoPendiente set ChipId=@ChipIdCorrecto where ChipId=@chipidErrado and  serial=@serialErrado
						update tbl_CodigoBajaProductosPendiente set ChipId=@ChipIdCorrecto where ChipId=@chipidErrado and  serial=@serialErrado
						update tbl_CodigoSalidaTraspasoPendiente set ChipId=@ChipIdCorrecto where ChipId=@chipidErrado and  serial=@serialErrado
																								
			end
			
		commit transaction
		end try
		begin catch
			rollback transaction
			select error_message() as Error
		end catch

end







GO

/* [dbo].[spx_ModificarUsuarioSucursal] */
create proc spx_ModificarUsuarioSucursal(@Id_Usuario int, @Id_Sucursal int,@Estado bit)
as
	update tbl_usuariosucursal  set e_Eliminado = @Estado where Id_Usuario=@Id_Usuario and Id_Sucursal=@Id_Sucursal
GO

/* [dbo].[spx_Obtener_BajaProductosNacional] */
create proc dbo.spx_Obtener_BajaProductosNacional
as
	select Id,Id_BajaProductos,Id_CodigoBajaProductos, Observacion,FechaTransaccion,Id_Ruta,Ruta,Id_EstadoProductos,
	EstadoProductos,Id_Tecnico,Tecnico,Id_TipoBajaProductosPendiente,TipoBajaProductosPendiente,Total,
	case when Cobrado =1 then 'Si' else 'No' end Cobrado,
	Id_TipoCobroBaja,TipoCobroBaja,Id_Sucursal,Sucursal
	from tbl_bajaproductosnacional
	order by id_bajaproductos desc
GO

/* [dbo].[spx_Obtener_CasosCorrecionErrores] */


create proc [dbo].[spx_Obtener_CasosCorrecionErrores]
as
select * from tbl_CasosCorrecionErrores where e_Eliminado=0

GO

/* [dbo].[spx_obteneractividadesconformacioncuadrillaweb] */

CREATE PROC [dbo].[spx_ObtenerActividadesConformacionCuadrillaWeb]
AS
BEGIN
    SET NOCOUNT ON;

    SELECT 'TITULAR' AS actividad
    UNION ALL
    SELECT 'BACKUP' AS actividad;
END



GO

/* [dbo].[spx_ObtenerAnticiposEmpleado] */


 --spx_ObtenerAnticiposEmpleado 'DMK-2074',1

CREATE proc [dbo].[spx_ObtenerAnticiposEmpleado](@codEmpleado nvarchar(15),@Makiro int)
as 
if(@Makiro=1)
begin
	select 1 IdFormaDePago,'Anticipo'FormaDePago,Monto Cobro,9 Id_EntidadBancaria,'ANTICIPO' EntidadBancaria,'ANTICIPO'Cuenta,ant.Fecha_Anticipo Fecha,ant.Id_Anticipo CodigoOPCaja,
	ant.Gestion,ant.Observacion,0 Descontar
	from [tigo.makiro.com.bo].bdSistemaBoletasMakiro.dbo.tbl_anticipo ant
	where estado in ('AprobadoContabilidad','DescuentoEnPlanilla' ) and idusuarioregplanillaq=0
	and id_empleado in (
	select id_empleado from [tigo.makiro.com.bo].bdSistemaBoletasMakiro.dbo.tbl_empleado where codEmpleado=@codEmpleado and e_eliminado=0)
	and e_eliminado=0
	ORDER BY ant.Id_Anticipo desc
end
else
begin

	select 0 Descontar,2 IDFormaDePago,'Comision'FormaDePago,Cobro,10 Id_EntidadBancaria,'COMISION'EntidadBancaria,'COMISION' Cuenta,ant.FechaRegistro Fecha,ant.Id CodigoOPCaja,
	ant.MesComision Gestion,ant.Observacion
	from [tigo.makiro.com.bo].bdComisiones.dbo.tbl_controldthprepagoDescuentos_Cobros ant
	where ehMakiro =@codEmpleado	and 
	e_eliminado=0 and TomadoEnCuentaBaja=0
	
	union all
	
	select 0 Descontar,6 IDFormaDePago,'ComisionTercerizado'FormaDePago,Cobro,12 Id_EntidadBancaria,'COMISIONTERCERIZADO'EntidadBancaria,'COMISIONTERCERIZADO' Cuenta,ant.FechaRegistro Fecha,ant.Id CodigoOPCaja,
	ant.MesComision Gestion,ant.Observacion
	from [tigo.makiro.com.bo].bdComisiones.dbo.tbl_controlpdainstaladoresDescuentos_Cobros ant
	where ehMakiro =@codEmpleado	and 
	e_eliminado=0 and TomadoEnCuentaBaja=0
	
	union all 
	
	select 0 Descontar,5 IDFormaDePago,'DescuentoAntesComision'FormaDePago,Pago*(-1),11 Id_EntidadBancaria,'DESCUENTOANTESCOMISION'EntidadBancaria,'DESCUENTOANTESCOMISION' Cuenta,ant.FechaRegistro Fecha,ant.Id CodigoOPCaja,
	ant.MesComision Gestion,ant.Soporte
	from tbl_descuentosdthprepagoantesbaja ant
	where eh =@codEmpleado	and 
	e_eliminado=0 and TomadoEnCuentaBaja=0
	
end 

GO

/* [dbo].[spx_obtenerauxiliaresconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ObtenerAuxiliaresConformacionCuadrillaWeb]
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        v.Id_Vendedor AS id_tecnicoAuxiliar,
        v.Nombre AS auxiliar,
        v.CuentaSF AS cuenta_sf,
        v.SalesForce AS salesforce,
        v.Habilidad AS habilidad,
        v.Vehiculo AS vehiculo,
        v.*
    FROM dbo.tbl_Vendedor v
    WHERE v.E_Eliminado = 0
    and v.Id_Vendedor>0 and v.id_tiposolicitante=1
    and id_vendedor not in (
		select id_vendedor from tbl_ruta where e_eliminado=0 and id_vendedor>0
    )
    ORDER BY v.Nombre;
END


GO

/* [dbo].[spx_ObtenerBajaProductos] */
CREATE proc [dbo].[spx_ObtenerBajaProductos](@codigo int)
as
	SELECT b.id_BajaProductos,b.id_Usuario, u.Nombre Usuario,b.observacion,b.fecha,b.fechaRegistro,
	b.Id_Ruta , r.Nombre Ruta,b.id_EstadoProductos,ep.Nombre EstadoProductos,
	v.Id_Vendedor,v.Nombre Tecnico,tp.Id_TipoBajaProductosPendiente,
	tp.TipoBaja,b.Total,case when b.cobrado = 1 then 'Si' else 'No' end Cobrado,		
	v.codEmpleado,
	(select nombre from tbl_tiposolicitante where id_tipo_solicitante=v.Id_Tiposolicitante) TipoSolicitante
	FROM tbl_BajaProductos b inner join tbl_Usuario u on u.Id_Usuario = b.id_Usuario and b.id_BajaProductos = @codigo
	inner join tbl_Ruta r on r.Id_Ruta = b.Id_Ruta
	inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = b.Id_EstadoProductos
	inner join tbl_vendedor v on v.id_Vendedor=b.id_Tecnico
	inner join tbl_TipoBajaProductosPendiente tp on tp.Id_TipoBajaProductosPendiente=b.Id_TipoBajaProductosPendiente
	where b.e_eliminado=0  
	
	SELECT cb.id_CodigoBajaProductos,cb.id_BajaProductos,p.Id_Producto,p.Nombre Producto,cb.serie,cb.ChipID,cb.cantidad,
	cb.PrecioU ,
	cb.TotalP --,cb.estado
	FROM tbl_CodigoBajaProductos cb inner join tbl_producto p on p.Id_Producto = cb.id_Producto
	and cb.id_BajaProductos = @codigo
	where cb.e_eliminado=0 order by p.nombre
	
	SELECT cb.id_DetalleBajaProductos,cb.id_BajaProductos,p.Id_Producto,p.Nombre Producto,cb.Saldo,cb.Baja,cb.ItemsHoy
	FROM tbl_DetalleBajaProductos cb inner join tbl_producto p on p.Id_Producto = cb.id_Producto
	and cb.id_BajaProductos = @codigo
	where cb.e_eliminado=0  order by p.nombre
	

GO

/* [dbo].[spx_ObtenerBajaProductosNacional_xID] */
create proc [dbo].[spx_ObtenerBajaProductosNacional_xID](@idBaja int,@idSucursal int)
as 
select * from tbl_bajaproductosnacional  where id_bajaproductos=@idBaja and id_sucursal=@idSucursal
---------------------------------------------------------------

GO

/* [dbo].[spx_ObtenerBajaProductosPendiente] */
CREATE proc [dbo].[spx_ObtenerBajaProductosPendiente](@codigo int)
as
BEGIN
	SELECT b.Id_BajaProductosPendiente,b.FechaPendiente,b.FechaRegistrado,b.Estado,
	b.Id_UsuarioPendiente,	u.Nombre UsuarioPendiente,	
	b.observacion,	b.Id_Ruta ,	 r.Nombre Ruta,b.id_EstadoProductos,
	ep.Nombre EstadoProductos,
	v.Id_Vendedor,v.Nombre Tecnico,b.FechaRegistroPendiente,b.FechaRegistroRegistrado, B.Id_BajaProductos,tp.Id_TipoBajaProductosPendiente,
	tp.TipoBaja, b.total
	FROM tbl_BajaProductosPendiente b inner join tbl_Usuario u on u.Id_Usuario = b.Id_UsuarioPendiente 
	and b.id_BajaProductosPendiente = @codigo	
	inner join tbl_Ruta r on r.Id_Ruta = b.Id_Ruta
	inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = b.Id_EstadoProductos
	inner join tbl_vendedor v on v.id_Vendedor=b.id_Tecnico
	inner join tbl_TipoBajaProductosPendiente tp on tp.Id_TipoBajaProductosPendiente=b.Id_TipoBajaProductosPendiente
	where b.e_eliminado=0 
	
	
	SELECT cb.Id_DetalleBajaProductosPendiente,cb.Id_BajaProductosPendiente,p.Id_Producto,p.Nombre Producto,
	sum(s.cantidad) Saldo,sum(cb.Baja)Baja,sum (Itemshoy) ItemsHoy
	FROM tbl_DetalleBajaProductosPendiente cb inner join tbl_producto p on p.Id_Producto = cb.id_Producto
	inner join tbl_bajaproductospendiente b on b.id_bajaproductospendiente=cb.id_bajaproductospendiente
	inner join tbl_saldotarjetas s on s.id_ruta=b.id_ruta and s.id_producto=p.id_producto
	and cb.Id_BajaProductosPendiente = @codigo
	where cb.e_eliminado=0 
	group by cb.Id_DetalleBajaProductosPendiente,cb.Id_BajaProductosPendiente,p.Id_Producto,p.Nombre
	order by p.nombre
	
	SELECT cb.Id_CodigoBajaProductosPendiente,cb.Id_BajaProductosPendiente,p.Id_Producto,p.Nombre Producto,cb.Serial,
	cb.ChipID,cb.cantidad,cb.Id_Ruta, cb.PrecioU, cb.TotalP, estado, cb.Id_TipoMaterial
	FROM tbl_CodigoBajaProductosPendiente cb inner join tbl_producto p on p.Id_Producto = cb.id_Producto
	and cb.Id_BajaProductosPendiente = @codigo
	where cb.e_eliminado=0 
	order by p.nombre
END


GO

/* [dbo].[spx_obtenercaberaventapararegistrootwb] */
CREATE proc [dbo].[spx_ObtenerCaberaVentaParaRegistroOTwb](@cliente_nro int, @ot int,@tor nvarchar(15),@grupo nvarchar(250),@tecniconombre nvarchar(250))
as

select v.id_vendedor, v.nombre,r.id_ruta IdGrupo,r.nombre NombreGrupo,
(select id_tiposervicio from tbl_tiposervicio where prefijo =@tor) Id_TipoServicio,
@tor TOR,@ot OT,(select id_sucursal from tbl_version)Id_Sucursal,(select sucursal from tbl_version)Sucursal,@cliente_nro Cliente_Nro
from tbl_vendedor v inner join tbl_ruta r on r.id_vendedor=v.id_vendedor
where v.nombre=@tecniconombre and r.nombre=@grupo


GO

/* [dbo].[spx_ObtenerCantidadIngresos] */
create proc [dbo].[spx_ObtenerCantidadIngresos](@Fecha datetime)
as
declare @FechaInicio datetime
declare @FechaFin datetime
set @FechaInicio = (SELECT CONVERT(VARCHAR(25),DATEADD(dd,-(DAY(@Fecha)-1),@Fecha),103))
set @FechaFin = (SELECT CONVERT(VARCHAR(25),DATEADD(dd,-(DAY(DATEADD(mm,1,@Fecha))),DATEADD(mm,1,@Fecha)),103))
select COUNT(Id_IngresoAlmacen) from tbl_IngresoAlmacen  where E_Eliminado=0 
and dbo.DateOnly(Fecha) between dbo.DateOnly(@FechaInicio) and dbo.DateOnly(@FechaFin)
GO

/* [dbo].[spx_ObtenerCargoUsuario_XIDVenta] */
	
create proc [dbo].[spx_ObtenerCargoUsuario_XIDVenta](@Id_Venta int)
as
select * from tbl_venta where id_venta=@Id_Venta and e_eliminado=0
SELECT 
    c.*,
    pr.Nombre AS NombreProducto,
    pr.PrecioUsado AS Precio,(pr.PrecioUsado*c.cantidad )Total,
    CASE 
        WHEN pr.digitosimei > 0 THEN 
            CASE 
                WHEN EXISTS (
                    SELECT 1 
                    FROM tbl_productos ps 
                    WHERE ps.serial = c.serial 
                      AND ps.chipid = c.chipid 
                      AND ps.id_estadoproducto = 17
                )
                THEN 'Si'
                ELSE 'No'
            END
        ELSE 'Si'
    END AS SePuedeEditar
FROM tbl_codigoventacargousuario c
INNER JOIN tbl_producto pr 
    ON pr.id_producto = c.id_producto
WHERE c.id_venta = @Id_Venta 
  AND c.e_eliminado = 0;
  
  
  

GO

/* [dbo].[spx_ObtenerCargoUsuarioNoRealizado_ID] */
CREATE PROCEDURE [dbo].[spx_ObtenerCargoUsuarioNoRealizado_ID](@Codigo int)
as
select Id	,Id_Usuario	,Id_VendedorDigitador	,Nombre	,
case when EsVendedorUsuario=1 then 'Tecnico' else 'Digitador' end EsVendedorUsuario
,CodigoEmpleado	,Id_TipoServicio	,NombreTipoServicio	,Fecha_Ejecucion	,
Fecha_Registro	,OrdenTrabajo	,CodigoCliente	,Observacion	,Total	,Id_UsuarioE	,NombreE	,	
 case when Cobrado = 0 then 'No' else 'Si' end Cobrado
from tbl_CargoUsuarioNoRealizado where id=@Codigo 
select * from tbl_codigoCargoUsuarioNoRealizado where id_cargousuarionorealizado=@Codigo and e_eliminado=0

GO

/* [dbo].[spx_ObtenerCargoUsuarioNoRealizado_Listado] */
CREATE proc [dbo].[spx_ObtenerCargoUsuarioNoRealizado_Listado]
as
select * from (
	select Id,Id CodigoCUNR,Id_VendedorDigitador,Nombre,
	case when EsVendedorUsuario = 1 then 'Tecnico' else 'Digitador' end EsVendedorUsuario,
	Id_TipoServicio,NombreTipoServicio,
	Fecha_Ejecucion,Fecha_Registro,OrdenTrabajo,
	CodigoCliente,Observacion,
	Total TotalDeuda,0 TotalPagado,0 SaldoDeudor,	
	case when Cobrado = 0 then 'No' else 'Si' end Estado , CodigoEmpleado, 'Registro' Tipo
	from tbl_CargoUsuarioNoRealizado where e_eliminado=0
	
	union all 
	select id, CodigoCUNR,Id_Vendedor Id_VendedorDigitador,NombreDeudor Id_VendedorDigitador,
	TipoSolicitante EsVendedorUsuario,
	0 Id_TipoServicio,''NombreTipoServicio,
	fecha Fecha_Ejecucion,fechaRegistro,0 OrdenTrabajo,
	0 CodigoCliente,''Observacion,
	TotalDeuda,TotalPagado,MontoDeuda SaldoDeudor,Estado,CodigoEmpleado,'Deuda' Tipo
	from tbl_deudaCUNR where e_eliminado=0
) a
GO

/* [dbo].[spx_ObtenerCierreAlmacen_PSaldos] */
create proc [dbo].[spx_ObtenerCierreAlmacen_PSaldos](@Fecha datetime)
as 
Begin	
	declare @tbl_Cierre 
	table (
	Id_Producto int, 
	Nombre nvarchar(max),
	SaldoDiaAnterior decimal(18,2),
	SaldoDiaAnteriorDevolucion decimal(18,2),
	IngresoDia decimal(18,2),
	DevolucionIngreso decimal(18,2),
	DevolucionSalida decimal(18,2),
	SalidaDia decimal(18,2),
	SalidaBaja decimal(18,2),
	SaldoDiaHoy decimal(18,2),
	SaldoDiaHoyDevolucion decimal(18,2))
	------------------------saldo dia anterior
	------------------------saldo dia anterior
	--es el saldo del dia anterior
	insert into @tbl_Cierre
			SELECT 
			p.Id_Producto, p.Nombre,isnull(cc.SaldoDiaHoy,0) SaldoDiaHoy, isnull(cc.SaldoDiaHoyDevolucion,0) SaldoDiaHoyDevolucion,
			0,0,0,0,0,isnull(cc.SaldoDiaHoy,0) SaldoDiaHoy, isnull(cc.SaldoDiaHoyDevolucion,0) SaldoDiaHoyDevolucion
			FROM   dbo.tbl_Producto p left JOIN dbo.tbl_CodigoCierreAlmacen cc ON p.Id_Producto = cc.Id_Producto 
			AND cc.Id_CierreAlmacen = (select MAX(Id_CierreAlmacen)  from dbo.tbl_CierreAlmacen cd
			WHERE DBO.DATEONLY(cd.Fecha)<DBO.DATEONLY(@Fecha) and cd.E_Eliminado=0 )
			WHERE  p.e_eliminado=0 
			ORDER BY p.Observacion 
	declare @auxiliar table (Id_Producto int, Cantidad int)

	------------------------Ingreso Dia
	------------------------Ingreso Dia
	--compra del dia
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto,sum(cia.Cantidad)AS CompraDia
			FROM    dbo.tbl_IngresoAlmacen ia INNER JOIN 
			dbo.tbl_CodigoIngresoAlmacen cia ON ia.Id_IngresoAlmacen = cia.Id_IngresoAlmacen INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
			group by dbo.tbl_Producto.Id_Producto,dbo.tbl_Producto.Nombre,dbo.tbl_Producto.PrecioVenta

	update @tbl_Cierre
	set IngresoDia = a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto
		
		
	delete from @auxiliar 
	
	------------------------Salida Dia
	------------------------Salida Dia
	--salida dia = salida vendedor + entregado a tigo de almacen (excedente de almacen) + traspaso 
	--salida vendedor	
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, isnull(SUM(dbo.tbl_CodigoAlmacenVendedor.Cantidad),0)Cantidad				
			FROM    dbo.tbl_CodigoAlmacenVendedor INNER JOIN  
			dbo.tbl_AlmacenVendedor ON dbo.tbl_CodigoAlmacenVendedor.Id_AlmacenVendedor = dbo.tbl_AlmacenVendedor.Id_AlmacenVendedor INNER JOIN  
			dbo.tbl_Producto ON dbo.tbl_CodigoAlmacenVendedor.Id_Producto = dbo.tbl_Producto.Id_Producto  
			WHERE   (dbo.tbl_CodigoAlmacenVendedor.E_Eliminado = 0) AND (dbo.tbl_AlmacenVendedor.E_Eliminado = 0) AND   
			dbo.DateOnly(dbo.tbl_AlmacenVendedor.Fecha) >= dbo.DateOnly(@Fecha)
			and tbl_Producto.e_eliminado=0
			GROUP BY dbo.tbl_Producto.Id_Producto 
			order by dbo.tbl_Producto.Id_Producto

			
	update @tbl_Cierre
	set SalidaDia = a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto
	delete from @auxiliar 
	
	--entrega Tigo Devolucion excedente almacen
		insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and Id_TipoDevolucion=5
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set SalidaDia = SalidaDia + a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	--salida traspaso 
		insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_SalidaTraspaso ia INNER JOIN 
			dbo.tbl_CodigoSalidaTraspaso cia ON ia.Id_SalidaTraspaso = cia.Id_SalidaTraspaso  INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)>=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set SalidaDia = SalidaDia + a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	
	------------------------Salida Baja
	------------------------Salida Baja
	--Baja Productos 
	insert into @auxiliar 
			SELECT  p.Id_Producto, isnull(SUM(cbp.Cantidad),0)Cantidad				
			FROM    dbo.tbl_CodigoBajaProductos cbp INNER JOIN  
			dbo.tbl_BajaProductos bp ON cbp.id_BajaProductos= bp.id_BajaProductos INNER JOIN  
			dbo.tbl_Producto p ON cbp.Id_Producto = p.Id_Producto  
			WHERE   cbp.E_Eliminado = 0 AND bp.E_Eliminado = 0 AND   
			dbo.DateOnly(bp.Fecha) = dbo.DateOnly(@Fecha)
			and p.e_eliminado=0 and bp.id_estadoproductos in(1) and id_ruta in (0)
			GROUP BY p.Id_Producto 
			order by p.Id_Producto

			
	update @tbl_Cierre
	set SalidaBaja = a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto
	delete from @auxiliar 	
	------------------------Devolucion Ingreso
	------------------------Devolucion Ingreso
	--select * from tbl_TipoDevolucion--1 material dañado  2 material retirado   3 excedente ruta  4 devuelto tigo
	--1 material dañado no importa si esta entregado
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion in (1)
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set DevolucionIngreso = a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	
	--------
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion in (2)
			and cia.Entregado=1
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set DevolucionIngreso =DevolucionIngreso+ a.Cantidad
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 


	------------------------Devolucion Salida
	------------------------Devolucion Salida
    --si es excedente tiene que sacar de almacen
	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 and Id_TipoDevolucion=4
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set DevolucionSalida = a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
--	select * from tbl_BajaProductos
	--baja de material retirado
		insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_BajaProductos ia INNER JOIN 
			dbo.tbl_Codigobajaproductos cia ON ia.Id_bajaproductos= cia.Id_bajaproductos INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0
			and ia.Id_EstadoProductos in (4,5)
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set DevolucionSalida = DevolucionSalida+a.Cantidad	
	from @tbl_Cierre c inner join @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar 
	--[sp_ObtenerCierreAlmacen] '19/12/2017'
	
	------------------------SaldoDiaHoyDevolucion
	------------------------SaldoDiaHoyDevolucion
	update @tbl_Cierre
	set SaldoDiaHoyDevolucion = SaldoDiaAnteriorDevolucion+DevolucionIngreso-DevolucionSalida 
	from @tbl_Cierre c

	------------------------Ingreso x Excende Ruta

	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 0 and ia.id_TipoDevolucion = 3
			group by dbo.tbl_Producto.Id_Producto
			
	update @tbl_Cierre
	set IngresoDia = IngresoDia + a.Cantidad
	from @tbl_Cierre c INNER JOIN @auxiliar a on c.id_Producto = a.id_producto


	delete from @auxiliar 

	------------------------Devolucion Salida
	------------------------Devolucion Salida

	insert into @auxiliar 
			SELECT  dbo.tbl_Producto.Id_Producto, sum(cia.Cantidad)AS Cantidad
			FROM    dbo.tbl_Devolucion ia INNER JOIN 
			dbo.tbl_detalleDevolucion cia ON ia.Id_Devolucion = cia.Id_Devolucion INNER JOIN 
			dbo.tbl_Producto ON cia.Id_Producto = dbo.tbl_Producto.Id_Producto 
			where dbo.dateonly(ia.Fecha)=dbo.dateonly(@Fecha) and ia.E_Eliminado = 0 and cia.E_Eliminado=0 and Estado = 1 
			group by dbo.tbl_Producto.Id_Producto

	update @tbl_Cierre
	set SaldoDiaHoy = SaldoDiaHoy - a.Cantidad	
	from @tbl_Cierre c INNER JOIN @auxiliar a on a.id_Producto = c.id_producto

	delete from @auxiliar
	------------------------Saldo Dia
	------------------------Saldo Dia

	update @tbl_Cierre
	set SaldoDiaHoy = SaldoDiaAnterior + IngresoDia- SalidaDia - SalidaBaja 

	update @tbl_Cierre
	set SaldoDiaHoyDevolucion = SaldoDiaAnteriorDevolucion+DevolucionIngreso-DevolucionSalida 

	select * from @tbl_Cierre
end
GO

/* [dbo].[spx_obtenerconformacioncuadrillabackoffice] */
CREATE PROC [dbo].[spx_ObtenerConformacionCuadrillaBackOffice]
    @Fecha DATE = NULL,
    @Sucursal NVARCHAR(100) = NULL,
    @Limite INT = NULL
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP (CASE WHEN @Limite IS NULL OR @Limite <= 0 THEN 2147483647 ELSE @Limite END)
        *
    FROM dbo.tbl_ConformacionCuadrillaBackOffice
    WHERE e_eliminado = 0
     -- AND //(@Fecha IS NULL OR fecha = @Fecha)
      AND (
            @Sucursal IS NULL
            OR LTRIM(RTRIM(@Sucursal)) = ''
            OR sucursal = LTRIM(RTRIM(@Sucursal))
          )
    ORDER BY fechaRegistro DESC, id DESC;
END

GO

/* [dbo].[spx_obtenerconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ObtenerConformacionCuadrillaWeb]
    @Fecha DATE = NULL,
    @Sucursal NVARCHAR(100) = NULL,
    @Limite INT = NULL
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @FechaConsulta DATE = ISNULL(@Fecha, CAST(GETDATE() AS DATE));
    DECLARE @SucursalNormalizada NVARCHAR(100) = NULLIF(LTRIM(RTRIM(@Sucursal)), '');

    ;WITH VersionActual AS (
        SELECT TOP 1 LTRIM(RTRIM(v.sucursal)) AS sucursal
        FROM dbo.tbl_version v
        WHERE v.sucursal IS NOT NULL
          AND LTRIM(RTRIM(v.sucursal)) <> ''
    ),
    BaseCuadrillas AS (
        SELECT
            CAST(r.Id_Ruta AS BIGINT) AS id_ruta,
            @FechaConsulta AS fecha,
            CAST('PENDIENTE' AS NVARCHAR(20)) AS estado,
            CAST(
                CASE
                    WHEN UPPER(LTRIM(RTRIM(ISNULL(r.Tipo, '')))) IN ('TITULAR', 'BACKUP')
                        THEN UPPER(LTRIM(RTRIM(r.Tipo)))
                    ELSE 'TITULAR'
                END
                AS NVARCHAR(20)
            ) AS actividad,
            v.Id_Vendedor AS id_tecnico,
            v.CuentaSF AS cuenta_sf,
            v.SalesForce AS salesforce,
            v.Habilidad AS habilidad,
            v.Vehiculo AS vehiculo,
            r.Nombre AS grupo,
            COALESCE(NULLIF(LTRIM(RTRIM(r.BodegaTigo)), ''), NULLIF(LTRIM(RTRIM(r.almacenTigo)), '')) AS almacen,
            COALESCE(NULLIF(LTRIM(RTRIM(r.almacenTigo)), ''), NULLIF(LTRIM(RTRIM(r.BodegaTigo)), '')) AS grupoDigitacion,
            v.Nombre AS tecnico,
            va.sucursal AS sucursal,
            GETDATE() AS fechaRegistro,
            CONVERT(BIT, ISNULL(r.E_Eliminado, 0)) AS e_eliminado
        FROM dbo.tbl_Ruta r
        INNER JOIN dbo.tbl_Vendedor v
            ON v.Id_Vendedor = r.Id_Vendedor
        CROSS JOIN VersionActual va
        WHERE v.E_Eliminado = 0
    ),
    GuardadasGrupo AS (
        SELECT
            g.*,
            ROW_NUMBER() OVER (
                PARTITION BY
                    g.id_tecnico,
                    UPPER(LTRIM(RTRIM(ISNULL(g.grupo, ''))))
                ORDER BY ISNULL(g.fechaRegistro, '19000101') DESC, g.id DESC
            ) AS rn
        FROM dbo.tbl_ConformacionCuadrillaDiarioWeb g
        WHERE ISNULL(g.e_eliminado, 0) = 0
          AND g.fecha = @FechaConsulta
          AND (
                @SucursalNormalizada IS NULL
                OR UPPER(LTRIM(RTRIM(ISNULL(g.sucursal, '')))) = UPPER(@SucursalNormalizada)
              )
    ),
    GuardadasTecnico AS (
        SELECT
            g.*,
            ROW_NUMBER() OVER (
                PARTITION BY g.id_tecnico
                ORDER BY ISNULL(g.fechaRegistro, '19000101') DESC, g.id DESC
            ) AS rn
        FROM dbo.tbl_ConformacionCuadrillaDiarioWeb g
        WHERE ISNULL(g.e_eliminado, 0) = 0
          AND g.fecha = @FechaConsulta
          AND (
                @SucursalNormalizada IS NULL
                OR UPPER(LTRIM(RTRIM(ISNULL(g.sucursal, '')))) = UPPER(@SucursalNormalizada)
              )
    )
    SELECT TOP (CASE WHEN @Limite IS NULL OR @Limite <= 0 THEN 2147483647 ELSE @Limite END)
        b.id_ruta AS id,
        b.id_ruta AS id_ruta,
        COALESCE(ge.fecha, gt.fecha, b.fecha) AS fecha,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.estado)), ''), NULLIF(LTRIM(RTRIM(gt.estado)), ''), b.estado) AS estado,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.actividad)), ''), NULLIF(LTRIM(RTRIM(gt.actividad)), ''), b.actividad) AS actividad,
        b.id_tecnico,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.cuenta_sf)), ''), NULLIF(LTRIM(RTRIM(gt.cuenta_sf)), ''), b.cuenta_sf) AS cuenta_sf,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.salesforce)), ''), NULLIF(LTRIM(RTRIM(gt.salesforce)), ''), b.salesforce) AS salesforce,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.habilidad)), ''), NULLIF(LTRIM(RTRIM(gt.habilidad)), ''), b.habilidad) AS habilidad,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.vehiculo)), ''), NULLIF(LTRIM(RTRIM(gt.vehiculo)), ''), b.vehiculo) AS vehiculo,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.grupo)), ''), NULLIF(LTRIM(RTRIM(gt.grupo)), ''), b.grupo) AS grupo,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.almacen)), ''), NULLIF(LTRIM(RTRIM(gt.almacen)), ''), b.almacen) AS almacen,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.grupoDigitacion)), ''), NULLIF(LTRIM(RTRIM(gt.grupoDigitacion)), ''), b.grupoDigitacion) AS grupoDigitacion,
        COALESCE(ge.idUsuarioDigitador, gt.idUsuarioDigitador) AS idUsuarioDigitador,
        COALESCE(ge.digitador, gt.digitador) AS digitador,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.tecnico)), ''), NULLIF(LTRIM(RTRIM(gt.tecnico)), ''), b.tecnico) AS tecnico,
        COALESCE(ge.id_tecnicoAuxiliar, gt.id_tecnicoAuxiliar) AS id_tecnicoAuxiliar,
        COALESCE(ge.auxiliar, gt.auxiliar) AS auxiliar,
        COALESCE(ge.idUsuarioSupervisor, gt.idUsuarioSupervisor) AS idUsuarioSupervisor,
        COALESCE(ge.supervisorACargo, gt.supervisorACargo) AS supervisorACargo,
        COALESCE(NULLIF(LTRIM(RTRIM(ge.sucursal)), ''), NULLIF(LTRIM(RTRIM(gt.sucursal)), ''), b.sucursal) AS sucursal,
        COALESCE(ge.observacion, gt.observacion) AS observacion,
        COALESCE(ge.idUsuarioRegistra, gt.idUsuarioRegistra) AS idUsuarioRegistra,
        COALESCE(ge.fechaRegistro, gt.fechaRegistro, b.fechaRegistro) AS fechaRegistro,
        COALESCE(ge.e_eliminado, gt.e_eliminado, b.e_eliminado) AS e_eliminado
    FROM BaseCuadrillas b
    LEFT JOIN GuardadasGrupo ge
        ON ge.id_tecnico = b.id_tecnico
       AND UPPER(LTRIM(RTRIM(ISNULL(ge.grupo, '')))) = UPPER(LTRIM(RTRIM(ISNULL(b.grupo, ''))))
       AND ge.rn = 1
    LEFT JOIN GuardadasTecnico gt
        ON gt.id_tecnico = b.id_tecnico
       AND gt.rn = 1
    WHERE @SucursalNormalizada IS NULL
       OR UPPER(LTRIM(RTRIM(ISNULL(COALESCE(ge.sucursal, gt.sucursal, b.sucursal), '')))) = UPPER(@SucursalNormalizada)
    ORDER BY
        COALESCE(ge.e_eliminado, gt.e_eliminado, b.e_eliminado),
        COALESCE(ge.grupo, gt.grupo, b.grupo),
        COALESCE(ge.tecnico, gt.tecnico, b.tecnico),
        b.id_ruta;
END



GO

/* [dbo].[spx_obtenerconformacioncuadrillawebporid] */
CREATE PROC [dbo].[spx_ObtenerConformacionCuadrillaWebPorId]
    @Id BIGINT
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH VersionActual AS (
        SELECT TOP 1 LTRIM(RTRIM(v.sucursal)) AS sucursal
        FROM dbo.tbl_version v
        WHERE v.sucursal IS NOT NULL
          AND LTRIM(RTRIM(v.sucursal)) <> ''
    ),
    BaseRow AS (
        SELECT TOP 1
            CAST(r.Id_Ruta AS BIGINT) AS id_ruta,
            CAST(GETDATE() AS DATE) AS fecha,
            CAST('PENDIENTE' AS NVARCHAR(20)) AS estado,
            CAST(
                CASE
                    WHEN UPPER(LTRIM(RTRIM(ISNULL(r.Tipo, '')))) IN ('TITULAR', 'BACKUP')
                        THEN UPPER(LTRIM(RTRIM(r.Tipo)))
                    ELSE 'TITULAR'
                END
                AS NVARCHAR(20)
            ) AS actividad,
            v.Id_Vendedor AS id_tecnico,
            v.CuentaSF AS cuenta_sf,
            v.SalesForce AS salesforce,
            v.Habilidad AS habilidad,
            v.Vehiculo AS vehiculo,
            r.Nombre AS grupo,
            COALESCE(NULLIF(LTRIM(RTRIM(r.BodegaTigo)), ''), NULLIF(LTRIM(RTRIM(r.almacenTigo)), '')) AS almacen,
            COALESCE(NULLIF(LTRIM(RTRIM(r.almacenTigo)), ''), NULLIF(LTRIM(RTRIM(r.BodegaTigo)), '')) AS grupoDigitacion,
            v.Nombre AS tecnico,
            va.sucursal AS sucursal,
            GETDATE() AS fechaRegistro,
            CONVERT(BIT, ISNULL(r.E_Eliminado, 0)) AS e_eliminado
        FROM dbo.tbl_Ruta r
        INNER JOIN dbo.tbl_Vendedor v
            ON v.Id_Vendedor = r.Id_Vendedor
        CROSS JOIN VersionActual va
        WHERE r.Id_Ruta = @Id
          AND v.E_Eliminado = 0
    ),
    GuardadaGrupo AS (
        SELECT TOP 1 g.*
        FROM dbo.tbl_ConformacionCuadrillaDiarioWeb g
        INNER JOIN BaseRow b
            ON b.id_tecnico = g.id_tecnico
           AND UPPER(LTRIM(RTRIM(ISNULL(g.grupo, '')))) = UPPER(LTRIM(RTRIM(ISNULL(b.grupo, ''))))
        WHERE ISNULL(g.e_eliminado, 0) = 0
        ORDER BY ISNULL(g.fechaRegistro, '19000101') DESC, g.id DESC
    ),
    GuardadaTecnico AS (
        SELECT TOP 1 g.*
        FROM dbo.tbl_ConformacionCuadrillaDiarioWeb g
        INNER JOIN BaseRow b
            ON b.id_tecnico = g.id_tecnico
        WHERE ISNULL(g.e_eliminado, 0) = 0
        ORDER BY ISNULL(g.fechaRegistro, '19000101') DESC, g.id DESC
    )
    SELECT TOP 1
        b.id_ruta AS id,
        b.id_ruta,
        COALESCE(gg.fecha, gt.fecha, b.fecha) AS fecha,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.estado)), ''), NULLIF(LTRIM(RTRIM(gt.estado)), ''), b.estado) AS estado,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.actividad)), ''), NULLIF(LTRIM(RTRIM(gt.actividad)), ''), b.actividad) AS actividad,
        b.id_tecnico,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.cuenta_sf)), ''), NULLIF(LTRIM(RTRIM(gt.cuenta_sf)), ''), b.cuenta_sf) AS cuenta_sf,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.salesforce)), ''), NULLIF(LTRIM(RTRIM(gt.salesforce)), ''), b.salesforce) AS salesforce,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.habilidad)), ''), NULLIF(LTRIM(RTRIM(gt.habilidad)), ''), b.habilidad) AS habilidad,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.vehiculo)), ''), NULLIF(LTRIM(RTRIM(gt.vehiculo)), ''), b.vehiculo) AS vehiculo,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.grupo)), ''), NULLIF(LTRIM(RTRIM(gt.grupo)), ''), b.grupo) AS grupo,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.almacen)), ''), NULLIF(LTRIM(RTRIM(gt.almacen)), ''), b.almacen) AS almacen,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.grupoDigitacion)), ''), NULLIF(LTRIM(RTRIM(gt.grupoDigitacion)), ''), b.grupoDigitacion) AS grupoDigitacion,
        COALESCE(gg.idUsuarioDigitador, gt.idUsuarioDigitador) AS idUsuarioDigitador,
        COALESCE(gg.digitador, gt.digitador) AS digitador,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.tecnico)), ''), NULLIF(LTRIM(RTRIM(gt.tecnico)), ''), b.tecnico) AS tecnico,
        COALESCE(gg.id_tecnicoAuxiliar, gt.id_tecnicoAuxiliar) AS id_tecnicoAuxiliar,
        COALESCE(gg.auxiliar, gt.auxiliar) AS auxiliar,
        COALESCE(gg.idUsuarioSupervisor, gt.idUsuarioSupervisor) AS idUsuarioSupervisor,
        COALESCE(gg.supervisorACargo, gt.supervisorACargo) AS supervisorACargo,
        COALESCE(NULLIF(LTRIM(RTRIM(gg.sucursal)), ''), NULLIF(LTRIM(RTRIM(gt.sucursal)), ''), b.sucursal) AS sucursal,
        COALESCE(gg.observacion, gt.observacion) AS observacion,
        COALESCE(gg.idUsuarioRegistra, gt.idUsuarioRegistra) AS idUsuarioRegistra,
        COALESCE(gg.fechaRegistro, gt.fechaRegistro, b.fechaRegistro) AS fechaRegistro,
        COALESCE(gg.e_eliminado, gt.e_eliminado, b.e_eliminado) AS e_eliminado
    FROM BaseRow b
    LEFT JOIN GuardadaGrupo gg
        ON gg.id_tecnico = b.id_tecnico
       AND UPPER(LTRIM(RTRIM(ISNULL(gg.grupo, '')))) = UPPER(LTRIM(RTRIM(ISNULL(b.grupo, ''))))
    LEFT JOIN GuardadaTecnico gt
        ON gt.id_tecnico = b.id_tecnico;
END

GO

/* [dbo].[spx_ObtenerContraseñasAntiguas] */
CREATE proc spx_ObtenerContraseñasAntiguas(@loggin nvarchar(100))
as
select top 15 * from tbl_UsuarioContraseñas
where Loggin =@loggin
order by Id_UsuarioContraseñas desc

GO

/* [dbo].[spx_ObtenerContraseñaUsuario] */
create proc spx_ObtenerContraseñaUsuario(@Login nvarchar(25))
as
	select Password from tbl_usuario where Loggin=@Login and E_Eliminado=0
GO

/* [dbo].[spx_ObtenerCorrecionErrores_XID] */
CREATE proc [dbo].[spx_ObtenerCorrecionErrores_XID](@codigo int)
as
select  * from tbl_CorreccionErrores where e_eliminado=0 and id=@codigo


GO

/* [dbo].[spx_ObtenerCorreosBackOffice] */
create proc [dbo].[spx_ObtenerCorreosBackOffice]
as 
select * from tbl_correoBackOffice where e_eliminado=0

GO

/* [dbo].[spx_ObtenerDatoEmbajadoresBDComision] */
CREATE proc [dbo].[spx_ObtenerDatoEmbajadoresBDComision]
as 
SELECT cast(Eh as nvarchar(15))EH,Nombre,Sucursal FROM [tigo.makiro.com.bo].bdCOMISION.DBO.pdaricardo
union all
SELECT cast(Eh as nvarchar(15))Eh,Nombre,Sucursal FROM [tigo.makiro.com.bo].bdCOMISION.DBO.pdamakiro
union all
SELECT Eh,Nombre,Sucursal FROM [tigo.makiro.com.bo].bdCOMISION.DBO.pdaInstaladoresMakiro

GO

/* [dbo].[spx_ObtenerDatoEmpleadoBDBoletasMakiro] */
CREATE proc [dbo].[spx_ObtenerDatoEmpleadoBDBoletasMakiro]
as
select CodEmpleado,(e.Nombre + ' '+Apellido+' ' + ApellidoMaterno)Nombre , c.Cargo, suc.nombre Sucursal,
e.Id_Empleado
from  [tigo.makiro.com.bo].bdsistemaboletasmakiro.dbo.tbl_empleado e inner join [tigo.makiro.com.bo].bdsistemaboletasmakiro.dbo.tbl_cargo c on c.id_cargo = e.id_cargo
inner join [tigo.makiro.com.bo].bdsistemaboletasmakiro.dbo.tbl_empleadosucursal es on es.id_empleado=e.id_empleado 
inner join [tigo.makiro.com.bo].bdsistemaboletasmakiro.dbo.tbl_sucursal suc on suc.id_sucursal = es.id_sucursal
where e.id_cargo in (23,33,37,44,46,47,49,31,41,4,9,36,31) and e.e_eliminado=0
order by e.Nombre ,Apellido


GO

/* [dbo].[spx_ObtenerDatoEmpleadoBDBoletasMakiroXSucursal] */
create proc [dbo].[spx_ObtenerDatoEmpleadoBDBoletasMakiroXSucursal](@sucursal nvarchar(250))
as
select CodEmpleado,(e.Nombre + ' '+Apellido+' ' + ApellidoMaterno)Nombre , c.Cargo, suc.nombre Sucursal,
e.Id_Empleado
from  bdsistemaboletasmakiro..tbl_empleado e inner join bdsistemaboletasmakiro..tbl_cargo c on c.id_cargo = e.id_cargo
inner join bdsistemaboletasmakiro..tbl_empleadosucursal es on es.id_empleado=e.id_empleado 
inner join bdsistemaboletasmakiro..tbl_sucursal suc on suc.id_sucursal = es.id_sucursal and suc.nombre=@sucursal
where e.id_cargo in (23,33,37,44,46,47,49,31,41,4) and e.e_eliminado=0
order by e.Nombre ,Apellido
GO

/* [dbo].[spx_ObtenerDatosParaEjecucion] */
--select * from tbl_DatosDTHPrepago
--[spx_ObtenerDatosParaEjecucion] 2
CREATE proc [dbo].[spx_ObtenerDatosParaEjecucion](@Id_Sucursal int)
as
	
		select ID,TipoServicioTS,ORDENNRO OT,CLIENTENRO CODIGO,TOR,
		IdSucursal,Sucursal,Poblacion_ Poblacion,FechaCierre,Georeferencia,Altura,Cierre,ObservacionCierre,IdGrupo,Grupo,Cuadrilla
		from tbl_DatosDTHPrepago where IdSucursal= @Id_Sucursal
		AND (EstadoGestionDealer LIKE 'CONFIRMADA' OR EstadoGestionDealer LIKE 'REPROGRAMADA')
		and Cierre like ''
	UNION ALL
		select ID,TipoServicioTS,OT OT,CODIGO CODIGO,TIPO TOR,
		IdSucursal,Sucursal,Poblacion_ Poblacion,FechaCierre,Georeferencia,Altura,Cierre,ObservacionCierre,IdGrupo,Grupo,Cuadrilla
		from tbl_DatosHFC where IdSucursal= @Id_Sucursal
		AND (EstadoGestionDealer LIKE 'CONFIRMADA' OR EstadoGestionDealer LIKE 'REPROGRAMADA')
		and Cierre like ''
	UNION ALL	
		select ID,TipoServicioTS,OT OT,CODIGO CODIGO,TIPO TOR,
		IdSucursal,Sucursal,Poblacion_ Poblacion,FechaCierre,Georeferencia,Altura,Cierre,ObservacionCierre,IdGrupo,Grupo,Cuadrilla
		from tbl_DatosPostPago where IdSucursal= @Id_Sucursal
		AND (EstadoGestionDealer LIKE 'CONFIRMADA' OR EstadoGestionDealer LIKE 'REPROGRAMADA')
		and Cierre like ''

GO

/* [dbo].[spx_ObtenerDatosParaProgramacion] */
CREATE proc [dbo].[spx_ObtenerDatosParaProgramacion](@Id_Sucursal int)
as
		select ID,CLIENTENRO NumeroCliente,ORDENNRO OrdenTrabajo, TipoServicioTS TipoSolicitud,FechaGestion,EstadoGestionDealer,FechaProgramacion ,Observacion 
		from tbl_DatosDTHPrepago where IdSucursal= @Id_Sucursal AND 
		(EstadoGestionDealer LIKE 'ASIGNADA' OR EstadoGestionDealer LIKE 'PENDIENTE')
		UNION ALL
		select ID,[NUMERO CLIENTE] NumeroCliente, OT OrdenTrabajo, TipoServicioTS TipoSolicitud,FechaGestion,EstadoGestionDealer,FechaProgramacion ,Observacion 
		from tbl_DatosHFC where IdSucursal= @Id_Sucursal AND 
		(EstadoGestionDealer LIKE 'ASIGNADA' OR EstadoGestionDealer LIKE 'PENDIENTE')
		UNION ALL
		select ID,[NUMERO CLIENTE] NumeroCliente,OT OrdenTrabajo, TipoServicioTS TipoSolicitud,FechaGestion,EstadoGestionDealer,FechaProgramacion ,Observacion 
		from tbl_DatosPostPago where IdSucursal= @Id_Sucursal AND 
		(EstadoGestionDealer LIKE 'ASIGNADA' OR EstadoGestionDealer LIKE 'PENDIENTE')
GO

/* [dbo].[spx_obtenerdatostecnicoconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ObtenerDatosTecnicoConformacionCuadrillaWeb]   --389
    @Id_Tecnico INT
AS
BEGIN
    SET NOCOUNT ON;

    ;WITH VersionActual AS (
        SELECT TOP 1 LTRIM(RTRIM(v.sucursal)) AS sucursal
        FROM dbo.tbl_version v
        WHERE v.sucursal IS NOT NULL
          AND LTRIM(RTRIM(v.sucursal)) <> ''
    )
    SELECT TOP 1
        v.Id_tecnico AS id_tecnico,
        v.tecnico AS tecnico,
        v.Cuenta_SF AS cuenta_sf,
        v.SalesForce AS salesforce,
        v.Habilidad AS habilidad,
        v.Vehiculo AS vehiculo,
        v.Idgrupo AS id,
        v.Grupo AS id_ruta,
        CAST(GETDATE() AS DATE) AS fecha,
        CAST('PENDIENTE' AS NVARCHAR(20)) AS estado,
        CAST(
            CASE
                WHEN UPPER(LTRIM(RTRIM(ISNULL(ruta.Tipo, '')))) IN ('TITULAR', 'BACKUP')
                    THEN UPPER(LTRIM(RTRIM(ruta.Tipo)))
                ELSE 'TITULAR'
            END
            AS NVARCHAR(20)
        ) AS actividad,
        ruta.Nombre AS grupo,
        COALESCE(NULLIF(LTRIM(RTRIM(ruta.BodegaTigo)), ''), NULLIF(LTRIM(RTRIM(ruta.almacenTigo)), '')) AS almacen,
        COALESCE(NULLIF(LTRIM(RTRIM(ruta.almacenTigo)), ''), NULLIF(LTRIM(RTRIM(ruta.BodegaTigo)), '')) AS grupoDigitacion,
        idUsuarioDigitador AS idUsuarioDigitador,
        digitador AS digitador,
        idtecnicoAuxiliar,
        tecnicoAuxiliar auxiliar,
        idUsuarioSupervisor,
        supervisorACargo,
        va.sucursal AS sucursal,
        CAST(NULL AS NVARCHAR(500)) AS observacion,
        CAST(NULL AS INT) AS idUsuarioRegistra,
        GETDATE() AS fechaRegistro,
        CONVERT(BIT, ISNULL(ruta.E_Eliminado, 0)) AS e_eliminado
    FROM dbo.tbl_conformacioncuadrillabackoffice v
    OUTER APPLY (
        SELECT TOP 1 r.*
        FROM dbo.tbl_Ruta r
        WHERE r.Id_Vendedor = v.Id_tecnico
          AND ISNULL(r.E_Eliminado, 0) = 0
        ORDER BY r.Id_Ruta DESC   -- última ruta
    ) ruta
    CROSS JOIN VersionActual va
    WHERE v.Id_tecnico = @Id_Tecnico
      AND v.E_Eliminado = 0
    ORDER BY v.fechaRegistro DESC; -- último registro del técnico
END

GO

/* [dbo].[spx_obtenerdatostecnicocuadrilla] */
CREATE PROC [dbo].[spx_ObtenerDatosTecnicoCuadrilla]
    @Id_Tecnico INT
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP 1
        v.*,
        r.*,
        (SELECT TOP 1 sucursal FROM dbo.tbl_version) AS sucursal
    FROM dbo.tbl_Vendedor v
    LEFT JOIN dbo.tbl_Ruta r ON r.Id_Vendedor = v.Id_Vendedor AND r.E_Eliminado = 0
    WHERE v.Id_Vendedor = @Id_Tecnico
      AND v.E_Eliminado = 0
    ORDER BY r.Id_Ruta;
END


GO

/* [dbo].[spx_ObtenerDatosTipoTrabajo] */

CREATE PROC spx_ObtenerDatosTipoTrabajo
as 
select * from tbl_TipoTrabajo where e_eliminado=0
GO

/* [dbo].[spx_ObtenerDatosUsuario] */
CREATE proc [dbo].[spx_ObtenerDatosUsuario](@Id_Usuario int)
as
select * from tbl_usuario where e_eliminado=0 and id_usuario=@Id_Usuario

GO

/* [dbo].[spx_ObtenerDeudaBajasReporte] */
create proc [dbo].[spx_ObtenerDeudaBajasReporte](@codigo int)
as
select * from tbl_DeudaBajas where id = @codigo and e_eliminado=0
select * from tbl_DeudaBajasDetallePago where iddeudaBajas=@codigo and e_eliminado=0


GO

/* [dbo].[spx_ObtenerDeudaCUNRReporte] */

create proc [dbo].[spx_ObtenerDeudaCUNRReporte](@codigo int)
as
select * from tbl_DeudaCUNR where id = @codigo and e_eliminado=0
select * from tbl_DeudaCUNRDetallePago where idCUNR=@codigo and e_eliminado=0


GO

/* [dbo].[spx_ObtenerDeudasPEvaluaciones] */
create proc spx_ObtenerDeudasPEvaluaciones(@Id_Empleado int, @FechaInicio datetime,@FechaFin datetime)
as 
select id_empleado, FechaDeuda,MontoDeuda
from BDAlmacenNacional.dbo.tbl_DeudasNacionalGestion 
where E_eliminado = 0 and Cobrar = 1 and id_empleado = @Id_Empleado
and dbo.DateOnly(FechaDeuda) between dbo.DateOnly(@FechaInicio) and dbo.DateOnly(@FechaFin)

GO

/* [dbo].[spx_ObtenerDevolucionTigoPendiente] */
CREATE proc [dbo].[spx_ObtenerDevolucionTigoPendiente](@Id_Devolucion int)
as
begin
	select d.Id_DevolucionTigoPendiente , (TP.Nombre +' - '+ d.Observacion) Observacion,Estado,
	d.FechaPendiente,d.FechaEntregado,d.FechaRegistrado ,d.Id_UsuarioPendiente Id_Usuario,u.Nombre UsuarioP,
	tp.Nombre TipoDevolucion,d.Id_Devolucion
	from tbl_DevolucionTigoPendiente  d inner join tbl_TipoDevolucion tp on tp.Id_TipoDevolucion=d.Id_TipoDevolucion
	inner join tbl_Usuario  u on u.Id_Usuario=d.Id_UsuarioPendiente
	where d.Id_DevolucionTigoPendiente =@Id_Devolucion

	select cd.Id_DevolucionTigoPendiente, Id_CodigoDevolucionTipoPendiente  Id_CodigoDevolucion,cd.Id_Producto,p.Nombre,cd.Serial Cod_Inicio,cd.ChipID,
	cd.Cantidad--, p.DigitosImei
	from  tbl_CodigoDevolucionTigoPendiente  cd, tbl_Producto p
	where cd.Id_Producto = p.Id_Producto
	and cd.E_Eliminado=0 and cd.Id_DevolucionTigoPendiente =@Id_Devolucion 
	order by p.Nombre,DigitosImei


	select Id_detalleDevolucionTigoPendiente,cd.Id_Producto,p.Nombre,cd.Cantidad 
	from  tbl_detalleDevolucionTigoPendiente  cd, tbl_Producto p
	where cd.Id_Producto = p.Id_Producto
	and cd.E_Eliminado=0 and cd.Id_DevolucionTigoPendiente =@Id_Devolucion
	order by p.Nombre,DigitosImei
end

GO

/* [dbo].[spx_obtenerdigitadores] */
CREATE PROCEDURE dbo.spx_ObtenerDigitadores
AS
BEGIN
    SET NOCOUNT ON;

    SELECT u.Id_Usuario,
           u.Nombre,
           u.Loggin,
           u.Id_Rol
    FROM dbo.tbl_Usuario u
    WHERE u.E_Eliminado = 0
      AND u.Id_Rol = 3
    ORDER BY u.Nombre;
END

GO

/* [dbo].[spx_obtenerdigitadoresconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ObtenerDigitadoresConformacionCuadrillaWeb]
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        u.Id_Usuario AS idUsuarioDigitador,
        u.Nombre AS digitador,
        r.Nombre AS rol,
        u.*
    FROM dbo.tbl_Usuario u
    LEFT JOIN dbo.tbl_Rol r
        ON r.Id_Rol = u.Id_Rol
    WHERE u.E_Eliminado = 0
      AND u.Id_Rol = 3
    ORDER BY u.Nombre;
END


GO

/* [dbo].[spx_ObtenerEntidadBancaria] */
create proc [dbo].[spx_ObtenerEntidadBancaria]
as
	select * from tbl_EntidadBancaria where e_eliminado=0
	

GO

/* [dbo].[spx_ObtenerEstadoProducto] */
create proc spx_ObtenerEstadoProducto
as 
select * from tbl_EstadoProducto
GO

/* [dbo].[spx_ObtenerEstadoProductoBP] */

CREATE proc [dbo].[spx_ObtenerEstadoProductoBP]
as 
--if(@Id_Ruta>0)--si es ruta cualquiera solo tiene q cargar en ruta y dañado---1,2,4,5
	select * from tbl_EstadoProducto where e_eliminado=0 and id_estadoproducto in (1,2,4,5)
--------------------------------------------------------

GO

/* [dbo].[spx_ObtenerEstadoProductoBP_RetNoEntregado] */

create proc [dbo].[spx_ObtenerEstadoProductoBP_RetNoEntregado]
as 
--if(@Id_Ruta>0)--si es ruta cualquiera solo tiene q cargar en ruta y dañado---1,2,4,5
	select * from tbl_EstadoProducto where e_eliminado=0 and id_estadoproducto in (11)
	
GO

/* [dbo].[spx_ObtenerEstadoProductoBPCE18] */
create proc [dbo].[spx_ObtenerEstadoProductoBPCE18]
as
	select * from tbl_EstadoProducto where e_eliminado=0 and id_estadoproducto in (1,2,4,5,23)
GO

/* [dbo].[spx_ObtenerEstadoProductoTA] */

CREATE proc [dbo].[spx_ObtenerEstadoProductoTA]
as 
	select * from tbl_EstadoProducto where e_eliminado=0 and id_estadoproducto in (1)
	
	
GO

/* [dbo].[spx_ObtenerEstadoProductoXSerial] */
create proc [dbo].[spx_ObtenerEstadoProductoXSerial](@serial nvarchar(150))
as
begin
	select * from tbl_productos where serial =@serial
end

GO

/* [dbo].[spx_ObtenerEstadoSiguiente] */
create proc [dbo].[spx_ObtenerEstadoSiguiente](@idestado int)
as
if (@idestado =17)
begin
select * from tbl_estadoproducto 
where id_EstadoProducto in (20,21)
end


GO

/* [dbo].[spx_ObtenerFechaActual] */

/********************************/

create proc spx_ObtenerFechaActual
as 
select getdate()Fecha



GO

/* [dbo].[spx_ObtenerFormaDePago] */
create proc [dbo].[spx_ObtenerFormaDePago]
as

		select * from tbl_FormaDePago where e_eliminado=0 and id in (3,4)

GO

/* [dbo].[spx_obtenerGrupoDigitacion] */
create proc spx_obtenerGrupoDigitacion
as
SELECT * FROM tbl_grupodigitacion where e_eliminado=0

GO

/* [dbo].[spx_ObtenerIngresoDetMaterialTigo_xId] */
CREATE proc dbo.spx_ObtenerIngresoDetMaterialTigo_xId( @idIngresoMaterialTigo int )
as
	select 	imt.id_IngresoMaterialTigo,
			imt.fechaEntregaTigo,
			imt.fechaIngreso,
			imt.proveedor,
			imt.nroComprobante,
			imt.observacion,
			imt.archivo,
			imt.nombrearchivo,
			case when imt.estadoIngresoCompleto = 0 
				then 'Falta Registrar Productos' 
				else 'Completo' 
				end estadoIngresoCompleto,
			u.Nombre Usuario 
	from tbl_IngresoMaterialTigo imt
		inner join tbl_Usuario u on 
			u.Id_Usuario = imt.id_Usuario and
			imt.e_eliminado = 0 and 
			imt.id_IngresoMaterialTigo = @idIngresoMaterialTigo
	
	select cimt.*, p.Nombre from tbl_CodigoIngresoMaterialTigo cimt 
		inner join tbl_producto p on 
			p.Id_Producto = cimt.id_producto and 
			cimt.e_eliminado = 0 and id_IngresoMaterialTigo = @idIngresoMaterialTigo

GO

/* [dbo].[spx_ObtenerIngresoMaterialTigo] */
  
create proc spx_ObtenerIngresoMaterialTigo(@Id_IngresoMaterialTigo int)
as
begin
	select id_IngresoMaterialTigo,fechaEntregaTigo,fechaIngreso,proveedor,nroComprobante,observacion,estadoIngresoCompleto
	from tbl_IngresoMaterialTigo where id_IngresoMaterialTigo = @Id_IngresoMaterialTigo
	select pr.Id_Producto,pr.Nombre,c.cantidad
	from tbl_CodigoIngresoMaterialTigo c inner join tbl_producto pr on pr.Id_Producto=c.id_producto
	where id_IngresoMaterialTigo = @Id_IngresoMaterialTigo
	 and pr.e_eliminado = 0
end  

GO

/* [dbo].[spx_ObtenerListaCierresAlmacen] */


create PROC [dbo].[spx_ObtenerListaCierresAlmacen]
as 
select top 50* from
( 
	SELECT c.Id_CierreAlmacen,'Cierre Almacen PNuevos' Tipo,c.Fecha,c.Observacion,c.Fecha_Registro,u.Nombre Usuario
	FROM tbl_CierreAlmacen c inner join tbl_Usuario u on u.Id_Usuario =c.Id_Usuario
	WHERE c.E_Eliminado = 0 
	union all
	SELECT c.Id_CierreAlmacenPR_PD Id_CierreAlmacen,'Cierre Almacen PRetirados y Dañados' Tipo,c.Fecha,c.Observacion,c.Fecha_Registro,u.Nombre Usuario
	FROM tbl_CierreAlmacenPR_PD c inner join tbl_Usuario u on u.Id_Usuario =c.Id_Usuario
	WHERE c.E_Eliminado = 0 
) a order by Fecha desc
GO

/* [dbo].[spx_ObtenerListaCU] */

create proc [dbo].[spx_ObtenerListaCU]
as
Begin
	Select	
	 distinct(v.Id_Venta)Id_Venta,r.Id_Ruta,r.Nombre Ruta,ve.Id_Vendedor, ve.Nombre Vendedor,t.Id_TipoServicio,t.Nombre TipoServicio,
	 v.Fecha_Ejecucion,v.Fecha_Registro,v.OrdenTrabajo,v.CodigoCliente,v.Observacion
	
	From 
	tbl_venta V inner join tbl_codigoventacargousuario  cu on cu.id_venta=v.id_venta
	inner join tbl_TipoServicio t on V.Id_TipoServicio = T.Id_TipoServicio--3413
	inner join tbl_Ruta R on R.Id_Ruta = V.Id_Ruta 
	inner join tbl_vendedor ve on ve.id_vendedor=v.id_vendedor	
	Where
	v.E_eliminado = 0 and cu.e_eliminado=0
	order by v.id_venta desc
end


GO

/* [dbo].[spx_obtenerlistadigitadores] */
CREATE proc [dbo].[spx_ObtenerListaDigitadores]
as
select u.id_usuario Código, u.Nombre,Loggin,TipoUsuario EsAdmin,tu.Id_Rol , tu.Nombre Rol,CodEmpleado
from tbl_usuario u inner join tbl_Rol tu on tu.id_rol=u.id_rol
where u.E_Eliminado=0 
and u.id_rol=3
order by u.nombre 

GO

/* [dbo].[spx_obtenerlistadoconformacioncuadrillabackoffice] */
CREATE proc [dbo].[spx_ObtenerListadoConformacionCuadrillaBackOffice]
as
	select * FROM tbl_ConformacionCuadrillaBackOffice where e_eliminado=0 order by id desc

GO

/* [dbo].[spx_ObtenerListadoSucursalXUsuario] */
create proc [dbo].[spx_ObtenerListadoSucursalXUsuario](@Id_Usuario int)
as
select * from tbl_sucursal suc inner join tbl_UsuarioSucursal us on us.Id_Sucursal = suc.Id_Sucursal
inner join tbl_Usuario u on u.Id_Usuario = us.Id_Usuario
where suc.E_Eliminado=0 and us.E_Eliminado=0
and us.Id_Usuario = @Id_Usuario


GO

/* [dbo].[spx_ObtenerListaIngresoAlmacen_Devolucion] */

CREATE proc [dbo].[spx_ObtenerListaIngresoAlmacen_Devolucion]
as
	select * from (
		select 	im.Id_IngresoAlmacen Codigo,
				--im.Id_IngresoAlmacen,
				im.Proveedor, 
				im.Fecha,
				im.Observacion,
				im.NroRecibo,
				im.Id_Usuario,
				um.Nombre,
				--'Material Comprado en Oficina' TipoIngreso,
				'Ingreso' TipoIngreso,
				'Completo' EstadoIngresoCompleto
		from tbl_IngresoAlmacen im, tbl_Usuario um
		where im.Id_Usuario = um.Id_Usuario 
				and im.E_Eliminado = 0
			union all 
			select 	imt.id_IngresoMaterialTigo Codigo,
					--imt.Id_Ingreso Id_IngresoAlmacen,
					imt.Proveedor, 
					imt.fechaIngreso Fecha,
					imt.Observacion,
					imt.nroComprobante NroRecibo,
					imt.Id_Usuario,
					umt.Nombre,
					--'Material Entregado X Tigo' TipoIngreso,
					'Pedido' TipoIngreso,
					case when imt.estadoIngresoCompleto = 0 
						then 'Falta Registrar Productos' 
						else 'Completo' 
						end estadoIngresoCompleto
			from tbl_IngresoMaterialTigo imt, tbl_Usuario umt
			where imt.Id_Usuario = umt.Id_Usuario 
					and imt.e_eliminado = 0
			--and dbo.DateOnly( imt.fechaIngreso ) = dbo.DateOnly( @Fecha )
		) ingresoM order by ingresoM.Fecha desc
GO

/* [dbo].[spx_ObtenerListaIngresoAlmacenPedidos] */

create proc spx_ObtenerListaIngresoAlmacenPedidos
as
begin
	select top 100 id_IngresoMaterialTigo Codigo,fechaIngreso Fecha,'Pedido'TipoIngreso,
	case when estadoIngresoCompleto = 1 then 'Completo' else 'Incompleto' end EstadoIngresoCompleto,
	proveedor,nroComprobante NroRecibo,observacion,u.id_Usuario,u.Nombre
	from tbl_IngresoMaterialTigo i inner join tbl_Usuario u on u.Id_Usuario=i.id_Usuario
	where i.e_eliminado = 0
	order by id_IngresoMaterialTigo desc
end 
GO

/* [dbo].[spx_ObtenerListaRol] */
create proc spx_ObtenerListaRol
as
select Id_Rol, Nombre Rol from tbl_rol 
where e_eliminado=0
order by nombre 

GO

/* [dbo].[spx_ObtenerListaSupervisor] */
create proc [dbo].[spx_ObtenerListaSupervisor]
as
select u.id_usuario Código, u.Nombre,Loggin,TipoUsuario EsAdmin,tu.Id_Rol , tu.Nombre Rol,CodEmpleado
from tbl_usuario u inner join tbl_Rol tu on tu.id_rol=u.id_rol
where u.E_Eliminado=0 
and u.id_rol=9
order by u.nombre 


GO

/* [dbo].[spx_ObtenerListaUsuario] */
CREATE proc [dbo].[spx_ObtenerListaUsuario]
as
select u.id_usuario Código, u.Nombre,Loggin,TipoUsuario EsAdmin,tu.Id_Rol , tu.Nombre Rol,CodEmpleado
from tbl_usuario u inner join tbl_Rol tu on tu.id_rol=u.id_rol
where u.E_Eliminado=0 order by u.nombre


GO

/* [dbo].[spx_ObtenerMenu_X_IdRol] */
CREATE proc [dbo].[spx_ObtenerMenu_X_IdRol](@id_rol int)
as
select * from tbl_Rolmenu where E_Eliminado = 0
and id_rol =@id_rol
GO

/* [dbo].[spx_ObtenerMenuRolXIdRol] */
CREATE proc [dbo].[spx_ObtenerMenuRolXIdRol](@IdRol int)
as 
declare @id int 
set @id = (select top 1 id_rol from tbl_Rol where id_rol = @IdRol and E_Eliminado=0) 
set @id=isnull(@id,-1)
SELECT  m.Id_Menu,upper(m.NOMBRE) as Nombre, m.E_Eliminado
FROM    TBL_TABLA_MENU m INNER JOIN 
tbl_rolmenu mu ON m.id_menu = mu.Id_menu
where mu.id_Rol = @id  and m.E_Eliminado =0 and mu.E_ELIMINADO = 0
GO

/* [dbo].[spx_ObtenerMenuXUsuario] */
CREATE proc [dbo].[spx_ObtenerMenuXUsuario](@Id_Usuario int)
as
	select * from tbl_TablaMenu tm inner join tbl_TablaMenuUsuario tmu on tmu.Id_Menu= tm.Id_TablaMenu
	where tm.E_Eliminado=0
	and tmu.Id_Usuario=@Id_Usuario
GO

/* [dbo].[spx_ObtenerNombre_Rol] */
create proc spx_ObtenerNombre_Rol(@rol nvarchar(150))
as
select nombre from tbl_rol where e_eliminado=0 and nombre=@rol

GO

/* [dbo].[spx_ObtenerNomencladores] */
CREATE proc [dbo].[spx_ObtenerNomencladores]
as
	select * from tbl_producto where sufijonomenclador <> '' and e_eliminado=0
	
	
GO

/* [dbo].[spx_ObtenerOrdeTrabajoCompletaXID] */

	

CREATE proc [dbo].[spx_ObtenerOrdeTrabajoCompletaXID](@id int)
as
begin 
	select v.Id_Venta,v.Id_Usuario,u.Nombre,v.Id_Vendedor, ve.Nombre Tecnico, r.Id_Ruta,r.Nombre Ruta,
	ts.Id_TipoServicio,ts.Nombre TipoServicio,v.Fecha_Ejecucion,v.Fecha_Registro,
	v.OrdenTrabajo,v.Observacion,v.Id_Estado, e.Nombre Estado,
	--suc.Id_Sucursal,suc.Sucursal,
	v.CodigoCliente
	,v.TieneObservacion
	from tbl_Venta v inner join tbl_TipoServicio ts on ts.Id_TipoServicio=v.Id_TipoServicio
	inner join tbl_Vendedor ve on v.Id_Vendedor= ve.Id_Vendedor
	inner join tbl_Usuario u on u.Id_Usuario= v.Id_Usuario
	inner join tbl_Estado e on e.id_estado=v.Id_Estado
	inner join tbl_Ruta r on r.Id_Ruta = v.Id_Ruta
	--inner join tbl_Sucursal suc on suc.Id_Sucursal = v.Id_Sucursal
	where Id_Venta =@id and v.E_Eliminado=0

	select cv.Id_CodigoVenta, cv.Id_Venta,pr.Id_Producto,pr.Nombre,
	tm.Id_TipoMaterial,tm.Nombre TipoMaterial,cv.Cod_Inicio,cv.ChipID,cv.Cantidad
	from tbl_CodigoVenta cv inner join tbl_producto pr on pr.Id_Producto = cv.Id_Producto
	inner join tbl_TipoMaterial tm on tm.Id_TipoMaterial =cv.Id_TipoMaterial
	where Id_Venta =@id
	and cv.E_Eliminado=0
end
GO

/* [dbo].[spx_ObtenerPedidoVendedorRFechas] */

create  proc [dbo].[spx_ObtenerPedidoVendedorRFechas] (@fecha datetime,@FechaFin datetime)
as
SELECT     dbo.tbl_PedidoVendedor.Id_PedidoVendedor, dbo.tbl_PedidoVendedor.Id_Ruta, dbo.tbl_Ruta.Nombre, dbo.tbl_PedidoVendedor.Fecha, 
dbo.tbl_PedidoVendedor.Observacion
FROM         dbo.tbl_PedidoVendedor INNER JOIN
dbo.tbl_Ruta ON dbo.tbl_PedidoVendedor.Id_Ruta = dbo.tbl_Ruta.Id_Ruta
WHERE dbo.tbl_PedidoVendedor.E_Eliminado = 0 and dbo.dateonly(tbl_PedidoVendedor.Fecha) between dbo.dateonly(@fecha) and dbo.dateonly(@FechaFin)
order by dbo.tbl_Ruta.id_ruta

GO

/* [dbo].[spx_ObtenerPlacavehiculos] */
create proc spx_ObtenerPlacavehiculos
as 
select * from tbl_placaVehiculo 
where e_eliminado = 0

GO

/* [dbo].[spx_ObtenerPlacavehiculos_SinTecnico] */
create  proc [dbo].[spx_ObtenerPlacavehiculos_SinTecnico]--53
as
select * from tbl_placaVehiculo p 
where p.e_eliminado=0 and p.id not in (select idvehiculo from tbl_vendedor where e_eliminado=0 and idvehiculo is not null)

GO

/* [dbo].[spx_obtenerplacavehiculos_tecnico] */
CREATE proc [dbo].[spx_ObtenerPlacavehiculos_Tecnico]--53
as
select id, placa,sucursal,case when  v.Nombre is null then '' else v.Nombre end Tecnico
from tbl_placaVehiculo pv left join tbl_vendedor v on v.vehiculo=pv.placa
where pv.e_eliminado=0 order by pv.placa

GO

/* [dbo].[spx_obtenerprivilegiosroldetalle] */
CREATE PROCEDURE dbo.spx_ObtenerPrivilegiosRolDetalle
    @IdRol INT
AS
BEGIN
    SET NOCOUNT ON;

    IF @IdRol IS NULL
    BEGIN
        RAISERROR('IdRol es requerido.', 16, 1);
        RETURN;
    END

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Rol r
        WHERE r.Id_Rol = @IdRol
          AND ISNULL(r.E_Eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('Rol no encontrado o inactivo.', 16, 1);
        RETURN;
    END

    SELECT m.ID_MENU AS Id_Menu,
           m.NOMBRE AS Nombre,
           m.NIVEL AS Nivel,
           m.PADRE AS Padre,
           CASE
               WHEN rm.Id_RolMenu IS NULL THEN CAST(0 AS bit)
               ELSE CAST(1 AS bit)
           END AS Asignado
    FROM dbo.tbl_Tabla_Menu m
    LEFT JOIN dbo.tbl_RolMenu rm
           ON rm.Id_Menu = m.ID_MENU
          AND rm.Id_Rol = @IdRol
          AND ISNULL(rm.E_Eliminado, 0) = 0
    WHERE ISNULL(m.E_Eliminado, 0) = 0
    ORDER BY m.PADRE, m.NIVEL, m.ID_MENU;
END

GO

/* [dbo].[spx_obtenerprivilegiosroles] */
CREATE PROCEDURE dbo.spx_ObtenerPrivilegiosRoles
AS
BEGIN
    SET NOCOUNT ON;

    SELECT r.Id_Rol,
           r.Nombre AS Rol
    FROM dbo.tbl_Rol r
    WHERE ISNULL(r.E_Eliminado, 0) = 0
    ORDER BY r.Nombre;
END

GO

/* [dbo].[spx_ObtenerProductosConSerie] */
CREATE proc [dbo].[spx_ObtenerProductosConSerie]
as
	select p.* 
	from tbl_producto p inner join tbl_ProductosMascaras pm on pm.id_producto=p.id_producto
	where p.E_Eliminado = 0 and pm.E_Eliminado = 0 and
	(p.DigitosImei > 0 or p.Digitoschipid > 0)
	order by p.nombre
	
	 
GO

/* [dbo].[spx_ObtenerProductosConSerieSinTarjeta] */


create proc [dbo].[spx_ObtenerProductosConSerieSinTarjeta]
as
	select * 
	from tbl_producto 
	where DigitosImei > 0  and DigitosChipId>0 and E_Eliminado = 0
	order by nombre
GO

/* [dbo].[spx_ObtenerProductosDatosXRuta] */
CREATE proc [dbo].[spx_ObtenerProductosDatosXRuta](@Id_Ruta int,@Id_Estado int)
as

if(@Id_Estado >0)
begin
	if (@Id_Ruta = -1)
	begin
		select r.Nombre Ruta, pr.Id_Producto,pr.Nombre Producto ,p.serial,p.chipId,ep.Nombre EstadoProducto,FechaTransaccion
		from tbl_productos p inner join tbl_producto pr on pr.Id_Producto=p.id_producto
		inner join tbl_Ruta r on r.Id_Ruta = p.id_Ruta
		inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = p.id_EstadoProducto
		where p.id_EstadoProducto =@Id_Estado and p.e_eliminado=0
	end
	else
	begin
		select r.Nombre Ruta, pr.Id_Producto,pr.Nombre Producto ,p.serial,p.chipId,ep.Nombre EstadoProducto,FechaTransaccion
		from tbl_productos p inner join tbl_producto pr on pr.Id_Producto=p.id_producto
		inner join tbl_Ruta r on r.Id_Ruta = p.id_Ruta
		inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = p.id_EstadoProducto
		where p.id_Ruta=@id_ruta and p.id_EstadoProducto =@Id_Estado and p.e_eliminado=0
	end	
end
else 
begin
	if (@Id_Ruta = -1)
	begin
		select r.Nombre Ruta, pr.Id_Producto,pr.Nombre Producto ,p.serial,p.chipId,ep.Nombre EstadoProducto,FechaTransaccion
		from tbl_productos p inner join tbl_producto pr on pr.Id_Producto=p.id_producto
		inner join tbl_Ruta r on r.Id_Ruta = p.id_Ruta
		inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = p.id_EstadoProducto
		where p.id_EstadoProducto in (select Id_EstadoProducto from tbl_EstadoProducto ) and p.e_eliminado=0
	end
	else
	begin
		select r.Nombre Ruta, pr.Id_Producto,pr.Nombre Producto ,p.serial,p.chipId,ep.Nombre EstadoProducto,FechaTransaccion
		from tbl_productos p inner join tbl_producto pr on pr.Id_Producto=p.id_producto
		inner join tbl_Ruta r on r.Id_Ruta = p.id_Ruta
		inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = p.id_EstadoProducto
		where p.id_Ruta=@id_ruta and p.id_EstadoProducto in (select Id_EstadoProducto from tbl_EstadoProducto ) and p.e_eliminado=0
	end
	
end
GO

/* [dbo].[spx_ObtenerProductosNoEntregadosOT] */
CREATE proc [dbo].[spx_ObtenerProductosNoEntregadosOT]( @Id_Ruta int)
as	
select dev.Id_Devolucion,dd.Id_DetalleDevolucion,dev.NroOrdenTrabajo,dev.Id_Ruta,dev.Id_TipoDevolucion,dev.Id_Vendedor,
pr.Id_Producto,
pr.Nombre,dd.Cod_Inicio,dd.ChipID,dd.Cantidad,dev.Fecha, dev.Id_Venta,
pr.PrecioVenta,dd.Id_TipoMaterial,tm.nombre TipoMaterial
from tbl_DetalleDevolucion dd inner join tbl_Devolucion dev on dev.Id_Devolucion = dd.Id_Devolucion
inner join tbl_producto pr on pr.Id_Producto = dd.Id_Producto
inner join tbl_tipomaterial tm on tm.id_tipomaterial=dd.id_tipomaterial
and dd.PendienteRecojo=1 and dd.E_Eliminado=0 and dev.E_Eliminado=0	and dev.Id_Ruta=@id_ruta

GO

/* [dbo].[spx_ObtenerProductosPCargoUsuario] */
CREATE proc [dbo].[spx_ObtenerProductosPCargoUsuario]
as
select * from 
(
	--select * 
	--from tbl_producto 
	--where DigitosImei > 0 and E_Eliminado = 0
	--union all
	select * 
	from tbl_producto where  E_Eliminado = 0
---	where id_producto in (5)
)a order by nombre 

GO

/* [dbo].[spx_ObtenerProductosPendientesEntrega] */

CREATE proc spx_ObtenerProductosPendientesEntrega
as

declare @tabla table(Id_Producto int,Producto nvarchar(150),Cantidad int)
declare @tabla1 table(Id_Producto int,Producto nvarchar(150),Cantidad int)
insert into @tabla
	select pr.Id_Producto,pr.Nombre Producto, SUM(ddev.Cantidad) Cantidad
	from tbl_detalleDevolucion ddev inner join tbl_Devolucion dev on dev.Id_Devolucion=ddev.Id_Devolucion
	inner join tbl_producto pr on pr.Id_Producto = ddev.Id_Producto
	inner join tbl_Ruta r on r.Id_Ruta = dev.Id_Ruta
	inner join tbl_Vendedor v on v.Id_Vendedor = dev.Id_Vendedor
	inner join tbl_TipoDevolucion td on td.Id_TipoDevolucion = dev.Id_TipoDevolucion
	where dev.Id_TipoDevolucion in(1,2) and dev.E_Eliminado=0 and ddev.E_Eliminado=0 and NroOrdenTrabajo <>'-1' and ddev.Cod_Inicio=''
	group by pr.Id_Producto,pr.Nombre

insert into @tabla1
	select pr.Id_Producto, pr.Nombre Producto,SUM(ddev.Cantidad) Cantidad
	from tbl_detalleDevolucion ddev inner join tbl_Devolucion dev on dev.Id_Devolucion=ddev.Id_Devolucion
	inner join tbl_producto pr on pr.Id_Producto = ddev.Id_Producto
	inner join tbl_Ruta r on r.Id_Ruta = dev.Id_Ruta
	inner join tbl_Vendedor v on v.Id_Vendedor = dev.Id_Vendedor
	inner join tbl_TipoDevolucion td on td.Id_TipoDevolucion = dev.Id_TipoDevolucion
	where dev.Id_TipoDevolucion in(4) and dev.E_Eliminado=0 and ddev.E_Eliminado=0 and ddev.Cod_Inicio=''
	group by pr.Id_Producto,pr.Nombre

SELECT p.serial,p.chipId,e.Nombre EstadoProducto,r.Nombre Ruta,pr.Nombre Producto,pr.Id_Producto,
	1 Cantidad
	FROM tbl_Productos p inner join tbl_Ruta r on r.Id_Ruta = p.id_Ruta 
	inner join tbl_EstadoProducto e on e.Id_EstadoProducto = p.id_EstadoProducto
	inner join tbl_producto pr on pr.Id_Producto = p.id_producto
	WHERE p.id_EstadoProducto in (4,5,11,10) and p.e_eliminado=0
union all
select '','','','GRUPO ALMACEN' Ruta,t.Producto,t.Id_Producto,(t.Cantidad -  t1.Cantidad)TieneAlmacen
 from @tabla t inner join @tabla1 t1 on t1.Id_Producto = t.Id_Producto


GO

/* [dbo].[spx_obtenerramal] */
CREATE PROCEDURE spx_ObtenerRamal
as
select * from tbl_ramal where e_eliminado=0

GO

/* [dbo].[spx_ObtenerRutaXIdTecnico] */
CREATE PROCEDURE [dbo].[spx_ObtenerRutaXIdTecnico]
    @Id_Tecnico INT = NULL
AS
BEGIN
    SET NOCOUNT ON;

    IF OBJECT_ID('dbo.tbl_Ruta', 'U') IS NULL
    BEGIN
        RAISERROR('No existe la tabla dbo.tbl_Ruta.', 16, 1);
        RETURN;
    END

    DECLARE @sql NVARCHAR(MAX);

    SET @sql = N'
    SELECT
        r.Id_Ruta AS id_ruta,
        r.Nombre AS cuadrilla,
        r.Nombre AS ruta,
        r.Id_Vendedor AS id_tecnico,'
        + CASE WHEN COL_LENGTH('dbo.tbl_Ruta', 'Tipo') IS NOT NULL
            THEN N' r.Tipo AS tipo,'
            ELSE N' CAST(NULL AS NVARCHAR(50)) AS tipo,'
          END
        + CASE WHEN COL_LENGTH('dbo.tbl_Ruta', 'visible') IS NOT NULL
            THEN N' r.visible AS visible,'
            ELSE N' CAST(NULL AS BIT) AS visible,'
          END
        + CASE WHEN COL_LENGTH('dbo.tbl_Ruta', 'BodegaTigo') IS NOT NULL
            THEN N' r.BodegaTigo AS bodega_tigo,'
            ELSE N' CAST(NULL AS NVARCHAR(250)) AS bodega_tigo,'
          END
        + CASE WHEN COL_LENGTH('dbo.tbl_Ruta', 'almacenTigo') IS NOT NULL
            THEN N' r.almacenTigo AS almacen_tigo,'
            ELSE N' CAST(NULL AS NVARCHAR(250)) AS almacen_tigo,'
          END
        + N'
        r.Id_Ruta,
        r.Id_Vendedor,
        v.Nombre AS NombreTecnico,
        v.CuentaSF,
        v.SalesForce
    FROM dbo.tbl_Ruta r
    INNER JOIN dbo.tbl_vendedor v ON r.id_vendedor = v.id_vendedor
    WHERE ISNULL(r.E_Eliminado, 0) = 0
      AND (v.grupodigitacion IS NOT NULL AND v.grupodigitacion <> '''')
      AND (@Id_Tecnico IS NULL OR r.Id_Vendedor = @Id_Tecnico)
    ORDER BY r.Nombre;';

    EXEC sp_executesql @sql, N'@Id_Tecnico INT', @Id_Tecnico = @Id_Tecnico;
END

GO

/* [dbo].[spx_ObtenerSaldoRuta] */
CREATE proc spx_ObtenerSaldoRuta(@Id_Ruta int,@Fecha datetime)
as
declare @tablaSaldo table(
	Id_Producto int,
	Nombre nvarchar(max),
	Saldo decimal(18,2),
	Venta decimal(18,2),
	Sobrante decimal(18,2),		
	Precio decimal(18,2),
	TotalVendidos decimal(18,2)
	)
		
	insert into @tablaSaldo
	select st.Id_Producto,pr.Nombre,st.Cantidad ,0,0,pr.PrecioVenta,0
	from tbl_SaldoTarjetas st inner join tbl_producto pr on pr.Id_Producto = st.Id_Producto
	where Id_Ruta=@Id_Ruta and st.E_Eliminado=0


declare @Venta table(
	Id_Producto int,
	Id_Ruta int,
	Venta decimal(18,2),
	Nombre nvarchar(250)
)	
insert into @Venta
 exec sp_TraerVentaDiaRuta @Id_Ruta ,@Fecha
 
 	update @tablaSaldo
	set Venta = v.Venta
	from @tablaSaldo c inner join @venta v on v.id_Producto = c.id_producto
	
	update @tablaSaldo
	set TotalVendidos = Venta*Precio
	--from @tablaSaldo c inner join @venta v on v.id_Producto = c.id_producto
	
	update @tablaSaldo
	set Sobrante = Saldo - venta 
	
		
select * from @tablaSaldo order by Sobrante asc
GO

/* [dbo].[spx_ObtenerSaldoRuta_CierreAlmacen] */

--spx_ObtenerSaldoRuta_CierreAlmacen '06/07/2022'
create proc spx_ObtenerSaldoRuta_CierreAlmacen(@fecha datetime)
as
declare @tbsaldoAlmacen table(id_producto int, nombre nvarchar(150),SaldoDiaAnterior decimal(18,2),SaldoDiaAnteriorDevolucion decimal(18,2)
,IngresoDia decimal(18,2),DevolucionIngreso decimal(18,2),DevolucionSalida decimal(18,2),SalidaDia decimal(18,2),SalidaBaja decimal(18,2),
SaldoDiaHoy decimal(18,2),SaldoDiaHoyDevolucion decimal(18,2)
)
insert into @tbsaldoAlmacen 
exec sp_ObtenerCierreAlmacen @fecha


select p.id_producto,p.nombre Producto,tp.Id_TipoProducto,tp.Nombre TipoProducto,SaldoDiaHoy ,'SaldoAlmacen' Tipo, 'Almacen' Ruta
from @tbsaldoAlmacen  t
inner join tbl_producto p on p.id_producto = t.id_producto
inner join tbl_tipoproducto tp on tp.id_tipoproducto = p.id_tipoproducto
where t.SaldoDiaHoy>0

union all

select p.id_producto,p.nombre Producto,tp.id_tipoproducto ,tp.nombre TipoProducto,
 s.cantidad,  'Saldo Ruta' Tipo,r.nombre Ruta
from tbl_saldotarjetas s inner join tbl_producto p  on p.id_producto = s.id_producto
inner join tbl_ruta r on r.id_ruta = s.id_ruta
inner join tbl_tipoproducto tp on tp.id_tipoproducto = p.id_tipoproducto
where s.cantidad>0
and s.e_eliminado=0 and p.e_eliminado=0 and r.e_eliminado=0
and r.id_ruta>0

GO

/* [dbo].[spx_ObtenerSalidaTraspaso] */

CREATE proc [dbo].[spx_ObtenerSalidaTraspaso](@Codigo int)
as
begin
	select st.Id_SalidaTraspaso,st.Id_Ruta,r.Nombre Ruta,st.Fecha,st.Fecha_Registro,st.Observacion,
	u.Id_Usuario,u.Nombre Usuario 
	from tbl_SalidaTraspaso st inner join tbl_ruta r on r.id_ruta = st.id_ruta 
	inner join tbl_Usuario u on u.Id_Usuario = st.Id_Usuario
	where st.Id_SalidaTraspaso = @Codigo

	select cst.Id_CodigoSalidaTraspaso,cst.Id_SalidaTraspaso,cst.Id_Producto,pr.Nombre Producto,
	cst.Serie,cst.ChipID,cst.Cantidad
	from tbl_CodigoSalidaTraspaso cst inner join tbl_producto pr on pr.Id_Producto = cst.Id_Producto
	where cst.Id_SalidaTraspaso = @Codigo
	order by pr.nombre

	select cst.Id_DetalleSalidaTraspaso,cst.Id_SalidaTraspaso,cst.Id_Producto,pr.Nombre Producto,
	cst.Cantidad
	from tbl_DetalleSalidaTraspaso cst inner join tbl_producto pr on pr.Id_Producto = cst.Id_Producto
	where cst.Id_SalidaTraspaso = @Codigo
	order by pr.nombre
end



GO

/* [dbo].[spx_ObtenerSalidaTraspasoPendiente] */
CREATE proc [dbo].[spx_ObtenerSalidaTraspasoPendiente](@Codigo int)
as
begin
---select * from tbl_SalidaTraspasoPendiente
	select st.Id_SalidaTraspasoPendiente,st.Id_SalidaTraspaso,st.FechaPendiente,st.FechaRegistrado,st.Estado,st.Observacion,
	st.Id_UsuarioPendiente,u.Nombre UsuarioPendiente,st.Id_UsuarioRegistrado,u.Nombre UsuarioRegistrado
	from tbl_SalidaTraspasoPendiente st --inner join tbl_ruta r on r.id_ruta = st.id_ruta 
	inner join tbl_Usuario u on u.Id_Usuario = st.Id_UsuarioPendiente
	inner join tbl_Usuario ureg on ureg.Id_Usuario = st.Id_UsuarioRegistrado
	where st.Id_SalidaTraspasoPendiente = @Codigo

	select cst.Id_CodigoSalidaTraspasoPendiente,cst.Id_SalidaTraspasoPendiente,cst.Id_Producto,pr.Nombre Producto,
	cst.Serial,cst.ChipID,cst.Cantidad
	from tbl_CodigoSalidaTraspasoPendiente cst inner join tbl_producto pr on pr.Id_Producto = cst.Id_Producto
	where cst.Id_SalidaTraspasoPendiente= @Codigo
	order by pr.nombre
--select * from tbl_detalleSalidaTraspasoPendiente	
	select cst.Id_DetalleSalidaTraspasoPendiente,cst.Id_SalidaTraspasoPendiente,cst.Id_Producto,pr.Nombre Producto,
	cst.Cantidad
	from tbl_DetalleSalidaTraspasoPendiente cst inner join tbl_producto pr on pr.Id_Producto = cst.Id_Producto
	where cst.Id_SalidaTraspasoPendiente = @Codigo
	order by pr.nombre
end
GO

/* [dbo].[spx_ObtenerSucursal] */
CREATE proc spx_ObtenerSucursal
as
select * from tbl_sucursal where e_eliminado=0
GO

/* [dbo].[spx_obtenersucursalesconexion] */
CREATE PROCEDURE [dbo].[spx_ObtenerSucursalesConexion]
as 
select * from tbl_Sucursal where E_Eliminado=0
---and SucursalConexionWeb =1 
 order by Sucursal

GO

/* [dbo].[spx_obtenersucursalesconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ObtenerSucursalesConformacionCuadrillaWeb]
AS
BEGIN
    SET NOCOUNT ON;

    SELECT DISTINCT
        LTRIM(RTRIM(v.sucursal)) AS sucursal
    FROM dbo.tbl_version v
    WHERE v.sucursal IS NOT NULL
      AND LTRIM(RTRIM(v.sucursal)) <> ''
    ORDER BY sucursal;
END



GO

/* [dbo].[spx_obtenersupervisores] */
CREATE PROCEDURE dbo.spx_ObtenerSupervisores
AS
BEGIN
    SET NOCOUNT ON;

    SELECT u.Id_Usuario,
           u.Nombre,
           u.Loggin,
           u.Id_Rol,
           r.Nombre AS Rol
    FROM dbo.tbl_Usuario u
    INNER JOIN dbo.tbl_Rol r ON r.Id_Rol = u.Id_Rol
    WHERE u.E_Eliminado = 0
      AND (r.Nombre = 'Supervisor' OR r.Nombre LIKE '%supervisor%')
    ORDER BY u.Nombre;
END

GO

/* [dbo].[spx_obtenersupervisoresconformacioncuadrillaweb] */

CREATE PROC [dbo].[spx_ObtenerSupervisoresConformacionCuadrillaWeb]
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        u.Id_Usuario AS idUsuarioSupervisor,
        u.Nombre AS supervisorACargo,
        r.Nombre AS rol,
        u.*
    FROM dbo.tbl_Usuario u
    INNER JOIN dbo.tbl_Rol r
        ON r.Id_Rol = u.Id_Rol
    WHERE u.E_Eliminado = 0
      AND (r.Nombre = 'Supervisor' OR r.Nombre LIKE '%supervisor%')
    ORDER BY u.Nombre;
END



GO

/* [dbo].[spx_ObtenerTablaProcedimientos] */
create proc [dbo].[spx_ObtenerTablaProcedimientos]
as
select * from tbl_TablaProcedimientos where E_Eliminado = 0


GO

/* [dbo].[spx_obtenertecnicosconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ObtenerTecnicosConformacionCuadrillaWeb]
AS
BEGIN
    SET NOCOUNT ON;

    SELECT
        v.Id_Vendedor AS id_tecnico,
        v.Nombre AS tecnico,
        v.CuentaSF AS cuenta_sf,
        v.SalesForce AS salesforce,
        v.Habilidad AS habilidad,
        v.Vehiculo AS vehiculo,
        r.Id_Ruta AS id_ruta,
        r.Nombre AS grupo,
        r.BodegaTigo AS almacen,
        r.almacenTigo AS grupoDigitacion,
        v.*
    FROM dbo.tbl_Vendedor v inner join tbl_Ruta r on r.id_vendedor=v.id_vendedor 
    
    WHERE v.E_Eliminado = 0 and v.id_vendedor >0 and r.e_eliminado=0 and (v.cuentasf is not null and v.cuentasf <>'')
    and (v.salesforce is not null and v.salesforce <>'')
    ORDER BY v.Nombre;
END

GO

/* [dbo].[spx_obtenertecnicosenruta] */
CREATE proc spx_ObtenerTecnicosEnRuta
as
select * from tbl_vendedor where e_eliminado=0 and id_vendedor in (
	select id_vendedor from tbl_ruta where e_eliminado=0 
)and id_vendedor>0
order by nombre

GO

/* [dbo].[spx_ObtenerTipoBajaProductosPendiente] */
create proc spx_ObtenerTipoBajaProductosPendiente
as
select * from tbl_TipoBajaProductosPendiente where e_eliminado=0

GO

/* [dbo].[spx_ObtenerTipoCobroBaja] */

create proc dbo.spx_ObtenerTipoCobroBaja 
as
select * from tbl_TipoCobroBaja where e_eliminado=0 order by tipocobro
GO

/* [dbo].[spx_ObtenerTipoDatosXTabla] */
create proc [dbo].[sp_ObtenerTipoDatosXTabla](@NombreTabla nvarchar(100))
as
Select  TABLE_NAME,COLUMN_NAME,IS_NULLABLE,DATA_TYPE,CHARACTER_MAXIMUM_LENGTH,NUMERIC_PRECISION,NUMERIC_PRECISION_RADIX
from information_schema.columns WHERE TABLE_NAME=@NombreTabla
order by ORDINAL_POSITION
GO

/* [dbo].[spx_ObtenerTipoProducto] */
create proc spx_ObtenerTipoProducto
as 
select * from tbl_TipoProducto where E_Eliminado = 0
GO

/* [dbo].[spx_ObtenerTipoServicio] */
CREATE PROCEDURE spx_ObtenerTipoServicio
as
select * from tbl_tiposervicio where e_eliminado=0 order by prefijo

GO

/* [dbo].[spx_ObtenerTipoServicioCUNR] */
create proc [dbo].[spx_ObtenerTipoServicioCUNR]
as
select * from tbl_tiposervicio where id_tiposervicio in (4,5,12,14,6,8,7) order by nombre 
GO

/* [dbo].[spx_obtenertipotecnologia] */
CREATE PROCEDURE spx_ObtenerTipoTecnologia(@id_ruta int)
as

select * from tbl_tipotecnologia where e_eliminado=0
and tipoGrupo in (select tipoGrupo from tbl_ruta where id_ruta=@id_ruta)

GO

/* [dbo].[spx_ObtenerTodasRuta] */
CREATE proc [dbo].[spx_ObtenerTodasRuta]
as
select * from tbl_Ruta where E_Eliminado=0 order by Id_ruta, Nombre 


GO

/* [dbo].[spx_ObtenerTodosRol] */
CREATE proc [dbo].[spx_ObtenerTodosRol]
as
select * from tbl_Rol where E_Eliminado = 0

GO

/* [dbo].[spx_ObtenerUsoPromedioMaterialOT] */
--spx_ObtenerUsoPromedioMaterialOT 92,'16/10/2018 9:44:43','15/11/2018 9:44:43'

CREATE proc [dbo].[spx_ObtenerUsoPromedioMaterialOT](@Id_Ruta int, @FechaInicio datetime , @FechaFin datetime)
as

declare @Ordenes table (Id_Venta int ,Fecha datetime,Grupo nvarchar(700), Id_TipoServicio int, TipoServicio nvarchar(700),Ordenes nvarchar(15),marca int)
declare @DetalleOrdenes table (Id_Venta int,Fecha datetime,Grupo nvarchar(700),Ordenes nvarchar(15),Id_TipoServicio int,TipoServicio nvarchar(700),
								Id_Producto int,Producto nvarchar(250), Cantidad decimal(18,2),Promedio decimal(18,2),marca int)

insert into @Ordenes
	select Id_Venta,dbo.DateOnly(Fecha_Ejecucion),r.Nombre,ts.Id_TipoServicio,ts.Nombre,v.OrdenTrabajo,0 from tbl_Venta v inner join tbl_TipoServicio ts on ts.Id_TipoServicio =v.Id_TipoServicio 	
	inner join tbl_Ruta r on r.Id_Ruta = v.Id_Ruta
	where v.Id_Ruta = @Id_Ruta and dbo.DateOnly(Fecha_Ejecucion) between dbo.DateOnly(@FechaInicio) and dbo.DateOnly(@FechaFin) AND v.E_Eliminado =0
		
	
insert into @DetalleOrdenes
	select cv.Id_Venta,dbo.DateOnly(o.Fecha),o.Grupo,o.Ordenes,o.Id_TipoServicio,o.TipoServicio,cv.Id_Producto,pr.Nombre,SUM(cv.Cantidad)Cantidad,
	pu.CantidadMaxima,
	case when sum(cv.Cantidad) > pu.CantidadMaxima then 1 else 0 end Marca
	from tbl_CodigoVenta cv inner join tbl_producto pr on pr.Id_Producto=cv.Id_Producto	 
	inner join @Ordenes o on o.Id_Venta=cv.Id_Venta
	left join tbl_PromedioUsoMateriales pu on pu.Id_Producto = pr.Id_Producto and pu.Id_TipoServicio = o.Id_TipoServicio
	and pu.E_Eliminado =0
	group by cv.Id_Venta,dbo.DateOnly(o.Fecha),o.Grupo,o.Ordenes,o.Id_TipoServicio,o.TipoServicio,cv.Id_Producto,pr.Nombre,pu.CantidadMaxima
	
update @Ordenes
set marca = s.mysum
from @Ordenes as o,
( 
select SUM(do.marca)as mysum,Id_Venta
	from @DetalleOrdenes do
	group by do.Id_Venta
)as s
where s.id_venta=o.id_venta

select Id_Venta,Fecha,Grupo,TipoServicio,Ordenes,marca TieneExcedente from @Ordenes
select Id_Venta,Fecha,Ordenes,Producto,Cantidad,Promedio,marca TieneExcedente from @DetalleOrdenes

--spx_ObtenerUsoPromedioMaterialOT 3,'23/10/2018','24/10/2018'


--select * from tbl_Usuario
--select * from tbl_PromedioUsoMateriales where Id_TipoProducto=1
GO

/* [dbo].[spx_ObtenerUsoPromedioMaterialOTAlarma] */

CREATE proc [dbo].[spx_ObtenerUsoPromedioMaterialOTAlarma](@FechaInicio datetime , @FechaFin datetime)
as

declare @Ordenes table (Id_Venta int ,Fecha datetime,Grupo nvarchar(250), Id_TipoServicio int, TipoServicio nvarchar(250),Ordenes nvarchar(15),marca int)
declare @DetalleOrdenes table (Id_Venta int,Fecha datetime,Grupo nvarchar(250),Ordenes nvarchar(15),Id_TipoServicio int,TipoServicio nvarchar(250),
								Id_Producto int,Producto nvarchar(250), Cantidad decimal(18,2),Promedio decimal(18,2),marca int)


insert into @Ordenes
	select Id_Venta,dbo.DateOnly(Fecha_Ejecucion),r.Nombre,ts.Id_TipoServicio,ts.Nombre,v.OrdenTrabajo,0 from tbl_Venta v inner join tbl_TipoServicio ts on ts.Id_TipoServicio =v.Id_TipoServicio 	
	inner join tbl_Ruta r on r.Id_Ruta = v.Id_Ruta
	where dbo.DateOnly(Fecha_Ejecucion) between dbo.DateOnly(@FechaInicio) and dbo.DateOnly(@FechaFin) AND v.E_Eliminado =0
	
insert into @DetalleOrdenes
	select cv.Id_Venta,dbo.DateOnly(o.Fecha),o.Grupo,o.Ordenes,o.Id_TipoServicio,o.TipoServicio,cv.Id_Producto,pr.Nombre,SUM(cv.Cantidad)Cantidad,
	pu.CantidadMaxima,
	case when sum(cv.Cantidad) > pu.CantidadMaxima then 1 else 0 end Marca
	from tbl_CodigoVenta cv inner join tbl_producto pr on pr.Id_Producto=cv.Id_Producto	 
	inner join @Ordenes o on o.Id_Venta=cv.Id_Venta
	left join tbl_PromedioUsoMateriales pu on pu.Id_Producto = pr.Id_Producto and pu.Id_TipoServicio = o.Id_TipoServicio AND pu.E_Eliminado=0
	group by cv.Id_Venta,dbo.DateOnly(o.Fecha),o.Grupo,o.Ordenes,o.Id_TipoServicio,o.TipoServicio,cv.Id_Producto,pr.Nombre,pu.CantidadMaxima
	
update @Ordenes
set marca = s.mysum
from @Ordenes as o,
( 
select SUM(do.marca)as mysum,Id_Venta
	from @DetalleOrdenes do
	group by do.Id_Venta
)as s
where s.id_venta=o.id_venta

select Id_Venta,convert(varchar(10), Fecha, 103)Fecha,Grupo,TipoServicio,Ordenes,marca TieneExcedente from @Ordenes where marca >0
order by Fecha,Grupo,TipoServicio
GO

/* [dbo].[spx_obtenerUsuarioCNR] */
create proc [dbo].[spx_obtenerUsuarioCNR]
as 
select * from tbl_usuario where e_eliminado=0 and id_rol not in(4,5)

GO

/* [dbo].[spx_ObtenerUsuarioSucursal] */
create proc spx_ObtenerUsuarioSucursal(@Id_Usuario int)
 as
 select * from tbl_UsuarioSucursal where Id_Usuario=@Id_Usuario 
 and E_Eliminado=0
GO

/* [dbo].[spx_obtenervehiculosconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_ObtenerVehiculosConformacionCuadrillaWeb]
AS
select * from dbo.tbl_placaVehiculo
where e_eliminado=0 
and placa not in (select vehiculo from tbl_vendedor where e_eliminado=0 and vehiculo is not null)



GO

/* [dbo].[spx_ObtenerVersionActual] */
create proc [dbo].[spx_ObtenerVersionActual]
as 
select top 1* from tbl_Version where Habilitado=0
GO

/* [dbo].[spx_ObtenerVersionCentral_tblSucrusal] */
create proc spx_ObtenerVersionCentral_tblSucrusal(@sucursal nvarchar(50))
as
select * from tbl_sucursal where sucursal=@sucursal and e_eliminado=0

GO

/* [dbo].[spx_QuitarPendienteRecojo] */
create proc [dbo].[spx_QuitarPendienteRecojo](@Id_DetalleDevolucion int,@PendienteRecojo bit)
as
update tbl_DetalleDevolucion set PendienteRecojo = @PendienteRecojo  where Id_DetalleDevolucion =@Id_DetalleDevolucion

GO

/* [dbo].[spx_rechazarProductoDevuelvo] */
CREATE proc [dbo].[spx_rechazarProductoDevuelvo]( @idDevolucion int, @idDetalleDevolucion int, @idProducto int, @Serial nvarchar(50), @ChipID nvarchar(50), @conSerial int, @fechaTransaccion datetime, @idUsuario int )
as
	--update tbl_DetalleDevolucion set E_Eliminado = 1 where Id_DetalleDevolucion = @idDetalleDevolucion
	--if ( @conSerial = 1 ) begin
	--	update tbl_productos set id_EstadoProducto = 10 where id_producto = @idProducto and serial = @Serial
	--	insert into tbl_bitacora values ( @idProducto, @Serial,@ChipID, @idDevolucion, 'tbl_Devolucion', @fechaTransaccion, GETDATE(), 0, @idUsuario, 0, 'Rechazado', 10 )
	--end 		
	
	declare  @id_tipoDevolucion int
set @id_tipoDevolucion = (select id_tipodevolucion from tbl_Devolucion where Id_Devolucion =@idDevolucion)
if(@id_tipoDevolucion  = 6)--retirado
begin
	update tbl_DetalleDevolucion set E_Eliminado = 1 where Id_DetalleDevolucion = @idDetalleDevolucion
	if ( @conSerial = 1 ) begin
		update tbl_productos set id_EstadoProducto = 14 where id_producto = @idProducto and serial = @Serial
		insert into tbl_bitacora values ( @idProducto, @Serial,@ChipID, @idDevolucion, 'tbl_Devolucion', @fechaTransaccion, GETDATE(), 0, @idUsuario, 0, 'Rechazado_PRetirado', 14 )
	end 		
end
if(@id_tipoDevolucion  = 7)--dañado
begin
	update tbl_DetalleDevolucion set E_Eliminado = 1 where Id_DetalleDevolucion = @idDetalleDevolucion
	if ( @conSerial = 1 ) begin
		update tbl_productos set id_EstadoProducto = 13 where id_producto = @idProducto and serial = @Serial
		insert into tbl_bitacora values ( @idProducto, @Serial,@ChipID, @idDevolucion, 'tbl_Devolucion', @fechaTransaccion, GETDATE(), 0, @idUsuario, 0, 'Rechazado_PDañado', 13 )
	end 	
end


GO

/* [dbo].[spx_rechazarProductoDevuelvoTigoPendiente] */
create proc [dbo].[spx_rechazarProductoDevuelvoTigoPendiente]( @idDevolucion int, @idDetalleDevolucion int, @FechaRegistro datetime,@Id_UsuarioM  int)
as
declare  @id_tipoDevolucion int

--set @id_tipoDevolucion = (select id_tipodevolucion from tbl_Devolucion where Id_Devolucion =@idDevolucion)
--if(@id_tipoDevolucion  = 6)--retirado
--begin
	update tbl_CodigoDevolucionTigoPendiente set E_Eliminado = 1,
	fecharegistroM=@FechaRegistro,id_usuarioM=@Id_UsuarioM
	where Id_CodigoDevolucionTipoPendiente= @idDetalleDevolucion

--end	
GO

/* [dbo].[spx_registrarconformacioncuadrillabackoffice] */
CREATE PROCEDURE dbo.spx_RegistrarConformacionCuadrillaBackOffice
    @Fecha DATE = NULL,
    @Estado NVARCHAR(50),
    @Actividad NVARCHAR(50),
    @Id_Tecnico INT,
    @Cuenta_SF NVARCHAR(100) = NULL,
    @Salesforce NVARCHAR(100) = NULL,
    @Habilidad NVARCHAR(100) = NULL,
    @Vehiculo NVARCHAR(100) = NULL,
    @Grupo NVARCHAR(100) = NULL,
    @Almacen NVARCHAR(100) = NULL,
    @GrupoDigitacion NVARCHAR(100) = NULL,
    @IdUsuarioDigitador INT = NULL,
    @Digitador NVARCHAR(150) = NULL,
    @Tecnico NVARCHAR(150) = NULL,
    @Id_TecnicoAuxiliar INT = NULL,
    @Auxiliar NVARCHAR(150) = NULL,
    @IdUsuarioSupervisor INT,
    @SupervisorACargo NVARCHAR(150) = NULL,
    @Sucursal NVARCHAR(100),
    @Observacion NVARCHAR(500) = NULL,
    @IdUsuarioRegistra INT
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.tbl_ConformacionCuadrillaDiario (
        fecha,
        estado,
        actividad,
        id_tecnico,
        cuenta_sf,
        salesforce,
        habilidad,
        vehiculo,
        [grupo],
        almacen,
        grupoDigitacion,
        idUsuarioDigitador,
        digitador,
        tecnico,
        id_tecnicoAuxiliar,
        auxiliar,
        idUsuarioSupervisor,
        supervisorACargo,
        sucursal,
        observacion,
        idUsuarioRegistra,
        fechaRegistro,
        e_eliminado
    )
    VALUES (
        ISNULL(@Fecha, CAST(GETDATE() AS DATE)),
        @Estado,
        @Actividad,
        @Id_Tecnico,
        @Cuenta_SF,
        @Salesforce,
        @Habilidad,
        @Vehiculo,
        @Grupo,
        @Almacen,
        @GrupoDigitacion,
        @IdUsuarioDigitador,
        @Digitador,
        @Tecnico,
        @Id_TecnicoAuxiliar,
        @Auxiliar,
        @IdUsuarioSupervisor,
        @SupervisorACargo,
        @Sucursal,
        @Observacion,
        @IdUsuarioRegistra,
        GETDATE(),
        0
    );
END

GO

/* [dbo].[spx_registrarconformacioncuadrillaweb] */
CREATE PROC [dbo].[spx_RegistrarConformacionCuadrillaWeb]
    @Fecha DATE = NULL,
    @Estado NVARCHAR(50),
    @Actividad NVARCHAR(50),
    @Id_Tecnico INT,
    @Cuenta_SF NVARCHAR(100) = NULL,
    @Salesforce NVARCHAR(100) = NULL,
    @Habilidad NVARCHAR(100) = NULL,
    @Vehiculo NVARCHAR(100) = NULL,
    @Grupo NVARCHAR(100) = NULL,
    @Almacen NVARCHAR(100) = NULL,
    @GrupoDigitacion NVARCHAR(100) = NULL,
    @IdUsuarioDigitador INT = NULL,
    @Digitador NVARCHAR(150) = NULL,
    @Tecnico NVARCHAR(150) = NULL,
    @Id_TecnicoAuxiliar INT = NULL,
    @Auxiliar NVARCHAR(150) = NULL,
    @IdUsuarioSupervisor INT,
    @SupervisorACargo NVARCHAR(150) = NULL,
    @Sucursal NVARCHAR(100),
    @Observacion NVARCHAR(500) = NULL,
    @IdUsuarioRegistra INT
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO dbo.tbl_ConformacionCuadrillaDiarioWeb (
        fecha,
        estado,
        actividad,
        id_tecnico,
        cuenta_sf,
        salesforce,
        habilidad,
        vehiculo,
        [grupo],
        almacen,
        grupoDigitacion,
        idUsuarioDigitador,
        digitador,
        tecnico,
        id_tecnicoAuxiliar,
        auxiliar,
        idUsuarioSupervisor,
        supervisorACargo,
        sucursal,
        observacion,
        idUsuarioRegistra,
        fechaRegistro,
        e_eliminado
    )
    VALUES (
        ISNULL(@Fecha, CAST(GETDATE() AS DATE)),
        @Estado,
        @Actividad,
        @Id_Tecnico,
        @Cuenta_SF,
        @Salesforce,
        @Habilidad,
        @Vehiculo,
        @Grupo,
        @Almacen,
        @GrupoDigitacion,
        @IdUsuarioDigitador,
        @Digitador,
        @Tecnico,
        @Id_TecnicoAuxiliar,
        @Auxiliar,
        @IdUsuarioSupervisor,
        @SupervisorACargo,
        @Sucursal,
        @Observacion,
        @IdUsuarioRegistra,
        GETDATE(),
        0
    );

    SELECT CAST(SCOPE_IDENTITY() AS BIGINT) AS id;
END


GO

/* [dbo].[spx_registrarordentrabajo] */
CREATE PROCEDURE dbo.spx_RegistrarOrdenTrabajo
    @Id_Usuario INT,
    @Id_Ruta INT,
    @Id_TipoServicio INT,
    @CodigoCliente INT = NULL,
    @Id_Estado INT = NULL,
    @Observacion NVARCHAR(255) = NULL,
    @TieneObservacion BIT = 0,
    @Id_Sucursal INT = NULL,
    @NombreCliente NVARCHAR(200) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    IF @Id_Usuario IS NULL OR @Id_Ruta IS NULL OR @Id_TipoServicio IS NULL
    BEGIN
        RAISERROR('Parametros requeridos faltantes.', 16, 1);
        RETURN;
    END

    IF @TieneObservacion = 1 AND ( @Observacion IS NULL OR LTRIM(RTRIM(@Observacion)) = '' )
    BEGIN
        RAISERROR('Observacion requerida cuando TieneObservacion=1.', 16, 1);
        RETURN;
    END

    DECLARE @Id_Vendedor INT;
    SELECT @Id_Vendedor = Id_Vendedor
    FROM dbo.tbl_Ruta
    WHERE Id_Ruta = @Id_Ruta AND E_Eliminado = 0;

    IF @Id_Vendedor IS NULL
    BEGIN
        RAISERROR('Ruta no valida.', 16, 1);
        RETURN;
    END

    DECLARE @OrdenTrabajo INT;

    BEGIN TRAN;
        -- Bloqueo pesimista: evita que dos sesiones tomen el mismo OrdenTrabajo
        SELECT @OrdenTrabajo = ISNULL(MAX(OrdenTrabajo), 0) + 1
        FROM dbo.tbl_Venta WITH (TABLOCKX, HOLDLOCK);

        INSERT INTO dbo.tbl_Venta (
            Id_Usuario,
            Id_Vendedor,
            Id_Ruta,
            Id_TipoServicio,
            Fecha_Ejecucion,
            Fecha_Registro,
            OrdenTrabajo,
            Observacion,
            Total,
            Id_UsuarioE,
            E_Eliminado,
            Nombre,
            Id_Estado,
            Id_Sucursal,
            CodigoCliente,
            TieneObservacion
        )
        VALUES (
            @Id_Usuario,
            @Id_Vendedor,
            @Id_Ruta,
            @Id_TipoServicio,
            GETDATE(),
            GETDATE(),
            @OrdenTrabajo,
            @Observacion,
            NULL,
            NULL,
            0,
            @NombreCliente,
            @Id_Estado,
            @Id_Sucursal,
            @CodigoCliente,
            @TieneObservacion
        );
    COMMIT TRAN;

    SELECT CAST(SCOPE_IDENTITY() AS INT) AS Id_Venta, @OrdenTrabajo AS OrdenTrabajo;
END

GO

/* [dbo].[spx_RegistrarProductosSaldos] */

create proc [dbo].[spx_RegistrarProductosSaldos] (@accion int,@Id_Ruta int)
as
--1 cierre de productos nuevos
if(@accion = 1)
begin
	insert into tbl_productosSaldos
	select *,1,'tbl_CierreAlmacen',getdate(),'Nuevo' 
	from tbl_productos where e_eliminado=0 and id_estadoproducto in (1)
	
	insert into tbl_productosSaldos
	select *,1,'tbl_CierreAlmacen',getdate(),'Excedente' 
	from tbl_productos where e_eliminado=0 and id_estadoproducto in (6)
end
--2 cierre de productos Retirados
if(@accion = 2)
begin
	insert into tbl_productosSaldos
	select *,1,'tbl_CierreAlmacenPR_PD',getdate(),'Retirados' 
	from tbl_productos where e_eliminado=0 and id_estadoproducto in (4,14)
	
	insert into tbl_productosSaldos
	select *,1,'tbl_CierreAlmacenPR_PD',getdate(),'Dañado' 
	from tbl_productos where e_eliminado=0 and id_estadoproducto in (5,13)
end
--saldo ruta cuadre
if(@accion = 3)
begin
	insert into tbl_productosSaldos
	select *,1,'tbl_Cuadre',getdate(),'SaldoRuta' 
	from tbl_productos where e_eliminado=0 and id_estadoproducto in (2) and id_ruta = @Id_Ruta
	
	insert into tbl_productosSaldos
		select  0,Cod_Inicio,ChipId,0,v.Id_Ruta,cv.Id_Producto,cv.E_Eliminado,Fecha_Ejecucion,cv.Cantidad,'CodigoVenta',v.Fecha_Ejecucion,'Vendido'
		from tbl_venta v inner join tbl_codigoventa cv on cv.id_venta=v.id_venta 
		where v.id_Ruta = @Id_Ruta and dbo.dateonly(Fecha_Ejecucion)=getdate() and Id_TipoMaterial=1

end



GO

/* [dbo].[spx_registrarventapararegistrootwb] */
CREATE PROCEDURE [dbo].[spx_RegistrarVentaParaRegistroOTwb]
    @Id_Usuario INT,
    @Id_Vendedor INT,
    @Id_Grupo INT,
    @Id_TipoServicio INT,
    @OrdenTrabajo INT,
    @Observacion NVARCHAR(MAX) = NULL,
    @Total DECIMAL(18,2) = 0,
    @Id_UsuarioE INT = NULL,
    @E_Eliminado BIT = 0,
    @Nombre NVARCHAR(250) = NULL,
    @Origen NVARCHAR(100),            -- NUEVO
    @Id_Estado INT,
    @Id_Sucursal INT,
    @CodigoCliente INT,
    @TieneObservacion BIT = 0,
    @Latitud DECIMAL(9,6) = NULL,
    @Longitud DECIMAL(9,6) = NULL
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        IF @Origen IS NULL OR LTRIM(RTRIM(@Origen)) = ''
        BEGIN
            RAISERROR('Origen es requerido.',16,1);
            RETURN;
        END

        IF COL_LENGTH('dbo.tbl_venta', 'Origen') IS NULL
        BEGIN
            RAISERROR('La columna Origen no existe en dbo.tbl_venta.',16,1);
            RETURN;
        END

        BEGIN TRANSACTION;

        INSERT INTO dbo.tbl_venta (
            Id_Usuario,
            Id_Vendedor,
            Id_Ruta,
            Id_TipoServicio,
            Fecha_Ejecucion,
            Fecha_Registro,
            OrdenTrabajo,
            Observacion,
            Total,
            Id_UsuarioE,
            E_Eliminado,
            Nombre,
            Origen,                     -- NUEVO
            Id_Estado,
            Id_Sucursal,
            CodigoCliente,
            TieneObservacion,
            Latitud,
            Longitud
        )
        VALUES (
            @Id_Usuario,
            @Id_Vendedor,
            @Id_Grupo,
            @Id_TipoServicio,
            GETDATE(),
            GETDATE(),
            @OrdenTrabajo,
            @Observacion,
            ISNULL(@Total,0),
            @Id_UsuarioE,
            ISNULL(@E_Eliminado,0),
            @Nombre,
            @Origen,                    -- NUEVO
            @Id_Estado,
            @Id_Sucursal,
            @CodigoCliente,
            ISNULL(@TieneObservacion,0),
            @Latitud,
            @Longitud
        );

        DECLARE @Id_Venta INT;
        SET @Id_Venta = CAST(SCOPE_IDENTITY() AS INT);

        COMMIT TRANSACTION;

        SELECT
            @Id_Venta AS Id_Venta,
            @OrdenTrabajo AS OrdenTrabajo,
            @CodigoCliente AS CodigoCliente,
            @Id_Sucursal AS Id_Sucursal,
            @Origen AS Origen,
            @Latitud AS Latitud,
            @Longitud AS Longitud;
    END TRY
    BEGIN CATCH
        IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
        DECLARE @ErrMsg NVARCHAR(4000);
        SET @ErrMsg = ERROR_MESSAGE();
        RAISERROR(@ErrMsg,16,1);
    END CATCH
END;

GO

/* [dbo].[spx_RegMod_Productos] */
CREATE proc [dbo].[spx_RegMod_Productos](@Serie nvarchar(150),@ChipID nvarchar(150),@Id_Ruta int,@Id_Producto int,
@Accion int,@codigoTabla int,@Id_Usuario int,@MaterialInsRetirado int,@FechaTransaccion datetime)
as 
--1registrar
declare @cuantos int
declare @idestadoProducto int
declare @idTipoDevolucion int
declare @OrdenTrabajo int
declare @idBitacora int
declare @idRuta int
declare @fechaAnteriorEliminacion datetime

	if(@Accion =1)
	begin
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
		if(@cuantos>0)
		begin
			update tbl_productos  
			set id_EstadoProducto=1,id_Ruta=0,id_producto= @Id_Producto,FechaTransaccion=getdate()
			where serial=@Serie and chipid=@chipid
		end
		else
		begin  
			insert into tbl_productos values(@Serie,@ChipID,1,0,@Id_Producto,0,GETDATE())
		end
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_IngresoAlmacen',@FechaTransaccion,GETDATE(),@Id_Ruta,@Id_Usuario,0, 'Ingreso Almacen', 1)
	end
	if(@Accion =2)--entregar al vendedor
	begin
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
		if(@cuantos>0)
		begin
			update tbl_productos  
			set id_EstadoProducto=2,id_Ruta=@Id_Ruta,id_producto= @Id_Producto,FechaTransaccion=getdate()
			where serial=@Serie and chipid=@chipid
		end
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_AlmacenVendedor',@FechaTransaccion,GETDATE(),@Id_Ruta,@Id_Usuario,0, 'Pedido', 2)
	end 
	if(@Accion =3)--ordern trabajo realizada
	begin 
		if(@MaterialInsRetirado in (1,3,4))--Id_TipoMaterial
		begin
			--si es instalado cambia el estado
			set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
			if(@cuantos>0)
			begin
				update tbl_productos  
				set id_EstadoProducto=3,id_Ruta=@Id_Ruta,id_producto= @Id_Producto,FechaTransaccion=getdate()
				where serial=@Serie and chipid=@chipid
				set @idestadoProducto = 3				
			end 
		end
		else
		begin--idtipomaterial ==2 retirado
			set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
			if(@cuantos>0)
			begin
				update tbl_productos  
				set id_EstadoProducto=4,id_Ruta=@Id_Ruta,id_producto= @Id_Producto,FechaTransaccion=getdate()
				where serial=@Serie	and chipid=@chipid
			end
			else
			begin  --select * from tbl_productos
				insert into tbl_productos values(@Serie,@ChipID,4,@Id_Ruta,@Id_Producto,0,getdate())
			end
			set @idestadoProducto = 4			
		end  
		set @OrdenTrabajo = (select OrdenTrabajo from tbl_Venta where Id_Venta = @codigoTabla and e_eliminado = 0);
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_Venta',@FechaTransaccion,GETDATE(),@Id_Ruta,@Id_Usuario,0, 'Orden de Trabajo-' + cast(@OrdenTrabajo as varchar), @idestadoProducto)
	end	
	if(@Accion = 4)--devolucion x retiro esto queda registrado en la OT
	begin --se marca en almacen para entregar a otra ruta
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_Devolucion',@FechaTransaccion,GETDATE(),@Id_Ruta,@Id_Usuario,0, 'Devolución x Retiro', 4)
	end

	if(@Accion = 5)--devolucion x excedente o dañado ( tipoDevolucion = 1 (Dañado), 3 (Excedente)
	begin 
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0 )
		if(@cuantos>0)
		begin
			set @idTipoDevolucion = ( select Id_TipoDevolucion from tbl_Devolucion WHERE Id_Devolucion = @codigoTabla  and e_eliminado = 0)			
			if ( @idTipoDevolucion = 1 )
			begin		
				set @idestadoProducto = 5 -- dañado							
			end
			if ( @idTipoDevolucion = 3 )
			begin
				set @idestadoProducto = 6 -- excedente				
			end
			
			update tbl_productos  
			set id_EstadoProducto = @idestadoProducto, id_Ruta = 0,id_producto= @Id_Producto,FechaTransaccion=getdate()
			where serial=@Serie and chipid=@chipid
		end
		if(@idestadoProducto=5)--dañado
			insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_Devolucion',@FechaTransaccion,GETDATE(),0,@Id_Usuario,0, 'Devolución Dañado', @idestadoProducto)		
		if(@idestadoProducto=6)--excedente
			insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_Devolucion',@FechaTransaccion,GETDATE(),0,@Id_Usuario,0, 'Devolución x Excedente', @idestadoProducto)		
			
	end	 --SELECT * FROM tbl_EstadoProducto 

	if(@Accion = 6)--DEVOLUCION A TIGO PRODUCTOS RETIRADOS
	begin
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
		if(@cuantos>0)
		begin
			update tbl_productos  
			set id_EstadoProducto=15,id_Ruta=0,id_producto= @Id_Producto,FechaTransaccion=getdate()
			where serial=@Serie and chipid=@chipid
		end
		
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_Devolucion',@FechaTransaccion,GETDATE(),0,@Id_Usuario,0, 'Devuelto x Excedente en almacen', 7)		
	end
--select * from tbl_EStadoproducto -- 5 dañado   4 retirado
	if(@Accion = 40)--PRODUCTOS RETIRADOS DE OT
	begin	
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
		if(@cuantos>0)
		begin
			update tbl_productos  
			set id_EstadoProducto=15,id_Ruta=0,id_producto= @Id_Producto,FechaTransaccion=getdate()
			where serial=@Serie and chipid=@chipid
		end
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_Devolucion',@FechaTransaccion,GETDATE(),0,@Id_Usuario,0, 'Devuelto a Tigo PRetirado', 15)		
	end

	if(@Accion = 38)--devolucion a Tigo  _ PRODUCTOS DAÑADOS
	begin
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
		if(@cuantos>0)
		begin
			update tbl_productos  
			set id_EstadoProducto=16,id_Ruta=0,id_producto= @Id_Producto,FechaTransaccion=getdate()
			where serial=@Serie and chipid=@chipid
		end
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_Devolucion',@FechaTransaccion,GETDATE(),0,@Id_Usuario,0, 'Devuelto a Tigo PDañado', 16)		
	end
	if(@Accion = 7)--Baja Productos
	begin
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
		if(@cuantos>0)
		begin
			declare @estado int 
			set @estado=(select id_estadoproducto from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
			if(@estado!=4)
			begin
					if(@estado=1 or @estado=2 or @estado=5 or @estado=6 or @estado=10)
					begin
						update tbl_productos  
						set id_EstadoProducto=8,id_Ruta=0,id_producto= @Id_Producto,FechaTransaccion=getdate()
						where serial=@Serie and chipid=@chipid
						
						insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_BajaProducto',@FechaTransaccion,GETDATE(),0,@Id_Usuario,0, 'Baja Producto', 8)
					end
			end
			else
			begin			
				-- actualizamos estado del producto.			
				update tbl_productos  
				set id_EstadoProducto=8,id_Ruta=0,id_producto= @Id_Producto,FechaTransaccion=getdate()
				where serial=@Serie and chipid=@chipid
				insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_BajaProducto',@FechaTransaccion,GETDATE(),0,@Id_Usuario,0, 'Baja Producto', 8)
			end 			
		end
	end
	if(@Accion = 8)--Baja Productos PERDIDO MATERIAL RETIRADO NO ENTREGADO
	begin
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
		if(@cuantos>0)
		begin
			--declare @estado int 
			set @estado=(select id_estadoproducto from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
			
			if(@estado=11)
			begin					
						update tbl_productos  
						set id_EstadoProducto=8,id_Ruta=@Id_Ruta,id_producto= @Id_Producto,FechaTransaccion=getdate()
						where serial=@Serie and chipid=@chipid
			
						update tbl_DetalleDevolucion set PendienteRecojo=0 where Cod_Inicio=@Serie and chipid=@chipid			
						insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_BajaProducto',@FechaTransaccion,GETDATE(),@Id_Ruta,@Id_Usuario,0, 'Baja Producto', 8)			
			end					
		end 
	end --select top 10 * from tbl_detalledevolucion
	
	if(@Accion = 31) 
	begin -- casos: productos instalados , productos retirados propios, productos retirados terceros.		
	
		set @cuantos = (select COUNT(0) from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0 order by id_bitacora desc ) bitacora)
		if ( @cuantos = 2 ) --E4_11Nuevooentregado
		begin--Aqui borra en bitacora y productos
		
			set @idBitacora = (select id_bitacora from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0 ) bitacora where observacion like 'Orden de Trabajo%')		
			update tbl_bitacora set e_eliminado = 1 where id_bitacora = @idBitacora						
		
			set @cuantos = (select top 1 id_bitacora from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0 order by id_bitacora desc ) bitacora where observacion = 'Devolución x Retiro')
			update tbl_bitacora set e_eliminado = 1 where id_bitacora = @cuantos
	
			
			update tbl_productos set id_EstadoProducto=9,id_Ruta=0, e_eliminado = 1 where serial=@Serie	and chipid=@chipid				
		end
		if ( @cuantos = 3 ) --e3  e4_11  --e8_11  --e4_11_4
		begin
		
				set @cuantos = ( select COUNT(0) from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0 
					order by id_bitacora desc ) bitacora where tabla = 'tbl_Devolucion')
					
				if ( @cuantos = 2 ) --e4_11_4
				begin
					set @idBitacora = (select id_bitacora from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0
								order by id_bitacora desc ) bitacora where idEstadoProducto in (7,15,16))	
								
					set @idestadoProducto = (select idEstadoProducto from tbl_bitacora where id_bitacora= @idBitacora)					
				end
				else 
					begin
					
										
						set @cuantos = (select COUNT(0) from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid  and e_eliminado = 0
										order by id_bitacora desc ) bitacora where tabla = 'tbl_Venta')
						if ( @cuantos = 2 )  --e4_11
						begin
							set @idBitacora = ( select id_bitacora from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0
												order by id_bitacora desc ) bitacora where idEstadoProducto = 3 )
							set @idestadoProducto = 3				
						end
						else 
						if ( @cuantos = 1 ) 
						begin --e3  e8_11 se pierde
					
							set @cuantos = (select COUNT(0) from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid  and e_eliminado = 0
										order by id_bitacora desc ) bitacora where idEstadoProducto in (2))
							if(@cuantos=0)		
							begin
								set @cuantos = (select COUNT(0) from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid  and e_eliminado = 0
										order by id_bitacora desc ) bitacora where idEstadoProducto in (12))
								if(@cuantos=1)
								begin
									set @idBitacora = ( select id_bitacora from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0
												order by id_bitacora desc ) bitacora where idEstadoProducto in (12) )
									set @idestadoProducto = 12	
								end
								else 
								begin
									set @idBitacora = ( select id_bitacora from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0
												order by id_bitacora desc ) bitacora where idEstadoProducto in (8) )
									set @idestadoProducto = 8
								end 
							end
							else 
							begin
							
								set @idBitacora = ( select id_bitacora from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0
												order by id_bitacora desc ) bitacora where idEstadoProducto in (2) )
								set @idestadoProducto = 2		
							end	
						end			
					end
				
				--e3  --e4_11
				set @cuantos = (select top 1 id_bitacora from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0 order by id_bitacora desc ) bitacora where observacion like 'Orden de Trabajo%')
				update tbl_bitacora set e_eliminado = 1 where id_bitacora = @cuantos--e3 elimina la ot en bitacora --e4_11 elimina estado 4 bitacora

				set @cuantos = (select COUNT(0) from ( select top 3 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0 order by id_bitacora desc ) bitacora where observacion = 'Devolución x Retiro')
				if ( @cuantos > 0 ) begin			--e4_11
					set @cuantos = (select top 1 id_bitacora from ( select top 3 * from tbl_bitacora where serial = @Serie and chipid=@chipid and e_eliminado = 0 order by id_bitacora desc ) bitacora where observacion = 'Devolución x Retiro')			
					update tbl_bitacora set e_eliminado = 1 where id_bitacora = @cuantos
				end 
				set @idRuta = ( select id_Ruta from tbl_bitacora where id_bitacora = @idBitacora and e_eliminado = 0 );
				set @fechaAnteriorEliminacion = ( select fecharegistro from tbl_bitacora where id_bitacora = @idBitacora and e_eliminado = 0 );
				
				update tbl_productos set id_EstadoProducto = @idestadoProducto, id_Ruta = @idRuta,fechatransaccion=@fechaAnteriorEliminacion where serial = @Serie and chipid=@chipid
		end	
		-----------------------------------------------------------------------------------------------------------------------------------
		-----------------------------------------------------------------------------------------------------------------------------------
		
		
	end
	
--	spx_RegMod_Productos 'TIG-1430-008490','00569616653', 0, 1, 32, 0, 1, 0, '30/03/2022 10:48:14'
	if(@Accion = 32) 
	begin -- casos: productos instalados , productos retirados propios, productos retirados terceros.		
		

		set @cuantos = (select COUNT(0) from ( select top 3 * from tbl_bitacora where serial = @Serie and chipid=@chipid and e_eliminado = 0 order by id_bitacora desc ) bitacora )	
		if(@cuantos=1)--se creo el prdocuto de  0
		begin			
			set @cuantos = (select top 1 id_bitacora from ( select top 1 * from tbl_bitacora where serial = @Serie and chipid=@chipid and e_eliminado = 0 order by id_bitacora desc ) bitacora where observacion = 'CodigoVentaCargoUsuario')
			update tbl_bitacora set e_eliminado = 1 where id_bitacora = @cuantos
			
			update tbl_productos set id_EstadoProducto=19,id_Ruta=0, e_eliminado = 1 where serial=@Serie and chipid=@chipid	
		end
		if ( @cuantos = 3 ) --c1 inst-sa-ing hay mnuchas transacciones
		begin
			set @cuantos = (select COUNT(0) from ( select top 2 * from tbl_bitacora where serial = @Serie and chipid=@chipid and e_eliminado = 0 order by id_bitacora desc ) bitacora where observacion like 'Devuelto a Tig%' )
			if(@cuantos=1)--entra x devolucion
			begin
				set @cuantos =  ( select id_bitacora from ( select top 2 * from tbl_bitacora where serial = @Serie and chipid=@chipid and e_eliminado = 0
												order by id_bitacora desc ) bitacora where idEstadoProducto  in(7,15,16) )
												
				set @idestadoProducto = ( select idestadoproducto from ( select top 2 * from tbl_bitacora where serial = @Serie and chipid=@chipid and e_eliminado = 0
												order by id_bitacora desc ) bitacora where idEstadoProducto  in(7,15,16) )
				
				set @fechaAnteriorEliminacion = ( select fecharegistro from ( select top 2 * from tbl_bitacora where serial = @Serie and chipid=@chipid and e_eliminado = 0
												order by id_bitacora desc ) bitacora where idEstadoProducto  in(7,15,16) )
												
				update tbl_productos set id_ruta=0, id_estadoproducto=@idestadoProducto,fechatransaccion=@fechaAnteriorEliminacion where serial=@serie and chipid=@chipid
				
			end
			set @cuantos = (select COUNT(0) from ( select top 2 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0 order by id_bitacora desc ) bitacora where observacion like 'Orden de Trabajo%' )
			if(@cuantos=1)
			begin
				set @cuantos =  ( select id_bitacora from ( select top 2 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0
												order by id_bitacora desc ) bitacora where idEstadoProducto  in(3) )
												
				set @idestadoProducto = ( select id_ruta from ( select top 2 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0
												order by id_bitacora desc ) bitacora where idEstadoProducto  in(3) )
												
				set @fechaAnteriorEliminacion = ( select fecharegistro from ( select top 2 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0
												order by id_bitacora desc ) bitacora where idEstadoProducto  in(3) )
												
				update tbl_productos set id_ruta=@idestadoProducto, id_estadoproducto=3,fechatransaccion=@fechaAnteriorEliminacion where serial=@serie and chipid=@chipid
				
			end
			
			set @cuantos = (select top 1 id_bitacora from ( select top 1 * from tbl_bitacora where serial = @serie and chipid=@chipid and e_eliminado = 0 order by id_bitacora desc ) bitacora where observacion = 'CodigoVentaCargoUsuario')
			update tbl_bitacora set e_eliminado = 1 where id_bitacora = @cuantos	
			
		end				
	end
	

	if(@Accion =35)--ordern trabajo realizada productos no entregados solo deberian registrar los retirados
	begin 
		if(@MaterialInsRetirado not in (1,3,4))--Id_TipoMaterial
		begin--idtipomaterial ==2 retirado
			set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
			if(@cuantos>0)
			begin
				update tbl_productos  
				set id_EstadoProducto=11,id_Ruta=@Id_Ruta,id_producto= @Id_Producto,FechaTransaccion=getdate()
				where serial=@Serie and chipid=@chipid
			end
			else
			begin  --select * from tbl_productos
				insert into tbl_productos values(@Serie,@ChipID,11,@Id_Ruta,@Id_Producto,0,getdate())
			end
			set @idestadoProducto = 11
		end  
		--se quito esta fila no entiendo que hace
		--set @OrdenTrabajo = (select isnull(NroOrdenTrabajo,'-') from tbl_Devolucion where Id_Devolucion = @codigoTabla and e_eliminado = 0);
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_Devolucion',@FechaTransaccion,GETDATE(),@Id_Ruta,@Id_Usuario,0, 'Devolución x Retiro', @idestadoProducto)
	end
	if(@Accion = 36)--devolucion de un material no entregado
	begin 
		update tbl_productos 
		set id_EstadoProducto = 4,FechaTransaccion=getdate()
		where serial=@Serie and chipid=@chipid
		
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_Devolucion',@FechaTransaccion,GETDATE(),@Id_Ruta,@Id_Usuario,0, 'Devolución x Retiro Anterior', 4)
	end
	
	if(@Accion =37)--traspaso
	begin
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@chipid and e_eliminado = 0)
		if(@cuantos>0)
		begin
			update tbl_productos  
			set id_EstadoProducto=12,id_Ruta=@Id_Ruta,id_producto= @Id_Producto,FechaTransaccion=getdate()
			where serial=@Serie and chipid=@chipid
		end
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_SalidaTraspaso',@FechaTransaccion,GETDATE(),@Id_Ruta,@Id_Usuario,0, 'Salida Traspaso', 12)
	end 

	if(@Accion =41)--REGISTRO CARGO USUARIO
	begin--se modifica el estado		
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@ChipID and id_producto=@id_producto and e_eliminado = 0)
		if(@cuantos<=0)		
		begin  
			insert into tbl_productos values(@Serie,@ChipID,17,@Id_Ruta,@Id_Producto,0,getdate())
		end				
			update tbl_productos  
			set id_EstadoProducto=17,id_Ruta=@Id_Ruta,id_producto= @Id_Producto,FechaTransaccion=getdate()
			where serial=@Serie	and chipid=@ChipID and id_producto=@id_producto	
				
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_CodigoVentaCargoUsuario',@FechaTransaccion,GETDATE(),@Id_Ruta,@Id_Usuario,0, 'CodigoVentaCargoUsuario', 17)
	end 	
	
	if(@Accion =43)--REGISTRO CARGO USUARIO NO REALIZADO
	begin--se modifica el estado		
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@ChipID and id_producto=@id_producto and e_eliminado = 0)
		if(@cuantos<=0)		
		begin  
		--spx_RegMod_Productos '5454879315444444','555 5555 4444',0,37,43,4,1,0,'08/01/2025 15:23:19'
--		select * from tbl_productos where serial='5454879315444444'
--select * from tbl_bitacora where serial='5454879315444444'
--select * from tbl_cargousuarionorealizado
--select * from tbl_codigocargousuarionorealizado
	--select  5
			insert into tbl_productos values(@Serie,@ChipID,22,0,@Id_Producto,0,getdate())
		end				
			update tbl_productos  
			set id_EstadoProducto=22,id_producto= @Id_Producto,FechaTransaccion=getdate()
			where serial=@Serie	and chipid=@ChipID and id_producto=@id_producto	
				
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_CodigoCargoUsuarioNoRealizado',@FechaTransaccion,GETDATE(),0,@Id_Usuario,0, 'CodigoCargoUsuarioNoRealizado', 22)
	end 
	
	
	if(@Accion =42)
	begin
		set @cuantos =(select COUNT(*) from tbl_productos where serial=@Serie and chipid=@ChipId  and id_producto=@Id_Producto and e_eliminado = 0)
		if(@cuantos>0)
		begin
			update tbl_productos  
			set id_EstadoProducto=18,id_Ruta=0,FechaTransaccion=getdate()
			where serial=@Serie and chipid=@ChipId and id_producto=@Id_Producto
		end
		else
		begin  
			insert into tbl_productos values(@Serie,@ChipID,18,0,@Id_Producto,0,getdate())
		end
		insert into tbl_bitacora values(@Id_Producto,@Serie,@ChipID,@codigoTabla,'tbl_IngresoProductosE18',@FechaTransaccion,GETDATE(),@Id_Ruta,@Id_Usuario,0, 'Ingreso Almacen E18', 18)
	end





GO

/* [dbo].[spx_rutasCuadre] */
CREATE proc [dbo].[spx_rutasCuadre]( @fecha datetime )
as
	declare @tablaRutaCuadre table ( idRuta int, idCuadre int, cantPedido int, cantOT int, cantDevolucion int) 
	-- idRuta | idCuadre
	insert into @tablaRutaCuadre select tr.Id_Ruta, ISNULL ( tc.Id_Cuadre, -1), 0, 0, 0
			from tbl_Ruta tr left join tbl_Cuadre tc on tc.Id_Ruta = tr.Id_Ruta 
			and tr.E_Eliminado = 0 and tc.E_Eliminado = 0  and dbo.DateOnly( Fecha ) = dbo.DateOnly (@fecha)
			where tr.Id_Ruta not in (0)


--select * from @tablaRutaCuadre
	-- Pedidos por ruta
	update @tablaRutaCuadre		
	set cantPedido = ( select count(0) from tbl_PedidoVendedor tpv
							where tpv.id_Ruta =  idRuta and tpv.E_Eliminado = 0
							and dbo.DateOnly( tpv.Fecha ) = dbo.DateOnly (@fecha) )		
		
	-- OTs por ruta.
	update @tablaRutaCuadre		
	set cantOT = ( select count(0) from tbl_Venta tv
							where tv.id_Ruta =  idRuta and tv.E_Eliminado = 0
							and dbo.DateOnly( tv.Fecha_Ejecucion ) = dbo.DateOnly (@fecha) )	
	
	
	-- Devolucion
	update @tablaRutaCuadre		
	set cantDevolucion = ( select count(0) from tbl_Devolucion td
							where td.id_Ruta =  idRuta and Estado = 0 and td.E_Eliminado = 0
							and dbo.DateOnly( td.Fecha ) = dbo.DateOnly(@fecha) )
		
	select * from @tablaRutaCuadre
	where idCuadre = -1 and ( cantPedido > 0 or cantOT > 0 or cantDevolucion > 0)



--select * from tbl_Ruta 

--[spx_rutasCuadre] '15/10/2020'
GO

/* [dbo].[spx_SePuedeBorrarIngresoMaterialTigo] */
create proc spx_SePuedeBorrarIngresoMaterialTigo(@Id_IngresoMaterialTigo int)
as 
begin 
	declare @contador int,@respuesta nvarchar(25)
	set @respuesta = 'NoSePuede'
	set @contador = (select COUNT(*) from tbl_IngresoMaterialTigo_Almacen where id_IngresoMaterialTigo=@Id_IngresoMaterialTigo)
	declare @estadoIngresoCompleto bit
	set @estadoIngresoCompleto =(select estadoingresocompleto from tbl_IngresoMaterialTigo where id_IngresoMaterialTigo = @Id_IngresoMaterialTigo)
	if(@contador=0 and @estadoIngresoCompleto=0)
	begin
		set @respuesta ='SePuede'
	end
	select @respuesta 
end 
	
GO

/* [dbo].[spx_SePuedeCrearCuentaSalesforce] */
create proc [dbo].[spx_SePuedeCrearCuentaSalesforce](@cuenta_salesforce nvarchar(150))
as
declare @sePuede nvarchar(150)
set @sePuede ='NoSePuede'
declare @cuantos int
set @cuantos = (select count(id) from tbl_salesforce where cuenta_sf = @cuenta_salesforce)
if(@cuantos<=0)
begin
	set @sePuede='SePuede'
end
select @sePuede

GO

/* [dbo].[spx_SePuedeCrearGrupo] */
CREATE proc [dbo].[spx_SePuedeCrearGrupo](@nombre nvarchar(150))
as
declare @sePuede nvarchar(150)
set @sePuede ='NoSePuede'
declare @cuantos int
set @cuantos = (select count(id_ruta) from tbl_ruta where nombre = @nombre)
if(@cuantos<=0)
begin
	set @sePuede='SePuede'
end
select @sePuede
GO

/* [dbo].[spx_SePuedeCrearSalesforce] */
create proc [dbo].[spx_SePuedeCrearSalesforce](@salesforce nvarchar(150))
as
declare @sePuede nvarchar(150)
set @sePuede ='NoSePuede'
declare @cuantos int
set @cuantos = (select count(id) from tbl_salesforce where salesforce = @salesforce)
if(@cuantos<=0)
begin
	set @sePuede='SePuede'
end
select @sePuede

GO

/* [dbo].[spx_SePuedeCrearVehiculo] */

create proc [dbo].[spx_SePuedeCrearVehiculo](@placa nvarchar(150))
as
declare @sePuede nvarchar(150)
set @sePuede ='NoSePuede'
declare @cuantos int
set @cuantos = (select count(id) from tbl_placavehiculo where placa = @placa)
if(@cuantos<=0)
begin
	set @sePuede='SePuede'
end
select @sePuede

GO

/* [dbo].[spx_SePuedeEliminarProducto] */
CREATE proc [dbo].[spx_SePuedeEliminarProducto](@IdProducto int)
as
declare @respuesta nvarchar(25)
set @respuesta ='NoSePuede'
declare @tableRespuesta table(SaldoRuta int, SaldoProductos int, SaldoPendienteEntregaRetirados int, SaldoMovimientos int)


declare @fechaInicio datetime
declare @fechaFin datetime
set @fechaInicio  = getdate();
set @fechaFin =  DATEADD(day, -60, @fechainicio);

declare @cantidadSaldoRuta int
set @cantidadSaldoRuta = (SELECT count(*) FROM TBL_SALDOTARJETAS WHERE ID_PRODUCTO IN (@IdProducto) AND CANTIDAD>0)
--select @cantidadSaldoRuta 


declare @cantidadSaldoProductos int
set @cantidadSaldoProductos = (SELECT count(*) FROM tbl_productos WHERE ID_PRODUCTO IN (@IdProducto) and e_eliminado=0)
--select @cantidadSaldoProductos 

declare @cantidadSaldoPendienteEntregaRetirados int
set @cantidadSaldoPendienteEntregaRetirados  = (select count(*)
					from tbl_DetalleDevolucion dd inner join tbl_Devolucion dev on dev.Id_Devolucion = dd.Id_Devolucion
					inner join tbl_producto pr on pr.Id_Producto = dd.Id_Producto
					and pr.Id_Producto=@IdProducto
					and dd.PendienteRecojo=1
					and dd.E_Eliminado=0 and dev.E_Eliminado=0	)


declare @tableMovimientos table(id_Ruta int, Nombre nvarchar(250),TipoMovimiento nvarchar(150),cantidad int)
insert into @tableMovimientos 
			select v.Id_Ruta,r.Nombre, 'PEDIDOVENDEDOR' TipoMovimiento,count(v.Id_PedidoVendedor) Cantidad 
				from tbl_pedidovendedor v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				inner join tbl_codigopedidovendedor cpv on cpv.Id_pedidovendedor=v.Id_pedidovendedor
				where dbo.dateonly(fecha) between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and cpv.Id_producto=@IdProducto
				group by v.Id_Ruta,r.Nombre
			Union ALL				
				select  r.Id_Ruta,r.Nombre, 'ALMACENVENDEDOR' TipoMovimiento,count(v.Id_AlmacenVendedor) Cantidad 				
				from tbl_almacenvendedor v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				inner join tbl_codigoalmacenvendedor cpv on cpv.Id_almacenvendedor=v.Id_almacenvendedor
				where dbo.dateonly(fecha) between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and cpv.Id_producto=@IdProducto
				group by r.Id_Ruta,r.Nombre
				
			Union ALL			
				select  r.Id_Ruta,r.Nombre, 'OT' TipoMovimiento,count(v.Id_Venta) Cantidad 				
				from tbl_Venta v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				inner join tbl_codigoventa cpv on cpv.Id_venta=v.Id_venta
				where dbo.dateonly(fecha_ejecucion) between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and cpv.Id_producto=@IdProducto
				group by r.Id_Ruta,r.Nombre
			Union ALL			
				select  r.Id_Ruta,r.Nombre, 'DEVOLUCION' TipoMovimiento,count(v.Id_Devolucion) Cantidad 				
				from tbl_Devolucion v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				inner join tbl_codigodevolucion cpv on cpv.Id_devolucion=v.Id_devolucion
				where dbo.dateonly(fecha)  between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and cpv.Id_producto=@IdProducto
				group by r.Id_Ruta,r.Nombre
			Union ALL			
				select  r.Id_Ruta,r.Nombre, 'BAJA_PRODUCTOS' TipoMovimiento,count(v.id_BajaProductos) Cantidad 								
				from tbl_BajaProductos v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				inner join tbl_codigobajaproductos cpv on cpv.Id_bajaproductos=v.Id_bajaproductos
				where dbo.dateonly(fecha)between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and cpv.Id_producto=@IdProducto
				group by r.Id_Ruta,r.Nombre
			Union all				
				select  r.Id_Ruta,r.Nombre, 'CUADRE' TipoMovimiento,count(v.Id_Cuadre) Cantidad 												
				from tbl_Cuadre v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				inner join tbl_codigocuadre cpv on cpv.Id_cuadre=v.Id_cuadre
				where dbo.dateonly(fecha) between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and cpv.Id_producto=@IdProducto
				group by r.Id_Ruta,r.Nombre
			Union ALL			
				select  r.Id_Ruta,r.Nombre, 'SALIDA_TRASPASOS' TipoMovimiento,count(v.Id_SalidaTraspaso) Cantidad 																
				from tbl_SalidaTraspaso v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				inner join tbl_codigosalidatraspaso cpv on cpv.Id_salidatraspaso=v.Id_salidatraspaso
				where dbo.dateonly(fecha)  between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and cpv.Id_producto=@IdProducto
				group by r.Id_Ruta,r.Nombre
		
		
		
declare @cantidadSaldoMovimientos int
set @cantidadSaldoMovimientos = (SELECT count(*) FROM @tableMovimientos where cantidad>0)
--select @cantidadSaldoMovimientos


insert into @tableRespuesta 
select @cantidadSaldoRuta , @cantidadSaldoProductos , @cantidadSaldoPendienteEntregaRetirados , @cantidadSaldoMovimientos

--select @respuesta
--select * from @tableRespuesta 
if((@cantidadSaldoRuta + @cantidadSaldoProductos + @cantidadSaldoPendienteEntregaRetirados + @cantidadSaldoMovimientos)<=0)
	set @respuesta ='SePuede'	


select @respuesta
select * from  @tableRespuesta
GO

/* [dbo].[spx_SePuedeEliminarRuta] */
CREATE proc [dbo].[spx_SePuedeEliminarRuta](@Idruta int)
as
--declare @Idruta int
declare @fechaInicio datetime
declare @fechaFin datetime
set @fechaInicio =  DATEADD(day, -3, getdate()) 
set @fechaFin = (select getdate())
--set @Idruta  =5

				select r.Id_Ruta,r.Nombre, 'PEDIDOVENDEDOR' TipoMovimiento,count(Id_PedidoVendedor) Cantidad 
				from tbl_pedidovendedor v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and r.Id_Ruta =@Idruta
				group by r.Id_Ruta,r.Nombre
			Union ALL				
				select  r.Id_Ruta,r.Nombre, 'ALMACENVENDEDOR' TipoMovimiento,count(Id_AlmacenVendedor) Cantidad 
				
				from tbl_almacenvendedor v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and r.Id_Ruta =@Idruta
				group by r.Id_Ruta,r.Nombre
				
			Union ALL			
				select  r.Id_Ruta,r.Nombre, 'OT' TipoMovimiento,count(Id_Venta) Cantidad 				
				from tbl_Venta v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha_ejecucion)  between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and r.Id_Ruta =@Idruta
				group by r.Id_Ruta,r.Nombre
			Union ALL			
				select  r.Id_Ruta,r.Nombre, 'DEVOLUCION' TipoMovimiento,count(Id_Devolucion) Cantidad 				
				from tbl_Devolucion v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha)  between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and r.Id_Ruta =@Idruta
				group by r.Id_Ruta,r.Nombre
			Union ALL			
				select  r.Id_Ruta,r.Nombre, 'BAJA_PRODUCTOS' TipoMovimiento,count(id_BajaProductos) Cantidad 								
				from tbl_BajaProductos v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha)between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and r.Id_Ruta =@Idruta
				group by r.Id_Ruta,r.Nombre
			Union all				
				select  r.Id_Ruta,r.Nombre, 'CUADRE' TipoMovimiento,count(Id_Cuadre) Cantidad 												
				from tbl_Cuadre v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and r.Id_Ruta =@Idruta
				group by r.Id_Ruta,r.Nombre
			Union ALL			
				select  r.Id_Ruta,r.Nombre, 'SALIDA_TRASPASOS' TipoMovimiento,count(Id_SalidaTraspaso) Cantidad 																
				from tbl_SalidaTraspaso v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha)  between dbo.dateonly(@fechaInicio) and dbo.dateonly(@fechaFin) and v.e_eliminado = 0
				and r.Id_Ruta =@Idruta
				group by r.Id_Ruta,r.Nombre
	
	
	
GO

/* [dbo].[spx_SePuedeEliminarTecnico] */
create proc spx_SePuedeEliminarTecnico(@Id_Vendedor int)
as
begin
	declare @SePuedeEliminar nvarchar(max)
	declare @cuantos int
	set @cuantos = (select count(*) from tbl_ruta where id_vendedor= @Id_Vendedor and e_eliminado=0)
	if(@cuantos=0)
		set @SePuedeEliminar ='SePuede';
	else 
		set @SePuedeEliminar ='NoSePuede';
	select 	@SePuedeEliminar 
end

GO

/* [dbo].[spx_SePuedeEliminarVehiculo] */
create proc [dbo].[spx_SePuedeEliminarVehiculo](@vehiculo nvarchar(15))
as
select * from tbl_vendedor where vehiculo=@vehiculo and e_Eliminado=0

GO

/* [dbo].[spx_SePuedeHacerCierreAlmacen] */


create proc [dbo].[spx_SePuedeHacerCierreAlmacen](@Fecha datetime)
as
BEGIN
declare @RespuestaFinal table(respuesta nvarchar(150),Observacion nvarchar(500))
declare @respuesta nvarchar(500),@Observacion nvarchar(500), @diferenciaDias int 
	
declare @cuantostablamov_pendientes int, @hayCierrePRPDPend int
declare @tablamov_pendientes table(movimiento nvarchar(150), cantidad int)
insert into @tablamov_pendientes exec [spx_ValidaMovimientos] @fecha
--select DATEDIFF(DAY,'08/01/2021',GETDATE())
--[spx_ValidaMovimientos] '08/01/2021'
	set @diferenciaDias=(select DATEDIFF(DAY,@fecha,GETDATE()))
	if(@diferenciaDias>=0)
	begin
		set @cuantostablamov_pendientes = (select COUNT(*) from @tablamov_pendientes)
			if(@cuantostablamov_pendientes>0)--hay transaccione pendientes
			begin
				set @respuesta='NoSePuede'
				set @Observacion='Hay transacciones pendientes'
			end
			else---no hay transacciones pendientes
			begin
				set @hayCierrePRPDPend =(select COUNT(*) from tbl_CierreAlmacen where cierrealmacenPR_PD=0 and E_Eliminado =0)
				if(@hayCierrePRPDPend >0)
				begin
					set @respuesta='NoSePuede'
					set @Observacion='Hay un cierre PR_PD pendiente'
				end
				else 
				begin
					set @respuesta='SePuede'	
					set @Observacion='Hay un cierre PR_PD pendiente'
				end
				--se puede
			end
	end
	else 
	begin
		set @cuantostablamov_pendientes = (select COUNT(*) from @tablamov_pendientes)
			if(@cuantostablamov_pendientes>0)--hay transaccione pendientes
			begin
				set @respuesta='NoSePuede'
				set @Observacion='Hay transacciones pendientes'
			end
			else---no hay transacciones pendientes
			begin
				set @hayCierrePRPDPend =(select COUNT(*) from tbl_CierreAlmacen where cierrealmacenPR_PD=0 and E_Eliminado =0)
				if(@hayCierrePRPDPend >0)
				begin
					set @respuesta='NoSePuede'
					set @Observacion='Hay un cierre PR_PD pendiente'
				end
				else 
				begin
					set @respuesta='SePuede'						
				end				
			end
	end
	
	insert into @RespuestaFinal
	select @respuesta, @Observacion
	
	select * from @RespuestaFinal
END


GO

/* [dbo].[spx_SePuedeHacerCierreAlmacenPR_PD] */

create proc [dbo].[spx_SePuedeHacerCierreAlmacenPR_PD](@Fecha datetime)
as
BEGIN
declare @RespuestaFinal table(respuesta nvarchar(150),Observacion nvarchar(500))
declare @respuesta nvarchar(500),@Observacion nvarchar(500), @diferenciaDias int 
	
declare @cuantostablamov_pendientes int, @FechaPendienteCierre datetime
declare @tablamov_pendientes table(movimiento nvarchar(150), cantidad int)
insert into @tablamov_pendientes exec [spx_ValidaMovimientos] @fecha
--[spx_ValidaMovimientos] '08/01/2021'

declare @textopendientes nvarchar(max)
set @textopendientes =  (SELECT STUFF(
							(SELECT ', ' + movimiento
							FROM @tablamov_pendientes        
							FOR XML PATH ('')),
							1,2, ''))



declare @val1 nvarchar(150),@valguardado nvarchar(150),@cantidad2 int
	set @diferenciaDias=(select DATEDIFF(DAY,@fecha,GETDATE()))
	if(@diferenciaDias>=0)
	begin
		set @cuantostablamov_pendientes = (select COUNT(*) from @tablamov_pendientes)
			if(@cuantostablamov_pendientes>0)--hay transaccione pendientes
			begin
				if(@cuantostablamov_pendientes=1)
				begin
					set @val1 = (select  'CIERREALMACEN - '+CONVERT(varchar,dbo.DateOnly(@Fecha),103) )
					set @valguardado = (select movimiento from @tablamov_pendientes )
					set @cantidad2 = (select cantidad from @tablamov_pendientes )
					if(@val1 = @valguardado and  @cantidad2=1)
					begin
						set @FechaPendienteCierre =(select fecha from tbl_CierreAlmacen where cierrealmacenPR_PD=0 and E_Eliminado =0)
						if(dbo.DateOnly(@FechaPendienteCierre)=dbo.DateOnly(@Fecha))
						begin
							set @respuesta='SePuede'					
						end
						else 
						begin
							set @respuesta='NoSePuede'	
							set @Observacion='No hay un cierre pendiente'
						end
					end
				end
				else
				begin
					set @respuesta='NoSePuede'
					set @Observacion='Hay transacciones pendientes \r'+@textopendientes
				end				
			end
			else---no hay transacciones pendientes
			begin
				set @FechaPendienteCierre =(select fecha from tbl_CierreAlmacen where cierrealmacenPR_PD=0 and E_Eliminado =0)
				if(dbo.DateOnly(@FechaPendienteCierre)=dbo.DateOnly(@Fecha))
				begin
					set @respuesta='SePuede'					
				end
				else 
				begin
					set @respuesta='NoSePuede'	
					set @Observacion='No hay un cierre pendiente'
				end
				--se puede
			end
	end
	else 
	begin
		set @cuantostablamov_pendientes = (select COUNT(*) from @tablamov_pendientes)
			if(@cuantostablamov_pendientes>0)--hay transaccione pendientes
			begin
				set @respuesta='NoSePuede'
				set @Observacion='Hay transacciones pendientes'
			end
			else---no hay transacciones pendientes
			begin
				set  @FechaPendienteCierre =(select fecha from tbl_CierreAlmacen where cierrealmacenPR_PD=0 and E_Eliminado =0)
					if(dbo.DateOnly(@FechaPendienteCierre)=dbo.DateOnly(@Fecha))
				begin
					set @respuesta='SePuede'	
					
				end
				else 
				begin
					set @respuesta='NoSePuede'
					set @Observacion='Hay un cierre PR_PD pendiente'				
				end				
			end
	end
	
	insert into @RespuestaFinal
	select @respuesta, @Observacion
	
	select * from @RespuestaFinal
END


GO

/* [dbo].[spx_sepuedemodificarordentrabajo] */
CREATE PROCEDURE [dbo].[spx_sepuedemodificarordentrabajo] AS BEGIN SET NOCOUNT ON; END
GO

/* [dbo].[spx_SePuedeRegistrarCierrePR_PD] */

create proc [dbo].[spx_SePuedeRegistrarCierrePR_PD](@fecha datetime)
as 
Begin	
declare @RespuestaFinal table(respuesta nvarchar(150),Observacion nvarchar(500))
declare @respuesta nvarchar(500)
declare @Observacion nvarchar(500)
declare @CantidadCierre int 
declare @FechaUltimoCierre datetime

	set @FechaUltimoCierre = (select Max(Fecha) as Fecha from tbl_cierrealmacen where e_eliminado = 0)	
	if(dbo.DateOnly(@FechaUltimoCierre) <> dbo.DateOnly(@fecha))
	begin
		set @respuesta ='NoSePuede'		
		set @Observacion='Hay un cierre antes y/o después de la fecha seleccionada'
	end
	else	
	begin
		set @CantidadCierre = (select COUNT(*)from tbl_CierreAlmacenPR_PD ca where dbo.DateOnly(fecha)=dbo.DateOnly(@Fecha) and E_Eliminado =0)
		if(@CantidadCierre = 0)
			set @respuesta ='SePuede'
		else
		begin
			set @respuesta ='NoSePuede'		
			set @Observacion='Existe un cierre'
		end
	end
	
	insert into @RespuestaFinal
	select @respuesta,@Observacion
	
	select * from @RespuestaFinal
end
	
	

GO

/* [dbo].[spx_sp_ObtenerListadoTipoServicioTSTablas] */
Create proc spx_sp_ObtenerListadoTipoServicioTSTablas
as
select * from tbl_TipoServicioTSTablas where e_eliminado=0
GO

/* [dbo].[spx_TipoDevolucion_Dañado] */
create proc [dbo].[spx_TipoDevolucion_Dañado]
as
 select * from tbl_TipoDevolucion where E_Eliminado = 0 and Id_TipoDevolucion in ( 1 )

GO

/* [dbo].[spx_TipoDevolucion_ExcedenteDanado] */
CREATE proc [dbo].[spx_TipoDevolucion_ExcedenteDanado]
as
	select * from tbl_TipoDevolucion where E_Eliminado = 0 and Id_TipoDevolucion in ( 1, 3 )
	
GO

/* [dbo].[spx_TipoDevolucion_ExcRuta] */
create proc [dbo].[spx_TipoDevolucion_ExcRuta]
as
 select * from tbl_TipoDevolucion where E_Eliminado = 0 and Id_TipoDevolucion in ( 3 )
 

GO

/* [dbo].[spx_TraerChipID] */
CREATE proc [dbo].[spx_TraerChipID](@Id_Producto int,@Serie nvarchar(50))
as
begin
	select * from tbl_productos where serial = @Serie and id_producto=@Id_Producto and e_eliminado=0
end



GO

/* [dbo].[spx_TraerChipID2] */



CREATE proc [dbo].[spx_TraerChipID2](@Serie nvarchar(50))
as
begin
	select ps.*,pr.nombre NombreProducto , e.Nombre EstadoProducto
	from tbl_productos ps inner join tbl_producto pr on pr.id_producto = ps.id_producto
	inner join tbl_estadoproducto e on ps.id_estadoproducto= e.id_estadoproducto
	where serial = @Serie  and ps.e_eliminado=0
end

GO

/* [dbo].[spx_TraerChipID2_vacio] */
create proc [dbo].[spx_TraerChipID2_vacio](@Serie nvarchar(50),@ChipId nvarchar(50))
as
begin
	select ps.*,pr.nombre NombreProducto , e.Nombre EstadoProducto
	from tbl_productos ps inner join tbl_producto pr on pr.id_producto = ps.id_producto
	inner join tbl_estadoproducto e on ps.id_estadoproducto= e.id_estadoproducto
	where serial = @Serie  and chipid=@ChipId and ps.e_eliminado=0
end
GO

/* [dbo].[spx_TraerDatoSerieChipIdCU] */


CREATE proc [dbo].[spx_TraerDatoSerieChipIdCU](@serial nvarchar(70))
as
begin
declare @EstadosPermitidos table(id_Estado int)
insert into @EstadosPermitidos select id_estadoproducto from tbl_estadoproducto where id_estadoproducto in (3,12,7,15,16)


declare @Existe nvarchar(150), @SePuede nvarchar(150),@Observacion nvarchar(150)
declare @contador int ,@contador2 int
declare @tablaDatos table(Id_Productos int, serial nvarchar(150),chipid nvarchar(150),id_EstadoProducto int,EstadoProducto nvarchar(150),id_Ruta int, id_producto int ,nombre nvarchar(250),Tipo nvarchar(100),Ruta nvarchar(100))
declare @Resultado table(Id_Producto int, serial nvarchar(150),chipid nvarchar(150),EstadoProducto nvarchar(250),nombre nvarchar(250))
declare @ResultadoCorto table(Existe nvarchar(150),SePuede nvarchar(150), Observacion nvarchar(150))

	insert into @tablaDatos 
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsSerial'Tipo,(select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where serial=@serial and ps.e_eliminado=0
		union all
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsChipId'Tipo ,(select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where chipid=@serial and ps.e_eliminado=0
	

	set @contador = (select count(*) from @tablaDatos)
	
	if(@contador =1)--select * from tbl_Estadoproducto--3 INSTALADO_OT , 7  ENTREGADO_TIGO ,15 ENTREGADO_TIGO_PRETIRADO, 16 ENTREGADO_TIGO_PDAÑADO
	begin	
	
		set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (select id_estado from @EstadosPermitidos) ) --(3,12,7,15,16) )
		
		if(@contador2=1)
		begin
			set @Existe = 'Existe'
			set @SePuede ='SePuede'			
			insert into @Resultado		
			select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
			where id_estadoproducto in (select id_estado from @EstadosPermitidos ) 
			--select * from @EstadosPermitidos 
			--where id_estadoproducto in (3,12,7,15,16) 
			
		end
		else
		begin
			set @Existe = 'Existe'
			set @SePuede ='NoSePuede'
			set @Observacion ='ESTADO : ' + (select top 1 estadoproducto  from @tablaDatos)+ CHAR(13) + CHAR(10) +
							' - Grupo : ' +(select top 1 ruta  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Producto : ' +(select top 1 nombre  from @tablaDatos)
		end
	end	
	if(@contador >1)
	begin
		set @Existe = 'Existe'
		set @SePuede ='NoSePuede'
		set @Observacion ='Serial/ChipId repetido' 
	end
	
	
	
	if(len(@Existe)>0 )
	begin
		insert into @ResultadoCorto
		select @Existe,@SePuede,@Observacion
		
		select * from @ResultadoCorto
		select * from @tablaDatos
	end
end


GO

/* [dbo].[spx_TraerDatoSerieChipIdCU_CUNR2] */
CREATE PROCEDURE [dbo].[spx_TraerDatoSerieChipIdCU_CUNR2](@serial nvarchar(70),@chipid nvarchar(70))
as
begin
declare @EstadosPermitidos table(id_Estado int)
insert into @EstadosPermitidos select id_estadoproducto from tbl_estadoproducto where id_estadoproducto in (17)

declare @Existe nvarchar(150), @SePuede nvarchar(150),@Observacion nvarchar(150)
declare @contador int ,@contador2 int
declare @tablaDatos table(Id_Productos int, serial nvarchar(150),chipid nvarchar(150),id_EstadoProducto int,EstadoProducto nvarchar(150),id_Ruta int, id_producto int ,nombre nvarchar(250),Tipo nvarchar(100),Ruta nvarchar(100))
declare @Resultado table(Id_Producto int, serial nvarchar(150),chipid nvarchar(150),EstadoProducto nvarchar(250),nombre nvarchar(250))
declare @ResultadoCorto table(Existe nvarchar(150),SePuede nvarchar(150), Observacion nvarchar(150))

	insert into @tablaDatos 
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsSerial'Tipo,(select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where serial=@serial and chipid=@chipid and ps.e_eliminado=0
		--union all
		--select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		--p.nombre,'EsChipId'Tipo ,(select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		--from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		--where chipid=@serial and ps.e_eliminado=0
	

	set @contador = (select count(*) from @tablaDatos)
	
	if(@contador =1)
	begin	
	
		set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (select id_estado from @EstadosPermitidos) ) --(3,12,7,15,16) )
		
		if(@contador2=1)
		begin
			set @Existe = 'Existe'
			set @SePuede ='SePuede'			
			insert into @Resultado		
			select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
			where id_estadoproducto in (select id_estado from @EstadosPermitidos ) 			
		end
		else
		begin
			set @Existe = 'Existe'
			set @SePuede ='NoSePuede'
			set @Observacion ='ESTADO : ' + (select top 1 estadoproducto  from @tablaDatos)+ CHAR(13) + CHAR(10) +
							' - Grupo : ' +(select top 1 ruta  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Producto : ' +(select top 1 nombre  from @tablaDatos)
		end
	end	
	if(@contador >1)
	begin
		set @Existe = 'Existe'
		set @SePuede ='NoSePuede'
		set @Observacion ='Serial/ChipId repetido' 
	end
	
	
	
	if(len(@Existe)>0 )
	begin
		insert into @ResultadoCorto
		select @Existe,@SePuede,@Observacion
		
		select * from @ResultadoCorto
		select * from @tablaDatos
	end
end

GO

/* [dbo].[spx_TraerDatoSerieChipIdCU_ImportarIngreso] */


CREATE proc [dbo].[spx_TraerDatoSerieChipIdCU_ImportarIngreso](@serial nvarchar(70))
as
begin
declare @Existe nvarchar(150), @SePuede nvarchar(150),@Observacion nvarchar(150)
declare @contador int ,@contador2 int
declare @tablaDatos table(Id_Productos int, serial nvarchar(150),chipid nvarchar(150),id_EstadoProducto int,EstadoProducto nvarchar(150),id_Ruta int, id_producto int ,producto nvarchar(250),Tipo nvarchar(100),idsucursal int ,sucursal nvarchar(150),FechaTransaccion datetime, origen nvarchar(5))
declare @Resultado table(Id_Producto int, serial nvarchar(150),chipid nvarchar(150),EstadoProducto nvarchar(250),nombre nvarchar(250))
declare @ResultadoCorto table(Existe nvarchar(150),SePuede nvarchar(150), Observacion nvarchar(150))

	insert into @tablaDatos 
	select top 1 * from (
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre EstadoProducto,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsSerial'Tipo,9 id_sucursal,'Santa_Cruz' sucursal,FechaTransaccion,'sc' origen
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep 
		on ep.id_estadoproducto = ps.id_estadoproducto
		where serial=@serial and ps.e_eliminado=0		
		union all
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre EstadoProducto,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsChipId'Tipo,9 id_sucursal,'Santa_Cruz' sucursal,FechaTransaccion,'sc' origen
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep 
		on ep.id_estadoproducto = ps.id_estadoproducto
		where chipid=@serial and ps.e_eliminado=0
		union all
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,EstadoProducto,Id_Ruta,Id_Producto,
		Producto,'EsSerial'Tipo,id_sucursal, sucursal,FechaTransaccion,'otro' origen
		from tbl_productonacional ps 
		where serial=@serial and ps.e_eliminadoproductolocal=0
		union all
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,EstadoProducto,Id_Ruta,Id_Producto,
		Producto,'EsChipId'Tipo,id_sucursal, sucursal,FechaTransaccion,'otro' origen
		from tbl_productonacional ps 
		where chipid=@serial and ps.e_eliminadoproductolocal=0
		)a order by fechatransaccion desc
		
		
	

	set @contador = (select count(*) from @tablaDatos)
	
	if(@contador >0)--select * from tbl_Estadoproducto--3 INSTALADO_OT , 7  ENTREGADO_TIGO ,15 ENTREGADO_TIGO_PRETIRADO, 16 ENTREGADO_TIGO_PDAÑADO
	begin	
		set @contador2 = (select count(*) from @tablaDatos where estadoproducto in ('EN_ALMACEN','EN_RUTA','INSTALADO_OT','RETIRADO_OT','DAÑADO','DEVUELTO_EXCEDENTE','RETIRADO_OT_NOENTREGADO')  )
		--select * from tbl_estadoproducto where id_estadoproducto in (1,2,3,4,5,6,11)
		if(@contador2>0)
		begin
			set @Existe = 'Existe'
			set @SePuede ='SePuede'			
			insert into @Resultado		
			select id_producto,serial,chipid,EstadoProducto,t.producto from @tablaDatos t 			
			where estadoproducto in ('EN_ALMACEN','EN_RUTA','INSTALADO_OT','RETIRADO_OT','DAÑADO','DEVUELTO_EXCEDENTE','RETIRADO_OT_NOENTREGADO') 
--			select * from tbl_estadoproducto where id_estadoproducto in (1,2,3,4,5,6,11) 
		end
		else
		begin
			set @Existe = 'Existe'
			set @SePuede ='NoSePuede'
			set @Observacion ='ESTADO : ' + (select top 1 estadoproducto  from @tablaDatos)
		end
	end
	
	if(len(@Existe)>0 )
	begin
		insert into @ResultadoCorto
		select @Existe,@SePuede,@Observacion
		
		select * from @ResultadoCorto
		select * from @tablaDatos
	end
end

GO

/* [dbo].[spx_TraerDatoSerieChipIdCU_OT] */
--select top 10* from tbl_productos
--[spx_TraerDatoSerieChipIdCU_OT] '8080248952',51,2,3
--select top 10* from tbl_productos where id_estadoproducto=8
--[spx_TraerDatoSerieChipIdCU_OTVF] '8080248952','0600817B2D661971',51,2
----------------------------------------------------------------------------------------------
----------------------------------------------------------------------------------------------
CREATE proc [dbo].[spx_TraerDatoSerieChipIdCU_OT](@serial nvarchar(70),@IdProducto int,@TipoMaterial int,@IdRuta int)
as
begin
declare @EstadosPermitidos table(id_Estado int)
insert into @EstadosPermitidos select id_estadoproducto from tbl_estadoproducto where id_estadoproducto in (3,12,7,15,16,21)

--[spx_TraerDatoSerieChipIdCU_OT] 'TIG-1443-015903',106,2,136

declare @Existe nvarchar(150), @SePuede nvarchar(150),@Observacion nvarchar(150)
declare @contador int ,@contador2 int
declare @tablaDatos table(Id_Productos int, serial nvarchar(150),chipid nvarchar(150),id_EstadoProducto int,EstadoProducto nvarchar(150),id_Ruta int, id_producto int ,nombre nvarchar(250),Tipo nvarchar(100),Ruta nvarchar(100))
declare @Resultado table(Id_Producto int, serial nvarchar(150),chipid nvarchar(150),EstadoProducto nvarchar(250),nombre nvarchar(250))
declare @ResultadoCorto table(Existe nvarchar(150),SePuede nvarchar(150), Observacion nvarchar(150))

	insert into @tablaDatos 
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsSerial'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto		
		where serial=@serial and ps.e_eliminado=0
		union all
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsChipId'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta 
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where chipid=@serial and ps.e_eliminado=0
	
	set @contador = (select count(*) from @tablaDatos)
	
	if(@contador =1)--select * from tbl_Estadoproducto--3 INSTALADO_OT , 7  ENTREGADO_TIGO ,15 ENTREGADO_TIGO_PRETIRADO, 16 ENTREGADO_TIGO_PDAÑADO
	begin		
		if(@TipoMaterial=1)
			set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (2) and id_producto=@IdProducto and id_ruta=@IdRuta)
		else
			set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (select id_estado from @EstadosPermitidos) and id_producto=@IdProducto )
			--set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (3,7,12,15,16)  )
		
		
		if(@contador2=1)
		begin
			set @Existe = 'Existe'
			set @SePuede ='SePuede'			
			
			if(@TipoMaterial=1)	
			begin
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
				where id_estadoproducto in (2) and id_ruta=@IdRuta
			end
			else 
			begin			
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
				where id_estadoproducto in (select id_estado from @EstadosPermitidos) 
				--where id_estadoproducto in (3,12,7,15,16) 
			end
			
		end
		else
		begin
			set @Existe = 'Existe'
			set @SePuede ='NoSePuede'
			set @Observacion ='Estado : ' + (select top 1 estadoproducto  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Grupo : ' +(select top 1 ruta  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Producto : ' +(select top 1 nombre  from @tablaDatos)
		end
	end	
	if(@contador >1)
	begin
		set @Existe = 'Existe'
		set @SePuede ='NoSePuede'
		set @Observacion ='Serial/ChipId repetido' 
	end
	
	
	if(len(@Existe)>0 )
	begin
		insert into @ResultadoCorto
		select @Existe,@SePuede,@Observacion
		
		select * from @ResultadoCorto
		select * from @tablaDatos
	end
end




GO

/* [dbo].[spx_TraerDatoSerieChipIdCU_OT_OTProductoBaja] */



CREATE proc [dbo].[spx_TraerDatoSerieChipIdCU_OT_OTProductoBaja](@serial nvarchar(70),@IdProducto int,@TipoMaterial int,@IdRuta int)
as
begin
declare @EstadosPermitidos table(id_Estado int)
insert into @EstadosPermitidos select id_estadoproducto from tbl_estadoproducto where id_estadoproducto in (3,12,7,15,16,8)

declare @VerificacionBaja table(id_Bitacora int, id_producto int, serial nvarchar(150), chipid nvarchar(150),codigo int,tabla nvarchar(250),
idEstadoProducto int)

declare @Existe nvarchar(150), @SePuede nvarchar(150),@Observacion nvarchar(150)
declare @contador int ,@contador2 int, @tipobaja int 
declare @tablaDatos table(Id_Productos int, serial nvarchar(150),chipid nvarchar(150),id_EstadoProducto int,EstadoProducto nvarchar(150),id_Ruta int, id_producto int ,nombre nvarchar(250),Tipo nvarchar(100),Ruta nvarchar(100))
declare @Resultado table(Id_Producto int, serial nvarchar(150),chipid nvarchar(150),EstadoProducto nvarchar(250),nombre nvarchar(250))
declare @ResultadoCorto table(Existe nvarchar(150),SePuede nvarchar(150), Observacion nvarchar(150))

	insert into @tablaDatos 
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsSerial'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto		
		where serial=@serial and ps.e_eliminado=0
		union all
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsChipId'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta 
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where chipid=@serial and ps.e_eliminado=0
	
	set @contador = (select count(*) from @tablaDatos)
	
	if(@contador =1)--select * from tbl_Estadoproducto--3 INSTALADO_OT , 7  ENTREGADO_TIGO ,15 ENTREGADO_TIGO_PRETIRADO, 16 ENTREGADO_TIGO_PDAÑADO
	begin		
		if(@TipoMaterial=1)
			set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (2) and id_producto=@IdProducto and id_ruta=@IdRuta)
		else
		begin
			set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (select id_estado from @EstadosPermitidos)  and id_producto=@IdProducto)			
			if(@contador2 =1)
			begin
				insert into @VerificacionBaja
				select top 1 id_bitacora,id_producto,serial,chipid,codigo,tabla,idestadoproducto from tbl_bitacora where serial=@serial and e_eliminado=0 order by id_bitacora desc
				
 				  set @tipobaja = (select id_tipobajaproductospendiente from tbl_bajaproductos where id_bajaproductos=(select codigo from @VerificacionBaja where tabla='tbl_BajaProducto') )
				if(@tipobaja!=1)--ajuste 
				set @contador2=0
			end
		end
		
		
		if(@contador2=1)
		begin
			set @Existe = 'Existe'
			set @SePuede ='SePuede'			
			
			if(@TipoMaterial=1)	
			begin
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
				where id_estadoproducto in (2) and id_ruta=@IdRuta
			end
			else 
			begin			
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
				where id_estadoproducto in (select id_estado from @EstadosPermitidos) 				
			end
			
		end
		else
		begin
			set @Existe = 'Existe'
			set @SePuede ='NoSePuede'
			set @Observacion ='Estado : ' + (select top 1 estadoproducto  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Grupo : ' +(select top 1 ruta  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Producto : ' +(select top 1 nombre  from @tablaDatos)
		end
	end	
	if(@contador >1)
	begin
		set @Existe = 'Existe'
		set @SePuede ='NoSePuede'
		set @Observacion ='Serial/ChipId repetido' 
	end
	
	
	if(len(@Existe)>0 )
	begin
		insert into @ResultadoCorto
		select @Existe,@SePuede,@Observacion
		
		select * from @ResultadoCorto
		select * from @tablaDatos
	end
end

GO

/* [dbo].[spx_TraerDatoSerieChipIdCU_OTVF] */

--select top 10* from tbl_productos where id_estadoproducto=8
--[spx_TraerDatoSerieChipIdCU_OTVF] '8080248952','0600817B2D661971',51,2
--[spx_TraerDatoSerieChipIdCU_OTVF] 'TIG-1443-015903',106,2,136
-------------------------------------------------------------------------------------------
-------------------------------------------------------------------------------------------
CREATE proc [dbo].[spx_TraerDatoSerieChipIdCU_OTVF](@serial nvarchar(70),@chipId nvarchar(70),@IdProducto int,@TipoMaterial int)
as
begin

declare @EstadosPermitidos table(id_Estado int)
insert into @EstadosPermitidos select id_estadoproducto from tbl_estadoproducto where id_estadoproducto in (3,12,7,15,16,21)



declare @Existe nvarchar(150), @SePuede nvarchar(150),@Observacion nvarchar(150)
declare @contador int ,@contador2 int
declare @tablaDatos table(Id_Productos int, serial nvarchar(150),chipid nvarchar(150),id_EstadoProducto int,EstadoProducto nvarchar(150),id_Ruta int, id_producto int ,nombre nvarchar(250),Tipo nvarchar(100),Ruta nvarchar(100))
declare @Resultado table(Id_Producto int, serial nvarchar(150),chipid nvarchar(150),EstadoProducto nvarchar(250),nombre nvarchar(250))
declare @ResultadoCorto table(Existe nvarchar(150),SePuede nvarchar(150), Observacion nvarchar(150))

	insert into @tablaDatos 
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsSerial'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where serial=@serial and ps.e_eliminado=0
		union all
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsChipId'Tipo , (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where chipid=@serial and ps.e_eliminado=0
		
		--select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		--p.nombre,'EsSerial'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		--from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		--where serial='TIG-1701-007246' and ps.e_eliminado=0
		--union all
		--select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		--p.nombre,'EsChipId'Tipo , (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		--from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		--where chipid='TIG-1701-007246' and ps.e_eliminado=0
		
	
	set @contador = (select count(*) from @tablaDatos)
	
	if(@contador =1)--select * from tbl_Estadoproducto--3 INSTALADO_OT , 7  ENTREGADO_TIGO ,15 ENTREGADO_TIGO_PRETIRADO, 16 ENTREGADO_TIGO_PDAÑADO
	begin
	
	
	--select * from @tablaDatos
		if(@TipoMaterial=1)
			set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (2) and id_producto=@IdProducto )
		else
			--set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (3,12,7,15,16)  )
				set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (select id_estado from @EstadosPermitidos) and id_producto=@IdProducto )
	
		
		if(@contador2=1)
		begin
			set @Existe = 'Existe'
			set @SePuede ='SePuede'			
			
			if(@TipoMaterial=1)	
			begin
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
				where id_estadoproducto in (2) 
				
			end
			else 
			begin
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
					where id_estadoproducto in (select id_estado from @EstadosPermitidos) 
			--	where id_estadoproducto in (3,12,7,15,16) 
			--	select * from @Resultado		
				
			end
		end
		else
		begin
		
			set @Existe = 'Existe'
			set @SePuede ='NoSePuede'
			set @Observacion ='ESTADO : ' + (select top 1 estadoproducto  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Grupo : ' +(select top 1 ruta  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Producto : ' +(select top 1 nombre  from @tablaDatos)
		end
	end	
	if(@contador >1)
	begin
		set @Existe = 'Existe'
		set @SePuede ='NoSePuede'
		set @Observacion ='Serial/ChipId repetido' 
	end
	delete from @tablaDatos
	--select @SePuede
	
	if(@SePuede='SePuede' or @contador=0)
	begin
	
	insert into @tablaDatos 
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsSerial'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where serial=@chipid and ps.e_eliminado=0
		union all
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsChipId'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where chipid=@chipid and ps.e_eliminado=0
		
	end
	
	set @contador = (select count(*) from @tablaDatos)
	
	if(@contador =1)--select * from tbl_Estadoproducto--3 INSTALADO_OT , 7  ENTREGADO_TIGO ,15 ENTREGADO_TIGO_PRETIRADO, 16 ENTREGADO_TIGO_PDAÑADO
	begin		
	
	
		if(@tipomaterial=1)
			set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (2) and id_producto=@IdProducto and Serial=@serial )		
		else
		set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (select id_estado from @EstadosPermitidos) and  Serial=@serial )
			--set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (3,7,12,15,16) and  Serial=@serial )
	--		 [spx_TraerDatoSerieChipIdCU_OTVF] 'TIG-1701-007246','00648189260',107,2		
		if(@contador2=1)
		begin
			set @Existe = 'Existe'
			set @SePuede ='SePuede'		
			if(@tipomaterial=1)
			begin
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
				where id_estadoproducto in (2) 
			end
			else	
			begin
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
				where id_estadoproducto in (select id_estado from @EstadosPermitidos) 
--				where id_estadoproducto in (3,7,15,16) 
				--select * from  @Resultado		
			end
			


		end
		else
		begin
		
			set @Existe = 'Existe'
			set @SePuede ='NoSePuede'
			set @Observacion ='ESTADO : ' + (select top 1 estadoproducto  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Grupo : ' +(select top 1 ruta  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Producto : ' +(select top 1 nombre  from @tablaDatos)
		end
	end	
	if(@contador >1)
	begin
		set @Existe = 'Existe'
		set @SePuede ='NoSePuede'
		set @Observacion ='Serial/ChipId repetido' 
	end
	
	
	
	
	
	if(len(@Existe)>0 )
	begin
		insert into @ResultadoCorto
		select @Existe,@SePuede,@Observacion
		
		select * from @ResultadoCorto
		select * from @tablaDatos
	end
end


GO

/* [dbo].[spx_TraerDatoSerieChipIdCU_OTVF_OTProductoBaja] */
----------------------------------------------------------------------------
--select * from tbl_productos where id_estadoproducto=8
--select * from tbl_bitacora where serial='TIG-1502-005542'--no
--select * from tbl_bajaproductos where id_bajaproductos=328 --no

--select * from tbl_bitacora where serial='7363419599'--si
--select * from tbl_bajaproductos where id_bajaproductos=493--si


--[spx_TraerDatoSerieChipIdCU_OT_OTProductoBaja] '7363419599',50,2,5

--[spx_TraerDatoSerieChipIdCU_OTVF_OTProductoBaja] '7363419599','060082c0b5747b56',50,2

CREATE proc [dbo].[spx_TraerDatoSerieChipIdCU_OTVF_OTProductoBaja](@serial nvarchar(70),@chipId nvarchar(70),@IdProducto int,@TipoMaterial int)
as
begin

declare @EstadosPermitidos table(id_Estado int)
insert into @EstadosPermitidos select id_estadoproducto from tbl_estadoproducto where id_estadoproducto in (3,12,7,15,16,8)

declare @VerificacionBaja table(id_Bitacora int, id_producto int, serial nvarchar(150), chipid nvarchar(150),codigo int,tabla nvarchar(250),
idEstadoProducto int)

declare @Existe nvarchar(150), @SePuede nvarchar(150),@Observacion nvarchar(150)
declare @contador int ,@contador2 int,@tipobaja int 
declare @tablaDatos table(Id_Productos int, serial nvarchar(150),chipid nvarchar(150),id_EstadoProducto int,EstadoProducto nvarchar(150),id_Ruta int, id_producto int ,nombre nvarchar(250),Tipo nvarchar(100),Ruta nvarchar(100))
declare @Resultado table(Id_Producto int, serial nvarchar(150),chipid nvarchar(150),EstadoProducto nvarchar(250),nombre nvarchar(250))
declare @ResultadoCorto table(Existe nvarchar(150),SePuede nvarchar(150), Observacion nvarchar(150))

	insert into @tablaDatos 
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsSerial'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where serial=@serial and ps.e_eliminado=0
		union all
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsChipId'Tipo , (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where chipid=@serial and ps.e_eliminado=0
		
	
	set @contador = (select count(*) from @tablaDatos)
	
	if(@contador =1)--select * from tbl_Estadoproducto--3 INSTALADO_OT , 7  ENTREGADO_TIGO ,15 ENTREGADO_TIGO_PRETIRADO, 16 ENTREGADO_TIGO_PDAÑADO
	begin
	--
	
	--select * from @tablaDatos
		if(@TipoMaterial=1)
			set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (2) and id_producto=@IdProducto )
		else
		begin
			set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (select id_estado from @EstadosPermitidos) and id_producto=@IdProducto )			
			if(@contador2 =1)
			begin
				insert into @VerificacionBaja
					select top 1 id_bitacora,id_producto,serial,chipid,codigo,tabla,idestadoproducto from tbl_bitacora where serial=@serial and e_eliminado=0 order by id_bitacora desc
				
 				set @tipobaja = (select id_tipobajaproductospendiente from tbl_bajaproductos where id_bajaproductos=(select codigo from @VerificacionBaja where tabla='tbl_BajaProducto') )
				if(@tipobaja!=1)--ajuste 
				set @contador2=0
				--select @contador2
			end
		--		set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (select id_estado from @EstadosPermitidos)  )
		end
	
		 
		if(@contador2=1)
		begin
			set @Existe = 'Existe'
			set @SePuede ='SePuede'			
			
			if(@TipoMaterial=1)	
			begin
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
				where id_estadoproducto in (2) 
				
			end
			else 
			begin
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
					where id_estadoproducto in (select id_estado from @EstadosPermitidos) 			
			end
		end
		else
		begin
		
			set @Existe = 'Existe'
			set @SePuede ='NoSePuede'
			set @Observacion ='ESTADO : ' + (select top 1 estadoproducto  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Grupo : ' +(select top 1 ruta  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Producto : ' +(select top 1 nombre  from @tablaDatos)
		end
	end	
	if(@contador >1)
	begin
		set @Existe = 'Existe'
		set @SePuede ='NoSePuede'
		set @Observacion ='Serial/ChipId repetido' 
	end
	delete from @tablaDatos
	--select @SePuede
	
	if(@SePuede='SePuede' or @contador=0)
	begin
	
	insert into @tablaDatos 
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsSerial'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where serial=@chipid and ps.e_eliminado=0
		union all
		select ps.Id_Productos,ps.Serial,ps.ChipId,ps.Id_EstadoProducto,ep.Nombre,ps.Id_Ruta,ps.Id_Producto,
		p.nombre,'EsChipId'Tipo, (select nombre from tbl_ruta where id_ruta=ps.Id_Ruta) Ruta
		from tbl_productos ps inner join tbl_producto p on p.id_producto = ps.id_producto inner join tbl_estadoproducto ep on ep.id_estadoproducto = ps.id_estadoproducto
		where chipid=@chipid and ps.e_eliminado=0
		
	end	
	set @contador = (select count(*) from @tablaDatos)
	
	if(@contador =1)--select * from tbl_Estadoproducto--3 INSTALADO_OT , 7  ENTREGADO_TIGO ,15 ENTREGADO_TIGO_PRETIRADO, 16 ENTREGADO_TIGO_PDAÑADO
	begin		
		if(@tipomaterial=1)
			set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (2) and id_producto=@IdProducto and Serial=@serial )		
		else
		set @contador2 = (select count(*) from @tablaDatos where id_estadoproducto in (select id_estado from @EstadosPermitidos) and  Serial=@serial )			
		if(@contador2=1)
		begin
			set @Existe = 'Existe'
			set @SePuede ='SePuede'		
			if(@tipomaterial=1)
			begin
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
				where id_estadoproducto in (2) 
			end
			else	
			begin
				insert into @Resultado		
				select id_producto,serial,chipid,EstadoProducto,t.nombre from @tablaDatos t 			
				where id_estadoproducto in (select id_estado from @EstadosPermitidos) 
--				where id_estadoproducto in (3,7,15,16) 
				--select * from  @Resultado		
			end			
		end
		else
		begin
		
			set @Existe = 'Existe'
			set @SePuede ='NoSePuede'
			set @Observacion ='ESTADO : ' + (select top 1 estadoproducto  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Grupo : ' +(select top 1 ruta  from @tablaDatos)+CHAR(13) + CHAR(10) +
							' - Producto : ' +(select top 1 nombre  from @tablaDatos)
		end
	end	
	if(@contador >1)
	begin
		set @Existe = 'Existe'
		set @SePuede ='NoSePuede'
		set @Observacion ='Serial/ChipId repetido' 
	end	
	
	if(len(@Existe)>0 )
	begin
		insert into @ResultadoCorto
		select @Existe,@SePuede,@Observacion
		
		select * from @ResultadoCorto
		select * from @tablaDatos
	end
end




GO

/* [dbo].[spx_TraerProductoUniversoE18] */

create proc [dbo].[spx_TraerProductoUniversoE18] (@serie nvarchar(50))
as
select * from tbl_FaltanteE18 where serie=@serie 
GO

/* [dbo].[spx_TraerSerie2] */

create proc [dbo].[spx_TraerSerie2](@ChipId nvarchar(50))
as
begin
	select ps.*,pr.nombre NombreProducto , e.Nombre EstadoProducto
	from tbl_productos ps inner join tbl_producto pr on pr.id_producto = ps.id_producto
	inner join tbl_estadoproducto e on ps.id_estadoproducto= e.id_estadoproducto
	where chipid = @ChipId  and ps.e_eliminado=0
end

GO

/* [dbo].[spx_TraerTipoSolicitante] */

create proc spx_TraerTipoSolicitante
as
select* from tbl_TipoSolicitante where e_eliminado=0
GO

/* [dbo].[spx_TraerTipoSolicitanteEmpleado] */

CREATE proc [dbo].[spx_TraerTipoSolicitanteEmpleado]
as 
select * from tbl_tiposolicitante where e_eliminado=0 --and id_tipo_solicitante in (1,4) 



GO

/* [dbo].[spx_TraerTipoSolicitanteSociosEmbajador] */

CREATE proc [dbo].[spx_TraerTipoSolicitanteSociosEmbajador]
as
select* from tbl_TipoSolicitante where e_eliminado=0 and id_tipo_solicitante in (3,5)

GO

/* [dbo].[spx_TraerTipoUsuario] */

create proc spx_TraerTipoUsuario
as
select * from tbl_tipousuario where e_eliminado=0 order by nombre

GO

/* [dbo].[spx_TraerTodosBajasProductos] */

CREATE proc [dbo].[spx_TraerTodosBajasProductos]
as
begin
	select * from
	(
		SELECT     b.Id_BajaProductosPendiente Id_BajaProductos,r.Nombre Ruta,v.Nombre Tecnico,'BajaProductosAnticipados' Tipo,Estado,
		es.Nombre EstadoProductos,b.observacion,
		b.FechaPendiente Fecha,u.Nombre Usuario,tp.Id_TipoBajaProductosPendiente,tp.TipoBaja ,
		b.Total, 'No' Cobrado,0 id, 'Ninguno' TipoCobro,r.Id_ruta
		FROM    dbo.tbl_BajaProductosPendiente b inner join tbl_Usuario u on u.Id_Usuario = b.Id_UsuarioRegistrado and b.e_eliminado=0
		inner join tbl_ruta r on r.id_ruta = b.id_ruta 
		inner join tbl_estadoproducto es on es.id_estadoproducto=b.Id_EstadoProductos
		inner join tbl_Vendedor v on v.id_vendedor=r.Id_Vendedor
		inner join tbl_TipoBajaProductosPendiente tp on tp.Id_TipoBajaProductosPendiente=b.Id_TipoBajaProductosPendiente	
		where Estado='Pendiente' and  b.e_eliminado=0

		union all
		
		SELECT     b.id_BajaProductos,r.Nombre Ruta,v.Nombre Tecnico,'BajaProductos' Tipo ,
		case when (archivo is null)  then 'RegistroAnterior' 
		 when (archivo ='AA==') then 'FaltaDocumento' 
		else 'Completo' end Estado,
		es.Nombre EstadoProductos,b.observacion,b.fecha,u.Nombre Usuario,tp.Id_TipoBajaProductosPendiente,tp.TipoBaja ,
		b.Total, case when cobrado =1 then 'Si' else 'No' end Cobrado,
		(select id from tbl_tipocobrobaja where id =b.id_tipocobrobaja) Id,
		(select TipoCobro from tbl_tipocobrobaja where id =b.id_tipocobrobaja)TipoCobro,r.Id_ruta
		 
		FROM         dbo.tbl_BajaProductos b inner join tbl_Usuario u on u.Id_Usuario = b.id_Usuario and b.e_eliminado=0
		inner join tbl_ruta r on r.id_ruta = b.id_ruta 
		inner join tbl_estadoproducto es on es.id_estadoproducto=b.Id_EstadoProductos
		inner join tbl_Vendedor v on v.id_vendedor=r.Id_Vendedor
		inner join tbl_TipoBajaProductosPendiente tp on tp.Id_TipoBajaProductosPendiente=b.Id_TipoBajaProductosPendiente
		--inner join tbl_tipocobrobaja tc on tc.id=b.id_tipocobrobaja
		where b.e_eliminado=0
	)a order by Tipo desc,Id_BajaProductos  desc
end


GO

/* [dbo].[spx_TraerTodosBajasProductos_Cobro] */
CREATE proc [dbo].[spx_TraerTodosBajasProductos_Cobro]
as
begin
	select * from
	(
		SELECT Id, CodigoBaja Id_BajaProductos,0 Id_Ruta,'DEUDA' Ruta,Id_Vendedor Id_Tecnico, TipoSolicitante,CodigoEH CodEmpleado,NombreDeudor Tecnico,
			'Deuda'Tipo, Estado,
			''EstadoProductos,''Observacion,
		Fecha,''Usuario,0 Id_TipoBaja,''TipoBaja,
		TotalDeuda,TotalPagado,MontoDeuda SaldoDeudor,''Cobrado
		FROM tbl_DeudaBajas
		
		union all
		SELECT     b.id_BajaProductos Id,b.id_BajaProductos,b.Id_ruta,r.Nombre Ruta,b.id_Tecnico
		, (select nombre from tbl_tiposolicitante where id_tipo_solicitante = v.id_Tiposolicitante)TipoSolicitante
		,v.CodEmpleado,v.Nombre Tecnico,
		'BajaProductos' Tipo ,
		case when (Nombrearchivo is null )  then 'RegistroAnterior' 
		when (Nombrearchivo ='') then 'FaltaDocumento' 
		else 'Completo' end Estado,
		es.Nombre EstadoProductos,b.observacion,b.fecha,u.Nombre Usuario,tp.Id_TipoBajaProductosPendiente,tp.TipoBaja ,
		b.Total TotalDeuda,0 TotalPagado,b.Total SaldoDeudor, case when cobrado =1 then 'Si' else 'No' end Cobrado
		FROM         dbo.tbl_BajaProductos b inner join tbl_Usuario u on u.Id_Usuario = b.id_Usuario and b.e_eliminado=0
		inner join tbl_ruta r on r.id_ruta = b.id_ruta 
		inner join tbl_estadoproducto es on es.id_estadoproducto=b.Id_EstadoProductos
		inner join tbl_Vendedor v on v.id_vendedor=b.Id_Tecnico		
		inner join tbl_TipoBajaProductosPendiente tp on tp.Id_TipoBajaProductosPendiente=b.Id_TipoBajaProductosPendiente		
		where b.e_eliminado=0 and tp.Id_TipoBajaProductosPendiente=2
		
	)a order by  tipo desc, Id_BajaProductos desc,id desc
end
GO

/* [dbo].[spx_TraerTodosBajasProductosHoy] */
CREATE proc dbo.spx_TraerTodosBajasProductosHoy(@Fecha datetime)
as
	SELECT     b.id_BajaProductos,b.observacion,b.fecha
	FROM         dbo.tbl_BajaProductos b 
	where b.E_Eliminado=0 and dbo.dateonly(b.Fecha)=dbo.dateonly(@Fecha) 

GO

/* [dbo].[spx_TraerTodosBajasProductosRangoFechas] */
CREATE proc [dbo].[spx_TraerTodosBajasProductosRangoFechas](@FechaInicio datetime, @FechaFin datetime )
as
begin
select * from
	(
	SELECT     b.Id_BajaProductosPendiente Id_BajaProductos,b.FechaPendiente Fecha,r.Nombre Ruta,v.Nombre Tecnico ,'BajaProductosAnticipados' Tipo,Estado,
	es.Nombre EstadoProductos,b.observacion,u.Nombre Usuario,tp.Id_TipoBajaProductosPendiente,tp.TipoBaja ,
	b.Total, '' Cobrado,0 id, '' TipoCobro
	FROM         dbo.tbl_BajaProductosPendiente b inner join tbl_Usuario u on u.Id_Usuario = b.Id_UsuarioPendiente and b.e_eliminado=0
	inner join tbl_ruta r on r.id_ruta = b.id_ruta 
	inner join tbl_estadoproducto es on es.id_estadoproducto=b.Id_EstadoProductos
	inner join tbl_Vendedor v on v.id_vendedor=r.Id_Vendedor
	inner join tbl_TipoBajaProductosPendiente tp on tp.Id_TipoBajaProductosPendiente=b.Id_TipoBajaProductosPendiente
	where dbo.dateonly(b.FechaPendiente)between dbo.dateonly(@FechaInicio)  and dbo.dateonly(@FechaFin)
	
	
	union all
	
	SELECT     b.id_BajaProductos,b.fecha,r.Nombre Ruta,v.Nombre Tecnico ,'BajaProductos' Tipo ,
	case when (archivo is null)  then 'RegistroAnterior' 
		 when (archivo ='AA==') then 'FaltaDocumento' 
		else 'Completo' end Estado,
	es.Nombre EstadoProductos,b.observacion,u.Nombre Usuario,tp.Id_TipoBajaProductosPendiente,tp.TipoBaja ,
	b.Total, case when cobrado =1 then 'Si' else 'No' end Cobrado,
		(select id from tbl_tipocobrobaja where id =b.id_tipocobrobaja) Id,
		(select TipoCobro from tbl_tipocobrobaja where id =b.id_tipocobrobaja)TipoCobro
	FROM         dbo.tbl_BajaProductos b inner join tbl_Usuario u on u.Id_Usuario = b.id_Usuario and b.e_eliminado=0
	inner join tbl_ruta r on r.id_ruta = b.id_ruta 
	inner join tbl_estadoproducto es on es.id_estadoproducto=b.Id_EstadoProductos
	inner join tbl_Vendedor v on v.id_vendedor=r.Id_Vendedor
	inner join tbl_TipoBajaProductosPendiente tp on tp.Id_TipoBajaProductosPendiente=b.Id_TipoBajaProductosPendiente
	where dbo.dateonly(b.Fecha)between dbo.dateonly(@FechaInicio)  and dbo.dateonly(@FechaFin)
	)a order by Tipo desc,Id_BajaProductos  desc
end

GO

/* [dbo].[spx_TraerTodosSalidasTraspaso] */

CREATE proc [dbo].[spx_TraerTodosSalidasTraspaso]
as
select * from (
	SELECT     b.Id_SalidaTraspasoPendiente Id_SalidaTraspaso,0 Id_Ruta,'GRUPO ALMACEN'  Ruta,'SalidaTraspasoAnticipado' Tipo,Estado,b.FechaPendiente Fecha,b.FechaRegistrado Fecha_Registro,b.Observacion,u.Nombre Usuario
	FROM         dbo.tbl_SalidaTraspasoPendiente b inner join tbl_Usuario u on u.Id_Usuario= b.id_UsuarioPendiente  and b.e_eliminado=0
	where estado='Pendiente'
	union all
	SELECT     b.Id_SalidaTraspaso,b.Id_Ruta,r.Nombre Ruta,'SalidaTraspaso' Tipo,
	case when archivo is null then 'RegistroAnterior' else 'Completo' end Estado,
	b.Fecha,b.Fecha_Registro,b.Observacion,u.Nombre Usuario
	FROM         dbo.tbl_SalidaTraspaso b inner join tbl_Usuario u on u.Id_Usuario = b.id_Usuario and b.e_eliminado=0
	inner join tbl_ruta r on r.id_ruta = b.id_ruta 
	
) a order by Tipo desc,Id_SalidaTraspaso  desc

GO

/* [dbo].[spx_TraerTodosSalidasTraspasoHoy] */
create proc [dbo].[spx_TraerTodosSalidasTraspasoHoy](@Fecha datetime)
as
SELECT     b.Id_SalidaTraspaso,b.Id_Ruta,r.Nombre Ruta,b.Fecha,b.Fecha_Registro,b.Observacion,u.Nombre Usuario
FROM         dbo.tbl_SalidaTraspaso b inner join tbl_Usuario u on u.Id_Usuario = b.id_Usuario and b.e_eliminado=0
inner join tbl_ruta r on r.id_ruta = b.id_ruta 
where dbo.DateOnly(b.Fecha)=dbo.DateOnly(@Fecha)
order by b.Id_SalidaTraspaso desc

---
GO

/* [dbo].[spx_TraerTodosSalidasTraspasoRangoFechas] */
CREATE proc [dbo].[spx_TraerTodosSalidasTraspasoRangoFechas](@FechaInicio datetime,@FechaFin datetime)
as
begin
	select * from (
		SELECT     b.Id_SalidaTraspasoPendiente Id_SalidaTraspaso,0 Id_Ruta,'GRUPO ALMACEN'  Ruta,'SalidaTraspasoAnticipado' Tipo,Estado,b.FechaPendiente Fecha,b.FechaRegistrado Fecha_Registro,b.Observacion,u.Nombre Usuario
		FROM         dbo.tbl_SalidaTraspasoPendiente b inner join tbl_Usuario u on u.Id_Usuario = b.id_UsuarioPendiente and b.e_eliminado=0
		where dbo.DateOnly(b.FechaPendiente) between dbo.DateOnly(@FechaInicio) and dbo.DateOnly(@FechaFin)
		union all
		SELECT     b.Id_SalidaTraspaso,b.Id_Ruta,r.Nombre Ruta,'SalidaTraspaso' Tipo,
		case when archivo is null then 'RegistroAnterior' else 'Completo' end Estado,b.Fecha,b.Fecha_Registro,b.Observacion,u.Nombre Usuario
		FROM         dbo.tbl_SalidaTraspaso b inner join tbl_Usuario u on u.Id_Usuario = b.id_Usuario and b.e_eliminado=0
		inner join tbl_ruta r on r.id_ruta = b.id_ruta 
		where dbo.DateOnly(b.Fecha) between dbo.DateOnly(@FechaInicio) and dbo.DateOnly(@FechaFin)
	)a order by Tipo desc,Id_SalidaTraspaso  desc
end
GO

/* [dbo].[spx_TraerTodosVendedores] */
create proc spx_TraerTodosVendedores
as 
select * from tbl_vendedor where e_eliminado=0

GO

/* [dbo].[spx_traervendedores_x_formtecnico] */
CREATE PROCEDURE dbo.spx_TraerVendedores_x_FormTecnico
AS
BEGIN
    SET NOCOUNT ON;

    SELECT v.*, ts.id_Tipo_Solicitante, ts.Nombre AS TipoSolicitante
    FROM dbo.tbl_Vendedor v
    INNER JOIN dbo.tbl_TipoSolicitante ts ON ts.id_Tipo_Solicitante = v.id_tiposolicitante
    WHERE v.E_Eliminado = 0
    ORDER BY v.Nombre;
END

GO

/* [dbo].[spx_Trazabilidad] */
CREATE proc [dbo].[spx_Trazabilidad](@serial nvarchar(50),@ChipID nvarchar(50), @Id_TipoCodigo int)
as
	if(@Id_TipoCodigo = 0)
	begin
		select tb.id_bitacora, tep.Nombre Estado, tb.id_producto, tp.Nombre Producto, tb.serial,tb.ChipID, tb.codigo, tb.tabla, 
				tb.observacion Accion, tb.fechaTransaccion, tb.fechaRegistro, tb.id_Ruta, tr.Nombre Ruta, tb.id_Usuario, tu.Nombre Usuario
		from tbl_bitacora tb
			inner join tbl_producto tp on tp.Id_Producto = tb.id_producto
			left join tbl_productos tps on tp.Id_Producto = tps.id_producto and tps.e_eliminado = 0 and tps.serial = @serial -- 'TIG-2121-212121'
			left join tbl_EstadoProducto tep on tep.Id_EstadoProducto = tps.id_EstadoProducto
			left join tbl_Ruta tr on tr.Id_Ruta = tb.id_Ruta
			left join tbl_Usuario tu on tu.Id_Usuario = tb.id_Usuario
		where tb.serial = @serial and tb.e_eliminado = 0 
		order by tb.fechaRegistro desc
	end
	else 
	begin
		select tb.id_bitacora, tep.Nombre Estado, tb.id_producto, tp.Nombre Producto, tb.serial,tb.ChipID, tb.codigo, tb.tabla, 
				tb.observacion Accion, tb.fechaTransaccion, tb.fechaRegistro, tb.id_Ruta, tr.Nombre Ruta, tb.id_Usuario, tu.Nombre Usuario
		from tbl_bitacora tb
			inner join tbl_producto tp on tp.Id_Producto = tb.id_producto
			left join tbl_productos tps on tp.Id_Producto = tps.id_producto and tps.e_eliminado = 0 and tps.chipId = @ChipID
			left join tbl_EstadoProducto tep on tep.Id_EstadoProducto = tps.id_EstadoProducto
			left join tbl_Ruta tr on tr.Id_Ruta = tb.id_Ruta
			left join tbl_Usuario tu on tu.Id_Usuario = tb.id_Usuario
		where tb.chipID = @ChipID and tb.e_eliminado = 0 
		order by tb.fechaRegistro desc
	end 


GO

/* [dbo].[spx_Trazabilidad_X_SerialChipId] */

CREATE proc [dbo].[spx_Trazabilidad_X_SerialChipId](@serial nvarchar(50),@ChipID nvarchar(50))
as
	
		select tb.id_bitacora, tep.Nombre Estado, tb.id_producto, tp.Nombre Producto, tb.serial,tb.ChipID, tb.codigo, tb.tabla, 
				tb.observacion Accion, tb.fechaTransaccion, tb.fechaRegistro, tb.id_Ruta, tr.Nombre Ruta, tb.id_Usuario, tu.Nombre Usuario
		from tbl_bitacora tb
			inner join tbl_producto tp on tp.Id_Producto = tb.id_producto
			left join tbl_productos tps on tp.Id_Producto = tps.id_producto and tps.e_eliminado = 0 and tps.serial = @serial 
			and  tps.chipid=@ChipID
			left join tbl_EstadoProducto tep on tep.Id_EstadoProducto = tps.id_EstadoProducto
			left join tbl_Ruta tr on tr.Id_Ruta = tb.id_Ruta
			left join tbl_Usuario tu on tu.Id_Usuario = tb.id_Usuario
		where tb.serial = @serial and tb.chipID = @ChipID and tb.e_eliminado = 0 
		order by tb.fechaRegistro desc
		
GO

/* [dbo].[spx_updateArchivoBajaProducto] */

create proc [dbo].[spx_updateArchivoBajaProducto]( @Id int, @Observacion nvarchar(max), 
		 @Archivo nvarchar(max), @NombreArchivo nvarchar(150))
as
		update tbl_BajaProductos
		set Observacion = @Observacion, 			
			archivo = @Archivo,
			nombrearchivo = @NombreArchivo
		where Id_BajaProductos= @Id and E_Eliminado = 0;
		
		
GO

/* [dbo].[spx_updateArchivoLlamadaAtencion] */
create proc [dbo].[spx_updateArchivoLlamadaAtencion]( @Id int,
		@Archivo nvarchar(max), @NombreArchivo nvarchar(150),
		@ArchivoF nvarchar(max), @NombreArchivoF nvarchar(150),@QueMod int)
as
if(@QueMod =1)--1 modifica archivo firmado
begin
		update tbl_llamadadeatencion
		set ArchivoFirmado = @ArchivoF,
			nombrearchivoFirmado = @NombreArchivoF
		where Id_llamadaDeAtencion = @Id and E_Eliminado = 0;
end
if(@QueMod =0)--1 modifica archivo 
begin
		update tbl_llamadadeatencion
		set Archivo = @Archivo,
			nombrearchivo = @NombreArchivo
		where Id_llamadaDeAtencion = @Id and E_Eliminado = 0;
end

if(@QueMod =2)--2 modifica todo
begin
		update tbl_llamadadeatencion
		set Archivo = @Archivo,
			nombrearchivo = @NombreArchivo,
			ArchivoFirmado = @ArchivoF,
			nombrearchivoFirmado = @NombreArchivoF
		where Id_llamadaDeAtencion = @Id and E_Eliminado = 0;
end

select * from tbl_llamadadeatencion
GO

/* [dbo].[spx_updateDevolucion] */
CREATE proc spx_updateDevolucion( @Id int, @Observacion nvarchar(max), 
		@FechaRegistro datetime, @Archivo nvarchar(max), @NombreArchivo nvarchar(150), @ModArchivo int )
as
	if ( @ModArchivo = 0 ) -- actualizar el detalle o la observacion.
		update tbl_Devolucion
		set Observacion = @Observacion, 
			Fecha_Registro = @FechaRegistro 			
		where Id_Devolucion = @Id and E_Eliminado = 0;
	
	if ( @ModArchivo = 1 ) -- se cambio el archivo
		update tbl_Devolucion
		set Observacion = @Observacion, 
			Fecha_Registro = @FechaRegistro, 
			archivo = @Archivo,
			nombrearchivo = @NombreArchivo
		where Id_Devolucion = @Id and E_Eliminado = 0;

GO

/* [dbo].[spx_UsuarioDebeCambiarPassword] */

CREATE PROC dbo.spx_UsuarioDebeCambiarPassword
    @Login nvarchar(50)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @RequierePorFlag bit = 1;
    DECLARE @DiasVigenciaPassword int = 60;

    SELECT TOP 1
        @RequierePorFlag = RequierePorFlag,
        @DiasVigenciaPassword = DiasVigenciaPassword
    FROM dbo.tbl_configuracion_seguridad
    WHERE Activo = 1
    ORDER BY Id_Configuracion DESC;

    ;WITH U AS
    (
        SELECT TOP 1
            u.Id_Usuario,
            u.Loggin,
            u.Nombre,
            CAST(ISNULL(u.NecesitaCambio, 0) AS bit) AS NecesitaCambio,
            u.UltimaModificacion,
            CASE
                WHEN u.UltimaModificacion IS NULL THEN NULL
                ELSE DATEDIFF(DAY, u.UltimaModificacion, GETDATE())
            END AS DiasDesdeUltimaModificacion
        FROM dbo.tbl_usuario u
        WHERE u.E_Eliminado = 0
          AND u.Loggin = @Login
    )
    SELECT
        U.Id_Usuario,
        U.Loggin,
        U.Nombre,
        U.NecesitaCambio,
        U.UltimaModificacion,
        U.DiasDesdeUltimaModificacion,
        @RequierePorFlag AS RequierePorFlag,
        @DiasVigenciaPassword AS DiasVigenciaPassword,
        CAST(
            CASE
                WHEN @RequierePorFlag = 1 AND U.NecesitaCambio = 1 THEN 1
                WHEN @DiasVigenciaPassword > 0
                     AND ISNULL(U.DiasDesdeUltimaModificacion, 999999) > @DiasVigenciaPassword THEN 1
                ELSE 0
            END
            AS bit
        ) AS DebeCambiar
    FROM U;
END

GO

/* [dbo].[spx_ValidaMovimientos] */

CREATE proc [dbo].[spx_ValidaMovimientos](@fecha datetime)
as 
Begin	
	declare @resultado table(movimiento nvarchar(150),cantidad int)
	declare @FechaUltimoCierre datetime, @cantidadcierres int,@CantidadCierrePRPD int
	set @FechaUltimoCierre = (select Max (Fecha) as Fecha from tbl_cierrealmacen where e_eliminado = 0)	
	set @cantidadcierres = (select COUNT(*) from tbl_CierreAlmacen where E_Eliminado=0)
	
	if (@cantidadcierres = 0)
	begin
		insert into @resultado
		select * from (			
				select  'PEDIDOVENDEDOR - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_PedidoVendedor) Cantidad 
				from tbl_pedidovendedor v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union ALL				
				select  'ALMACENVENDEDOR - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_AlmacenVendedor) Cantidad 
				from tbl_almacenvendedor v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union ALL			
				select  'OT - '+CONVERT(varchar,dbo.DateOnly(v.Fecha_Ejecucion),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Venta) Cantidad 
				from tbl_Venta v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha_ejecucion) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha_ejecucion) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha_Ejecucion)
			Union ALL			
				select  'DEVOLUCION - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Devolucion) Cantidad 
				from tbl_Devolucion v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union ALL			
				select  'BAJA_PRODUCTOS - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(id_BajaProductos) Cantidad 
				from tbl_BajaProductos v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union all				
				select  'CUADRE - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Cuadre) Cantidad 
				from tbl_Cuadre v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union ALL			
				select  'SALIDA_TRASPASOS - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_SalidaTraspaso) Cantidad 
				from tbl_SalidaTraspaso v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union ALL			
				select  'INGRESO - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+V.Proveedor MOVIMIENTO,count(Id_IngresoAlmacen) Cantidad 
					from tbl_IngresoAlmacen v --inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by v.Proveedor, dbo.DateOnly(v.Fecha)
		) Temporal where Cantidad > 0
	end
	else
	begin
		if(dbo.dateonly(@fecha) <> dbo.dateonly(@FechaUltimoCierre))
		begin	
			insert into @resultado
			select * from 
			(				
					select  'PEDIDOVENDEDOR - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_PedidoVendedor) Cantidad 
					from tbl_pedidovendedor v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)
				Union ALL				
					select  'ALMACENVENDEDOR - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_AlmacenVendedor) Cantidad 
					from tbl_almacenvendedor v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)				
				Union ALL			
					select  'VENTA - '+CONVERT(varchar,dbo.DateOnly(v.Fecha_Ejecucion),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Venta) Cantidad 
					from tbl_Venta v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha_Ejecucion) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha_Ejecucion) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha_Ejecucion)
				Union ALL				
					select  'DEVOLUCION - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Devolucion) Cantidad 
					from tbl_devolucion v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)
				Union ALL
					select  'BAJA_PRODUCTOS - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(id_BajaProductos) Cantidad 
					from tbl_BajaProductos v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)
				Union ALL				
					select  'SALIDA_TRASPASOS - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_SalidaTraspaso) Cantidad 
					from tbl_SalidaTraspaso v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)
				Union all				
					select  'CUADRE - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Cuadre) Cantidad 
					from tbl_Cuadre v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)
				Union ALL								
					select  'CIERREALMACEN - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103)  MOVIMIENTO,count(Id_CierreAlmacen) Cantidad 
					from tbl_CierreAlmacen v 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by dbo.DateOnly(v.Fecha)
				Union ALL								
					select  'CIERREALMACENPR_PD - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103)  MOVIMIENTO,count(Id_CierreAlmacenpr_pd) Cantidad 
					from tbl_CierreAlmacenpr_pd v					
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0					
					group by dbo.DateOnly(v.Fecha)
				Union ALL								
					select  'INGRESO - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+V.Proveedor MOVIMIENTO,count(Id_IngresoAlmacen) Cantidad 
					from tbl_IngresoAlmacen v --inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by v.Proveedor, dbo.DateOnly(v.Fecha)
					
			) Temporal where Cantidad > 0
		end
		else
		begin --si la fecha es igual a la del servidor tengo q verifcar que no haya cierre
			
			insert into @resultado
				select 'CIERREALMACEN - '++CONVERT(varchar,dbo.DateOnly(v.Fecha),103) as MOVIMIENTO, count(*) as Cantidad 
				from tbl_CierreAlmacen v where 
				dbo.dateonly(fecha) >= dbo.dateonly(@fecha) and e_eliminado = 0		
				group by dbo.DateOnly(v.Fecha)
				UNION ALL
				select 'CIERREALMACENPR_PD - '++CONVERT(varchar,dbo.DateOnly(v.Fecha),103) as MOVIMIENTO, count(*) as Cantidad 
				from tbl_CierreAlmacenPR_PD v where 
				dbo.dateonly(fecha) >= dbo.dateonly(@fecha) and e_eliminado = 0		
				group by dbo.DateOnly(v.Fecha)
		end
	end	
	SELECT * FROM @resultado 
End




GO

/* [dbo].[spx_ValidaMovimientosSCierre] */

create proc [dbo].[spx_ValidaMovimientosSCierre](@fecha datetime)
as 
Begin	
	declare @resultado table(movimiento nvarchar(150),cantidad int)
	declare @FechaUltimoCierre datetime, @cantidadcierres int,@CantidadCierrePRPD int
	set @FechaUltimoCierre = (select Max (Fecha) as Fecha from tbl_cierrealmacen where e_eliminado = 0)	
	set @cantidadcierres = (select COUNT(*) from tbl_CierreAlmacen where E_Eliminado=0)
	
	if (@cantidadcierres = 0)
	begin
		insert into @resultado
		select * from (			
				select  'PEDIDOVENDEDOR - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_PedidoVendedor) Cantidad 
				from tbl_pedidovendedor v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union ALL			
				select  'OT - '+CONVERT(varchar,dbo.DateOnly(v.Fecha_Ejecucion),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Venta) Cantidad 
				from tbl_Venta v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha_ejecucion) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha_ejecucion) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha_Ejecucion)
			Union ALL			
				select  'DEVOLUCION - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Devolucion) Cantidad 
				from tbl_Devolucion v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union ALL			
				select  'BAJA_PRODUCTOS - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(id_BajaProductos) Cantidad 
				from tbl_BajaProductos v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union all				
				select  'CUADRE - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Cuadre) Cantidad 
				from tbl_Cuadre v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union ALL			
				select  'SALIDA_TRASPASOS - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_SalidaTraspaso) Cantidad 
				from tbl_SalidaTraspaso v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by r.Nombre, dbo.DateOnly(v.Fecha)
			Union ALL			
				select  'INGRESO - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+V.Proveedor MOVIMIENTO,count(Id_IngresoAlmacen) Cantidad 
				from tbl_IngresoAlmacen v --inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
				where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
				group by v.Proveedor, dbo.DateOnly(v.Fecha)
		) Temporal where Cantidad > 0
	end
	else
	begin
		if(dbo.dateonly(@fecha) <> dbo.dateonly(@FechaUltimoCierre))
		begin
			insert into @resultado
			select * from 
			(				
					select  'PEDIDOVENDEDOR - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_PedidoVendedor) Cantidad 
					from tbl_pedidovendedor v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)
				Union ALL			
					select  'VENTA - '+CONVERT(varchar,dbo.DateOnly(v.Fecha_Ejecucion),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Venta) Cantidad 
					from tbl_Venta v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha_Ejecucion) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha_Ejecucion) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha_Ejecucion)
				Union ALL				
					select  'DEVOLUCION - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Devolucion) Cantidad 
					from tbl_devolucion v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)
				Union ALL
					select  'BAJA_PRODUCTOS - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(id_BajaProductos) Cantidad 
					from tbl_BajaProductos v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)
				Union ALL				
					select  'SALIDA_TRASPASOS - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_SalidaTraspaso) Cantidad 
					from tbl_SalidaTraspaso v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)
				Union all				
					select  'CUADRE - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+r.Nombre MOVIMIENTO,count(Id_Cuadre) Cantidad 
					from tbl_Cuadre v inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by r.Nombre, dbo.DateOnly(v.Fecha)				
				Union ALL								
					select  'INGRESO - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103) +' - '+V.Proveedor MOVIMIENTO,count(Id_IngresoAlmacen) Cantidad 
					from tbl_IngresoAlmacen v --inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) > dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					group by v.Proveedor, dbo.DateOnly(v.Fecha)
				UNION ALL
					select  'CIERREALMACENPR_PD - '+CONVERT(varchar,dbo.DateOnly(v.Fecha),103)  MOVIMIENTO,count(Id_CierreAlmacen) Cantidad 
					from tbl_CierreAlmacen v --inner join tbl_Ruta r on r.Id_Ruta=v.Id_Ruta 
					where dbo.dateonly(Fecha) >= dbo.dateonly(@FechaUltimoCierre) and dbo.dateonly(Fecha) < dbo.dateonly(@fecha) and v.e_eliminado = 0
					and CierreAlmacenPR_PD=0
					group by dbo.DateOnly(v.Fecha)
					
					
			) Temporal where Cantidad > 0
		end
		
	end	

	SELECT * FROM @resultado 
End



GO

/* [dbo].[spx_ValidarCuadreRuta] */
CREATE proc dbo.spx_ValidarCuadreRuta(@idRuta int, @fecha datetime )
as
	select count(*) from tbl_Cuadre 
	where dbo.DateOnly( Fecha ) = dbo.DateOnly( @fecha ) 
		and E_Eliminado = 0 and Id_Ruta = @idRuta;		

GO

/* [dbo].[spx_validarusuario] */


CREATE PROC [dbo].[spx_ValidarUsuario]
    @Login nvarchar(50),
    @PasswordHash varchar(50)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP 1
        u.Id_Usuario,
        u.Nombre,
        u.Loggin,
        u.Id_Rol,
        u.NecesitaCambio,
        u.UltimaModificacion,
        u.TipoUsuario,
        u.Id_Empleado,
        u.CodEmpleado,
        u.correo,
        r.nombre NombreRol
    FROM dbo.tbl_usuario u inner join tbl_rol r on r.id_rol=u.id_rol
    WHERE u.E_Eliminado = 0
      AND u.Loggin = @Login
      AND u.Password = @PasswordHash;
END
GO

/* [dbo].[spx_validarusuariosucursal] */
CREATE PROC [dbo].[spx_ValidarUsuarioSucursal]
    @Login nvarchar(50),
    @PasswordHash varchar(50),
    @Id_Sucursal int
AS
BEGIN
    SET NOCOUNT ON;

    SELECT TOP 1
        u.Id_Usuario,
        u.Nombre,
        u.Loggin,
        u.Id_Rol,
        u.NecesitaCambio,
        u.UltimaModificacion,
        u.TipoUsuario,
        u.Id_Empleado,
        u.CodEmpleado,
        u.correo,
        us.Id_Sucursal
    FROM dbo.tbl_usuario u
    INNER JOIN dbo.tbl_UsuarioSucursal us
        ON us.Id_Usuario = u.Id_Usuario
       AND us.E_Eliminado = 0
       AND us.Id_Sucursal = @Id_Sucursal
    WHERE u.E_Eliminado = 0
      AND u.Loggin = @Login
      AND u.Password = @PasswordHash;
END

GO

/* [dbo].[spx_validarventaydetallewb] */
CREATE PROCEDURE [dbo].[spx_validarventaydetallewb] AS BEGIN SET NOCOUNT ON; END
GO

/* [dbo].[spx_VerificarEstadoSerie] */
--spx_VerificarEstadoSerie '017981809100','',116,7,1,0

CREATE proc [dbo].[spx_VerificarEstadoSerie]( 
	@serie nvarchar( 150 ),  -- serie del producto/material a verificar.
	@ChipId  nvarchar( 150 ),
	@Id_Producto int, 		-- llave del producto/material
	@Accion int, 			-- acción a realizar
	@InstRetirado int, 		-- 
	@Id_Ruta int 			-- ruta del producto/material a buscar.
	)
as
	--1 es para ingreso
	
	declare @contador int,@contadorChipId int,@Observacion nvarchar(150),@ObservacionChipID nvarchar(150)	
	declare @tablaResultados table(PuedeRegistrar nvarchar(25),Observacion nvarchar(150))
	declare @PuedeRegistrar nvarchar(50)
	declare @contadorss int=0,@contadorsc int=0,@contadorcs int=0, @contadorcc int=0,@cantidadDigitosSerial int =0
	declare @cantidadDigitosChipId int
	set @PuedeRegistrar='NoSePuedeRegistrar';
	set @observacion='Verifique el dato ingresado';
	declare @SePuedeRegistrar int=0
	set @cantidadDigitosSerial = (select DigitosImei from tbl_producto where id_producto=@Id_Producto )
	set @cantidadDigitosChipId = (select DigitosChipId from tbl_producto where id_producto=@Id_Producto )
	
	if(@cantidadDigitosSerial=0 and @cantidadDigitosChipId=0)
	begin
		set @PuedeRegistrar='SePuedeRegistrar'; 
	end
	else
	begin
		if(len(@serie)>0)
		begin
			set @contadorss = (select COUNT(*) from tbl_productos where serial=@serie and e_eliminado = 0 )---ok
			set @contadorcs = (select COUNT(*) from tbl_productos where chipid=@serie and e_eliminado = 0 )
		end	
		if(len(@ChipId)>0)
		begin
			set @contadorsc = (select COUNT(*) from tbl_productos where serial=@ChipId and e_eliminado = 0 )
			set @contadorcc = (select COUNT(*) from tbl_productos where chipid=@ChipId and e_eliminado = 0 )--ok
		end 	
	
		if(@cantidadDigitosSerial>0 and @cantidadDigitosChipId>0)
		begin	
			if(@contadorss=1 and  @contadorsc=0 and @contadorcc=1 and  @contadorcs=0)
			begin		
				set @contador = (select count(*) from tbl_productos where serial=@serie and chipid=@ChipId and e_eliminado=0);			
				if(@contador=1)
				begin			
					set @SePuedeRegistrar=1;
				end
				else 
				begin
				--select @contador 
					set @Observacion='Serie/ChipID con otro Tipo de Producto'
					set @PuedeRegistrar='NoSePuedeRegistrar';	
				end
			end
			else
			begin
				if(@contadorss=0 and  @contadorsc=0 and @contadorcc=0 and  @contadorcs=0 and @InstRetirado=2)
				begin
					set @SePuedeRegistrar=1;
				end
				else 
				begin
		
				set @SePuedeRegistrar=2;
				set @Observacion='Serie/ChipID registrado más de 1 vez.'
				set @PuedeRegistrar='NoSePuedeRegistrar';
				end			
			end		
	end 
	
	if(@cantidadDigitosChipId<=0 and len(@ChipId)<=0)
	begin			
	--select 	@cantidadDigitosChipId,len(@ChipId)
		if(@contadorss=1 and  @contadorcs=0)
		begin	
		--select 	@contadorss
		--select 	@contadorcs
			set @contador = (select count(*) from tbl_productos where serial=@serie  and e_eliminado=0)			
			if(@contador=1)
			begin	
				set @SePuedeRegistrar=1;
				set @Observacion='';				
			end
			else 
			begin				
				set @Observacion='Serie/ChipID con otro Tipo de Producto'
				set @PuedeRegistrar='NoSePuedeRegistrar';	
				
			end
		end		
		else
		begin
			if(@InstRetirado =2 or @InstRetirado =0)
			begin
				set @contador = (select count(*) from tbl_productos where serial=@serie  and e_eliminado=0)
				if(@contador =0)
					set @SePuedeRegistrar=1;
			end
			else
			begin
				set @Observacion='Serie/ChipID registrado más de 1 vez-'
				set @PuedeRegistrar='NoSePuedeRegistrar';	
				
			end
		end		
	end

if( @SePuedeRegistrar=1 or @SePuedeRegistrar=2)
begin
	if(@Accion = 1 ) -- validaciones para el ingreso
	begin		
	
		if(@SePuedeRegistrar=2 and @contadorss=0 and  @contadorsc=0 and @contadorcc=0 and  @contadorcs=0)
		begin
			set @PuedeRegistrar = 'SePuedeRegistrar'	
			set @Observacion=''
		end
		
		if(@SePuedeRegistrar=1 )
		begin	
		
--		select * from tbl_productos where serial=	'588888888888'
 
			set @contador = (select count(*) from tbl_productos where serial=@serie and chipid=@ChipId and id_producto = @id_producto 
							and e_eliminado=0)
			if(@contador=1)
			begin			
				if(@cantidadDigitosChipId<=0)
					set @contador = (select count(*) from tbl_productos where serial=@serie and id_producto = @id_producto 
								and e_eliminado=0 and id_estadoproducto in (3,7,8,12,15,16,20))	
				else
					set @contador = (select count(*) from tbl_productos where serial=@serie and chipid=@chipid and id_producto = @id_producto 
								and e_eliminado=0 and id_estadoproducto in (3,7,8,12,15,16,20))	
								
				if(@contador=1)
				begin
					set @PuedeRegistrar = 'SePuedeRegistrar'
					set @Observacion=''	
				end 
				else 
				begin
					set @Observacion='Serie/ChipID registrado con Estado No Permitido'
					set @PuedeRegistrar='NoSePuedeRegistrar';	
				end 	
			end
			else 
			begin			
				if(@contadorss=0 and  @contadorcs=0 and @contadorsc=0 and @contadorcc=0 and @Accion=1)
				begin
					set @PuedeRegistrar='SePuedeRegistrar';	
					set @Observacion=''
				end 
				else
				begin
					set @Observacion='Serie/ChipID registrado con otro Tipo de Producto'
					set @PuedeRegistrar='NoSePuedeRegistrar';	
				end
			end	
		end
	end

	if(@Accion = 2 and @SePuedeRegistrar=1)--entregavendedor
	--se puede entregar si esta con estado 1 en almacen	
	begin	
		
			set @contador = (select COUNT(*) from tbl_productos where serial =@serie and chipid=@chipId and id_producto=@id_producto 
							and e_eliminado = 0)
			if((@contador)=1)
			begin			
				set @contador = (select count(*) from tbl_productos where serial=@serie and chipid=@ChipId and id_producto = @id_producto 
							and e_eliminado=0 and id_estadoproducto in (1,6))	
				if(@contador=1)
				begin
					set @PuedeRegistrar = 'SePuedeRegistrar'	
					set @Observacion=''
				end 
				else 

				begin
					set @Observacion='Serie/ChipID registrado con Estado No Permitido'
					set @PuedeRegistrar='NoSePuedeRegistrar';	
				end 	
			end
			else 
			begin
				set @PuedeRegistrar='NoSePuedeRegistrar';
				set @Observacion ='Serie/ChipID registrado con otro Tipo de Producto'				
			end	
	end
	

--3 Registrar OrdenTrabajo	select * from tbl_estadoproducto  
--si no existe en almacen
--si su ultimo movimiento fue 3 si estuvo instalado
--7 entregado a tigo 
--select * from tbl_EstadoProducto 
	if(@Accion = 3 and @SePuedeRegistrar=1)	
	--se puede entregar si esta con estado 1 en almacen	
	begin		 	
				if(@InstRetirado <> 2 )--instalado excedenete reposiin es material gastado de la ruta
				begin
				
					set @contador = (select COUNT(*) from tbl_productos where serial =@serie and chipId=@chipId and id_producto=@id_producto  and e_eliminado = 0 )
					
					if((@contador)=1)
					begin
					
						set @Observacion = (select 'Estado: Serial' + ep.Nombre + ' - '+ r.Nombre
											from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
											inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
											and serial =@serie and chipId=@ChipId and id_producto=@Id_Producto and ep.Id_EstadoProducto in(2) and s.id_Ruta =@Id_Ruta)
						--select @Observacion
						if(LEN(@Observacion)>0)
						begin
							set @PuedeRegistrar ='SePuedeRegistrar';	
							set @Observacion=''		 
						end
						else
						begin
								set @PuedeRegistrar='NoSePuedeRegistrar';
								set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre
													from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
													inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
													and serial =@serie and chipId =@chipId and id_producto=@Id_Producto )					
						end 
					end				
					else
					begin
					-- set @Observacion='No existe el producto'
						set @PuedeRegistrar='NoSePuedeRegistrar';
						set @Observacion ='Serie/ChipID registrado con otro Tipo de Producto'
					end
				end
				else --esto es para retiro
				begin		
						
						set @contador = (select COUNT(*) from tbl_productos where serial=@serie and chipid=@chipid and id_producto=@Id_Producto  and e_eliminado = 0 )
						if((@contador)>0)
						begin			--existe el prod
							set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre
												from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
												inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
												and serial =@serie and chipId=@ChipId and id_producto=@Id_Producto and ep.Id_EstadoProducto in(3, 7, 8,12,15,16,21))
												
							if(LEN(@Observacion)>0)
							begin
								set @Observacion=''
								set @PuedeRegistrar ='SePuedeRegistrar';	
							end
							else 
							begin
								set @PuedeRegistrar ='NoSePuedeRegistrar';						
								set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre
												from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
												inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
												and serial =@serie and chipId=@ChipId and id_producto=@Id_Producto)
								if(LEN(@Observacion)<0)
									set @Observacion ='Error'
							end
						end
						else	
						begin	
								if((select DigitosChipId from tbl_producto where Id_Producto=@Id_Producto and E_Eliminado=0)>0)---es deco
								begin
									set @contador = (select COUNT(*) from tbl_productos where chipId = @ChipId and id_producto=@id_producto and e_eliminado = 0 )
									if(@contador<=0)	
									begin		
										set @PuedeRegistrar = 'SePuedeRegistrar'										
										set @Observacion=''
									end
								end
								else 
								begin
									set @PuedeRegistrar = 'SePuedeRegistrar'	
									set @Observacion=''
								end
										
								if(@PuedeRegistrar='NoSePuedeRegistrar')
									set @Observacion ='Verificar ChipID'
						end
											
				end
	end

	if(@Accion = 5 and @SePuedeRegistrar=1)--productos dañados y excedente en ruta
	--se puede entregar si esta con estado 1 en almacen	
	begin	--select * from tbl_estadoproducto
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and chipid =@chipid and id_producto=@id_producto  and e_eliminado = 0 )
		if((@contador)=1)		
		begin			
			set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
								from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
								 and serial =@serie and chipid =@chipid and id_producto=@Id_Producto and ep.Id_EstadoProducto in(2) and r.Id_Ruta =@Id_Ruta)
			if(LEN(@Observacion)>0)
			begin
				set @PuedeRegistrar ='SePuedeRegistrar';			 
				set @Observacion=''
			end
			else
	
			begin
				set @PuedeRegistrar = 'NoSePuedeRegistrar'
					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
										and serial =@serie and id_producto=@Id_Producto and id_producto=@Id_Producto )
			end
		end
	end 
	
	if(@Accion = 6 and @SePuedeRegistrar=1)--ENTREGA TIGO PRODUCTOS RETIRADOS
	begin		
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and chipid=@chipid and id_producto= @id_producto  and e_eliminado = 0 )
		if((@contador)>0)
		begin
			set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								 and serial =@serie and chipid=@chipid and id_producto=@id_producto and ep.Id_EstadoProducto in (4,14))--(4,5, 10))
			if(LEN(@Observacion)>0)
			begin
				set @PuedeRegistrar ='SePuedeRegistrar';			 
				set @Observacion=''
			end
			else
			begin
				set @PuedeRegistrar = 'NoSePuedeRegistrar'
					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
										and serial =@serie and chipid=@chipid and id_producto=@Id_Producto )
			end
		end	
		else
		begin
			set @Observacion = 'Verificar el Producto'
		end 
	end

	if(@Accion = 15 and @SePuedeRegistrar=1)--devolucion a Tigo PRODUCTOS DAÑADOS	
	begin
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and chipid=@chipid and id_producto= @id_producto  and e_eliminado = 0 )
		if((@contador)=1)
		begin
			set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								 and serial =@serie and chipid=@chipid and id_producto=@id_producto and ep.Id_EstadoProducto in(5,13))
			if(LEN(@Observacion)>0)
			begin
				set @PuedeRegistrar ='SePuedeRegistrar';			 
				set @Observacion=''
			end
			else
			begin
				set @PuedeRegistrar = 'NoSePuedeRegistrar'
					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
										and serial =@serie and chipid=@chipid and id_producto=@Id_Producto )
			end
		end		
		else
		begin
			set @Observacion='Verificar el Producto'
			set @PuedeRegistrar='NoSePuedeRegistrar';	
		end 
	end

	if(@Accion = 7 and @SePuedeRegistrar=1)--Baja Producto
	--se puede entregar si esta con estado 1 en almacen	
	begin	
	
		declare @idestado int		
		set @idestado =(select id_Estadoproducto from tbl_productos where serial=@serie and chipid=@chipid and e_eliminado=0)
		
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and chipid=@chipid  and id_producto= @id_producto  and e_eliminado = 0 )
		if((@contador)=1)
		begin

			if(@InstRetirado=4)--4
			begin
				set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								 and serial =@serie and chipid=@chipid and id_producto=@id_producto and ep.Id_EstadoProducto in(4) )
			end
			if(@InstRetirado=1)--4
			begin
				set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								 and serial =@serie and chipid=@chipid and id_producto=@id_producto and ep.Id_EstadoProducto in(1,6,10) )
			end
			if(@InstRetirado=2 or @InstRetirado=5)--4
			begin
				set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								 and serial =@serie and chipid=@chipid and id_producto=@id_producto and ep.Id_EstadoProducto in(2, 5) 
								 and s.id_ruta=@Id_ruta and s.id_Estadoproducto=@instRetirado)
		
			end 			

			if(LEN(@Observacion)>0)
			begin
				set @PuedeRegistrar ='SePuedeRegistrar';			 
				set @Observacion=''
			end
			else
			begin
				set @PuedeRegistrar = 'NoSePuedeRegistrar'
					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
										and serial =@serie and chipid=@chipid and id_producto=@Id_Producto )
			end
		end
		else
		begin
			set @Observacion='Serie/ChipID registrado con Estado No Permitido'
			set @PuedeRegistrar='NoSePuedeRegistrar';
		end 
	end

	if(@Accion = 8 and @SePuedeRegistrar=1)--Recibir Producto no entregado de una OT
	begin	
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and chipid=@chipid and id_producto= @id_producto  and e_eliminado = 0 )
		if((@contador)=1)
		begin
			set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								 and serial =@serie and chipid=@chipid and id_producto=@id_producto and ep.Id_EstadoProducto in(11))
			if(LEN(@Observacion)>0)
			begin
				set @PuedeRegistrar ='SePuedeRegistrar';			 
				set @Observacion=''
			end
			else
			begin
				set @PuedeRegistrar = 'NoSePuedeRegistrar'
					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
										and serial =@serie and chipid=@chipid and id_producto=@Id_Producto )
			end
		end		
		else
		begin
				set @Observacion='Serie/ChipID registrado con Estado No Permitido'
				set @PuedeRegistrar='NoSePuedeRegistrar';	
		end 
	end


	if(@Accion = 9 and @SePuedeRegistrar=1)--salida traspaso
	--se puede entregar si esta con estado 1 en almacen	
	begin	
	
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and chipid=@chipid and id_producto= @id_producto  and e_eliminado = 0 )
		--select @contador 
		if(@contador=1)
		begin
			set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								 and serial =@serie and chipid=@chipid and id_producto=@id_producto and ep.Id_EstadoProducto in(1,6))
			if(LEN(@Observacion)>0)
			begin
				set @PuedeRegistrar ='SePuedeRegistrar';	
				set @Observacion=''
			end
			else
			begin
				set @PuedeRegistrar = 'NoSePuedeRegistrar'
					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
										and serial =@serie  and chipid=@chipid and id_producto=@Id_Producto )
			end
		end
		else
		begin
			set @PuedeRegistrar = 'NoSePuedeRegistrar'
			set @Observacion = 'No existe el producto'
		end 
	end
	
	
end		
end
----
	insert into @tablaResultados select @PuedeRegistrar,@Observacion
	select * from @tablaResultados 




--select * from tbl_estadoproducto --4 RETIRADO_OT 14 RECHAZADO_ENTREGADO_TIGORETIRADO
--select * from tbl_productos where serial='017981329760'
--update tbl_productos set id_estadoproducto=8 where serial='017981329760'


GO

/* [dbo].[spx_VerificarEstadoSerie2] */

CREATE proc [dbo].[spx_VerificarEstadoSerie2]( 
	@serie nvarchar( 20 ),  -- serie del producto/material a verificar.
	@ChipId  nvarchar( 20 ),
	@Id_Producto int, 		-- llave del producto/material
	@Accion int, 			-- acción a realizar
	@InstRetirado int, 		-- 
	@Id_Ruta int 			-- ruta del producto/material a buscar.
	)
as
	--1 es para ingreso
	declare @tablaResultados table(PuedeRegistrar nvarchar(25),Observacion nvarchar(150))
	declare @PuedeRegistrar nvarchar(50)
	set @PuedeRegistrar='NoSePuedeRegistrar';
	if(@Accion = 1) -- validaciones para el ingreso
		--se puede dar de baja nuevamente si no existe,
		--si su ultimo estado era 3(estaba instalado fue recogido x otros y tigo lo volvio a entregar
		--o su ultimo estado era 7 entregado a tigo
	begin
	declare @contador int
	declare @Observacion nvarchar(150)	
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto and e_eliminado = 0 and id_EstadoProducto not in (3, 7, 8) )
		if((@contador)>0)
		begin			
			set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
								from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta and serial =@serie and ChipId=@ChipId and id_producto=@Id_Producto )

			set @PuedeRegistrar ='NoSePuedeRegistrar';			 			

		end

		else	-- producto no existe en almacen.			

			set @PuedeRegistrar = 'SePuedeRegistrar'					

	end

	

	if(@Accion = 2)--entregavendedor

	--se puede entregar si esta con estado 1 en almacen	

	begin	

		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto)

		if((@contador) > 0 )

		begin

			set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto  and e_eliminado = 0 and Id_EstadoProducto in(1,6))

			if((@contador)=0)

			begin			

				set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre

									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

									and serial =@serie and id_producto=@Id_Producto

									 and serial =@serie and id_producto=@Id_Producto)

				-- if(LEN(@Observacion)=0)					

				--	set @Observacion  ='El producto no existe en Almacen';											

			end

			else set @PuedeRegistrar='SePuedeRegistrar';

		end

		else set @Observacion  ='El producto no existe en Almacen';	

	end

--3 Registrar OrdenTrabajo	select * from tbl_estadoproducto  

--si no existe en almacen

--si su ultimo movimiento fue 3 si estuvo instalado

--7 entregado a tigo 

	if(@Accion = 3)

	--se puede entregar si esta con estado 1 en almacen	

	begin		

		if(@InstRetirado <> 2 )--instalado excedenete reposiin es material gastado de la ruta

		begin

			set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto  and e_eliminado = 0 )

			if((@contador)>0)

			begin

				set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre

									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

										and serial =@serie and id_producto=@Id_Producto

									and serial =@serie and id_producto=@Id_Producto and ep.Id_EstadoProducto in(2) and s.id_Ruta =@Id_Ruta)

				if(LEN(@Observacion)>0)

					set @PuedeRegistrar ='SePuedeRegistrar';			 

				else

				begin

				--	set @PuedeRegistrar = 'NoSePuedeRegistrar'

						set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre

											from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

											inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

											and serial =@serie and id_producto=@Id_Producto )					




				end 

			end

			else set @Observacion='No existe el producto'

		end

		else --select * from tbl_estadoproducto

		begin		

			set @contador = (select COUNT(*) from tbl_productos where serial=@serie and id_producto=@Id_Producto  and e_eliminado = 0 )

			if((@contador)>0)

			begin			

				set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre

									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

										and serial =@serie and id_producto=@Id_Producto

									and serial =@serie and id_producto=@Id_Producto and ep.Id_EstadoProducto in(3, 7, 8))

				--print @observacion

				if(LEN(@Observacion)>0)	

					set @PuedeRegistrar ='SePuedeRegistrar';	

				else 

				begin

					set @PuedeRegistrar ='NoSePuedeRegistrar';						

					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre

									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

									and serial =@serie and id_producto=@Id_Producto

									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

									and serial =@serie and id_producto=@Id_Producto)

				end

			end--puede entrar por asistencia

			else

			begin

					set @PuedeRegistrar ='SePuedeRegistrar';	--se puede registrar xq nunca existio

			end

				

		end

	end

	

	if(@Accion = 5)--devolucion x excedente @TipoDev select * from tbl_tipodevolucion

	--se puede entregar si esta con estado 1 en almacen	

	begin	--select * from tbl_estadoproducto

	

		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto  and e_eliminado = 0 )

		if((@contador)>0)		

		begin			

			set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 

								from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

								inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

								 and serial =@serie and id_producto=@Id_Producto and ep.Id_EstadoProducto in(2) and r.Id_Ruta =@Id_Ruta)

			if(LEN(@Observacion)>0)

				set @PuedeRegistrar ='SePuedeRegistrar';			 

			else

			begin

				set @PuedeRegistrar = 'NoSePuedeRegistrar'

					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 

										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

										and serial =@serie and id_producto=@Id_Producto )

			end

		end

	end

	if(@Accion = 6)--devolucion a Tigo

	--se puede entregar si esta con estado 1 en almacen	

	begin	--select * from tbl_estadoproducto  select * from tbl_productos

	

		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto= @id_producto  and e_eliminado = 0 )

		if((@contador)>0)

		begin

			set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

								 and serial =@serie and id_producto=@id_producto and ep.Id_EstadoProducto in(4,5, 10))

			if(LEN(@Observacion)>0)

				set @PuedeRegistrar ='SePuedeRegistrar';			 

			else

			begin

				set @PuedeRegistrar = 'NoSePuedeRegistrar'

					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 

										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

										and serial =@serie and id_producto=@Id_Producto )

			end

		end

		else

		begin

			set @Observacion = 'No existe el producto'

		end 

		

	end




	if(@Accion = 7)--Baja Producto

	--se puede entregar si esta con estado 1 en almacen	

	begin	--select * from tbl_estadoproducto  select * from tbl_productos

	

		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto= @id_producto  and e_eliminado = 0 )

		if((@contador)>0)

		begin

			set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

								 and serial =@serie and id_producto=@id_producto and ep.Id_EstadoProducto in(1, 2, 4, 5, 6, 10))

			if(LEN(@Observacion)>0)

				set @PuedeRegistrar ='SePuedeRegistrar';			 

			else

			begin

				set @PuedeRegistrar = 'NoSePuedeRegistrar'

					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 

										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

										and serial =@serie and id_producto=@Id_Producto )

			end

		end

		else

		begin

			set @Observacion = 'No existe el producto'

		end 

		

	end

	insert into @tablaResultados select @PuedeRegistrar,@Observacion

	select * from @tablaResultados 
GO

/* [dbo].[spx_VerificarEstadoSerieAntesAccion3] */
--spx_VerificarEstadoSerie 'TIG-5252-525253','52525252222',1,3,2,3
create proc [dbo].[spx_VerificarEstadoSerieAntesAccion3]( 
	@serie nvarchar( 20 ),  -- serie del producto/material a verificar.
	@ChipId  nvarchar( 20 ),
	@Id_Producto int, 		-- llave del producto/material
	@Accion int, 			-- acción a realizar
	@InstRetirado int, 		-- 
	@Id_Ruta int 			-- ruta del producto/material a buscar.
	)
as
	--1 es para ingreso
	declare @tablaResultados table(PuedeRegistrar nvarchar(25),Observacion nvarchar(150))
	declare @PuedeRegistrar nvarchar(50)
	set @PuedeRegistrar='NoSePuedeRegistrar';
	if(@Accion = 1) -- validaciones para el ingreso
	begin
		--se puede dar de baja nuevamente si no existe,
		--si su ultimo estado era 3(estaba instalado fue recogido x otros y tigo lo volvio a entregar
		--o su ultimo estado era 7 entregado a tigo	
	declare @contador int
	declare @contadorChipId int
	declare @Observacion nvarchar(150)		
	declare @ObservacionChipID nvarchar(150)	
		
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@Id_Producto and e_eliminado = 0 )
		if(@contador>0)
		begin						
				set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@Id_Producto and e_eliminado = 0 and id_EstadoProducto not in (3, 7, 8) )
					if(@contador>0)
					begin		
						set @Observacion = (select 'Estado Serial: ' + ep.Nombre + ' - '+ r.Nombre 
											from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
											inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta and serial =@serie and id_producto=@Id_Producto )								
						set @PuedeRegistrar ='NoSePuedeRegistrar';			 			
					end				
					else	-- producto no existe en almacen.			aqui verifica si es tarnea
					begin					
						if((select DigitosChipId from tbl_producto where Id_Producto=@Id_Producto and E_Eliminado=0)>0)---es deco
						begin						
							set @contador = (select COUNT(*) from tbl_productos where serial=@serie and ChipId=@ChipId and id_producto=@Id_Producto and e_eliminado = 0)	
							if(@contador>0)
							begin
									set @contador = (select COUNT(*) from tbl_productos where serial =@serie and ChipId=@ChipId and id_producto=@Id_Producto and e_eliminado = 0 and id_EstadoProducto not in (3, 7, 8) )				
									if(@contador>1)
									begin
										set @Observacion = (select 'Estado Serial: ' + ep.Nombre + ' - '+ r.Nombre 
														from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
														inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta and serial =@serie and id_producto=@Id_Producto )								
										set @PuedeRegistrar ='NoSePuedeRegistrar';		
									end
									else 
										set @PuedeRegistrar ='SePuedeRegistrar';		
							end							
							else 
							begin
									set @Observacion ='El producto existe pero No coincide Serie != ChipID'
									set @PuedeRegistrar ='NoSePuedeRegistrar';		
							end
						end	
						else 				
						set @PuedeRegistrar = 'SePuedeRegistrar'	
					end
		end
		else 
		begin		
			if((select DigitosChipId from tbl_producto where Id_Producto=@Id_Producto and E_Eliminado=0)>0)---es deco
			begin
				set @contador = (select COUNT(*) from tbl_productos where chipId = @ChipId and id_producto=@id_producto and e_eliminado = 0 )
				if(@contador<=0)			
					set @PuedeRegistrar = 'SePuedeRegistrar'	
			end
			else 
					set @PuedeRegistrar = 'SePuedeRegistrar'	
		end
	end
	if(@Accion = 2)--entregavendedor
	--se puede entregar si esta con estado 1 en almacen	
	begin	
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto)
		if((@contador) > 0 )
		begin
			set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto  and e_eliminado = 0 and Id_EstadoProducto in(1,6))
			if((@contador)=0)
			begin			
				set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre
									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
									and serial =@serie and id_producto=@Id_Producto
									 and serial =@serie and id_producto=@Id_Producto)							
			end
			else set @PuedeRegistrar='SePuedeRegistrar';
		end
		else set @Observacion  ='El producto no existe en Almacen';	
	end

--3 Registrar OrdenTrabajo	select * from tbl_estadoproducto  
--si no existe en almacen
--si su ultimo movimiento fue 3 si estuvo instalado
--7 entregado a tigo 
	if(@Accion = 3)
	--se puede entregar si esta con estado 1 en almacen	
	begin		 --select * from tbl_productos
		if(@InstRetirado <> 2 )--instalado excedenete reposiin es material gastado de la ruta
		begin
			set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto  and e_eliminado = 0 )
			if((@contador)>0)
			begin
				set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre
									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
									and serial =@serie and chipId =@chipId and id_producto=@Id_Producto and ep.Id_EstadoProducto in(2) and s.id_Ruta =@Id_Ruta)
				if(LEN(@Observacion)>0)
					set @PuedeRegistrar ='SePuedeRegistrar';			 
				else
				begin
						set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre
											from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
											inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
											and serial =@serie and chipId =@chipId and id_producto=@Id_Producto )					
				end 
			end
			else set @Observacion='No existe el producto'
		end
		else 
		begin	
			set @contador = (select COUNT(*) from tbl_productos where serial=@serie and chipId=@ChipId and id_producto=@Id_Producto  and e_eliminado = 0 )
			if((@contador)>0)
			begin			
				set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre
									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
									and serial =@serie and chipId=@ChipId and id_producto=@Id_Producto and ep.Id_EstadoProducto in(3, 7, 8))
				if(LEN(@Observacion)>0)	
					set @PuedeRegistrar ='SePuedeRegistrar';	
				else 
				begin
					set @PuedeRegistrar ='NoSePuedeRegistrar';						
					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre
									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
									and serial =@serie and chipId=@ChipId and id_producto=@Id_Producto)
				end
			end--puede entrar por asistencia
			else
			begin
					set @PuedeRegistrar ='SePuedeRegistrar';	--se puede registrar xq nunca existio
			end
		end
	end
	if(@Accion = 5)--devolucion x excedente @TipoDev select * from tbl_tipodevolucion
	--se puede entregar si esta con estado 1 en almacen	
	begin	--select * from tbl_estadoproducto
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto  and e_eliminado = 0 )
		if((@contador)>0)		
		begin			
			set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
								from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
								 and serial =@serie and id_producto=@Id_Producto and ep.Id_EstadoProducto in(2) and r.Id_Ruta =@Id_Ruta)
			if(LEN(@Observacion)>0)
				set @PuedeRegistrar ='SePuedeRegistrar';			 
			else
			begin
				set @PuedeRegistrar = 'NoSePuedeRegistrar'
					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
										and serial =@serie and id_producto=@Id_Producto )
			end
		end
	end
	if(@Accion = 6)--devolucion a Tigo
	--se puede entregar si esta con estado 1 en almacen	
	begin	--select * from tbl_estadoproducto  select * from tbl_productos
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto= @id_producto  and e_eliminado = 0 )
		if((@contador)>0)
		begin
			set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								 and serial =@serie and id_producto=@id_producto and ep.Id_EstadoProducto in(4,5, 10))
			if(LEN(@Observacion)>0)
				set @PuedeRegistrar ='SePuedeRegistrar';			 
			else
			begin
				set @PuedeRegistrar = 'NoSePuedeRegistrar'
					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
										and serial =@serie and id_producto=@Id_Producto )
			end
		end
		else
		begin
			set @Observacion = 'No existe el producto'
		end 
	end
	if(@Accion = 7)--Baja Producto
	--se puede entregar si esta con estado 1 en almacen	
	begin	--select * from tbl_estadoproducto  select * from tbl_productos
		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto= @id_producto  and e_eliminado = 0 )
		if((@contador)>0)
		begin
			set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
								 and serial =@serie and id_producto=@id_producto and ep.Id_EstadoProducto in(1, 2, 4, 5, 6, 10))
			if(LEN(@Observacion)>0)
				set @PuedeRegistrar ='SePuedeRegistrar';			 
			else
			begin
				set @PuedeRegistrar = 'NoSePuedeRegistrar'
					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 
										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0
										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 
										and serial =@serie and id_producto=@Id_Producto )
			end
		end
		else
		begin
			set @Observacion = 'No existe el producto'
		end 
	end
	insert into @tablaResultados select @PuedeRegistrar,@Observacion
	select * from @tablaResultados 
GO

/* [dbo].[spx_VerificarEstadoSerieOriginal] */
CREATE proc [dbo].[spx_VerificarEstadoSerieOriginal]( 

	@serie nvarchar( 20 ),  -- serie del producto/material a verificar.

	@Id_Producto int, 		-- llave del producto/material

	@Accion int, 			-- acción a realizar

	@InstRetirado int, 		-- 

	@Id_Ruta int 			-- ruta del producto/material a buscar.

	)

as

	--1 es para ingreso

	declare @tablaResultados table(PuedeRegistrar nvarchar(25),Observacion nvarchar(150))

	declare @PuedeRegistrar nvarchar(50)

	set @PuedeRegistrar='NoSePuedeRegistrar';

	if(@Accion = 1) -- validaciones para el ingreso

		--se puede dar de baja nuevamente si no existe,

	--si su ultimo estado era 3(estaba instalado fue recogido x otros y tigo lo volvio a entregar

	--o su ultimo estado era 7 entregado a tigo

	begin

	declare @contador int

	declare @Observacion nvarchar(150)

		

		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto and e_eliminado = 0 and id_EstadoProducto not in (3, 7, 8) )

		if((@contador)>0)

		begin			
		print 'df'
			set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 

								from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

								inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

								 and serial =@serie and id_producto=@Id_Producto

								 and serial = @serie and id_producto=@Id_Producto )

			set @PuedeRegistrar ='NoSePuedeRegistrar';			 			

		end

		else	-- producto no existe en almacen.			

			set @PuedeRegistrar = 'SePuedeRegistrar'					

	end

	

	if(@Accion = 2)--entregavendedor

	--se puede entregar si esta con estado 1 en almacen	

	begin	

		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto)

		if((@contador) > 0 )

		begin

			set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto  and e_eliminado = 0 and Id_EstadoProducto in(1,6))

			if((@contador)=0)

			begin			

				set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre

									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

									and serial =@serie and id_producto=@Id_Producto

									 and serial =@serie and id_producto=@Id_Producto)

				-- if(LEN(@Observacion)=0)					

				--	set @Observacion  ='El producto no existe en Almacen';											

			end

			else set @PuedeRegistrar='SePuedeRegistrar';

		end

		else set @Observacion  ='El producto no existe en Almacen';	

	end

--3 Registrar OrdenTrabajo	select * from tbl_estadoproducto  

--si no existe en almacen

--si su ultimo movimiento fue 3 si estuvo instalado

--7 entregado a tigo 

	if(@Accion = 3)

	--se puede entregar si esta con estado 1 en almacen	

	begin		

		if(@InstRetirado <> 2 )--instalado excedenete reposiin es material gastado de la ruta

		begin

			set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto  and e_eliminado = 0 )

			if((@contador)>0)

			begin

				set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre

									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

										and serial =@serie and id_producto=@Id_Producto

									and serial =@serie and id_producto=@Id_Producto and ep.Id_EstadoProducto in(2) and s.id_Ruta =@Id_Ruta)

				if(LEN(@Observacion)>0)

					set @PuedeRegistrar ='SePuedeRegistrar';			 

				else

				begin

				--	set @PuedeRegistrar = 'NoSePuedeRegistrar'

						set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre

											from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

											inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

											and serial =@serie and id_producto=@Id_Producto )					




				end 

			end

			else set @Observacion='No existe el producto'

		end

		else --select * from tbl_estadoproducto

		begin		

			set @contador = (select COUNT(*) from tbl_productos where serial=@serie and id_producto=@Id_Producto  and e_eliminado = 0 )

			if((@contador)>0)

			begin			

				set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre

									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

										and serial =@serie and id_producto=@Id_Producto

									and serial =@serie and id_producto=@Id_Producto and ep.Id_EstadoProducto in(3, 7, 8))

				--print @observacion

				if(LEN(@Observacion)>0)	

					set @PuedeRegistrar ='SePuedeRegistrar';	

				else 

				begin

					set @PuedeRegistrar ='NoSePuedeRegistrar';						

					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre

									from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

									and serial =@serie and id_producto=@Id_Producto

									inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

									and serial =@serie and id_producto=@Id_Producto)

				end

			end--puede entrar por asistencia

			else

			begin

					set @PuedeRegistrar ='SePuedeRegistrar';	--se puede registrar xq nunca existio

			end

				

		end

	end

	

	if(@Accion = 5)--devolucion x excedente @TipoDev select * from tbl_tipodevolucion

	--se puede entregar si esta con estado 1 en almacen	

	begin	--select * from tbl_estadoproducto

	

		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto=@id_producto  and e_eliminado = 0 )

		if((@contador)>0)		

		begin			

			set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 

								from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

								inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

								 and serial =@serie and id_producto=@Id_Producto and ep.Id_EstadoProducto in(2) and r.Id_Ruta =@Id_Ruta)

			if(LEN(@Observacion)>0)

				set @PuedeRegistrar ='SePuedeRegistrar';			 

			else

			begin

				set @PuedeRegistrar = 'NoSePuedeRegistrar'

					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 

										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

										and serial =@serie and id_producto=@Id_Producto )

			end

		end

	end

	if(@Accion = 6)--devolucion a Tigo

	--se puede entregar si esta con estado 1 en almacen	

	begin	--select * from tbl_estadoproducto  select * from tbl_productos

	

		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto= @id_producto  and e_eliminado = 0 )

		if((@contador)>0)

		begin

			set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

								 and serial =@serie and id_producto=@id_producto and ep.Id_EstadoProducto in(4,5, 10))

			if(LEN(@Observacion)>0)

				set @PuedeRegistrar ='SePuedeRegistrar';			 

			else

			begin

				set @PuedeRegistrar = 'NoSePuedeRegistrar'

					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 

										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

										and serial =@serie and id_producto=@Id_Producto )

			end

		end

		else

		begin

			set @Observacion = 'No existe el producto'

		end 

		

	end




	if(@Accion = 7)--Baja Producto

	--se puede entregar si esta con estado 1 en almacen	

	begin	--select * from tbl_estadoproducto  select * from tbl_productos

	

		set @contador = (select COUNT(*) from tbl_productos where serial =@serie and id_producto= @id_producto  and e_eliminado = 0 )

		if((@contador)>0)

		begin

			set @Observacion = (select ep.Nombre from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

								 and serial =@serie and id_producto=@id_producto and ep.Id_EstadoProducto in(1, 2, 4, 5, 6, 10))

			if(LEN(@Observacion)>0)

				set @PuedeRegistrar ='SePuedeRegistrar';			 

			else

			begin

				set @PuedeRegistrar = 'NoSePuedeRegistrar'

					set @Observacion = (select 'Estado: ' + ep.Nombre + ' - '+ r.Nombre 

										from tbl_productos s inner join tbl_EstadoProducto ep on ep.Id_EstadoProducto = s.id_EstadoProducto and s.e_eliminado = 0

										inner join tbl_Ruta r on r.Id_Ruta= s.id_Ruta 

										and serial =@serie and id_producto=@Id_Producto )

			end

		end

		else

		begin

			set @Observacion = 'No existe el producto'

		end 

		

	end

	insert into @tablaResultados select @PuedeRegistrar,@Observacion

	select * from @tablaResultados 
GO

/* [dbo].[spz_ObtenerTodosLosBajasXRangoFechas] */
CREATE proc [dbo].[spz_ObtenerTodosLosBajasXRangoFechas](@FechaInicioDatos datetime,@FechaFinDatos datetime)
as
select (select sucursal from tbl_version)Sucursal,i.id_BajaProductos, u.Nombre Usuario,r.Nombre Ruta,dbo.DateOnly(i.Fecha)Fecha,i.fechaRegistro, i.Observacion,
pr.Nombre,c.Cantidad,c.Serie,c.ChipID, DATEPART(year, i.Fecha)Año,DATEname(MONTH, i.Fecha)Mes,tp.TipoBaja,
ep.Nombre EstadoProducto
from tbl_BajaProductos i, tbl_Usuario u, tbl_CodigoBajaProductos c , tbl_producto pr, tbl_Ruta r ,tbl_tipobajaproductospendiente tp
, tbl_estadoproducto ep 
where u.Id_Usuario=i.Id_Usuario and i.E_Eliminado=0 
and c.id_BajaProductos = i.id_BajaProductos
and pr.Id_Producto = c.Id_Producto and 
r.Id_Ruta = i.Id_Ruta and
tp.id_tipobajaproductospendiente=i.id_tipobajaproductospendiente
and dbo.dateonly(i.fecha)between dbo.dateonly(@FechaInicioDatos) and dbo.dateonly(@FechaFinDatos)
and ep.Id_EstadoProducto = i.Id_EstadoProductos

--select sucursal from tbl_version
GO

/* [dbo].[spz_ObtenerTodosLosCierresXRangoFechas] */
CREATE proc [dbo].[spz_ObtenerTodosLosCierresXRangoFechas](@FechaInicioDatos datetime,@FechaFinDatos datetime)
AS
SELECT    (select sucursal from tbl_version)Sucursal, c.id_cierrealmacen, c.id_usuario, c.fecha, c.observacion, c.e_eliminado, c.fecha_registro, u.Nombre, 
'CierreAlmacen' Tipo, pr.Nombre Producto, DATEPART(year, c.Fecha) Año, DATEname(MONTH, c.Fecha) Mes,cc.saldodiahoy Saldo,
0 SaldoPRetirado
FROM         tbl_CierreAlmacen c INNER JOIN
                      tbl_Usuario u ON u.Id_Usuario = c.Id_Usuario INNER JOIN
                      tbl_CodigoCierreAlmacen cc ON cc.Id_CierreAlmacen = c.Id_CierreAlmacen INNER JOIN
                      tbl_producto pr ON pr.Id_Producto = cc.Id_Producto
WHERE     c.E_Eliminado = 0 AND cc.E_Eliminado = 0 and 
dbo.dateonly(c.fecha) between dbo.dateonly(@FechaInicioDatos) and dbo.dateonly(@FechaFinDatos)
UNION ALL
SELECT   (select sucursal from tbl_version)Sucursal,  c.id_cierrealmacenpr_pd id_cierrealmacen, c.id_usuario, c.fecha, c.observacion, c.e_eliminado, c.fecha_registro, u.Nombre, 
'CierreAlmacenPR_PD' Tipo, pr.Nombre Producto, DATEPART(year, c.Fecha) Año, DATEname(MONTH, c.Fecha) Mes, cc.SaldoDiaHoyPD Saldo,
cc.SaldoDiaHoyPR SaldoPRetirado
FROM         tbl_CierreAlmacenPR_PD c INNER JOIN
  tbl_Usuario u ON u.Id_Usuario = c.Id_Usuario INNER JOIN
  tbl_CodigoCierreAlmacenPR_PD cc ON cc.Id_CierreAlmacenPR_PD = c.Id_CierreAlmacenPR_PD INNER JOIN
  tbl_producto pr ON pr.Id_Producto = cc.Id_Producto
WHERE     c.E_Eliminado = 0 AND cc.E_Eliminado = 0 and 
dbo.dateonly(c.fecha) between dbo.dateonly(@FechaInicioDatos) and dbo.dateonly(@FechaFinDatos)


GO

/* [dbo].[spz_ObtenerTodosLosCuadreXRangoFechas] */

CREATE proc [dbo].[spz_ObtenerTodosLosCuadreXRangoFechas](@FechaInicioDatos datetime,@FechaFinDatos datetime)
as
select (select sucursal from tbl_version)Sucursal, i.Id_Cuadre, u.Nombre Usuario,r.Nombre Ruta,dbo.DateOnly(i.Fecha)Fecha,i.Fecha_Registro, i.Observacion,
pr.Nombre,c.ItemsSobrantes,c.ItemsVendidos,c.ItemsRetirados, DATEPART(year, i.Fecha)Año,DATEname(MONTH, i.Fecha)Mes
from tbl_cuadre i, tbl_Usuario u, tbl_CodigoCuadre c , tbl_producto pr, tbl_Ruta r 
where u.Id_Usuario=i.Id_Usuario and i.E_Eliminado=0 and c.E_Eliminado=0 
and c.Id_Cuadre= i.Id_Cuadre
and pr.Id_Producto = c.Id_Producto and 
r.Id_Ruta = i.Id_Ruta 
and dbo.DateOnly(i.Fecha)between dbo.dateonly(@FechaInicioDatos) and dbo.dateonly(@FechaFinDatos)

GO

/* [dbo].[spz_ObtenerTodosLosDevolucionXRangoFechas] */
CREATE proc [dbo].[spz_ObtenerTodosLosDevolucionXRangoFechas](@FechaInicioDatos datetime,@FechaFinDatos datetime)
as
select (select sucursal from tbl_version)Sucursal, i.Id_Devolucion, u.Nombre Usuario,r.Nombre Ruta,dbo.DateOnly(i.Fecha)Fecha,i.Fecha_Registro, i.Observacion,
pr.Nombre,c.Cantidad,c.Cod_Inicio,c.ChipID,ts.Nombre TipoDevolucion,
case when i.Estado = 0 then 'Recepcionado en Almacen' else 'Entregado a Tigo' end Estado
, DATEPART(year, i.Fecha)Año,DATEname(MONTH, i.Fecha)Mes, i.NroOrdenTrabajo
from tbl_Devolucion i, tbl_Usuario u, tbl_DetalleDevolucion c , tbl_producto pr, tbl_Ruta r , tbl_TipoDevolucion ts
where u.Id_Usuario=i.Id_Usuario and i.E_Eliminado=0 
and c.Id_Devolucion= i.Id_Devolucion
and pr.Id_Producto = c.Id_Producto and 
r.Id_Ruta = i.Id_Ruta and
i.Id_TipoDevolucion= ts.Id_TipoDevolucion and
dbo.DateOnly(i.Fecha)between dbo.dateonly(@FechaInicioDatos) and dbo.dateonly(@FechaFinDatos)
GO

/* [dbo].[spz_ObtenerTodosLosEntregasXRangoFechas] */

CREATE proc [dbo].[spz_ObtenerTodosLosEntregasXRangoFechas](@FechaInicioDatos datetime,@FechaFinDatos datetime)
as
select (select sucursal from tbl_version)Sucursal,i.Id_AlmacenVendedor, u.Nombre Usuario,r.Nombre Ruta,dbo.DateOnly(i.Fecha)Fecha,i.Fecha_Registro, i.Observacion,
pr.Nombre,c.Cantidad,c.Cod_Inicio,c.ChipID, DATEPART(year, i.Fecha)Año,DATEname(MONTH, i.Fecha)Mes, i.Id_PedidoVendedor
from tbl_AlmacenVendedor i, tbl_Usuario u, tbl_CodigoAlmacenVendedor c , tbl_producto pr, tbl_Ruta r 
where u.Id_Usuario=i.Id_Usuario and i.E_Eliminado=0 
and c.Id_AlmacenVendedor= i.Id_AlmacenVendedor
and pr.Id_Producto = c.Id_Producto and 
r.Id_Ruta = i.Id_Ruta 
and dbo.dateonly(i.fecha) between dbo.dateonly(@FechaInicioDatos) and dbo.dateonly(@FechaFinDatos)

GO

/* [dbo].[spz_ObtenerTodosLosIngresosXRangoFechas] */





CREATE proc  [dbo].[spz_ObtenerTodosLosIngresosXRangoFechas](@FechaInicioDatos datetime,@FechaFinDatos datetime)
as
select (select sucursal from tbl_version)Sucursal,i.Id_IngresoAlmacen, u.Nombre Usuario,i.Proveedor,dbo.DateOnly(i.Fecha)Fecha,i.Fecha_Registro, i.Observacion,
pr.Nombre,c.Cantidad,c.Cod_Inicio Serie, c.Chip_Id, DATEPART(year, i.Fecha)Año,DATEname(MONTH, i.Fecha)Mes
from tbl_IngresoAlmacen i, tbl_Usuario u, tbl_CodigoIngresoAlmacen c , tbl_producto pr
where u.Id_Usuario=i.Id_Usuario and i.E_Eliminado=0 
and c.Id_IngresoAlmacen = i.Id_IngresoAlmacen 
and pr.Id_Producto = c.Id_Producto  
and dbo.dateonly(i.fecha) between dbo.dateonly(@FechaInicioDatos) and dbo.dateonly(@FechaFinDatos)





GO

/* [dbo].[spz_ObtenerTodosLosOrdenesTrabajoXRangoFechas] */
CREATE proc [dbo].[spz_ObtenerTodosLosOrdenesTrabajoXRangoFechas](@FechaInicioDatos datetime,@FechaFinDatos datetime)
as
select (select sucursal from tbl_version)Sucursal,i.Id_Venta, u.Nombre Usuario,r.Nombre Ruta,dbo.DateOnly(i.Fecha_Ejecucion)Fecha,i.Fecha_Registro, i.Observacion,
pr.Nombre,c.Cantidad,c.Cod_Inicio,c.ChipID,i.CodigoCliente,i.OrdenTrabajo,ts.Nombre TipoServicio
, DATEPART(year, i.Fecha_Ejecucion)Año,DATEname(MONTH, i.Fecha_Ejecucion)Mes, c.Id_TipoMaterial
from tbl_Venta i, tbl_Usuario u, tbl_CodigoVenta c , tbl_producto pr, tbl_Ruta r , tbl_TipoServicio ts
where u.Id_Usuario=i.Id_Usuario and i.E_Eliminado=0 
and c.Id_Venta= i.Id_Venta
and pr.Id_Producto = c.Id_Producto and 
r.Id_Ruta = i.Id_Ruta and
i.Id_TipoServicio = ts.Id_TipoServicio
and dbo.dateonly(i.fecha_ejecucion)between dbo.dateonly(@FechaInicioDatos) and dbo.dateonly(@FechaFinDatos)

GO

/* [dbo].[spz_ObtenerTodosLosTraspasosXRangoFechas] */
CREATE proc [dbo].[spz_ObtenerTodosLosTraspasosXRangoFechas](@FechaInicioDatos datetime,@FechaFinDatos datetime)
as
select (select sucursal from tbl_version)Sucursal,i.Id_SalidaTraspaso, u.Nombre Usuario,r.Nombre Ruta,dbo.DateOnly(i.Fecha)Fecha,i.Fecha_Registro, i.Observacion,
pr.Nombre,c.Cantidad,c.Serie,c.ChipID, DATEPART(year, i.Fecha)Año,DATEname(MONTH, i.Fecha)Mes
from tbl_SalidaTraspaso i, tbl_Usuario u, tbl_CodigoSalidaTraspaso c , tbl_producto pr, tbl_Ruta r 
where u.Id_Usuario=i.Id_Usuario and i.E_Eliminado=0 
and c.Id_SalidaTraspaso= i.Id_SalidaTraspaso
and pr.Id_Producto = c.Id_Producto and 
r.Id_Ruta = i.Id_Ruta 
and dbo.dateonly(i.fecha)between dbo.dateonly(@FechaInicioDatos) and dbo.dateonly(@FechaFinDatos)
GO

/* [dbo].[TraerTodosLosProductos] */

CREATE proc [dbo].[TraerTodosLosProductos]
as
select * from tbl_producto  where E_Eliminado=0
order by Nombre Asc

GO

/* [dbo].[traertodoslosproductos_sinfungibleweb] */
CREATE proc [dbo].[TraerTodosLosProductos_SinFungibleWeb]
as
select * from tbl_producto  where E_Eliminado=0 and tipomaterial='MATERIAL'
order by nombre


GO

/* [dbo].[traertodoslosproductos_x_idrutaweb] */
CREATE  proc [dbo].[TraerTodosLosProductos_x_IdRutaWeb](@Id_Ruta int)
as
select pr.* 
from tbl_saldotarjetas s inner join tbl_producto pr on pr.id_producto=s.id_producto
where s.id_ruta=@Id_Ruta and s.e_eliminado=0 and s.cantidad>0 
order by Nombre Asc


GO

/* [dbo].[TraerTodosLosProductosWeb] */
CREATE proc [dbo].[TraerTodosLosProductosWeb]
as
select * from tbl_producto  where E_Eliminado=0 and tipomaterial='MATERIAL'


GO

