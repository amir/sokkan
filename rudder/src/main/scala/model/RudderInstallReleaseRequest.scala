package sokkan.rudder.model

final case class RudderInstallReleaseRequest(
                                              name: String,
                                              namespace: String,
                                              repo: String,
                                              chart: String,
                                              version: String)
