# sql-on-fly
SQL library for Java

last version 1.3

Usage:

For generating implementations append into your pom.xml in plugins section:
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-javadoc-plugin</artifactId>
  <version>2.9.1</version>
  <executions>
    <execution>
      <id>sql-on-fly</id>
      <goals>
        <goal>javadoc</goal>
      </goals>
      <phase>process-sources</phase>
      <configuration>
        <doclet>org.master.sqlonfly.impl.scanner.SqlDoclet</doclet>
        <docletArtifact>
          <groupId>org.master.sqlonfly</groupId>
          <artifactId>scanner</artifactId>
          <version>1.3</version>
        </docletArtifact>
        <useStandardDocletOptions>false</useStandardDocletOptions>
        <additionalparam>-genpath ${project.build.directory}/generated-sources/sql</additionalparam>
      </configuration>
    </execution>
  </executions>
</plugin>
```

For append generated sources to compiler:
```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId>
  <artifactId>build-helper-maven-plugin</artifactId>
  <version>1.8</version>
  <executions>
    <execution>
      <phase>generate-sources</phase>
      <goals>
        <goal>add-source</goal>
      </goals>
      <configuration>
        <sources>
          <source>${project.build.directory}/generated-sources/sql</source>
        </sources>
      </configuration>
    </execution>
  </executions>
</plugin>
```

Code example:
```java
/**
 * @connection system
 */
public interface SampleSQL extends ISQLBatch<SampleSQL> {

    /**
     *
     * {@code
     *  select
     *      ConfigName
     *  from
     *      config
     *  where
     *      ConfigStorage = <#storage#>
     * }
     *
     * @param storage VARCHAR(250)
     *
     * @return ConfigName
     * @throws org.master.unitoo.core.errors.DatabaseException
     *
     * @execute select
     */
    List<String> keys(String storage) throws DatabaseException;


    @Override
    /**
     *
     * {@code
     *  select
     *      ConfigName,
     *      ConfigUpdated     
     *  from
     *      config
     *  where
     *      ConfigStorage = <#storage#>
     * }
     *
     * @param storage VARCHAR(250)
     *
     * @return SqlDefaultDataTable
     * @throws org.master.unitoo.core.errors.DatabaseException
     *
     * @execute select
     */
    SqlDefaultDataTable getKeysInfo(String storage) throws DatabaseException;

    /**
     *
     * {@code
     *  update translates set
     *      TranslateValue = <#value#>,
     *      TranslateUpdated = NOW()
     *  where
     *      LabelKey = <#code#>
     *      and LanguageCode = <#lang#>
     * }
     *
     * @param lang VARCHAR(250)
     * @param code VARCHAR(250)
     * @param value VARCHAR(255)
     *
     * @return @@rowcount
     * @throws org.master.unitoo.core.errors.DatabaseException
     *
     * @execute update
     */
     int setValue(String lang, String code, Object value) throws DatabaseException;
     
    /**
     *
     * {@code
     *  insert into translates (LanguageCode, LabelKey, TranslateValue, TranslateUpdated)
     *      values (<#lang#>, <#code#>, <#value#>, NOW())
     * }
     *
     * @param lang VARCHAR(250)
     * @param code VARCHAR(250)
     * @param value VARCHAR(255)
     *
     * @return @@identity
     * @throws org.master.unitoo.core.errors.DatabaseException
     *
     * @execute update
     */
     long addValue(String lang, String code, Object value) throws DatabaseException;     
}
```
