provider "kubernetes" {
  config_path = var.kubeconfig_path
}

# The shared teaching cluster assigns namespaces externally today. Keeping the
# default false makes plan a contract/validation check with no namespace
# changes; it does not silently claim or mutate instructor-owned namespaces.
resource "kubernetes_namespace" "crm" {
  for_each = var.manage_namespaces ? var.namespaces : {}

  metadata {
    name = each.value
    labels = {
      "app.kubernetes.io/part-of" = "northstar-crm"
      "northstar-crm/environment" = each.key
    }
  }
}
