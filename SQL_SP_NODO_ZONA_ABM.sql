USE BDControlOrdenes;
GO

IF OBJECT_ID('dbo.spx_CrearNodoZona', 'P') IS NOT NULL
    DROP PROCEDURE dbo.spx_CrearNodoZona;
GO

CREATE PROCEDURE dbo.spx_CrearNodoZona
    @Nodos_Asociados NVARCHAR(50),
    @Distrito NVARCHAR(50),
    @Zona NVARCHAR(50),
    @Usuario NVARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;

    SET @Nodos_Asociados = UPPER(NULLIF(LTRIM(RTRIM(@Nodos_Asociados)), N''));
    SET @Distrito = NULLIF(LTRIM(RTRIM(@Distrito)), N'');
    SET @Zona = UPPER(NULLIF(LTRIM(RTRIM(@Zona)), N''));
    SET @Usuario = NULLIF(LTRIM(RTRIM(@Usuario)), N'');

    IF @Nodos_Asociados IS NULL OR @Distrito IS NULL OR @Zona IS NULL
    BEGIN
        RAISERROR('Nodo, distrito y zona son requeridos.', 16, 1);
        RETURN;
    END;

    IF EXISTS (
        SELECT 1
        FROM dbo.tbl_Nodo_Zona
        WHERE ISNULL(E_Eliminado, 0) = 0
          AND UPPER(LTRIM(RTRIM(ISNULL(Nodos_Asociados, N'')))) = UPPER(@Nodos_Asociados)
          AND UPPER(LTRIM(RTRIM(ISNULL(Distrito, N'')))) = UPPER(@Distrito)
          AND UPPER(LTRIM(RTRIM(ISNULL(Zona, N'')))) = UPPER(@Zona)
    )
    BEGIN
        RAISERROR('Ya existe un nodo zona activo con esos datos.', 16, 1);
        RETURN;
    END;

    INSERT INTO dbo.tbl_Nodo_Zona (
        Nodos_Asociados,
        Distrito,
        Zona,
        Usuario,
        FechaRegistro,
        E_Eliminado
    )
    VALUES (
        @Nodos_Asociados,
        @Distrito,
        @Zona,
        ISNULL(@Usuario, N'sistema'),
        GETDATE(),
        0
    );

    SELECT *
    FROM dbo.tbl_Nodo_Zona
    WHERE id = SCOPE_IDENTITY();
END;
GO

IF OBJECT_ID('dbo.spx_EliminarNodoZona', 'P') IS NOT NULL
    DROP PROCEDURE dbo.spx_EliminarNodoZona;
GO

CREATE PROCEDURE dbo.spx_EliminarNodoZona
    @Id INT,
    @Usuario NVARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;

    IF @Id IS NULL OR @Id <= 0
    BEGIN
        RAISERROR('Id requerido.', 16, 1);
        RETURN;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Nodo_Zona
        WHERE id = @Id
          AND ISNULL(E_Eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('No existe un nodo zona activo con ese id.', 16, 1);
        RETURN;
    END;

    UPDATE dbo.tbl_Nodo_Zona
    SET E_Eliminado = 1,
        Usuario = ISNULL(NULLIF(LTRIM(RTRIM(@Usuario)), N''), Usuario)
    WHERE id = @Id
      AND ISNULL(E_Eliminado, 0) = 0;

    SELECT *
    FROM dbo.tbl_Nodo_Zona
    WHERE id = @Id;
END;
GO

IF OBJECT_ID('dbo.spx_CrearNodoDistrito', 'P') IS NOT NULL
    DROP PROCEDURE dbo.spx_CrearNodoDistrito;
GO

CREATE PROCEDURE dbo.spx_CrearNodoDistrito
    @Nodos_Asociados NVARCHAR(50),
    @Distrito NVARCHAR(50),
    @Zona NVARCHAR(50),
    @DistritoNuevo NVARCHAR(50),
    @Usuario NVARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;

    SET @Nodos_Asociados = UPPER(NULLIF(LTRIM(RTRIM(@Nodos_Asociados)), N''));
    SET @Distrito = NULLIF(LTRIM(RTRIM(@Distrito)), N'');
    SET @Zona = UPPER(NULLIF(LTRIM(RTRIM(@Zona)), N''));
    SET @DistritoNuevo = UPPER(NULLIF(LTRIM(RTRIM(@DistritoNuevo)), N''));
    SET @Usuario = NULLIF(LTRIM(RTRIM(@Usuario)), N'');

    IF @Nodos_Asociados IS NULL OR @Distrito IS NULL OR @Zona IS NULL OR @DistritoNuevo IS NULL
    BEGIN
        RAISERROR('Nodo, distrito, zona y distrito nuevo son requeridos.', 16, 1);
        RETURN;
    END;

    IF EXISTS (
        SELECT 1
        FROM dbo.tbl_Nodo_Distrito
        WHERE ISNULL(E_Eliminado, 0) = 0
          AND UPPER(LTRIM(RTRIM(ISNULL(Nodos_Asociados, N'')))) = UPPER(@Nodos_Asociados)
          AND UPPER(LTRIM(RTRIM(ISNULL(Distrito, N'')))) = UPPER(@Distrito)
          AND UPPER(LTRIM(RTRIM(ISNULL(Zona, N'')))) = UPPER(@Zona)
          AND UPPER(LTRIM(RTRIM(ISNULL(DistritoNuevo, N'')))) = UPPER(@DistritoNuevo)
    )
    BEGIN
        RAISERROR('Ya existe un nodo distrito activo con esos datos.', 16, 1);
        RETURN;
    END;

    INSERT INTO dbo.tbl_Nodo_Distrito (
        Nodos_Asociados,
        Distrito,
        Zona,
        DistritoNuevo,
        Usuario,
        FechaRegistro,
        E_Eliminado
    )
    VALUES (
        @Nodos_Asociados,
        @Distrito,
        @Zona,
        @DistritoNuevo,
        ISNULL(@Usuario, N'sistema'),
        GETDATE(),
        0
    );

    SELECT *
    FROM dbo.tbl_Nodo_Distrito
    WHERE id = SCOPE_IDENTITY();
END;
GO

IF OBJECT_ID('dbo.spx_EliminarNodoDistrito', 'P') IS NOT NULL
    DROP PROCEDURE dbo.spx_EliminarNodoDistrito;
GO

CREATE PROCEDURE dbo.spx_EliminarNodoDistrito
    @Id INT,
    @Usuario NVARCHAR(50)
AS
BEGIN
    SET NOCOUNT ON;

    IF @Id IS NULL OR @Id <= 0
    BEGIN
        RAISERROR('Id requerido.', 16, 1);
        RETURN;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_Nodo_Distrito
        WHERE id = @Id
          AND ISNULL(E_Eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('No existe un nodo distrito activo con ese id.', 16, 1);
        RETURN;
    END;

    UPDATE dbo.tbl_Nodo_Distrito
    SET E_Eliminado = 1,
        Usuario = ISNULL(NULLIF(LTRIM(RTRIM(@Usuario)), N''), Usuario)
    WHERE id = @Id
      AND ISNULL(E_Eliminado, 0) = 0;

    SELECT *
    FROM dbo.tbl_Nodo_Distrito
    WHERE id = @Id;
END;
GO

IF OBJECT_ID('dbo.spx_CrearEstadoCorteTap', 'P') IS NOT NULL
    DROP PROCEDURE dbo.spx_CrearEstadoCorteTap;
GO

CREATE PROCEDURE dbo.spx_CrearEstadoCorteTap
    @Estado NVARCHAR(150),
    @Usuario NVARCHAR(150)
AS
BEGIN
    SET NOCOUNT ON;

    SET @Estado = UPPER(NULLIF(LTRIM(RTRIM(@Estado)), N''));
    SET @Usuario = NULLIF(LTRIM(RTRIM(@Usuario)), N'');

    IF @Estado IS NULL
    BEGIN
        RAISERROR('Estado requerido.', 16, 1);
        RETURN;
    END;

    IF EXISTS (
        SELECT 1
        FROM dbo.tbl_EstadoCorteTap
        WHERE ISNULL(E_Eliminado, 0) = 0
          AND UPPER(LTRIM(RTRIM(ISNULL(Estado, N'')))) = UPPER(@Estado)
    )
    BEGIN
        RAISERROR('Ya existe un estado corte TAP activo con ese nombre.', 16, 1);
        RETURN;
    END;

    INSERT INTO dbo.tbl_EstadoCorteTap (
        Estado,
        Usuario,
        FechaRegistro,
        E_Eliminado
    )
    VALUES (
        @Estado,
        ISNULL(@Usuario, N'sistema'),
        GETDATE(),
        0
    );

    SELECT *
    FROM dbo.tbl_EstadoCorteTap
    WHERE id = SCOPE_IDENTITY();
END;
GO

IF OBJECT_ID('dbo.spx_EliminarEstadoCorteTap', 'P') IS NOT NULL
    DROP PROCEDURE dbo.spx_EliminarEstadoCorteTap;
GO

CREATE PROCEDURE dbo.spx_EliminarEstadoCorteTap
    @Id INT,
    @Usuario NVARCHAR(150)
AS
BEGIN
    SET NOCOUNT ON;

    IF @Id IS NULL OR @Id <= 0
    BEGIN
        RAISERROR('Id requerido.', 16, 1);
        RETURN;
    END;

    IF NOT EXISTS (
        SELECT 1
        FROM dbo.tbl_EstadoCorteTap
        WHERE id = @Id
          AND ISNULL(E_Eliminado, 0) = 0
    )
    BEGIN
        RAISERROR('No existe un estado corte TAP activo con ese id.', 16, 1);
        RETURN;
    END;

    UPDATE dbo.tbl_EstadoCorteTap
    SET E_Eliminado = 1,
        Usuario = ISNULL(NULLIF(LTRIM(RTRIM(@Usuario)), N''), Usuario)
    WHERE id = @Id
      AND ISNULL(E_Eliminado, 0) = 0;

    SELECT *
    FROM dbo.tbl_EstadoCorteTap
    WHERE id = @Id;
END;
GO
