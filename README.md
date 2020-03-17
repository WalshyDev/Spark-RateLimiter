# Spark-RateLimiter

## Description
This is a tiny library which allows you to set up rate-limiting using SparkJava. If you run an API and need to rate-limit requests then this is the lib for you!

## Usage
### Basic
Here's a basic usage which allows for 30 requests in a minute span using the requester IP address as the defining key.
This will map to all /api requests.
```java
private final RateLimiter rateLimiter = new RateLimiter(30, 1, TimeUnit.MINUTES);
    
public void setup() {
    rateLimiter.map("/api/*");
    
    Spark.path("/api", () -> Spark.get("/", (req, res) -> "{\"Hello\", \"World!\"}"));
}
```

### Defining the key
Defining which key to use as the rate-limiter is easy. Just add a new parameter to the `RateLimiter` class constructor.
```java
private final RateLimiter rateLimiter = new RateLimiter(30, 1, TimeUnit.MINUTES, Request::queryString);
    
public void setup() {
    rateLimiter.map("/api/*");
    
    Spark.path("/api", () -> Spark.get("/", (req, res) -> "{\"Hello\", \"World!\"}"));
}
```

### Map to a specific endpoint
To map to a specific path rather than a group you can just use that endpoint. For example:
```java
private final RateLimiter rateLimiter = new RateLimiter(30, 1, TimeUnit.MINUTES);
    
public void setup() {
    rateLimiter.map("/api/some/endpoint");
    
    // Since we only map to `/api/some/endpoint` this GET will not be rate-limited.
    Spark.path("/api", () -> Spark.get("/", (req, res) -> "{\"Hello\", \"World!\"}"));
}
```

## Maven/Gradle
### Maven
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

...

<dependency>
	<groupId>com.github.WalshyDev</groupId>
	<artifactId>Spark-RateLimiter</artifactId>
    <version>VERSION</version>
</dependency>
```

### Gradle
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

...

dependencies {
    implementation 'com.github.WalshyDev:Spark-RateLimiter:VERSION'
}
```

## Cleanup
The `RateLimiter` uses a `ScheduledExecutorService` internally. You should stop this executor on program shutdown. To do this just call `RateLimiter#stop`