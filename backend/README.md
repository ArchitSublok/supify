# Supify — Java Backend

Java JDBC backend for the Supify supplier verification platform, connecting to MySQL.

## Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/supify/
│   │   │   ├── config/      # DBConnection, app constants
│   │   │   ├── dao/         # Data Access Objects (JDBC CRUD)
│   │   │   ├── model/       # POJOs (Organisation, Supplier, Claim, etc.)
│   │   │   ├── service/     # Business logic layer
│   │   │   ├── servlet/     # HTTP servlet handlers (or main CLI runner)
│   │   │   └── util/        # Helpers (hashing, date formatting, etc.)
│   │   └── resources/       # db.properties, SQL templates
│   └── test/java/com/supify/ # JUnit tests
├── lib/                      # mysql-connector-j JAR
└── README.md
```

## Stack

- **Language:** Java 17+
- **Database:** MySQL 8.0+
- **Connectivity:** JDBC (mysql-connector-j)
- **Build:** javac / Maven (TBD)
- **Testing:** JUnit 5
