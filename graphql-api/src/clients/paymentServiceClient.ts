import { BackendError } from '../errors/backendError.js';
import JSONbig from 'json-bigint';
const json=JSONbig({storeAsString:true});

export class PaymentServiceClient {
  constructor(private baseUrl:string,private timeoutMs=5000){}
  private async request(path:string,correlationId:string,init?:RequestInit){
    const response=await fetch(`${this.baseUrl}${path}`,{...init,signal:AbortSignal.timeout(this.timeoutMs),headers:{'content-type':'application/json','x-correlation-id':correlationId,...init?.headers}});
    const text=await response.text(); const body=text?json.parse(text):null;
    if(!response.ok)throw new BackendError(response.status,body??{}); return body;
  }
  get(path:string,cid:string){return this.request(path,cid)}
  post(path:string,body:unknown,cid:string){return this.request(path,cid,{method:'POST',body:JSON.stringify(body)})}
}
