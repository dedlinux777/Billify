import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';

export default function Plans() {
    const [plans, setPlans]   = useState([]);
    const [page, setPage]     = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(true);
    const [error, setError]   = useState('');
    const [subscribing, setSubscribing] = useState(null);

    const { logout } = useAuth();
    const navigate   = useNavigate();

    useEffect(() => {
        const fetchPlans = async () => {
            setLoading(true);
            try {
                const res = await api.get(`/plans?page=${page}&size=5`);
                setPlans(res.data.content);
                setTotalPages(res.data.totalPages);
            } catch (err) {
                setError('Failed to load plans', err);
            } finally {
                setLoading(false);
            }
        };
        fetchPlans();
    }, [page]);

    const handleSubscribe = async (planId) => {
        setSubscribing(planId);
        try {
            await api.post(`/subscriptions/subscribe/${planId}`);
            navigate('/subscription');
        } catch (err) {
            alert(err.response?.data?.error || 'Subscription failed');
        } finally {
            setSubscribing(null);
        }
    };

    return (
        <div style={styles.page}>
            <div style={styles.header}>
                <h1 style={styles.title}>Available Plans</h1>
                <div style={styles.headerActions}>
                    <button style={styles.linkBtn} onClick={() => navigate('/subscription')}>
                        My Subscription
                    </button>
                    <button style={styles.linkBtn} onClick={() => navigate('/billing')}>
                        Billing History
                    </button>
                    <button style={styles.logoutBtn} onClick={() => { logout(); navigate('/login'); }}>
                        Logout
                    </button>
                </div>
            </div>

            {loading && <p style={styles.msg}>Loading plans...</p>}
            {error   && <p style={styles.error}>{error}</p>}

            <div style={styles.grid}>
                {plans.map(plan => (
                    <div key={plan.id} style={styles.card}>
                        <h3 style={styles.planName}>{plan.name}</h3>
                        <p style={styles.desc}>{plan.description}</p>
                        <p style={styles.price}>₹{plan.price} / {plan.durationInDays} days</p>
                        <button
                            style={styles.subBtn}
                            onClick={() => handleSubscribe(plan.id)}
                            disabled={subscribing === plan.id}
                        >
                            {subscribing === plan.id ? 'Subscribing...' : 'Subscribe'}
                        </button>
                    </div>
                ))}
            </div>

            {totalPages > 1 && (
                <div style={styles.pagination}>
                    <button
                        style={styles.pageBtn}
                        disabled={page === 0}
                        onClick={() => setPage(p => p - 1)}
                    >
                        Previous
                    </button>
                    <span style={styles.pageInfo}>Page {page + 1} of {totalPages}</span>
                    <button
                        style={styles.pageBtn}
                        disabled={page >= totalPages - 1}
                        onClick={() => setPage(p => p + 1)}
                    >
                        Next
                    </button>
                </div>
            )}
        </div>
    );
}

const styles = {
    page: { maxWidth:'900px', margin:'0 auto', padding:'2rem' },
    header: { display:'flex', justifyContent:'space-between',
        alignItems:'center', marginBottom:'2rem' },
    title: { fontSize:'24px', fontWeight:'600', margin:0 },
    headerActions: { display:'flex', gap:'12px', alignItems:'center' },
    linkBtn: { background:'none', border:'none', color:'#4f46e5',
        cursor:'pointer', fontSize:'14px', textDecoration:'underline' },
    logoutBtn: { padding:'6px 14px', background:'#ef4444', color:'#fff',
        border:'none', borderRadius:'6px', cursor:'pointer', fontSize:'13px' },
    grid: { display:'grid', gridTemplateColumns:'repeat(auto-fill, minmax(240px, 1fr))',
        gap:'1.5rem' },
    card: { background:'#fff', border:'1px solid #e5e7eb', borderRadius:'8px',
        padding:'1.5rem', boxShadow:'0 1px 4px rgba(0,0,0,0.06)' },
    planName: { margin:'0 0 8px', fontSize:'18px', fontWeight:'600' },
    desc: { color:'#6b7280', fontSize:'14px', margin:'0 0 12px' },
    price: { fontWeight:'600', color:'#4f46e5', fontSize:'16px', margin:'0 0 16px' },
    subBtn: { width:'100%', padding:'8px', background:'#4f46e5', color:'#fff',
        border:'none', borderRadius:'6px', cursor:'pointer', fontSize:'14px' },
    pagination: { display:'flex', alignItems:'center', gap:'16px',
        justifyContent:'center', marginTop:'2rem' },
    pageBtn: { padding:'6px 16px', background:'#4f46e5', color:'#fff',
        border:'none', borderRadius:'6px', cursor:'pointer' },
    pageInfo: { color:'#6b7280', fontSize:'14px' },
    msg: { color:'#6b7280' },
    error: { color:'#dc2626' },
};