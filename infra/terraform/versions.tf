terraform {
  required_version = ">= 1.5.0"

  required_providers {
    kubernetes = {
      source  = "hashicorp/kubernetes"
      version = "~> 2.35"
    }
  }

  # Configure the approved remote backend before enabling manage_namespaces.
  # No state or credentials belong in this repository.
}
