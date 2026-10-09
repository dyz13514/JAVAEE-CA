# Project Documentation

This directory stores project documentation, design references, and screenshots.

The login artwork is stored only in [`src/main/resources/static/images/cats-all-star.png`](../src/main/resources/static/images/cats-all-star.png), where Spring Boot serves it to the login page.

## Local database connection

The application connects to the MySQL `cats` database on `localhost:3306`.
Set `DB_USERNAME` and `DB_PASSWORD`, or create `config/application.properties`
in the project root with your local settings:

```properties
spring.datasource.username=${DB_USERNAME:your_mysql_username}
spring.datasource.password=${DB_PASSWORD:your_mysql_password}
```

Do not wrap the password in quotes: Spring treats those quotes as part of it.
This local file is ignored by Git. Run the application from the project root
so Spring Boot loads it. Ensure the database exists and the account can access
it; Hibernate updates the application tables during startup.
