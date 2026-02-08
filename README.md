# Attendance Eligibility Management System

A comprehensive Java-based Project-Based Learning (PBL) system designed to help students and institutions manage and track attendance eligibility according to the 75% attendance rule. This system provides real-time calculations for both overall and subject-wise attendance, helping students make informed decisions about class attendance.

## 📋 Overview

The **Attendance Eligibility Management System** is a desktop application built using Java that automates attendance tracking and eligibility calculation. It addresses the common challenge students face in maintaining the mandatory 75% attendance requirement by providing:

- Real-time attendance percentage calculations
- Subject-wise and overall attendance tracking
- Predictive analysis of classes that can be missed or must be attended
- Holiday and planned leave management
- Weekly class schedule integration

This system serves as a practical solution for educational institutions implementing attendance policies and helps students stay compliant with attendance requirements while optimizing their time management.

## ✨ Features

### Core Features
- **75% Attendance Calculator**: Automatically calculates if a student meets the 75% attendance threshold
- **Subject-wise Tracking**: Individual attendance tracking for each subject with separate eligibility status
- **Overall Attendance**: Combined attendance percentage across all subjects
- **Predictive Analysis**: 
  - Shows how many more classes can be missed while maintaining 75% attendance
  - Indicates how many classes must be attended to reach 75% if currently below threshold
- **Holiday Management**: Factor in holidays and non-working days for accurate calculations
- **Planned Leave Integration**: Account for planned leaves (medical, personal, etc.) in attendance projections
- **Weekly Schedule**: Configure weekly class count per subject for accurate forecasting
- **User-friendly Interface**: Intuitive GUI built with Java Swing/JavaFX
- **Data Persistence**: Store attendance records in MySQL database
- **Real-time Updates**: Instant recalculation when attendance data is modified

### Additional Features
- **Student Profile Management**: Maintain student information and enrollment details
- **Attendance History**: View historical attendance records with date-wise tracking
- **Report Generation**: Generate attendance reports for individual students or entire classes
- **Alert System**: Notifications when attendance falls below critical thresholds
- **Multi-user Support**: Separate interfaces for students and administrators
- **Data Export**: Export attendance data to CSV/Excel formats

## 📊 Business Logic - The 75% Rule

### Understanding the 75% Attendance Rule

Most educational institutions mandate a minimum of **75% attendance** for students to be eligible to appear for examinations. This system implements the following logic:

### Calculation Formula

```
Attendance Percentage = (Classes Attended / Total Classes Conducted) × 100
```

### Key Scenarios

#### Scenario 1: Current Attendance Above 75%
If a student has attended 30 out of 35 classes (85.7% attendance):
- **Current Status**: Eligible ✓
- **Classes can miss**: Calculate maximum missable classes while maintaining 75%
- **Formula**: `(Classes Attended - 0.75 × Total Classes) / 0.75`
- **Example**: Student can miss up to 5 more classes and still maintain 75%

#### Scenario 2: Current Attendance Below 75%
If a student has attended 20 out of 35 classes (57.1% attendance):
- **Current Status**: Not Eligible ✗
- **Classes must attend**: Calculate required consecutive attendance to reach 75%
- **Formula**: `(0.75 × Total Classes - Classes Attended) / 0.25`
- **Example**: Student must attend next 20 consecutive classes to reach 75%

### Factors Considered

1. **Holidays**: Excluded from total class count
2. **Planned Leaves**: Can be marked in advance (medical certificates, emergencies)
3. **Weekly Schedule**: Different subjects have different class frequencies
4. **Semester Duration**: Calculate projections for remaining semester

### Subject-wise vs Overall Calculation

- **Subject-wise**: Each subject must individually meet 75% requirement
- **Overall**: Combined attendance across all subjects must meet 75%
- **Priority**: System alerts if any subject falls below threshold, even if overall is acceptable

## 🛠️ Tech Stack

### Core Technologies
- **Programming Language**: Java (JDK 8 or higher)
- **GUI Framework**: 
  - Java Swing (for lightweight desktop interface)
  - JavaFX (alternative for modern UI components)
- **Database**: MySQL 8.0 or higher
- **Database Connectivity**: JDBC (Java Database Connectivity)

### Java Concepts Utilized

#### 1. **Object-Oriented Programming (OOP)**
- Classes: `Student`, `Attendance`, `Subject`, `AttendanceCalculator`
- Inheritance: User hierarchy (Student, Teacher, Admin)
- Polymorphism: Different report generation strategies
- Encapsulation: Private data members with public getters/setters
- Abstraction: Abstract classes for common user operations

#### 2. **Collections Framework**
- `ArrayList<Student>`: Dynamic student list management
- `HashMap<String, Subject>`: Quick subject lookup by code
- `TreeSet<LocalDate>`: Sorted holiday dates
- `LinkedList<AttendanceRecord>`: Efficient insertion of attendance records
- Iterators and Streams for data processing

#### 3. **Exception Handling**
- Custom exceptions: `InvalidAttendanceException`, `DatabaseConnectionException`
- Try-catch blocks for database operations
- Try-with-resources for automatic resource management
- Exception propagation and logging

#### 4. **Multithreading**
- Background threads for database operations to keep UI responsive
- `SwingWorker` for asynchronous task execution
- Thread-safe collections for concurrent access
- Executor framework for managing thread pools

#### 5. **JDBC Integration**
- Connection pooling for efficient database access
- Prepared statements to prevent SQL injection
- Batch processing for bulk attendance updates
- Transaction management for data consistency

#### 6. **Design Patterns**
- **Singleton**: Database connection manager
- **Factory**: User object creation based on role
- **Observer**: Real-time UI updates when data changes
- **MVC**: Model-View-Controller architecture

## 📚 Syllabus Mapping

This project demonstrates proficiency in key Java concepts typically covered in academic curricula:

### Semester-wise Coverage

#### Core Java (Semester 3-4)
- ✅ Classes and Objects
- ✅ Constructors and Methods
- ✅ Inheritance and Polymorphism
- ✅ Interfaces and Abstract Classes
- ✅ Packages and Access Modifiers
- ✅ Exception Handling (checked and unchecked)
- ✅ File I/O operations
- ✅ String manipulation

#### Advanced Java (Semester 5-6)
- ✅ Collections Framework (List, Set, Map)
- ✅ Generics
- ✅ Lambda Expressions and Stream API
- ✅ Multithreading and Concurrency
- ✅ JDBC and Database Connectivity
- ✅ GUI Development (Swing/JavaFX)
- ✅ Event Handling
- ✅ MVC Architecture

#### Software Engineering Concepts
- ✅ Requirements Analysis
- ✅ System Design (UML diagrams)
- ✅ Database Design (ER diagrams)
- ✅ Testing and Debugging
- ✅ Version Control (Git)
- ✅ Documentation

### Learning Outcomes
1. Develop enterprise-level Java applications
2. Implement database-driven systems
3. Create user-friendly GUI applications
4. Apply design patterns to solve real-world problems
5. Handle exceptions and errors gracefully
6. Write maintainable and scalable code

## 🚀 How to Run

### Prerequisites
- Java Development Kit (JDK) 8 or higher
- MySQL Server 8.0 or higher
- IDE: IntelliJ IDEA, Eclipse, or NetBeans (optional)
- Git for version control

### Step 1: Clone the Repository
```bash
git clone https://github.com/archittmittal/Attendance-Eligibility-Management-System-JAVA-.git
cd Attendance-Eligibility-Management-System-JAVA-
```

### Step 2: Database Setup
1. Start MySQL server
2. Create a new database:
```sql
CREATE DATABASE attendance_system;
USE attendance_system;
```

3. Run the SQL script to create tables:
```sql
-- Create tables (script should be provided in /database/schema.sql)
SOURCE database/schema.sql;
```

4. Update database credentials in configuration file:
```java
// src/config/DatabaseConfig.java
DB_URL = "jdbc:mysql://localhost:3306/attendance_system"
DB_USER = "your_username"
DB_PASSWORD = "your_password"
```

### Step 3: Compile the Project

#### Using Command Line:
```bash
# Compile all Java files
javac -d bin -sourcepath src src/**/*.java

# If using external JARs (MySQL Connector)
javac -cp "lib/*" -d bin -sourcepath src src/**/*.java
```

#### Using IDE:
- Import the project as a Java project
- Add MySQL Connector JAR to build path
- Build the project (Ctrl+B / Cmd+B)

### Step 4: Run the Application

#### Using Command Line:
```bash
# Run main class
java -cp "bin:lib/*" com.attendance.Main

# On Windows
java -cp "bin;lib/*" com.attendance.Main
```

#### Using IDE:
- Locate the main class (e.g., `Main.java` or `AttendanceApp.java`)
- Right-click and select "Run"

### Step 5: Default Login Credentials
```
Username: admin
Password: admin123
```
(Change these after first login)

### Troubleshooting

**Issue**: Database connection error  
**Solution**: Verify MySQL is running and credentials are correct

**Issue**: ClassNotFoundException for MySQL Driver  
**Solution**: Add MySQL Connector JAR to classpath

**Issue**: GUI not displaying properly  
**Solution**: Ensure Java Swing/JavaFX runtime is available

## 🔮 Future Enhancements

### Planned Features
1. **Mobile Application**: Android/iOS app for on-the-go attendance tracking
2. **Biometric Integration**: Face recognition or fingerprint for automated attendance marking
3. **QR Code Attendance**: Quick attendance marking via QR code scanning
4. **Analytics Dashboard**: Visual charts and graphs for attendance trends
5. **Email/SMS Notifications**: Automated alerts for low attendance
6. **Parent Portal**: Allow parents to monitor student attendance
7. **Integration with LMS**: Connect with Learning Management Systems (Moodle, Canvas)
8. **Cloud Deployment**: Web-based version with cloud database
9. **AI Predictions**: Machine learning to predict attendance patterns
10. **Multi-semester Support**: Historical data across multiple semesters

### Technical Improvements
- **REST API**: Backend API for mobile integration
- **Spring Boot Migration**: Modern framework for enterprise features
- **Hibernate ORM**: Replace raw JDBC with ORM framework
- **JWT Authentication**: Token-based security
- **Docker Containerization**: Easy deployment with containers
- **Microservices Architecture**: Scalable service-based design
- **GraphQL**: Flexible data querying
- **CI/CD Pipeline**: Automated testing and deployment

### User Experience
- **Dark Mode**: Theme customization
- **Multi-language Support**: Internationalization (i18n)
- **Accessibility**: WCAG compliance for differently-abled users
- **Offline Mode**: Work without internet connectivity
- **Data Synchronization**: Sync across multiple devices

## 📄 License

This project is developed as part of an academic Project-Based Learning (PBL) initiative.

## 👥 Contributors

- **Archit Mittal** - Project Lead & Developer

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/archittmittal/Attendance-Eligibility-Management-System-JAVA-/issues).

## 📧 Contact

For questions or suggestions, please reach out through GitHub issues.

---

**Note**: This is an educational project created for learning purposes and demonstrates the application of various Java programming concepts in a real-world scenario.