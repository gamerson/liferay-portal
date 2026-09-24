# Client Extensions On Kubernetes: Diagrams

Companion to `CLIENT_EXTENSION_DESIGN.md`. Every arrow here is exercised by the scripts in `hack/`.

## Who Owns What

The chart owns the objects a platform team expects to configure. The operator owns the objects whose lifetime depends on Liferay having answered.

```mermaid
graph TB
    subgraph build["Build: liferay-sample-workspace"]
        YAML["client-extension.yaml"]
        GRADLE["Gradle build<br/>expands globs, inlines files"]
        IMAGE["Container image"]
        YAML --> GRADLE --> IMAGE
        GRADLE --> EXPANDED["expanded client-extension.yaml"]
    end

    subgraph chart["Helm chart: liferay-client-extension"]
        CR["ClientExtension<br/>(config + pod template)"]
        SVC["Service"]
        ING["Ingress"]
        SA["ServiceAccount"]
    end

    subgraph operator["DXP operator"]
        REC["ClientExtension reconciler"]
        TRANS["cxconfig translator<br/>YAML to OSGi JSON"]
        REC --- TRANS
    end

    subgraph cxns["Client extension namespace"]
        WL["Deployment / Job / CronJob"]
        SEC["Secret: ext-init mirror"]
        MIR["ConfigMap: virtual instance mirror<br/>shared by every CX"]
    end

    subgraph dxpns["Liferay namespace"]
        EPP["ConfigMap: ext-provision (public)"]
        EPI["ConfigMap: ext-provision (internal)"]
        EI["ConfigMap: ext-init"]
        DXPM["ConfigMap: virtual instance metadata"]
        LR["Liferay<br/>portal-k8s-agent"]
    end

    EXPANDED -.-> CR
    IMAGE -.-> CR

    CR --> REC
    REC -->|owns| EPP
    REC -->|owns| EPI
    REC -->|owns| WL
    REC -->|owns| SEC
    REC -->|owns| MIR

    EPP --> LR
    EPI --> LR
    LR -->|writes back| EI
    LR -->|publishes| DXPM

    EI -.->|mirrored| SEC
    DXPM -.->|mirrored| MIR

    SEC -->|volume| WL
    MIR -->|volume| WL
    SVC -->|selects| WL
    ING --> SVC
    SA -.-> WL

    classDef chartClass fill:#e8f0fe,stroke:#4285f4,color:#111
    classDef opClass fill:#e6f4ea,stroke:#34a853,color:#111
    classDef dxpClass fill:#fef7e0,stroke:#f9ab00,color:#111
    class CR,SVC,ING,SA chartClass
    class REC,TRANS,WL,SEC,MIR,EPP,EPI opClass
    class LR,EI,DXPM dxpClass
```

Blue is chart-owned, green is operator-owned, amber is written by Liferay.

## The Handshake

The ordering problem the chart cannot solve: the workload mounts credentials that do not exist until Liferay has read the configuration and answered.

```mermaid
sequenceDiagram
    autonumber
    participant H as Helm
    participant K as API server
    participant O as DXP operator
    participant L as Liferay agent
    participant W as Workload pod

    H->>K: apply ClientExtension (+ Service, Ingress, ServiceAccount)
    K-->>O: watch event

    O->>K: get LiferayEnvironment
    Note over O: consent check:<br/>is this namespace listed?
    O->>K: get <vi>-lxc-dxp-metadata
    Note over O: does Liferay know<br/>this virtual instance?

    O->>O: translate client-extension.yaml<br/>split by classification
    O->>K: apply ext-provision (public) + ext-provision (internal)
    Note over O: update in place, never recreate:<br/>Liferay keys configurations by ConfigMap UID
    O->>K: status Delivered=True

    K-->>L: watch event on metadataType=ext-provision
    L->>L: labels become configuration properties;<br/>mainDomain sets baseURL and .serviceAddress
    L->>L: create client extension types<br/>and OAuth2 applications
    L->>K: write ext-init ConfigMap<br/>(client id + plaintext client secret)

    K-->>O: watch event on metadataType=ext-init
    O->>K: list by serviceId label
    Note over O: found by label, never by<br/>reconstructing the name
    O->>K: mirror into a Secret in the CX namespace
    O->>K: mirror virtual instance metadata if namespaces differ
    O->>K: status Provisioned=True

    O->>K: apply Deployment / Job / CronJob
    Note over O: only now: the pod would<br/>hang on a missing mount otherwise
    K-->>W: schedule pod
    W->>W: mount /etc/liferay/lxc/dxp-metadata<br/>and /etc/liferay/lxc/ext-init-metadata
    W->>L: authenticate with the provisioned credentials
    O->>K: status Ready=True
```

## Dual Domains

A frontend asset is fetched by a browser and needs a routable host. A microservice is called by Liferay and can use a cluster-internal address. Liferay resolves both from one `mainDomain` annotation per ConfigMap, so the operator splits the payload.

```mermaid
graph LR
    CE["ClientExtension<br/>domains.public: cx.example.com<br/>domains.internal: auto"]

    CE --> SPLIT{"classification<br/>of each entry"}

    SPLIT -->|frontend| PUB["ext-provision (public)<br/>mainDomain: cx.example.com"]
    SPLIT -->|microservice<br/>configuration| INT["ext-provision (internal)<br/>mainDomain: svc.ns.svc.cluster.local:8080"]

    PUB --> BURL["baseURL = https://cx.example.com/o/...<br/>browser fetches assets"]
    INT --> SADDR[".serviceAddress = svc.ns.svc.cluster.local:8080<br/>Liferay calls the microservice"]

    BROWSER["Browser"] -.->|GET| BURL
    LIFERAY["Liferay"] -.->|POST| SADDR

    classDef pubClass fill:#e8f0fe,stroke:#4285f4,color:#111
    classDef intClass fill:#e6f4ea,stroke:#34a853,color:#111
    class PUB,BURL,BROWSER pubClass
    class INT,SADDR,LIFERAY intClass
```

## Cross Namespace

Liferay's agent is bound to exactly one namespace on both read and write, so the operator is the only thing that can bridge the gap.

```mermaid
graph TB
    subgraph teamA["Namespace: team-a"]
        CXA["ClientExtension A"]
        CXB["ClientExtension B"]
        WLA["Workload A"]
        WLB["Workload B"]
        MIRROR["ConfigMap mirror<br/>liferay.com-lxc-dxp-metadata<br/>ownerRefs: A and B, controller=false"]
        SECA["Secret: ext-init A"]
        SECB["Secret: ext-init B"]
    end

    subgraph teamB["Namespace: team-b (not consented)"]
        CXC["ClientExtension C<br/>Degraded: NamespaceNotPermitted"]
    end

    subgraph prod["Namespace: liferay-prod"]
        ENV["LiferayEnvironment<br/>clientExtensionNamespaces: [team-a]"]
        AGENT["Liferay agent<br/>watches THIS namespace only"]
        SRC["ConfigMap<br/>liferay.com-lxc-dxp-metadata"]
        EPA["ext-provision A"]
        EPB["ext-provision B"]
        EIA["ext-init A"]
        EIB["ext-init B"]
    end

    CXA -->|writes| EPA
    CXB -->|writes| EPB
    CXC -.->|refused| ENV

    EPA --> AGENT
    EPB --> AGENT
    AGENT --> EIA
    AGENT --> EIB
    AGENT --> SRC

    SRC -.->|one mirror, shared| MIRROR
    EIA -.-> SECA
    EIB -.-> SECB

    MIRROR --> WLA
    MIRROR --> WLB
    SECA --> WLA
    SECB --> WLB

    classDef deniedClass fill:#fce8e6,stroke:#ea4335,color:#111
    class CXC deniedClass
```

Note that the ext-provision ConfigMaps carry **no owner reference**: owner references must be same-namespace, and a cross-namespace one causes the garbage collector to delete the dependent. A finalizer cleans them up instead.