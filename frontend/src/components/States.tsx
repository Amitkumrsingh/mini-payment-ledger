export function Loading(){return <div className="state" role="status">Loading financial data…</div>}
export function ErrorBanner({message}:{message:string}){return <div className="banner error" role="alert">{message}</div>}
export function Empty({message}:{message:string}){return <div className="state">{message}</div>}
export function Badge({status}:{status:string}){return <span className={`badge ${status.toLowerCase()}`}>{status.replace('_',' ')}</span>}
