/** Mirrors the Spring DTO validation rule and the ck_customer_status database constraint. */
export type CustomerStatus = 'PROSPECT' | 'ACTIVE' | 'CLOSED';

export interface Customer {
  customerId: string;
  fullName: string;
  status: CustomerStatus;
  createdAt: string;
}

export interface CreateCustomerRequest {
  fullName: string;
  status: 'PROSPECT' | 'ACTIVE';
}

export interface UpdateCustomerRequest {
  fullName: string;
  status: CustomerStatus;
}
