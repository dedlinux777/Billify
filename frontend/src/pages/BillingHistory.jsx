import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';

export default function BillingHistory() {
    const [payments, setPayments] = useState([]);
    const [loading, setLoading]   = useState(true);
    const [error, setError]       = useState('');
    const navigate = useNavigate();

    useEffect(() => {
        api.get('/payments/my')
            .then(res => setPayments(res.data))
            .catch(() => setError('Failed to load billing history'))
            .finally(() => setLoading(false));
    }, []);

    return (
        <div style={styles.page}>
            <div style={styles.header}>
                <h1 style={styles.title}>Billing History</h1>
                <div style={{ display:'flex', gap:'12px' }}>
                    <button style={styles.linkBtn} onClick={() => navigate('/plans')}>
                        Plans
                    </button>
                    <button style={styles.linkBtn} onClick={() => navigate('/subscription')}>
                        My Subscription
                    </button>
                </div>
            </div>

            {loading && <p style={styles.msg}>Loading...</p>}
            {error   && <p style={styles.error}>{error}</p>}

            {!loading && payments.length === 0 && (
                <p style={styles.msg}>No payment records yet.</p>
            )}

            <div style={styles.list}>
                {payments.map(p => (
                    <div key={p.id} style={styles.row}>
                        <div>
                            <span style={styles.planName}>{p.planName}</span>
                            <span style={styles.date}>
                {new Date(p.paymentDate).toLocaleDateString()}
              </span>
                        </div>
                        <div style={styles.right}>
                            <span style={styles.amount}>₹{p.amount}</span>
                            <span style={{
                                ...styles.status,
                                background: p.status === 'SUCCESS' ? '#dcfce7' : '#fee2e2',
                                color:      p.status === 'SUCCESS' ? '#16a34a' : '#dc2626',
                            }}>
                {p.status}
              </span>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}

const styles = {
    page: { maxWidth:'700px', margin:'0 auto', padding:'2rem' },
    header: { display:'flex', justifyContent:'space-between',
        alignItems:'center', marginBottom:'2rem' },
    title: { fontSize:'24px', fontWeight:'600', margin:0 },
    linkBtn: { background:'none', border:'none', color:'#4f46e5',
        cursor:'pointer', fontSize:'14px', textDecoration:'underline' },
    list: { display:'flex', flexDirection:'column', gap:'12px' },
    row: { display:'flex', justifyContent:'space-between', alignItems:'center',
        background:'#fff', border:'1px solid #e5e7eb', borderRadius:'8px',
        padding:'14px 18px', boxShadow:'0 1px 4px rgba(0,0,0,0.04)' },
    planName: { fontWeight:'600', fontSize:'15px', marginRight:'12px' },
    date: { color:'#9ca3af', fontSize:'13px' },
    right: { display:'flex', alignItems:'center', gap:'12px' },
    amount: { fontWeight:'600', color:'#111' },
    status: { padding:'2px 10px', borderRadius:'12px',
        fontSize:'12px', fontWeight:'600' },
    msg: { color:'#6b7280' },
    error: { color:'#dc2626' },
};