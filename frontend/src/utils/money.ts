export function dollarsToCents(value:string):string{
  const normalized=value.trim(); if(!/^\d+(\.\d{0,2})?$/.test(normalized))throw new Error('Enter a valid amount with at most two decimals');
  const [whole,fraction='']=normalized.split('.'); return (BigInt(whole)*100n+BigInt((fraction+'00').slice(0,2))).toString();
}
export function formatCents(value:string|number|bigint):string{
  const cents=BigInt(value); const sign=cents<0n?'-':''; const absolute=cents<0n?-cents:cents;
  const dollars=absolute/100n, remainder=(absolute%100n).toString().padStart(2,'0');
  return `${sign}${new Intl.NumberFormat('en-US',{style:'currency',currency:'USD',minimumFractionDigits:0,maximumFractionDigits:0}).format(Number(dollars))}.${remainder}`;
}

