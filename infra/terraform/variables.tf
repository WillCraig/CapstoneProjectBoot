variable "kubeconfig_path" {
  description = "Path to the kubeconfig used only when Terraform manages namespaces."
  type        = string
  default     = "~/.kube/config"
}

variable "manage_namespaces" {
  description = "Keep false while the instructor owns shared-cluster namespace assignment."
  type        = bool
  default     = false
}

variable "namespaces" {
  description = "Placeholder environment namespaces; replace with instructor assignments."
  type        = map(string)
  default = {
    dev       = "northstar-crm-dev"
    test      = "northstar-crm-test"
    stage     = "northstar-crm-stage"
    prod-like = "northstar-crm-prod-like"
  }

  validation {
    condition = alltrue([
      for namespace in values(var.namespaces) :
      can(regex("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$", namespace))
    ])
    error_message = "Namespaces must be valid lowercase Kubernetes DNS labels."
  }
}

variable "ingress_hosts" {
  description = "Placeholder environment hosts; replace with instructor assignments."
  type        = map(string)
  default = {
    dev       = "crm-dev.example.invalid"
    test      = "crm-test.example.invalid"
    stage     = "crm-stage.example.invalid"
    prod-like = "crm-prod-like.example.invalid"
  }
}
