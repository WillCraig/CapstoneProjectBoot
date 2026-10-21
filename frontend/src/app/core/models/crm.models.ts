export interface Customer {
  customerId: string;
  fullName: string;
  email: string;
  status: string;
  createdAt?: string;
}

export type InteractionChannel = 'PHONE' | 'EMAIL' | 'CHAT';

export interface Interaction {
  interactionId: string;
  customerId: string;
  channel: InteractionChannel;
  summary: string;
  createdAt: string;
  version: number | null;
}

export interface CreateInteractionRequest {
  channel: InteractionChannel;
  summary: string;
}

export type UpdateInteractionRequest = CreateInteractionRequest;
