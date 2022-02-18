DO
'
declare
	Nikita_user_id BIGINT;
	Vasily_user_id BIGINT;
	Sergey_user_id BIGINT;
	Vita_user_id BIGINT;

	admin_role_id BIGINT;
	user_role_id BIGINT;

begin

	-- SCHEMA: test_program_unit
	CREATE SCHEMA IF NOT EXISTS test_program_unit;

	-- Table Users
	CREATE TABLE IF NOT EXISTS test_program_unit.users
	(
		id bigserial NOT NULL,
		name text NOT NULL,
		createdate timestamp without time zone NOT NULL DEFAULT now(),
		comment text NOT NULL,
		CONSTRAINT "PK_users" PRIMARY KEY (id)
	);

	INSERT INTO test_program_unit.users (name, comment, createdate) VALUES (''Nikita Konstantinovich Chistousov'', ''Nikitos'', to_date(''2021/07/08'', ''YYYY/MM/DD'')) RETURNING id INTO Nikita_user_id;
	INSERT INTO test_program_unit.users (name, comment, createdate) VALUES (''Vasily Nikolaevich Shalashov'', ''Shalashov'', to_date(''2021/07/08'', ''YYYY/MM/DD'')) RETURNING id INTO Vasily_user_id;
	INSERT INTO test_program_unit.users (name, comment, createdate) VALUES (''Sergey Olegovich Mozgovoy'', ''Varangian'', to_date(''2019/10/11'', ''YYYY/MM/DD'')) RETURNING id INTO Sergey_user_id;
	INSERT INTO test_program_unit.users (name, comment, createdate) VALUES (''Vita Alekseevna Khodakov'', ''ATATA'', to_date(''2018/07/08'', ''YYYY/MM/DD'')) RETURNING id INTO Vita_user_id;

	-- Table Roles
	CREATE TABLE IF NOT EXISTS test_program_unit.roles
	(
		id bigserial NOT NULL,
		name text NOT NULL,
		comment text NOT NULL,
		CONSTRAINT "PK_roles" PRIMARY KEY (id)
	);

	INSERT INTO test_program_unit.roles (name, comment) VALUES (''admin'', ''admin'') RETURNING id INTO admin_role_id;
	INSERT INTO test_program_unit.roles (name, comment) VALUES (''user'', ''user'') RETURNING id INTO user_role_id;

	CREATE TABLE IF NOT EXISTS  test_program_unit.users_roles
	(
		id bigserial NOT NULL,
		userid bigint NOT NULL,
		roleid bigint NOT NULL,
		CONSTRAINT "PK_users_roles" PRIMARY KEY (id),
		CONSTRAINT "FK_us_us_roles" FOREIGN KEY (userid)
			REFERENCES test_program_unit.users (id) MATCH FULL
			ON UPDATE CASCADE
			ON DELETE CASCADE,
		CONSTRAINT "FK_role_us_roles" FOREIGN KEY (roleid)
			REFERENCES test_program_unit.roles (id) MATCH FULL
			ON UPDATE CASCADE
			ON DELETE CASCADE
	);

	INSERT INTO test_program_unit.users_roles (userid, roleid) VALUES (Nikita_user_id, admin_role_id);
	INSERT INTO test_program_unit.users_roles (userid, roleid) VALUES (Nikita_user_id, user_role_id);
	INSERT INTO test_program_unit.users_roles (userid, roleid) VALUES (Vasily_user_id, admin_role_id);
	INSERT INTO test_program_unit.users_roles (userid, roleid) VALUES (Vasily_user_id, user_role_id);
	INSERT INTO test_program_unit.users_roles (userid, roleid) VALUES (Sergey_user_id, admin_role_id);
	INSERT INTO test_program_unit.users_roles (userid, roleid) VALUES (Vita_user_id, user_role_id);

	-- no in and out
	CREATE OR REPLACE FUNCTION test_program_unit.is_exist_users()
		RETURNS boolean
		LANGUAGE ''plpgsql''
	AS $BODY$
		DECLARE
			flag boolean;
		BEGIN
			select exists(
				select * from test_program_unit.users
			)
			into flag;
			RETURN flag;
	END;
	$BODY$;

	-- one in param
	CREATE OR REPLACE FUNCTION test_program_unit.get_name_user_by_id(user_id test_program_unit.users.id%type)
		RETURNS test_program_unit.users.name%type
		LANGUAGE ''plpgsql''
	AS $BODY$
		DECLARE
			name test_program_unit.users.name%type;
		BEGIN
			select us.name
			into name
			from test_program_unit.users us
			where us.id = user_id
			LIMIT 1;

			RETURN name;
	END;
	$BODY$;


	CREATE OR REPLACE PROCEDURE test_program_unit.insert_and_delete()
	LANGUAGE ''plpgsql''
	AS $BODY$
		DECLARE
			test_user_id test_program_unit.users.id%type;
		BEGIN
			INSERT INTO test_program_unit.users (name, comment) VALUES (''Test Foo Bar'', ''test'') RETURNING id INTO test_user_id;

			DELETE FROM test_program_unit.users us
				WHERE us.id = test_user_id;
		END;
	$BODY$;

	CREATE OR REPLACE PROCEDURE test_program_unit.get_role_id_by_name(
		IN role_name test_program_unit.roles.name%type,
		OUT user_role_id test_program_unit.roles.id%type
	)
	LANGUAGE ''plpgsql''
	AS $BODY$
		BEGIN
			SELECT id
			INTO user_role_id
			FROM test_program_unit.roles
			where name = role_name
			LIMIT 1;
		END;
	$BODY$;


	CREATE OR REPLACE PROCEDURE test_program_unit.get_2_first_user(
		create_date_more test_program_unit.users.createdate%type,
		OUT ref_cursor refcursor
	)
	LANGUAGE ''plpgsql''
	AS $BODY$
		BEGIN
			OPEN ref_cursor FOR
				select us.id, us.name
				from test_program_unit.users us
				where us.createdate >= create_date_more;
		END;
	$BODY$;

	CREATE OR REPLACE PROCEDURE test_program_unit.add_user(
		name test_program_unit.users.name%type,
		comment test_program_unit.users.comment%type
	)
	LANGUAGE ''plpgsql''
	AS $BODY$
		BEGIN
			INSERT INTO test_program_unit.users (name, comment) VALUES (name, comment);
		END;
	$BODY$;
	

	CREATE OR REPLACE PROCEDURE test_program_unit.get_some_user(
		OUT admins refcursor,
		OUT users refcursor
	)
	LANGUAGE ''plpgsql''
	AS $BODY$
		BEGIN
			OPEN admins FOR
				SELECT u.name, u.comment, u.createdate
				FROM test_program_unit.users u, test_program_unit.users_roles ur, test_program_unit.roles r
				WHERE u.id = ur.userid
				  AND ur.roleid = r.id
				  AND r.name = ''admin''
				LIMIT 2;

			OPEN users FOR
				SELECT u.name, u.comment, u.createdate
				FROM test_program_unit.users u, test_program_unit.users_roles ur, test_program_unit.roles r
				WHERE u.id = ur.userid
				  AND ur.roleid = r.id
				  AND r.name = ''user''
				LIMIT 2;
		END;
	$BODY$;
end;
'  LANGUAGE PLPGSQL;