import { GraphQLError } from 'graphql';

export class BackendError extends Error {
  constructor(public status:number,public body:{code?:string;message?:string;correlationId?:string}){super(body.message??'Payment service request failed');}
}
export function toGraphQLError(error:unknown,correlationId:string){
  if(error instanceof BackendError)return new GraphQLError(error.body.message??'Request failed',{extensions:{code:error.body.code??'BACKEND_ERROR',httpStatus:error.status,correlationId:error.body.correlationId??correlationId}});
  return new GraphQLError('The service is temporarily unavailable',{extensions:{code:'SERVICE_UNAVAILABLE',httpStatus:503,correlationId}});
}

