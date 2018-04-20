[_Helm_](https://helm.sh), the Kubernetes package manager, consists of a command line program named `helm` and a server-side component named Tiller. The command line component serves as a Tiller client and communicates using gRPC service exposed by Tiller. Tiller runs inside the Kubernetes cluster and manages packages.

Helm packages are called Charts and contain at least two things:

 - A description of the package, `Chart.yaml`.
 - One or more templates containing Kubernetes manifest files.

Furthermore charts can have their dependencies—which are charts themselves—bundled.

Unfortunately, Tiller does not entirely encapsulate the logic of installing and updating chart releases and it’s its command-line component which processes a Helm chart’s requirements and decides what needs to be sent to Tiller.

Each requirement of a chart, specified in `requirements.yaml`, may contain two optional fields:

 - `tags`
 - `condition`

By default all subcharts of a chart are loaded but if `tags` or `condition` fields are present they need to be evaluated and used to control loading of the charts they are applied to. As mentioned above this logic is not present in Tiller but in the command line program, therefore, any client wishing to communicate with Tiller need to:

 - Parse `values.yaml` of chart and all of its subcharts
 - Respect export and import-values fields to enable merging keys between parent and children charts
 - Coalesce values and also coalesce global values of a release
 - Parse conditions, and respect semantic versioning concepts.

Most of this logic is present in Helm’s [`pkg/chartutil`](https://github.com/kubernetes/helm/tree/v2.8.2/pkg/chartutil) package and almost all of that package needs to be imported or re-written in any 3rd party client. A web service written in Go programming language would be able to directly import this package, therefore, making communication with Tiller without command-line component a possibility. One such web service can be found at [AcalephStorage/rudder](https://github.com/AcalephStorage/rudder).

A complete Java implementation of this logic is available at [microbean/microbean-helm](https://github.com/microbean/microbean-helm). An incomplete, pure Scala, implementation of this logic can be found at: [amir/sokkan](https://github.com/amir/sokkan).


Considering the tight coupling of Tiller and helm CLI, any third-party solution need to closely monitor upstream changes in Tiller. Following table compares the effort required to maintain Tiller clients.

Solution | Maintenance Effort | Notes
:--------|:-------------------|:-----
helm CLI | None               | Being part of the distribution, no out-of-tree maintenance required
[AcalephStorage/rudder](https://github.com/AcalephStorage/rudder) | Medium | Low to medium maintenance required since this approach directly consumes a subset of Helm codebase as a library
[sokkan](https://github.com/amir/sokkan) or [microbean-helm](https://github.com/microbean/microbean-helm) | High | All upstream developments needs to be tracked closely

As indicated in the table above, there’s little to no efforts required to keep track of developments in Tiller—should it need to be upgraded—if helm CLI is chosen as the client. However, CLI might degrade the usability.

One common scenario in managing Helm releases is retrieving values from Kubernetes (in the form of `ConfigMaps`) and using those values to install or update charts. Even though helm CLI provides facilities to override values (using the `--set` flag), a long series of overrides may make command line length longer than POSIX `ARG_MAX`. Helm CLI also allows reading values from a file (using `--values` flag) and that requires managing lifecycle of a temporary resource URI.

The biggest shortcoming of directly using helm CLI as a client, however, is the CLI’s inability to produce output in a structured format (e.g., similar to kubectl’s `-o json`), desirable for listing or getting releases. There are several issues and PRs open at Helm to add this functionality. See: [1534](https://github.com/kubernetes/helm/issues/1534) and [2950](https://github.com/kubernetes/helm/pull/2950 ).
