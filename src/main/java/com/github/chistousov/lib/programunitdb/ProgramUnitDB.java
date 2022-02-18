package com.github.chistousov.lib.programunitdb;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

import com.github.chistousov.lib.programunitdb.annotations.Column;
import com.github.chistousov.lib.programunitdb.annotations.OutParam;



/**
 * <p>
 * This is a parameterized class that wraps a stored function or stored procedure in a database.
 * (Это параметризированный класс, который является обёрткой над хранимой
 * функцией или хранимой процедурой в базе данных.)
 * </p>
 * 
 * <p>
 * The basis is the
 * {@link org.springframework.jdbc.core.simple.SimpleJdbcCall}, which belongs <a href=
 * "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html">Spring
 * JDBC Template</a>
 * (Основой является класс
 * {@link org.springframework.jdbc.core.simple.SimpleJdbcCall}, который
 * принадлежит <a href=
 * "https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html">Spring
 * JDBC Template)</a>
 * </p>
 * 
 * <p>
 * Tested on PostgreSQL 14.1 and Oracle 12c (Протестирован на PostgreSQL 14.1 и Oracle 12с)
 * </p>
 * 
 * <p>
 * Connections are made through the interface {@link javax.sql.DataSource}. (Подключения создаются через интерфейс {@link javax.sql.DataSource}.)
 * </p>
 * 
 *
 * @author Nikita Chistousov (chistousov.nik@yandex.ru)
 * @since 8
 * @see <a href=
 *      "https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC</a>
 * @see javax.sql.DataSource
 * @see org.springframework.jdbc.core.simple.SimpleJdbcCall
 */
public class ProgramUnitDB<T> {

    // error message if the function is not defined for the given outputs
    // cообщение об ошибке если функция не определена для данный выходных параметров
    private static final String FUNCTION_UNDEFINED_MESSAGE = "This function is not defined!!!";
    // name of the returned parameter of the stored function
    // наименование возвращаемого параметра хранимой функции
    private static final String DEFAULT_NAME_RETURN_PARAM_IN_FUNCTION = "";

    // object directly calling a stored function or stored procedure in the database
    // объект непосредственно, вызывающий хранимую функцию или хранимую процедуру в БД
    private SimpleJdbcCall programUnit;

    // class describing output parameters via @Column and/or @OutParam annotations
    // класс, описывающий выходные параметры через аннотации @Column и/или @OutParam
    private Class<T> clazzOutParameters;

    // multiple output parameters
    // to store mapping output_parameter_name(OutParam.name) -> class_fields
    // несколько выходных параметров
    // для хранения отображения имя_выходного_параметра(OutParam.name) -> поля_класса
    private Map<String, Field> mappingOutParamToField;
    // to store mapping output_parameter_name(OutParam.name) -> class_method
    // для хранения отображения имя_выходного_параметра(OutParam.name) -> метод_класса
    private Map<String, Method> mappingOutParamToMethod;

    // Assembly error
    // Ошибка конпоновки
    private boolean flagDefaultConstructorNotFound = false;
    private boolean flagFieldNotFound = false;
    private boolean flagMethodInccorect = false;
    private boolean flagMetaDataNotFound = false;
    private boolean flagAmountColumnDontGet = false;
    private boolean flagColumnNotFound = false;
    private boolean flagCastFromDBToJavaWithError = false;

    // function types
    // типы функиций
    private boolean isReturnedOnlyOneCursor = false;
    private boolean isReturnedSeveralOutParam = false;

    /**
     * ProgramInitDB constructor (Конструктор ProgramInitDB)
     * 
     * A class is a wrapper for a stored procedure or stored function in any database (Класс является оберткой для хранимой процедуры или хранимой функции в любой
     * базе данных)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC</a>
     * @see javax.sql.DataSource
     * 
     * @param dataSource             is a data source that includes a connection creation mechanism and a JDBC database connection driver (является источником данных, который включает
     *                               механизм создания подключения и драйвер JDBC
     *                               подключения к БД)
     * @param schemaName             - scheme (схема)
     * @param catalogName            - directory (for example, a package in Oracle). May be null. (каталог (например, пакет в Oracle). Может
     *                               быть null)
     * @param procedureOrFuctionName - procedure name (имя процедуры)
     * @param inParameters           - list of incoming parameters (outgoing parameters can be omitted) (список входящих параметров (выходящие
     *                               параметры можно не указывать))
     * @param clazzOutParameters     - contract class describing output parameters using {@link OutParam} and {@link Column} annotations (класс-контракт, описывающий выходные
     *                               параметры с помощью аннотаций {@link OutParam}
     *                               и {@link Column})
     * @param isFunction             - if true, then the object is a function, otherwise stored procedure 
     *                               (если true, то объект является функцией, иначе
     *                               хранимой процедурой)
     * @throws Exception initialization error
     */
    public ProgramUnitDB(DataSource dataSource, String schemaName, String catalogName, String procedureOrFuctionName,
            List<SqlParameter> inParameters, Class<T> clazzOutParameters, boolean isFunction) throws Exception {
        Objects.requireNonNull(dataSource, "dataSource");
        Objects.requireNonNull(schemaName, "schemaName");
        Objects.requireNonNull(procedureOrFuctionName, "procedureOrFuctionName");

        programUnit = new SimpleJdbcCall(dataSource).withSchemaName(schemaName).withCatalogName(catalogName);

        programUnit.setFunction(isFunction);
        programUnit.setProcedureName(procedureOrFuctionName);


        if (inParameters != null) {
            inParameters.forEach(par -> programUnit.addDeclaredParameter(par));
        }

        // stored procedure with no output parameters
        // хранимая процедура без выходных параметров
        if (clazzOutParameters == null) {
            
            
            
            this.clazzOutParameters = null;



        // non-cursor types
        // некурсорные типы
        } else if (
                clazzOutParameters.equals(java.lang.Character.class) 
                ||
                clazzOutParameters.equals(java.lang.Boolean.class) 
                ||
                clazzOutParameters.equals(java.lang.Byte.class)
                ||
                clazzOutParameters.equals(java.lang.Short.class) 
                ||
                clazzOutParameters.equals(java.lang.Integer.class) 
                ||
                clazzOutParameters.equals(java.lang.Long.class) 
                ||
                clazzOutParameters.equals(java.lang.Float.class) 
                ||
                clazzOutParameters.equals(java.lang.Double.class)
                ||
                clazzOutParameters.equals(java.lang.Void.class) 
                || 
                clazzOutParameters.equals(String.class)
                || 
                clazzOutParameters.equals(LocalDateTime.class)
                ||
                clazzOutParameters.equals(LocalDate.class)
                ||
                clazzOutParameters.equals(LocalTime.class)
                ) {



            this.clazzOutParameters = clazzOutParameters;

        // multiple output parameters
        // несколько выходных параметров
        } else if (Arrays.asList(clazzOutParameters.getDeclaredFields()).stream()
                .anyMatch(field -> field.isAnnotationPresent(OutParam.class))
                ||
                Arrays.asList(clazzOutParameters.getDeclaredMethods()).stream().anyMatch(method -> Arrays.asList(method.getParameters()).stream().anyMatch(parameter -> parameter.isAnnotationPresent(OutParam.class)))
                ) {

            // TODO move mapping Fields and Methods into separate functions (polymorphism in annotations???) (вынести mapping Fields и Methods в отдельные функции (полиморфизм в аннотациях???))

            this.clazzOutParameters = clazzOutParameters;
            
            // note that you can call the executeReturnedSeveralOutParams method
            // помечаем, что можно вызвать метод executeReturnedSeveralOutParams
            this.isReturnedSeveralOutParam = true;
            
            // check if there is a field in the class with the annotation @OutParam
            // проверяем есть ли поле в классе с аннотацией @OutParam
            mappingOutParamToField = new HashMap<>();
            Field[] fiels = clazzOutParameters.getDeclaredFields();
            for (int i = 0; i < fiels.length; i++) {

                if (fiels[i].isAnnotationPresent(OutParam.class)) {
                    mappingOutParamToField.put(fiels[i].getAnnotation(OutParam.class).name().toUpperCase(), fiels[i]);
                }
            }


            // check if there is a method in the class with @OutParam annotated parameters
            // проверяем есть ли метод в классе с аннотированными @OutParam параметрами
            Method[] methods = clazzOutParameters.getDeclaredMethods();
            mappingOutParamToMethod = new HashMap<>();
            for (int i = 0; i < methods.length; i++) {

                Parameter[] parameters = methods[i].getParameters();
                if (Arrays.asList(parameters).stream()
                        .allMatch(parameter -> parameter.isAnnotationPresent(OutParam.class))) {
                    for (int j = 0; j < parameters.length; j++) {
                        mappingOutParamToMethod.put(parameters[j].getAnnotation(OutParam.class).name().toUpperCase(),
                                methods[i]);
                    }

                } else if (Arrays.asList(parameters).stream()
                        .anyMatch(parameter -> parameter.isAnnotationPresent(OutParam.class))) {
                    throw new Exception(
                            "Either mark all parameters with the @OutParam annotation, or don't use this method at all "
                                    + methods[i].getName() + " as receiving data from output parameters");
                }

            }

            // check if there is a class in a class with @OutParam annotated parameters
            // проверяем есть ли класс в классе с аннотированными @OutParam параметрами
            boolean isExistInternalClazzes = false;
            Class<?>[] internalClazzes = clazzOutParameters.getDeclaredClasses();
            for (int i = 0; i < internalClazzes.length; i++) {
                if (internalClazzes[i].isAnnotationPresent(OutParam.class)) {
                    // take the value of the @OutParam annotation above the class
                    //берем значение аннотации @OutParam над классом
                    String paramName = internalClazzes[i].getAnnotation(OutParam.class).name();

                    Map<String, Field> mappingToField = new HashMap<>();
                    Map<String, Method> mappingToMethod = new HashMap<>();

                    Field[] fielsInternalClazzes = internalClazzes[i].getDeclaredFields();
                    for (int j = 0; j < fielsInternalClazzes.length; j++) {

                        if (fielsInternalClazzes[j].isAnnotationPresent(Column.class)) {
                            mappingToField.put(fielsInternalClazzes[j].getAnnotation(Column.class).name().toUpperCase(),
                                    fielsInternalClazzes[j]);
                        }
                    }

                    Method[] methodsInternalClazzes = internalClazzes[i].getDeclaredMethods();
                    for (int j = 0; j < methodsInternalClazzes.length; j++) {

                        Parameter[] parametersMethodsInternalClazzes = methodsInternalClazzes[j].getParameters();
                        if (Arrays.asList(parametersMethodsInternalClazzes).stream()
                                .allMatch(parameter -> parameter.isAnnotationPresent(Column.class))) {
                            for (int k = 0; k < parametersMethodsInternalClazzes.length; k++) {
                                mappingToMethod.put(parametersMethodsInternalClazzes[k].getAnnotation(Column.class)
                                        .name().toUpperCase(), methodsInternalClazzes[j]);
                            }

                        } else if (Arrays.asList(parametersMethodsInternalClazzes).stream()
                                .anyMatch(parameter -> parameter.isAnnotationPresent(Column.class))) {
                            throw new Exception(
                                    "Either mark all parameters with the @Column annotation or don't use this "
                                            + methodsInternalClazzes[j].getName()
                                            + " method at all as receiving data from output parameters");
                        }

                    }
                    programUnit.addDeclaredRowMapper(
                            paramName.equals("") ? DEFAULT_NAME_RETURN_PARAM_IN_FUNCTION.toUpperCase()
                                    : paramName.toUpperCase(),
                            getHandlerOneRecordByCursor(internalClazzes[i], clazzOutParameters, mappingToField,
                                    mappingToMethod));

                    isExistInternalClazzes = true;
                }
            }

            if (mappingOutParamToField.isEmpty() && mappingOutParamToMethod.isEmpty() && isExistInternalClazzes) {
                throw new Exception("No fields, method parameters or classes annotated with @OutParam");
            }



            // только один курсор
        } else if (Arrays.asList(clazzOutParameters.getDeclaredFields()).stream()
                .anyMatch(field -> field.isAnnotationPresent(Column.class))
                ||
                Arrays.asList(clazzOutParameters.getDeclaredMethods()).stream().anyMatch(method -> Arrays.asList(method.getParameters()).stream().anyMatch(parameter -> parameter.isAnnotationPresent(Column.class)))
                ) {



            // note that you can call the executeReturnedOnlyOneCursor method
            // помечаем, что можно вызвать метод executeReturnedOnlyOneCursor
            this.isReturnedOnlyOneCursor = true;

            OutParam outParamAnnotation = null;

            String returnCursorName = DEFAULT_NAME_RETURN_PARAM_IN_FUNCTION.toUpperCase();

            if (clazzOutParameters.isAnnotationPresent(OutParam.class)) {
                outParamAnnotation = clazzOutParameters.getAnnotation(OutParam.class);
            }

            // if the class represents a stored function, then it must be either not annotated or only annotated with isReturnFucntionParam=true
            // если класс представляет хранимую функцию, то он должен быть либо не аннотирован либо аннотирован только с isReturnFucntionParam=true
            if (programUnit.isFunction() && (outParamAnnotation == null || (outParamAnnotation != null
                    && outParamAnnotation.isReturnFucntionParam() && outParamAnnotation.name().equals("")))) {
                returnCursorName = DEFAULT_NAME_RETURN_PARAM_IN_FUNCTION.toUpperCase();
            // otherwise we work with a stored procedure
            // иначе работаем с хранимой процедурой
            } else if (!programUnit.isFunction() && (outParamAnnotation != null
                    && !outParamAnnotation.isReturnFucntionParam() && !outParamAnnotation.name().equals(""))) {
                returnCursorName = outParamAnnotation.name().toUpperCase();
            } else {
                throw new Exception("Unknown how to display the class " + clazzOutParameters.getCanonicalName()
                        + " to output cursor");
            }

            // to store cursor column name mapping in class field
            // для хранения отображение имени столбца курсора в поле класса
            Map<String, Field> mappingColumnCursorToField = new HashMap<>();
            // to store the mapping of the cursor column name to class method parameters
            // для хранения отображения имени столбца курсора в параметры метода класса
            Map<String, Method> mappingColumnCursorToMethod = new HashMap<>();

            // check if there is a field in the class with the @Column annotation
            // проверяем есть ли поле в классе с аннотацией @Column
            Field[] fiels = clazzOutParameters.getDeclaredFields();
            for (int i = 0; i < fiels.length; i++) {

                if (fiels[i].isAnnotationPresent(Column.class)) {
                    mappingColumnCursorToField.put(fiels[i].getAnnotation(Column.class).name().toUpperCase(), fiels[i]);
                }
            }

            // check if there is a method in the class with @Column annotated parameters
            // проверяем есть ли метод в классе с аннотированными @Column параметрами
            Method[] methods = clazzOutParameters.getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {

                Parameter[] parameters = methods[i].getParameters();
                if (Arrays.asList(parameters).stream()
                        .allMatch(parameter -> parameter.isAnnotationPresent(Column.class))) {
                    for (int j = 0; j < parameters.length; j++) {
                        mappingColumnCursorToMethod.put(parameters[j].getAnnotation(Column.class).name().toUpperCase(),
                                methods[i]);
                    }

                } else if (Arrays.asList(parameters).stream()
                        .anyMatch(parameter -> parameter.isAnnotationPresent(Column.class))) {
                    throw new Exception(
                            "Either mark all parameters with the @Column annotation or don't use this "
                                    + methods[i].getName() + " method at all as receiving data from the cursor");
                }

            }

            if (mappingColumnCursorToField.isEmpty() && mappingColumnCursorToMethod.isEmpty()) {
                throw new Exception("No fields or method parameters annotated with @Column");
            }

            programUnit.addDeclaredRowMapper(returnCursorName, getHandlerOneRecordByCursor(clazzOutParameters, null,
                    mappingColumnCursorToField, mappingColumnCursorToMethod));

        } else {
            throw new Exception("It is not clear how to handle output parameters");
        }
    }

    //------------------------------------------------------------------------
    //  call block
    //  блок вызова
    //------------------------------------------------------------------------
    /**
     * 
     * The function is called only for a stored procedure with no output parameters (Функция вызывается только для хранимой процедуры без выходных параметров)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC</a>
     * @see javax.sql.DataSource
     * 
     * @param params input parameters (входные параметры)
     * @throws Exception runtime error
     */
    public void executeWithoutOutParameters(Object... params) throws Exception {

        if (this.clazzOutParameters != null) {
            throw new Exception(FUNCTION_UNDEFINED_MESSAGE);
        }
        this.programUnit.execute(params);
    }
    
    /**
     * 
     * The function is called only for a stored procedure or for a stored function with non-cursor input parameters 
     * (Функция вызывается только для хранимой процедуры или для хранимой функции с некурсорными входными параметрами)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC</a>
     * @see javax.sql.DataSource
     * 
     * @param params input parameters (входные параметры)
     * @throws Exception runtime error
     * @return returns a non-cursor value (возвращает не курсорное значение)
     */
    @SuppressWarnings("unchecked")
    public T executeReturnedOnlyOneNonCursor(Object... params) throws Exception {

        if (this.clazzOutParameters == null) {
            throw new Exception(FUNCTION_UNDEFINED_MESSAGE);
        }
        if(this.clazzOutParameters.equals(Short.class)){
            return (T) Short.valueOf(programUnit.executeFunction(Object.class, params).toString());
        }
        if(this.clazzOutParameters.equals(Integer.class)){
            return (T) Integer.valueOf(programUnit.executeFunction(Object.class, params).toString());
        }
        if(this.clazzOutParameters.equals(Long.class)){
            return (T) Long.valueOf(programUnit.executeFunction(Object.class, params).toString());
        }
        if(this.clazzOutParameters.equals(Float.class)){
            return (T) Float.valueOf(programUnit.executeFunction(Object.class, params).toString());
        }
        if(this.clazzOutParameters.equals(Double.class)){
            return (T) Double.valueOf(programUnit.executeFunction(Object.class, params).toString());
        }
        if (this.clazzOutParameters.equals(LocalDateTime.class)) {
            return (T) programUnit.executeFunction(java.sql.Timestamp.class, params).toLocalDateTime();
        }
        if(this.clazzOutParameters.equals(LocalDate.class)){
            return (T) programUnit.executeFunction(java.sql.Date.class, params).toLocalDate();
        }
        if(this.clazzOutParameters.equals(LocalTime.class)){
            return (T) programUnit.executeFunction(java.sql.Time.class, params).toLocalTime();
        }
        return programUnit.executeFunction(this.clazzOutParameters, params);
    }

    /**
     * 
     * The function is called on a stored procedure or stored function with a single exit cursor that returns a list of values
     * (Функция вызывается для хранимой процедуры или для хранимой функции с одним выходным курсором, который возвращает список значений)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC</a>
     * @see javax.sql.DataSource
     * 
     * @param params input parameters (входные параметры)
     * @throws Exception runtime error
     * 
     * @return records from the cursor
     */
    @SuppressWarnings("unchecked")
    public List<T> executeReturnedOnlyOneCursor(Object... params) throws Exception {

        if (!this.isReturnedOnlyOneCursor) {
            throw new Exception(FUNCTION_UNDEFINED_MESSAGE);
        }

        checkThisObject();

        List<T> reList;

        reList = programUnit.executeFunction((new ArrayList<>()).getClass(), params);

        checkThisObject();

        return reList;
    }

    /**
     * Function called on a stored procedure or stored function with a single exit cursor that returns a single value
     * (Функция вызывается для хранимой процедуры или для хранимой функции с одним выходным курсором, который возвращает одно значение)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC</a>
     * @see javax.sql.DataSource
     * 
     * @param params input parameters (входные параметры)
     * @throws Exception runtime error
     * 
     * @return records from the cursor
     */
    public T executeReturnedOnlyOneCursorFirstRecord(Object... params) throws Exception {

        List<T> records = executeReturnedOnlyOneCursor(params);

        return records.isEmpty() ? null : records.get(0);
    }

    /**
     * Function called on a stored procedure or stored function with many different output parameters
     * (Функция вызывается для хранимой процедуры или для хранимой функции с множеством различных выходных параметров)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC</a>
     * @see javax.sql.DataSource
     * 
     * @param params input parameters (входные параметры)
     * @throws Exception runtime error
     * @return output parameters (выходные параметры)
     */
    @SuppressWarnings("unchecked")
    public T executeReturnedSeveralOutParams(Object... params) throws Exception {
        if (!this.isReturnedSeveralOutParam) {
            throw new Exception(FUNCTION_UNDEFINED_MESSAGE);
        }

        checkThisObject();

        Map<String, Object> outParams = programUnit.execute(params);

        checkThisObject();

        // create an object of the type using the default constructor
        // создаем объект типа с помощью конструктора по умл
        Constructor<?>[] constructors = clazzOutParameters.getConstructors();
        if (constructors.length == 0) {
            throw new Exception("У класса " + clazzOutParameters.getCanonicalName() + " нет конструкторов");
        }
        Object objOutParams = constructors[0].newInstance();

        // copy data by methods
        // копируем данные по методам
        Map<String, Method> mapMethod = new HashMap<>(this.mappingOutParamToMethod);

        // running through the fields
        // пробегаем по полям
        for (Map.Entry<String,Object> outParam : outParams.entrySet()) {

            String outParamName = outParam.getKey().toUpperCase();

            // if this field
            // если это поле
            if (this.mappingOutParamToField.containsKey(outParamName)) {
                Field fieldClass = this.mappingOutParamToField.get(outParamName);

                // gives access even if the field is private
                // дает доступ даже если поле private
                fieldClass.setAccessible(true);

                // get the value from the column and convert it to the field type
                // получаем значение со столбца и преобразуем его к типу поля
                fieldClass.set(objOutParams, castFromDBOutParamToJava(fieldClass.getType(), outParam.getValue()));

            }
            // if these are method parameters
            // если это параметры метода
            else if (mapMethod.containsKey(outParamName)) {
                Method methodClass = mapMethod.get(outParamName);

                // gives access even if the field is private
                // дает доступ даже если метод private
                methodClass.setAccessible(true);

                Parameter[] parameters = methodClass.getParameters();
                List<Object> paramsForInvoke = new ArrayList<>();
                for (int j = 0; j < parameters.length; j++) {

                    // get annotation value
                    // берем значение аннотации
                    String columnAndParamName = parameters[j].getAnnotation(OutParam.class).name().toUpperCase();

                    // we take the value from the row from the database, convert it to the type of the parameter
                    // берем значение из строки с БД, преобразует к типу параметра
                    paramsForInvoke.add(j, castFromDBOutParamToJava(parameters[j].getType(),
                            outParams.get(columnAndParamName.toUpperCase())));

                    // remove the parameter from the display
                    // удаляем параметр из отображения
                    mapMethod.remove(columnAndParamName, methodClass);

                }

                methodClass.invoke(objOutParams, paramsForInvoke.toArray());

            }

        }

        return clazzOutParameters.cast(objOutParams);
    }


    //------------------------------------------------------------------------
    // auxiliary function block 
    // блок вспомогательных функций
    //------------------------------------------------------------------------
    /**
     * 
     * The function checks whether an error occurred during the cursor processing stage? If yes, an exception is thrown
     * (Функция проверяет: возникла ли ошибка на этапе обработки курсора?
     * Если да вываливается исключение)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     * 
     * @throws Exception when parsing (при парсинге)
     */
    private void checkThisObject() throws Exception {
        if (flagDefaultConstructorNotFound) {
            throw new Exception("No default constructor");
        }
        if (flagFieldNotFound) {
            throw new Exception("Called field is missing");
        }
        if (flagMethodInccorect) {
            throw new Exception("Called method is missing");
        }
        if (flagMetaDataNotFound) {
            throw new Exception("Metadata not found");
        }
        if (flagAmountColumnDontGet) {
            throw new Exception("No entries in cursor");
        }
        if (flagColumnNotFound) {
            throw new Exception("Column not found in selection");
        }
        if (flagCastFromDBToJavaWithError) {
            throw new Exception("Error when converting type from column type to Java type");
        }
    }

    /**
     * The function converts the type of the cursor column from the database from the current to the required one
     * (Функция преобразует тип колонки курсора с БД из текущего в нужный)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC</a>
     * @see java.sql.ResultSet
     * 
     * @param toCast what do we transform (к чему преобразуем)
     * @param rs current cursor entry(текущая запись курсора)
     * @param columnName column in cursor entry(колонка в записи курсора)
     * @throws SQLException error when converting from JDBC type to JAVA type
     */
    private Object castFromDBToJava(Class<?> toCast, ResultSet rs, String columnName) throws SQLException {
        if (toCast.equals(short.class)) {
            return rs.getShort(columnName);
        } else if (toCast.equals(int.class)) {
            return rs.getInt(columnName);
        } else if (toCast.equals(long.class)) {
            return rs.getLong(columnName);
        } else if (toCast.equals(Short.class)) {
            return rs.getShort(columnName);
        } else if (toCast.equals(Integer.class)) {
            return rs.getInt(columnName);
        } else if (toCast.equals(Long.class)) {
            return rs.getLong(columnName);
        } else if (toCast.equals(LocalDate.class)) {
			java.sql.Date date = rs.getDate(columnName);
            return date == null ? null : date.toLocalDate();
        } else if (toCast.equals(LocalTime.class)) {
			java.sql.Time time = rs.getTime(columnName);
            return time == null ? null : time.toLocalTime();
        } else if (toCast.equals(LocalDateTime.class)) {
			java.sql.Timestamp datetime = rs.getTimestamp(columnName);
            return datetime == null ? null : datetime.toLocalDateTime();
        } else if (toCast.equals(String.class)) {
            return rs.getString(columnName);
        } else if (toCast.equals(Double.class)) {
            return Double.valueOf(rs.getString(columnName));
        } else if (toCast.equals(double.class)) {
            return Double.valueOf(rs.getString(columnName));
        } else if (toCast.equals(Boolean.class)) {
            return rs.getBoolean(columnName);
        } else if (toCast.equals(boolean.class)) {
            return rs.getBoolean(columnName);
        } else {
            return rs.getObject(columnName);
        }
        
    }

    /**
     * The function converts the object to the desired type
     * (Функция преобразует объект к нужному типу)
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC</a>
     * @see java.sql.ResultSet
     * 
     * @param toCast what do we transform (к чему преобразуем)
     * @param obj what we transform (что преобразуем)
     * @throws SQLException when converting
     */
    private Object castFromDBOutParamToJava(Class<?> toCast, Object obj) {
		if(obj == null){
			return null; 	
		} else if (toCast.equals(short.class)) {
            return Short.valueOf(obj.toString());
        } else if (toCast.equals(int.class)) {
            return Integer.valueOf(obj.toString());
        } else if (toCast.equals(long.class)) {
            return Long.valueOf(obj.toString());
        } else if (toCast.equals(Short.class)) {
            return Short.valueOf(obj.toString());
        } else if (toCast.equals(Integer.class)) {
            return Integer.valueOf(obj.toString());
        } else if (toCast.equals(Long.class)) {
            return Long.valueOf(obj.toString());
        } else if (toCast.equals(LocalDate.class)) {
            return ((java.sql.Date) obj).toLocalDate();
        } else if (toCast.equals(LocalTime.class)) {
            return ((java.sql.Time) obj).toLocalTime();
        } else if (toCast.equals(LocalDateTime.class)) {
            return ((java.sql.Timestamp) obj).toLocalDateTime();
        } else if (toCast.equals(String.class)) {
            return obj.toString();
        } else if (toCast.equals(Double.class)) {
            return Double.valueOf(obj.toString());
        } else if (toCast.equals(double.class)) {
            return Double.valueOf(obj.toString());
        } else if (toCast.equals(Boolean.class)) {
            return Boolean.valueOf(obj.toString());
        } else if (toCast.equals(boolean.class)) {
            return Boolean.valueOf(obj.toString());
        } else {
            return obj;
        }
    }

    /**
     * 
     * The function returns a {@link RowMapper} that maps the cursor to a {@link List} of class objects (classCursorDefinition)
     * (Функция возвращает {@link RowMapper}, который отображает курсор в {@link List} объектов класса (classCursorDefinition))
     * 
     * @author Nikita Chistousov (chistousov.nik@yandex.ru)
     * @since 8
     * @see <a href=
     *      "https://docs.oracle.com/javase/8/docs/technotes/guides/jdbc/">JDBC</a>
     * @see java.sql.ResultSet
     * 
     * @param classCursorDefinition the class whose object is being created (класс, объект которого создается)
     * @param mainClassDefinition main class if classCursorDefinition is nested. May be null (главный класс, если classCursorDefinition является вложенным. Может быть null)
     * @param mappingColumnCursorToField display cursor column in class field (отображение колонки курсора в поле класса)
     * @param mappingColumnCursorToMethod mapping cursor column to class method parameter (отображение колонки курсора в параметр метода класса)
     * @throws SQLException when processing one record from the cursor
     */
    private RowMapper<?> getHandlerOneRecordByCursor(Class<?> classCursorDefinition, Class<?> mainClassDefinition,
            Map<String, Field> mappingColumnCursorToField, Map<String, Method> mappingColumnCursorToMethod) {

        return (ResultSet rs, int rowNumber) -> {
            // copies the mapping into a function for convenience
            // копирует отображение в функцию для удобства
            Map<String, Method> mapMethod = new HashMap<>(mappingColumnCursorToMethod);

            // calling the constructor without parameters
            // вызываем конструктор без параметров
            Object oneRecordObj = null;
            try {
                
                if (mainClassDefinition == null) {
                    Constructor<?> defaultConstructor = classCursorDefinition.getDeclaredConstructor();
                    oneRecordObj = defaultConstructor.newInstance();
                } else {
                // nested class
                // класс вложенный
                    Constructor<?> defaultConstructorInnerClass = classCursorDefinition
                            .getDeclaredConstructor(mainClassDefinition);
                    Constructor<?> defaultConstructorOuterClass = mainClassDefinition.getDeclaredConstructor();
                    oneRecordObj = defaultConstructorInnerClass.newInstance(defaultConstructorOuterClass.newInstance());
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.flagDefaultConstructorNotFound = true;
                return new Object();
            }

            // get column names
            // получаем названия колонок
            ResultSetMetaData resultSetMetaData = null;
            try {
                resultSetMetaData = rs.getMetaData();
            } catch (SQLException e1) {
                e1.printStackTrace();
                this.flagMetaDataNotFound = true;
                return new Object();
            }

            // get the number of records
            // получаем количество записей
            int columnCount = 0;
            try {
                columnCount = resultSetMetaData.getColumnCount();
            } catch (SQLException e1) {
                e1.printStackTrace();
                this.flagAmountColumnDontGet = true;
                return new Object();
            }

            // iterate over all columns
            // перебираем все колонки
            for (int i = 1; i <= columnCount; i++) {
                String columnName;
                try {
                    columnName = resultSetMetaData.getColumnName(i).toUpperCase();
                } catch (SQLException e1) {
                    e1.printStackTrace();
                    this.flagColumnNotFound = true;
                    return new Object();
                }

                // if this field
                // если это поле
                if (mappingColumnCursorToField.containsKey(columnName)) {
                    Field fieldClass = mappingColumnCursorToField.get(columnName);

                    // gives access even if the field is private
                    // дает доступ даже если поле private
                    fieldClass.setAccessible(true);

                    // get the value from the column and convert it to the field type
                    // получаем значение со столбца и преобразуем его к типу поля
                    try {
                        fieldClass.set(oneRecordObj, castFromDBToJava(fieldClass.getType(), rs, columnName));
                    } catch (Exception e) {
                        this.flagFieldNotFound = true;
                    }

                }
                // if these are method parameters
                // если это параметры метода
                else if (mapMethod.containsKey(columnName)) {
                    Method methodClass = mapMethod.get(columnName);

                    // gives access even if the method is private
                    // дает доступ даже если метод private
                    methodClass.setAccessible(true);

                    Parameter[] parameters = methodClass.getParameters();
                    List<Object> paramsForInvoke = new ArrayList<>();
                    for (int j = 0; j < parameters.length; j++) {

                        // get annotation value
                        // берем значение аннотации
                        String columnAndParamName = parameters[j].getAnnotation(Column.class).name().toUpperCase();

                        // we take the value from the row from the database, convert it to the type of the parameter
                        // берем значение из строки с БД, преобразует к типу параметра
                        try {
                            paramsForInvoke.add(j, castFromDBToJava(parameters[j].getType(), rs, columnAndParamName));
                        } catch (SQLException e) {
                            e.printStackTrace();
                            this.flagCastFromDBToJavaWithError = true;
                            return new Object();
                        }

                        // remove the parameter from the display
                        // удаляем параметр из отображения
                        mapMethod.remove(columnAndParamName, methodClass);

                    }
                    try {
                        methodClass.invoke(oneRecordObj, paramsForInvoke.toArray());
                    } catch (Exception ex) {
                        this.flagMethodInccorect = true;
                    }

                }
            }
            return oneRecordObj;
        };
    }

}