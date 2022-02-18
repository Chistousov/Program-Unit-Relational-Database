package com.github.chistousov.lib.programunitdb;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.github.chistousov.lib.programunitdb.GetSomeUser.Admin;
import com.github.chistousov.lib.programunitdb.GetSomeUser.User;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@RunWith(SpringRunner.class)
@SpringBootTest
@Transactional(value = "jdbcTempleteTransactionManagerPostgres")
public class ProgramUnitDBPostgres {

	@ClassRule
	public static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.1")).withInitScript("postgres-init.sql");

	@Configuration
	public static class JdbcConfigTest {
		@Bean
		public DataSource dataSourcePostgres(){
			HikariConfig hikariConfig = new HikariConfig();
			hikariConfig.setDriverClassName(postgres.getDriverClassName());
			hikariConfig.setUsername(postgres.getUsername());
			hikariConfig.setPassword(postgres.getPassword());
			hikariConfig.setJdbcUrl(postgres.getJdbcUrl());
			hikariConfig.addDataSourceProperty("escapeSyntaxCallMode", "callIfNoReturn");
			return new HikariDataSource(hikariConfig);
		}
	
		@Bean
		public PlatformTransactionManager jdbcTempleteTransactionManagerPostgres(@Autowired DataSource dataSourcePostgres)throws Exception {
			return new DataSourceTransactionManager(dataSourcePostgres);
		}
	}

	@Autowired 
	DataSource dataSourcePostgres;

	@Test
	@DisplayName("PostgreSQL function example without input parameters")
	public void PostgreSQLFunctionExampleWithoutInputParameters() throws Exception {
		
		// given
		String schemaName = "test_program_unit";
		String catalogName = null;
		String procedureOrFuctionName = "is_exist_users";
		List<SqlParameter> inParameters = null;
		Class<Boolean> clazzString = Boolean.class;
		boolean isFunction = true;

		Boolean expected = true;

		// when
		ProgramUnitDB<Boolean> programUnitDB = new ProgramUnitDB<Boolean>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
		Boolean actual = programUnitDB.executeReturnedOnlyOneNonCursor();

		// then
		assertThat(actual).isEqualTo(expected);
	}

    @Test
	@DisplayName("PostgreSQL function example with integer input and string output")
	public void PostgreSQLFunctionExampleWithIntegerInputAndStringOutput() throws Exception {
		
		// given
		String schemaName = "test_program_unit";
		String catalogName = null;
		String procedureOrFuctionName = "get_name_user_by_id";
		
        List<SqlParameter> inParameters = new ArrayList<>();
        inParameters.add(new SqlParameter("user_id", java.sql.Types.BIGINT));

		Class<String> clazzString = String.class;
		boolean isFunction = true;

		String expected = "Nikita Konstantinovich Chistousov";

		// when
		ProgramUnitDB<String> programUnitDB = new ProgramUnitDB<String>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
		String actual = programUnitDB.executeReturnedOnlyOneNonCursor(1L);

		// then
		assertThat(actual).isEqualTo(expected);
	}

    @Test
	@DisplayName("PostgreSQL stored procedure example without input and output parameters")
	public void PostgreSQLStoredProcedureExampleWithoutInputAndOutputParameters() throws Exception {
		
		// given
		String schemaName = "test_program_unit";
		String catalogName = null;
		String procedureOrFuctionName = "insert_and_delete";
		
        List<SqlParameter> inParameters = null;

		boolean isFunction = false;

		// when
		ProgramUnitDB<Void> programUnitDB = new ProgramUnitDB<Void>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, null, isFunction);
		// then
		assertDoesNotThrow(() -> programUnitDB.executeWithoutOutParameters());
	}

    @Test
	@DisplayName("PostgreSQL stored procedure example with string input and integer output")
	public void PostgreSQLStoredProcedureExampleWithStringInputAndIntegerOutput() throws Exception {
		
		// given
		String schemaName = "test_program_unit";
		String catalogName = null;
		String procedureOrFuctionName = "get_role_id_by_name";
		
        List<SqlParameter> inParameters = new ArrayList<>();
        inParameters.add(new SqlParameter("role_name", java.sql.Types.VARCHAR));

		Class<Long> clazzString = Long.class;
		boolean isFunction = false;

		Long expected = 1L;

		// when
		ProgramUnitDB<Long> programUnitDB = new ProgramUnitDB<Long>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
		Long actual = programUnitDB.executeReturnedOnlyOneNonCursor("admin");

		// then
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	@DisplayName("PostgreSQL stored procedure example with timestamp input parameter and output cursor")
	public void PostgreSQLStoredProcedureExampleWithTimestampInputParameterAndOutputCursor() throws Exception {
		
		// given
		String schemaName = "test_program_unit";
		String catalogName = null;
		String procedureOrFuctionName = "get_2_first_user";
		
        List<SqlParameter> inParameters = new ArrayList<>();
        inParameters.add(new SqlParameter("create_date_more", java.sql.Types.TIMESTAMP));

		Class<Get2FirstUser> clazzString = Get2FirstUser.class;
		boolean isFunction = false;

		List<Get2FirstUser> expected = new ArrayList<>();
		
		Get2FirstUser first = new Get2FirstUser();
		first.setId(1L);
		first.setName("Nikita Konstantinovich Chistousov");
		
		expected.add(first);
		
		Get2FirstUser second = new Get2FirstUser();
		second.setId(2L);
		second.setName("Vasily Nikolaevich Shalashov");

		expected.add(second);
		
		// when
		ProgramUnitDB<Get2FirstUser> programUnitDB = new ProgramUnitDB<Get2FirstUser>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
		List<Get2FirstUser> actual = programUnitDB.executeReturnedOnlyOneCursor(java.sql.Timestamp.valueOf( LocalDateTime.parse("2020-01-01T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME)));

		// then
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	@DisplayName("PostgreSQL stored procedure example with two input parameters")
	public void PostgreSQLStoredProcedureExampleWithTwoInputParameters() throws Exception {
		
		// given
		String schemaName = "test_program_unit";
		String catalogName = null;
		String procedureOrFuctionName = "add_user";
		
        List<SqlParameter> inParameters = new ArrayList<>();
        inParameters.add(new SqlParameter("name", java.sql.Types.VARCHAR));
		inParameters.add(new SqlParameter("comment", java.sql.Types.VARCHAR));

		Class<Void> clazzString = null;
		boolean isFunction = false;

		// when
		ProgramUnitDB<Void> programUnitDB = new ProgramUnitDB<Void>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
		// then
		assertDoesNotThrow(() -> programUnitDB.executeWithoutOutParameters("Foo Bar", "Comment"));
	}

	@Test
	@DisplayName("PostgreSQL stored procedure example with two output parameters")
	public void PostgreSQLStoredProcedureExampleWithTwoOutputParameters() throws Exception {
		
		// given
		String schemaName = "test_program_unit";
		String catalogName = null;
		String procedureOrFuctionName = "get_some_user";
		
        List<SqlParameter> inParameters = new ArrayList<>();

		Class<GetSomeUser> clazzString = GetSomeUser.class;
		boolean isFunction = false;

		GetSomeUser expected = new GetSomeUser();
		
		List<Admin> admins = new ArrayList<>();

		Admin firstAdmin = expected.new Admin();
		firstAdmin.setName("Nikita Konstantinovich Chistousov");
		firstAdmin.setComment("Nikitos");
		firstAdmin.setCreatedate(LocalDateTime.parse("2021-07-08T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		
		admins.add(firstAdmin);
		
		Admin secondAdmin = expected.new Admin();
		secondAdmin.setName("Vasily Nikolaevich Shalashov");
		secondAdmin.setComment("Shalashov");
		secondAdmin.setCreatedate(LocalDateTime.parse("2021-07-08T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		admins.add(secondAdmin);

		expected.setAdmins(admins);

		List<User> users = new ArrayList<>();

		User firstUser = expected.new User();
		firstUser.setName("Nikita Konstantinovich Chistousov");
		firstUser.setComment("Nikitos");
		firstUser.setCreatedate(LocalDateTime.parse("2021-07-08T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));
		
		users.add(firstUser);
		
		User secondUser = expected.new User();
		secondUser.setName("Vasily Nikolaevich Shalashov");
		secondUser.setComment("Shalashov");
		secondUser.setCreatedate(LocalDateTime.parse("2021-07-08T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME));

		users.add(secondUser);

		expected.setUsers(users);


		// when
		ProgramUnitDB<GetSomeUser> programUnitDB = new ProgramUnitDB<GetSomeUser>(dataSourcePostgres, schemaName, catalogName, procedureOrFuctionName, inParameters, clazzString, isFunction);
		GetSomeUser actual = programUnitDB.executeReturnedSeveralOutParams();
		
		// then
		assertAll(
			()->assertTrue(actual.getAdmins().size() == expected.getAdmins().size() && actual.getAdmins().containsAll(expected.getAdmins()) && actual.getAdmins().containsAll(expected.getAdmins())),
			()->assertTrue(actual.getUsers().size() == expected.getUsers().size() && actual.getUsers().containsAll(expected.getUsers()) && actual.getUsers().containsAll(expected.getUsers()))
		);
	}
}