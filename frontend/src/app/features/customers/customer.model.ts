export interface Customer {
  customerId: string;
  fullName: string;
  status: string;
}

export interface SaveCustomerRequest {
  fullName: string;
  status: string;
}
