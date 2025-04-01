I've chosen Java for this project because its robust libraries for concurrency and networking, 
make it perfect for building scalable and efficient distributed systems.

-------------------------------------------
Setup Instructions
1. Java Version -  Java 17 (or later)

2. Maven - needed to build and run this project. If you don’t have it, please install it from the Maven website.

3. Lombok Installation - I've used Lombok to reduce boilerplate code for getters, setters, etc.

Adding Lombok to IDE:

In IntelliJ, open File > Settings > Plugins, search for “Lombok”, and install it. Then enable annotation processing in File > Settings > Build, Execution, Deployment > Compiler > Annotation Processors.

If this is not setup, IDE might complain about missing get/set methods.

4. Clone the Project
   Clone this repository or download it as a ZIP and extract it.
--------------------------------------------------
How to Run:

Open a terminal in the project folder.

Build the project:
mvn clean package

Run the application:
mvn spring-boot:run
----------------------------------------------------
ALTERNATE WAY OF RUNNING WITHOUT INSTALLING/SETTING:

Alternatively, I've added the fat jar file within the target folder(i.e. including dependencies).
we can just run following command to run the project using jar as well:

   java -jar target/log-aggregator-0.0.1-SNAPSHOT.jar

---------------------------------------------------
By default, the service starts on port 8080.

POST logs at http://localhost:8080/logs

example post request:

curl -X POST -H "Content-Type: application/json" \
-d '{
"serviceName": "auth-service",
"timestamp": "2025-04-01T10:15:02Z",
"message": "User login successful"
}' \

GET logs at http://localhost:8080/logs?service=...&start=...&end=...

example post request:

curl "http://localhost:8080/logs?service=auth-service&start=2025-04-01T10:00:00Z&end=2025-04-01T10:30:00Z"

------------------------------------------------

Data Structures and Efficiency:

1. ConcurrentHashMap<String, ConcurrentSkipListMap<Instant, List<LogEntry>>>

    Outer Map (ConcurrentHashMap): Keys are service names. This makes it easy to store logs by each service. It also supports thread-safe operations.

    Inner Map (ConcurrentSkipListMap): Keys are timestamps, and values are lists of log entries for that time. This structure is sorted by time, so it’s easy to fetch a range of logs.

2. Why This Helps:

    ConcurrentHashMap handles concurrency so multiple threads can add logs at the same time without messing each other up.
    
    ConcurrentSkipListMap is like a tree or skip list internally. It lets us do range queries (like subMap(start, end)) in O(log n) time to find the start point. Then we can collect logs quickly.

3. Time Complexity

    Add Log: Inserting into the ConcurrentSkipListMap is O(log n). Appending to the list at that timestamp is O(1).
    
    Query Logs: Doing a sub-map lookup in O(log n) to find the start, then collecting the logs in that range will take O(k), where k is number of logs in that range.
    
    Purge: We remove all entries older than 1 hour by using headMap(oneHourAgo). This is also O(log n) to find the cutoff point, and then we remove them in a loop.

4. Handling Old Logs
   
   We run a scheduled job every minute to remove logs older than 1 hour.
