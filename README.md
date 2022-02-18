# **Program Unit DB**

## Description

Program Unit DB helps to conveniently map the result of calling stored procedures or stored functions into a regular Java class. The call to stored procedures or stored functions occurs using the [Spring JDBC Template](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html), and the mapping to the class is done through the Java Reflection API mechanism.

Program Unit DB помогает удобным образом отображать результат вызова хранимых процедур или хранимых функций в обычный Java класс. Вызов хранимых процедур или хранимых функций происходит с помощью [Spring JDBC Template](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html), а отображение в класс через механизм Java Reflection API.

## Getting Started

Requires Java >= 8 (Требуется Java >= 8).

To build, you can enter the command (Для сборки можно ввести команду) :

```bash
#windows
gradlew.bat build

#linux
./gradlew build
```
For testing and code coverage reporting (Для тестирования и отчета об покрытии кода):
```bash
#windows
gradlew.bat test jacocoTestReport 

#linux
./gradlew test jacocoTestReport 
```

<hr>

## Working with databases through Spring JDBC Template (Работа с базами данных через Spring JDBC Template)

A stored function or stored procedure (below program units) is described by the following program elements:
* data source (javax.sql.DataSource) - includes the mechanism for creating a connection and the JDBC driver for connecting to the database:
* full path to the program unit in the database (eg solution_med.pak_lis_plate.get_specimen_info_by_id);
* input parameters (SqlParameter, java.sql.Types);
* contract-class;


Хранимая функция или хранимая процедура (ниже программные юниты) описываются следующими элементами программы:
* источником данных (javax.sql.DataSource) - включает механизм создания подключения и драйвер JDBC подключения к БД:
* полным путем к программному юниту в базе данный (например solution_med.pak_lis_plate.get_specimen_info_by_id);
* входными параметрами (SqlParameter, java.sql.Types);
* классом-контрактом;

The contract class defines the program unit's output parameters through the @OutParam annotation. This annotation is placed over class fields or method parameters, and either all method parameters are marked or none. For a function that returns a cursor, the annotation name is equal to "", and isReturnFucntionParam is true.

Класс-контракт определяет выходные параметры программного юнита через аннотацию @OutParam. Данная аннотация ставиться над полями класса или параметрами методами, причем либо помечаются все параметры метода либо ни одного. Для функции, которая возвращает курсор у аннотации name равен "", а isReturnFucntionParam - true.

The @Column annotation is used to describe the cursor columns.

Для описание колонок курсора применяет аннотация @Column.

### PostgreSQL function example without input parameters (Пример функции PostgreSQL без входных параметров)

```sql
CREATE OR REPLACE FUNCTION test_program_unit.is_exist_users()
	RETURNS boolean
	LANGUAGE 'plpgsql'
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
```

```java
String schemaName = "test_program_unit";
String catalogName = null;
String procedureOrFuctionName = "is_exist_users";
List<SqlParameter> inParameters = null;
Class<Boolean> clazzString = Boolean.class;
boolean isFunction = true;

ProgramUnitDB<Boolean> programUnitDB = new ProgramUnitDB<Boolean>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
Boolean actual = programUnitDB.executeReturnedOnlyOneNonCursor();

```

### PostgreSQL function example with integer input and string output (Пример функции PostgreSQL c входным целочисленным параметром и со строковым выходным параметром)
```sql
CREATE OR REPLACE FUNCTION test_program_unit.get_name_user_by_id(user_id test_program_unit.users.id%type)
	RETURNS test_program_unit.users.name%type
	LANGUAGE 'plpgsql'
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
```

```java
String schemaName = "test_program_unit";
String catalogName = null;
String procedureOrFuctionName = "get_name_user_by_id";
		
List<SqlParameter> inParameters = new ArrayList<>();
inParameters.add(new SqlParameter("user_id", java.sql.Types.BIGINT));

Class<String> clazzString = String.class;
boolean isFunction = true;

ProgramUnitDB<String> programUnitDB = new ProgramUnitDB<String>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
String actual = programUnitDB.executeReturnedOnlyOneNonCursor(1L);

```

### PostgreSQL stored procedure example without input and output parameters (Пример хранимой процедуры PostgreSQL без входных и выходных параметров)

```sql
CREATE OR REPLACE PROCEDURE test_program_unit.insert_and_delete()
LANGUAGE 'plpgsql'
AS $BODY$
	DECLARE
		test_user_id test_program_unit.users.id%type;
	BEGIN
		INSERT INTO test_program_unit.users (name, comment) VALUES ('Test Foo Bar', 'test') RETURNING id INTO test_user_id;

		DELETE FROM test_program_unit.users us
			WHERE us.id = test_user_id;
	END;
$BODY$;
```

```java
String schemaName = "test_program_unit";
String catalogName = null;
String procedureOrFuctionName = "insert_and_delete";
		
List<SqlParameter> inParameters = null;

boolean isFunction = false;

ProgramUnitDB<Void> programUnitDB = new ProgramUnitDB<Void>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, null, isFunction);
```

### PostgreSQL stored procedure example with string input and integer output(Пример хранимой процедуры PostgreSQL c входным строковым параметром и выходным числовым параметром)

```sql
CREATE OR REPLACE PROCEDURE test_program_unit.get_role_id_by_name(
	IN role_name test_program_unit.roles.name%type,
	OUT user_role_id test_program_unit.roles.id%type
)
LANGUAGE 'plpgsql'
AS $BODY$
	BEGIN
		SELECT id
		INTO user_role_id
		FROM test_program_unit.roles
		where name = role_name
		LIMIT 1;
	END;
$BODY$;
```

```java
String schemaName = "test_program_unit";
String catalogName = null;
String procedureOrFuctionName = "get_role_id_by_name";
		
List<SqlParameter> inParameters = new ArrayList<>();
inParameters.add(new SqlParameter("role_name", java.sql.Types.VARCHAR));

Class<Long> clazzString = Long.class;
boolean isFunction = false;

ProgramUnitDB<Long> programUnitDB = new ProgramUnitDB<Long>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
Long actual = programUnitDB.executeReturnedOnlyOneNonCursor("admin");
```


### PostgreSQL stored procedure example with timestamp input parameter and output cursor (Пример хранимой процедуры PostgreSQL c входным параметром timestamp и выходным курсором)
```sql
CREATE OR REPLACE PROCEDURE test_program_unit.get_2_first_user(
	create_date_more test_program_unit.users.createdate%type,
	OUT ref_cursor refcursor
)
LANGUAGE 'plpgsql'
AS $BODY$
	BEGIN
		OPEN ref_cursor FOR
			select us.id, us.name
			from test_program_unit.users us
			where us.createdate >= create_date_more;
	END;
$BODY$;
```

Get2FirstUser:
```java
@OutParam(name = "REF_CURSOR")
public class Get2FirstUser {
    
    @Column(name = "id")
    private Long id;
    private String name; 

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(@Column(name = "name") String name) {
        this.name = name;
    }
    ...
}
```

```java
String schemaName = "test_program_unit";
String catalogName = null;
String procedureOrFuctionName = "get_2_first_user";
		
List<SqlParameter> inParameters = new ArrayList<>();
inParameters.add(new SqlParameter("create_date_more", java.sql.Types.TIMESTAMP));

Class<Get2FirstUser> clazzString = Get2FirstUser.class;
boolean isFunction = false;
		
ProgramUnitDB<Get2FirstUser> programUnitDB = new ProgramUnitDB<Get2FirstUser>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
List<Get2FirstUser> actual = programUnitDB.executeReturnedOnlyOneCursor(java.sql.Timestamp.valueOf( LocalDateTime.parse("2020-01-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
```
### PostgreSQL stored procedure example with two input parameters (Пример хранимой процедуры PostgreSQL c двумя входящими параметрами)

```sql
CREATE OR REPLACE PROCEDURE test_program_unit.add_user(
	name test_program_unit.users.name%type,
	comment test_program_unit.users.comment%type
)
LANGUAGE 'plpgsql'
AS $BODY$
	BEGIN
		INSERT INTO test_program_unit.users (name, comment) VALUES (name, comment);
	END;
$BODY$;
```

```java
String schemaName = "test_program_unit";
String catalogName = null;
String procedureOrFuctionName = "add_user";
		
List<SqlParameter> inParameters = new ArrayList<>();
inParameters.add(new SqlParameter("name", java.sql.Types.VARCHAR));
inParameters.add(new SqlParameter("comment", java.sql.Types.VARCHAR));

Class<Void> clazzString = null;
boolean isFunction = false;

ProgramUnitDB<Void> programUnitDB = new ProgramUnitDB<Void>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
```

### PostgreSQL stored procedure example with two output parameters (Пример хранимой процедуры PostgreSQL c двумя выходными параметрами)

```sql
CREATE OR REPLACE PROCEDURE test_program_unit.get_some_user(
	OUT admins refcursor,
	OUT users refcursor
)
LANGUAGE 'plpgsql'
AS $BODY$
	BEGIN
		OPEN admins FOR
			SELECT u.name, u.comment, u.createdate
			FROM test_program_unit.users u, test_program_unit.users_roles ur, test_program_unit.roles r
			WHERE u.id = ur.userid
				AND ur.roleid = r.id
				AND r.name = 'admin'
			LIMIT 2;

		OPEN users FOR
			SELECT u.name, u.comment, u.createdate
			FROM test_program_unit.users u, test_program_unit.users_roles ur, test_program_unit.roles r
			WHERE u.id = ur.userid
				AND ur.roleid = r.id
				AND r.name = 'user'
			LIMIT 2;
	END;
$BODY$;
```

GetSomeUser:
```java

public class GetSomeUser {

    @OutParam(name = "admins")
    public class Admin {
        private String name;
        private String comment;
        private LocalDateTime createdate;
        
        public String getName() {
            return name;
        }
        public void setName(@Column(name = "name") String name) {
            this.name = name;
        }
        public String getComment() {
            return comment;
        }
        public void setComment(@Column(name = "comment") String comment) {
            this.comment = comment;
        }
        public LocalDateTime getCreatedate() {
            return createdate;
        }
        public void setCreatedate(@Column(name = "createdate") LocalDateTime createdate) {
            this.createdate = createdate;
        }
        ...
    }

    private List<Admin> admins;
    
    public List<Admin> getAdmins() {
        return admins;
    }
    public void setAdmins(@OutParam(name = "admins") List<Admin> admins) {
        this.admins = admins;
    }

    @OutParam(name = "users")
    public class User {
        private String name;
        private String comment;
        private LocalDateTime createdate;
        
        public String getName() {
            return name;
        }
        public void setName(@Column(name = "name") String name) {
            this.name = name;
        }
        public String getComment() {
            return comment;
        }
        public void setComment(@Column(name = "comment") String comment) {
            this.comment = comment;
        }
        public LocalDateTime getCreatedate() {
            return createdate;
        }
        public void setCreatedate(@Column(name = "createdate") LocalDateTime createdate) {
            this.createdate = createdate;
        }
        ...
    }

    private List<User> users;

    public List<User> getUsers() {
        return users;
    }
    public void setUsers(@OutParam(name = "users") List<User> users) {
        this.users = users;
    }
    ....

}
```

```java
String schemaName = "test_program_unit";
String catalogName = null;
String procedureOrFuctionName = "get_some_user";
		
List<SqlParameter> inParameters = new ArrayList<>();

Class<GetSomeUser> clazzString = GetSomeUser.class;
boolean isFunction = false;

ProgramUnitDB<GetSomeUser> programUnitDB = new ProgramUnitDB<GetSomeUser>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
GetSomeUser actual = programUnitDB.executeReturnedSeveralOutParams();	
```

Detailed (Подробнее):
* [JDBC](https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/)
* [Spring JDBC Template](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html)
* javax.sql.DataSource
* org.springframework.jdbc.core.simple.SimpleJdbcCall
* org.springframework.jdbc.core.SqlParameter
* java.sql.Types

<hr>

## Creators

Nikita Konstantinovich Chistousov 

chistousov.nik@yandex.ru

## License

MIT