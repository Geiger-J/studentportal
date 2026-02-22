# IB Computer Science IA — Design Overview Diagrams

---

## 1. UML Class Diagram

```mermaid
classDiagram
    direction TB

    class User {
        +id : Integer
        +fullName : String
        +email : String
        +password : String
        +role : Role
        +yearGroup : Integer
        +examBoard : ExamBoard
        +profileComplete : Boolean
        +availability : Set~Timeslot~
        +subjects : Set~Subject~
    }

    class Request {
        +id : Integer
        +type : RequestType
        +status : RequestStatus
        +weekStartDate : LocalDate
        +availableTimeslots : Set~Timeslot~
        +chosenTimeslot : Timeslot
        +archived : Boolean
    }

    class Subject {
        +id : Integer
        +code : String
        +displayName : String
    }

    class Timeslot {
        <<enumeration>>
        MON_P1
        MON_P2
        TUE_P1
        ...
        FRI_P7
    }

    class Role {
        <<enumeration>>
        STUDENT
        ADMIN
    }

    class RequestType {
        <<enumeration>>
        TUTOR
        TUTEE
    }

    class RequestStatus {
        <<enumeration>>
        PENDING
        MATCHED
        DONE
        CANCELLED
    }

    class ExamBoard {
        <<enumeration>>
        GCSE
        A_LEVELS
        IB
        NONE
    }

    class MatchingService {
        +performMatching(requests) List~Match~
        -buildGraph(tutors, tutees) Graph
        -applyConstraints(edge) Boolean
        -computeWeight(tutor, tutee, timeslot) Integer
        -deconflict(matches) List~Match~
    }

    class RequestService {
        +createRequest(user, form) Request
        +cancelRequest(id) void
        +archiveRequest(id) void
        +listActiveForUser(user) List~Request~
    }

    class UserService {
        +register(form) User
        +updateProfile(user, form) void
        +determineRole(email) Role
        +completeProfile(user) Boolean
    }

    class RequestStatusScheduler {
        +checkAndMarkDone() void
    }

    User "1" --> "0..*" Request : creates
    Request "0..*" --> "1" User : initiatedBy
    Request "0..*" --> "0..1" User : matchedWith
    Request "0..*" --> "1" Subject : about
    User "0..*" --> "0..*" Subject : studies
    User --> Role
    User --> ExamBoard
    User --> Timeslot
    Request --> RequestType
    Request --> RequestStatus
    Request --> Timeslot

    MatchingService ..> Request : processes
    MatchingService ..> User : reads
    RequestService ..> Request : manages
    RequestService ..> User : validates
    UserService ..> User : manages
    RequestStatusScheduler ..> Request : updates
    RequestStatusScheduler ..> MatchingService : triggers
```

---

## 2. System Overview Diagram

```mermaid
graph TD
    subgraph Users
        S[Student]
        A[Admin]
    end

    subgraph Web Interface
        PUB[Public Pages\nHome · Login · Register]
        STU[Student Portal\nDashboard · Profile · Requests]
        ADM[Admin Portal\nDashboard · User Management · Matching]
    end

    subgraph Application Core
        AUTH[Authentication &\nAuthorisation]
        REQ[Request Manager]
        MATCH[Matching Engine]
        SCHED[Status Scheduler]
        SUBJ[Subject Catalogue]
    end

    subgraph Data Store
        DB[(Persistent Storage\nUsers · Requests · Subjects)]
    end

    S -->|browsing| PUB
    S -->|authenticated| STU
    A -->|authenticated| ADM

    PUB --> AUTH
    STU --> AUTH
    ADM --> AUTH

    AUTH --> REQ
    AUTH --> SUBJ

    STU --> REQ
    ADM --> REQ
    ADM --> MATCH

    REQ --> DB
    MATCH --> REQ
    MATCH --> DB
    SCHED --> REQ
    SCHED --> DB
    SUBJ --> DB
```

---

## 3. Flowchart — Request / Booking Workflow

```mermaid
flowchart TD
    A([Student logs in]) --> B{Profile\ncomplete?}
    B -- No --> C[Complete profile\nyear · exam board · subjects · availability]
    C --> D{Profile\nvalid?}
    D -- No --> C
    D -- Yes --> E[Profile saved]
    B -- Yes --> E
    E --> F[Student chooses\nTUTOR or TUTEE role for request]
    F --> G[Selects subject\nfrom catalogue]
    G --> H[Selects available\ntimeslots for the week]
    H --> I{Duplicate\nrequest exists?}
    I -- Yes --> J[Error: request\nalready active]
    J --> F
    I -- No --> K[Request created\nStatus: PENDING]
    K --> L([Student waits\nfor matching round])
    L --> M{Admin runs\nmatching?}
    M -- No --> L
    M -- Yes --> N[Matching Engine\nprocesses PENDING requests]
    N --> O{Match\nfound?}
    O -- No --> P[Request remains\nPENDING]
    P --> L
    O -- Yes --> Q[Request updated\nStatus: MATCHED\nChosen timeslot set\nPartner linked]
    Q --> R[Student views\nmatched partner on dashboard]
    R --> S{Timeslot\nend-time passed?}
    S -- No --> R
    S -- Yes --> T[Scheduler marks\nRequest as DONE]
    T --> U([Session complete])
```

---

## 4. Flowchart — Matching Algorithm

```mermaid
flowchart TD
    A([Admin triggers\nMatching Run]) --> B[Load all PENDING\nrequests from storage]
    B --> C[Separate into\nTUTOR requests and TUTEE requests]
    C --> D[Expand each request\ninto one node per available timeslot]

    D --> E[For each TUTOR node\nand each TUTEE node:]
    E --> F{Same subject?}
    F -- No --> G[Skip pair]
    F -- Yes --> H{Same timeslot?}
    H -- No --> G
    H -- Yes --> I{Tutor year ≥\nTutee year?}
    I -- No --> G
    I -- Yes --> J{Different\nusers?}
    J -- No --> G
    J -- Yes --> K[Add edge to\nbipartite graph]

    K --> L[Compute edge weight:\nBase 100\n+ Exam board match +50\n+ Year proximity bonus +10–30]

    G --> M{More pairs\nto check?}
    L --> M
    M -- Yes --> E
    M -- No --> N[Run Maximum-Weight\nBipartite Matching on graph]

    N --> O[Sort matched edges\nby weight descending]
    O --> P[Greedy selection loop]
    P --> Q{Edge already\nused by matched request\nor timeslot conflict?}
    Q -- Yes --> R[Skip edge]
    Q -- No --> S[Confirm match:\nlink partners\nset chosen timeslot\nmark both MATCHED]
    R --> T{More edges?}
    S --> T
    T -- Yes --> P
    T -- No --> U[Save all confirmed\nmatches to storage]
    U --> V([Matching round\ncomplete])
```

---

## 5. Flowchart — User Interaction Process

```mermaid
flowchart TD
    A([User visits portal]) --> B{Registered?}
    B -- No --> C[Fill registration form\nfull name · email · password]
    C --> D{Email format\nvalid?}
    D -- No --> C
    D -- Yes --> E{Email starts\nwith digit?}
    E -- Yes --> F[Role: STUDENT]
    E -- No --> G[Role: ADMIN]
    F --> H[Account created]
    G --> H
    B -- Yes --> I[Login with\nemail & password]
    H --> I
    I --> J{Credentials\ncorrect?}
    J -- No --> K[Show error] --> I
    J -- Yes --> L{Role?}
    L -- STUDENT --> M{Profile\ncomplete?}
    M -- No --> N[Redirect to\nprofile setup]
    N --> O[Save year group\nexam board\nsubjects\nweekly availability]
    O --> P[Student Dashboard]
    M -- Yes --> P
    P --> Q{Student action?}
    Q -- View requests --> R[List active requests\nwith status & partner]
    Q -- New request --> S[Create Tutor/Tutee request\nfor chosen subject & timeslots]
    Q -- Cancel request --> T[Cancel PENDING request]
    R --> P
    S --> P
    T --> P
    L -- ADMIN --> U[Admin Dashboard]
    U --> V{Admin action?}
    V -- Run matching --> W[Trigger matching engine]
    V -- Manage users --> X[View or deactivate users]
    V -- Manage requests --> Y[View, cancel or archive requests]
    W --> U
    X --> U
    Y --> U
```

---

## 6. Sequence Diagram — Student Books a Tutoring Session

```mermaid
sequenceDiagram
    actor Student
    participant Portal as Web Portal
    participant Auth as Authentication
    participant Profile as Profile Manager
    participant Requests as Request Manager
    participant Matching as Matching Engine
    participant Storage as Data Store

    Student->>Portal: Visit login page
    Portal->>Auth: Submit credentials
    Auth->>Storage: Look up user by email
    Storage-->>Auth: Return user record
    Auth-->>Portal: Authentication confirmed
    Portal-->>Student: Redirect to dashboard

    Student->>Portal: Open "New Request" form
    Portal->>Profile: Check profile completeness
    Profile->>Storage: Load user profile
    Storage-->>Profile: Return profile data
    Profile-->>Portal: Profile complete — allow request

    Student->>Portal: Submit request (TUTEE, Maths, Mon P2 / Tue P3)
    Portal->>Requests: Create request
    Requests->>Storage: Check for duplicate active request
    Storage-->>Requests: No duplicate found
    Requests->>Storage: Save new PENDING request
    Storage-->>Requests: Request saved
    Requests-->>Portal: Success
    Portal-->>Student: Dashboard updated — request shown as PENDING

    Note over Portal, Matching: Later — Admin triggers matching round

    Matching->>Storage: Load all PENDING requests
    Storage-->>Matching: Return request list
    Matching->>Matching: Build bipartite graph\nApply constraints & weights\nRun maximum-weight matching
    Matching->>Storage: Save matched pairs\nSet status MATCHED and chosen timeslot
    Storage-->>Matching: Saved

    Student->>Portal: Refresh dashboard
    Portal->>Storage: Load student's requests
    Storage-->>Portal: Return updated requests
    Portal-->>Student: Show MATCHED status\nwith partner name and timeslot

    Note over Portal, Storage: After timeslot end-time passes

    Matching->>Storage: Scheduler checks MATCHED requests
    Storage-->>Matching: Return requests past end-time
    Matching->>Storage: Mark requests as DONE
    Portal-->>Student: Session shown as DONE on next visit
```

---

## 7. Data Model (ER-style) Diagram

```mermaid
erDiagram
    USER {
        int id PK
        string fullName
        string email
        string role
        int yearGroup
        string examBoard
        bool profileComplete
        datetime createdAt
    }

    REQUEST {
        int id PK
        string type
        string status
        date weekStartDate
        string chosenTimeslot
        bool archived
        datetime createdAt
    }

    SUBJECT {
        int id PK
        string code
        string displayName
    }

    TIMESLOT_SLOT {
        string value PK
    }

    USER ||--o{ REQUEST : "creates"
    USER |o--o{ REQUEST : "matched as partner"
    REQUEST }o--|| SUBJECT : "is about"
    USER }o--o{ SUBJECT : "studies"
    USER }o--o{ TIMESLOT_SLOT : "available at"
    REQUEST }o--o{ TIMESLOT_SLOT : "offered at"
```

---

## 8. State Diagram — Request Lifecycle

```mermaid
stateDiagram-v2
    [*] --> PENDING : Student submits request

    PENDING --> MATCHED : Admin runs matching\nand a suitable partner is found

    PENDING --> CANCELLED : Student or admin\ncancels the request

    MATCHED --> DONE : Scheduled checker detects\ntimeslot end-time has passed

    MATCHED --> CANCELLED : Student or admin\ncancels before session

    DONE --> [*] : Request archived\nor removed from active view

    CANCELLED --> [*] : Request closed
```

---

## 9. Module Dependency Diagram

```mermaid
graph LR
    subgraph Presentation
        PC[Profile Controller]
        RC[Request Controller]
        AC[Admin Controller]
        AU[Auth Controller]
    end

    subgraph Business Logic
        US[User Service]
        RS[Request Service]
        MS[Matching Service]
        SS[Subject Service]
        SCH[Status Scheduler]
        TS[Time Service]
    end

    subgraph Data Access
        UR[User Repository]
        RR[Request Repository]
        SR[Subject Repository]
    end

    subgraph Domain Model
        U[User]
        R[Request]
        SU[Subject]
    end

    PC --> US
    RC --> RS
    AC --> MS
    AC --> RS
    AC --> US
    AU --> US

    US --> UR
    RS --> RR
    MS --> RR
    MS --> TS
    SS --> SR
    SCH --> RR
    SCH --> TS

    UR --> U
    RR --> R
    SR --> SU
    U --> SU
    R --> U
    R --> SU
```
