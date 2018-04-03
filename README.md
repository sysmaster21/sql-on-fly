# sql-on-fly
SQL library for Java

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

