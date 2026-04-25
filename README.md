What This Is?
It A backend spring boot project that prevents bot spam using Redis. It Handles 200 concurrent requests without breaking.

Tech Stack Used
* Java 21 + Spring Boot 4.0
* PostgreSQL 15 (data storage)
* Redis 7 (counters & locks)

Features
Phase 1: Basic API
    Create posts, comments, likes
    PostgreSQL database

Phase 2: Redis Guardrails
    100 bot replies max per post (atomic counter)
    20 comment depth limit
    10 minute cooldown between bot-human interactions

Instead of relying on Java synchronization (which fails in distributed systems), I used Redis as a centralized, thread-safe gatekeeper.
  1. Horizontal Cap (Max 100 Bot Replies)
    Approach:-
        I used Redis’ atomic INCR operation:

  Why this is thread-safe:
        INCR is atomic in Redis
        Even with 200 concurrent requests:
            Each increment happens sequentially inside Redis
            No two threads can corrupt the value
  2. Cooldown Cap (Bot ↔ Human Interaction)
    Approach:-
        Used Redis SETNX (set-if-absent) with TTL:

  Why this is thread-safe:
        SETNX is atomic
        Only one request succeeds
        Others fail immediately    

Phase 3: Smart Notifications
    Max 1 notification per 15 minutes
    Extra notifications queued in Redis
    CRON sweeper batches them every 5 minutes

Phase 4: Testing
    Race condition test (200 concurrent requests)
    Statelessness verification
    Data integrity checks

Quick Start:
1. Start databases
    docker-compose up -d
2. Run app
    mvn spring-boot:run
3. Test it
    Open Postman
    Click Import
    Upload the spring-assignment.postman_collection.json file
    Run the tests
