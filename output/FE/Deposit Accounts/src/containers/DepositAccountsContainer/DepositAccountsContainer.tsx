import React, { useEffect, useState } from 'react';
import { Table } from '../components/Table/Table';
import { Table } from '../components/Table/Table';
import { Table } from '../components/Table/Table';

import { fetchData } from '../../services/depositAccountsService';
export const DepositAccountsContainer: React.FC = () => { const [data,setData]=useState<unknown>(); useEffect(()=>{ fetchData({}).then(setData).catch(console.error); },[]);
return <div><Table /><Table /><Table /></div>; };
