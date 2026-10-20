/** Mirrors the Spring DTO validation rule: CALL, EMAIL, NOTE, or MEETING. */
export type InteractionType = 'CALL' | 'EMAIL' | 'NOTE' | 'MEETING';

export interface Interaction {
  id: string;
  customerId: string;
  interactionType: InteractionType;
  summary: string;
  correlationId: string;
  createdAt: string;
}

export interface CreateInteractionRequest {
  customerId: string;
  interactionType: InteractionType;
  summary: string;
  correlationId?: string;
}
