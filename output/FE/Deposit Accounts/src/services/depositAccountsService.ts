import axios from 'axios';
export async function fetchData(request: DepositAccountsRequest): Promise<DepositAccountsResponse> { const r=await axios.post<DepositAccountsResponse>('',request); return r.data; }
