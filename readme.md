# TimedLdapTemplate

`TimedLdapTemplate` is a custom extension of the Spring LDAP `LdapTemplate` class, designed to provide timing metrics for critical LDAP operations. This project aims to make it easier to monitor and measure the performance of LDAP client pool usage and query execution, helping developers diagnose bottlenecks and optimize application performance.

---

## 📖 Table of Contents
- [Introduction](#Introduction)
- [Features](#features)
- [Why TimedLdapTemplate?](#why-timedldaptemplate)
- [Getting Started](#getting-started)
- [Usage Examples](#usage-examples)
    - [Example 1: Execute Read-Only Operation](#example-1-execute-read-only-operation)
    - [Example 2: Search Example](#example-2-search-example)
- [Performance Metrics](#performance-metrics)
- [Test Cases](#test-cases)
- [Contributing](#contributing)
- [License](#license)

---

## Introduction

The `TimedLdapTemplate` class extends Spring's `LdapTemplate` to add instrumentation for tracking the time taken to:
1. Acquire an LDAP client from the connection pool.
2. Perform the actual LDAP operation (e.g., search, read-only execution).
3. Release the client back to the pool.

This enhancement is critical for diagnosing performance issues in systems that rely heavily on LDAP, such as user authentication or directory lookups.

---

## Features

- **Transparent Metrics Collection**: Track timing metrics (`acquire`, `search`, `release`) for every LDAP operation.
- **Drop-in Replacement**: Fully compatible with Spring LDAP's `LdapTemplate`, requiring minimal code changes.
- **Thread-Safe Metrics Storage**: Utilizes a `ThreadLocal` mechanism to isolate metrics for each thread.
- **Enhanced Logging**: Logs operation timings to help developers identify slow operations.

---

## Why TimedLdapTemplate?

Spring LDAP's `LdapTemplate` provides robust utilities for interacting with LDAP servers but lacks built-in support for monitoring client pool performance. In high-concurrency environments, understanding the time spent on client acquisition, query execution, and client release is crucial for:

- Diagnosing bottlenecks in LDAP client pool management.
- Optimizing the configuration of LDAP connection pools.
- Gaining deeper insights into query performance.

`TimedLdapTemplate` addresses this gap by integrating performance metrics directly into the `LdapTemplate` workflow, making it easier for developers to analyze and optimize LDAP operations.

---

## Getting Started

### Prerequisites

- Java 8 or higher
- Spring LDAP dependency
- An LDAP server (real or in-memory for testing)

### Installation

Add the following dependency to your `build.gradle` file:

```groovy
implementation 'org.springframework.ldap:spring-ldap-core:<version>'
```


## Usage Examples

### Example 1: Execute Read-Only Operation

```java
TimedLdapTemplate ldapTemplate = new TimedLdapTemplate(contextSource);

String result = ldapTemplate.executeReadOnly(ctx -> {
  SearchControls controls = new SearchControls();
  controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
  NamingEnumeration<SearchResult> results = ctx.search("ou=users", "(uid=john.doe)", controls);
  if (results.hasMore()) {
    return results.next().getNameInNamespace();
  }
  return null;
});

System.out.println("Search Result: " + result);
Map<String, Long> metrics = TimedLdapTemplate.getMetrics();
metrics.forEach((key, value) -> System.out.println(key + ": " + value + " ms"));
```


#### What This Does:
- Executes a read-only LDAP operation.
- Collects and logs metrics for client acquisition, query execution, and client release.

---

### Example 2: Search Example

```java
SearchControls searchControls = new SearchControls();
searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

ldapTemplate.search(
    "ou=users",
            "(uid=john.doe)",
    searchControls, 
    (attributes) -> "User: " + attributes.get("cn").get()
);

Map<String, Long> metrics = TimedLdapTemplate.getMetrics();
metrics.forEach((key, value) -> System.out.println(key + ": " + value + " ms"));
```


#### What This Does:
- Executes an LDAP search operation.
- Logs timing metrics for each stage of the operation.

---

# Performance Metrics

The following metrics are collected for every LDAP operation:

- **`acquire`**: Time taken to acquire a client from the LDAP connection pool.
- **`search`**: Time spent performing the LDAP operation (e.g., search or read-only execution).
- **`release`**: Time taken to release the client back to the pool.

You can access the metrics using:

```java
Map<String, Long> metrics = TimedLdapTemplate.getMetrics();
metrics.forEach((key, value) -> System.out.println(key + ": " + value + " ms"));
```

#### Note:
- Metrics are thread-local, ensuring no cross-contamination between threads.

---

# Test Cases

### Included Test Cases

1. **`TimedLdapTemplateExampleTest`**:
  - Demonstrates end-to-end usage with an in-memory LDAP server.
  - Covers search operations and metrics collection.

2. **`TimedLdapTemplateExecuteReadOnlyTest`**:
  - Tests the `executeReadOnly` method with an in-memory LDAP server.
  - Validates performance metrics for read-only operations.

3. **`TimedLdapTemplateTest`**:
  - Unit tests for mocked components, including:
    - **`executeReadOnly()`**: Verifies metrics collection.
    - **`search()`**: Ensures proper exception handling and context management.

---

# Contributing

Contributions are welcome! To contribute:

1. Fork this repository.
2. Create a new branch for your feature or bugfix.
3. Submit a pull request with a detailed description.

---

# License

This project is licensed under the Apache License. See the LICENSE file for details.

