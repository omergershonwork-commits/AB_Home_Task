# AI Usage

AI was used as an assistant during development, mainly after I had already decided the architecture and processing flow.

## How I Used AI

### 1. Edge cases and failure scenarios
I used AI to help identify cases I could miss, such as Kafka retries, duplicate messages, poison records, database batch failures, DLQ behavior, and transient database outages.

I reviewed these cases myself and decided which ones were relevant to the assignment.

### 2. Faster implementation
After defining the design, classes, interfaces, and data flow, I used AI to speed up writing Java code, tests, configuration, and repetitive boilerplate.

The implementation direction and design decisions were mine; AI was used mainly to reduce development time.

### 3. Review and testing
I used AI to suggest tests and to review failure flows, especially around Kafka batching, database batching, idempotency, retries, and DLQ handling.

## Validation

No AI-generated code or recommendation was accepted blindly.

Every change was reviewed by me, tested, and changed or rejected when needed. Several suggestions were simplified or modified to keep the solution focused on the assignment rather than adding unnecessary complexity.

The final architecture, behavior, and submitted code are decisions I understand and verified myself.
