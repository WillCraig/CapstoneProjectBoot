output "environment_contract" {
  description = "Non-secret namespace and Ingress contract consumed by deployment documentation."
  value = {
    for environment, namespace in var.namespaces : environment => {
      namespace    = namespace
      ingress_host = var.ingress_hosts[environment]
      managed      = var.manage_namespaces
    }
  }
}
