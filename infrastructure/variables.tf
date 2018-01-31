variable "product" {
  type    = "string"
  default = "cmc"
}

variable "microservice" {
  type = "string"
  default = "pdf-service"
}

variable "location" {
  type    = "string"
  default = "UK South"
}

variable "env" {
  type = "string"
}

variable "s2s-url" {
  default = "http://betaDevBccidamS2SLB.reform.hmcts.net"
}

variable "ilbIp"{}
