import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import { randomUUID } from 'node:crypto';
import { ApolloServer } from '@apollo/server';
import { expressMiddleware } from '@as-integrations/express4';
import { schema,type Context } from './graphql/schema.js';
import { PaymentServiceClient } from './clients/paymentServiceClient.js';

const port=Number(process.env.PORT??4000);
const origins=(process.env.CORS_ALLOWED_ORIGINS??'http://localhost:5173').split(',');
const client=new PaymentServiceClient(process.env.PAYMENT_SERVICE_BASE_URL??'http://localhost:8080');
const app=express(); app.use(helmet({contentSecurityPolicy:process.env.NODE_ENV==='production'}));
app.get('/health',(_req,res)=>res.json({status:'UP'}));
const server=new ApolloServer<Context>({schema,includeStacktraceInErrorResponses:false}); await server.start();
app.use('/graphql',cors({origin:origins}),express.json({limit:'100kb'}),expressMiddleware(server,{context:async({req,res})=>{const supplied=req.header('x-correlation-id');const correlationId=supplied&&supplied.length<=100?supplied:randomUUID();res.setHeader('x-correlation-id',correlationId);console.info(JSON.stringify({event:'graphql_request',correlationId,operation:req.body?.operationName??'anonymous'}));return{client,correlationId}}}));
app.listen(port,()=>console.info(JSON.stringify({event:'server_started',port})));
